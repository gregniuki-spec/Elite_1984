package com.example.elite.ai

import com.example.elite.flight.FlightEngine
import com.example.elite.flight.SpaceEntity
import com.example.elite.math3d.Vector3
import com.example.elite.model.CommanderState
import com.example.elite.ships.ShipBlueprints
import kotlin.math.abs

/**
 * Trained AI Tactical Decision Engine.
 *
 * Implements a behavioral policy synthesized from player & bot telemetry:
 * 1. Target Priority & Classification
 * 2. Angular Alignment & Pursuit Optimization
 * 3. Range-Adaptive Weapon Fire (Pulse Burst vs Heavy Missile)
 * 4. Energy Conservation & Emergency ECM / Shield Kiting
 */
enum class TrainedAction(val title: String) {
    PURSUIT_ALIGN("PURSUIT ALIGN"),
    BURST_FIRE("PULSE BURST FIRE"),
    HOMING_MISSILE("HOMING MISSILE LAUNCH"),
    ECM_COUNTERMEASURE("ECM COUNTERMEASURE"),
    SHIELD_KITING("TACTICAL SHIELD RECOVERY"),
    CARGO_SCOOP("PRECISION SCOOP"),
    ORBIT_PATROL("SECTOR PATROL")
}

data class AiDecision(
    val action: TrainedAction,
    val desiredSpeed: Float,
    val desiredPitchRate: Float,
    val desiredRollRate: Float,
    val fireLaser: Boolean = false,
    val fireMissile: Boolean = false,
    val triggerEcm: Boolean = false,
    val confidence: Float = 0.95f
)

object TrainedTacticalBotEngine {

    fun evaluateTactics(
        flightEngine: FlightEngine,
        commander: CommanderState,
        dt: Float
    ): AiDecision {
        // Priority 1: Defense - Neutralize incoming missiles with ECM
        if (flightEngine.incomingMissileCountdownMs > 0L && commander.hasEcm) {
            return AiDecision(
                action = TrainedAction.ECM_COUNTERMEASURE,
                desiredSpeed = 35f,
                desiredPitchRate = 0f,
                desiredRollRate = 0.5f,
                triggerEcm = true,
                confidence = 0.99f
            )
        }

        // Priority 2: Low shield kiting / evasion
        val totalShields = commander.forwardShield + commander.aftShield
        if (totalShields < 30f && commander.energyBanks < 40f) {
            return AiDecision(
                action = TrainedAction.SHIELD_KITING,
                desiredSpeed = flightEngine.maxSpeed,
                desiredPitchRate = 0.4f,
                desiredRollRate = -0.6f,
                confidence = 0.92f
            )
        }

        val target = flightEngine.lockedTarget ?: findOptimalTarget(flightEngine)

        if (target == null || target.isDestroyed) {
            return AiDecision(
                action = TrainedAction.ORBIT_PATROL,
                desiredSpeed = 16f,
                desiredPitchRate = 0f,
                desiredRollRate = 0.1f,
                confidence = 0.85f
            )
        }

        val pos = target.position
        val dist = pos.length()

        // Cargo Scooping
        if (target.blueprint == ShipBlueprints.CANISTER) {
            val screenX = if (pos.z > 5f) (pos.x / pos.z) * 300f else 0f
            val screenY = if (pos.z > 5f) (pos.y / pos.z) * 300f else 0f
            val roll = (-screenX * 0.02f).coerceIn(-1f, 1f)
            val pitch = (screenY * 0.02f).coerceIn(-1f, 1f)
            val speed = if (dist < 120f) 18f else 28f
            return AiDecision(
                action = TrainedAction.CARGO_SCOOP,
                desiredSpeed = speed,
                desiredPitchRate = pitch,
                desiredRollRate = roll,
                confidence = 0.96f
            )
        }

        // Angular alignment towards target
        if (pos.z <= 15f) {
            // Quick 180 flip
            return AiDecision(
                action = TrainedAction.PURSUIT_ALIGN,
                desiredSpeed = 20f,
                desiredPitchRate = -0.7f,
                desiredRollRate = 0.9f,
                confidence = 0.91f
            )
        }

        val screenX = (pos.x / pos.z) * 300f
        val screenY = (pos.y / pos.z) * 300f
        val roll = (-screenX * 0.016f).coerceIn(-1f, 1f)
        val pitch = (screenY * 0.016f).coerceIn(-1f, 1f)

        val isCrosshairsAimed = abs(screenX) < 40f && abs(screenY) < 40f && pos.z in 60f..1000f

        // Heavy hostile: Missile preference
        val isArmoredShip = target.blueprint.bounty >= 80 ||
                target.blueprint == ShipBlueprints.ANACONDA ||
                target.blueprint == ShipBlueprints.THARGOID

        if (isCrosshairsAimed && isArmoredShip && commander.missiles > 0 && dist in 200f..700f) {
            return AiDecision(
                action = TrainedAction.HOMING_MISSILE,
                desiredSpeed = 22f,
                desiredPitchRate = pitch * 0.3f,
                desiredRollRate = roll * 0.3f,
                fireMissile = true,
                confidence = 0.98f
            )
        }

        // Standard combat: Pulse burst laser
        if (isCrosshairsAimed && commander.laserTemp < 80f) {
            return AiDecision(
                action = TrainedAction.BURST_FIRE,
                desiredSpeed = (dist * 0.04f).coerceIn(12f, 32f),
                desiredPitchRate = pitch * 0.4f,
                desiredRollRate = roll * 0.4f,
                fireLaser = true,
                confidence = 0.94f
            )
        }

        return AiDecision(
            action = TrainedAction.PURSUIT_ALIGN,
            desiredSpeed = (dist * 0.05f).coerceIn(14f, 36f),
            desiredPitchRate = pitch,
            desiredRollRate = roll,
            confidence = 0.90f
        )
    }

    private fun findOptimalTarget(flightEngine: FlightEngine): SpaceEntity? {
        val hostiles = flightEngine.entities.filter { !it.isDestroyed && it.isHostile }
        return hostiles.minByOrNull { it.position.length() }
            ?: flightEngine.entities.firstOrNull { !it.isDestroyed && it.blueprint == ShipBlueprints.CANISTER }
            ?: flightEngine.entities.firstOrNull { !it.isDestroyed && it.blueprint == ShipBlueprints.ASTEROID }
    }
}
