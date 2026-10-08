package com.example.elite.market.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Historical price snapshot records for charting commodity price trends over time.
 */
@Entity(
    tableName = "market_price_history",
    indices = [
        Index(value = ["systemId", "commodityId"]),
        Index(value = ["timestamp"])
    ]
)
data class MarketPriceHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val systemId: Int,
    val systemName: String,
    val commodityId: Int,
    val commodityName: String,
    val priceDeciCr: Int,
    val cycle: Int,
    val economicState: String,
    val timestamp: Long = System.currentTimeMillis()
)
