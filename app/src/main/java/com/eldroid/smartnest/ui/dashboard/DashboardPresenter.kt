package com.eldroid.smartnest.ui.dashboard

import android.os.Handler
import android.os.Looper
import com.eldroid.smartnest.data.FirebaseConstants
import com.eldroid.smartnest.data.model.SensorReading
import com.eldroid.smartnest.data.model.SmartNestDevice
import com.eldroid.smartnest.data.repository.DeviceRegistryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DashboardPresenter(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(FirebaseConstants.DATABASE_URL),
    private val registry: DeviceRegistryRepository = DeviceRegistryRepository()
) : DashboardContract.Presenter {

    private var view: DashboardContract.View? = null

    // sensor data
    private var sensorListener: ValueEventListener? = null
    private var sensorRef: DatabaseReference? = null

    // registry + per-device status
    private var registryListener: ValueEventListener? = null
    private val statusListeners = mutableMapOf<String, ValueEventListener>()
    private val rawStatus = mutableMapOf<String, Pair<String?, Long?>>()   // key -> (state, lastSeen)
    private var devices: List<SmartNestDevice> = emptyList()

    // Firebase server clock offset
    private var offsetRef: DatabaseReference? = null
    private var offsetListener: ValueEventListener? = null
    private var serverOffsetMs = 0L

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            publishDevices()
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun attachView(view: DashboardContract.View) {
        this.view = view
    }

    override fun detachView() {
        stopListening()
        view = null
    }

    override fun getSelectedDeviceUid(): String? =
        devices.firstOrNull()?.let { SmartNestDevice.keyFor(it.macAddress) }

    override fun startListening() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            view?.navigateToLogin()
            return
        }

        listenForSensorData(uid)
        listenForServerOffset()
        listenForDevices()
        handler.postDelayed(ticker, TICK_MS)
    }

    private fun listenForSensorData(uid: String) {
        val ref = database.getReference("devices").child(uid).child("current_status")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reading = snapshot.getValue(SensorReading::class.java)
                if (reading != null) view?.showSensorReading(reading) else view?.showNoSensorData()
            }

            override fun onCancelled(error: DatabaseError) {
                view?.showLoadError("Unable to load sensor data. Please check your connection.")
            }
        }

        ref.addValueEventListener(listener)
        sensorListener = listener
        sensorRef = ref
    }

    private fun listenForServerOffset() {
        val ref = database.getReference(".info/serverTimeOffset")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                serverOffsetMs = (snapshot.value as? Number)?.toLong() ?: 0L
                publishDevices()
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }
        ref.addValueEventListener(listener)
        offsetRef = ref
        offsetListener = listener
    }

    private fun listenForDevices() {
        registryListener = registry.observeDevices(object : DeviceRegistryRepository.DeviceListListener {
            override fun onDevicesChanged(devices: List<SmartNestDevice>) {
                this@DashboardPresenter.devices = devices
                stopStatusListeners()

                if (devices.isEmpty()) {
                    view?.showEmptyState()
                    return
                }

                devices.forEach { device ->
                    val key = SmartNestDevice.keyFor(device.macAddress)
                    registry.observeDeviceStatus(key) { state, lastSeen ->
                        rawStatus[key] = state to lastSeen
                        publishDevices()
                    }?.let { statusListeners[key] = it }
                }
                publishDevices()
            }

            override fun onError(message: String) {
                view?.showLoadError(message)
            }
        })
    }

    private fun publishDevices() {
        if (devices.isEmpty()) return

        val serverNow = System.currentTimeMillis() + serverOffsetMs
        val items = devices.map { device ->
            val key = SmartNestDevice.keyFor(device.macAddress)
            val (state, lastSeen) = rawStatus[key] ?: (null to null)
            val online = state == "online" && lastSeen != null &&
                    (serverNow - lastSeen) <= HEARTBEAT_TIMEOUT_MS
            DashboardDevice(device, online)
        }
        view?.showDevices(items)
    }

    override fun unpairDevice(device: SmartNestDevice) {
        registry.unpairDevice(device) { success, errorMessage ->
            if (success) {
                view?.showUnpairSuccess(device.displayName)
            } else {
                view?.showLoadError(errorMessage ?: "Failed to unpair device.")
            }
        }
    }

    private fun stopStatusListeners() {
        statusListeners.forEach { (key, listener) -> registry.stopObservingOnlineStatus(key, listener) }
        statusListeners.clear()
        rawStatus.clear()
    }

    override fun stopListening() {
        handler.removeCallbacks(ticker)

        sensorListener?.let { sensorRef?.removeEventListener(it) }
        sensorListener = null
        sensorRef = null

        offsetListener?.let { offsetRef?.removeEventListener(it) }
        offsetListener = null
        offsetRef = null

        stopStatusListeners()

        registryListener?.let { registry.stopObserving(it) }
        registryListener = null
    }

    override fun onDrawerOpened() {
        view?.showUserEmail(auth.currentUser?.email ?: "Unknown user")
        val name = auth.currentUser?.displayName
        view?.showUserName(if (name.isNullOrBlank()) "SmartNest User" else name)
    }

    override fun onLogoutClicked() {
        stopListening()
        auth.signOut()
        view?.navigateToLogin()
    }

    private companion object {
        const val TICK_MS = 5_000L
        const val HEARTBEAT_TIMEOUT_MS = 30_000L
    }
}