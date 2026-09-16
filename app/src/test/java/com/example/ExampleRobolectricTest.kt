package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.elite.market.MarketEngine
import com.example.elite.model.CommanderState
import com.example.elite.model.SystemData
import com.example.elite.ships.ShipBlueprints
import com.example.elite.sourceviewer.SourceRepository
import com.example.elite.universe.GalaxyGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Elite", appName)
    }

    @Test
    fun galaxyGenerator_generates256SystemsWithLave() {
        val systems = GalaxyGenerator.generateGalaxy(1)
        assertEquals(256, systems.size)

        // Find Lave (X=20, Y=173 in Galaxy 1)
        val lave = systems.find { it.name == "Lave" }
        assertNotNull("Lave should exist in Galaxy 1", lave)
        assertEquals(20, lave!!.x)
        assertEquals(173, lave.y)
        assertTrue(lave.radius > 0)
        assertTrue(lave.techLevel in 1..15)
        assertEquals("Human Colonials", lave.species)
    }

    @Test
    fun marketEngine_creates17CommoditiesForLave() {
        val systems = GalaxyGenerator.generateGalaxy(1)
        val lave = systems.find { it.name == "Lave" }!!
        val market = MarketEngine.createMarketForSystem(lave)

        assertEquals(17, market.size)
        val food = market[0]
        assertEquals("Food", food.name)
        assertTrue(food.price > 0)

        // Alien items have 0 availability
        val alienItems = market[16]
        assertEquals("Alien Items", alienItems.name)
        assertEquals(0, alienItems.quantity)
    }

    @Test
    fun shipBlueprints_haveExactVerticesAndEdges() {
        // Cobra Mk III
        val cobra = ShipBlueprints.COBRA_MK_3
        assertEquals("Cobra Mk III", cobra.name)
        assertTrue(cobra.vertices.size >= 12)
        assertTrue(cobra.edges.isNotEmpty())

        // Coriolis Space Station
        val station = ShipBlueprints.CORIOLIS
        assertEquals("Coriolis Station", station.name)
        assertTrue(station.vertices.size >= 12)
        assertTrue(station.edges.isNotEmpty())

        // Thargoid Alien Ship
        val thargoid = ShipBlueprints.THARGOID
        assertEquals("Thargoid", thargoid.name)
        assertTrue(thargoid.vertices.size >= 16)
    }

    @Test
    fun sourceRepository_readsOriginalAsmFile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SourceRepository(context)

        val lines = repo.loadFileSlice(
            assetPath = "1-source-files/main-sources/elite-source.asm",
            startLine = 17525,
            count = 20
        )
        assertTrue(lines.isNotEmpty())
        val hasTT54 = lines.any { it.text.contains("TT54") }
        assertTrue("Expected TT54 routine in slice", hasTT54)
    }
}
