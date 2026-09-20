#include <Arduino.h>
#include "WifiProvisioningBle.h"
#include "secrets.h"

#include <FirebaseESP32.h>
#include <addons/TokenHelper.h>

WifiProvisioningBle wifiProvisioning;

FirebaseData fbdo;
FirebaseAuth fbAuth;
FirebaseConfig fbConfig;

bool firebaseReady = false;

void setupFirebase() {
  fbConfig.api_key = FIREBASE_API_KEY;
  fbConfig.database_url = FIREBASE_DATABASE_URL;
  Firebase.begin(&fbConfig, &fbAuth);
  Firebase.reconnectWiFi(true);

  firebaseReady = true;
}

void setup() {
  Serial.begin(115200);
  delay(500);

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


  delay(1000);
}