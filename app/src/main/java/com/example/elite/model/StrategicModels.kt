package com.example.elite.model

import androidx.compose.ui.graphics.Color
import com.example.elite.market.MarketEngine
import com.example.elite.universe.GalaxyGenerator
import kotlin.math.abs
import kotlin.math.sqrt

enum class PlanetType(
    val displayName: String,
    val description: String,
    val primaryResource: String,
    val wireColor: Color,
    val hasRings: Boolean,
    val atmosphere: String,
    val surfaceTempC: Int,
    val gravityG: Float,
    val mineralYieldFactor: Float
) {
    TERRAN(
        displayName = "Terran Continental",
        description = "Temperate nitrogen-oxygen world featuring arable landmasses, lush oceans, and thriving colonial settlements.",
        primaryResource = "Bio-Organics & Luxuries",
        wireColor = Color(0xFF00FF66), // BBC Green
        hasRings = false,
        atmosphere = "N2/O2 (1.02 atm, Breathable)",
        surfaceTempC = 19,
        gravityG = 1.00f,
        mineralYieldFactor = 1.1f
    ),
    OCEANIC(
        displayName = "Oceanic Glacial",
        description = "Global deep hydrosphere covered by vast tidal currents, archipelago chains, and polar ice shelves.",
        primaryResource = "Hydro-Carbons & Marine Food",
        wireColor = Color(0xFF00FFFF), // BBC Cyan
        hasRings = false,
        atmosphere = "N2/O2/H2O (1.18 atm, Moist)",
        surfaceTempC = 8,
        gravityG = 0.94f,
        mineralYieldFactor = 1.0f
    ),
    DESERT(
        displayName = "Scorched Dune",
        description = "Arid silicate world of shifting equatorial dunes, exposed canyon fault lines, and rich dry lake mineral beds.",
        primaryResource = "Rare Earths & Gold",
        wireColor = Color(0xFFFFCC00), // BBC Yellow/Gold
        hasRings = false,
        atmosphere = "CO2/N2 (0.65 atm, Arid)",
        surfaceTempC = 48,
        gravityG = 0.88f,
        mineralYieldFactor = 1.45f
    ),
    GAS_GIANT(
        displayName = "Banded Jovian Gas Giant",
        description = "Colossal jovian body characterized by turbulent helium-hydrogen currents, great storm vortexes, and ice rings.",
        primaryResource = "Fuel Plasma & Radioactives",
        wireColor = Color(0xFFFF9933), // Jovian Amber
        hasRings = true,
        atmosphere = "H2/He/CH4 (High Pressure)",
        surfaceTempC = -135,
        gravityG = 2.45f,
        mineralYieldFactor = 1.6f
    ),
    VOLCANIC(
        displayName = "Molten Basaltic",
        description = "Hyper-active tectonic world criss-crossed by basaltic fissures, caldera magma lakes, and heavy metal deposits.",
        primaryResource = "Heavy Titanium Alloys",
        wireColor = Color(0xFFFF3333), // BBC Red
        hasRings = false,
        atmosphere = "SO2/CO2 (2.4 atm, Toxic)",
        surfaceTempC = 340,
        gravityG = 1.25f,
        mineralYieldFactor = 1.85f
    ),
    ICE_WORLD(
        displayName = "Glacial Cryo-World",
        description = "Sub-zero world with thick nitrogen-water ice tectonic plates, cryo-geysers, and deep sub-glacial oceans.",
        primaryResource = "Cryo-Fluids & Gem-Stones",
        wireColor = Color(0xFFD4FFFF), // Frost White/Cyan
        hasRings = false,
        atmosphere = "N2/CH4 (0.42 atm, Frigid)",
        surfaceTempC = -85,
        gravityG = 0.72f,
        mineralYieldFactor = 1.35f
    ),
    TOXIC_BARREN(
        displayName = "Corrosive Acidic",
        description = "Dense sulfur cloud cover generating heavy chemical precipitation, high greenhouse heat, and exotic compounds.",
        primaryResource = "Industrial Radioactives & Catalysts",
        wireColor = Color(0xFFDDFF00), // Acid Yellow/Green
        hasRings = false,
        atmosphere = "H2SO4/CO2 (4.1 atm, Corrosive)",
        surfaceTempC = 185,
        gravityG = 1.15f,
        mineralYieldFactor = 1.65f
    ),
    RINGWORLD_CAPITAL(
        displayName = "Ecumenopolis Ringworld",
        description = "High-technology planetary capital encased by an equatorial megastructure orbital ring lattice and orbital shipyards.",
        primaryResource = "Advanced Computers & Cybernetics",
        wireColor = Color(0xFFCC66FF), // Imperial Purple/Magenta
        hasRings = true,
        atmosphere = "Atmospherically Regulated (1.0 atm)",
        surfaceTempC = 22,
        gravityG = 1.05f,
        mineralYieldFactor = 1.3f
    );

    companion object {
        fun fromSeeds(seed0: Int, seed1: Int, seed2: Int, economy: Int): PlanetType {
            val hash = ((seed0 xor (seed1 shl 3)) + seed2 + economy).let { if (it < 0) -it else it }
            val types = entries.toTypedArray()
            return types[hash % types.size]
        }
    }
}

