package com.example.elite.dataset.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.elite.dataset.model.DatasetSessionEntity
import com.example.elite.dataset.model.TelemetryEventEntity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DatasetRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeSessions(userId: String): Flow<List<DatasetSessionEntity>> = callbackFlow {
        val collection = db.collection("users")
            .document(userId)
            .collection("sessions")
            .orderBy("updatedAt", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Error observing dataset sessions: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val sessions = snapshot.toObjects(DatasetSessionEntity::class.java)
                trySend(sessions)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun createOrUpdateSession(
        userId: String,
        sessionId: String,
        pilotCallsign: String,
        totalKills: Long,
        laserShotsFired: Long,
        laserHits: Long,
        accuracyRate: Double,
        missilesFired: Long,
        ecmSuccessCount: Long,
        creditsEarned: Double,
        survivalDurationSec: Long,
        bestStrategyArchetype: String,
        sampleCount: Long,
        status: String = "ACTIVE"
    ) {
        val docRef = db.collection("users")
            .document(userId)
            .collection("sessions")
            .document(sessionId)

        val docSnapshot = docRef.get().await()
        if (!docSnapshot.exists()) {
            val payload = mutableMapOf<String, Any>(
                "userId" to userId,
                "pilotCallsign" to pilotCallsign,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "totalKills" to totalKills,
                "laserShotsFired" to laserShotsFired,
                "laserHits" to laserHits,
                "accuracyRate" to accuracyRate,
                "missilesFired" to missilesFired,
                "ecmSuccessCount" to ecmSuccessCount,
                "creditsEarned" to creditsEarned,
                "survivalDurationSec" to survivalDurationSec,
                "bestStrategyArchetype" to bestStrategyArchetype,
                "sampleCount" to sampleCount,
                "status" to status
            )
            docRef.set(payload).await()
        } else {
            val updatePayload = mutableMapOf<String, Any>(
                "pilotCallsign" to pilotCallsign,
                "updatedAt" to FieldValue.serverTimestamp(),
                "totalKills" to totalKills,
                "laserShotsFired" to laserShotsFired,
                "laserHits" to laserHits,
                "accuracyRate" to accuracyRate,
                "missilesFired" to missilesFired,
                "ecmSuccessCount" to ecmSuccessCount,
                "creditsEarned" to creditsEarned,
                "survivalDurationSec" to survivalDurationSec,
                "bestStrategyArchetype" to bestStrategyArchetype,
                "sampleCount" to sampleCount,
                "status" to status
            )
            docRef.update(updatePayload).await()
        }
    }

    suspend fun recordTelemetry(
        userId: String,
        sessionId: String,
        telemetryId: String,
        eventType: String,
        combatRank: String,
        outcomeSuccess: Boolean,
        targetShipType: String? = null,
        playerShipSpeed: Double = 0.0,
        playerShields: Double = 0.0,
        playerEnergy: Double = 0.0,
        targetDistance: Double = 0.0,
        targetRelativeAngle: Double = 0.0,
        tacticalAction: String? = null,
        systemName: String? = null,
        isAutoPlay: Boolean = false
    ) {
        val docRef = db.collection("users")
            .document(userId)
            .collection("sessions")
            .document(sessionId)
            .collection("telemetry")
            .document(telemetryId)

        val payload = mutableMapOf<String, Any>(
            "userId" to userId,
            "sessionId" to sessionId,
            "eventType" to eventType,
            "timestamp" to FieldValue.serverTimestamp(),
            "combatRank" to combatRank,
            "outcomeSuccess" to outcomeSuccess,
            "playerShipSpeed" to playerShipSpeed,
            "playerShields" to playerShields,
            "playerEnergy" to playerEnergy,
            "targetDistance" to targetDistance,
            "targetRelativeAngle" to targetRelativeAngle,
            "isAutoPlay" to isAutoPlay
        )
        if (targetShipType != null) payload["targetShipType"] = targetShipType
        if (tacticalAction != null) payload["tacticalAction"] = tacticalAction
        if (systemName != null) payload["systemName"] = systemName

        docRef.set(payload).await()
    }

    companion object {
        private const val TAG = "DatasetRepository"
    }
}
