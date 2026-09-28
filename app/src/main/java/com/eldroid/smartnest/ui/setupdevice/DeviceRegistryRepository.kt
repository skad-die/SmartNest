package com.eldroid.smartnest.data.repository

import com.eldroid.smartnest.data.FirebaseConstants
import com.eldroid.smartnest.data.model.SmartNestDevice
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DeviceRegistryRepository {

    interface DeviceListListener {
        fun onDevicesChanged(devices: List<SmartNestDevice>)
        fun onError(message: String)
    }

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private val database: FirebaseDatabase
        get() = FirebaseDatabase.getInstance(FirebaseConstants.DATABASE_URL)

    private fun currentUid(): String? = auth.currentUser?.uid

    private fun deviceRoot(uid: String) = database.getReference("devices").child(uid)
    private fun registryRef(uid: String) = deviceRoot(uid).child("registry")

    fun observeDevices(listener: DeviceListListener): ValueEventListener? {
        val uid = currentUid() ?: run {
            listener.onError("Not signed in.")
            return null
        }

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val devices = snapshot.children.mapNotNull { child ->
                    child.getValue(SmartNestDevice::class.java)
                }
                listener.onDevicesChanged(devices)
            }

            override fun onCancelled(error: DatabaseError) {
                listener.onError(error.message)
            }
        }

        registryRef(uid).addValueEventListener(valueEventListener)
        return valueEventListener
    }

    fun stopObserving(listener: ValueEventListener) {
        val uid = currentUid() ?: return
        registryRef(uid).removeEventListener(listener)
    }

    fun upsertDevice(device: SmartNestDevice, onComplete: (Boolean, String?) -> Unit) {
        val uid = currentUid() ?: run {
            onComplete(false, "Not signed in.")
            return
        }

        val key = SmartNestDevice.keyFor(device.macAddress)
        registryRef(uid).child(key).setValue(device)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { e -> onComplete(false, e.message ?: "Failed to save device.") }
    }

    fun unpairDevice(device: SmartNestDevice, onComplete: (Boolean, String?) -> Unit) {
        val uid = currentUid() ?: run {
            onComplete(false, "Not signed in.")
            return
        }

        val key = SmartNestDevice.keyFor(device.macAddress)
        val root = deviceRoot(uid)

        // Use atomic multi-location update to execute unpair signal and deletion in a single request
        val updates = hashMapOf<String, Any?>(
            "commands/$key/unpair" to true,
            "registry/$key" to null,
            "status/$key" to null
        )

        root.updateChildren(updates)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { e -> onComplete(false, e.message ?: "Failed to unpair device.") }
    }

    fun observeDeviceOnline(
        key: String,
        onChange: (isOnline: Boolean) -> Unit
    ): ValueEventListener? {
        val uid = currentUid() ?: return null

        val sanitizedKey = SmartNestDevice.keyFor(key)
        val ref = deviceRoot(uid).child("status").child(sanitizedKey)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.child("state").getValue(String::class.java)

                val lastSeen = when (val raw = snapshot.child("lastSeen").value) {
                    is Number -> raw.toLong()
                    else -> null
                }

                val isHeartbeatFresh = lastSeen != null &&
                        (System.currentTimeMillis() - lastSeen) <= HEARTBEAT_TIMEOUT_MS

                val isOnline = state == "online" && isHeartbeatFresh
                onChange(isOnline)
            }

            override fun onCancelled(error: DatabaseError) {
                onChange(false)
            }
        }

        ref.addValueEventListener(listener)
        return listener
    }

    fun stopObservingOnlineStatus(key: String, listener: ValueEventListener) {
        val uid = currentUid() ?: return
        val sanitizedKey = SmartNestDevice.keyFor(key)
        deviceRoot(uid).child("status").child(sanitizedKey).removeEventListener(listener)
    }

    companion object {
        private const val HEARTBEAT_TIMEOUT_MS = 30_000L
    }
}