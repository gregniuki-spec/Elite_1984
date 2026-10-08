package com.example.elite.dataset.model

import com.google.firebase.Timestamp

data class DatasetSessionEntity(
    val userId: String = "",
    val pilotCallsign: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val totalKills: Long = 0L,
    val laserShotsFired: Long = 0L,
    val laserHits: Long = 0L,
    val accuracyRate: Double = 0.0,
    val missilesFired: Long = 0L,
    val ecmSuccessCount: Long = 0L,
    val creditsEarned: Double = 0.0,
    val survivalDurationSec: Long = 0L,
    val bestStrategyArchetype: String = "BALANCED_TACTICIAN",
    val sampleCount: Long = 0L,
    val status: String = "ACTIVE"
)

data class TelemetryEventEntity(
    val userId: String = "",
    val sessionId: String = "",
    val eventType: String = "COMBAT_LASER_PULSE",
    val timestamp: Timestamp? = null,
    val combatRank: String = "HARMLESS",
    val outcomeSuccess: Boolean = false,
    val targetShipType: String? = null,
    val playerShipSpeed: Double = 0.0,
    val playerShields: Double = 0.0,
    val playerEnergy: Double = 0.0,
    val targetDistance: Double = 0.0,
    val targetRelativeAngle: Double = 0.0,
    val tacticalAction: String? = null,
    val systemName: String? = null,
    val isAutoPlay: Boolean = false
)
