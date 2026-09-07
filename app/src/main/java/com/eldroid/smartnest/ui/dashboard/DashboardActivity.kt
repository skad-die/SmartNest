package com.eldroid.smartnest.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.eldroid.smartnest.R
import com.eldroid.smartnest.data.model.SensorReading
import com.eldroid.smartnest.ui.login.LoginActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity(), DashboardContract.View {

    private val presenter: DashboardContract.Presenter = DashboardPresenter()

    private lateinit var tvTemperature: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvAirQuality: TextView
    private lateinit var tvTrayStatus: TextView
    private lateinit var tvDeviceStatus: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        tvTemperature = findViewById(R.id.tvTemperature)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvAirQuality = findViewById(R.id.tvAirQuality)
        tvTrayStatus = findViewById(R.id.tvTrayStatus)
        tvDeviceStatus = findViewById(R.id.tvDeviceStatus)
        tvLastUpdated = findViewById(R.id.tvLastUpdated)
        btnLogout = findViewById(R.id.btnLogout)

        presenter.attachView(this)

        btnLogout.setOnClickListener {
            presenter.onLogoutClicked()
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

    // ---- DashboardContract.View implementation ----

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
    }

    override fun showLoadError(message: String) {
        tvDeviceStatus.text = message
    }

    override fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}