enum class StarType(
    val displayName: String,
    val spectralClass: String,
    val stellarColor: Color,
    val solarScoopRate: Float,
    val radiationLevel: String
) {
    O_BLUE_SUPERGIANT("Blue Supergiant", "Class O", Color(0xFF4488FF), 2.2f, "CRITICAL UV/GAMMA"),
    B_BLUE_WHITE("Blue-White Star", "Class B", Color(0xFF88CCFF), 1.7f, "HIGH RADIATION"),
    A_WHITE("White Main Sequence", "Class A", Color(0xFFE8F4FF), 1.4f, "MODERATE RADIATION"),
    G_YELLOW_DWARF("Golden Dwarf (Sol-Type)", "Class G", Color(0xFFFFCC00), 1.2f, "STANDARD SOLAR"),
    K_ORANGE_DWARF("Orange Dwarf", "Class K", Color(0xFFFF9933), 1.0f, "STABLE LOW RADIATION"),
    M_RED_DWARF("Crimson Dwarf", "Class M", Color(0xFFFF4444), 0.8f, "HIGH FLARE EMISSION"),
    NEUTRON_STAR("Relativistic Pulsar", "Pulsar", Color(0xFFCC44FF), 3.0f, "GRAVITATIONAL ANOMALY"),
    BINARY_SYSTEM("Binary Stellar Pair", "Dual Core", Color(0xFFFFBB44), 1.8f, "DUAL CORONAL MATRIX");

    companion object {
        fun fromSeeds(seed0: Int, seed1: Int, seed2: Int): StarType {
            val hash = ((seed2 shr 4) xor (seed0 shr 2)).let { if (it < 0) -it else it }
            val stars = entries.toTypedArray()
            return stars[hash % stars.size]
        }
    }
}

enum class SystemFaction(
    val displayName: String,
    val motto: String,
    val color: Color,
    val tariffRate: Int, // in percent
    val securitySupport: String
) {
    GALACTIC_COOP(
        displayName = "Galactic Cooperative",
        motto = "Law, Order & Free Enterprise",
        color = Color(0xFF00FFFF),
        tariffRate = 5,
        securitySupport = "Viper Police Cruiser Squadrons"
    ),
    INDEPENDENT_COLONIES(
        displayName = "Free Frontier League",
        motto = "Liberty on the Cosmic Edge",
        color = Color(0xFF00FF66),
        tariffRate = 0,
        securitySupport = "Local Militia & Armed Freighters"
    ),
    PIRATE_CARTEL(
        displayName = "Black Nebula Syndicate",
        motto = "To the Victor the Spoils",
        color = Color(0xFFFF3333),
        tariffRate = 15,
        securitySupport = "Outlaw Marauder Raiding Wings"
    ),
    CORPORATE_SYNDICATE(
        displayName = "OmniCorp Combine",
        motto = "Maximum Efficiency, Maximum Profit",
        color = Color(0xFFFFCC00),
        tariffRate = 8,
        securitySupport = "Automated Defense Drones & Gunships"
    ),
    REBEL_CONFEDERACY(
        displayName = "Frontier Resistance Front",
        motto = "Unbroken Under the Stars",
        color = Color(0xFFFF9933),
        tariffRate = 3,
        securitySupport = "Patrol Gunboats & ECM Stations"
    );

    companion object {
        fun fromGovernment(gov: Int, seed1: Int): SystemFaction {
            return when (gov) {
                0 -> PIRATE_CARTEL               // Anarchy
                1 -> REBEL_CONFEDERACY           // Feudal
                2, 3 -> INDEPENDENT_COLONIES     // Multi-Gov / Dictatorship
                4, 5 -> GALACTIC_COOP            // Communist / Confederacy
                else -> CORPORATE_SYNDICATE      // Democracy / Corporate State
            }
        }
    }
}

