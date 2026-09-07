package com.eldroid.smartnest.ui.dashboard

import com.eldroid.smartnest.data.model.SensorReading
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DashboardPresenter(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) : DashboardContract.Presenter {

    private var view: DashboardContract.View? = null
    private var sensorListener: ValueEventListener? = null
    private var listenerRef: DatabaseReference? = null

    override fun attachView(view: DashboardContract.View) {
        this.view = view
    }

    override fun detachView() {
        view = null
    }

    override fun startListening() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            view?.navigateToLogin()
            return
        }

        val statusRef = database.getReference("devices").child(uid).child("current_status")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reading = snapshot.getValue(SensorReading::class.java)
                if (reading == null) {
                    view?.showLoadError("No sensor data available yet.")
                    return
                }
                if (!reading.deviceOnline) {
                    view?.showDeviceOffline()
                    return
                }
                view?.showSensorReading(reading)
            }

            override fun onCancelled(error: DatabaseError) {
                view?.showLoadError("Unable to load sensor data. Please check your connection.")
            }
        }

        statusRef.addValueEventListener(listener)
        sensorListener = listener
        listenerRef = statusRef
    }

    override fun stopListening() {
        val listener = sensorListener
        val ref = listenerRef
        if (listener != null && ref != null) {
            ref.removeEventListener(listener)
        }
        sensorListener = null
        listenerRef = null
    }

    override fun onLogoutClicked() {
        stopListening()
        auth.signOut()
        view?.navigateToLogin()
    }
}