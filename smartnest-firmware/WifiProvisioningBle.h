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
#define MAX_REPORTED_NETWORKS 8

class WifiProvisioningBle {
public:
    void begin() {
        preferences.begin("wifi-config", false);
        String savedSsid = preferences.getString("ssid", "");

        if (savedSsid.length() > 0) {
            connectToSavedNetwork(savedSsid);
        } else {
            startBleProvisioning();
        }
    }

    void loop() {
        if (hasPendingScan) {
            hasPendingScan = false;
            performScan();
        }
        if (hasPendingCredentials) {
            hasPendingCredentials = false;
            handleCredentials(pendingCredentialsValue);
        }
    }

    bool isProvisioning() const {
        return provisioningActive;
    }

    bool isConnected() const {
        return WiFi.status() == WL_CONNECTED;
    }

private:
    Preferences preferences;
    bool provisioningActive = false;
    bool deviceConnected = false;

    volatile bool hasPendingScan = false;
    volatile bool hasPendingCredentials = false;
    String pendingCredentialsValue;

    BLECharacteristic *statusCharacteristic = nullptr;
    BLECharacteristic *networksCharacteristic = nullptr;
    BLEServer *server = nullptr;

    void connectToSavedNetwork(const String &ssid) {
        String password = preferences.getString("password", "");
        Serial.printf("Connecting to saved network: %s\n", ssid.c_str());

        WiFi.mode(WIFI_STA);
        WiFi.begin(ssid.c_str(), password.c_str());

        unsigned long startAttempt = millis();
        const unsigned long timeoutMs = 15000;

        while (WiFi.status() != WL_CONNECTED && millis() - startAttempt < timeoutMs) {
            delay(300);
            Serial.print(".");
        }

        if (WiFi.status() == WL_CONNECTED) {
            Serial.println();
            Serial.printf("Connected. IP: %s\n", WiFi.localIP().toString().c_str());
        } else {
            Serial.println();
            Serial.println("Failed to connect with saved credentials. Starting BLE provisioning.");
            startBleProvisioning();
        }
    }

    void startBleProvisioning() {
        provisioningActive = true;

        uint8_t mac[6];
        WiFi.macAddress(mac);
        char deviceName[32];
        snprintf(deviceName, sizeof(deviceName), "SmartNest-%02X%02X", mac[4], mac[5]);

        BLEDevice::init(deviceName);
        BLEDevice::setMTU(512); 

        server = BLEDevice::createServer();
        server->setCallbacks(new ServerCallbacks(this));

        BLEService *service = server->createService(SERVICE_UUID);

        BLECharacteristic *credentialsCharacteristic = service->createCharacteristic(
            CREDENTIALS_CHAR_UUID,
            BLECharacteristic::PROPERTY_WRITE
        );
        credentialsCharacteristic->setCallbacks(new CredentialsWriteCallback(this));

        BLECharacteristic *scanTriggerCharacteristic = service->createCharacteristic(
            SCAN_TRIGGER_CHAR_UUID,
            BLECharacteristic::PROPERTY_WRITE
        );
        scanTriggerCharacteristic->setCallbacks(new ScanTriggerWriteCallback(this));

        networksCharacteristic = service->createCharacteristic(
            NETWORKS_CHAR_UUID,
            BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY
        );
        networksCharacteristic->addDescriptor(new BLE2902());
        networksCharacteristic->setValue("{\"networks\":[]}");

        statusCharacteristic = service->createCharacteristic(
            STATUS_CHAR_UUID,
            BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY
        );
        statusCharacteristic->addDescriptor(new BLE2902());
        statusCharacteristic->setValue("waiting_for_credentials");

        service->start();

        BLEAdvertising *advertising = BLEDevice::getAdvertising();
        advertising->addServiceUUID(SERVICE_UUID);
        advertising->setScanResponse(true);
        BLEDevice::startAdvertising();

        Serial.printf("BLE provisioning started. Advertising as: %s\n", deviceName);
    }

    void onClientConnected() {
        deviceConnected = true;
        Serial.println("App connected via BLE.");
    }

    void onClientDisconnected() {
        deviceConnected = false;
        Serial.println("App disconnected from BLE. Restarting advertising...");
        delay(500);
        if (server != nullptr) {
            server->startAdvertising();
        }
    }

    void handleCredentials(const String &value) {
        int separatorPos = value.indexOf('|');
        if (separatorPos == -1) {
            reportStatus("error_invalid_format");
            return;
        }

        String ssid = value.substring(0, separatorPos);
        String password = value.substring(separatorPos + 1);

        if (ssid.length() == 0) {
            reportStatus("error_ssid_required");
            return;
        }

        reportStatus("connecting");

        WiFi.mode(WIFI_STA);
        WiFi.begin(ssid.c_str(), password.c_str());

        unsigned long start = millis();
        while (WiFi.status() != WL_CONNECTED && millis() - start < 15000) {
            delay(300);
        }

        if (WiFi.status() == WL_CONNECTED) {
            preferences.putString("ssid", ssid);
            preferences.putString("password", password);
            reportStatus("connected");
            delay(500); 
            ESP.restart();
        } else {
            reportStatus("connect_failed");
            WiFi.disconnect(true);
        }
    }

    void performScan() {
        reportStatus("scanning");

        WiFi.mode(WIFI_STA);
        WiFi.disconnect();
        delay(100);

        WiFi.scanNetworks(true, true); 

        unsigned long startScanTime = millis();
        while (WiFi.scanComplete() == WIFI_SCAN_RUNNING && millis() - startScanTime < 10000) {
            delay(200);
        }

        int networkCount = WiFi.scanComplete();
        Serial.printf("Scan finished. Found networks: %d\n", networkCount);

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
        Serial.printf("Payload size: %d bytes\n", payload.length());

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

    class ServerCallbacks : public BLEServerCallbacks {
    public:
        explicit ServerCallbacks(WifiProvisioningBle *owner) : owner(owner) {}

        void onConnect(BLEServer *pServer) override {
            owner->onClientConnected();
        }

        void onDisconnect(BLEServer *pServer) override {
            owner->onClientDisconnected();
        }

    private:
        WifiProvisioningBle *owner;
    };


    class CredentialsWriteCallback : public BLECharacteristicCallbacks {
    public:
        explicit CredentialsWriteCallback(WifiProvisioningBle *owner) : owner(owner) {}

        void onWrite(BLECharacteristic *characteristic) override {
            owner->pendingCredentialsValue = String(characteristic->getValue().c_str());
            owner->hasPendingCredentials = true;
        }

    private:
        WifiProvisioningBle *owner;
    };

    class ScanTriggerWriteCallback : public BLECharacteristicCallbacks {
    public:
        explicit ScanTriggerWriteCallback(WifiProvisioningBle *owner) : owner(owner) {}

        void onWrite(BLECharacteristic *characteristic) override {
            owner->hasPendingScan = true;
        }

    private:
        WifiProvisioningBle *owner;
    };
};

#endif