package com.example.elite.ships.data

/**
 * Specifications and progression tables for Starship Upgrades.
 */
data class HullTier(
    val level: Int,
    val name: String,
    val description: String,
    val maxHull: Float,
    val armorPercent: Int,
    val priceDeciCredits: Long,
    val minTechLevel: Int
)

data class CargoTier(
    val tier: Int,
    val name: String,
    val description: String,
    val maxTonnes: Int,
    val priceDeciCredits: Long,
    val minTechLevel: Int
)

data class WeaponSpec(
    val code: String,
    val name: String,
    val description: String,
    val power: Int,
    val priceDeciCredits: Long,
    val minTechLevel: Int,
    val isRear: Boolean = false
)

data class CoolingTier(
    val tier: Int,
    val name: String,
    val description: String,
    val multiplier: Float,
    val priceDeciCredits: Long,
    val minTechLevel: Int
)

data class MissilePylonTier(
    val tier: Int,
    val name: String,
    val description: String,
    val capacity: Int,
    val priceDeciCredits: Long,
    val minTechLevel: Int
)

object ShipUpgradeCatalog {

    val HULL_TIERS = listOf(
        HullTier(
            level = 0,
            name = "Standard Lightweight Alloy",
            description = "Stock unarmored hull frame. Standard 100 integrity.",
            maxHull = 100f,
            armorPercent = 0,
            priceDeciCredits = 0L,
            minTechLevel = 1
        ),
        HullTier(
            level = 1,
            name = "Reinforced Bulkheads",
            description = "High-tensile internal struts. +25 Hull, 10% damage deflection.",
            maxHull = 125f,
            armorPercent = 10,
            priceDeciCredits = 2500L, // 250.0 CR
            minTechLevel = 3
        ),
        HullTier(
            level = 2,
            name = "Composite Military Plating",
            description = "Layered ceramic and titanium mesh. +50 Hull, 20% damage deflection.",
            maxHull = 150f,
            armorPercent = 20,
            priceDeciCredits = 6000L, // 600.0 CR
            minTechLevel = 6
        ),
        HullTier(
            level = 3,
            name = "Reactive Armor Matrix",
            description = "Explosive dispersal tiles absorbing kinetic laser shocks. +85 Hull, 30% deflection.",
            maxHull = 185f,
            armorPercent = 30,
            priceDeciCredits = 12000L, // 1200.0 CR
            minTechLevel = 8
        ),
        HullTier(
            level = 4,
            name = "Heavy Duranium Lattice",
            description = "Naval-grade alloy with energy dampening weave. +130 Hull, 40% deflection.",
            maxHull = 230f,
            armorPercent = 40,
            priceDeciCredits = 25000L, // 2500.0 CR
            minTechLevel = 11
        ),
        HullTier(
            level = 5,
            name = "Dreadnought Carapace",
            description = "Near-indestructible capital ship armor coating. +180 Hull, 50% deflection.",
            maxHull = 280f,
            armorPercent = 50,
            priceDeciCredits = 50000L, // 5000.0 CR
            minTechLevel = 13
        )
    )

    val CARGO_TIERS = listOf(
        CargoTier(
            tier = 0,
            name = "Standard Hold (20t)",
            description = "Stock Cobra Mk III factory hold space.",
            maxTonnes = 20,
            priceDeciCredits = 0L,
            minTechLevel = 1
        ),
        CargoTier(
            tier = 1,
            name = "Large Cargo Bay (30t)",
            description = "Standard bay expansion utilizing rear avionics space.",
            maxTonnes = 30,
            priceDeciCredits = 4000L, // 400.0 CR
            minTechLevel = 3
        ),
        CargoTier(
            tier = 2,
            name = "Modular Bulk Racks (45t)",
            description = "Lightweight composite shelving modules. +15t capacity.",
            maxTonnes = 45,
            priceDeciCredits = 12000L, // 1200.0 CR
            minTechLevel = 5
        ),
        CargoTier(
            tier = 3,
            name = "Pressurized Freighter Bay (65t)",
            description = "Atmospheric cargo enclosure for high-volume commercial hauling.",
            maxTonnes = 65,
            priceDeciCredits = 28000L, // 2800.0 CR
            minTechLevel = 7
        ),
        CargoTier(
            tier = 4,
            name = "Deep-Space Hauler Bay (90t)",
            description = "Expanded internal chassis frame supporting massive merchant operations.",
            maxTonnes = 90,
            priceDeciCredits = 65000L, // 6500.0 CR
            minTechLevel = 10
        ),
        CargoTier(
            tier = 5,
            name = "Titan Heavy Cargo Hold (120t)",
            description = "Maximum commercial envelope. Dominates interstellar commodity trade.",
            maxTonnes = 120,
            priceDeciCredits = 140000L, // 14000.0 CR
            minTechLevel = 12
        )
    )

