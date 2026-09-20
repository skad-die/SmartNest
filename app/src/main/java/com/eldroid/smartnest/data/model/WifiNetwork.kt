package com.eldroid.smartnest.data.model

data class WifiNetwork(
    val ssid: String,
    val rssi: Int,
    val secure: Boolean
)