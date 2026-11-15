package com.eldroid.smartnest.alerts

import android.content.Context

/** Device-local settings for mobile alerts. */
class AlertPrefs(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("alert_prefs", Context.MODE_PRIVATE)

    var alertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /** True once the notification permission prompt has been shown, so it is not repeated. */
    var permissionRequested: Boolean
        get() = prefs.getBoolean(KEY_PERMISSION_REQUESTED, false)
        set(value) = prefs.edit().putBoolean(KEY_PERMISSION_REQUESTED, value).apply()

    /** Server timestamp of the newest alert already shown as a notification, or 0 if none yet. */
    var lastNotifiedTimestamp: Long
        get() = prefs.getLong(KEY_LAST_NOTIFIED, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_NOTIFIED, value).apply()

    private companion object {
        const val KEY_ENABLED = "alerts_enabled"
        const val KEY_PERMISSION_REQUESTED = "permission_requested"
        const val KEY_LAST_NOTIFIED = "last_notified_timestamp"
    }
}
