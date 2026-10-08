package com.example.elite.ships

import com.example.elite.math3d.Vector3
import com.example.elite.model.PlanetType
import com.example.elite.model.StarType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object PlanetWireframeFactory {

    fun createPlanetBlueprint(type: PlanetType, radius: Float = 400f): ShipBlueprint {
        val verts = mutableListOf<Vector3>()
        val edges = mutableListOf<Edge>()

        fun addRing(r: Float, y: Float, count: Int): Int {
            val start = verts.size
            for (i in 0 until count) {
                val angle = (i * 2.0 * Math.PI / count).toFloat()
                verts.add(Vector3(cos(angle) * r, y, sin(angle) * r))
                val next = (i + 1) % count
                edges.add(Edge(start + i, start + next))
            }
            return start
        }

        fun addMeridian(r: Float, count: Int, xzPlaneAngle: Float = 0f) {
            val start = verts.size
            val cPlane = cos(xzPlaneAngle)
            val sPlane = sin(xzPlaneAngle)
            for (i in 0 until count) {
                val angle = (i * 2.0 * Math.PI / count).toFloat()
                val y = sin(angle) * r
                val h = cos(angle) * r
                verts.add(Vector3(h * cPlane, y, h * sPlane))
                val next = (i + 1) % count
                edges.add(Edge(start + i, start + next))
            }
        }

        // Base planetary sphere framework
        addRing(radius, 0f, 24) // Equator
        addMeridian(radius, 24, 0f) // Meridian 1
        addMeridian(radius, 24, (Math.PI / 2.0).toFloat()) // Meridian 2

        when (type) {
            PlanetType.GAS_GIANT -> {
                // 5 dense atmospheric bands
                val latAngles = listOf(-0.9f, -0.55f, -0.25f, 0.25f, 0.55f, 0.9f)
                for (a in latAngles) {
                    val r = radius * cos(a)
                    val y = radius * sin(a)
                    addRing(r, y, 20)
                }

                // Great Red Spot cyclonic storm vortex on southern hemisphere
                val stormLat = -0.4f
                val stormLon = 0.5f
                val stormR = radius * cos(stormLat)
                val stormY = radius * sin(stormLat)
                val stormCenter = Vector3(cos(stormLon) * stormR, stormY, sin(stormLon) * stormR)
                val stormStart = verts.size
                val stormPoints = 12
                for (i in 0 until stormPoints) {
                    val th = (i * 2.0 * Math.PI / stormPoints).toFloat()
                    val dx = cos(th) * 45f
                    val dy = sin(th) * 22f
                    verts.add(Vector3(stormCenter.x + dx, stormCenter.y + dy, stormCenter.z))
                    val next = (i + 1) % stormPoints
                    edges.add(Edge(stormStart + i, stormStart + next))
                }

                // Double concentric outer ice rings with radial spokes
                val innerRingR = radius * 1.55f
                val outerRingR = radius * 2.05f
                val r1Start = addRing(innerRingR, 0f, 32)
                val r2Start = addRing(outerRingR, 0f, 32)
                for (i in 0 until 32 step 4) {
                    edges.add(Edge(r1Start + i, r2Start + i))
                }
            }

            PlanetType.VOLCANIC -> {
                // High-latitude circles
                val r7 = radius * 0.707f
                addRing(r7, r7, 16)
                addRing(r7, -r7, 16)

                // Molten magma fissures across surface
                val f1Start = verts.size
                val f1Coords = listOf(
                    Vector3(0f, radius, 0f),
                    Vector3(radius * 0.3f, radius * 0.85f, radius * 0.4f),
                    Vector3(radius * 0.5f, radius * 0.6f, radius * 0.6f),
                    Vector3(radius * 0.7f, radius * 0.2f, radius * 0.65f),
                    Vector3(radius * 0.9f, -radius * 0.1f, radius * 0.4f),
                    Vector3(radius * 0.75f, -radius * 0.5f, radius * 0.4f),
                    Vector3(radius * 0.4f, -radius * 0.85f, radius * 0.3f),
                    Vector3(0f, -radius, 0f)
                )
                verts.addAll(f1Coords)
                for (i in 0 until f1Coords.size - 1) {
                    edges.add(Edge(f1Start + i, f1Start + i + 1))
                }

                // Caldera volcanic crater polygon
                val cStart = verts.size
                val cCount = 8
                for (i in 0 until cCount) {
                    val a = (i * 2.0 * Math.PI / cCount).toFloat()
                    verts.add(Vector3(cos(a) * 45f + radius * 0.5f, radius * 0.35f, sin(a) * 45f + radius * 0.5f))
                    val next = (i + 1) % cCount
                    edges.add(Edge(cStart + i, cStart + next))
                }
            }

            PlanetType.TERRAN -> {
                // Standard upper & lower latitudes
                val r7 = radius * 0.707f
                addRing(r7, r7, 16)
                addRing(r7, -r7, 16)

                // Northern Continent Coastline
                val nStart = verts.size
                val northCoast = listOf(
                    Vector3(0f, radius * 0.9f, radius * 0.3f),
                    Vector3(radius * 0.45f, radius * 0.7f, radius * 0.5f),
                    Vector3(radius * 0.65f, radius * 0.45f, radius * 0.55f),
                    Vector3(radius * 0.4f, radius * 0.25f, radius * 0.85f),
                    Vector3(radius * 0.1f, radius * 0.35f, radius * 0.92f),
                    Vector3(-radius * 0.35f, radius * 0.5f, radius * 0.78f),
                    Vector3(-radius * 0.5f, radius * 0.75f, radius * 0.4f)
                )
                verts.addAll(northCoast)
                for (i in 0 until northCoast.size) {
                    edges.add(Edge(nStart + i, nStart + ((i + 1) % northCoast.size)))
                }

                // Southern Continent Coastline
                val sStart = verts.size
                val southCoast = listOf(
                    Vector3(radius * 0.2f, -radius * 0.15f, radius * 0.95f),
                    Vector3(radius * 0.55f, -radius * 0.3f, radius * 0.75f),
                    Vector3(radius * 0.6f, -radius * 0.65f, radius * 0.45f),
                    Vector3(radius * 0.25f, -radius * 0.85f, radius * 0.45f),
                    Vector3(-radius * 0.1f, -radius * 0.7f, radius * 0.7f),
                    Vector3(-radius * 0.2f, -radius * 0.35f, radius * 0.9f)
                )
                verts.addAll(southCoast)
                for (i in 0 until southCoast.size) {
                    edges.add(Edge(sStart + i, sStart + ((i + 1) % southCoast.size)))
                }
            }

            PlanetType.OCEANIC -> {
                // Fine-pitch tidal current lines
                for (a in listOf(-0.75f, -0.45f, -0.2f, 0.2f, 0.45f, 0.75f)) {
                    val r = radius * cos(a)
                    val y = radius * sin(a)
                    addRing(r, y, 20)
                }

                // North Polar Ice Shelf (octagonal cap)
                val nCapStart = verts.size
                val capR = radius * 0.35f
                val capY = radius * 0.93f
                for (i in 0 until 8) {
                    val a = (i * 2.0 * Math.PI / 8).toFloat()
                    verts.add(Vector3(cos(a) * capR, capY, sin(a) * capR))
                    edges.add(Edge(nCapStart + i, nCapStart + ((i + 1) % 8)))
                }

                // South Polar Ice Shelf
                val sCapStart = verts.size
                for (i in 0 until 8) {
                    val a = (i * 2.0 * Math.PI / 8).toFloat()
                    verts.add(Vector3(cos(a) * capR, -capY, sin(a) * capR))
                    edges.add(Edge(sCapStart + i, sCapStart + ((i + 1) % 8)))
                }
            }

            PlanetType.DESERT -> {
                // Dune ridge lines & canyon trench
                val r7 = radius * 0.707f
                addRing(r7, r7, 16)
                addRing(r7, -r7, 16)

                // Grand canyon rift across equator
                val cStart = verts.size
                val canyon1 = listOf(
                    Vector3(-radius * 0.8f, -15f, radius * 0.58f),
                    Vector3(-radius * 0.4f, 10f, radius * 0.9f),
                    Vector3(0f, -5f, radius),
                    Vector3(radius * 0.45f, 15f, radius * 0.88f),
                    Vector3(radius * 0.8f, -10f, radius * 0.58f)
                )
                val canyon2 = listOf(
                    Vector3(-radius * 0.8f, -40f, radius * 0.56f),
                    Vector3(-radius * 0.4f, -20f, radius * 0.88f),
                    Vector3(0f, -35f, radius * 0.98f),
                    Vector3(radius * 0.45f, -15f, radius * 0.86f),
                    Vector3(radius * 0.8f, -35f, radius * 0.56f)
                )
                verts.addAll(canyon1)
                verts.addAll(canyon2)
                for (i in 0 until 4) {
                    edges.add(Edge(cStart + i, cStart + i + 1))
                    edges.add(Edge(cStart + 5 + i, cStart + 5 + i + 1))
                    edges.add(Edge(cStart + i, cStart + 5 + i)) // cross-struts
                }
            }

            PlanetType.ICE_WORLD -> {
                // Polygonal cryo-frost tectonic plates
                val pStart = verts.size
                val points = listOf(
                    Vector3(0f, radius, 0f),
                    Vector3(radius * 0.55f, radius * 0.65f, 0f),
                    Vector3(radius * 0.8f, 0f, radius * 0.55f),
                    Vector3(0f, -radius * 0.5f, radius * 0.85f),
                    Vector3(-radius * 0.7f, 0f, radius * 0.7f),
                    Vector3(-radius * 0.6f, radius * 0.65f, 0f),
                    Vector3(0f, -radius, 0f)
                )
                verts.addAll(points)
                edges.add(Edge(pStart + 0, pStart + 1))
                edges.add(Edge(pStart + 1, pStart + 2))
                edges.add(Edge(pStart + 2, pStart + 3))
                edges.add(Edge(pStart + 3, pStart + 4))
                edges.add(Edge(pStart + 4, pStart + 5))
                edges.add(Edge(pStart + 5, pStart + 0))
                edges.add(Edge(pStart + 2, pStart + 6))
                edges.add(Edge(pStart + 3, pStart + 6))
                edges.add(Edge(pStart + 4, pStart + 6))
            }

            PlanetType.TOXIC_BARREN -> {
                // Dense asymmetric toxic haze loops
                for (a in listOf(-0.6f, -0.3f, 0.1f, 0.45f, 0.8f)) {
                    val r = radius * cos(a)
                    val y = radius * sin(a)
                    addRing(r, y, 16)
                }
            }

            PlanetType.RINGWORLD_CAPITAL -> {
                // Massive equatorial orbital ring megastructure
                val ringR = radius * 1.8f
                val ringStart = verts.size
                val ringSegments = 16
                for (i in 0 until ringSegments) {
                    val a = (i * 2.0 * Math.PI / ringSegments).toFloat()
                    verts.add(Vector3(cos(a) * ringR, 0f, sin(a) * ringR))
                    val next = (i + 1) % ringSegments
                    edges.add(Edge(ringStart + i, ringStart + next))
                }

                // 4 Space Elevator Spires connecting planet poles to orbital ring
                val nPole = verts.size
                verts.add(Vector3(0f, radius, 0f))
                val sPole = verts.size
                verts.add(Vector3(0f, -radius, 0f))

                for (idx in listOf(0, 4, 8, 12)) {
                    edges.add(Edge(nPole, ringStart + idx))
                    edges.add(Edge(sPole, ringStart + idx))
                }
            }
        }

        return ShipBlueprint(
            name = type.displayName,
            baseScale = 1.0f,
            maxSpeed = 0f,
            bounty = 0,
            vertices = verts,
            edges = edges
        )
    }

    fun createStarBlueprint(type: StarType, radius: Float = 450f): ShipBlueprint {
        val verts = mutableListOf<Vector3>()
        val edges = mutableListOf<Edge>()

        fun addRing(r: Float, count: Int): Int {
            val start = verts.size
            for (i in 0 until count) {
                val a = (i * 2.0 * Math.PI / count).toFloat()
                verts.add(Vector3(cos(a) * r, sin(a) * r, 0f))
                val next = (i + 1) % count
                edges.add(Edge(start + i, start + next))
            }
            return start
        }

        // Inner stellar core
        addRing(radius, 24)

        when (type) {
            StarType.NEUTRON_STAR -> {
                // Compact dense core + dual relativistic polar jets + accretion disc
                val p1 = verts.size
                verts.add(Vector3(0f, radius * 2.4f, 0f)) // North beam tip
                val p2 = verts.size
                verts.add(Vector3(0f, -radius * 2.4f, 0f)) // South beam tip
                val coreCenter = verts.size
                verts.add(Vector3(0f, 0f, 0f))

                edges.add(Edge(coreCenter, p1))
                edges.add(Edge(coreCenter, p2))

                // Accretion disk
                val accR = radius * 1.6f
                val accStart = verts.size
                for (i in 0 until 16) {
                    val a = (i * 2.0 * Math.PI / 16).toFloat()
                    verts.add(Vector3(cos(a) * accR, 0f, sin(a) * accR))
                    edges.add(Edge(accStart + i, accStart + ((i + 1) % 16)))
                }
            }

            StarType.BINARY_SYSTEM -> {
                // Dual orbiting stellar cores
                val c1 = verts.size
                val rHalf = radius * 0.6f
                for (i in 0 until 16) {
                    val a = (i * 2.0 * Math.PI / 16).toFloat()
                    verts.add(Vector3(cos(a) * rHalf - radius * 0.7f, sin(a) * rHalf, 0f))
                    edges.add(Edge(c1 + i, c1 + ((i + 1) % 16)))
                }

                val c2 = verts.size
                for (i in 0 until 16) {
                    val a = (i * 2.0 * Math.PI / 16).toFloat()
                    verts.add(Vector3(cos(a) * rHalf + radius * 0.7f, sin(a) * rHalf, 0f))
                    edges.add(Edge(c2 + i, c2 + ((i + 1) % 16)))
                }

                // Plasma bridge between stars
                edges.add(Edge(c1, c2))
            }

            else -> {
                // Coronal radiation flare loops
                val flareCount = if (type == StarType.O_BLUE_SUPERGIANT) 18 else 12
                for (i in 0 until flareCount) {
                    val a = (i * 2.0 * Math.PI / flareCount).toFloat()
                    val spikeLen = if (i % 2 == 0) radius * 1.55f else radius * 1.3f
                    val baseIdx = verts.size
                    verts.add(Vector3(cos(a) * radius, sin(a) * radius, 0f))
                    verts.add(Vector3(cos(a) * spikeLen, sin(a) * spikeLen, 0f))
                    edges.add(Edge(baseIdx, baseIdx + 1))
                }
            }
        }

        return ShipBlueprint(
            name = type.displayName,
            baseScale = 1.0f,
            maxSpeed = 0f,
            bounty = 0,
            vertices = verts,
            edges = edges
        )
    }
}
