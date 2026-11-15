package com.eldroid.smartnest.ui.history

import com.eldroid.smartnest.data.repository.HistoryRepository

class HistoryPresenter(
    private val repository: HistoryRepository = HistoryRepository()
) : HistoryContract.Presenter {

    private var view: HistoryContract.View? = null

    private var macKey: String = ""
    private var tab = HistoryContract.Tab.READINGS
    private var range = HistoryContract.Range.DAY

    // Identifies the latest request so a slow, superseded response is ignored.
    private var requestId = 0

    override fun attachView(view: HistoryContract.View) {
        this.view = view
    }

    override fun detachView() {
        view = null
    }

    override fun start(macKey: String, initialTab: HistoryContract.Tab) {
        this.macKey = macKey
        tab = initialTab
        repository.pruneOld(macKey)
        load()
    }

    override fun onTabSelected(tab: HistoryContract.Tab) {
        if (this.tab == tab) return
        this.tab = tab
        load()
    }

    override fun onRangeSelected(range: HistoryContract.Range) {
        if (this.range == range) return
        this.range = range
        load()
    }

    private fun load() {
        val id = ++requestId
        val requestedTab = tab
        val sinceMs = System.currentTimeMillis() - range.durationMs

        view?.showLoading()

        val onError: (String) -> Unit = { message ->
            if (id == requestId) view?.showError(message)
        }

        when (requestedTab) {
            HistoryContract.Tab.READINGS -> repository.loadReadings(macKey, sinceMs, { readings ->
                if (id == requestId) {
                    if (readings.isEmpty()) view?.showEmpty(requestedTab)
                    else view?.showReadings(readings)
                }
            }, onError)

            HistoryContract.Tab.ALERTS -> repository.loadAlerts(macKey, sinceMs, { alerts ->
                if (id == requestId) {
                    if (alerts.isEmpty()) view?.showEmpty(requestedTab)
                    else view?.showAlerts(alerts)
                }
            }, onError)
        }
    }
}
