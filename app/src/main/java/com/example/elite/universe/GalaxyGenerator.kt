package com.example.elite.universe

import com.example.elite.model.SystemData
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Procedural Universe Generator recreating the 6502 assembly routines
 * TT54, cpl, TT20, TT24, and TT25 from elite-source.asm.
 */
object GalaxyGenerator {

    private val DIGRAPHS = arrayOf(
        "AL", "LE", "XE", "GE", "ZA", "CE", "BI", "SO",
        "US", "ES", "AR", "MA", "IN", "DI", "RE", "A?",
        "ER", "AT", "EN", "BE", "RA", "LA", "VE", "TI",
        "ED", "OR", "QU", "AN", "TE", "IS", "RI", "ON"
    )

    private val GOVERNMENTS = arrayOf(
        "Anarchy",
        "Feudal",
        "Multi-Government",
        "Dictatorship",
        "Communist",
        "Confederacy",
        "Democracy",
        "Corporate State"
    )

    private val ECONOMIES = arrayOf(
        "Rich Industrial",
        "Average Industrial",
        "Poor Industrial",
        "Mainly Industrial",
        "Mainly Agricultural",
        "Rich Agricultural",
        "Average Agricultural",
        "Poor Agricultural"
    )

    // Base seeds for Galaxy 1 (Tibedied is system 0, Lave is system 7)
    // In BBC Micro 6502: &5A4A, &0248, &B753
    private const val GALAXY_1_SEED_0 = 0x5A4A
    private const val GALAXY_1_SEED_1 = 0x0248
    private const val GALAXY_1_SEED_2 = 0xB753

    /**
     * Subroutine TT54: Twists the three 16-bit seeds once.
     * s0' = s1
     * s1' = s2
     * s2' = s0 + s1 + s2
     */
    fun twist(s0: Int, s1: Int, s2: Int): Triple<Int, Int, Int> {
        val n0 = s1 and 0xFFFF
        val n1 = s2 and 0xFFFF
        val n2 = (s0 + s1 + s2) and 0xFFFF
        return Triple(n0, n1, n2)
    }

    /**
     * Subroutine TT20: Twists seeds 4 times between systems.
     */
    fun twistFour(s0: Int, s1: Int, s2: Int): Triple<Int, Int, Int> {
        var (c0, c1, c2) = Triple(s0, s1, s2)
        repeat(4) {
            val next = twist(c0, c1, c2)
            c0 = next.first
            c1 = next.second
            c2 = next.third
        }
        return Triple(c0, c1, c2)
    }

    /**
     * Subroutine cpl: Generates procedural system name from seeds.
     */
    fun generateSystemName(seed0: Int, seed1: Int, seed2: Int): String {
        var s0 = seed0
        var s1 = seed1
        var s2 = seed2

        val s0Lo = s0 and 0xFF
        val loopCount = if ((s0Lo and 0x40) != 0) 4 else 3

        val sb = StringBuilder()
        for (i in 0 until loopCount) {
            val s2Hi = (s2 shr 8) and 0xFF
            val pairIdx = s2Hi and 0x1F

            if (pairIdx in 1..31) {
                val pair = DIGRAPHS[pairIdx]
                for (ch in pair) {
                    if (ch != '?') {
                        sb.append(ch)
                    }
                }
            }
            val next = twist(s0, s1, s2)
            s0 = next.first
            s1 = next.second
            s2 = next.third
        }

        val raw = sb.toString().lowercase()
        return raw.replaceFirstChar { it.uppercase() }
    }

