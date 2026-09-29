#ifndef WIFI_PROVISIONING_BLE_H
#define WIFI_PROVISIONING_BLE_H

#include <WiFi.h>
#include <Preferences.h>
#include <ArduinoJson.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define SERVICE_UUID           "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
#define CREDENTIALS_CHAR_UUID  "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
#define STATUS_CHAR_UUID       "6e400003-b5a3-f393-e0a9-e50e24dcca9e"
#define SCAN_TRIGGER_CHAR_UUID "6e400004-b5a3-f393-e0a9-e50e24dcca9e"
#define NETWORKS_CHAR_UUID     "6e400005-b5a3-f393-e0a9-e50e24dcca9e"

constexpr int MAX_REPORTED_NETWORKS = 8;
constexpr int RESET_BUTTON_PIN = 0;
constexpr unsigned long HOLD_TIME_MS = 5000;
constexpr int ONBOARD_LED_PIN = 2;

class WifiProvisioningBle : public BLEServerCallbacks, public BLECharacteristicCallbacks {
public:
    WifiProvisioningBle() = default;
    ~WifiProvisioningBle() {
        stopBleProvisioning();
    }

    void begin() {
        pinMode(RESET_BUTTON_PIN, INPUT_PULLUP);
        pinMode(ONBOARD_LED_PIN, OUTPUT);

        preferences.begin("wifi-config", true);
        String savedSsid = preferences.getString("ssid", "");
        String savedOwner = preferences.getString("owner", "");
        preferences.end();

        if (savedSsid.length() > 0 && savedOwner.length() > 0) {
            connectToSavedNetwork(savedSsid);
        } else {
            startBleProvisioning();
        }
    }

    void loop() {
        checkResetButton();
        handleWifiConnectionState();

        if (hasPendingScan) {
            hasPendingScan = false;
            performScan();
        }
        if (hasPendingCredentials) {
            hasPendingCredentials = false;
            String credsCopy;
            portENTER_CRITICAL(&spinlock);
            credsCopy = pendingCredentialsValue;
            portEXIT_CRITICAL(&spinlock);
            processCredentials(credsCopy);
        }
    }

    bool isProvisioning() const { return provisioningActive; }
    bool isConnected() const { return WiFi.status() == WL_CONNECTED; }

    String getOwnerUid() {
        preferences.begin("wifi-config", true);
        String v = preferences.getString("owner", "");
        preferences.end();
        return v;
    }

    String getSavedMacAddress() {
        preferences.begin("wifi-config", true);
        String mac = preferences.getString("device_mac", "");
        preferences.end();
        return mac;
    }

    void clearCredentialsAndReset() {
        Serial.println(F("\n*** Factory Reset Triggered ***"));
        preferences.begin("wifi-config", false);
        preferences.clear();
        preferences.end();

        for (int i = 0; i < 10; i++) {
            digitalWrite(ONBOARD_LED_PIN, !digitalRead(ONBOARD_LED_PIN));
            delay(50);
        }

        Serial.println(F("Reset complete. Restarting ESP32..."));
        delay(500);
        ESP.restart();
    }

    // BLE Callbacks (Reused instances to avoid memory leaks)
    void onConnect(BLEServer*) override {
        deviceConnected = true;
        Serial.println(F("App connected via BLE."));
    }

    void onDisconnect(BLEServer* s) override {
        deviceConnected = false;
        Serial.println(F("App disconnected from BLE. Restarting advertising..."));
        delay(100);
        s->startAdvertising();
    }

    void onWrite(BLECharacteristic *c) override {
        if (c->getUUID().equals(BLEUUID(CREDENTIALS_CHAR_UUID))) {
            portENTER_CRITICAL(&spinlock);
            pendingCredentialsValue = c->getValue().c_str();
            hasPendingCredentials = true;
            portEXIT_CRITICAL(&spinlock);
        } else if (c->getUUID().equals(BLEUUID(SCAN_TRIGGER_CHAR_UUID))) {
            hasPendingScan = true;
        }
    }

private:
    Preferences preferences;
    portMUX_TYPE spinlock = portMUX_INITIALIZER_UNLOCKED;

    bool provisioningActive = false;
    bool deviceConnected = false;
    bool connectingToWifi = false;
    unsigned long wifiConnectStart = 0;

    volatile bool hasPendingScan = false;
    volatile bool hasPendingCredentials = false;
    String pendingCredentialsValue;

    BLECharacteristic *statusCharacteristic = nullptr;
    BLECharacteristic *networksCharacteristic = nullptr;
    BLEServer *server = nullptr;

    void checkResetButton() {
        if (digitalRead(RESET_BUTTON_PIN) == LOW) {
            unsigned long pressStart = millis();
            bool confirmed = true;

            Serial.println(F("\nReset button pressed! Hold for 5 seconds..."));

            while (millis() - pressStart < HOLD_TIME_MS) {
                digitalWrite(ONBOARD_LED_PIN, (millis() / 100) % 2 == 0 ? HIGH : LOW);

                if (digitalRead(RESET_BUTTON_PIN) == HIGH) {
                    Serial.println(F("Reset cancelled."));
                    digitalWrite(ONBOARD_LED_PIN, LOW);
                    confirmed = false;
                    break;
                }
                delay(10);
            }

            if (confirmed) {
                clearCredentialsAndReset();
            }
        }
    }

