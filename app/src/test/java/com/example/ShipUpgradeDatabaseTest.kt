package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.elite.market.data.MarketDatabase
import com.example.elite.ships.data.ShipUpgradeCatalog
import com.example.elite.ships.data.ShipUpgradeDao
import com.example.elite.ships.data.ShipUpgradeEntity
import com.example.elite.ships.data.ShipUpgradeRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ShipUpgradeDatabaseTest {

    private lateinit var database: MarketDatabase
    private lateinit var dao: ShipUpgradeDao
    private lateinit var repository: ShipUpgradeRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MarketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.shipUpgradeDao()
        repository = ShipUpgradeRepository(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testDefaultShipUpgradeInitialization() = runBlocking {
        // Initial query should bootstrap default Cobra Mk III
        val ship = repository.getOrInitShipUpgrade()
        assertNotNull(ship)
        assertEquals("Cobra Mk III", ship.shipName)
        assertEquals(0, ship.hullReinforcementLevel)
        assertEquals(100f, ship.maxHullIntegrity, 0.01f)
        assertEquals(100f, ship.currentHullIntegrity, 0.01f)
        assertEquals(0, ship.armorRatingPercent)
        assertEquals(0, ship.cargoCapacityTier)
        assertEquals(20, ship.cargoHoldMaxTonnes)
        assertEquals("PULSE", ship.frontWeapon)
        assertNull(ship.rearWeapon)
        assertEquals(4, ship.missileCapacity)
        assertEquals(3, ship.missilesArmed)

        // Verify Flow emits the persisted entity
        val observed = repository.observeShipUpgrade().first()
        assertNotNull(observed)
        assertEquals(1L, observed?.id)
        assertEquals(20, observed?.cargoHoldMaxTonnes)
    }

    @Test
    fun testHullReinforcementUpgradesAndArmorScaling() = runBlocking {
        repository.getOrInitShipUpgrade()

        // Purchase Level 2 Composite Military Plating (150 HP, 20% Armor)
        val upgradedLvl2 = repository.purchaseHullUpgrade(2)
        assertEquals(2, upgradedLvl2.hullReinforcementLevel)
        assertEquals(150f, upgradedLvl2.maxHullIntegrity, 0.01f)
        assertEquals(150f, upgradedLvl2.currentHullIntegrity, 0.01f)
        assertEquals(20, upgradedLvl2.armorRatingPercent)
        assertEquals(6000L, upgradedLvl2.totalCreditsInvestedDeciCr)

        // Upgrade further to Level 5 Dreadnought Carapace (280 HP, 50% Armor)
        val upgradedLvl5 = repository.purchaseHullUpgrade(5)
        assertEquals(5, upgradedLvl5.hullReinforcementLevel)
        assertEquals(280f, upgradedLvl5.maxHullIntegrity, 0.01f)
        assertEquals(50, upgradedLvl5.armorRatingPercent)
        assertEquals(56000L, upgradedLvl5.totalCreditsInvestedDeciCr)

        // Verify persistence from DAO
        val fromDao = dao.getShipUpgrade(1L)
        assertNotNull(fromDao)
        assertEquals(5, fromDao?.hullReinforcementLevel)
        assertEquals(280f, fromDao?.maxHullIntegrity ?: 0f, 0.01f)
    }

    @Test
    fun testHullDamageAbsorptionAndRepairService() = runBlocking {
        repository.getOrInitShipUpgrade()

        // Upgrade to Level 2 (20% armor mitigation, 150 max HP)
        repository.purchaseHullUpgrade(2)

        // Deal 50 incoming raw damage -> 20% mitigated = 40 damage taken
        val damaged = repository.damageHull(50f)
        assertEquals(110f, damaged.currentHullIntegrity, 0.01f)
        assertTrue(damaged.isHullDamaged())

        // Calculate repair cost: 40 missing HP * 2 deci-credits = 80 deci-credits (8.0 CR)
        val repairCost = ShipUpgradeCatalog.calculateRepairCostDeciCr(damaged.currentHullIntegrity, damaged.maxHullIntegrity)
        assertEquals(80L, repairCost)

        // Perform repair
        val repaired = repository.repairHull()
        assertEquals(150f, repaired.currentHullIntegrity, 0.01f)
        assertFalse(repaired.isHullDamaged())
    }

    @Test
    fun testCargoCapacityExpansionTiers() = runBlocking {
        repository.getOrInitShipUpgrade()

        // Tier 1: Large Cargo Bay (30t)
        val tier1 = repository.purchaseCargoUpgrade(1)
        assertEquals(1, tier1.cargoCapacityTier)
        assertEquals(30, tier1.cargoHoldMaxTonnes)

        // Tier 3: Pressurized Freighter Bay (65t)
        val tier3 = repository.purchaseCargoUpgrade(3)
        assertEquals(3, tier3.cargoCapacityTier)
        assertEquals(65, tier3.cargoHoldMaxTonnes)

        // Tier 5: Titan Heavy Cargo Hold (120t)
        val tier5 = repository.purchaseCargoUpgrade(5)
        assertEquals(5, tier5.cargoCapacityTier)
        assertEquals(120, tier5.cargoHoldMaxTonnes)

        // Verify from database
        val persisted = dao.getShipUpgrade(1L)
        assertEquals(120, persisted?.cargoHoldMaxTonnes)
    }

    @Test
    fun testWeaponSystemsAndCoolingUpgrades() = runBlocking {
        repository.getOrInitShipUpgrade()

        // Upgrade front weapon to Military Laser
        val withMilitary = repository.purchaseFrontWeapon("MILITARY")
        assertEquals("MILITARY", withMilitary.frontWeapon)
        assertEquals(60, withMilitary.weaponPowerRating)

        // Install Rear Beam Laser
        val withRear = repository.purchaseRearWeapon("BEAM")
        assertEquals("BEAM", withRear.rearWeapon)

        // Upgrade cooling to Tier 2 (Liquid Nitrogen Core, 2.0x multiplier)
        val withCooling = repository.purchaseCoolingUpgrade(2)
        assertEquals(2, withCooling.weaponCoolingTier)
        assertEquals(2.0f, withCooling.weaponCoolingMultiplier, 0.01f)

        // Upgrade missile rack to Hex Pylons (6x capacity) and arm a missile
        val withPylons = repository.purchaseMissilePylons(1)
        assertEquals(6, withPylons.missileCapacity)

        val armed = repository.armMissile(300L)
        assertEquals(4, armed.missilesArmed)

        // Fire a missile
        val fired = repository.fireMissile()
        assertEquals(3, fired.missilesArmed)

        // Verify in Room database
        val persisted = dao.getShipUpgrade(1L)
        assertNotNull(persisted)
        assertEquals("MILITARY", persisted?.frontWeapon)
        assertEquals("BEAM", persisted?.rearWeapon)
        assertEquals(2.0f, persisted?.weaponCoolingMultiplier ?: 0f, 0.01f)
        assertEquals(6, persisted?.missileCapacity)
    }

    @Test
    fun testEquipmentInstallation() = runBlocking {
        repository.getOrInitShipUpgrade()

        val withEcm = repository.purchaseEquipment("ecm", 6000L)
        assertTrue(withEcm.hasEcm)

        val withDocking = repository.purchaseEquipment("docking_comp", 15000L)
        assertTrue(withDocking.hasDockingComputer)

        val withHyper = repository.purchaseEquipment("galactic_hyper", 50000L)
        assertTrue(withHyper.hasGalacticHyperdrive)

        val checkDb = dao.getShipUpgrade(1L)
        assertTrue(checkDb?.hasEcm == true)
        assertTrue(checkDb?.hasDockingComputer == true)
        assertTrue(checkDb?.hasGalacticHyperdrive == true)
    }
}
