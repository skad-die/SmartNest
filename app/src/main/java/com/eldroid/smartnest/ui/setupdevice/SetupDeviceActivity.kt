package com.eldroid.smartnest.ui.setupdevice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View as AndroidView
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eldroid.smartnest.R
import com.eldroid.smartnest.data.model.SmartNestDevice
import com.eldroid.smartnest.data.model.WifiNetwork

class SetupDeviceActivity : AppCompatActivity(), SetupDeviceContract.View {

    private val presenter: SetupDeviceContract.Presenter by lazy { SetupDevicePresenter(this) }
    private lateinit var networkAdapter: WifiNetworkAdapter
    private lateinit var connectedDeviceAdapter: ConnectedDeviceAdapter

    private lateinit var toolbar: Toolbar
    private lateinit var tvInstructions: TextView
    private lateinit var progressScanning: ProgressBar
    private lateinit var btnRescan: Button
    private lateinit var etSsid: EditText
    private lateinit var etWifiPassword: EditText
    private lateinit var tvConfigureError: TextView
    private lateinit var btnConnect: Button
    private lateinit var btnAddDevice: Button

    private lateinit var layoutDeviceRegistry: AndroidView
    private lateinit var layoutEmptyRegistry: AndroidView
    private lateinit var rvConnectedDevices: RecyclerView
    private lateinit var layoutProvisioningContainer: AndroidView
    private lateinit var layoutNetworkSelection: AndroidView
    private lateinit var layoutEmptyNetworks: AndroidView
    private lateinit var rvNetworks: RecyclerView
    private lateinit var btnRetryFromEmptyState: Button
    private lateinit var layoutPasswordEntry: AndroidView
    private lateinit var btnBackToNetworks: Button

