#include <Arduino.h>
#include "WifiProvisioningBle.h"
#include "secrets.h"

#include <FirebaseESP32.h>
#include <addons/TokenHelper.h>

#include <DHT.h>

// Sensor pins
#define DHT_PIN       4
#define DHT_TYPE      DHT22

#define MQ135_PIN     34
#define TCRT5000_PIN  27

// LED pins
#define LED_AIR       18
#define LED_TEMP      19
#define LED_HUMIDITY  21

// Alert thresholds
const float TEMP_LIMIT = 32.0;
const float LOW_HUMIDITY_LIMIT = 40.0;
const float HIGH_HUMIDITY_LIMIT = 70.0;
const int AIR_QUALITY_LIMIT = 1000;
const int TRAY_DIRTY_STATE = LOW;

// DHT22 object
DHT dht(DHT_PIN, DHT_TYPE);

// Sensor timing
unsigned long lastRead = 0;
const unsigned long READ_INTERVAL = 2000;

WifiProvisioningBle wifiProvisioning;

FirebaseData fbdo;         
FirebaseData fbdoStream;   
FirebaseAuth fbAuth;
FirebaseConfig fbConfig;

bool firebaseStarted = false;   
bool streamStarted = false;
volatile bool unpairRequested = false;

String statusPath;
String unpairPath;

String sensorDataPath;
String historyPath;
String alertsPath;
String deviceMacKey;

float latestTemperature = 0.0;
float latestHumidity = 0.0;
int latestAirReading = 0;
String latestTrayStatus = "Unknown";
bool latestDhtValid = false;

unsigned long lastHeartbeat = 0;
bool firstHeartbeatSent = false;
unsigned long lastSensorUpload = 0;
constexpr unsigned long HEARTBEAT_INTERVAL_MS = 10000;

// Historical logs: one sensor snapshot is appended every HISTORY_INTERVAL_MS
unsigned long lastHistoryLog = 0;
bool firstHistoryLogged = false;
constexpr unsigned long HISTORY_INTERVAL_MS = 5UL * 60UL * 1000UL;

// Alert events: one entry is appended when a condition turns on, at most once
// per ALERT_COOLDOWN_MS for each type so a value hovering at the limit does not spam.
enum AlertType { ALERT_HIGH_TEMP, ALERT_LOW_HUMIDITY, ALERT_HIGH_HUMIDITY, ALERT_POOR_AIR, ALERT_TRAY_DIRTY, ALERT_TYPE_COUNT };
const char *const ALERT_TYPE_NAMES[ALERT_TYPE_COUNT] = {
  "HIGH_TEMP", "LOW_HUMIDITY", "HIGH_HUMIDITY", "POOR_AIR", "TRAY_DIRTY"
};
bool alertActive[ALERT_TYPE_COUNT] = { false };     // latest condition from readSensors()
bool alertReported[ALERT_TYPE_COUNT] = { false };   // condition state last sent to Firebase
unsigned long lastAlertSent[ALERT_TYPE_COUNT] = { 0 };
bool alertEverSent[ALERT_TYPE_COUNT] = { false };
constexpr unsigned long ALERT_COOLDOWN_MS = 5UL * 60UL * 1000UL;

// Failed history/alert writes are retried no more often than this
constexpr unsigned long LOG_RETRY_MS = 30000;
unsigned long lastHistoryAttempt = 0;
unsigned long lastAlertFailure = 0;
bool alertWriteFailed = false;
constexpr int DELETE_RETRIES = 3;



void unpairStreamCallback(StreamData data) {
  if (data.dataType() == "boolean" && data.boolData()) {
    unpairRequested = true;
  }
}

void unpairStreamTimeoutCallback(bool timeout) {
  if (timeout) Serial.println(F("Unpair stream timed out, resuming..."));
}

void setupFirebase() {
  String owner = wifiProvisioning.getOwnerUid();
  String macKey = wifiProvisioning.getSavedMacAddress();

  if (owner.length() == 0 || macKey.length() == 0) {
    Serial.println(F("Missing owner UID or device MAC. Skipping Firebase setup."));
    return;
  }

  // Normalise to AA-BB-CC-DD-EE-FF to match the database paths
  macKey.replace(":", "-");

  statusPath = "/devices/" + owner + "/status/" + macKey;
  unpairPath = "/devices/" + owner + "/commands/" + macKey + "/unpair";
  sensorDataPath = "/devices/" + owner + "/current_status";
  historyPath = "/devices/" + owner + "/history/" + macKey;
  alertsPath = "/devices/" + owner + "/alerts";
  deviceMacKey = macKey;

  fbConfig.database_url = FIREBASE_DATABASE_URL;
  fbConfig.signer.tokens.legacy_token = FIREBASE_API_KEY;   // database secret (rotate it, keep it out of git)

  Firebase.begin(&fbConfig, &fbAuth);
  Firebase.reconnectWiFi(true);

  firebaseStarted = true;
  Serial.printf("Firebase initialised for device MAC: %s\n", macKey.c_str());
}

