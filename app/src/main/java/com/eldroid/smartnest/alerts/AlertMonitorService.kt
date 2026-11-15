package com.eldroid.smartnest.alerts

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.eldroid.smartnest.R
import com.eldroid.smartnest.data.model.AlertEvent
import com.eldroid.smartnest.data.model.SmartNestDevice
import com.eldroid.smartnest.data.repository.DeviceRegistryRepository
import com.eldroid.smartnest.data.repository.HistoryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener

/**
 * Keeps a database listener alive while the app is in the background and turns
 * each new alert entry written by a device into a phone notification.
 */
class AlertMonitorService : Service() {

    private val history = HistoryRepository()
    private val registry = DeviceRegistryRepository()

    private lateinit var prefs: AlertPrefs
    private lateinit var notifier: AlertNotifier

    private var alertSubscription: HistoryRepository.AlertSubscription? = null
    private var registryListener: ValueEventListener? = null
    private var deviceNames: Map<String, String> = emptyMap()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = AlertPrefs(this)
        notifier = AlertNotifier(this)
        notifier.ensureChannels()

        if (!enterForeground()) {
            stopSelf()
            return
        }

        if (!prefs.alertsEnabled || FirebaseAuth.getInstance().currentUser == null) {
            stopSelf()
            return
        }

        startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        alertSubscription?.cancel()
        alertSubscription = null
        registryListener?.let { registry.stopObserving(it) }
        registryListener = null
        super.onDestroy()
    }

    private fun enterForeground(): Boolean = try {
        val notification = notifier.monitoringNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                AlertNotifier.MONITORING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(AlertNotifier.MONITORING_NOTIFICATION_ID, notification)
        }
        true
    } catch (e: IllegalStateException) {
        // Android 12+ refuses foreground starts from the background (e.g. a system restart
        // while the app is not visible). Monitoring resumes the next time the app is opened.
        false
    }

    private fun startListening() {
        registryListener = registry.observeDevices(object : DeviceRegistryRepository.DeviceListListener {
            override fun onDevicesChanged(devices: List<SmartNestDevice>) {
                deviceNames = devices.associate {
                    SmartNestDevice.keyFor(it.macAddress) to it.displayName
                }
            }

            override fun onError(message: String) = Unit
        })

        alertSubscription = history.observeNewAlerts(startTimestamp(), ::onAlert)
    }

    /**
     * Where to resume from: the last alert already notified, but never further back than
     * [MAX_BACKLOG_MS] so a long gap does not replay a pile of stale alerts.
     */
    private fun startTimestamp(): Long {
        val now = System.currentTimeMillis()
        val saved = prefs.lastNotifiedTimestamp
        return if (saved == 0L) now - FIRST_RUN_LOOKBACK_MS else maxOf(saved, now - MAX_BACKLOG_MS)
    }

    private fun onAlert(event: AlertEvent) {
        if (event.timestamp > prefs.lastNotifiedTimestamp) {
            prefs.lastNotifiedTimestamp = event.timestamp
        }
        val deviceName = deviceNames[event.macKey]?.takeIf { it.isNotBlank() }
            ?: getString(R.string.alert_default_device_name)
        notifier.showAlert(event, deviceName)
    }

    companion object {
        private const val FIRST_RUN_LOOKBACK_MS = 2 * 60 * 1000L
        private const val MAX_BACKLOG_MS = 60 * 60 * 1000L

        /** Starts monitoring if alerts are enabled and a user is signed in. Call while the app is visible. */
        fun start(context: Context) {
            if (!AlertPrefs(context).alertsEnabled) return
            if (FirebaseAuth.getInstance().currentUser == null) return
            ContextCompat.startForegroundService(
                context,
                Intent(context, AlertMonitorService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlertMonitorService::class.java))
        }
    }
}
