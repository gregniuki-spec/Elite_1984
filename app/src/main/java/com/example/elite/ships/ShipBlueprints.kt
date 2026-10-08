package com.example.elite.ships

import com.example.elite.math3d.Vector3

/**
 * Ship blueprints extracted directly from elite-source.asm
 * containing exact vertices, edge connections, and face normals.
 */
data class Edge(val v1: Int, val v2: Int)

data class Face(
    val normal: Vector3,
    val vertexIndices: List<Int>
)

data class ShipBlueprint(
    val name: String,
    val vertices: List<Vector3>,
    val edges: List<Edge>,
    val faces: List<Face> = emptyList(),
    val baseScale: Float = 1.0f,
    val maxSpeed: Float = 30f,
    val bounty: Int = 0
)

object ShipBlueprints {

    // --- COBRA MK III (Lines 37357 - 37430) ---
    val COBRA_MK_3 = ShipBlueprint(
        name = "Cobra Mk III",
        baseScale = 0.8f,
        maxSpeed = 30f,
        bounty = 45,
        vertices = listOf(
            Vector3(32f, 0f, 76f),     // 0
            Vector3(-32f, 0f, 76f),    // 1
            Vector3(0f, 26f, 24f),     // 2
            Vector3(-120f, -3f, -8f),  // 3
            Vector3(120f, -3f, -8f),   // 4
            Vector3(-88f, 16f, -40f),  // 5
            Vector3(88f, 16f, -40f),   // 6
            Vector3(128f, -8f, -40f),  // 7
            Vector3(-128f, -8f, -40f), // 8
            Vector3(0f, 26f, -40f),    // 9
            Vector3(-32f, -24f, -40f), // 10
            Vector3(32f, -24f, -40f),  // 11
            Vector3(-36f, 8f, -40f),   // 12 (exhaust)
            Vector3(-8f, 12f, -40f),   // 13
            Vector3(8f, 12f, -40f),    // 14
            Vector3(36f, 8f, -40f),    // 15
            Vector3(36f, -12f, -40f),  // 16
            Vector3(8f, -16f, -40f),   // 17
            Vector3(-8f, -16f, -40f),  // 18
            Vector3(-36f, -12f, -40f)  // 19
        ),
        edges = listOf(
            Edge(0, 1), Edge(0, 4), Edge(1, 3), Edge(3, 8), Edge(4, 7),
            Edge(6, 7), Edge(6, 9), Edge(5, 9), Edge(5, 8), Edge(2, 5),
            Edge(2, 6), Edge(3, 5), Edge(4, 6), Edge(1, 2), Edge(0, 2),
            Edge(8, 10), Edge(10, 11), Edge(7, 11), Edge(1, 10), Edge(0, 11),
            Edge(1, 5), Edge(0, 6), Edge(2, 9),
            // Exhaust vent
            Edge(12, 13), Edge(18, 19), Edge(14, 15), Edge(16, 17),
            Edge(15, 16), Edge(14, 17), Edge(13, 18), Edge(12, 19)
        )
    )

    // --- CORIOLIS SPACE STATION (Lines 37590 - 37645) ---
    val CORIOLIS = ShipBlueprint(
        name = "Coriolis Station",
        baseScale = 1.2f,
        maxSpeed = 0f,
        bounty = 0,
        vertices = listOf(
            Vector3(160f, 0f, 160f),    // 0
            Vector3(0f, 160f, 160f),    // 1
            Vector3(-160f, 0f, 160f),   // 2
            Vector3(0f, -160f, 160f),   // 3
            Vector3(160f, -160f, 0f),   // 4
            Vector3(160f, 160f, 0f),    // 5
            Vector3(-160f, 160f, 0f),   // 6
            Vector3(-160f, -160f, 0f),  // 7
            Vector3(160f, 0f, -160f),   // 8
            Vector3(0f, 160f, -160f),   // 9
            Vector3(-160f, 0f, -160f),  // 10
            Vector3(0f, -160f, -160f),  // 11
            // Docking corridor portal (front face)
            Vector3(10f, -30f, 160f),   // 12
            Vector3(10f, 30f, 160f),    // 13
            Vector3(-10f, 30f, 160f),   // 14
            Vector3(-10f, -30f, 160f)   // 15
        ),
        edges = listOf(
            Edge(0, 3), Edge(0, 1), Edge(1, 2), Edge(2, 3),
            Edge(3, 4), Edge(0, 4), Edge(0, 5), Edge(5, 1),
            Edge(1, 6), Edge(2, 6), Edge(2, 7), Edge(3, 7),
            Edge(8, 11), Edge(8, 9), Edge(9, 10), Edge(10, 11),
            Edge(4, 11), Edge(4, 8), Edge(5, 8), Edge(5, 9),
            Edge(6, 9), Edge(6, 10), Edge(7, 10), Edge(7, 11),
            // Docking Slot edges
            Edge(12, 13), Edge(13, 14), Edge(14, 15), Edge(15, 12)
        ),
        faces = listOf(
            Face(Vector3(0f, 0f, 1f), listOf(0, 1, 2, 3)),
            Face(Vector3(0f, 0f, -1f), listOf(8, 11, 10, 9)),
            Face(Vector3(1f, 0f, 0f), listOf(0, 5, 8, 4)),
            Face(Vector3(-1f, 0f, 0f), listOf(2, 7, 10, 6)),
            Face(Vector3(0f, 1f, 0f), listOf(1, 6, 9, 5)),
            Face(Vector3(0f, -1f, 0f), listOf(3, 4, 11, 7)),
            Face(Vector3(0.577f, 0.577f, 0.577f), listOf(0, 1, 5)),
            Face(Vector3(-0.577f, 0.577f, 0.577f), listOf(1, 2, 6)),
            Face(Vector3(-0.577f, -0.577f, 0.577f), listOf(2, 3, 7)),
            Face(Vector3(0.577f, -0.577f, 0.577f), listOf(3, 0, 4)),
            Face(Vector3(0.577f, 0.577f, -0.577f), listOf(5, 9, 8)),
            Face(Vector3(-0.577f, 0.577f, -0.577f), listOf(6, 10, 9)),
            Face(Vector3(-0.577f, -0.577f, -0.577f), listOf(7, 11, 10)),
            Face(Vector3(0.577f, -0.577f, -0.577f), listOf(4, 8, 11))
        )
    )

