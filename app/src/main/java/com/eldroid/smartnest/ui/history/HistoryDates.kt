package com.eldroid.smartnest.ui.history

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Date helpers shared by the history list adapters. */
internal object HistoryDates {

    fun time(timestamp: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))

    fun day(timestamp: Long): String =
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(timestamp))

    fun isSameDay(first: Long, second: Long): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = first }
        val b = Calendar.getInstance().apply { timeInMillis = second }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    /** For a newest-first list of timestamps, marks the entries that start a new day. */
    fun startsNewDay(timestamps: List<Long>): List<Boolean> =
        timestamps.mapIndexed { index, timestamp ->
            index == 0 || !isSameDay(timestamps[index - 1], timestamp)
        }
}
