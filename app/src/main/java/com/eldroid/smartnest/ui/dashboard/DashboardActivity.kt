package com.eldroid.smartnest.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eldroid.smartnest.R
import com.eldroid.smartnest.data.model.SensorReading
import com.eldroid.smartnest.data.model.SmartNestDevice
import com.eldroid.smartnest.ui.login.LoginActivity
import com.eldroid.smartnest.ui.settings.SettingsActivity
import com.eldroid.smartnest.ui.setupdevice.ProvisioningBottomSheet
import com.eldroid.smartnest.ui.setupdevice.SetupDeviceActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity(), DashboardContract.View {

    private val presenter: DashboardContract.Presenter = DashboardPresenter()

    private val deviceAdapter = DashboardDeviceAdapter(
        onItemClick = { device ->
            openCageOverview(device)
        },
        onUnpairClick = { device ->
            confirmUnpairDevice(device)
        }
    )

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var contentScroll: View
    private lateinit var layoutEmptyDashboard: View
    private lateinit var rvDevices: RecyclerView
    private lateinit var fabAddDevice: FloatingActionButton
    private lateinit var tvDrawerUserEmail: TextView
    private lateinit var tvDrawerUserName: TextView

    private var currentDialog: AlertDialog? = null
    private lateinit var tvDialogTitle: TextView
    private lateinit var tvDialogLastUpdated: TextView
    private lateinit var tvDialogTemperature: TextView
    private lateinit var tvDialogHumidity: TextView
    private lateinit var tvDialogAirQuality: TextView
    private lateinit var tvDialogTrayStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        bindViews()
        setupInsets()
        setupDrawerAndToolbar()
        setupDeviceList()
        setupBackPressHandler()

        presenter.attachView(this)
    }

    private fun bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        toolbar = findViewById(R.id.toolbar)
        contentScroll = findViewById(R.id.contentScroll)
        layoutEmptyDashboard = findViewById(R.id.layoutEmptyDashboard)
        rvDevices = findViewById(R.id.rvDevices)
        fabAddDevice = findViewById(R.id.fabAddDevice)

        val headerView = navView.getHeaderView(0)
        tvDrawerUserEmail = headerView.findViewById(R.id.tvDrawerUserEmail)
        tvDrawerUserName = headerView.findViewById(R.id.tvDrawerUserName)
    }

    private fun setupInsets() {
        val drawerProfileCard = navView.getHeaderView(0).findViewById<View>(R.id.drawerProfileCard)
        val basePaddingLeft = drawerProfileCard.paddingLeft
        val basePaddingTop = drawerProfileCard.paddingTop
        val basePaddingRight = drawerProfileCard.paddingRight
        val basePaddingBottom = drawerProfileCard.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(drawerProfileCard) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(basePaddingLeft, basePaddingTop + systemBars.top, basePaddingRight, basePaddingBottom)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = systemBars.top
            view.layoutParams = params
            insets
        }

        val fabBaseBottomMargin = (fabAddDevice.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(fabAddDevice) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = fabBaseBottomMargin + systemBars.bottom
            view.layoutParams = params
            insets
        }
    }

    private fun setupDrawerAndToolbar() {
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                presenter.onDrawerOpened()
            }
        })

        val drawerProfileCard = navView.getHeaderView(0).findViewById<View>(R.id.drawerProfileCard)
        drawerProfileCard.setOnClickListener {
            Toast.makeText(this, "User profile coming soon", Toast.LENGTH_SHORT).show()
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_setup_device -> startActivity(Intent(this, SetupDeviceActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_logout -> presenter.onLogoutClicked()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupDeviceList() {
        rvDevices.layoutManager = LinearLayoutManager(this)
        rvDevices.adapter = deviceAdapter

        fabAddDevice.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(ProvisioningBottomSheet.TAG) == null) {
                ProvisioningBottomSheet().show(supportFragmentManager, ProvisioningBottomSheet.TAG)
            }
        }
    }

    private fun openCageOverview(device: DashboardDevice) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_cage_overview, null)

        tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle)
        tvDialogLastUpdated = dialogView.findViewById(R.id.tvDialogLastUpdated)
        tvDialogTemperature = dialogView.findViewById(R.id.tvDialogTemperature)
        tvDialogHumidity = dialogView.findViewById(R.id.tvDialogHumidity)
        tvDialogAirQuality = dialogView.findViewById(R.id.tvDialogAirQuality)
        tvDialogTrayStatus = dialogView.findViewById(R.id.tvDialogTrayStatus)

        tvDialogTitle.text = device.device.displayName

        currentDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        currentDialog?.setOnDismissListener {
            currentDialog = null
        }

        currentDialog?.show()
    }

    private fun confirmUnpairDevice(device: SmartNestDevice) {
        AlertDialog.Builder(this)
            .setTitle("Unpair Device")
            .setMessage("Are you sure you want to unpair ${device.displayName}? This will reset its Wi-Fi configuration.")
            .setPositiveButton("Unpair") { _, _ ->
                presenter.unpairDevice(device)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        presenter.startListening()
    }

    override fun onStop() {
        presenter.stopListening()
        super.onStop()
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun showSensorReading(reading: SensorReading) {
        if (currentDialog?.isShowing == true) {
            tvDialogTemperature.text = getString(R.string.temperature_value, reading.temperatureCelsius)
            tvDialogHumidity.text = getString(R.string.humidity_value, reading.humidityPercent)
            tvDialogAirQuality.text = getString(R.string.air_quality_value, reading.airQualityPpm)
            tvDialogTrayStatus.text = reading.trayStatus

            val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            tvDialogLastUpdated.text = getString(R.string.last_updated, formatter.format(Date(reading.timestamp)))
        }
    }

    override fun showNoSensorData() {
        // Handle if needed
    }

    override fun showDevices(devices: List<DashboardDevice>) {
        layoutEmptyDashboard.visibility = View.GONE
        contentScroll.visibility = View.VISIBLE
        deviceAdapter.submitList(devices)
    }

    override fun showEmptyState() {
        contentScroll.visibility = View.GONE
        layoutEmptyDashboard.visibility = View.VISIBLE
        deviceAdapter.submitList(emptyList())
    }

    override fun showUnpairSuccess(deviceName: String) {
        Toast.makeText(this, "$deviceName unpaired successfully", Toast.LENGTH_SHORT).show()
    }

    override fun showLoadError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showUserEmail(email: String) {
        tvDrawerUserEmail.text = email
    }

    override fun showUserName(name: String) {
        tvDrawerUserName.text = name
    }

    override fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}