    // --- DODECAHEDRON SPACE STATION (High-Tech System Dodec Station) ---
    val DODEC = ShipBlueprint(
        name = "Dodecahedron Station",
        baseScale = 1.25f,
        maxSpeed = 0f,
        bounty = 0,
        vertices = listOf(
            Vector3(80f, 80f, 80f),               // 0
            Vector3(80f, 80f, -80f),              // 1
            Vector3(80f, -80f, 80f),              // 2
            Vector3(80f, -80f, -80f),             // 3
            Vector3(-80f, 80f, 80f),              // 4
            Vector3(-80f, 80f, -80f),             // 5
            Vector3(-80f, -80f, 80f),             // 6
            Vector3(-80f, -80f, -80f),            // 7
            Vector3(0f, 49.4f, 129.4f),           // 8 (front top)
            Vector3(0f, 49.4f, -129.4f),          // 9 (rear top)
            Vector3(0f, -49.4f, 129.4f),          // 10 (front bottom)
            Vector3(0f, -49.4f, -129.4f),         // 11 (rear bottom)
            Vector3(49.4f, 129.4f, 0f),           // 12 (top right)
            Vector3(49.4f, -129.4f, 0f),          // 13 (bottom right)
            Vector3(-49.4f, 129.4f, 0f),          // 14 (top left)
            Vector3(-49.4f, -129.4f, 0f),         // 15 (bottom left)
            Vector3(129.4f, 0f, 49.4f),           // 16 (right front)
            Vector3(129.4f, 0f, -49.4f),          // 17 (right rear)
            Vector3(-129.4f, 0f, 49.4f),          // 18 (left front)
            Vector3(-129.4f, 0f, -49.4f),         // 19 (left rear)
            // Docking corridor slot on front pentagon (+Z face)
            Vector3(12f, -32f, 131f),             // 20
            Vector3(12f, 32f, 131f),              // 21
            Vector3(-12f, 32f, 131f),             // 22
            Vector3(-12f, -32f, 131f)             // 23
        ),
        edges = listOf(
            // Dodecahedron 30 pentagonal boundary edges
            Edge(8, 0), Edge(0, 16), Edge(16, 2), Edge(2, 10), Edge(10, 8),
            Edge(8, 4), Edge(4, 18), Edge(18, 6), Edge(6, 10),
            Edge(12, 0), Edge(12, 14), Edge(14, 4),
            Edge(16, 17), Edge(17, 1), Edge(1, 12),
            Edge(2, 13), Edge(13, 15), Edge(15, 6),
            Edge(18, 19), Edge(19, 7), Edge(7, 15),
            Edge(14, 5), Edge(5, 19),
            Edge(1, 9), Edge(9, 5),
            Edge(9, 11), Edge(11, 7),
            Edge(17, 3), Edge(3, 13),
            Edge(3, 11),
            // Docking slot aperture
            Edge(20, 21), Edge(21, 22), Edge(22, 23), Edge(23, 20)
        ),
        faces = listOf(
            Face(Vector3(0f, 0f, 1f), listOf(8, 0, 16, 2, 10)),
            Face(Vector3(-0.6f, 0f, 0.8f), listOf(8, 10, 6, 18, 4)),
            Face(Vector3(0f, 0.85f, 0.52f), listOf(8, 4, 14, 12, 0)),
            Face(Vector3(0f, -0.85f, 0.52f), listOf(10, 2, 13, 15, 6)),
            Face(Vector3(0.85f, 0.52f, 0f), listOf(0, 12, 1, 17, 16)),
            Face(Vector3(0.85f, -0.52f, 0f), listOf(2, 16, 17, 3, 13)),
            Face(Vector3(0f, 0.85f, -0.52f), listOf(9, 1, 12, 14, 5)),
            Face(Vector3(-0.6f, 0f, -0.8f), listOf(9, 5, 19, 7, 11)),
            Face(Vector3(0.52f, 0f, -0.85f), listOf(9, 11, 3, 17, 1)),
            Face(Vector3(-0.85f, 0.52f, 0f), listOf(4, 18, 19, 5, 14)),
            Face(Vector3(-0.85f, -0.52f, 0f), listOf(6, 15, 7, 19, 18)),
            Face(Vector3(0f, -0.85f, -0.52f), listOf(11, 7, 15, 13, 3))
        )
    )

    // --- KRAIT (Pirate Raider, Classic 1984 Elite) ---
    val KRAIT = ShipBlueprint(
        name = "Krait",
        baseScale = 0.72f,
        maxSpeed = 38f,
        bounty = 40,
        vertices = listOf(
            Vector3(0f, 0f, 84f),      // 0 (needle nose)
            Vector3(0f, 18f, 10f),     // 1 (dorsal canopy apex)
            Vector3(0f, -14f, 10f),    // 2 (ventral keel apex)
            Vector3(96f, -10f, -48f),  // 3 (starboard wingtip)
            Vector3(-96f, -10f, -48f), // 4 (port wingtip)
            Vector3(48f, 12f, -48f),   // 5 (starboard upper wing)
            Vector3(-48f, 12f, -48f),  // 6 (port upper wing)
            Vector3(0f, 16f, -48f),    // 7 (upper stern)
            Vector3(0f, -16f, -48f),   // 8 (lower stern)
            Vector3(12f, 6f, 26f),     // 9 (starboard canopy glass)
            Vector3(-12f, 6f, 26f)     // 10 (port canopy glass)
        ),
        edges = listOf(
            Edge(0, 1), Edge(0, 2), Edge(0, 3), Edge(0, 4),
            Edge(1, 5), Edge(1, 6), Edge(2, 3), Edge(2, 4),
            Edge(3, 5), Edge(4, 6),
            Edge(5, 7), Edge(6, 7), Edge(3, 8), Edge(4, 8),
            Edge(7, 8),
            // Cockpit glass frame
            Edge(0, 9), Edge(0, 10), Edge(9, 10), Edge(1, 9), Edge(1, 10)
        ),
        faces = listOf(
            Face(Vector3(0.5f, 0.7f, 0.3f).normalized(), listOf(0, 1, 5, 3)),
            Face(Vector3(-0.5f, 0.7f, 0.3f).normalized(), listOf(0, 1, 6, 4)),
            Face(Vector3(0f, -0.9f, 0.3f).normalized(), listOf(0, 2, 3)),
            Face(Vector3(0f, -0.9f, 0.3f).normalized(), listOf(0, 2, 4)),
            Face(Vector3(0f, 0.8f, -0.6f).normalized(), listOf(1, 5, 7, 6)),
            Face(Vector3(0f, -0.8f, -0.6f).normalized(), listOf(2, 3, 8, 4)),
            Face(Vector3(0f, 0f, -1f), listOf(5, 7, 8, 3)),
            Face(Vector3(0f, 0f, -1f), listOf(6, 7, 8, 4))
        )
    )

