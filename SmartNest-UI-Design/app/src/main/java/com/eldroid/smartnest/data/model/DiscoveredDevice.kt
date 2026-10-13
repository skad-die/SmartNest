package com.eldroid.smartnest.data.model

import android.bluetooth.BluetoothDevice

/**
 * Represents a BLE SmartNest hub discovered during local device scanning.
 */
data class DiscoveredDevice(
    val name: String,
    val address: String,
    val bluetoothDevice: BluetoothDevice? = null
)