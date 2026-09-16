package com.example.elite.market

import com.example.elite.model.Commodity
import com.example.elite.model.SystemData
import kotlin.math.abs

/**
 * Market engine implementing the exact 6502 assembly routines
 * QQ23, TT151, var, GVL, and TT167 from elite-source.asm.
 */
object MarketEngine {

    // 17 Commodities defined in QQ23 table
    private val BASE_ITEMS = listOf(
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

    fun createMarketForSystem(system: SystemData, marketSeed: Int = system.seed0 and 0xFF): List<Commodity> {
        val econ = system.economy
        val rand = marketSeed

        return BASE_ITEMS.map { base ->
            val factor = base.factor
            val absFactor = abs(factor)
            val econEffect = econ * absFactor

            // Price calculation (TT151):
            // In Elite: price in deci-credits = (base_price + (rand AND mask) +/- econEffect) * 4
            val rawPrice = if (factor >= 0) {
                base.basePrice + (rand and base.mask) + econEffect
            } else {
                base.basePrice + (rand and base.mask) - econEffect
            }
            // deci-credits (tenths of a CR): rawPrice * 4
            val priceInDeciCredits = (rawPrice.coerceAtLeast(1) * 4)

            // Availability calculation (GVL):
            val rawQty = if (factor >= 0) {
                base.baseQuantity + (rand and base.mask) - econEffect
            } else {
                base.baseQuantity + (rand and base.mask) + econEffect
            }

            val qty = if (base.id == 16) {
                0 // Alien items are always 0 availability in normal markets
            } else if (rawQty < 0) {
                0
            } else {
                rawQty and 0x3F // Max 63 units
            }

            base.copy(
                price = priceInDeciCredits,
                quantity = qty
            )
        }
    }
}