    // --- GECKO (Agile Pirate Light Fighter) ---
    val GECKO = ShipBlueprint(
        name = "Gecko",
        baseScale = 0.68f,
        maxSpeed = 42f,
        bounty = 35,
        vertices = listOf(
            Vector3(0f, 4f, 64f),      // 0 (nose)
            Vector3(24f, 10f, 12f),    // 1 (starboard shoulder)
            Vector3(-24f, 10f, 12f),   // 2 (port shoulder)
            Vector3(0f, -12f, 12f),    // 3 (keel)
            Vector3(72f, 0f, -32f),    // 4 (starboard wing)
            Vector3(-72f, 0f, -32f),   // 5 (port wing)
            Vector3(24f, 18f, -32f),   // 6 (starboard fin)
            Vector3(-24f, 18f, -32f),  // 7 (port fin)
            Vector3(20f, -8f, -32f),   // 8 (starboard engine)
            Vector3(-20f, -8f, -32f)   // 9 (port engine)
        ),
        edges = listOf(
            Edge(0, 1), Edge(0, 2), Edge(0, 3),
            Edge(1, 4), Edge(2, 5), Edge(3, 4), Edge(3, 5),
            Edge(1, 6), Edge(2, 7), Edge(4, 6), Edge(5, 7),
            Edge(6, 8), Edge(7, 9), Edge(8, 9), Edge(3, 8), Edge(3, 9)
        ),
        faces = listOf(
            Face(Vector3(0f, 0.8f, 0.5f).normalized(), listOf(0, 1, 2)),
            Face(Vector3(0.6f, 0.4f, 0.4f).normalized(), listOf(0, 1, 4, 3)),
            Face(Vector3(-0.6f, 0.4f, 0.4f).normalized(), listOf(0, 2, 5, 3)),
            Face(Vector3(0f, -0.9f, 0.1f).normalized(), listOf(3, 4, 8)),
            Face(Vector3(0f, -0.9f, 0.1f).normalized(), listOf(3, 5, 9)),
            Face(Vector3(0f, 0f, -1f), listOf(6, 7, 9, 8))
        )
    )

    // --- ASP MK II (Military Combat Craft) ---
    val ASP_MK_2 = ShipBlueprint(
        name = "Asp Mk II",
        baseScale = 0.78f,
        maxSpeed = 36f,
        bounty = 110,
        vertices = listOf(
            Vector3(0f, 0f, 88f),      // 0 (nose prow)
            Vector3(34f, 16f, 24f),    // 1 (dorsal starboard)
            Vector3(-34f, 16f, 24f),   // 2 (dorsal port)
            Vector3(34f, -16f, 24f),   // 3 (ventral starboard)
            Vector3(-34f, -16f, 24f),  // 4 (ventral port)
            Vector3(90f, 0f, -28f),    // 5 (starboard outrigger)
            Vector3(-90f, 0f, -28f),   // 6 (port outrigger)
            Vector3(34f, 22f, -60f),   // 7 (starboard fin top)
            Vector3(-34f, 22f, -60f),  // 8 (port fin top)
            Vector3(34f, -16f, -60f),  // 9 (starboard engine base)
            Vector3(-34f, -16f, -60f), // 10 (port engine base)
            Vector3(0f, 18f, -60f),    // 11 (stern top center)
            Vector3(0f, -14f, -60f)    // 12 (stern bottom center)
        ),
        edges = listOf(
            Edge(0, 1), Edge(0, 2), Edge(0, 3), Edge(0, 4),
            Edge(1, 2), Edge(3, 4), Edge(1, 3), Edge(2, 4),
            Edge(1, 5), Edge(3, 5), Edge(2, 6), Edge(4, 6),
            Edge(5, 7), Edge(5, 9), Edge(6, 8), Edge(6, 10),
            Edge(1, 7), Edge(2, 8), Edge(3, 9), Edge(4, 10),
            Edge(7, 11), Edge(8, 11), Edge(9, 12), Edge(10, 12),
            Edge(11, 12)
        ),
        faces = listOf(
            Face(Vector3(0f, 0.7f, 0.7f).normalized(), listOf(0, 1, 2)),
            Face(Vector3(0f, -0.7f, 0.7f).normalized(), listOf(0, 3, 4)),
            Face(Vector3(0.7f, 0f, 0.6f).normalized(), listOf(0, 1, 5, 3)),
            Face(Vector3(-0.7f, 0f, 0.6f).normalized(), listOf(0, 2, 6, 4)),
            Face(Vector3(0f, 0.8f, -0.4f).normalized(), listOf(1, 7, 11, 8, 2)),
            Face(Vector3(0f, -0.8f, -0.4f).normalized(), listOf(3, 9, 12, 10, 4)),
            Face(Vector3(0f, 0f, -1f), listOf(7, 8, 11, 12, 10, 9))
        )
    )

