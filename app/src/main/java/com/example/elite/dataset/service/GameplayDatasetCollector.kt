package com.example.elite.dataset.service

import com.example.elite.dataset.data.DatasetRepository
import com.example.elite.dataset.model.DatasetSessionEntity
import com.example.elite.flight.FlightEngine
import com.example.elite.model.CommanderState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class GameplayDatasetCollector(
    private val datasetRepository: DatasetRepository,
    private val scope: CoroutineScope
) {
    var currentUserId: String? = null
        private set

    val currentSessionId = UUID.randomUUID().toString()

    private var sessionStartTimeMs: Long = System.currentTimeMillis()
    private var telemetryHarvestJob: Job? = null

    // Session Metrics Accumulators
    private var laserShotsFired: Long = 0L
    private var laserHits: Long = 0L
    private var missilesFired: Long = 0L
    private var ecmSuccessCount: Long = 0L
    private var creditsEarned: Double = 0.0
    private var sampleCount: Long = 0L

    private val _isRecordingActive = MutableStateFlow(false)
    val isRecordingActive: StateFlow<Boolean> = _isRecordingActive.asStateFlow()

    private val _recentSessions = MutableStateFlow<List<DatasetSessionEntity>>(emptyList())
    val recentSessions: StateFlow<List<DatasetSessionEntity>> = _recentSessions.asStateFlow()

    private val _statusLog = MutableStateFlow("DATASET RECORDER INITIALIZED. AWAITING FLIGHT TELEMETRY.")
    val statusLog: StateFlow<String> = _statusLog.asStateFlow()

    fun onUserAuthenticated(userId: String) {
        currentUserId = userId
        _isRecordingActive.value = true
        _statusLog.value = "PILOT IDENTIFIED: $userId. DATA HARVEST PIPELINE ONLINE."

        // Observe cloud sessions
        scope.launch {
            datasetRepository.observeSessions(userId).collect { sessions ->
                _recentSessions.value = sessions
            }
        }
    }

    fun onUserSignedOut() {
        currentUserId = null
        _isRecordingActive.value = false
        _statusLog.value = "RECORDING HALTED. SIGN IN TO RESUME DATASET HARVEST."
    }

    fun startContinuousTelemetryHarvest(flightEngine: FlightEngine, commander: CommanderState) {
        telemetryHarvestJob?.cancel()
        telemetryHarvestJob = scope.launch {
            while (isActive) {
                delay(3000L) // Capture high-resolution strategic state slice every 3 seconds
                val uid = currentUserId ?: continue
                if (!_isRecordingActive.value) continue

                sampleCount++
                val eventType = if (flightEngine.lockedTarget != null) "COMBAT_LASER_PULSE" else "WAR_SECTOR_ENGAGEMENT"
                val dist = flightEngine.lockedTarget?.let {
                    it.position.length().toDouble()
                } ?: 0.0

                val targetBp = flightEngine.lockedTarget?.blueprint?.name
                val action = if (flightEngine.isAutoPlayActive) {
                    "BOT_${flightEngine.autoPlayMode.name}"
                } else {
                    "MANUAL_STICK_PITCH_${flightEngine.currentPitch.toInt()}"
                }

                try {
                    datasetRepository.recordTelemetry(
                        userId = uid,
                        sessionId = currentSessionId,
                        telemetryId = "frame_${System.currentTimeMillis()}_${sampleCount}",
                        eventType = eventType,
                        combatRank = commander.combatRank.name,
                        outcomeSuccess = (flightEngine.commander.killCount > 0),
                        targetShipType = targetBp,
                        playerShipSpeed = flightEngine.speed.toDouble(),
                        playerShields = (commander.forwardShield + commander.aftShield).toDouble(),
                        playerEnergy = commander.energyBanks.toDouble(),
                        targetDistance = dist,
                        tacticalAction = action,
                        isAutoPlay = flightEngine.isAutoPlayActive
                    )

                    // Keep session aggregate metadata fresh in cloud
                    val durationSec = (System.currentTimeMillis() - sessionStartTimeMs) / 1000L
                    val accuracy = if (laserShotsFired > 0) (laserHits.toDouble() / laserShotsFired * 100.0) else 0.0

                    datasetRepository.createOrUpdateSession(
                        userId = uid,
                        sessionId = currentSessionId,
                        pilotCallsign = commander.name,
                        totalKills = commander.killCount.toLong(),
                        laserShotsFired = laserShotsFired,
                        laserHits = laserHits,
                        accuracyRate = Math.round(accuracy * 10.0) / 10.0,
                        missilesFired = missilesFired,
                        ecmSuccessCount = ecmSuccessCount,
                        creditsEarned = creditsEarned,
                        survivalDurationSec = durationSec,
                        bestStrategyArchetype = classifyStrategy(),
                        sampleCount = sampleCount,
                        status = "ACTIVE"
                    )

                    _statusLog.value = "TELEMETRY HARVESTED: ${sampleCount} SAMPLES -> CLOUD DATASET"
                } catch (e: Exception) {
                    _statusLog.value = "TELEMETRY QUEUE NOTE: ${e.message ?: "syncing"}"
                }
            }
        }
    }

    fun onLaserFired(hit: Boolean) {
        laserShotsFired++
        if (hit) laserHits++
    }

    fun onMissileFired() {
        missilesFired++
    }

    fun onEcmNeutralized() {
        ecmSuccessCount++
    }

    fun onBountyEarned(credits: Double) {
        creditsEarned += credits
    }

    private fun classifyStrategy(): String {
        return when {
            laserHits > 20 && missilesFired > 3 -> "AGGRESSIVE_COMBAT_ACE"
            ecmSuccessCount > 2 -> "TACTICAL_DEFENSE_SURVIVOR"
            creditsEarned > 500.0 -> "PROFIT_MAXIMIZING_TRADER"
            else -> "BALANCED_FLEET_TACTICIAN"
        }
    }

    fun toggleRecording() {
        _isRecordingActive.value = !_isRecordingActive.value
        _statusLog.value = if (_isRecordingActive.value) {
            "AUTO TELEMETRY HARVEST: ACTIVE"
        } else {
            "AUTO TELEMETRY HARVEST: PAUSED"
        }
    }
}
