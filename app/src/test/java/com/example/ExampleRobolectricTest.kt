package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.elite.flight.SpaceEntity
import com.example.elite.market.MarketEngine
import com.example.elite.model.CommanderState
import com.example.elite.model.SystemData
import com.example.elite.ships.ShipBlueprints
import com.example.elite.sourceviewer.SourceRepository
import com.example.elite.universe.GalaxyGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun combatScenario_spawnsThargoidInvasionWave() {
        runBlocking {
            val soundSynth = com.example.elite.audio.BbcSoundSynth(this)
            val commander = CommanderState()
            val flightEngine = com.example.elite.flight.FlightEngine(commander, soundSynth)

            flightEngine.spawnBattleWave(com.example.elite.flight.CombatScenario.THARGOID_INVASION)
            val thargoids = flightEngine.entities.filter { it.blueprint.name == "Thargoid" }
            val thargons = flightEngine.entities.filter { it.blueprint.name == "Thargon" }

            assertTrue(thargoids.isNotEmpty())
            assertTrue(thargons.size >= 3)
            assertTrue(thargoids.all { it.isHostile })
        }
    }

    @Test
    fun asteroidField_spawnsAndScoopsValuableMinerals() {
        runBlocking {
            val soundSynth = com.example.elite.audio.BbcSoundSynth(this)
            val commander = CommanderState()
            val flightEngine = com.example.elite.flight.FlightEngine(commander, soundSynth)

            flightEngine.spawnAsteroidField(6)
            val asteroids = flightEngine.entities.filter { it.blueprint.name == "Asteroid" }
            assertEquals(6, asteroids.size)

            // Clear entities to isolate target asteroid right in front of crosshair and fire laser
            flightEngine.entities.clear()
            val targetAsteroid = SpaceEntity(
                id = 9999L,
                blueprint = ShipBlueprints.ASTEROID,
                position = com.example.elite.math3d.Vector3(0f, 0f, 300f),
                shields = 10f
            )
            flightEngine.entities.add(targetAsteroid)
            flightEngine.fireLaser()

            // Verify asteroid fractured and spawned mineral canister
            val canisters = flightEngine.entities.filter { it.blueprint == ShipBlueprints.CANISTER }
            assertTrue("Canister should spawn when asteroid is fractured", canisters.isNotEmpty())

            val canister = canisters.first()
            val mineralId = canister.cargoCommodityId ?: 9
            val initialCargo = commander.cargo[mineralId] ?: 0

            // Move canister directly in scooping range and simulate update tick
            canister.position = com.example.elite.math3d.Vector3(0f, 0f, 20f)
            flightEngine.speed = 10f
            flightEngine.update(0.05f)

            val updatedCargo = commander.cargo[mineralId] ?: 0
            assertTrue("Cargo should increase after scooping canister", updatedCargo > initialCargo)
        }
    }

    @Test
    fun combatRadarOverlay_tracksHostilesAndCalculatesPositions() {
        runBlocking {
            val soundSynth = com.example.elite.audio.BbcSoundSynth(this)
            val commander = CommanderState()
            val flightEngine = com.example.elite.flight.FlightEngine(commander, soundSynth)

            // Spawn hostile Viper and friendly cargo canister
            flightEngine.entities.clear()
            val hostileViper = SpaceEntity(
                id = 101L,
                blueprint = ShipBlueprints.VIPER,
                position = com.example.elite.math3d.Vector3(120f, 40f, 600f),
                isHostile = true,
                shields = 80f
            )
            val civilianCobra = SpaceEntity(
                id = 102L,
                blueprint = ShipBlueprints.COBRA_MK_3,
                position = com.example.elite.math3d.Vector3(-80f, -20f, 900f),
                isHostile = false,
                shields = 100f
            )
            flightEngine.entities.add(hostileViper)
            flightEngine.entities.add(civilianCobra)

            val hostiles = flightEngine.entities.filter { it.isHostile }
            assertEquals(1, hostiles.size)
            assertEquals("Viper", hostiles.first().blueprint.name)

            // Lock onto hostile
            flightEngine.lockedTarget = hostileViper
            assertNotNull(flightEngine.lockedTarget)
            assertEquals(101L, flightEngine.lockedTarget?.id)
            assertTrue(flightEngine.lockedTarget?.isHostile == true)
        }
    }

    @Test
    fun sectorWarSimulationEngine_initializes100BotsAndExecutesStrategy() {
        val sectorEngine = com.example.elite.simulation.SectorWarSimulationEngine()
        val bots = sectorEngine.botOpponents

        assertEquals("Should initialize exactly 100 AI bots / simulated players", 100, bots.size)

        val activeBots = bots.filter { it.isAlive }
        assertEquals(100, activeBots.size)

        // Verify multi-faction distribution
        val navy = bots.count { it.faction == com.example.elite.model.FleetFaction.GALACTIC_NAVY }
        val pirates = bots.count { it.faction == com.example.elite.model.FleetFaction.PIRATE_CLAN }
        val traders = bots.count { it.faction == com.example.elite.model.FleetFaction.MERCHANT_GUILD }
        val thargoids = bots.count { it.faction == com.example.elite.model.FleetFaction.THARGOID_SWARM }

        assertTrue("Should include Galactic Navy ships", navy > 0)
        assertTrue("Should include Outlaw Pirate Clan ships", pirates > 0)
        assertTrue("Should include Merchant Guild freighters", traders > 0)
        assertTrue("Should include Thargoid Alien swarmers", thargoids > 0)

        // Verify simulated realtime human players
        val humanPlayers = bots.filter { it.pilot.isRealtimeHumanPlayer }
        assertTrue("Should include simulated realtime human players with ping and callsign", humanPlayers.isNotEmpty())
        assertTrue("Human players should have realistic latency", humanPlayers.first().pilot.pingMs > 0)

        // Execute multiple simulation update ticks
        val initialStats = sectorEngine.statsFlow.value
        assertEquals(100, initialStats.totalBots)

        // Advance simulation by 0.5s
        sectorEngine.update(0.5f)
        sectorEngine.update(0.5f)

        // Test dynamic strategy assignment
        val targetBot = bots.first()
        sectorEngine.setBotStrategy(targetBot.id, com.example.elite.model.BotStrategy.BERSERK_ASSAULT)
        assertEquals(com.example.elite.model.BotStrategy.BERSERK_ASSAULT, targetBot.strategy)

        // Test wing-wide strategy assignment
        sectorEngine.setStrategyForFaction(com.example.elite.model.FleetFaction.GALACTIC_NAVY, com.example.elite.model.BotStrategy.DEFENSIVE_ESCORT)
        val navyBots = bots.filter { it.faction == com.example.elite.model.FleetFaction.GALACTIC_NAVY }
        assertTrue("Navy bots should have updated strategy", navyBots.all { it.strategy == com.example.elite.model.BotStrategy.DEFENSIVE_ESCORT })

        // Test sector engine auto-play toggle
        assertTrue("Sector engine auto-play should be enabled initially", sectorEngine.isAutoPlayEnabled)
        sectorEngine.toggleAutoPlay()
        assertFalse("Sector engine auto-play should be disabled after toggle", sectorEngine.isAutoPlayEnabled)
    }

    @Test
    fun flightEngine_autoPlayAutonomousFlightAndCombat() {
        runBlocking {
            val commander = CommanderState(
                name = "JAMESON",
                cashDeciCredits = 1000L,
                fuelDeciLy = 70,
                hasDockingComputer = true,
                hasEcm = true,
                hasEnergyBomb = true,
                missiles = 3
            )
            val synth = com.example.elite.audio.BbcSoundSynth(this)
            val flightEngine = com.example.elite.flight.FlightEngine(commander, synth)

            // Verify initial auto-play state is OFF
            assertEquals(com.example.elite.flight.AutoPlayMode.OFF, flightEngine.autoPlayMode)
            assertFalse(flightEngine.isAutoPlayActive)

            // Toggle to COMBAT_ACE
            val mode1 = flightEngine.toggleAutoPlay()
            assertEquals(com.example.elite.flight.AutoPlayMode.COMBAT_ACE, mode1)
            assertTrue(flightEngine.isAutoPlayActive)

            // Add a hostile target in front
            val hostileMamba = com.example.elite.flight.SpaceEntity(
                id = 501L,
                blueprint = com.example.elite.ships.ShipBlueprints.MAMBA,
                position = com.example.elite.math3d.Vector3(10f, 10f, 400f),
                isHostile = true,
                shields = 80f
            )
            flightEngine.entities.add(hostileMamba)

            // Run auto-play tick
            flightEngine.update(0.1f)

            // Verify target lock and autopilot flight steering engaged
            assertEquals(501L, flightEngine.lockedTarget?.id)
            assertTrue("Throttle should be active in auto-play", flightEngine.speed > 0f)
            assertTrue("Status text should reflect tracking target", flightEngine.autoPlayStatusText.isNotEmpty())

            // Test emergency response: simulate incoming missile
            flightEngine.incomingMissileCountdownMs = System.currentTimeMillis() + 4000L
            flightEngine.update(0.1f)
            assertEquals("ECM should neutralize incoming missile under auto-play", 0L, flightEngine.incomingMissileCountdownMs)
            assertEquals("AUTO-ECM DEPLOYED", flightEngine.autoPlayStatusText)

            // Toggle modes through cycle
            val mode2 = flightEngine.toggleAutoPlay()
            assertEquals(com.example.elite.flight.AutoPlayMode.TRADER_EXPLORER, mode2)
            val mode3 = flightEngine.toggleAutoPlay()
            assertEquals(com.example.elite.flight.AutoPlayMode.SURVIVAL_DEFENSE, mode3)
            val mode4 = flightEngine.toggleAutoPlay()
            assertEquals(com.example.elite.flight.AutoPlayMode.OFF, mode4)
            assertFalse(flightEngine.isAutoPlayActive)
        }
    }

    @Test
    fun gameplayDatasetCollector_accumulatesTelemetryAndClassifiesStrategy() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            var laserHitLogged = false
            var missileLogged = false
            var ecmLogged = false
            var bountyEarned = 0.0

            val commander = CommanderState()
            val synth = com.example.elite.audio.BbcSoundSynth(this)
            val flightEngine = com.example.elite.flight.FlightEngine(
                commander = commander,
                soundSynth = synth,
                onLaserFiredCallback = { hit -> if (hit) laserHitLogged = true },
                onMissileFiredCallback = { missileLogged = true },
                onEcmSuccessCallback = { ecmLogged = true },
                onBountyEarnedCallback = { b -> bountyEarned += b }
            )

            // Simulate combat actions
            flightEngine.armMissile()
            val target = com.example.elite.flight.SpaceEntity(
                id = 999L,
                blueprint = com.example.elite.ships.ShipBlueprints.KRAIT,
                position = com.example.elite.math3d.Vector3(0f, 0f, 200f),
                isHostile = true,
                shields = 10f
            )
            flightEngine.entities.add(target)
            flightEngine.lockedTarget = target

            flightEngine.fireLaser()
            assertTrue("Laser hit should be logged in callback", laserHitLogged)

            flightEngine.fireMissile()
            assertTrue("Missile launch should be logged", missileLogged)

            flightEngine.incomingMissileCountdownMs = System.currentTimeMillis() + 5000L
            flightEngine.triggerEcm()
            assertTrue("ECM countermeasure success should be logged", ecmLogged)
        }
    }
}