    // --- MORAY STAR BOAT (Aquatic / Atmospheric Combat Yacht) ---
    val MORAY = ShipBlueprint(
        name = "Moray Star Boat",
        baseScale = 0.75f,
        maxSpeed = 34f,
        bounty = 75,
        vertices = listOf(
            Vector3(0f, 0f, 80f),      // 0 (bow snout)
            Vector3(0f, 30f, 10f),     // 1 (dorsal ridge)
            Vector3(0f, -22f, 10f),    // 2 (ventral keel)
            Vector3(76f, 6f, -18f),    // 3 (starboard fin)
            Vector3(-76f, 6f, -18f),   // 4 (port fin)
            Vector3(0f, 42f, -50f),    // 5 (tail fin peak)
            Vector3(0f, 16f, -60f),    // 6 (stern top)
            Vector3(0f, -16f, -60f),   // 7 (stern bottom)
            Vector3(26f, 0f, -60f),    // 8 (starboard engine)
            Vector3(-26f, 0f, -60f)    // 9 (port engine)
        ),
        edges = listOf(
            Edge(0, 1), Edge(0, 2), Edge(0, 3), Edge(0, 4),
            Edge(1, 3), Edge(1, 4), Edge(2, 3), Edge(2, 4),
            Edge(1, 5), Edge(5, 6), Edge(2, 7),
            Edge(3, 8), Edge(4, 9), Edge(6, 8), Edge(6, 9),
            Edge(7, 8), Edge(7, 9), Edge(8, 9)
        ),
        faces = listOf(
            Face(Vector3(0.5f, 0.7f, 0.5f).normalized(), listOf(0, 1, 3)),
            Face(Vector3(-0.5f, 0.7f, 0.5f).normalized(), listOf(0, 1, 4)),
            Face(Vector3(0.5f, -0.7f, 0.5f).normalized(), listOf(0, 2, 3)),
            Face(Vector3(-0.5f, -0.7f, 0.5f).normalized(), listOf(0, 2, 4)),
            Face(Vector3(0f, 0.9f, -0.2f).normalized(), listOf(1, 5, 6)),
            Face(Vector3(0f, 0f, -1f), listOf(6, 8, 7, 9))
        )
    )

    // --- SIDEWINDER (Lines 37078 - 37120) ---
    val SIDEWINDER = ShipBlueprint(
        name = "Sidewinder",
        baseScale = 0.7f,
        maxSpeed = 35f,
        bounty = 25,
        vertices = listOf(
            Vector3(-32f, 0f, 36f),    // 0
            Vector3(32f, 0f, 36f),     // 1
            Vector3(64f, 0f, -28f),    // 2
            Vector3(-64f, 0f, -28f),   // 3
            Vector3(0f, 16f, -28f),    // 4
            Vector3(0f, -16f, -28f),   // 5
            Vector3(-12f, 6f, -28f),   // 6
            Vector3(12f, 6f, -28f),    // 7
            Vector3(12f, -6f, -28f),   // 8
            Vector3(-12f, -6f, -28f)   // 9
        ),
        edges = listOf(
            Edge(0, 1), Edge(1, 2), Edge(1, 4), Edge(0, 4),
            Edge(0, 3), Edge(3, 4), Edge(2, 4), Edge(3, 5),
            Edge(2, 5), Edge(1, 5), Edge(0, 5),
            // Rear Thruster
            Edge(6, 7), Edge(7, 8), Edge(6, 9), Edge(8, 9)
        )
    )

    // --- VIPER (POLICE) (Lines 37159 - 37195) ---
    val VIPER = ShipBlueprint(
        name = "Viper",
        baseScale = 0.75f,
        maxSpeed = 40f,
        bounty = 50,
        vertices = listOf(
            Vector3(0f, 0f, 72f),      // 0
            Vector3(0f, 16f, 24f),     // 1
            Vector3(0f, -16f, 24f),    // 2
            Vector3(48f, 0f, -24f),    // 3
            Vector3(-48f, 0f, -24f),   // 4
            Vector3(24f, -16f, -24f),  // 5
            Vector3(-24f, -16f, -24f), // 6
            Vector3(24f, 16f, -24f),   // 7
            Vector3(-24f, 16f, -24f)   // 8
        ),
        edges = listOf(
            Edge(0, 3), Edge(0, 1), Edge(0, 2), Edge(0, 4),
            Edge(1, 7), Edge(1, 8), Edge(2, 5), Edge(2, 6),
            Edge(7, 8), Edge(5, 6), Edge(4, 8), Edge(4, 6),
            Edge(3, 7), Edge(3, 5)
        )
    )

    // --- MAMBA (PIRATE) (Lines 37250 - 37290) ---
    val MAMBA = ShipBlueprint(
        name = "Mamba",
        baseScale = 0.75f,
        maxSpeed = 38f,
        bounty = 60,
        vertices = listOf(
            Vector3(0f, 0f, 64f),      // 0
            Vector3(-64f, -8f, -32f),  // 1
            Vector3(-32f, 8f, -32f),   // 2
            Vector3(32f, 8f, -32f),    // 3
            Vector3(64f, -8f, -32f),   // 4
            Vector3(-4f, 4f, 16f),     // 5
            Vector3(4f, 4f, 16f),      // 6
            Vector3(8f, 3f, 28f),      // 7
            Vector3(-8f, 3f, 28f)      // 8
        ),
        edges = listOf(
            Edge(0, 1), Edge(0, 4), Edge(1, 4), Edge(1, 2),
            Edge(2, 3), Edge(3, 4),
            // Cockpit glass
            Edge(5, 6), Edge(6, 7), Edge(7, 8), Edge(5, 8)
        )
    )

    // --- THARGOID (Lines 37485 - 37525) ---
    val THARGOID = ShipBlueprint(
        name = "Thargoid",
        baseScale = 0.85f,
        maxSpeed = 32f,
        bounty = 100,
        vertices = listOf(
            Vector3(32f, -48f, 48f),   // 0
            Vector3(32f, -68f, 0f),    // 1
            Vector3(32f, -48f, -48f),  // 2
            Vector3(32f, 0f, -68f),    // 3
            Vector3(32f, 48f, -48f),   // 4
            Vector3(32f, 68f, 0f),     // 5
            Vector3(32f, 48f, 48f),    // 6
            Vector3(32f, 0f, 68f),     // 7
            Vector3(-24f, -116f, 116f),// 8
            Vector3(-24f, -164f, 0f),  // 9
            Vector3(-24f, -116f, -116f),// 10
            Vector3(-24f, 0f, -164f),  // 11
            Vector3(-24f, 116f, -116f),// 12
            Vector3(-24f, 164f, 0f),   // 13
            Vector3(-24f, 116f, 116f), // 14
            Vector3(-24f, 0f, 164f)    // 15
        ),
        edges = listOf(
            Edge(0, 7), Edge(0, 1), Edge(1, 2), Edge(2, 3),
            Edge(3, 4), Edge(4, 5), Edge(5, 6), Edge(6, 7),
            Edge(0, 8), Edge(1, 9), Edge(2, 10), Edge(3, 11),
            Edge(4, 12), Edge(5, 13), Edge(6, 14), Edge(7, 15),
            Edge(8, 15), Edge(8, 9), Edge(9, 10), Edge(10, 11),
            Edge(11, 12), Edge(12, 13), Edge(13, 14), Edge(14, 15)
        )
    )

