package com.example.elite.ships.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.elite.model.LaserType

/**
 * Room Entity persisting player's ship upgrades and outfitting modifications.
 * Manages Hull Reinforcement, Cargo Capacity expansions, Weapon Systems,
 * and specialized avionics equipment.
 */
@Entity(tableName = "ship_upgrades")
data class ShipUpgradeEntity(
    @PrimaryKey
    val id: Long = 1L,

    val shipName: String = "Cobra Mk III",

    // --- HULL REINFORCEMENT ---
    val hullReinforcementLevel: Int = 0,     // 0 to 5
    val maxHullIntegrity: Float = 100f,       // 100 to 280
    val currentHullIntegrity: Float = 100f,   // Current hit points
    val armorRatingPercent: Int = 0,         // 0% to 50% damage reduction

    // --- CARGO CAPACITY ---
    val cargoCapacityTier: Int = 0,           // 0 to 5
    val cargoHoldMaxTonnes: Int = 20,         // 20t to 120t

    // --- WEAPON SYSTEMS ---
    val frontWeapon: String = "PULSE",        // PULSE, BEAM, MINING, MILITARY, PLASMA
    val rearWeapon: String? = null,           // null or weapon name
    val weaponPowerRating: Int = 15,          // Laser damage power
    val weaponCoolingTier: Int = 0,           // 0 to 3
    val weaponCoolingMultiplier: Float = 1.0f,// 1.0f to 2.8f
    val missileCapacity: Int = 4,             // 4, 6, 8
    val missilesArmed: Int = 3,

    // --- DEFENSE & PROPULSION ---
    val shieldGeneratorTier: Int = 1,         // 1 to 4
    val maxShields: Float = 100f,
    val thrusterTier: Int = 1,                // 1 to 3

    // --- UTILITIES & AVIONICS ---
    val hasEcm: Boolean = false,
    val hasFuelScoops: Boolean = false,
    val hasDockingComputer: Boolean = false,
    val hasGalacticHyperdrive: Boolean = false,
    val hasEscapePod: Boolean = false,
    val hasEnergyBomb: Boolean = false,
    val hasNavalEnergyUnit: Boolean = false,

    // --- AUDIT TRAIL ---
    val totalCreditsInvestedDeciCr: Long = 0L,
    val lastUpgradeTimestamp: Long = System.currentTimeMillis()
) {
    fun toFrontLaserType(): LaserType {
        return when (frontWeapon.uppercase()) {
            "BEAM" -> LaserType.BEAM
            "MINING" -> LaserType.MINING
            "MILITARY", "PLASMA" -> LaserType.MILITARY
            else -> LaserType.PULSE
        }
    }

    fun toRearLaserType(): LaserType? {
        val r = rearWeapon ?: return null
        return when (r.uppercase()) {
            "PULSE" -> LaserType.PULSE
            "BEAM" -> LaserType.BEAM
            "MINING" -> LaserType.MINING
            "MILITARY" -> LaserType.MILITARY
            else -> null
        }
    }

    fun isHullDamaged(): Boolean = currentHullIntegrity < maxHullIntegrity

    companion object {
        fun defaultCobraMk3(): ShipUpgradeEntity = ShipUpgradeEntity()
    }
}
