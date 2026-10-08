package com.example.elite.market.data

import com.example.elite.market.EconomyCondition
import com.example.elite.market.MarketEngine
import com.example.elite.model.Commodity
import com.example.elite.model.SystemData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Repository coordinating Room database persistence, planetary economy state transitions,
 * price fluctuation algorithms, and reactive Flow streams.
 */
class MarketRepository(
    private val marketDao: MarketDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Observe commodities reactively for the given system.
     */
    fun observeCommoditiesForSystem(systemId: Int): Flow<List<Commodity>> {
        return marketDao.getCommoditiesForSystem(systemId).map { entities ->
            entities.map { entity ->
                val base = MarketEngine.BASE_ITEMS.find { it.id == entity.commodityId }
                    ?: MarketEngine.BASE_ITEMS[0]
                base.copy(
                    price = entity.currentPriceDeciCr,
                    quantity = entity.availableQty
                )
            }
        }
    }

    /**
     * Observe raw Room entities with previous price data and fluctuation indicators.
     */
    fun observeCommodityEntities(systemId: Int): Flow<List<SystemCommodityEntity>> {
        return marketDao.getCommoditiesForSystem(systemId)
    }

    /**
     * Observe current economy condition and boom state for a system.
     */
    fun observeEconomyForSystem(systemId: Int): Flow<SystemEconomyEntity?> {
        return marketDao.getEconomyForSystem(systemId)
    }

    /**
     * Observe price trend history for a specific commodity in a system.
     */
    fun observePriceHistory(systemId: Int, commodityId: Int): Flow<List<MarketPriceHistoryEntity>> {
        return marketDao.getPriceHistoryForCommodity(systemId, commodityId)
    }

    /**
     * Initialize or ensure market and economy state exists in Room database for a given planetary system.
     * Generates initial baseline or loads existing persisted state.
     */
    suspend fun getOrInitializeMarket(system: SystemData): List<Commodity> = withContext(ioDispatcher) {
        val existingCommodities = marketDao.getCommoditiesForSystemOnce(system.id)
        var econEntity = marketDao.getEconomyForSystemOnce(system.id)

        if (econEntity == null) {
            val initialCondition = EconomyCondition.pickRandomForSystem(system, cycle = 1)
            econEntity = SystemEconomyEntity(
                systemId = system.id,
                systemName = system.name,
                economyType = system.economy,
                economyName = system.economyName,
                governmentType = system.government,
                techLevel = system.techLevel,
                marketCycle = 1,
                activeBoomState = initialCondition.code,
                boomDescription = initialCondition.description,
                tradeTaxPercent = initialCondition.taxRate
            )
            marketDao.insertEconomy(econEntity)
        }

        val condition = EconomyCondition.fromCode(econEntity.activeBoomState)

        if (existingCommodities.isEmpty()) {
            val freshMarket = MarketEngine.createMarketForSystem(
                system = system,
                marketSeed = (system.seed0 and 0xFF),
                condition = condition
            )

            val entities = freshMarket.map { item ->
                SystemCommodityEntity(
                    systemId = system.id,
                    commodityId = item.id,
                    name = item.name,
                    unit = item.units,
                    basePriceDeciCr = item.basePrice * 4,
                    currentPriceDeciCr = item.price,
                    previousPriceDeciCr = item.price,
                    availableQty = item.quantity,
                    demandMultiplier = MarketEngine.getCategoryModifier(item.id, condition),
                    lastFluctuationCycle = 1
                )
            }
            marketDao.insertCommodities(entities)

            // Record initial history points
            val historyRecords = freshMarket.map { item ->
                MarketPriceHistoryEntity(
                    systemId = system.id,
                    systemName = system.name,
                    commodityId = item.id,
                    commodityName = item.name,
                    priceDeciCr = item.price,
                    cycle = 1,
                    economicState = condition.code
                )
            }
            marketDao.insertPriceHistoryBatch(historyRecords)

            return@withContext freshMarket
        } else {
            return@withContext existingCommodities.map { entity ->
                val base = MarketEngine.BASE_ITEMS.find { it.id == entity.commodityId }
                    ?: MarketEngine.BASE_ITEMS[0]
                base.copy(
                    price = entity.currentPriceDeciCr,
                    quantity = entity.availableQty
                )
            }
        }
    }

    /**
     * Advance planetary economic cycle: Simulates market price fluctuations, random economic shocks,
     * supply shifts, and records price historical logs in Room.
     * Called on hyperspace jump or time advance.
     */
    suspend fun advanceEconomicCycle(
        system: SystemData,
        forceNewCondition: EconomyCondition? = null
    ): SystemEconomyEntity = withContext(ioDispatcher) {
        val currentEcon = marketDao.getEconomyForSystemOnce(system.id)
        val nextCycle = (currentEcon?.marketCycle ?: 0) + 1

        val condition = forceNewCondition ?: EconomyCondition.pickRandomForSystem(system, nextCycle)

        val updatedEconomy = SystemEconomyEntity(
            systemId = system.id,
            systemName = system.name,
            economyType = system.economy,
            economyName = system.economyName,
            governmentType = system.government,
            techLevel = system.techLevel,
            marketCycle = nextCycle,
            activeBoomState = condition.code,
            boomDescription = condition.description,
            tradeTaxPercent = condition.taxRate,
            lastJumpTimestamp = System.currentTimeMillis()
        )
        marketDao.insertEconomy(updatedEconomy)

        // Seed variance based on cycle and system coordinates
        val cycleSeed = (system.seed0 + (nextCycle * 41) + system.x * 3) and 0xFF
        val computedMarket = MarketEngine.createMarketForSystem(
            system = system,
            marketSeed = cycleSeed,
            condition = condition
        )

        val existingEntities = marketDao.getCommoditiesForSystemOnce(system.id)
            .associateBy { it.commodityId }

        val updatedEntities = computedMarket.map { item ->
            val prev = existingEntities[item.id]
            val prevPrice = prev?.currentPriceDeciCr ?: item.price
            
            // Subtle random noise fluctuation +/- 5%
            val jitter = 1.0f + (Random.nextFloat() * 0.10f - 0.05f)
            val fluctuatingPrice = (item.price * jitter).roundToInt().coerceAtLeast(4)

            SystemCommodityEntity(
                id = prev?.id ?: 0,
                systemId = system.id,
                commodityId = item.id,
                name = item.name,
                unit = item.units,
                basePriceDeciCr = item.basePrice * 4,
                currentPriceDeciCr = fluctuatingPrice,
                previousPriceDeciCr = prevPrice,
                availableQty = item.quantity,
                demandMultiplier = MarketEngine.getCategoryModifier(item.id, condition),
                lastFluctuationCycle = nextCycle,
                updatedAtTimestamp = System.currentTimeMillis()
            )
        }

        marketDao.insertCommodities(updatedEntities)

        // Append historical records
        val historyRecords = updatedEntities.map { entity ->
            MarketPriceHistoryEntity(
                systemId = system.id,
                systemName = system.name,
                commodityId = entity.commodityId,
                commodityName = entity.name,
                priceDeciCr = entity.currentPriceDeciCr,
                cycle = nextCycle,
                economicState = condition.code,
                timestamp = System.currentTimeMillis()
            )
        }
        marketDao.insertPriceHistoryBatch(historyRecords)

        return@withContext updatedEconomy
    }

    /**
     * Update stock after trading transaction (Buy or Sell).
     */
    suspend fun updateStock(systemId: Int, commodityId: Int, newQuantity: Int) = withContext(ioDispatcher) {
        marketDao.updateQuantity(systemId, commodityId, newQuantity.coerceAtLeast(0))
    }
}