    // --- CARGO CANISTER (Lines 37889 - 37920) ---
    val CANISTER = ShipBlueprint(
        name = "Cargo Canister",
        baseScale = 0.5f,
        maxSpeed = 10f,
        bounty = 0,
        vertices = listOf(
            Vector3(24f, 16f, 0f),
            Vector3(24f, 5f, 15f),
            Vector3(24f, -13f, 9f),
            Vector3(24f, -13f, -9f),
            Vector3(24f, 5f, -15f),
            Vector3(-24f, 16f, 0f),
            Vector3(-24f, 5f, 15f),
            Vector3(-24f, -13f, 9f),
            Vector3(-24f, -13f, -9f),
            Vector3(-24f, 5f, -15f)
        ),
        edges = listOf(
            Edge(0, 1), Edge(1, 2), Edge(2, 3), Edge(3, 4), Edge(0, 4),
            Edge(0, 5), Edge(1, 6), Edge(2, 7), Edge(3, 8), Edge(4, 9),
            Edge(5, 6), Edge(6, 7), Edge(7, 8), Edge(8, 9), Edge(9, 5)
        )
    )

    // --- ASTEROID (Lines 37796 - 37830) ---
    val ASTEROID = ShipBlueprint(
        name = "Asteroid",
        baseScale = 0.9f,
        maxSpeed = 5f,
        bounty = 0,
        vertices = listOf(
            Vector3(0f, 80f, 0f),
            Vector3(-80f, -10f, 0f),
            Vector3(0f, -80f, 0f),
            Vector3(70f, -40f, 0f),
            Vector3(60f, 50f, 0f),
            Vector3(50f, 0f, 60f),
            Vector3(-40f, 0f, 70f),
            Vector3(0f, 30f, -75f),
            Vector3(0f, -50f, -60f)
        ),
        edges = listOf(
            Edge(0, 1), Edge(0, 4), Edge(3, 4), Edge(2, 3), Edge(1, 2),
            Edge(1, 6), Edge(2, 6), Edge(2, 5), Edge(5, 6), Edge(0, 5),
            Edge(3, 5), Edge(0, 6), Edge(4, 5), Edge(1, 8), Edge(1, 7),
            Edge(0, 7), Edge(4, 7), Edge(3, 7), Edge(3, 8), Edge(2, 8),
            Edge(7, 8)
        )
    )

    // --- MISSILE (Lines 37697 - 37730) ---
    val MISSILE = ShipBlueprint(
        name = "Missile",
        baseScale = 0.4f,
        maxSpeed = 60f,
        bounty = 0,
        vertices = listOf(
            Vector3(0f, 0f, 68f),
            Vector3(8f, -8f, 36f),
            Vector3(8f, 8f, 36f),
            Vector3(-8f, 8f, 36f),
            Vector3(-8f, -8f, 36f),
            Vector3(8f, 8f, -44f),
            Vector3(8f, -8f, -44f),
            Vector3(-8f, -8f, -44f),
            Vector3(-8f, 8f, -44f)
        ),
        edges = listOf(
            Edge(0, 1), Edge(0, 2), Edge(0, 3), Edge(0, 4),
            Edge(1, 2), Edge(1, 4), Edge(3, 4), Edge(2, 3),
            Edge(2, 5), Edge(1, 6), Edge(4, 7), Edge(3, 8),
            Edge(5, 6), Edge(6, 7), Edge(7, 8), Edge(8, 5)
        )
    )

    // --- PYTHON (Lines 38096 - 38200) ---
    val PYTHON = ShipBlueprint(
        name = "Python",
        baseScale = 0.8f,
        maxSpeed = 20f,
        bounty = 150,
        vertices = listOf(
            Vector3(0f, 0f, 224f),     // 0
            Vector3(0f, 48f, 48f),     // 1
            Vector3(96f, 0f, -16f),    // 2
            Vector3(-96f, 0f, -16f),   // 3
            Vector3(0f, 48f, -32f),    // 4
            Vector3(0f, 24f, -112f),   // 5
            Vector3(-48f, 0f, -112f),  // 6
            Vector3(48f, 0f, -112f),   // 7
            Vector3(0f, -48f, 48f),    // 8
            Vector3(0f, -48f, -32f),   // 9
            Vector3(0f, -24f, -112f)   // 10
        ),
        edges = listOf(
            Edge(0, 8), Edge(0, 3), Edge(0, 2), Edge(0, 1), Edge(2, 4), Edge(1, 2),
            Edge(2, 8), Edge(1, 3), Edge(3, 8), Edge(2, 9), Edge(3, 4), Edge(3, 9),
            Edge(3, 5), Edge(3, 10), Edge(2, 5), Edge(2, 10), Edge(2, 7), Edge(3, 6),
            Edge(5, 6), Edge(5, 7), Edge(7, 10), Edge(6, 10), Edge(4, 5), Edge(9, 10),
            Edge(1, 4), Edge(8, 9)
        )
    )

    // --- FER-DE-LANCE (Classic Hunter Yacht) ---
    val FER_DE_LANCE = ShipBlueprint(
        name = "Fer-de-Lance",
        baseScale = 0.7f,
        maxSpeed = 42f,
        bounty = 180,
        vertices = listOf(
            Vector3(0f, 0f, 160f),      // 0 (needle nose)
            Vector3(0f, 22f, 30f),      // 1 (dorsal crest)
            Vector3(0f, -14f, 30f),     // 2 (ventral keel)
            Vector3(80f, 0f, -60f),     // 3 (starboard wing)
            Vector3(-80f, 0f, -60f),    // 4 (port wing)
            Vector3(36f, 18f, -110f),   // 5 (starboard upper fin)
            Vector3(-36f, 18f, -110f),  // 6 (port upper fin)
            Vector3(36f, -16f, -110f),  // 7 (starboard lower fin)
            Vector3(-36f, -16f, -110f), // 8 (port lower fin)
            Vector3(0f, 18f, -110f),    // 9 (upper stern)
            Vector3(0f, -16f, -110f)    // 10 (lower stern)
        ),
        edges = listOf(
            Edge(0, 1), Edge(0, 2), Edge(0, 3), Edge(0, 4),
            Edge(1, 3), Edge(1, 4), Edge(2, 3), Edge(2, 4),
            Edge(3, 5), Edge(3, 7), Edge(4, 6), Edge(4, 8),
            Edge(1, 9), Edge(2, 10),
            Edge(5, 9), Edge(6, 9), Edge(7, 10), Edge(8, 10),
            Edge(5, 7), Edge(6, 8), Edge(9, 10)
        )
    )

