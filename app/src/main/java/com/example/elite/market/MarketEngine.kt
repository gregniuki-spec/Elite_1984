package com.example.elite.market

import com.example.elite.model.Commodity
import com.example.elite.model.SystemData
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Economic macro event states affecting planetary commodity pricing.
 */
enum class EconomyCondition(
    val code: String,
    val title: String,
    val description: String,
    val foodMod: Float = 1.0f,
    val industrialMod: Float = 1.0f,
    val luxuryMod: Float = 1.0f,
    val mineralsMod: Float = 1.0f,
    val firearmsMod: Float = 1.0f,
    val narcoticsMod: Float = 1.0f,
    val taxRate: Int = 5
) {
    NORMAL(
        code = "NORMAL",
        title = "Orbital Equilibrium",
        description = "Standard merchant freight flow, steady consumer demand.",
        foodMod = 1.0f, industrialMod = 1.0f, luxuryMod = 1.0f, mineralsMod = 1.0f, firearmsMod = 1.0f, narcoticsMod = 1.0f,
        taxRate = 5
    ),
    BOOM(
        code = "BOOM",
        title = "Economic Boom",
        description = "Surging capital investment, increased wages, high luxury/tech appetite.",
        foodMod = 1.15f, industrialMod = 1.30f, luxuryMod = 1.60f, mineralsMod = 1.25f, firearmsMod = 0.90f, narcoticsMod = 1.20f,
        taxRate = 8
    ),
    RECESSION(
        code = "RECESSION",
        title = "Depression & Slump",
        description = "Credit crunch, factory idling, collapsed luxury markets.",
        foodMod = 0.85f, industrialMod = 0.70f, luxuryMod = 0.50f, mineralsMod = 0.75f, firearmsMod = 1.20f, narcoticsMod = 1.35f,
        taxRate = 3
    ),
    DROUGHT(
        code = "DROUGHT",
        title = "Severe Famine / Blight",
        description = "Agricultural failure across planetary biome; food and liquor prices skyrocket.",
        foodMod = 2.40f, industrialMod = 0.90f, luxuryMod = 0.80f, mineralsMod = 1.0f, firearmsMod = 1.15f, narcoticsMod = 1.10f,
        taxRate = 2
    ),
    MINERAL_RUSH(
        code = "MINERAL_RUSH",
        title = "Asteroid Gold Rush",
        description = "Deep crust mining strikes detected; tools & machinery urgently demanded, raw ores flood market.",
        foodMod = 1.10f, industrialMod = 1.45f, luxuryMod = 1.30f, mineralsMod = 0.60f, firearmsMod = 1.30f, narcoticsMod = 1.15f,
        taxRate = 7
    ),
    WAR_MOBILIZATION(
        code = "WAR_MOBILIZATION",
        title = "War Fleet Mobilization",
        description = "Border faction tensions; military contracts requisition all alloys and arms.",
        foodMod = 1.20f, industrialMod = 1.35f, luxuryMod = 0.60f, mineralsMod = 1.40f, firearmsMod = 2.20f, narcoticsMod = 0.80f,
        taxRate = 12
    ),
    PIRATE_BLOCKADE(
        code = "PIRATE_BLOCKADE",
        title = "Raider Siege & Embargo",
        description = "Outlaw corsairs harass orbital corridors; scarce incoming shipments drive all basic goods up.",
        foodMod = 1.65f, industrialMod = 1.40f, luxuryMod = 0.90f, mineralsMod = 1.10f, firearmsMod = 1.80f, narcoticsMod = 2.10f,
        taxRate = 0
    );

    companion object {
        fun fromCode(code: String): EconomyCondition {
            return entries.find { it.code == code } ?: NORMAL
        }

        fun pickRandomForSystem(system: SystemData, cycle: Int): EconomyCondition {
            val pseudoRandom = (system.seed0 + system.seed1 * 13 + cycle * 37) and 0x7FFFFFFF
            val roll = (pseudoRandom % 100)
            return when {
                // High tech/corporate systems more prone to boom or war
                system.government == 0 && roll < 40 -> PIRATE_BLOCKADE // Anarchy
                system.government == 7 && roll < 25 -> BOOM
                system.economy in 5..7 && roll < 20 -> DROUGHT // Agricultural famine
                system.economy in 0..2 && roll < 20 -> MINERAL_RUSH // Industrial rush
                roll < 10 -> WAR_MOBILIZATION
                roll < 20 -> RECESSION
                roll < 35 -> BOOM
                roll < 45 -> DROUGHT
                roll < 55 -> MINERAL_RUSH
                else -> NORMAL
            }
        }
    }
}