    private val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            presenter.onPermissionsGranted()
        } else {
            showDeviceSearchError(getString(R.string.bluetooth_permission_denied))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setup_device)

        toolbar = findViewById(R.id.toolbar)
        tvInstructions = findViewById(R.id.tvInstructions)
        progressScanning = findViewById(R.id.progressScanning)
        btnRescan = findViewById(R.id.btnRescan)
        etSsid = findViewById(R.id.etSsid)
        etWifiPassword = findViewById(R.id.etWifiPassword)
        tvConfigureError = findViewById(R.id.tvConfigureError)
        btnConnect = findViewById(R.id.btnConnect)
        btnAddDevice = findViewById(R.id.btnAddDevice)

        layoutDeviceRegistry = findViewById(R.id.layoutDeviceRegistry)
        layoutEmptyRegistry = findViewById(R.id.layoutEmptyRegistry)
        rvConnectedDevices = findViewById(R.id.rvConnectedDevices)
        layoutProvisioningContainer = findViewById(R.id.layoutProvisioningContainer)
        layoutNetworkSelection = findViewById(R.id.layoutNetworkSelection)
        layoutPasswordEntry = findViewById(R.id.layoutPasswordEntry)
        btnBackToNetworks = findViewById(R.id.btnBackToNetworks)

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = view.layoutParams as android.view.ViewGroup.MarginLayoutParams
            params.topMargin = systemBars.top
            view.layoutParams = params
            insets
        }

        layoutEmptyNetworks = findViewById(R.id.layoutEmptyNetworks)
        rvNetworks = findViewById(R.id.rvNetworks)
        btnRetryFromEmptyState = findViewById(R.id.btnRetryFromEmptyState)

        networkAdapter = WifiNetworkAdapter { network -> onNetworkSelected(network) }
        rvNetworks.layoutManager = LinearLayoutManager(this)
        rvNetworks.adapter = networkAdapter

        btnRetryFromEmptyState.setOnClickListener { presenter.onRescanNetworksClicked() }

        connectedDeviceAdapter = ConnectedDeviceAdapter { device -> presenter.onChangeWifiClicked(device) }
        rvConnectedDevices.layoutManager = LinearLayoutManager(this)
        rvConnectedDevices.adapter = connectedDeviceAdapter

        presenter.attachView(this)

        toolbar.setNavigationOnClickListener { finish() }
        btnRescan.setOnClickListener { presenter.onRescanNetworksClicked() }

        btnAddDevice.setOnClickListener {
            presenter.onAddDeviceClicked()
        }

        btnConnect.setOnClickListener {
            presenter.onConnectClicked(
                etSsid.text.toString(),
                etWifiPassword.text.toString()
            )
        }

        btnBackToNetworks.setOnClickListener {
            showNetworkSelectionView()
        }
    }

    override fun onDestroy() {
        presenter.onScreenClosed()
        presenter.detachView()
        super.onDestroy()
    }

    private fun onNetworkSelected(network: WifiNetwork) {
        etSsid.setText(network.ssid)
        etWifiPassword.setText("")
        etWifiPassword.requestFocus()

        layoutNetworkSelection.visibility = AndroidView.GONE
        layoutPasswordEntry.visibility = AndroidView.VISIBLE

        tvInstructions.text = getString(R.string.enter_network_security_key)
        progressScanning.visibility = AndroidView.GONE
    }

    private fun showNetworkSelectionView() {
        layoutPasswordEntry.visibility = AndroidView.GONE
        layoutNetworkSelection.visibility = AndroidView.VISIBLE

        tvInstructions.text = getString(R.string.select_target_wifi)
        progressScanning.visibility = AndroidView.GONE
    }

    override fun requestBluetoothPermissions() {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            presenter.onPermissionsGranted()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    override fun showConnectedDevices(devices: List<SmartNestDevice>) {
        connectedDeviceAdapter.submitList(devices)
        layoutEmptyRegistry.visibility = if (devices.isEmpty()) AndroidView.VISIBLE else AndroidView.GONE
        rvConnectedDevices.visibility = if (devices.isEmpty()) AndroidView.GONE else AndroidView.VISIBLE
    }

    override fun showRegistryError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showDeviceRegistryView() {
        layoutProvisioningContainer.visibility = AndroidView.GONE
        layoutDeviceRegistry.visibility = AndroidView.VISIBLE
    }

    override fun showProvisioningView() {
        layoutPasswordEntry.visibility = AndroidView.GONE
        layoutNetworkSelection.visibility = AndroidView.VISIBLE
        tvConfigureError.text = ""

        layoutDeviceRegistry.visibility = AndroidView.GONE
        layoutProvisioningContainer.visibility = AndroidView.VISIBLE
    }

    override fun showSearchingForDevice() {
        tvInstructions.text = getString(R.string.searching_for_device)
        progressScanning.visibility = AndroidView.VISIBLE
    }

    override fun showDeviceFound(deviceName: String) {
        tvInstructions.text = getString(R.string.device_connected, deviceName)
        progressScanning.visibility = AndroidView.GONE
    }

    override fun showDeviceSearchError(message: String) {
        progressScanning.visibility = AndroidView.GONE
        tvInstructions.text = message
    }

    override fun showScanningNetworks() {
        tvInstructions.text = getString(R.string.scanning_local_networks)
        progressScanning.visibility = AndroidView.VISIBLE
    }

    override fun showNetworks(networks: List<WifiNetwork>) {
        progressScanning.visibility = AndroidView.GONE
        tvInstructions.text = getString(R.string.select_target_wifi)
        networkAdapter.submitList(networks)
        val isEmpty = networks.isEmpty()
        layoutEmptyNetworks.visibility = if (isEmpty) AndroidView.VISIBLE else AndroidView.GONE
        rvNetworks.visibility = if (isEmpty) AndroidView.GONE else AndroidView.VISIBLE
    }

    override fun showNetworkScanError(message: String) {
        progressScanning.visibility = AndroidView.GONE
        tvInstructions.text = message
    }

    override fun showConfiguring() {
        progressScanning.visibility = AndroidView.VISIBLE
        btnConnect.isEnabled = false
        tvInstructions.text = getString(R.string.updating_wifi_credentials)
    }

    override fun showConfigureError(message: String) {
        progressScanning.visibility = AndroidView.GONE
        btnConnect.isEnabled = true
        tvConfigureError.text = message
        tvInstructions.text = getString(R.string.connection_failed_try_again)
    }

    override fun showConfigureSuccess() {
        progressScanning.visibility = AndroidView.GONE
        btnConnect.isEnabled = true
        Toast.makeText(this, getString(R.string.wifi_updated_successfully), Toast.LENGTH_LONG).show()
        showDeviceRegistryView()
    }

    override fun clearError() {
        tvConfigureError.text = ""
    }
}