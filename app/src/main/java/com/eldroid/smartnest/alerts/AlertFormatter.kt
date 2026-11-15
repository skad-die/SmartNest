package com.eldroid.smartnest.alerts

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.eldroid.smartnest.R
import com.eldroid.smartnest.data.model.AlertEvent

/** Turns an [AlertEvent] into the text, icon and colour shown in notifications and the history list. */
object AlertFormatter {

    fun title(context: Context, event: AlertEvent): String = context.getString(
        when (event.type) {
            AlertEvent.TYPE_HIGH_TEMP -> R.string.alert_high_temp_title
            AlertEvent.TYPE_LOW_HUMIDITY -> R.string.alert_low_humidity_title
            AlertEvent.TYPE_HIGH_HUMIDITY -> R.string.alert_high_humidity_title
            AlertEvent.TYPE_POOR_AIR -> R.string.alert_poor_air_title
            AlertEvent.TYPE_TRAY_DIRTY -> R.string.alert_tray_dirty_title
            else -> R.string.alert_unknown_title
        }
    )

    fun message(context: Context, event: AlertEvent): String = when (event.type) {
        AlertEvent.TYPE_HIGH_TEMP ->
            context.getString(R.string.alert_high_temp_message, event.value)

        AlertEvent.TYPE_LOW_HUMIDITY ->
            context.getString(R.string.alert_low_humidity_message, event.value)

        AlertEvent.TYPE_HIGH_HUMIDITY ->
            context.getString(R.string.alert_high_humidity_message, event.value)

        AlertEvent.TYPE_POOR_AIR ->
            context.getString(R.string.alert_poor_air_message, event.value)

        AlertEvent.TYPE_TRAY_DIRTY -> context.getString(R.string.alert_tray_dirty_message)
        else -> context.getString(R.string.alert_unknown_message)
    }

    @DrawableRes
    fun icon(event: AlertEvent): Int = when (event.type) {
        AlertEvent.TYPE_HIGH_TEMP -> R.drawable.ic_thermometer
        AlertEvent.TYPE_LOW_HUMIDITY, AlertEvent.TYPE_HIGH_HUMIDITY -> R.drawable.ic_humidity
        AlertEvent.TYPE_POOR_AIR -> R.drawable.ic_air_quality
        AlertEvent.TYPE_TRAY_DIRTY -> R.drawable.ic_tray
        else -> R.drawable.ic_notification_alert
    }

    @ColorRes
    fun color(event: AlertEvent): Int = when (event.type) {
        AlertEvent.TYPE_HIGH_TEMP -> R.color.sensor_temp_accent
        AlertEvent.TYPE_LOW_HUMIDITY, AlertEvent.TYPE_HIGH_HUMIDITY -> R.color.sensor_humidity_accent
        AlertEvent.TYPE_POOR_AIR -> R.color.sensor_air_accent
        AlertEvent.TYPE_TRAY_DIRTY -> R.color.sensor_tray_accent
        else -> R.color.smartnest_error
    }
}
