package com.eldroid.smartnest.ui.dashboard

import com.eldroid.smartnest.data.model.SensorReading

interface DashboardContract {

    interface View {
        fun showSensorReading(reading: SensorReading)
        fun showDeviceOffline()
        fun showLoadError(message: String)
        fun showUserEmail(email: String)
        fun showUserName(name: String)
        fun navigateToLogin()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun startListening()
        fun stopListening()
        fun onDrawerOpened()
        fun onLogoutClicked()
    }
}