package com.eldroid.smartnest.data.model

/**
 * One alert entry written by the device to /devices/{uid}/alerts when a sensor
 * condition turns on.
 */
data class AlertEvent(
    var macKey: String = "",
    var type: String = "",
    var value: Double = 0.0,
    var timestamp: Long = 0L
) {
    companion object {
        const val TYPE_HIGH_TEMP = "HIGH_TEMP"
        const val TYPE_LOW_HUMIDITY = "LOW_HUMIDITY"
        const val TYPE_HIGH_HUMIDITY = "HIGH_HUMIDITY"
        const val TYPE_POOR_AIR = "POOR_AIR"
        const val TYPE_TRAY_DIRTY = "TRAY_DIRTY"
    }
}