    val FRONT_WEAPONS = listOf(
        WeaponSpec(
            code = "PULSE",
            name = "Pulse Laser",
            description = "Rapid low-draw pulsed coherent beam. Standard defensive weapon.",
            power = 15,
            priceDeciCredits = 4000L, // 400.0 CR
            minTechLevel = 1
        ),
        WeaponSpec(
            code = "BEAM",
            name = "Beam Laser",
            description = "Continuous concentrated thermal laser. Slices through light shields.",
            power = 30,
            priceDeciCredits = 10000L, // 1000.0 CR
            minTechLevel = 4
        ),
        WeaponSpec(
            code = "MINING",
            name = "Mining Laser",
            description = "Heavy industrial resonance beam tuned for fracturing asteroids.",
            power = 45,
            priceDeciCredits = 8000L, // 800.0 CR
            minTechLevel = 5
        ),
        WeaponSpec(
            code = "MILITARY",
            name = "Military Laser",
            description = "Devastating Naval grade emitter. Rapidly vaporizes hostile vessels.",
            power = 60,
            priceDeciCredits = 60000L, // 6000.0 CR
            minTechLevel = 10
        ),
        WeaponSpec(
            code = "PLASMA",
            name = "Plasma Accelerator",
            description = "Experimental high-energy magnetic plasma bolt. Maximum firepower.",
            power = 85,
            priceDeciCredits = 150000L, // 15000.0 CR
            minTechLevel = 12
        )
    )

    val REAR_WEAPONS = listOf(
        WeaponSpec(
            code = "NONE",
            name = "No Rear Mount",
            description = "Empty rear weapons hardpoint.",
            power = 0,
            priceDeciCredits = 0L,
            minTechLevel = 1,
            isRear = true
        ),
        WeaponSpec(
            code = "PULSE",
            name = "Rear Pulse Laser",
            description = "Aft defensive battery to deter pursuing interceptors.",
            power = 15,
            priceDeciCredits = 5000L, // 500.0 CR
            minTechLevel = 2,
            isRear = true
        ),
        WeaponSpec(
            code = "BEAM",
            name = "Rear Beam Laser",
            description = "High-output aft continuous beam for dogfight disengagement.",
            power = 30,
            priceDeciCredits = 12000L, // 1200.0 CR
            minTechLevel = 5,
            isRear = true
        ),
        WeaponSpec(
            code = "MILITARY",
            name = "Rear Military Laser",
            description = "Navy combat rear hardpoint with high shield-penetration.",
            power = 60,
            priceDeciCredits = 75000L, // 7500.0 CR
            minTechLevel = 11,
            isRear = true
        )
    )

    val COOLING_TIERS = listOf(
        CoolingTier(
            tier = 0,
            name = "Standard Radiators",
            description = "Stock thermal dissipation radiators (1.0x rate).",
            multiplier = 1.0f,
            priceDeciCredits = 0L,
            minTechLevel = 1
        ),
        CoolingTier(
            tier = 1,
            name = "Cryo-Conduit System",
            description = "Active cryogenic pipes routing heat from weapon coils (1.5x rate).",
            multiplier = 1.5f,
            priceDeciCredits = 8000L, // 800.0 CR
            minTechLevel = 4
        ),
        CoolingTier(
            tier = 2,
            name = "Liquid Nitrogen Core",
            description = "High-pressure thermal exchangers doubling cooling efficiency (2.0x rate).",
            multiplier = 2.0f,
            priceDeciCredits = 22000L, // 2200.0 CR
            minTechLevel = 7
        ),
        CoolingTier(
            tier = 3,
            name = "Superconducting Grid",
            description = "Zero-resistance thermal radiator matrix. Rapid continuous fire (2.8x rate).",
            multiplier = 2.8f,
            priceDeciCredits = 55000L, // 5500.0 CR
            minTechLevel = 10
        )
    )

    val MISSILE_PYLON_TIERS = listOf(
        MissilePylonTier(
            tier = 0,
            name = "Quad Pylons (4x)",
            description = "Stock 4-bay missile rack.",
            capacity = 4,
            priceDeciCredits = 0L,
            minTechLevel = 1
        ),
        MissilePylonTier(
            tier = 1,
            name = "Hex Pylon Array (6x)",
            description = "Reinforced wing hardpoints for 6 guided missiles.",
            capacity = 6,
            priceDeciCredits = 12000L, // 1200.0 CR
            minTechLevel = 5
        ),
        MissilePylonTier(
            tier = 2,
            name = "Octo Battery (8x)",
            description = "Full military missile salvo rack carrying 8 warheads.",
            capacity = 8,
            priceDeciCredits = 30000L, // 3000.0 CR
            minTechLevel = 8
        )
    )

    /**
     * Calculates repair cost based on missing hull hitpoints.
     */
    fun calculateRepairCostDeciCr(currentHull: Float, maxHull: Float): Long {
        val missing = (maxHull - currentHull).coerceAtLeast(0f)
        return (missing * 2L).toLong() // 2 deci-credits (0.2 CR) per hull HP
    }
}
