package com.eldroid.smartnest.ui.settings

interface SettingsContract {

    interface View {
        fun navigateToChangePassword()
        fun navigateBack()
        fun showAlertsEnabled(enabled: Boolean)
        fun startAlertMonitoring()
        fun stopAlertMonitoring()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun onChangePasswordClicked()
        fun onBackClicked()
        fun onAlertsToggled(enabled: Boolean)
    }
}