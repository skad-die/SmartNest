#include <Arduino.h>
#include "WifiProvisioningBle.h"
#include "secrets.h"

#include <FirebaseESP32.h>
#include <addons/TokenHelper.h>

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

unsigned long lastHeartbeat = 0;
bool firstHeartbeatSent = false;
constexpr unsigned long HEARTBEAT_INTERVAL_MS = 10000;
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

void setup() {
  Serial.begin(115200);
  delay(500);

  wifiProvisioning.begin();
}

void loop() {
  wifiProvisioning.loop();

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
  if (!firstHeartbeatSent || now - lastHeartbeat >= HEARTBEAT_INTERVAL_MS) {
    firstHeartbeatSent = true;
    lastHeartbeat = now;
    reportHeartbeat();
  }
}