/**
 * Enhanced Market Engine implementing the 1984 BBC Micro 6502 assembly routines
 * (QQ23, TT151, GVL, TT167) augmented with planetary economy fluctuations,
 * event modifiers, and dynamic pricing curves.
 */
object MarketEngine {

    // 17 Commodities defined in QQ23 table
    val BASE_ITEMS = listOf(
        Commodity(0, "Food", 19, -2, "t", 6, 0x01),
        Commodity(1, "Textiles", 20, -1, "t", 10, 0x03),
        Commodity(2, "Radioactives", 65, -3, "t", 2, 0x07),
        Commodity(3, "Slaves", 40, -5, "t", 226, 0x1F),
        Commodity(4, "Liquor/Wines", 83, -5, "t", 251, 0x0F),
        Commodity(5, "Luxuries", 196, 8, "t", 54, 0x03),
        Commodity(6, "Narcotics", 235, 29, "t", 8, 0x78),
        Commodity(7, "Computers", 154, 14, "t", 56, 0x03),
        Commodity(8, "Machinery", 117, 6, "t", 40, 0x07),
        Commodity(9, "Alloys", 78, 1, "t", 17, 0x1F),
        Commodity(10, "Firearms", 124, 13, "t", 29, 0x07),
        Commodity(11, "Furs", 176, -9, "t", 220, 0x3F),
        Commodity(12, "Minerals", 32, -1, "t", 53, 0x03),
        Commodity(13, "Gold", 97, -1, "kg", 66, 0x07),
        Commodity(14, "Platinum", 171, -2, "kg", 55, 0x1F),
        Commodity(15, "Gem-Stones", 45, -1, "g", 250, 0x0F),
        Commodity(16, "Alien Items", 53, 15, "t", 192, 0x07)
    )

    /**
     * Compute authentic baseline market prices using the 6502 TT151 and GVL algorithm.
     */
    fun createMarketForSystem(
        system: SystemData,
        marketSeed: Int = system.seed0 and 0xFF,
        condition: EconomyCondition = EconomyCondition.NORMAL
    ): List<Commodity> {
        val econ = system.economy
        val rand = marketSeed

        return BASE_ITEMS.map { base ->
            val factor = base.factor
            val absFactor = abs(factor)
            val econEffect = econ * absFactor

            // Base 6502 raw price
            val rawPrice = if (factor >= 0) {
                base.basePrice + (rand and base.mask) + econEffect
            } else {
                base.basePrice + (rand and base.mask) - econEffect
            }
            var priceInDeciCredits = (rawPrice.coerceAtLeast(1) * 4)

            // Availability calculation (GVL):
            val rawQty = if (factor >= 0) {
                base.baseQuantity + (rand and base.mask) - econEffect
            } else {
                base.baseQuantity + (rand and base.mask) + econEffect
            }

            var qty = if (base.id == 16) {
                0 // Alien items are always 0 availability in normal markets
            } else if (rawQty < 0) {
                0
            } else {
                rawQty and 0x3F // Max 63 units
            }

            // Apply Economy Event Fluctuation Multipliers
            val modifier = getCategoryModifier(base.id, condition)
            priceInDeciCredits = (priceInDeciCredits * modifier).roundToInt().coerceAtLeast(4)

            // Adjust quantity inverse to price surges (scarcity drives up price)
            if (modifier > 1.25f && qty > 0) {
                qty = (qty / modifier).roundToInt().coerceAtLeast(1)
            } else if (modifier < 0.75f) {
                qty = (qty * (1f / modifier)).roundToInt().coerceAtMost(63)
            }

            base.copy(
                price = priceInDeciCredits,
                quantity = qty
            )
        }
    }

    /**
     * Maps commodity ID to corresponding economic condition multiplier.
     */
    fun getCategoryModifier(commodityId: Int, condition: EconomyCondition): Float {
        return when (commodityId) {
            0, 1, 11 -> condition.foodMod                // Food, Textiles, Furs
            4 -> (condition.foodMod + condition.luxuryMod) / 2.0f // Liquor/Wines
            2, 9, 12 -> condition.mineralsMod           // Radioactives, Alloys, Minerals
            13, 14, 15 -> condition.luxuryMod           // Gold, Platinum, Gems
            5, 7, 8 -> condition.industrialMod          // Luxuries, Computers, Machinery
            10 -> condition.firearmsMod                 // Firearms
            3, 6 -> condition.narcoticsMod              // Slaves, Narcotics
            16 -> condition.luxuryMod * 1.5f            // Alien Items
            else -> 1.0f
        }
    }
}
