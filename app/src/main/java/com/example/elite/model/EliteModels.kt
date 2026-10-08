package com.example.elite.model

/**
 * Core models representing the state, entities, and data structures
 * faithful to the BBC Micro 6502 Elite source code (1984).
 */

enum class GameScreen {
    SPACE_FLIGHT,
    BATTLE_ARENA,
    FLEET_STRATEGY,
    RESOURCE_EXPLORATION,
    FLIGHT_MANUAL,
    SHIP_ENCYCLOPEDIA,
    MARKET_PRICES,
    GALACTIC_CHART,
    SHORT_RANGE_CHART,
    SYSTEM_DATA,
    STATUS,
    INVENTORY,
    EQUIP_SHIP,
    ASM_INSPECTOR,
    AI_DATASET_TRAINING
}

enum class CombatRank(val title: String, val scoreNeeded: Int) {
    HARMLESS("Harmless", 0),
    MOSTLY_HARMLESS("Mostly Harmless", 8),
    POOR("Poor", 16),
    AVERAGE("Average", 32),
    ABOVE_AVERAGE("Above Average", 64),
    COMPETENT("Competent", 128),
    DANGEROUS("Dangerous", 512),
    DEADLY("Deadly", 2560),
    ELITE("--- E L I T E ---", 6400);

    val displayName: String get() = title
}

enum class LegalStatus(val title: String) {
    CLEAN("Clean"),
    OFFENDER("Offender"),
    FUGITIVE("Fugitive");

    val displayName: String get() = title
}

enum class LaserType(val title: String, val power: Int, val price: Int) {
    PULSE("Pulse Laser", 15, 400),
    BEAM("Beam Laser", 30, 1000),
    MINING("Mining Laser", 45, 800),
    MILITARY("Military Laser", 60, 6000);

    val displayName: String get() = title
}

data class SystemData(
    val id: Int,
    val name: String,
    val x: Int,
    val y: Int,
    val economy: Int,          // 0 = Rich Ind, 7 = Poor Agri
    val economyName: String,
    val government: Int,       // 0 = Anarchy, 7 = Corporate State
    val governmentName: String,
    val techLevel: Int,        // 1 to 15
    val population: Float,     // in billions
    val productivity: Int,     // in M CR
    val radius: Int,           // in km
    val species: String,       // Human Colonials or procedural alien
    val seed0: Int,
    val seed1: Int,
    val seed2: Int,
    val planetType: PlanetType = PlanetType.fromSeeds(seed0, seed1, seed2, economy),
    val starType: StarType = StarType.fromSeeds(seed0, seed1, seed2),
    val faction: SystemFaction = SystemFaction.fromGovernment(government, seed1),
    val securityLevel: SecurityLevel = SecurityLevel.fromGovernment(government)
)

data class Commodity(
    val id: Int,
    val name: String,
    val basePrice: Int,        // in deci-credits (tenths of a CR)
    val factor: Int,           // economic gradient factor
    val unit: String,          // "t", "kg", "g"
    val baseQuantity: Int,
    val mask: Int,
    var price: Int = 0,        // calculated dynamically per system
    var quantity: Int = 0      // available in current station market
) {
    val units: String get() = unit
}

data class CommanderState(
    var name: String = "JAMESON",
    var currentGalaxy: Int = 1,
    var currentSystemId: Int = 7,  // 7 = Lave in Galaxy 1
    var targetSystemId: Int = 7,
    var isDocked: Boolean = true,
    var cashDeciCredits: Long = 1000L, // 100.0 CR
    var fuelDeciLy: Int = 70,           // 7.0 Light Years (max 70)
    var legalStatus: LegalStatus = LegalStatus.CLEAN,
    var killCount: Int = 0,
    var combatRank: CombatRank = CombatRank.HARMLESS,
    var cargoHoldMax: Int = 20,         // 20t base (30t or 35t with expansion)
    var cargo: MutableMap<Int, Int> = mutableMapOf(), // commodityId -> units
    var laserFront: LaserType = LaserType.PULSE,
    var laserRear: LaserType? = null,
    var missiles: Int = 3,              // max 4
    var hasEcm: Boolean = false,
    var hasFuelScoops: Boolean = false,
    var hasEnergyUnit: Boolean = false, // Naval energy unit
    var hasDockingComputer: Boolean = false,
    var hasGalacticHyperdrive: Boolean = false,
    var hasCargoBayExpansion: Boolean = false,
    var hasEscapePod: Boolean = false,
    var hasEnergyBomb: Boolean = false,
    var forwardShield: Float = 100f,
    var aftShield: Float = 100f,
    var energyBanks: Float = 100f,       // 4 banks of 25f
    var altitude: Float = 50f,
    var cabinTemp: Float = 10f,
    var laserTemp: Float = 0f,
    var hullReinforcementLevel: Int = 0,
    var maxHullIntegrity: Float = 100f,
    var currentHullIntegrity: Float = 100f,
    var armorRatingPercent: Int = 0,
    var cargoCapacityTier: Int = 0,
    var weaponCoolingTier: Int = 0,
    var weaponCoolingMultiplier: Float = 1.0f,
    var missileCapacity: Int = 4,
    val investments: MutableList<ColonyInvestment> = mutableListOf(),
    val activeContracts: MutableList<StrategicContract> = mutableListOf(),
    var lastDividendEarningsDeciCr: Long = 0L
) {
    var cargoCapacity: Int
        get() = cargoHoldMax
        set(v) { cargoHoldMax = v }

    val cargoHold: MutableMap<Int, Int>
        get() = cargo

    fun currentCargoUsed(): Int {
        // Gold (13), Platinum (14), Gems (15) do not consume tonnes
        return cargo.entries.filter { it.key !in listOf(13, 14, 15) }.sumOf { it.value }
    }

    fun freeCargoSpace(): Int {
        return (cargoCapacity - currentCargoUsed()).coerceAtLeast(0)
    }
}
