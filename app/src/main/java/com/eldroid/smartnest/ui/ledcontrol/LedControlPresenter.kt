package com.eldroid.smartnest.ui.ledcontrol

import com.eldroid.smartnest.data.FirebaseConstants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.TimeUnit

class LedControlPresenter : LedControlContract.Presenter {

    companion object {
        private const val OFFLINE_THRESHOLD_SECONDS = 30
    }

    private val database = FirebaseDatabase.getInstance(FirebaseConstants.DATABASE_URL)
    private var view: LedControlContract.View? = null

    private var statusListener: ValueEventListener? = null
    private var deviceStatusListener: ValueEventListener? = null

    // Single identity model: the ESP32 authenticates to Firebase using the
    // SAME account as the signed-in app user, so both sides always agree
    // on which UID's /devices/<uid>/... node to read and write.
    private fun uid(): String? = FirebaseAuth.getInstance().currentUser?.uid

    private fun ledStatusRef() = uid()?.let { database.getReference("devices").child(it).child("led").child("status") }
    private fun ledCommandRef() = uid()?.let { database.getReference("devices").child(it).child("led").child("command") }
    private fun deviceStatusRef() = uid()?.let { database.getReference("devices").child(it).child("deviceStatus") }

    override fun attachView(view: LedControlContract.View) {
        this.view = view
    }

    override fun detachView() {
        view = null
    }

    override fun onScreenOpened() {
        val uidValue = uid()
        if (uidValue == null) {
            view?.showCommandError("Not signed in.")
            return
        }

        observeLedStatus()
        observeDeviceStatus()
    }

    override fun onScreenClosed() {
        statusListener?.let { ledStatusRef()?.removeEventListener(it) }
        deviceStatusListener?.let { deviceStatusRef()?.removeEventListener(it) }
        statusListener = null
        deviceStatusListener = null
    }

    private fun observeLedStatus() {
        val ref = ledStatusRef() ?: return
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(String::class.java)
                when (value) {
                    "ON" -> view?.showLedStatus(true)
                    "OFF" -> view?.showLedStatus(false)
                    else -> view?.showLedStatusUnknown()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                view?.showCommandError(error.message)
            }
        }
        ref.addValueEventListener(listener)
        statusListener = listener
    }

    private fun observeDeviceStatus() {
        val ref = deviceStatusRef() ?: return
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java)
                val state = snapshot.child("state").getValue(String::class.java)

                if (lastSeen == null || state == null) {
                    view?.showConnectionStatus(isOnline = false, isUnavailable = true)
                    view?.showLastUpdate("—")
                    return
                }

                val nowSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
                val secondsAgo = nowSeconds - lastSeen
                val isOnline = state == "online" && secondsAgo <= OFFLINE_THRESHOLD_SECONDS

                view?.showConnectionStatus(isOnline = isOnline, isUnavailable = false)
                view?.showLastUpdate(formatSecondsAgo(secondsAgo))
            }

            override fun onCancelled(error: DatabaseError) {
                view?.showCommandError(error.message)
            }
        }
        ref.addValueEventListener(listener)
        deviceStatusListener = listener
    }

    private fun formatSecondsAgo(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}s ago"
            seconds < 3600 -> "${seconds / 60}m ago"
            else -> "${seconds / 3600}h ago"
        }
    }

    override fun onTurnOnClicked() = sendCommand("ON")

    override fun onTurnOffClicked() = sendCommand("OFF")

    private fun sendCommand(command: String) {
        val ref = ledCommandRef()
        if (ref == null) {
            view?.showCommandError("Not signed in.")
            return
        }

        view?.showSendingCommand()
        ref.setValue(command)
            .addOnSuccessListener {
                view?.showCommandSent(isOn = command == "ON")
            }
            .addOnFailureListener { e ->
                view?.showCommandError(e.message ?: "Failed to send command.")
            }
    }
}