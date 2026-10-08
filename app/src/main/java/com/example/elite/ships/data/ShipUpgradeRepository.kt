package com.example.elite.ships.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository mediating between Room Database and Game Engine for Ship Upgrades.
 */
class ShipUpgradeRepository(
    private val shipUpgradeDao: ShipUpgradeDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun observeShipUpgrade(): Flow<ShipUpgradeEntity?> {
        return shipUpgradeDao.observeShipUpgrade(1L)
    }

    suspend fun getOrInitShipUpgrade(): ShipUpgradeEntity = withContext(ioDispatcher) {
        val existing = shipUpgradeDao.getShipUpgrade(1L)
        if (existing != null) {
            existing
        } else {
            val defaultShip = ShipUpgradeEntity.defaultCobraMk3()
            shipUpgradeDao.insertOrUpdate(defaultShip)
            defaultShip
        }
    }

    suspend fun saveShipUpgrade(entity: ShipUpgradeEntity) = withContext(ioDispatcher) {
        shipUpgradeDao.insertOrUpdate(entity.copy(lastUpgradeTimestamp = System.currentTimeMillis()))
    }

    suspend fun purchaseHullUpgrade(targetLevel: Int): ShipUpgradeEntity = withContext(ioDispatcher) {
        val current = getOrInitShipUpgrade()
        val tier = ShipUpgradeCatalog.HULL_TIERS.find { it.level == targetLevel }
            ?: return@withContext current

        val updated = current.copy(
            hullReinforcementLevel = tier.level,
            maxHullIntegrity = tier.maxHull,
            currentHullIntegrity = (current.currentHullIntegrity + (tier.maxHull - current.maxHullIntegrity)).coerceAtMost(tier.maxHull),
            armorRatingPercent = tier.armorPercent,
            totalCreditsInvestedDeciCr = current.totalCreditsInvestedDeciCr + tier.priceDeciCredits,
            lastUpgradeTimestamp = System.currentTimeMillis()
        )
        shipUpgradeDao.insertOrUpdate(updated)
        updated
    }

    suspend fun repairHull(): ShipUpgradeEntity = withContext(ioDispatcher) {
        val current = getOrInitShipUpgrade()
        val cost = ShipUpgradeCatalog.calculateRepairCostDeciCr(current.currentHullIntegrity, current.maxHullIntegrity)
        val updated = current.copy(
            currentHullIntegrity = current.maxHullIntegrity,
            totalCreditsInvestedDeciCr = current.totalCreditsInvestedDeciCr + cost,
            lastUpgradeTimestamp = System.currentTimeMillis()
        )
        shipUpgradeDao.insertOrUpdate(updated)
        updated
    }

    suspend fun damageHull(damageAmount: Float): ShipUpgradeEntity = withContext(ioDispatcher) {
        val current = getOrInitShipUpgrade()
        val armorMitigation = current.armorRatingPercent / 100f
        val effectiveDmg = damageAmount * (1f - armorMitigation)
        val newHull = (current.currentHullIntegrity - effectiveDmg).coerceAtLeast(0f)
        val updated = current.copy(
            currentHullIntegrity = newHull,
            lastUpgradeTimestamp = System.currentTimeMillis()
        )
        shipUpgradeDao.insertOrUpdate(updated)
        updated
    }

    suspend fun purchaseCargoUpgrade(targetTier: Int): ShipUpgradeEntity = withContext(ioDispatcher) {
        val current = getOrInitShipUpgrade()
        val tier = ShipUpgradeCatalog.CARGO_TIERS.find { it.tier == targetTier }
            ?: return@withContext current

        val updated = current.copy(
            cargoCapacityTier = tier.tier,
            cargoHoldMaxTonnes = tier.maxTonnes,
            totalCreditsInvestedDeciCr = current.totalCreditsInvestedDeciCr + tier.priceDeciCredits,
            lastUpgradeTimestamp = System.currentTimeMillis()
        )
        shipUpgradeDao.insertOrUpdate(updated)
        updated
    }

    suspend fun purchaseFrontWeapon(code: String): ShipUpgradeEntity = withContext(ioDispatcher) {
        val current = getOrInitShipUpgrade()
        val spec = ShipUpgradeCatalog.FRONT_WEAPONS.find { it.code.equals(code, ignoreCase = true) }
            ?: return@withContext current

        val updated = current.copy(
            frontWeapon = spec.code,
            weaponPowerRating = spec.power,
            totalCreditsInvestedDeciCr = current.totalCreditsInvestedDeciCr + spec.priceDeciCredits,
            lastUpgradeTimestamp = System.currentTimeMillis()
        )
        shipUpgradeDao.insertOrUpdate(updated)
        updated
    }

    suspend fun purchaseRearWeapon(code: String): ShipUpgradeEntity = withContext(ioDispatcher) {
        val current = getOrInitShipUpgrade()
        val spec = ShipUpgradeCatalog.REAR_WEAPONS.find { it.code.equals(code, ignoreCase = true) }
            ?: return@withContext current

        val rearVal = if (spec.code == "NONE") null else spec.code
        val updated = current.copy(
            rearWeapon = rearVal,
            totalCreditsInvestedDeciCr = current.totalCreditsInvestedDeciCr + spec.priceDeciCredits,
            lastUpgradeTimestamp = System.currentTimeMillis()
        )
        shipUpgradeDao.insertOrUpdate(updated)
        updated
    }

    suspend fun purchaseCoolingUpgrade(targetTier: Int): ShipUpgradeEntity = withContext(ioDispatcher) {
        val current = getOrInitShipUpgrade()
        val tier = ShipUpgradeCatalog.COOLING_TIERS.find { it.tier == targetTier }
            ?: return@withContext current

        val updated = current.copy(
            weaponCoolingTier = tier.tier,
            weaponCoolingMultiplier = tier.multiplier,
            totalCreditsInvestedDeciCr = current.totalCreditsInvestedDeciCr + tier.priceDeciCredits,
            lastUpgradeTimestamp = System.currentTimeMillis()
        )
        shipUpgradeDao.insertOrUpdate(updated)
        updated
    }

    suspend fun purchaseMissilePylons(targetTier: Int): ShipUpgradeEntity = withContext(ioDispatcher) {
        val current = getOrInitShipUpgrade()
        val tier = ShipUpgradeCatalog.MISSILE_PYLON_TIERS.find { it.tier == targetTier }
            ?: return@withContext current

        val updated = current.copy(
            missileCapacity = tier.capacity,
            totalCreditsInvestedDeciCr = current.totalCreditsInvestedDeciCr + tier.priceDeciCredits,
            lastUpgradeTimestamp = System.currentTimeMillis()
        )
        shipUpgradeDao.insertOrUpdate(updated)
        updated
    }

    suspend fun armMissile(priceDeciCr: Long = 300L): ShipUpgradeEntity = withContext(ioDispatcher) {
        val current = getOrInitShipUpgrade()
        if (current.missilesArmed >= current.missileCapacity) return@withContext current

        val updated = current.copy(
            missilesArmed = current.missilesArmed + 1,
            totalCreditsInvestedDeciCr = current.totalCreditsInvestedDeciCr + priceDeciCr,
            lastUpgradeTimestamp = System.currentTimeMillis()
        )
        shipUpgradeDao.insertOrUpdate(updated)
        updated
    }

    suspend fun fireMissile(): ShipUpgradeEntity = withContext(ioDispatcher) {
        val current = getOrInitShipUpgrade()
        if (current.missilesArmed <= 0) return@withContext current
        val updated = current.copy(
            missilesArmed = current.missilesArmed - 1,
            lastUpgradeTimestamp = System.currentTimeMillis()
        )
        shipUpgradeDao.insertOrUpdate(updated)
        updated
    }

    suspend fun purchaseEquipment(equipId: String, costDeciCr: Long): ShipUpgradeEntity = withContext(ioDispatcher) {
        val current = getOrInitShipUpgrade()
        val updated = when (equipId) {
            "ecm" -> current.copy(hasEcm = true)
            "scoops" -> current.copy(hasFuelScoops = true)
            "docking_comp" -> current.copy(hasDockingComputer = true)
            "galactic_hyper" -> current.copy(hasGalacticHyperdrive = true)
            "energy_bomb" -> current.copy(hasEnergyBomb = true)
            "escape_pod" -> current.copy(hasEscapePod = true)
            "naval_energy" -> current.copy(hasNavalEnergyUnit = true)
            else -> current
        }.copy(
            totalCreditsInvestedDeciCr = current.totalCreditsInvestedDeciCr + costDeciCr,
            lastUpgradeTimestamp = System.currentTimeMillis()
        )
        shipUpgradeDao.insertOrUpdate(updated)
        updated
    }

    suspend fun resetToDefaults(): ShipUpgradeEntity = withContext(ioDispatcher) {
        val defaultShip = ShipUpgradeEntity.defaultCobraMk3()
        shipUpgradeDao.insertOrUpdate(defaultShip)
        defaultShip
    }
}
