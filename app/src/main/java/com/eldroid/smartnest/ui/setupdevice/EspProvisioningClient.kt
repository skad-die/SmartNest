package com.eldroid.smartnest.ui.setupdevice

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import java.util.ArrayDeque
import java.util.UUID

class EspBleProvisioningClient(private val context: Context) {

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val CREDENTIALS_CHAR_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        val STATUS_CHAR_UUID: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        val SCAN_TRIGGER_CHAR_UUID: UUID = UUID.fromString("6e400004-b5a3-f393-e0a9-e50e24dcca9e")
        val NETWORKS_CHAR_UUID: UUID = UUID.fromString("6e400005-b5a3-f393-e0a9-e50e24dcca9e")
        private const val SCAN_TIMEOUT_MS = 15_000L
    }

    interface Listener {
        fun onDeviceFound(device: BluetoothDevice, name: String)
        fun onScanFinished()
        fun onConnected()
        fun onDisconnectedDuringProvisioning()
        fun onNetworksReceived(networksJson: String)
        fun onStatusUpdate(status: String)
        fun onError(message: String)
    }

    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private var listener: Listener? = null
    private val handler = Handler(Looper.getMainLooper())
    private var scanning = false
    private var targetMacAddress: String? = null

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    @SuppressLint("MissingPermission")
    fun startScan(bluetoothLeScanner: BluetoothLeScanner, targetMacAddress: String? = null) {
        if (scanning) return
        scanner = bluetoothLeScanner
        scanning = true
        this.targetMacAddress = targetMacAddress

        // Broad scan filter configuration to catch all variants of advertising names
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // Passing empty list or no strict service filter ensures compatibility if firmware advertises manufacturer data instead
        bluetoothLeScanner.startScan(null, settings, scanCallback)

        handler.postDelayed({ stopScan() }, SCAN_TIMEOUT_MS)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!scanning) return
        scanning = false
        try {
            scanner?.stopScan(scanCallback)
        } catch (_: Exception) {}
        listener?.onScanFinished()
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name = result.device.name ?: result.scanRecord?.deviceName ?: return
            if (!name.startsWith("SmartNest")) return

            val target = targetMacAddress
            if (target != null && !result.device.address.equals(target, ignoreCase = true)) {
                // Re-provisioning a specific device — ignore any other SmartNest unit nearby.
                return
            }

            stopScan()
            listener?.onDeviceFound(result.device, name)
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
            listener?.onError("Bluetooth scan failed (code $errorCode).")
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
    }

    @SuppressLint("MissingPermission")
    fun triggerNetworkScan() {
        val service = gatt?.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(SCAN_TRIGGER_CHAR_UUID)

        if (characteristic == null) {
            listener?.onError("Provisioning service or scan characteristic missing on device.")
            return
        }

        val payload = byteArrayOf(1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt?.writeCharacteristic(characteristic, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = payload
            @Suppress("DEPRECATION")
            gatt?.writeCharacteristic(characteristic)
        }
    }

    @SuppressLint("MissingPermission")
    fun sendCredentials(ssid: String, password: String) {
        val service = gatt?.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CREDENTIALS_CHAR_UUID)

        if (characteristic == null) {
            listener?.onError("Credentials characteristic not found on device.")
            return
        }

        val payload = "$ssid|$password".toByteArray(Charsets.UTF_8)
        val queued = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt?.writeCharacteristic(characteristic, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = payload
            @Suppress("DEPRECATION")
            gatt?.writeCharacteristic(characteristic) ?: false
        }

        if (!queued) {
            listener?.onError("Failed to send Wi-Fi credentials — connection may have dropped.")
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (_: Exception) {}
        gatt = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        private val notificationQueue = ArrayDeque<UUID>()

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                handler.post { listener?.onError("GATT connection error status: $status") }
                disconnect()
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                handler.postDelayed({ g.discoverServices() }, 600)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                handler.post { listener?.onDisconnectedDuringProvisioning() }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    g.requestMtu(512)
                } else {
                    proceedWithNotifications(g)
                }
            } else {
                handler.post { listener?.onError("Service discovery failed.") }
            }
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            proceedWithNotifications(g)
        }

        private fun proceedWithNotifications(g: BluetoothGatt) {
            notificationQueue.clear()
            notificationQueue.add(STATUS_CHAR_UUID)
            notificationQueue.add(NETWORKS_CHAR_UUID)
            processNextNotificationInQueue(g)
        }

        @SuppressLint("MissingPermission")
        private fun processNextNotificationInQueue(g: BluetoothGatt) {
            val nextUuid = notificationQueue.pollFirst()
            if (nextUuid == null) {
                handler.post { listener?.onConnected() }
                return
            }
            enableNotifications(g, nextUuid)
        }

        @SuppressLint("MissingPermission")
        private fun enableNotifications(g: BluetoothGatt, characteristicUuid: UUID) {
            val service = g.getService(SERVICE_UUID)
            if (service == null) {
                handler.post { listener?.onError("SmartNest BLE Service UUID not found on peripheral.") }
                return
            }
            val characteristic = service.getCharacteristic(characteristicUuid) ?: return
            g.setCharacteristicNotification(characteristic, true)

            val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            val descriptor = characteristic.getDescriptor(cccdUuid) ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                g.writeDescriptor(descriptor)
            }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            processNextNotificationInQueue(g)
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            handleCharacteristicChanged(characteristic.uuid, characteristic.value)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            handleCharacteristicChanged(characteristic.uuid, value)
        }

        private fun handleCharacteristicChanged(uuid: UUID, value: ByteArray?) {
            if (value == null) return
            when (uuid) {
                STATUS_CHAR_UUID -> {
                    val statusStr = String(value, Charsets.UTF_8)
                    handler.post { listener?.onStatusUpdate(statusStr) }
                }
                NETWORKS_CHAR_UUID -> {
                    val json = String(value, Charsets.UTF_8)
                    handler.post { listener?.onNetworksReceived(json) }
                }
            }
        }
    }
}