    // --- ANACONDA (Heavy Cruiser) ---
    val ANACONDA = ShipBlueprint(
        name = "Anaconda",
        baseScale = 0.9f,
        maxSpeed = 18f,
        bounty = 250,
        vertices = listOf(
            Vector3(0f, 28f, 210f),     // 0 (bow top)
            Vector3(0f, -28f, 210f),    // 1 (bow bottom)
            Vector3(110f, 18f, 50f),    // 2 (starboard fore)
            Vector3(-110f, 18f, 50f),   // 3 (port fore)
            Vector3(110f, -18f, 50f),   // 4
            Vector3(-110f, -18f, 50f),  // 5
            Vector3(130f, 30f, -150f),  // 6 (starboard stern top)
            Vector3(-130f, 30f, -150f), // 7 (port stern top)
            Vector3(130f, -30f, -150f), // 8 (starboard stern bottom)
            Vector3(-130f, -30f, -150f),// 9 (port stern bottom)
            Vector3(0f, 52f, -40f),     // 10 (command bridge apex)
            Vector3(0f, 34f, 40f),      // 11 (bridge fore)
            Vector3(0f, 45f, -150f),    // 12 (bridge aft)
            Vector3(0f, -30f, -150f)    // 13 (ventral aft)
        ),
        edges = listOf(
            Edge(0, 1), Edge(0, 2), Edge(0, 3), Edge(1, 4), Edge(1, 5),
            Edge(2, 4), Edge(3, 5), Edge(2, 6), Edge(3, 7), Edge(4, 8), Edge(5, 9),
            Edge(6, 7), Edge(8, 9), Edge(6, 8), Edge(7, 9),
            Edge(0, 11), Edge(11, 10), Edge(10, 12), Edge(12, 6), Edge(12, 7),
            Edge(1, 13), Edge(13, 8), Edge(13, 9)
        )
    )

    // --- THARGON (Alien Drone, Lines 37935 - 37980) ---
    val THARGON = ShipBlueprint(
        name = "Thargon",
        baseScale = 0.55f,
        maxSpeed = 36f,
        bounty = 40,
        vertices = listOf(
            Vector3(-9f, 0f, 40f),
            Vector3(-9f, -38f, 12f),
            Vector3(-9f, -24f, -32f),
            Vector3(-9f, 24f, -32f),
            Vector3(-9f, 38f, 12f),
            Vector3(9f, 0f, -8f),
            Vector3(9f, -10f, -15f),
            Vector3(9f, -6f, -26f),
            Vector3(9f, 6f, -26f),
            Vector3(9f, 10f, -15f)
        ),
        edges = listOf(
            Edge(0, 1), Edge(1, 2), Edge(2, 3), Edge(3, 4), Edge(0, 4),
            Edge(0, 5), Edge(1, 6), Edge(2, 7), Edge(3, 8), Edge(4, 9),
            Edge(5, 6), Edge(6, 7), Edge(7, 8), Edge(8, 9), Edge(9, 5)
        )
    )

    // --- ESCAPE POD (Lines 38002 - 38050) ---
    val ESCAPE_POD = ShipBlueprint(
        name = "Escape Pod",
        baseScale = 0.5f,
        maxSpeed = 12f,
        bounty = 0,
        vertices = listOf(
            Vector3(-7f, 0f, 36f),
            Vector3(-7f, -14f, -12f),
            Vector3(-7f, 14f, -12f),
            Vector3(21f, 0f, 0f)
        ),
        edges = listOf(
            Edge(0, 1), Edge(1, 2), Edge(2, 3), Edge(3, 0), Edge(0, 2), Edge(3, 1)
        )
    )

    // --- 3D WIREFRAME PLANET (Procedural Sphere with Equator & Meridians) ---
    fun createPlanetBlueprint(radius: Float = 400f): ShipBlueprint {
        val verts = mutableListOf<Vector3>()
        val edges = mutableListOf<Edge>()

        // 1. Equatorial circle (24 points at y = 0)
        val eqCount = 24
        val eqStart = verts.size
        for (i in 0 until eqCount) {
            val angle = (i * 2.0 * Math.PI / eqCount).toFloat()
            verts.add(Vector3(kotlin.math.cos(angle) * radius, 0f, kotlin.math.sin(angle) * radius))
            val next = (i + 1) % eqCount
            edges.add(Edge(eqStart + i, eqStart + next))
        }

        // 2. Upper latitude circle (+45 deg, r = radius * 0.707, y = radius * 0.707)
        val latCount = 16
        val latY = radius * 0.707f
        val latR = radius * 0.707f
        val latUpStart = verts.size
        for (i in 0 until latCount) {
            val angle = (i * 2.0 * Math.PI / latCount).toFloat()
            verts.add(Vector3(kotlin.math.cos(angle) * latR, latY, kotlin.math.sin(angle) * latR))
            val next = (i + 1) % latCount
            edges.add(Edge(latUpStart + i, latUpStart + next))
        }

        // 3. Lower latitude circle (-45 deg)
        val latDnStart = verts.size
        for (i in 0 until latCount) {
            val angle = (i * 2.0 * Math.PI / latCount).toFloat()
            verts.add(Vector3(kotlin.math.cos(angle) * latR, -latY, kotlin.math.sin(angle) * latR))
            val next = (i + 1) % latCount
            edges.add(Edge(latDnStart + i, latDnStart + next))
        }

        // 4. Longitudinal Meridians (2 orthogonal vertical rings)
        val merCount = 24
        val mer1Start = verts.size
        for (i in 0 until merCount) {
            val angle = (i * 2.0 * Math.PI / merCount).toFloat()
            verts.add(Vector3(0f, kotlin.math.cos(angle) * radius, kotlin.math.sin(angle) * radius))
            val next = (i + 1) % merCount
            edges.add(Edge(mer1Start + i, mer1Start + next))
        }

        val mer2Start = verts.size
        for (i in 0 until merCount) {
            val angle = (i * 2.0 * Math.PI / merCount).toFloat()
            verts.add(Vector3(kotlin.math.cos(angle) * radius, kotlin.math.sin(angle) * radius, 0f))
            val next = (i + 1) % merCount
            edges.add(Edge(mer2Start + i, mer2Start + next))
        }

        // 5. Equatorial Ring (Planetary Ring system)
        val ringCount = 24
        val ringR = radius * 1.55f
        val ringStart = verts.size
        for (i in 0 until ringCount) {
            val angle = (i * 2.0 * Math.PI / ringCount).toFloat()
            verts.add(Vector3(kotlin.math.cos(angle) * ringR, 0f, kotlin.math.sin(angle) * ringR))
            val next = (i + 1) % ringCount
            edges.add(Edge(ringStart + i, ringStart + next))
        }

        return ShipBlueprint(
            name = "Planet",
            baseScale = 1.0f,
            maxSpeed = 0f,
            bounty = 0,
            vertices = verts,
            edges = edges
        )
    }

