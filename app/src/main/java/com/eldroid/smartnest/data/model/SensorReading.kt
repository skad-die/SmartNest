package com.eldroid.smartnest.data.model

data class SensorReading(
    var temperatureCelsius: Double = 0.0,
    var humidityPercent: Double = 0.0,
    var airQualityPpm: Double = 0.0,
    var trayStatus: String = "Unknown", // "Clean" or "Needs Cleaning"
    var timestamp: Long = 0L,
    var deviceOnline: Boolean = false
)