    void connectToSavedNetwork(const String &ssid) {
        preferences.begin("wifi-config", true);
        String password = preferences.getString("password", "");
        preferences.end();

        Serial.printf("Connecting to saved network: %s\n", ssid.c_str());

        WiFi.mode(WIFI_STA);
        WiFi.begin(ssid.c_str(), password.c_str());

        connectingToWifi = true;
        wifiConnectStart = millis();
    }

    void handleWifiConnectionState() {
        if (!connectingToWifi) return;

        if (WiFi.status() == WL_CONNECTED) {
            connectingToWifi = false;
            Serial.printf("\nConnected. IP: %s\n", WiFi.localIP().toString().c_str());
            if (provisioningActive) {
                reportStatus("connected");
                delay(500);
                ESP.restart();
            }
        } else if (millis() - wifiConnectStart >= 15000) {
            connectingToWifi = false;
            Serial.println(F("\nFailed to connect to Wi-Fi."));
            if (provisioningActive) {
                reportStatus("connect_failed");
                WiFi.disconnect(true);
            } else {
                startBleProvisioning();
            }
        }
    }

    void startBleProvisioning() {
        if (provisioningActive) return;

        provisioningActive = true;
        WiFi.mode(WIFI_STA);

        uint8_t mac[6];
        WiFi.macAddress(mac);
        char deviceName[32];
        snprintf(deviceName, sizeof(deviceName), "SmartNest-%02X%02X", mac[4], mac[5]);

        BLEDevice::init(deviceName);
        BLEDevice::setMTU(512);

        server = BLEDevice::createServer();
        server->setCallbacks(this);

        BLEService *service = server->createService(SERVICE_UUID);

        BLECharacteristic *credentialsChar = service->createCharacteristic(CREDENTIALS_CHAR_UUID, BLECharacteristic::PROPERTY_WRITE);
        credentialsChar->setCallbacks(this);

        BLECharacteristic *scanTriggerChar = service->createCharacteristic(SCAN_TRIGGER_CHAR_UUID, BLECharacteristic::PROPERTY_WRITE);
        scanTriggerChar->setCallbacks(this);

        networksCharacteristic = service->createCharacteristic(NETWORKS_CHAR_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
        networksCharacteristic->addDescriptor(new BLE2902());
        networksCharacteristic->setValue("{\"networks\":[]}");

        statusCharacteristic = service->createCharacteristic(STATUS_CHAR_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
        statusCharacteristic->addDescriptor(new BLE2902());
        statusCharacteristic->setValue("waiting_for_credentials");

        service->start();

        BLEAdvertising *advertising = BLEDevice::getAdvertising();
        advertising->addServiceUUID(SERVICE_UUID);
        advertising->setScanResponse(true);
        BLEDevice::startAdvertising();

        Serial.printf("BLE provisioning started. Advertising as: %s\n", deviceName);
    }

    void stopBleProvisioning() {
        if (!provisioningActive) return;
        BLEDevice::deinit(true);
        provisioningActive = false;
    }

    void processCredentials(const String &value) {
        // Expected payload format: ssid|password|ownerUid|macAddress
        int first = value.indexOf('|');
        int second = (first != -1) ? value.indexOf('|', first + 1) : -1;
        int third = (second != -1) ? value.indexOf('|', second + 1) : -1;

        if (first == -1 || second == -1 || third == -1) {
            reportStatus("error_invalid_format");
            return;
        }

        String ssid = value.substring(0, first);
        String password = value.substring(first + 1, second);
        String owner = value.substring(second + 1, third);
        String mac = value.substring(third + 1);

        ssid.trim();
        owner.trim();
        mac.trim();

        if (ssid.length() == 0 || owner.length() == 0 || mac.length() == 0) {
            reportStatus("error_invalid_format");
            return;
        }

        preferences.begin("wifi-config", false);
        preferences.putString("ssid", ssid);
        preferences.putString("password", password);
        preferences.putString("owner", owner);
        preferences.putString("device_mac", mac); // Store authoritative MAC address
        preferences.end();

        reportStatus("connecting");

        WiFi.mode(WIFI_STA);
        WiFi.begin(ssid.c_str(), password.c_str());

        connectingToWifi = true;
        wifiConnectStart = millis();
    }

    void performScan() {
        reportStatus("scanning");

        WiFi.mode(WIFI_STA);
        WiFi.disconnect();
        delay(100);

        int networkCount = WiFi.scanNetworks(false, true);

        JsonDocument doc;
        JsonArray networks = doc["networks"].to<JsonArray>();

        if (networkCount > 0) {
            int reportedCount = min(networkCount, MAX_REPORTED_NETWORKS);
            for (int i = 0; i < reportedCount; i++) {
                JsonObject network = networks.add<JsonObject>();
                network["ssid"] = WiFi.SSID(i);
                network["rssi"] = WiFi.RSSI(i);
                network["secure"] = WiFi.encryptionType(i) != WIFI_AUTH_OPEN;
            }
        }

        String payload;
        serializeJson(doc, payload);

        if (networksCharacteristic != nullptr) {
            networksCharacteristic->setValue(payload.c_str());
            networksCharacteristic->notify();
        }

        reportStatus("waiting_for_credentials");
        WiFi.scanDelete();
    }

    void reportStatus(const char *status) {
        if (statusCharacteristic != nullptr && deviceConnected) {
            statusCharacteristic->setValue(status);
            statusCharacteristic->notify();
        }
        Serial.printf("Provisioning status: %s\n", status);
    }
};

#endif