enum class SecurityLevel(
    val title: String,
    val dangerIndex: Int, // 1 (safest) to 4 (anarchy)
    val color: Color,
    val pirateSpawnRate: Float
) {
    HIGH_SEC("SECURE CORE (High Security)", 1, Color(0xFF00FFFF), 0.05f),
    MEDIUM_SEC("PATROLLED (Medium Security)", 2, Color(0xFF00FF66), 0.15f),
    LOW_SEC("FRONTIER (Low Security)", 3, Color(0xFFFFCC00), 0.35f),
    ANARCHY_HAZARD("ANARCHY (Extreme Hazard)", 4, Color(0xFFFF3333), 0.70f);

    companion object {
        fun fromGovernment(gov: Int): SecurityLevel {
            return when (gov) {
                0 -> ANARCHY_HAZARD
                1 -> LOW_SEC
                2, 3 -> LOW_SEC
                4, 5 -> MEDIUM_SEC
                else -> HIGH_SEC
            }
        }
    }
}

enum class InvestmentType(
    val title: String,
    val baseCostCredits: Int,
    val dividendPerJumpCredits: Int,
    val description: String
) {
    AUTOMATED_MINING_RIG(
        title = "Automated Mining Rig",
        baseCostCredits = 450,
        dividendPerJumpCredits = 35,
        description = "Orbital extraction platform blastic asteroids and sending regular mineral dividends."
    ),
    ORBITAL_TRADE_RELAY(
        title = "Orbital Trade Relay",
        baseCostCredits = 750,
        dividendPerJumpCredits = 65,
        description = "Automated market exchange relay granting 6% commodity discounts and transaction dividends."
    ),
    DEFENSE_CITADEL(
        title = "Defense Citadel",
        baseCostCredits = 1250,
        dividendPerJumpCredits = 110,
        description = "Armed military battlestation suppressing pirate raids by 65% and collecting navy bounties."
    ),
    SOLAR_HARVEST_ARRAY(
        title = "Solar Harvest Array",
        baseCostCredits = 900,
        dividendPerJumpCredits = 80,
        description = "Coronal plasma collector siphoning stellar fuel and selling clean energy to planetary grids."
    )
}

data class ColonyInvestment(
    val id: Long,
    val systemId: Int,
    val systemName: String,
    val type: InvestmentType,
    var level: Int = 1,
    var totalEarnedDeciCr: Long = 0L
) {
    fun currentDividendCredits(): Int = type.dividendPerJumpCredits * level
    fun upgradeCostCredits(): Int = type.baseCostCredits * (level + 1)
}

enum class ContractType(val label: String) {
    BOUNTY_HUNT("BOUNTY INTERCEPTION"),
    SUPPLY_DELIVERY("CRITICAL SUPPLY LINE"),
    MINERAL_SURVEY("DEEP CORE PROSPECTING")
}

data class StrategicContract(
    val id: Long,
    val title: String,
    val targetSystemId: Int,
    val targetSystemName: String,
    val rewardCredits: Int,
    val type: ContractType,
    val requirementDescription: String,
    val commodityIdNeeded: Int? = null,
    val quantityNeeded: Int = 0,
    var isCompleted: Boolean = false
)

data class ArbitrageOpportunity(
    val sourceSystem: SystemData,
    val targetSystem: SystemData,
    val distanceLy: Float,
    val commodityName: String,
    val commodityId: Int,
    val buyPriceDeciCr: Int,
    val sellPriceDeciCr: Int,
    val profitPerTonneDeciCr: Int,
    val potentialTotalProfitCredits: Float,
    val roiPercent: Int,
    val riskRating: String
)

object TradeArbitrageEngine {

