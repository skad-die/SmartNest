package com.eldroid.smartnest.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.eldroid.smartnest.R
import com.eldroid.smartnest.alerts.AlertMonitorService
import com.eldroid.smartnest.alerts.AlertNotifier
import com.eldroid.smartnest.alerts.AlertPrefs
import com.eldroid.smartnest.ui.changepassword.ChangePasswordActivity
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity(), SettingsContract.View {

    private val alertPrefs by lazy { AlertPrefs(this) }
    private val presenter: SettingsContract.Presenter by lazy { SettingsPresenter(alertPrefs) }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, R.string.notification_permission_denied, Toast.LENGTH_LONG)
                    .show()
            }
        }

    private lateinit var toolbar: Toolbar
    private lateinit var rowChangePassword: LinearLayout
    private lateinit var switchMobileAlerts: MaterialSwitch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        toolbar = findViewById(R.id.toolbar)
        rowChangePassword = findViewById(R.id.rowChangePassword)
        switchMobileAlerts = findViewById(R.id.switchMobileAlerts)

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = view.layoutParams as android.view.ViewGroup.MarginLayoutParams
            params.topMargin = systemBars.top
            view.layoutParams = params
            insets
        }

        presenter.attachView(this)

        toolbar.setNavigationOnClickListener {
            presenter.onBackClicked()
        }

        rowChangePassword.setOnClickListener {
            presenter.onChangePasswordClicked()
        }

        switchMobileAlerts.setOnCheckedChangeListener { _, isChecked ->
            presenter.onAlertsToggled(isChecked)
        }
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun navigateToChangePassword() {
        startActivity(Intent(this, ChangePasswordActivity::class.java))
    }

    override fun navigateBack() {
        finish()
    }

    override fun showAlertsEnabled(enabled: Boolean) {
        switchMobileAlerts.isChecked = enabled
    }

    override fun startAlertMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !AlertNotifier(this).canPostNotifications()
        ) {
            alertPrefs.permissionRequested = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        AlertMonitorService.start(this)
    }

    override fun stopAlertMonitoring() {
        AlertMonitorService.stop(this)
    }
}