package com.example.elite.market.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted market state for a specific commodity in a specific planetary system.
 * Indexed by systemId and commodityId for fast lookups.
 */
@Entity(
    tableName = "system_market_commodities",
    indices = [
        Index(value = ["systemId", "commodityId"], unique = true),
        Index(value = ["systemId"])
    ]
)
data class SystemCommodityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val systemId: Int,
    val commodityId: Int,
    val name: String,
    val unit: String,
    val basePriceDeciCr: Int,
    val currentPriceDeciCr: Int,
    val previousPriceDeciCr: Int,
    val availableQty: Int,
    val demandMultiplier: Float = 1.0f,
    val lastFluctuationCycle: Int = 0,
    val updatedAtTimestamp: Long = System.currentTimeMillis()
)
