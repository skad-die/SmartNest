package com.eldroid.smartnest.data.repository

import com.eldroid.smartnest.data.FirebaseConstants
import com.eldroid.smartnest.data.model.AlertEvent
import com.eldroid.smartnest.data.model.SensorReading
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query

/**
 * Reads the logs the device writes:
 *  - /devices/{uid}/history/{macKey}/{pushId} : periodic sensor snapshots
 *  - /devices/{uid}/alerts/{pushId}           : alert events for all of the user's devices
 */
class HistoryRepository {

    /** Handle for a live alert listener; call [cancel] to stop it. */
    class AlertSubscription internal constructor(
        private val query: Query,
        private val listener: ChildEventListener
    ) {
        fun cancel() = query.removeEventListener(listener)
    }

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private val database: FirebaseDatabase
        get() = FirebaseDatabase.getInstance(FirebaseConstants.DATABASE_URL)

    private fun deviceRoot(): DatabaseReference? =
        auth.currentUser?.uid?.let { database.getReference("devices").child(it) }

    private fun historyRef(root: DatabaseReference, macKey: String) =
        root.child("history").child(macKey)

    private fun alertsRef(root: DatabaseReference) = root.child("alerts")

    /** Readings for one device since [sinceMs], newest first. */
    fun loadReadings(
        macKey: String,
        sinceMs: Long,
        onResult: (List<SensorReading>) -> Unit,
        onError: (String) -> Unit
    ) {
        val root = deviceRoot() ?: return onError(NOT_SIGNED_IN)

        historyRef(root, macKey)
            .orderByChild(FIELD_TIMESTAMP)
            .startAt(sinceMs.toDouble())
            .get()
            .addOnSuccessListener { snapshot ->
                val readings = snapshot.children
                    .mapNotNull { it.getValue(SensorReading::class.java) }
                    .sortedByDescending { it.timestamp }
                onResult(readings)
            }
            .addOnFailureListener { e -> onError(e.message ?: LOAD_FAILED) }
    }

    /** Alerts for one device since [sinceMs], newest first. */
    fun loadAlerts(
        macKey: String,
        sinceMs: Long,
        onResult: (List<AlertEvent>) -> Unit,
        onError: (String) -> Unit
    ) {
        val root = deviceRoot() ?: return onError(NOT_SIGNED_IN)

        alertsRef(root)
            .orderByChild(FIELD_TIMESTAMP)
            .startAt(sinceMs.toDouble())
            .get()
            .addOnSuccessListener { snapshot ->
                val alerts = snapshot.children
                    .mapNotNull { it.getValue(AlertEvent::class.java) }
                    .filter { it.macKey == macKey }
                    .sortedByDescending { it.timestamp }
                onResult(alerts)
            }
            .addOnFailureListener { e -> onError(e.message ?: LOAD_FAILED) }
    }

    /** Calls [onAlert] for every alert with a timestamp later than [afterMs], including new ones as they arrive. */
    fun observeNewAlerts(afterMs: Long, onAlert: (AlertEvent) -> Unit): AlertSubscription? {
        val root = deviceRoot() ?: return null

        val query = alertsRef(root)
            .orderByChild(FIELD_TIMESTAMP)
            .startAt((afterMs + 1).toDouble())

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(AlertEvent::class.java)?.let(onAlert)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) = Unit
            override fun onChildRemoved(snapshot: DataSnapshot) = Unit
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit
            override fun onCancelled(error: DatabaseError) = Unit
        }

        query.addChildEventListener(listener)
        return AlertSubscription(query, listener)
    }

    /**
     * Deletes entries past their retention period. There is no server doing this,
     * so it runs whenever the history screen is opened.
     */
    fun pruneOld(macKey: String, nowMs: Long = System.currentTimeMillis()) {
        val root = deviceRoot() ?: return
        deleteOlderThan(historyRef(root, macKey), nowMs - READING_RETENTION_MS)
        deleteOlderThan(alertsRef(root), nowMs - ALERT_RETENTION_MS)
    }

    private fun deleteOlderThan(ref: DatabaseReference, cutoffMs: Long) {
        ref.orderByChild(FIELD_TIMESTAMP)
            .endAt(cutoffMs.toDouble())
            .get()
            .addOnSuccessListener { snapshot ->
                val deletions = snapshot.children
                    .mapNotNull { it.key }
                    .associateWith<String, Any?> { null }
                if (deletions.isNotEmpty()) ref.updateChildren(deletions)
            }
    }

    companion object {
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val NOT_SIGNED_IN = "Not signed in."
        private const val LOAD_FAILED = "Unable to load history."

        private const val DAY_MS = 24L * 60 * 60 * 1000
        const val READING_RETENTION_MS = 7 * DAY_MS
        const val ALERT_RETENTION_MS = 30 * DAY_MS
    }
}
