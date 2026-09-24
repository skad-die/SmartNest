package com.eldroid.smartnest.ui.ledcontrol

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.eldroid.smartnest.R
import com.google.android.material.button.MaterialButton

class LedControlActivity : AppCompatActivity(), LedControlContract.View {

    private val presenter: LedControlContract.Presenter by lazy { LedControlPresenter() }

    private lateinit var toolbar: Toolbar
    private lateinit var tvConnectionStatus: TextView
    private lateinit var tvLastUpdate: TextView
    private lateinit var tvLedStatus: TextView
    private lateinit var tvCommandStatus: TextView
    private lateinit var tvErrorMessage: TextView
    private lateinit var progressCommand: ProgressBar
    private lateinit var btnTurnOn: MaterialButton
    private lateinit var btnTurnOff: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_led_control)

        toolbar = findViewById(R.id.toolbar)
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus)
        tvLastUpdate = findViewById(R.id.tvLastUpdate)
        tvLedStatus = findViewById(R.id.tvLedStatus)
        tvCommandStatus = findViewById(R.id.tvCommandStatus)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        progressCommand = findViewById(R.id.progressCommand)
        btnTurnOn = findViewById(R.id.btnTurnOn)
        btnTurnOff = findViewById(R.id.btnTurnOff)

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = view.layoutParams as android.view.ViewGroup.MarginLayoutParams
            params.topMargin = systemBars.top
            view.layoutParams = params
            insets
        }

        toolbar.setNavigationOnClickListener { finish() }

        presenter.attachView(this)

        btnTurnOn.setOnClickListener { presenter.onTurnOnClicked() }
        btnTurnOff.setOnClickListener { presenter.onTurnOffClicked() }

        presenter.onScreenOpened()
    }

    override fun onDestroy() {
        presenter.onScreenClosed()
        presenter.detachView()
        super.onDestroy()
    }

    override fun showConnectionStatus(isOnline: Boolean, isUnavailable: Boolean) {
        tvConnectionStatus.text = when {
            isUnavailable -> getString(R.string.device_status_unavailable)
            isOnline -> getString(R.string.device_status_online)
            else -> getString(R.string.device_status_offline)
        }
    }

    override fun showLastUpdate(secondsAgoText: String) {
        tvLastUpdate.text = secondsAgoText
    }

    override fun showLedStatus(isOn: Boolean) {
        tvLedStatus.text = getString(if (isOn) R.string.led_status_on else R.string.led_status_off)
    }

    override fun showLedStatusUnknown() {
        tvLedStatus.text = getString(R.string.led_status_unknown)
    }

    override fun showCommandSent(isOn: Boolean) {
        progressCommand.visibility = android.view.View.GONE
        tvErrorMessage.visibility = android.view.View.GONE
        tvCommandStatus.text = getString(if (isOn) R.string.command_sent_on else R.string.command_sent_off)
        setButtonsEnabled(true)
    }

    override fun showSendingCommand() {
        progressCommand.visibility = android.view.View.VISIBLE
        tvErrorMessage.visibility = android.view.View.GONE
        tvCommandStatus.text = getString(R.string.sending_command)
        setButtonsEnabled(false)
    }

    override fun showCommandError(message: String) {
        progressCommand.visibility = android.view.View.GONE
        tvErrorMessage.visibility = android.view.View.VISIBLE
        tvErrorMessage.text = message
        setButtonsEnabled(true)
    }

    override fun clearCommandStatus() {
        tvCommandStatus.text = getString(R.string.placeholder_dash)
        tvErrorMessage.visibility = android.view.View.GONE
    }

    override fun setButtonsEnabled(enabled: Boolean) {
        btnTurnOn.isEnabled = enabled
        btnTurnOff.isEnabled = enabled
    }
}