    /**
     * Subroutines TT75, TT205, TT206, TT207: Generates species description.
     */
    fun generateSpecies(seed0: Int, seed1: Int, seed2: Int): String {
        val s2Lo = seed2 and 0xFF
        if ((s2Lo and 0x80) == 0) {
            return "Human Colonials"
        }

        val s2Hi = (seed2 shr 8) and 0xFF
        val s0Hi = (seed0 shr 8) and 0xFF
        val s1Hi = (seed1 shr 8) and 0xFF

        val sb = StringBuilder()

        // Adjective 1 (Size)
        val adj1 = (s2Hi shr 2) and 0x07
        when (adj1) {
            0 -> sb.append("Large ")
            1 -> sb.append("Fierce ")
            2 -> sb.append("Small ")
        }

        // Adjective 2 (Color)
        val adj2 = (s2Hi shr 5) and 0x07
        when (adj2) {
            0 -> sb.append("Green ")
            1 -> sb.append("Red ")
            2 -> sb.append("Yellow ")
            3 -> sb.append("Blue ")
            4 -> sb.append("Black ")
            5 -> sb.append("Harmless ")
        }

        // Adjective 3 (Trait)
        val adj3 = (s0Hi xor s1Hi) and 0x07
        when (adj3) {
            0 -> sb.append("Slimy ")
            1 -> sb.append("Bug-eyed ")
            2 -> sb.append("Horned ")
            3 -> sb.append("Bony ")
            4 -> sb.append("Fat ")
            5 -> sb.append("Furry ")
        }

        // Body type
        val bodyIdx = ((s2Hi and 0x03) + adj3) and 0x07
        val bodies = arrayOf(
            "Rodents", "Frogs", "Lizards", "Lobsters",
            "Birds", "Humanoids", "Felines", "Insects"
        )
        sb.append(bodies[bodyIdx])

        return sb.toString()
    }

    /**
     * Subroutine TT24: Generates full planetary system data from seeds.
     */
    fun generateSystemData(id: Int, seed0: Int, seed1: Int, seed2: Int): SystemData {
        val name = generateSystemName(seed0, seed1, seed2)
        val s0Hi = (seed0 shr 8) and 0xFF
        val s1Hi = (seed1 shr 8) and 0xFF
        val s1Lo = seed1 and 0xFF
        val s2Hi = (seed2 shr 8) and 0xFF

        val x = s1Hi
        val y = s0Hi

        val gov = (s1Lo shr 3) and 0x07
        var econ = s0Hi and 0x07
        if (gov <= 1) {
            econ = econ or 0x02
        }

        val tech = (econ xor 0x07) + (s1Hi and 0x03) + (gov / 2) + 1
        val pop = ((tech * 4) + econ + gov + 1) / 10.0f
        val prod = ((econ xor 0x07) + 3) * (gov + 4) * ((tech * 4) + econ + gov + 1) * 8
        val radius = (((s2Hi and 0x0F) + 11) * 256) + s1Hi
        val species = generateSpecies(seed0, seed1, seed2)

        return SystemData(
            id = id,
            name = name,
            x = x,
            y = y,
            economy = econ,
            economyName = ECONOMIES[econ.coerceIn(0, 7)],
            government = gov,
            governmentName = GOVERNMENTS[gov.coerceIn(0, 7)],
            techLevel = tech.coerceIn(1, 15),
            population = pop,
            productivity = prod,
            radius = radius,
            species = species,
            seed0 = seed0,
            seed1 = seed1,
            seed2 = seed2
        )
    }

    /**
     * Generates all 256 systems of a specified galaxy (1 to 8).
     */
    fun generateGalaxy(galaxyNumber: Int = 1): List<SystemData> {
        // Galactic hyperdrive rotates seeds
        var s0 = GALAXY_1_SEED_0
        var s1 = GALAXY_1_SEED_1
        var s2 = GALAXY_1_SEED_2

        for (g in 1 until galaxyNumber) {
            // In Elite, jumping galaxy applies bit rotation to seeds
            s0 = rotateSeed(s0)
            s1 = rotateSeed(s1)
            s2 = rotateSeed(s2)
        }

        val systems = ArrayList<SystemData>(256)
        var cur0 = s0
        var cur1 = s1
        var cur2 = s2

        for (id in 0 until 256) {
            systems.add(generateSystemData(id, cur0, cur1, cur2))
            val (next0, next1, next2) = twistFour(cur0, cur1, cur2)
            cur0 = next0
            cur1 = next1
            cur2 = next2
        }

        return systems
    }

    private fun rotateSeed(seed: Int): Int {
        val b0 = (seed and 0xFF)
        val b1 = (seed shr 8) and 0xFF
        val rot0 = ((b0 shl 1) and 0xFF) or (b0 shr 7)
        val rot1 = ((b1 shl 1) and 0xFF) or (b1 shr 7)
        return (rot1 shl 8) or rot0
    }

    /**
     * Integer distance calculation in Light Years (TT146/hyp1)
     */
    fun distanceInDeciLy(sys1: SystemData, sys2: SystemData): Int {
        val dx = abs(sys1.x - sys2.x)
        val dy = abs(sys1.y - sys2.y) / 2
        val dist = sqrt((dx * dx + dy * dy).toDouble()) * 4.0 / 10.0
        return (dist * 10).toInt()
    }
}
