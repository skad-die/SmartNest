package com.eldroid.smartnest.ui.setupdevice

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import com.eldroid.smartnest.data.model.SmartNestDevice
import com.eldroid.smartnest.data.model.WifiNetwork
import com.eldroid.smartnest.data.repository.DeviceRegistryRepository
import com.google.firebase.database.ValueEventListener
import org.json.JSONObject

class SetupDevicePresenter(
    context: Context
) : SetupDeviceContract.Presenter, EspBleProvisioningClient.Listener {

    private val appContext = context.applicationContext
    private val bleClient = EspBleProvisioningClient(appContext)
    private val bluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val deviceRegistry = DeviceRegistryRepository()
    private var view: SetupDeviceContract.View? = null
    private var foundDevice: BluetoothDevice? = null
    private var connectedToDevice = false
    private var knownDevices: List<SmartNestDevice> = emptyList()
    private var registryListener: ValueEventListener? = null
    private var deviceBeingReprovisioned: SmartNestDevice? = null
    private var lastSubmittedSsid: String? = null
    private var provisioningResolved = false

    override fun attachView(view: SetupDeviceContract.View) {
        this.view = view
        bleClient.setListener(this)
        startObservingRegistry()
    }

    override fun detachView() {
        registryListener?.let { deviceRegistry.stopObserving(it) }
        registryListener = null
        bleClient.setListener(null)
        view = null
    }

    private fun startObservingRegistry() {
        registryListener = deviceRegistry.observeDevices(object : DeviceRegistryRepository.DeviceListListener {
            override fun onDevicesChanged(devices: List<SmartNestDevice>) {
                knownDevices = devices
                view?.showConnectedDevices(devices)
            }

            override fun onError(message: String) {
                view?.showRegistryError(message)
            }
        })
    }

    override fun onScreenOpened() {
        view?.requestBluetoothPermissions()
    }

    override fun onAddDeviceClicked() {
        deviceBeingReprovisioned = null
        view?.showProvisioningView()
        onScreenOpened()
    }

    override fun onChangeWifiClicked(device: SmartNestDevice) {
        deviceBeingReprovisioned = device
        view?.showProvisioningView()
        onScreenOpened()
    }

    override fun onPermissionsGranted() {
        startDeviceSearch()
    }

    override fun onScreenClosed() {
        bleClient.stopScan()
        bleClient.disconnect()
    }

    override fun onRescanNetworksClicked() {
        if (connectedToDevice) {
            view?.showScanningNetworks()
            bleClient.triggerNetworkScan()
        } else {
            startDeviceSearch()
        }
    }

    private fun startDeviceSearch() {
        foundDevice = null
        view?.showSearchingForDevice()
        val adapter = bluetoothManager.adapter
        if (adapter == null || !adapter.isEnabled) {
            view?.showDeviceSearchError("Bluetooth is turned off. Please enable it.")
            return
        }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            view?.showDeviceSearchError("BLE Scanner unavailable on this device.")
            return
        }

        val target = deviceBeingReprovisioned
        bleClient.startScan(scanner, targetMacAddress = target?.macAddress)
    }

    override fun onConnectClicked(ssid: String, password: String) {
        view?.clearError()

        val trimmedSsid = ssid.trim()
        val trimmedPassword = password.trim()

        if (trimmedSsid.isEmpty()) {
            view?.showConfigureError("Please select or enter a network name.")
            return
        }

        if (!connectedToDevice) {
            view?.showConfigureError("Not connected to the device yet. Please wait or try rescanning.")
            return
        }

        lastSubmittedSsid = trimmedSsid
        provisioningResolved = false
        view?.showConfiguring()
        bleClient.sendCredentials(trimmedSsid, trimmedPassword)
    }

    override fun onDeviceFound(device: BluetoothDevice, name: String) {
        foundDevice = device
        bleClient.stopScan()
        view?.showDeviceFound(name)
        bleClient.connect(device)
    }

    override fun onScanFinished() {
        if (foundDevice == null && !connectedToDevice) {
            view?.showDeviceSearchError("No SmartNest device found nearby. Ensure it's in pairing mode and tap Refresh.")
        }
    }

    override fun onConnected() {
        connectedToDevice = true
        view?.showScanningNetworks()
        bleClient.triggerNetworkScan()
    }

    override fun onDisconnectedDuringProvisioning() {
        connectedToDevice = false
        if (provisioningResolved) return
        provisioningResolved = true
        view?.showConfigureError(
            "Lost connection to the device before it confirmed the Wi-Fi update. " +
                    "It may still be trying to connect — check the device, or reconnect and try again."
        )
    }

    override fun onNetworksReceived(networksJson: String) {
        try {
            val json = JSONObject(networksJson)
            val networksArray = json.getJSONArray("networks")
            val networks = mutableListOf<WifiNetwork>()

            for (i in 0 until networksArray.length()) {
                val entry = networksArray.getJSONObject(i)
                networks.add(
                    WifiNetwork(
                        ssid = entry.getString("ssid"),
                        rssi = entry.getInt("rssi"),
                        secure = entry.getBoolean("secure")
                    )
                )
            }

            val deduped = networks
                .sortedByDescending { it.rssi }
                .distinctBy { it.ssid }

            view?.showNetworks(deduped)
        } catch (e: Exception) {
            view?.showNetworkScanError("Could not read the network list from the device.")
        }
    }

    override fun onStatusUpdate(status: String) {
        when (status) {
            "connecting" -> {
                view?.showConfiguring()
            }
            "connected" -> {
                provisioningResolved = true
                saveDeviceToRegistry()
                view?.showConfigureSuccess()
            }
            "connect_failed" -> {
                provisioningResolved = true
                view?.showConfigureError("Couldn't connect with that password. Please check it and try again.")
            }
            "error_invalid_format" -> {
                provisioningResolved = true
                view?.showConfigureError("Something went wrong sending credentials. Please try again.")
            }
            "error_ssid_required" -> {
                provisioningResolved = true
                view?.showConfigureError("Network name was empty. Please select a network and try again.")
            }
        }
    }

    private fun saveDeviceToRegistry() {
        val mac = foundDevice?.address ?: return
        val ssid = lastSubmittedSsid ?: return
        val existing = deviceBeingReprovisioned

        val device = SmartNestDevice(
            macAddress = mac,
            displayName = existing?.displayName?.takeIf { it.isNotBlank() }
                ?: foundDevice?.name
                ?: "SmartNest Hub",
            lastSsid = ssid,
            lastProvisionedAt = System.currentTimeMillis()
        )

        deviceRegistry.upsertDevice(device) { success, error ->
            if (!success) {
                view?.showRegistryError(error ?: "Couldn't save this device to your account.")
            }
        }
        deviceBeingReprovisioned = null
        lastSubmittedSsid = null
    }

    override fun onError(message: String) {
        view?.showConfigureError(message)
    }
}