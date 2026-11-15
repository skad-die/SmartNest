package com.eldroid.smartnest.ui.history

import com.eldroid.smartnest.data.model.AlertEvent
import com.eldroid.smartnest.data.model.SensorReading

interface HistoryContract {

    enum class Tab { READINGS, ALERTS }

    enum class Range(val durationMs: Long) {
        DAY(24L * 60 * 60 * 1000),
        WEEK(7L * 24 * 60 * 60 * 1000)
    }

    interface View {
        fun showLoading()
        fun showReadings(readings: List<SensorReading>)
        fun showAlerts(alerts: List<AlertEvent>)
        fun showEmpty(tab: Tab)
        fun showError(message: String)
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun start(macKey: String, initialTab: Tab)
        fun onTabSelected(tab: Tab)
        fun onRangeSelected(range: Range)
    }
}
