package com.eldroid.smartnest.ui.setupdevice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eldroid.smartnest.R
import com.eldroid.smartnest.data.model.SmartNestDevice
import com.eldroid.smartnest.data.model.WifiNetwork
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputLayout

class ProvisioningBottomSheet : BottomSheetDialogFragment(), SetupDeviceContract.View {
    companion object {
        const val TAG = "ProvisioningBottomSheet"
    }

    private lateinit var presenter: SetupDeviceContract.Presenter
    private lateinit var networkAdapter: WifiNetworkAdapter

    private lateinit var scrollViewProvisioning: NestedScrollView
    private lateinit var cardDeviceConnection: MaterialCardView
    private lateinit var layoutSearchingStatus: View
    private lateinit var progressScanning: CircularProgressIndicator
    private lateinit var tvSearchingStatus: TextView
    private lateinit var btnRetryDeviceSearch: MaterialButton
    private lateinit var layoutConnectedDevice: View
    private lateinit var tvDeviceConnectedTitle: TextView
    private lateinit var btnChangeDevice: MaterialButton
    private lateinit var cardNetworkSelection: MaterialCardView
    private lateinit var btnRescanNetworks: MaterialButton
    private lateinit var layoutWifiScanningProgress: View
    private lateinit var tvWifiScanStatus: TextView
    private lateinit var layoutEmptyNetworks: View
    private lateinit var btnRetryFromEmptyState: MaterialButton
    private lateinit var rvNetworks: RecyclerView
    private lateinit var cardPasswordEntry: MaterialCardView
    private lateinit var etSsid: EditText
    private lateinit var tilWifiPassword: TextInputLayout
    private lateinit var etWifiPassword: EditText
    private lateinit var tvConfigureError: TextView
    private lateinit var btnConnect: MaterialButton
    private lateinit var progressConnecting: CircularProgressIndicator
    private lateinit var btnBackToNetworks: MaterialButton

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.sheet_provisioning, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        scrollViewProvisioning = view.findViewById(R.id.scrollViewProvisioning)
        cardDeviceConnection = view.findViewById(R.id.cardDeviceConnection)
        layoutSearchingStatus = view.findViewById(R.id.layoutSearchingStatus)
        progressScanning = view.findViewById(R.id.progressScanning)
        tvSearchingStatus = view.findViewById(R.id.tvSearchingStatus)
        btnRetryDeviceSearch = view.findViewById(R.id.btnRetryDeviceSearch)
        layoutConnectedDevice = view.findViewById(R.id.layoutConnectedDevice)
        tvDeviceConnectedTitle = view.findViewById(R.id.tvDeviceConnectedTitle)
        btnChangeDevice = view.findViewById(R.id.btnChangeDevice)
        cardNetworkSelection = view.findViewById(R.id.cardNetworkSelection)
        btnRescanNetworks = view.findViewById(R.id.btnRescanNetworks)
        layoutWifiScanningProgress = view.findViewById(R.id.layoutWifiScanningProgress)
        tvWifiScanStatus = view.findViewById(R.id.tvWifiScanStatus)
        layoutEmptyNetworks = view.findViewById(R.id.layoutEmptyNetworks)
        btnRetryFromEmptyState = view.findViewById(R.id.btnRetryFromEmptyState)
        rvNetworks = view.findViewById(R.id.rvNetworks)
        cardPasswordEntry = view.findViewById(R.id.cardPasswordEntry)
        etSsid = view.findViewById(R.id.etSsid)
        tilWifiPassword = view.findViewById(R.id.tilWifiPassword)
        etWifiPassword = view.findViewById(R.id.etWifiPassword)
        tvConfigureError = view.findViewById(R.id.tvConfigureError)
        btnConnect = view.findViewById(R.id.btnConnect)
        progressConnecting = view.findViewById(R.id.progressConnecting)
        btnBackToNetworks = view.findViewById(R.id.btnBackToNetworks)
        networkAdapter = WifiNetworkAdapter { network -> onNetworkSelected(network) }
        rvNetworks.layoutManager = LinearLayoutManager(requireContext())
        rvNetworks.adapter = networkAdapter
        btnRetryDeviceSearch.setOnClickListener { presenter.onAddDeviceClicked() }
        btnChangeDevice.setOnClickListener { presenter.onAddDeviceClicked() }
        btnRescanNetworks.setOnClickListener { presenter.onRescanNetworksClicked() }
        btnRetryFromEmptyState.setOnClickListener { presenter.onRescanNetworksClicked() }
        btnBackToNetworks.setOnClickListener { showNetworkSelectionView() }
        btnConnect.setOnClickListener {
            presenter.onConnectClicked(etSsid.text.toString(), etWifiPassword.text.toString())
        }
        etWifiPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollViewProvisioning.post {
                    scrollViewProvisioning.smoothScrollTo(0, cardPasswordEntry.top)
                }
            }
        }

        presenter = SetupDevicePresenter(requireContext())
        presenter.attachView(this)
        presenter.onAddDeviceClicked()
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onDestroyView() {
        presenter.onScreenClosed()
        presenter.detachView()
        super.onDestroyView()
    }

    private fun onNetworkSelected(network: WifiNetwork) {
        etSsid.setText(network.ssid)
        etWifiPassword.setText("")
        clearError()

        cardDeviceConnection.visibility = View.VISIBLE
        cardNetworkSelection.visibility = View.VISIBLE
        cardPasswordEntry.visibility = View.VISIBLE

        etWifiPassword.requestFocus()
        scrollViewProvisioning.post {
            scrollViewProvisioning.smoothScrollTo(0, cardPasswordEntry.top)
        }
    }

    private fun showNetworkSelectionView() {
        cardDeviceConnection.visibility = View.VISIBLE
        cardNetworkSelection.visibility = View.VISIBLE
        cardPasswordEntry.visibility = View.GONE
    }

    override fun requestBluetoothPermissions() {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) presenter.onPermissionsGranted() else permissionLauncher.launch(requiredPermissions)
    }

    override fun showProvisioningView() {
        showNetworkSelectionView()
        clearError()
    }

    override fun showDeviceRegistryView() {
        // Handle UI transition to device registry view if needed
    }

    override fun showSearchingForDevice() {
        cardDeviceConnection.visibility = View.VISIBLE
        cardNetworkSelection.visibility = View.GONE
        cardPasswordEntry.visibility = View.GONE

        layoutSearchingStatus.visibility = View.VISIBLE
        layoutConnectedDevice.visibility = View.GONE

        tvSearchingStatus.text = getString(R.string.searching_for_device)
        progressScanning.visibility = View.VISIBLE
        btnRetryDeviceSearch.visibility = View.GONE
    }

    override fun showDeviceFound(deviceName: String) {
        cardDeviceConnection.visibility = View.VISIBLE

        layoutSearchingStatus.visibility = View.GONE
        layoutConnectedDevice.visibility = View.VISIBLE

        tvDeviceConnectedTitle.text = deviceName
    }

    override fun showConnectedDevices(devices: List<SmartNestDevice>) {
        if (devices.isNotEmpty()) {
            showDeviceFound(devices.first().displayName)
        }
    }

    override fun showDeviceSearchError(message: String) {
        cardDeviceConnection.visibility = View.VISIBLE
        cardNetworkSelection.visibility = View.GONE
        cardPasswordEntry.visibility = View.GONE

        layoutSearchingStatus.visibility = View.VISIBLE
        layoutConnectedDevice.visibility = View.GONE

        progressScanning.visibility = View.GONE
        tvSearchingStatus.text = message
        btnRetryDeviceSearch.visibility = View.VISIBLE
    }

    override fun showScanningNetworks() {
        showNetworkSelectionView()

        layoutWifiScanningProgress.visibility = View.VISIBLE
        tvWifiScanStatus.text = getString(R.string.scanning_local_networks)
        layoutEmptyNetworks.visibility = View.GONE
        rvNetworks.visibility = View.GONE
    }

    override fun showNetworks(networks: List<WifiNetwork>) {
        showNetworkSelectionView()

        layoutWifiScanningProgress.visibility = View.GONE
        networkAdapter.submitList(networks)

        if (networks.isEmpty()) {
            layoutEmptyNetworks.visibility = View.VISIBLE
            rvNetworks.visibility = View.GONE
        } else {
            layoutEmptyNetworks.visibility = View.GONE
            rvNetworks.visibility = View.VISIBLE
        }
    }

    override fun showNetworkScanError(message: String) {
        showNetworkSelectionView()

        layoutWifiScanningProgress.visibility = View.VISIBLE
        tvWifiScanStatus.text = message
        layoutEmptyNetworks.visibility = View.GONE
        rvNetworks.visibility = View.GONE
    }

    override fun showConfiguring() {
        cardDeviceConnection.visibility = View.VISIBLE
        cardNetworkSelection.visibility = View.VISIBLE
        cardPasswordEntry.visibility = View.VISIBLE

        clearError()

        // Disable input wrapper & buttons during connection attempt
        btnConnect.text = ""
        btnConnect.isEnabled = false
        btnBackToNetworks.isEnabled = false
        tilWifiPassword.isEnabled = false
        progressConnecting.visibility = View.VISIBLE
    }

    override fun showConfigureError(message: String) {
        // Re-enable input wrapper & buttons on failure
        btnConnect.setText(R.string.update_wifi_button)
        btnConnect.isEnabled = true
        btnBackToNetworks.isEnabled = true
        tilWifiPassword.isEnabled = true
        progressConnecting.visibility = View.GONE

        // Display user-facing error text
        tvConfigureError.text = message
        tvConfigureError.visibility = View.VISIBLE
    }

    override fun showConfigureSuccess() {
        progressConnecting.visibility = View.GONE
        Toast.makeText(requireContext(), "Device Wi-Fi configured successfully!", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    override fun showRegistryError(message: String) {
        showConfigureError(message)
    }

    override fun clearError() {
        tvConfigureError.text = ""
        tvConfigureError.visibility = View.GONE
    }
}