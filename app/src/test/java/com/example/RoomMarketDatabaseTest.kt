package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.elite.market.EconomyCondition
import com.example.elite.market.MarketEngine
import com.example.elite.market.data.MarketDatabase
import com.example.elite.market.data.MarketRepository
import com.example.elite.universe.GalaxyGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RoomMarketDatabaseTest {

    private lateinit var database: MarketDatabase
    private lateinit var repository: MarketRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MarketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MarketRepository(database.marketDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testMarketDatabaseInitializationAndPersistence() = runBlocking {
        val galaxy = GalaxyGenerator.generateGalaxy(1)
        val lave = galaxy.find { it.name == "Lave" } ?: galaxy[0]

        // Initialize market
        val initialMarket = repository.getOrInitializeMarket(lave)
        assertEquals(17, initialMarket.size)

        // Verify entities persisted in Room
        val persistedEntities = repository.observeCommodityEntities(lave.id).first()
        assertEquals(17, persistedEntities.size)

        val foodEntity = persistedEntities.find { it.name == "Food" }
        assertNotNull(foodEntity)
        assertTrue("Food price should be positive", foodEntity!!.currentPriceDeciCr > 0)
    }

    @Test
    fun testEconomicCycleAdvancementAndPriceFluctuation() = runBlocking {
        val galaxy = GalaxyGenerator.generateGalaxy(1)
        val lave = galaxy.find { it.name == "Lave" } ?: galaxy[0]

        // 1. Initial market
        repository.getOrInitializeMarket(lave)
        val initialEntities = repository.observeCommodityEntities(lave.id).first()
        val foodBefore = initialEntities.find { it.name == "Food" }!!.currentPriceDeciCr

        // 2. Advance economic cycle with a severe famine / drought condition
        val updatedEcon = repository.advanceEconomicCycle(lave, forceNewCondition = EconomyCondition.DROUGHT)
        assertEquals("DROUGHT", updatedEcon.activeBoomState)
        assertEquals(2, updatedEcon.marketCycle)

        // 3. Verify prices reacted to the planetary economy shock
        val updatedEntities = repository.observeCommodityEntities(lave.id).first()
        val foodAfter = updatedEntities.find { it.name == "Food" }!!.currentPriceDeciCr

        assertTrue("Drought should significantly increase food price ($foodAfter > $foodBefore)", foodAfter > foodBefore)

        // 4. Verify historical price logs exist in Room
        val foodHistory = repository.observePriceHistory(lave.id, 0).first()
        assertTrue("Price history should have at least 2 entries", foodHistory.size >= 2)
    }

    @Test
    fun testCommodityStockUpdateInRoom() = runBlocking {
        val galaxy = GalaxyGenerator.generateGalaxy(1)
        val lave = galaxy.find { it.name == "Lave" } ?: galaxy[0]

        repository.getOrInitializeMarket(lave)
        repository.updateStock(lave.id, 0, 42)

        val entities = repository.observeCommodityEntities(lave.id).first()
        val food = entities.find { it.commodityId == 0 }
        assertNotNull(food)
        assertEquals(42, food!!.availableQty)
    }
}
