package com.eldroid.smartnest.data.repository

import com.eldroid.smartnest.data.model.SmartNestDevice
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DeviceRegistryRepository {

    companion object {
        private const val DATABASE_URL =
            "https://smartnest-eldroid-default-rtdb.asia-southeast1.firebasedatabase.app"
    }

    interface DeviceListListener {
        fun onDevicesChanged(devices: List<SmartNestDevice>)
        fun onError(message: String)
    }

    private val database = FirebaseDatabase.getInstance(DATABASE_URL)

    private fun registryRef(uid: String) =
        database.getReference("devices").child(uid).child("registry")

    fun observeDevices(listener: DeviceListListener): ValueEventListener? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        registryRef(uid).removeEventListener(listener)
    }

    fun upsertDevice(device: SmartNestDevice, onComplete: (success: Boolean, error: String?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onComplete(false, "Not signed in.")
            return
        }

        val key = SmartNestDevice.keyFor(device.macAddress)
        registryRef(uid).child(key).setValue(device)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { e -> onComplete(false, e.message) }
    }
}