    // --- 3D WIREFRAME SUN (Blazing Star with Coronal Rays) ---
    fun createSunBlueprint(radius: Float = 350f): ShipBlueprint {
        val verts = mutableListOf<Vector3>()
        val edges = mutableListOf<Edge>()

        // Core 3D circles
        val segs = 20
        val c1Start = verts.size
        for (i in 0 until segs) {
            val a = (i * 2.0 * Math.PI / segs).toFloat()
            verts.add(Vector3(kotlin.math.cos(a) * radius, kotlin.math.sin(a) * radius, 0f))
            edges.add(Edge(c1Start + i, c1Start + ((i + 1) % segs)))
        }

        val c2Start = verts.size
        for (i in 0 until segs) {
            val a = (i * 2.0 * Math.PI / segs).toFloat()
            verts.add(Vector3(kotlin.math.cos(a) * radius, 0f, kotlin.math.sin(a) * radius))
            edges.add(Edge(c2Start + i, c2Start + ((i + 1) % segs)))
        }

        // Radiating Coronal Flare Spikes (16 spikes)
        val flareCount = 16
        for (i in 0 until flareCount) {
            val a = (i * 2.0 * Math.PI / flareCount).toFloat()
            val spikeLen = if (i % 2 == 0) radius * 1.6f else radius * 1.35f
            val baseIdx = verts.size
            val tipIdx = baseIdx + 1
            verts.add(Vector3(kotlin.math.cos(a) * radius, kotlin.math.sin(a) * radius, 0f))
            verts.add(Vector3(kotlin.math.cos(a) * spikeLen, kotlin.math.sin(a) * spikeLen, 0f))
            edges.add(Edge(baseIdx, tipIdx))
        }

        return ShipBlueprint(
            name = "Sun",
            baseScale = 1.0f,
            maxSpeed = 0f,
            bounty = 0,
            vertices = verts,
            edges = edges
        )
    }

    val PLANET = createPlanetBlueprint()
    val SUN = createSunBlueprint()

    // --- SHIP LORE & MANUAL SPECIFICATIONS ---
    data class ShipLoreData(
        val blueprint: ShipBlueprint,
        val manufacturer: String,
        val dimensions: String,
        val cargoCapacity: String,
        val maxSpeedMps: String,
        val hyperdriveCapable: Boolean,
        val weaponMounts: String,
        val description: String
    )

