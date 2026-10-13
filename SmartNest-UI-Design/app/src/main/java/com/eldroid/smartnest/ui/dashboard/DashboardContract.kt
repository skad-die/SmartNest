package com.eldroid.smartnest.ui.dashboard

import com.eldroid.smartnest.data.model.SensorReading
import com.eldroid.smartnest.data.model.SmartNestDevice

data class DashboardDevice(
    val device: SmartNestDevice,
    val isOnline: Boolean
)

interface DashboardContract {

    interface View {
        fun showSensorReading(reading: SensorReading)
        fun showNoSensorData()
        fun showDevices(devices: List<DashboardDevice>)
        fun showEmptyState()
        fun showLoadError(message: String)
        fun showUserEmail(email: String)
        fun showUserName(name: String)
        fun navigateToLogin()
        fun showUnpairSuccess(deviceName: String)
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun startListening()
        fun stopListening()
        fun onDrawerOpened()
        fun onLogoutClicked()
        fun getSelectedDeviceUid(): String?
        fun unpairDevice(device: SmartNestDevice)
    }
}