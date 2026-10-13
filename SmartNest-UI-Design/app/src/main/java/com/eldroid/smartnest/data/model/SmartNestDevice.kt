package com.eldroid.smartnest.data.model

data class SmartNestDevice(
    val macAddress: String = "",
    val displayName: String = "",
    val lastSsid: String = "",
    val lastProvisionedAt: Long = 0L
) {
    companion object {
        fun keyFor(macAddress: String): String =
            macAddress.replace(":", "-")
    }
}