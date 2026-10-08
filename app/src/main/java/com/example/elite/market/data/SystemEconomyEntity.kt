package com.example.elite.market.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted economic condition and market cycle for a planetary system.
 */
@Entity(
    tableName = "system_economy_states",
    indices = [
        Index(value = ["systemId"], unique = true)
    ]
)
data class SystemEconomyEntity(
    @PrimaryKey
    val systemId: Int,
    val systemName: String,
    val economyType: Int,          // 0 = Rich Industrial ... 7 = Poor Agricultural
    val economyName: String,
    val governmentType: Int,       // 0 = Anarchy ... 7 = Corporate State
    val techLevel: Int,
    val marketCycle: Int = 1,
    val activeBoomState: String = "NORMAL", // NORMAL, BOOM, RECESSION, DROUGHT, MINERAL_RUSH, WAR_MOBILIZATION, PIRATE_BLOCKADE
    val boomDescription: String = "Stable orbital commerce",
    val tradeTaxPercent: Int = 5,
    val lastJumpTimestamp: Long = System.currentTimeMillis()
)
