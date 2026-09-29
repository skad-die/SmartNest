package com.eldroid.smartnest.ui.setupdevice

import com.eldroid.smartnest.data.model.DiscoveredDevice
import com.eldroid.smartnest.data.model.SmartNestDevice
import com.eldroid.smartnest.data.model.WifiNetwork

interface SetupDeviceContract {

    interface View {
        fun showConnectedDevices(devices: List<SmartNestDevice>)
        fun showRegistryError(message: String)
        fun showSearchingForDevice()
        fun showDeviceFound(deviceName: String)
        fun showDeviceSearchError(message: String)

        /** Only called when the presenter runs in picker mode. Default no-op keeps SetupDeviceActivity compiling. */
        fun showDiscoveredDevices(devices: List<DiscoveredDevice>) {}

        fun showScanningNetworks()
        fun showNetworks(networks: List<WifiNetwork>)
        fun showNetworkScanError(message: String)
        fun showConfiguring()
        fun showConfigureError(message: String)
        fun showConfigureSuccess()

        fun showDeviceRegistryView()
        fun showProvisioningView()
        fun clearError()
        fun requestBluetoothPermissions()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun onScreenOpened()
        fun onAddDeviceClicked()
        fun onPermissionsGranted()
        fun onRescanNetworksClicked()
        fun onConnectClicked(ssid: String, password: String)
        fun onChangeWifiClicked(device: SmartNestDevice)
        fun onScreenClosed()
        fun onUnpairClicked(device: SmartNestDevice)
        fun onDeviceSelected(device: DiscoveredDevice)
    }
}