void startUnpairStream() {
  if (streamStarted || !Firebase.ready()) return;

  if (!Firebase.beginStream(fbdoStream, unpairPath.c_str())) {
    Serial.printf("Unpair stream failed: %s\n", fbdoStream.errorReason().c_str());
    return;
  }
  Firebase.setStreamCallback(fbdoStream, unpairStreamCallback, unpairStreamTimeoutCallback);
  streamStarted = true;
  Serial.println(F("Unpair stream started."));
}

void reportHeartbeat() {
  FirebaseJson json;
  json.set("state", "online");
  json.set("lastSeen/.sv", "timestamp");

  if (!Firebase.updateNode(fbdo, statusPath.c_str(), json)) {
    Serial.printf("Heartbeat failed: %s\n", fbdo.errorReason().c_str());
  }
}

void buildSensorJson(FirebaseJson &json) {
  json.set("temperatureCelsius", latestTemperature);
  json.set("humidityPercent", latestHumidity);
  json.set("airQualityAdc", latestAirReading);
  json.set("trayStatus", latestTrayStatus);
  json.set("timestamp/.sv", "timestamp");
  json.set("deviceOnline", true);
}

void uploadSensorData() {
  if (!latestDhtValid) return;

  FirebaseJson json;
  buildSensorJson(json);

  if (!Firebase.setJSON(fbdo, sensorDataPath.c_str(), json)) {
    Serial.printf("Sensor upload failed: %s\n",
                  fbdo.errorReason().c_str());
  } else {
    Serial.println("Sensor data uploaded successfully.");
  }
}

// Appends one sensor snapshot to /devices/{uid}/history/{mac}. Returns false if it should be retried.
bool logReading() {
  if (!latestDhtValid) return false;

  FirebaseJson json;
  buildSensorJson(json);

  if (!Firebase.pushJSON(fbdo, historyPath.c_str(), json)) {
    Serial.printf("History log failed: %s\n", fbdo.errorReason().c_str());
    return false;
  }
  Serial.println(F("History reading logged."));
  return true;
}

float alertValue(int type) {
  switch (type) {
    case ALERT_HIGH_TEMP:     return latestTemperature;
    case ALERT_LOW_HUMIDITY:
    case ALERT_HIGH_HUMIDITY: return latestHumidity;
    case ALERT_POOR_AIR:      return latestAirReading;
    default:                  return 0;
  }
}

// Appends an entry to /devices/{uid}/alerts for each condition that has just turned on.
void checkAlerts() {
  unsigned long now = millis();
  if (alertWriteFailed && now - lastAlertFailure < LOG_RETRY_MS) return;
  alertWriteFailed = false;

  for (int type = 0; type < ALERT_TYPE_COUNT; type++) {
    if (!alertActive[type]) {
      alertReported[type] = false;
      continue;
    }
    if (alertReported[type]) continue;
    if (alertEverSent[type] && now - lastAlertSent[type] < ALERT_COOLDOWN_MS) continue;

    FirebaseJson json;
    json.set("macKey", deviceMacKey);
    json.set("type", ALERT_TYPE_NAMES[type]);
    json.set("value", alertValue(type));
    json.set("timestamp/.sv", "timestamp");

    if (Firebase.pushJSON(fbdo, alertsPath.c_str(), json)) {
      alertReported[type] = true;
      alertEverSent[type] = true;
      lastAlertSent[type] = now;
      Serial.printf("Alert logged: %s\n", ALERT_TYPE_NAMES[type]);
    } else {
      Serial.printf("Alert log failed: %s\n", fbdo.errorReason().c_str());
      alertWriteFailed = true;
      lastAlertFailure = now;
      return;
    }
  }
}

bool deleteWithRetry(const String &path) {
  for (int i = 0; i < DELETE_RETRIES; i++) {
    if (Firebase.deleteNode(fbdo, path.c_str())) return true;   // deleting a missing node also succeeds
    Serial.printf("Delete failed (%s), attempt %d/%d\n", fbdo.errorReason().c_str(), i + 1, DELETE_RETRIES);
    delay(500);
  }
  return false;
}

void handleUnpair() {
  Serial.println(F("Unpair command detected from app. Cleaning up..."));

  Firebase.endStream(fbdoStream);
  Firebase.removeStreamCallback(fbdoStream);
  streamStarted = false;

  bool cmdOk = deleteWithRetry(unpairPath);
  bool statusOk = deleteWithRetry(statusPath);
  if (!cmdOk || !statusOk) {
    Serial.println(F("Cloud cleanup incomplete. Wiping locally anyway."));
  }

  delay(200);
  wifiProvisioning.clearCredentialsAndReset();
}

