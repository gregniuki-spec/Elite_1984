package com.example.elite.ships.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for persisting and querying ship upgrade states.
 */
@Dao
interface ShipUpgradeDao {

    @Query("SELECT * FROM ship_upgrades WHERE id = :id LIMIT 1")
    fun observeShipUpgrade(id: Long = 1L): Flow<ShipUpgradeEntity?>

    @Query("SELECT * FROM ship_upgrades WHERE id = :id LIMIT 1")
    suspend fun getShipUpgrade(id: Long = 1L): ShipUpgradeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(upgrade: ShipUpgradeEntity)

    @Update
    suspend fun update(upgrade: ShipUpgradeEntity)

    @Query(
        """
        UPDATE ship_upgrades 
        SET hullReinforcementLevel = :level,
            maxHullIntegrity = :maxHull,
            currentHullIntegrity = :currentHull,
            armorRatingPercent = :armor,
            totalCreditsInvestedDeciCr = totalCreditsInvestedDeciCr + :costDeciCr,
            lastUpgradeTimestamp = :timestamp
        WHERE id = :id
        """
    )
    suspend fun updateHullReinforcement(
        id: Long = 1L,
        level: Int,
        maxHull: Float,
        currentHull: Float,
        armor: Int,
        costDeciCr: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE ship_upgrades 
        SET currentHullIntegrity = :repairedHull,
            totalCreditsInvestedDeciCr = totalCreditsInvestedDeciCr + :repairCostDeciCr,
            lastUpgradeTimestamp = :timestamp
        WHERE id = :id
        """
    )
    suspend fun repairHullIntegrity(
        id: Long = 1L,
        repairedHull: Float,
        repairCostDeciCr: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE ship_upgrades 
        SET cargoCapacityTier = :tier,
            cargoHoldMaxTonnes = :tonnes,
            totalCreditsInvestedDeciCr = totalCreditsInvestedDeciCr + :costDeciCr,
            lastUpgradeTimestamp = :timestamp
        WHERE id = :id
        """
    )
    suspend fun updateCargoCapacity(
        id: Long = 1L,
        tier: Int,
        tonnes: Int,
        costDeciCr: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE ship_upgrades 
        SET frontWeapon = :front,
            rearWeapon = :rear,
            weaponPowerRating = :power,
            weaponCoolingTier = :coolingTier,
            weaponCoolingMultiplier = :coolingMult,
            totalCreditsInvestedDeciCr = totalCreditsInvestedDeciCr + :costDeciCr,
            lastUpgradeTimestamp = :timestamp
        WHERE id = :id
        """
    )
    suspend fun updateWeaponSystems(
        id: Long = 1L,
        front: String,
        rear: String?,
        power: Int,
        coolingTier: Int,
        coolingMult: Float,
        costDeciCr: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE ship_upgrades 
        SET missileCapacity = :capacity,
            missilesArmed = :armed,
            totalCreditsInvestedDeciCr = totalCreditsInvestedDeciCr + :costDeciCr,
            lastUpgradeTimestamp = :timestamp
        WHERE id = :id
        """
    )
    suspend fun updateMissilePylons(
        id: Long = 1L,
        capacity: Int,
        armed: Int,
        costDeciCr: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE ship_upgrades SET missilesArmed = :count WHERE id = :id")
    suspend fun updateMissilesArmed(id: Long = 1L, count: Int)

    @Query("DELETE FROM ship_upgrades")
    suspend fun deleteAll()
}
