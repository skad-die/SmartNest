package com.eldroid.smartnest.ui.settings

import com.eldroid.smartnest.alerts.AlertPrefs

class SettingsPresenter(
    private val alertPrefs: AlertPrefs
) : SettingsContract.Presenter {

    private var view: SettingsContract.View? = null

    override fun attachView(view: SettingsContract.View) {
        this.view = view
        view.showAlertsEnabled(alertPrefs.alertsEnabled)
    }

    override fun detachView() {
        view = null
    }

    override fun onChangePasswordClicked() {
        view?.navigateToChangePassword()
    }

    override fun onBackClicked() {
        view?.navigateBack()
    }

    override fun onAlertsToggled(enabled: Boolean) {
        if (alertPrefs.alertsEnabled == enabled) return
        alertPrefs.alertsEnabled = enabled
        if (enabled) view?.startAlertMonitoring() else view?.stopAlertMonitoring()
    }
}