    fun findTopArbitrageRoutes(
        fromSystem: SystemData,
        allSystems: List<SystemData>,
        maxDistanceLy: Float = 7.0f,
        cargoCapacity: Int = 20
    ): List<ArbitrageOpportunity> {
        val opportunities = mutableListOf<ArbitrageOpportunity>()
        val localMarket = MarketEngine.createMarketForSystem(fromSystem)

        val neighbors = allSystems.filter {
            it.id != fromSystem.id &&
            GalaxyGenerator.distanceInDeciLy(fromSystem, it) <= (maxDistanceLy * 10).toInt()
        }

        for (target in neighbors) {
            val distLy = GalaxyGenerator.distanceInDeciLy(fromSystem, target) / 10.0f
            val targetMarket = MarketEngine.createMarketForSystem(target)

            for (localItem in localMarket) {
                val targetItem = targetMarket.find { it.id == localItem.id } ?: continue
                val profitPerTonne = targetItem.price - localItem.price
                if (profitPerTonne > 8) { // Meaningful profit (> 0.8 CR per tonne)
                    val buyUnits = minOf(localItem.quantity, cargoCapacity).coerceAtLeast(1)
                    val totalProfitDeci = profitPerTonne.toLong() * buyUnits
                    val roi = ((profitPerTonne.toFloat() / localItem.price.toFloat()) * 100).toInt()
                    val risk = when {
                        target.government == 0 -> "HIGH (Anarchy)"
                        target.government in 1..2 -> "MEDIUM (Feudal)"
                        else -> "LOW (Secure)"
                    }

                    opportunities.add(
                        ArbitrageOpportunity(
                            sourceSystem = fromSystem,
                            targetSystem = target,
                            distanceLy = distLy,
                            commodityName = localItem.name,
                            commodityId = localItem.id,
                            buyPriceDeciCr = localItem.price,
                            sellPriceDeciCr = targetItem.price,
                            profitPerTonneDeciCr = profitPerTonne,
                            potentialTotalProfitCredits = totalProfitDeci / 10.0f,
                            roiPercent = roi,
                            riskRating = risk
                        )
                    )
                }
            }
        }

        return opportunities.sortedByDescending { it.profitPerTonneDeciCr }.take(8)
    }

    fun generateContractsForSystem(system: SystemData, allSystems: List<SystemData>): List<StrategicContract> {
        val contracts = mutableListOf<StrategicContract>()
        val neighbors = allSystems.filter {
            it.id != system.id &&
            GalaxyGenerator.distanceInDeciLy(system, it) <= 70
        }

        // 1. Bounty contract targeting high danger neighbor or local system
        val anarchyNeighbor = neighbors.find { it.government <= 1 } ?: system
        contracts.add(
            StrategicContract(
                id = system.id * 100L + 1,
                title = "OPERATION: OUTLAW SWEEP",
                targetSystemId = anarchyNeighbor.id,
                targetSystemName = anarchyNeighbor.name,
                rewardCredits = 480 + (system.techLevel * 25),
                type = ContractType.BOUNTY_HUNT,
                requirementDescription = "Eliminate hostile pirate marauders in the ${anarchyNeighbor.name} sector and secure shipping lanes."
            )
        )

        // 2. Supply contract (deliver agricultural goods if industrial, or tech if agricultural)
        val tradeNeighbor = neighbors.firstOrNull { it.economy != system.economy } ?: neighbors.firstOrNull() ?: system
        val (neededId, neededName) = if (system.economy in 0..3) {
            0 to "Food" // Industrial systems need food
        } else {
            7 to "Computers" // Agricultural systems need computers
        }
        contracts.add(
            StrategicContract(
                id = system.id * 100L + 2,
                title = "SUPPLY RUN: ${neededName.uppercase()}",
                targetSystemId = tradeNeighbor.id,
                targetSystemName = tradeNeighbor.name,
                rewardCredits = 320 + (system.techLevel * 18),
                type = ContractType.SUPPLY_DELIVERY,
                requirementDescription = "Transport critical cargo of 5t $neededName to starport in ${tradeNeighbor.name}.",
                commodityIdNeeded = neededId,
                quantityNeeded = 5
            )
        )

        // 3. Deep Core Prospecting
        contracts.add(
            StrategicContract(
                id = system.id * 100L + 3,
                title = "PROSPECTING SURVEY",
                targetSystemId = system.id,
                targetSystemName = system.name,
                rewardCredits = 550,
                type = ContractType.MINERAL_SURVEY,
                requirementDescription = "Extract valuable Gold, Platinum, or Gem-Stones from asteroid belts in ${system.name} system."
            )
        )

        return contracts
    }
}
