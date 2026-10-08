package com.example.elite.model

import androidx.compose.ui.graphics.Color
import com.example.elite.math3d.Vector3
import com.example.elite.ships.ShipBlueprint
import com.example.elite.ships.ShipBlueprints
import kotlin.random.Random

/**
 * Faction allegiance in the sector-wide tactical war.
 */
enum class FleetFaction(
    val callsign: String,
    val title: String,
    val color: Color,
    val isHostileToPlayerByDefault: Boolean
) {
    GALACTIC_NAVY("NAVY", "Galactic Cooperative Navy", Color(0xFF00E5FF), false),
    PIRATE_CLAN("OUTLAW", "Viper-Skull Raider Syndicate", Color(0xFFFF2222), true),
    MERCHANT_GUILD("TRADER", "Far-Trader Merchant Fleet", Color(0xFF00FF66), false),
    THARGOID_SWARM("ALIEN", "Thargoid Hive Mind Swarm", Color(0xFFFFCC00), true),
    MERCENARY_CORPS("CORPS", "Shadow Legion Mercenaries", Color(0xFFFF77FF), false);

    fun isHostileTo(other: FleetFaction): Boolean {
        if (this == other) return false
        if (this == THARGOID_SWARM || other == THARGOID_SWARM) return true
        if (this == PIRATE_CLAN || other == PIRATE_CLAN) return true
        return false
    }
}

/**
 * Strategic Behavior Directive for Autonomous Bot AI.
 */
enum class BotStrategy(
    val title: String,
    val description: String,
    val preferredSpeedMult: Float,
    val aggression: Float
) {
    AGGRESSIVE_INTERCEPT("INTERCEPTOR", "Engage nearest hostiles with maximum laser fire and pursuit", 1.2f, 0.95f),
    SWARM_AMBUSH("WOLF PACK", "Coordinate flank maneuvers in concentrated groups", 1.0f, 0.85f),
    DEFENSIVE_ESCORT("ESCORT GUARD", "Protect high-value capital freighters and defensive stations", 0.85f, 0.60f),
    CONVOY_TRADER("MERCHANT TRADE", "Haul trade commodities between jump vectors and flee pirates", 0.70f, 0.15f),
    PATROL_RECON("SECTOR PATROL", "Patrol perimeter corridors scanning for unauthorized ships", 0.90f, 0.70f),
    HIT_AND_RUN("RAIDER SKIRMISH", "High-speed alpha strikes followed by tactical disengagement", 1.35f, 0.80f),
    BERSERK_ASSAULT("HIVE ASSAULT", "Relentless suicide ramming and swarming energy fire", 1.40f, 1.0f)
}

/**
 * Realtime Simulated Human / Bot Commander Pilot.
 */
data class SimulatedPilot(
    val id: String,
    val callsign: String,
    val isRealtimeHumanPlayer: Boolean,
    val pingMs: Int = if (isRealtimeHumanPlayer) Random.nextInt(18, 48) else 0,
    val rating: CombatRank,
    val faction: FleetFaction
)

/**
 * AI Bot Opponent / Realtime Player Space Entity in Sector Simulation.
 * Up to 100 bots simulated simultaneously with complete 3D position,
 * health, faction, strategy, combat targets, kills, and tactical behaviors.
 */
data class TacticalBotOpponent(
    val id: Int,
    val name: String,
    val pilot: SimulatedPilot,
    val blueprint: ShipBlueprint,
    val faction: FleetFaction,
    var strategy: BotStrategy,
    var position: Vector3,
    var velocity: Vector3 = Vector3(0f, 0f, 0f),
    var rotation: Vector3 = Vector3(0f, 0f, 0f), // pitch, yaw, roll
    var shields: Float = 100f,
    var hull: Float = 100f,
    var maxShields: Float = 100f,
    var maxHull: Float = 100f,
    var isDestroyed: Boolean = false,
    var currentTargetId: Int? = null,
    var isFiringLaser: Boolean = false,
    var laserCooldown: Float = 0f,
    var kills: Int = 0,
    var creditsScore: Int = 1000 + Random.nextInt(5000),
    var cargoType: String? = null,
    var lastCombatEvent: String? = null
) {
    val isAlive: Boolean get() = !isDestroyed && (shields > 0f || hull > 0f)

    fun takeDamage(amount: Float): Boolean {
        if (isDestroyed) return false
        if (shields >= amount) {
            shields -= amount
        } else {
            val remain = amount - shields
            shields = 0f
            hull = (hull - remain).coerceAtLeast(0f)
        }
        if (hull <= 0f) {
            isDestroyed = true
            return true
        }
        return false
    }

    fun healOrRecharge(shieldDelta: Float) {
        if (isDestroyed) return
        shields = (shields + shieldDelta).coerceAtMost(maxShields)
    }
}

/**
 * Real-time Strategic Battle Arena Statistics.
 */
data class FleetWarStatistics(
    val totalBots: Int,
    val activeCount: Int,
    val destroyedCount: Int,
    val navyCount: Int,
    val outlawCount: Int,
    val traderCount: Int,
    val alienCount: Int,
    val realPlayersCount: Int,
    val topAceCallsign: String,
    val topAceKills: Int
)