void setupSensors() {
  pinMode(TCRT5000_PIN, INPUT);

  pinMode(LED_AIR, OUTPUT);
  pinMode(LED_TEMP, OUTPUT);
  pinMode(LED_HUMIDITY, OUTPUT);

  digitalWrite(LED_AIR, LOW);
  digitalWrite(LED_TEMP, LOW);
  digitalWrite(LED_HUMIDITY, LOW);

  analogReadResolution(12);
  analogSetPinAttenuation(MQ135_PIN, ADC_11db);

  dht.begin();

  Serial.println("Sensors initialized.");
} 


void readSensors() {
  if (millis() - lastRead < READ_INTERVAL) {
    return;
  }

  lastRead = millis();

  // Read DHT22
  float temperature = dht.readTemperature();
  float humidity = dht.readHumidity();

  bool dhtValid =
      !isnan(temperature) && !isnan(humidity);

  // Read MQ-135
  int airReading = analogRead(MQ135_PIN);

  // Read TCRT5000
  int trayReading = digitalRead(TCRT5000_PIN);
  bool trayDirty = (trayReading == TRAY_DIRTY_STATE);
  latestTemperature = temperature;
latestHumidity = humidity;
latestAirReading = airReading;
latestTrayStatus = trayDirty ? "Needs Cleaning" : "Clean";
latestDhtValid = dhtValid;

  // Check alerts
  bool poorAir = airReading >= AIR_QUALITY_LIMIT;
  bool highTemp =
      dhtValid && temperature >= TEMP_LIMIT;
  bool lowHumidity =
      dhtValid && humidity <= LOW_HUMIDITY_LIMIT;
  bool highHumidity =
    dhtValid && humidity >= HIGH_HUMIDITY_LIMIT;

  alertActive[ALERT_HIGH_TEMP] = highTemp;
  alertActive[ALERT_LOW_HUMIDITY] = lowHumidity;
  alertActive[ALERT_HIGH_HUMIDITY] = highHumidity;
  alertActive[ALERT_POOR_AIR] = poorAir;
  alertActive[ALERT_TRAY_DIRTY] = trayDirty;

  // Control LEDs
  digitalWrite(LED_AIR, poorAir ? HIGH : LOW);
  digitalWrite(LED_TEMP, highTemp ? HIGH : LOW);
  digitalWrite(LED_HUMIDITY, (lowHumidity || highHumidity) ? HIGH : LOW);
             
  Serial.printf("LED GPIO 18: %s\n", poorAir ? "ON" : "OFF");
Serial.printf("LED GPIO 19: %s\n", highTemp ? "ON" : "OFF");
Serial.printf("LED GPIO 21: %s\n", (lowHumidity || highHumidity) ? "ON" : "OFF");
              

  // Print readings
  Serial.println("----------------------------");

  if (dhtValid) {
    Serial.printf("Temperature: %.1f C\n", temperature);
    Serial.printf("Humidity: %.1f %%\n", humidity);
  } else {
    Serial.println("ERROR: DHT22 reading failed");
  }

  Serial.printf("MQ-135 ADC: %d / 4095\n", airReading);
  Serial.printf("Air alert: %s\n", poorAir ? "YES" : "NO");

  Serial.printf("TCRT5000 digital: %d\n", trayReading);
  Serial.printf("Tray status: %s\n",
                trayDirty ? "Needs cleaning" : "Clean");

  Serial.printf("High temperature: %s\n",
                highTemp ? "YES" : "NO");

  Serial.printf("Low humidity: %s\n",
                lowHumidity ? "YES" : "NO");
  Serial.printf("High humidity: %s\n",
              highHumidity ? "YES" : "NO");
}


void setup() {
  Serial.begin(115200);
  delay(500);

  setupSensors();

  wifiProvisioning.begin();
}


void loop() {
  wifiProvisioning.loop();

  
  

  // Read sensors independently of Firebase
  readSensors();

  if (wifiProvisioning.isProvisioning()) return;
  if (!wifiProvisioning.isConnected()) return;

  if (!firebaseStarted) {
    setupFirebase();
    return;
  }

  if (!Firebase.ready()) return;

  startUnpairStream();

  if (unpairRequested) {
    unpairRequested = false;
    handleUnpair();
    return;
  }

  unsigned long now = millis();

  if (!firstHeartbeatSent ||
      now - lastHeartbeat >= HEARTBEAT_INTERVAL_MS) {
    firstHeartbeatSent = true;
    lastHeartbeat = now;
    reportHeartbeat();
  }

  if (now - lastSensorUpload >= READ_INTERVAL) {
  lastSensorUpload = now;
  uploadSensorData();
}

  checkAlerts();

  bool historyDue = !firstHistoryLogged || now - lastHistoryLog >= HISTORY_INTERVAL_MS;
  bool retryAllowed = lastHistoryAttempt == 0 || now - lastHistoryAttempt >= LOG_RETRY_MS;
  if (historyDue && retryAllowed) {
    lastHistoryAttempt = now;
    if (logReading()) {
      firstHistoryLogged = true;
      lastHistoryLog = now;
    }
  }

  
}