    val SHIP_CATALOG: List<ShipLoreData> = listOf(
        ShipLoreData(
            blueprint = COBRA_MK_3,
            manufacturer = "Cowell & McGrath Shipyards, Lave",
            dimensions = "65 x 30 x 130 ft",
            cargoCapacity = "20 Tonnes (Expandable to 35t)",
            maxSpeedMps = "0.30 LM",
            hyperdriveCapable = true,
            weaponMounts = "Fore & Aft Pulse/Beam/Military",
            description = "The classic multi-purpose trading and combat vessel. Fast, well-shielded, and versatile. The commander's standard craft."
        ),
        ShipLoreData(
            blueprint = CORIOLIS,
            manufacturer = "Galactic Cooperative of Worlds",
            dimensions = "1 x 1 x 1 km (Standard)",
            cargoCapacity = "N/A (Colony Station)",
            maxSpeedMps = "0.0 LM (Rotational 0.4 rad/s)",
            hyperdriveCapable = false,
            weaponMounts = "Heavy Defense Turrets & Police Vipers",
            description = "Iconic cuboctahedral orbital space station featuring internal anti-gravity docking bays, continuous axial rotation, and flashing entrance beacons."
        ),
        ShipLoreData(
            blueprint = DODEC,
            manufacturer = "Galactic Cooperative Corporate Worlds",
            dimensions = "1.2 x 1.2 x 1.2 km (Heavy)",
            cargoCapacity = "N/A (Corporate Super-Station)",
            maxSpeedMps = "0.0 LM (Rotational 0.35 rad/s)",
            hyperdriveCapable = false,
            weaponMounts = "Naval Defense Batteries & Elite Police Wings",
            description = "Majestic 12-sided regular pentagonal dodecahedron space station situated in wealthy high-tech corporate state systems."
        ),
        ShipLoreData(
            blueprint = KRAIT,
            manufacturer = "Novi Sad Shipyards (Pirate Outlaw Variant)",
            dimensions = "40 x 14 x 75 ft",
            cargoCapacity = "0 Tonnes",
            maxSpeedMps = "0.38 LM",
            hyperdriveCapable = false,
            weaponMounts = "Fore Beam Laser",
            description = "Iconic angular triangular outlaw raider from original Elite. Notorious for lightning-fast diving attack passes against merchant convoys."
        ),
        ShipLoreData(
            blueprint = GECKO,
            manufacturer = "Ace & Aron Interplanetary",
            dimensions = "38 x 14 x 45 ft",
            cargoCapacity = "3 Tonnes",
            maxSpeedMps = "0.42 LM",
            hyperdriveCapable = true,
            weaponMounts = "Fore Pulse Laser",
            description = "Agile pirate light interceptor and courier craft with swept-back wings. Highly nimble in dogfights."
        ),
        ShipLoreData(
            blueprint = ASP_MK_2,
            manufacturer = "Galactic Navy Shipyards",
            dimensions = "70 x 25 x 75 ft",
            cargoCapacity = "0 Tonnes (Military Armor)",
            maxSpeedMps = "0.36 LM",
            hyperdriveCapable = true,
            weaponMounts = "Fore Military Laser + Dual Missiles",
            description = "Heavy military attack and reconnaissance craft with octagonal hull plating and heavy-duty armor shielding."
        ),
        ShipLoreData(
            blueprint = MORAY,
            manufacturer = "Marine Trench Engineering, Spant",
            dimensions = "60 x 25 x 65 ft",
            cargoCapacity = "7 Tonnes",
            maxSpeedMps = "0.34 LM",
            hyperdriveCapable = true,
            weaponMounts = "Fore Beam Laser",
            description = "Unique dual-environment aquatic and space combat boat with diamond cross-section and dorsal stabilizer fin."
        ),
        ShipLoreData(
            blueprint = SIDEWINDER,
            manufacturer = "Faulcon deLacy",
            dimensions = "35 x 15 x 30 ft",
            cargoCapacity = "0 Tonnes",
            maxSpeedMps = "0.35 LM",
            hyperdriveCapable = false,
            weaponMounts = "Fore Pulse Laser",
            description = "Lightweight scout and pirate interceptor. Extremely agile, highly maneuverable, and frequently encountered in asteroid belts."
        ),
        ShipLoreData(
            blueprint = VIPER,
            manufacturer = "Faulcon Manspace",
            dimensions = "40 x 20 x 70 ft",
            cargoCapacity = "0 Tonnes",
            maxSpeedMps = "0.40 LM",
            hyperdriveCapable = false,
            weaponMounts = "Fore Military Laser",
            description = "Standard police interceptor for the Galactic Navy and station security forces. Rapid acceleration and formidable firepower."
        ),
        ShipLoreData(
            blueprint = MAMBA,
            manufacturer = "Gauron & Krait Yards",
            dimensions = "45 x 15 x 65 ft",
            cargoCapacity = "0 Tonnes",
            maxSpeedMps = "0.38 LM",
            hyperdriveCapable = false,
            weaponMounts = "Fore Beam Laser",
            description = "Aggressive raider favored by space outlaws. Notable for high speed and deadly diving passes against isolated merchant ships."
        ),
        ShipLoreData(
            blueprint = FER_DE_LANCE,
            manufacturer = "Zorgon Petterson Group",
            dimensions = "85 x 20 x 145 ft",
            cargoCapacity = "2 Tonnes",
            maxSpeedMps = "0.42 LM",
            hyperdriveCapable = true,
            weaponMounts = "Fore & Aft Beam/Military Lasers",
            description = "Prestigious luxury hunter vessel. Favored by wealthy bounty hunters and assassins. Exquisite needle-nose aerodynamics."
        ),
        ShipLoreData(
            blueprint = PYTHON,
            manufacturer = "Whatley Shipyards, Earth",
            dimensions = "130 x 40 x 220 ft",
            cargoCapacity = "100 Tonnes",
            maxSpeedMps = "0.20 LM",
            hyperdriveCapable = true,
            weaponMounts = "Fore, Aft & Starboard Lasers",
            description = "Heavy merchant freighter capable of bulk interplanetary cargo transport. Tough multi-layered shielding and missile pylons."
        ),
        ShipLoreData(
            blueprint = ANACONDA,
            manufacturer = "Rimward Heavy Industries",
            dimensions = "170 x 60 x 280 ft",
            cargoCapacity = "175 Tonnes",
            maxSpeedMps = "0.18 LM",
            hyperdriveCapable = true,
            weaponMounts = "Fore, Aft & Flank Lasers + Missiles",
            description = "Massive armored trading cruiser. Often operated by corporate convoys and naval logistics detachments in perilous systems."
        ),
        ShipLoreData(
            blueprint = THARGOID,
            manufacturer = "Thargoid Hive Mind (Octagonal)",
            dimensions = "150 x 80 x 150 ft",
            cargoCapacity = "Unknown (Alien Bio-Hold)",
            maxSpeedMps = "0.32 LM",
            hyperdriveCapable = true,
            weaponMounts = "Omni-directional Plasma + Thargon Drones",
            description = "Feared alien combat warship from Witch-Space. Capable of deploying swarms of autonomous remote Thargon mini-fighters."
        ),
        ShipLoreData(
            blueprint = THARGON,
            manufacturer = "Thargoid Hive Mind",
            dimensions = "25 x 15 x 30 ft",
            cargoCapacity = "0 Tonnes",
            maxSpeedMps = "0.36 LM",
            hyperdriveCapable = false,
            weaponMounts = "Pulse Beam Emitter",
            description = "Small, autonomous remote-controlled drone deployed by Thargoid motherships to harass and disorient hostile commanders."
        ),
        ShipLoreData(
            blueprint = ASTEROID,
            manufacturer = "Natural Planetary Ring Debris",
            dimensions = "Varies (80-200 ft)",
            cargoCapacity = "Minable Alloys & Minerals",
            maxSpeedMps = "0.05 LM (Drifting)",
            hyperdriveCapable = false,
            weaponMounts = "None",
            description = "Irregular rocky bodies rich in minable ore. Can be blasted with mining lasers or standard weaponry into mineral canisters."
        ),
        ShipLoreData(
            blueprint = CANISTER,
            manufacturer = "Galactic Standard Freight Unit",
            dimensions = "10 x 8 x 15 ft",
            cargoCapacity = "1 Tonne Commodity",
            maxSpeedMps = "0.10 LM (Drifting)",
            hyperdriveCapable = false,
            weaponMounts = "None",
            description = "Standard jettisoned or salvaged cargo canister. Can be scooped into the ship's hold using cargo scoops or tractor bays."
        ),
        ShipLoreData(
            blueprint = PLANET,
            manufacturer = "Celestial Terrestrial World",
            dimensions = "4,000 - 12,000 km Radius",
            cargoCapacity = "Planetary Population & Industry",
            maxSpeedMps = "Orbital Velocity",
            hyperdriveCapable = false,
            weaponMounts = "Orbital Defense Grids",
            description = "Habitable planetary world with atmosphere, cloud bands, and oceans. System center of trade, production, and orbital docking."
        ),
        ShipLoreData(
            blueprint = SUN,
            manufacturer = "Stellar Fusion Furnace",
            dimensions = "500,000+ km Radius",
            cargoCapacity = "Solar Plasma / Witch-Fuel",
            maxSpeedMps = "Galactic Orbit",
            hyperdriveCapable = false,
            weaponMounts = "Extreme Thermal Corona & Radiation",
            description = "Primary star of the planetary system. Approaching with Fuel Scoops allows scooping plasma to refill hyperspace fuel banks."
        )
    )
}
