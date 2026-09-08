package com.eldroid.smartnest.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.eldroid.smartnest.R
import com.eldroid.smartnest.data.model.SensorReading
import com.eldroid.smartnest.ui.login.LoginActivity
import com.eldroid.smartnest.ui.settings.SettingsActivity
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity(), DashboardContract.View {

    private val presenter: DashboardContract.Presenter = DashboardPresenter()

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var tvTemperature: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvAirQuality: TextView
    private lateinit var tvTrayStatus: TextView
    private lateinit var tvDeviceStatus: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var tvDrawerUserEmail: TextView
    private lateinit var tvDrawerUserName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        toolbar = findViewById(R.id.toolbar)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvAirQuality = findViewById(R.id.tvAirQuality)
        tvTrayStatus = findViewById(R.id.tvTrayStatus)
        tvDeviceStatus = findViewById(R.id.tvDeviceStatus)
        tvLastUpdated = findViewById(R.id.tvLastUpdated)
        tvDrawerUserEmail = navView.getHeaderView(0).findViewById(R.id.tvDrawerUserEmail)
        tvDrawerUserName = navView.getHeaderView(0).findViewById(R.id.tvDrawerUserName)
        val drawerProfileCard = navView.getHeaderView(0).findViewById<android.view.View>(R.id.drawerProfileCard)
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
            val params = view.layoutParams as android.view.ViewGroup.MarginLayoutParams
            params.topMargin = systemBars.top
            view.layoutParams = params
            insets
        }

        presenter.attachView(this)
        presenter.onDrawerOpened() // populate name/email immediately, not only on open

        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: android.view.View) {
                presenter.onDrawerOpened()
            }
        })

        drawerProfileCard.setOnClickListener {
            // User profile screen not built yet.
            Toast.makeText(this, "User profile coming soon", Toast.LENGTH_SHORT).show()
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                R.id.nav_logout -> {
                    presenter.onLogoutClicked()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
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

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun showSensorReading(reading: SensorReading) {
        tvTemperature.text = getString(R.string.temperature_value, reading.temperatureCelsius)
        tvHumidity.text = getString(R.string.humidity_value, reading.humidityPercent)
        tvAirQuality.text = getString(R.string.air_quality_value, reading.airQualityPpm)
        tvTrayStatus.text = reading.trayStatus
        tvDeviceStatus.text = getString(R.string.device_online)

        val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        tvLastUpdated.text = getString(
            R.string.last_updated,
            formatter.format(Date(reading.timestamp))
        )
    }

    override fun showDeviceOffline() {
        tvDeviceStatus.text = getString(R.string.device_offline)
        tvTemperature.text = getString(R.string.placeholder_dash)
        tvHumidity.text = getString(R.string.placeholder_dash)
        tvAirQuality.text = getString(R.string.placeholder_dash)
        tvTrayStatus.text = getString(R.string.placeholder_dash)
    }

    override fun showLoadError(message: String) {
        tvDeviceStatus.text = message
        tvTemperature.text = getString(R.string.placeholder_dash)
        tvHumidity.text = getString(R.string.placeholder_dash)
        tvAirQuality.text = getString(R.string.placeholder_dash)
        tvTrayStatus.text = getString(R.string.placeholder_dash)
    }

    override fun showUserEmail(email: String) {
        tvDrawerUserEmail.text = email
    }

    override fun showUserName(name: String) {
        tvDrawerUserName.text = name
    }

    override fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}