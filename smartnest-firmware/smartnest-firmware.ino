#include <Arduino.h>
#include "WifiProvisioningBle.h"
#include "secrets.h"

#include <FirebaseESP32.h>
#include <addons/TokenHelper.h>

#define LED_PIN 2

WifiProvisioningBle wifiProvisioning;

FirebaseData fbdo;
FirebaseData streamFbdo;
FirebaseAuth fbAuth;
FirebaseConfig fbConfig;

bool firebaseReady = false;
bool streamStarted = false;
bool ledState = false;

unsigned long lastHeartbeat = 0;
const unsigned long HEARTBEAT_INTERVAL_MS = 10000;

String commandPath;
String statusPath;
String lastUpdatePath;
String devicePath;

void setLed(bool on) {
  ledState = on;
  digitalWrite(LED_PIN, on ? HIGH : LOW);
  Serial.println(on ? "LED -> ON" : "LED -> OFF");
}

void buildPaths(const String &uid) {
  commandPath = "/devices/" + uid + "/led/command";
  statusPath = "/devices/" + uid + "/led/status";
  lastUpdatePath = "/devices/" + uid + "/led/lastUpdate";
  devicePath = "/devices/" + uid + "/deviceStatus";
}

void reportLedStatus() {
  if (!firebaseReady) return;
  if (!Firebase.setString(fbdo, statusPath.c_str(), ledState ? "ON" : "OFF")) {
    Serial.println("Failed to report LED status: " + fbdo.errorReason());
  }
  if (!Firebase.setInt(fbdo, lastUpdatePath.c_str(), (int)(millis() / 1000))) {
    Serial.println("Failed to report lastUpdate: " + fbdo.errorReason());
  }
}

void reportDeviceHeartbeat() {
  if (!firebaseReady) return;
  if (!Firebase.setString(fbdo, (devicePath + "/state").c_str(), "online")) {
    Serial.println("Failed to report device heartbeat: " + fbdo.errorReason());
  }
  Firebase.setInt(fbdo, (devicePath + "/lastSeen").c_str(), (int)(millis() / 1000));
}

void pollLedCommand() {
  if (!streamStarted) return;

  if (!Firebase.readStream(streamFbdo)) {
    Serial.println("Stream read error: " + streamFbdo.errorReason());
    return;
  }

  if (streamFbdo.streamAvailable() && streamFbdo.dataType() == "string") {
    String command = streamFbdo.stringData();
    command.trim();
    command.toUpperCase();

    if (command == "ON") {
      setLed(true);
      reportLedStatus();
    } else if (command == "OFF") {
      setLed(false);
      reportLedStatus();
    } else {
      Serial.println("Ignoring unrecognized command: '" + command + "'");
    }
  }
}

void setupFirebase() {
  fbConfig.api_key = FIREBASE_API_KEY;
  fbConfig.database_url = FIREBASE_DATABASE_URL;

  fbAuth.user.email = FIREBASE_USER_EMAIL;
  fbAuth.user.password = FIREBASE_USER_PASSWORD;

  Firebase.begin(&fbConfig, &fbAuth);
  Firebase.reconnectWiFi(true);

  Serial.println("Authenticating with Firebase...");
  unsigned long start = millis();
  while (fbAuth.token.uid.length() == 0 && millis() - start < 15000) {
    Serial.print(".");
    delay(300);
  }
  Serial.println();

  firebaseReady = true;

  String uid = String(fbAuth.token.uid.c_str());

  if (uid.length() == 0) {
    Serial.println("Firebase auth timed out -- check email/password and that");
    Serial.println("Email/Password sign-in is enabled in Firebase Console.");
    return;
  }

  Serial.println("Firebase UID: " + uid);
  buildPaths(uid);

  if (Firebase.beginStream(streamFbdo, commandPath.c_str())) {
    streamStarted = true;
  } else {
    Serial.println("Could not begin command stream: " + streamFbdo.errorReason());
  }

  reportDeviceHeartbeat();
  reportLedStatus();
}

void setup() {
  Serial.begin(115200);
  delay(500);

  pinMode(LED_PIN, OUTPUT);
  setLed(false);

  wifiProvisioning.begin();

  if (wifiProvisioning.isConnected()) {
    setupFirebase();
  }
}

void loop() {
  wifiProvisioning.loop();

  if (wifiProvisioning.isProvisioning()) {
    delay(50);
    return;
  }

  if (!firebaseReady && wifiProvisioning.isConnected()) {
    setupFirebase();
  }

  if (firebaseReady && wifiProvisioning.isConnected()) {
    pollLedCommand();

    unsigned long now = millis();
    if (now - lastHeartbeat >= HEARTBEAT_INTERVAL_MS) {
      lastHeartbeat = now;
      reportDeviceHeartbeat();
    }
  }

  delay(100);
}