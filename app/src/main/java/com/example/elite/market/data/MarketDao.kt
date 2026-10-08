package com.example.elite.market.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketDao {

    // --- Commodity operations ---
    @Query("SELECT * FROM system_market_commodities WHERE systemId = :systemId ORDER BY commodityId ASC")
    fun getCommoditiesForSystem(systemId: Int): Flow<List<SystemCommodityEntity>>

    @Query("SELECT * FROM system_market_commodities WHERE systemId = :systemId ORDER BY commodityId ASC")
    suspend fun getCommoditiesForSystemOnce(systemId: Int): List<SystemCommodityEntity>

    @Query("SELECT * FROM system_market_commodities WHERE systemId = :systemId AND commodityId = :commodityId LIMIT 1")
    suspend fun getCommodity(systemId: Int, commodityId: Int): SystemCommodityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommodities(commodities: List<SystemCommodityEntity>)

    @Update
    suspend fun updateCommodity(commodity: SystemCommodityEntity)

    @Query("UPDATE system_market_commodities SET availableQty = :qty, updatedAtTimestamp = :timestamp WHERE systemId = :systemId AND commodityId = :commodityId")
    suspend fun updateQuantity(systemId: Int, commodityId: Int, qty: Int, timestamp: Long = System.currentTimeMillis())

    // --- Economy state operations ---
    @Query("SELECT * FROM system_economy_states WHERE systemId = :systemId LIMIT 1")
    fun getEconomyForSystem(systemId: Int): Flow<SystemEconomyEntity?>

    @Query("SELECT * FROM system_economy_states WHERE systemId = :systemId LIMIT 1")
    suspend fun getEconomyForSystemOnce(systemId: Int): SystemEconomyEntity?

    @Query("SELECT * FROM system_economy_states")
    fun getAllEconomyStates(): Flow<List<SystemEconomyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEconomy(economy: SystemEconomyEntity)

    // --- Price history operations ---
    @Query("SELECT * FROM market_price_history WHERE systemId = :systemId AND commodityId = :commodityId ORDER BY timestamp ASC LIMIT :limit")
    fun getPriceHistoryForCommodity(systemId: Int, commodityId: Int, limit: Int = 30): Flow<List<MarketPriceHistoryEntity>>

    @Query("SELECT * FROM market_price_history WHERE systemId = :systemId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistoryForSystem(systemId: Int, limit: Int = 50): Flow<List<MarketPriceHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistory(history: MarketPriceHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistoryBatch(history: List<MarketPriceHistoryEntity>)

    @Query("DELETE FROM market_price_history WHERE timestamp < :cutoffTimestamp")
    suspend fun purgeOldHistory(cutoffTimestamp: Long)

    @Query("DELETE FROM system_market_commodities")
    suspend fun clearAllCommodities()

    @Query("DELETE FROM system_economy_states")
    suspend fun clearAllEconomies()
}
