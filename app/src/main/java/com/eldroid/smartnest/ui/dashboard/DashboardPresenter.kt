package com.eldroid.smartnest.ui.dashboard

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

    private var sensorListener: ValueEventListener? = null
    private var listenerRef: DatabaseReference? = null

    private var registryListener: ValueEventListener? = null
    private var onlineListener: ValueEventListener? = null
    private var onlineListenerKey: String? = null

    override fun attachView(view: DashboardContract.View) {
        this.view = view
    }

    override fun detachView() {
        stopListening()
        view = null
    }

    override fun getSelectedDeviceUid(): String? = onlineListenerKey

    override fun startListening() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            view?.navigateToLogin()
            return
        }

        listenForSensorData(uid)
        listenForDeviceStatus()
    }

    private fun listenForSensorData(uid: String) {
        val statusRef = database.getReference("devices").child(uid).child("current_status")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reading = snapshot.getValue(SensorReading::class.java)
                if (reading != null) {
                    view?.showSensorReading(reading)
                } else {
                    view?.showNoDevicePaired()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                view?.showLoadError("Unable to load sensor data. Please check your connection.")
            }
        }

        statusRef.addValueEventListener(listener)
        sensorListener = listener
        listenerRef = statusRef
    }

    private fun listenForDeviceStatus() {
        registryListener = registry.observeDevices(object : DeviceRegistryRepository.DeviceListListener {
            override fun onDevicesChanged(devices: List<SmartNestDevice>) {
                stopOnlineListener()

                val device = devices.firstOrNull()
                if (device == null) {
                    view?.showNoDevicePaired()
                    return
                }

                // Ensure key replaces colons with dashes to match ESP32 status path: 68-FE-71-F8-74-76
                val key = device.macAddress.replace(":", "-")
                onlineListenerKey = key

                onlineListener = registry.observeDeviceOnline(key) { isOnline ->
                    if (isOnline) {
                        view?.showDeviceOnline()
                    } else {
                        view?.showDeviceOffline()
                    }
                }
            }

            override fun onError(message: String) {
                view?.showLoadError(message)
            }
        })
    }

    private fun stopOnlineListener() {
        val uid = auth.currentUser?.uid
        val key = onlineListenerKey
        val listener = onlineListener
        if (uid != null && key != null && listener != null) {
            database.getReference("devices").child(uid).child("status").child(key)
                .removeEventListener(listener)
        }
        onlineListener = null
        onlineListenerKey = null
    }

    override fun stopListening() {
        sensorListener?.let { listener ->
            listenerRef?.removeEventListener(listener)
        }
        sensorListener = null
        listenerRef = null

        stopOnlineListener()

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
}