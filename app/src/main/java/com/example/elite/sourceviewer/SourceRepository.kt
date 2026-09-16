package com.example.elite.sourceviewer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class AsmRoutineBookmark(
    val label: String,
    val title: String,
    val line: Int,
    val category: String,
    val description: String
)

data class AsmFileLine(
    val lineNumber: Int,
    val text: String,
    val isComment: Boolean,
    val isLabel: Boolean
)

class SourceRepository(private val context: Context) {

    val availableFiles = listOf(
        "1-source-files/main-sources/elite-source.asm",
        "1-source-files/main-sources/elite-loader.asm",
        "1-source-files/main-sources/elite-bcfs.asm",
        "1-source-files/main-sources/elite-build-options.asm",
        "1-source-files/main-sources/elite-disc.asm",
        "1-source-files/main-sources/elite-readme.asm",
        "1-source-files/main-sources/README.md"
    )

    val bookmarks = listOf(
        AsmRoutineBookmark(
            label = "TT54",
            title = "TT54: Galaxy Seed Twist",
            line = 17528,
            category = "Universe",
            description = "Twists the three 16-bit seeds in QQ15 once to generate procedural galaxies."
        ),
        AsmRoutineBookmark(
            label = "cpl",
            title = "cpl: System Name Generator",
            line = 22167,
            category = "Text",
            description = "Converts seeds into 3 or 4 two-letter digraph tokens (e.g. LA + VE = Lave)."
        ),
        AsmRoutineBookmark(
            label = "TT24",
            title = "TT24: System Data Generator",
            line = 17565,
            category = "Universe",
            description = "Calculates economy, government, tech level, population and productivity."
        ),
        AsmRoutineBookmark(
            label = "TT151",
            title = "TT151: Market Price Calculation",
            line = 20407,
            category = "Market",
            description = "Calculates commodity price in deci-credits from base price and economy factor."
        ),
        AsmRoutineBookmark(
            label = "QQ23",
            title = "QQ23: 17 Commodities Table",
            line = 32511,
            category = "Market",
            description = "Data table containing base prices, economic factors, units, and quantities."
        ),
        AsmRoutineBookmark(
            label = "SHIP_COBRA_MK_3",
            title = "SHIP_COBRA_MK_3: Cobra 3D Model",
            line = 37331,
            category = "Drawing ships",
            description = "3D wireframe coordinates, vertex definitions, edges, and normals for player ship."
        ),
        AsmRoutineBookmark(
            label = "SHIP_CORIOLIS",
            title = "SHIP_CORIOLIS: Space Station",
            line = 37564,
            category = "Drawing ships",
            description = "Cuboctahedron Coriolis station wireframe model with rotating docking slot."
        ),
        AsmRoutineBookmark(
            label = "SHIP_VIPER",
            title = "SHIP_VIPER: Police Interceptor",
            line = 37133,
            category = "Drawing ships",
            description = "Fast law enforcement craft wireframe model deployed by GalCop stations."
        ),
        AsmRoutineBookmark(
            label = "SHIP_THARGOID",
            title = "SHIP_THARGOID: Alien Mothership",
            line = 37459,
            category = "Drawing ships",
            description = "Octagonal warship of the insectoid Thargoids that strikes in witchspace."
        ),
        AsmRoutineBookmark(
            label = "DIALS",
            title = "DIALS: Mode 5 Dashboard",
            line = 1275,
            category = "Dashboard",
            description = "Renders the dual-plane 3D scanner radar, compass, shields, and energy dials."
        ),
        AsmRoutineBookmark(
            label = "BAY",
            title = "BAY: Docking Bay Entry",
            line = 30040,
            category = "Status",
            description = "Sets docked status flag QQ12 = &FF and transitions to Status screen."
        )
    )

    suspend fun loadFileSlice(
        assetPath: String = availableFiles[0],
        startLine: Int = 1,
        count: Int = 200
    ): List<AsmFileLine> = withContext(Dispatchers.IO) {
        val lines = mutableListOf<AsmFileLine>()
        try {
            context.assets.open(assetPath).use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    var currentLine = 1
                    val endLine = startLine + count
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (currentLine in startLine until endLine) {
                            val trimmed = line.trimStart()
                            val isComment = trimmed.startsWith("\\") || trimmed.startsWith(";")
                            val isLabel = trimmed.startsWith(".")
                            lines.add(
                                AsmFileLine(
                                    lineNumber = currentLine,
                                    text = line,
                                    isComment = isComment,
                                    isLabel = isLabel
                                )
                            )
                        }
                        if (currentLine >= endLine) break
                        currentLine++
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback sample if asset stream is interrupted
            lines.add(
                AsmFileLine(
                    lineNumber = startLine,
                    text = "\\ 1-source-files/main-sources/elite-source.asm",
                    isComment = true,
                    isLabel = false
                )
            )
        }
        lines
    }
}
