package com.example.elite.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elite.audio.BbcSoundSynth
import com.example.elite.model.CommanderState
import com.example.elite.model.InvestmentType
import com.example.elite.model.StrategicContract
import com.example.elite.model.SystemData
import com.example.elite.model.TradeArbitrageEngine
import com.example.elite.universe.GalaxyGenerator

enum class MapFilter(val label: String) {
    ALL("STANDARD"),
    TRADE("TRADE VECTORS"),
    DANGER("HAZARD HEATMAP"),
    OUTPOSTS("COLONY EMPIRE")
}

@Composable
fun GalacticChartView(
    commander: CommanderState,
    systems: List<SystemData>,
    currentSystem: SystemData,
    isShortRange: Boolean = false,
    soundSynth: BbcSoundSynth? = null,
    onSystemSelected: (SystemData) -> Unit,
    onHyperspaceJump: (SystemData, Int) -> Unit,
    onInvest: ((SystemData, InvestmentType) -> Unit)? = null,
    onAcceptContract: ((StrategicContract) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTarget by remember { mutableStateOf(currentSystem) }
    var showSystem3d by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(MapFilter.ALL) }

    val distDeciLy = GalaxyGenerator.distanceInDeciLy(currentSystem, selectedTarget)
    val fuelNeeded = distDeciLy
    val hasFuel = commander.fuelDeciLy >= fuelNeeded

    // Compute top trade arbitrage routes from current system
    val topTradeRoutes = remember(currentSystem.id) {
        TradeArbitrageEngine.findTopArbitrageRoutes(currentSystem, systems, 7.0f, commander.cargoCapacity)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(6.dp)
            .testTag(if (isShortRange) "short_range_chart" else "galactic_chart")
    ) {
        // Chart Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isShortRange) "SHORT-RANGE TACTICAL CHART" else "GALACTIC CHART 1 (256 WORLDS)",
                color = BBC_YELLOW,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "FUEL: ${"%.1f".format(commander.fuelDeciLy / 10.0)} LY",
                color = BBC_CYAN,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Strategic Map Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (filter in MapFilter.values()) {
                val isSel = activeFilter == filter
                Button(
                    onClick = {
                        activeFilter = filter
                        soundSynth?.playBeep(true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSel) Color(0xFF005524) else Color(0xFF101C26)
                    ),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                ) {
                    Text(
                        text = filter.label,
                        color = if (isSel) BBC_WHITE else Color(0xFFAABBCC),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }

        // Map Canvas & 3D Planetary View Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .background(Color(0xFF020905))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isShortRange, currentSystem) {
                        detectTapGestures { tapOffset ->
                            val canvasW = size.width.toFloat()
                            val canvasH = size.height.toFloat()

                            var closest: SystemData? = null
                            var minDist = Float.MAX_VALUE

                            for (sys in systems) {
                                val (sx, sy) = if (isShortRange) {
                                    val relX = (sys.x - currentSystem.x) * (canvasW / 70f) + (canvasW / 2f)
                                    val relY = (sys.y - currentSystem.y) * (canvasH / 50f) + (canvasH / 2f)
                                    Pair(relX, relY)
                                } else {
                                    val px = (sys.x / 256f) * canvasW
                                    val py = (sys.y / 256f) * canvasH
                                    Pair(px, py)
                                }

                                val d = (tapOffset.x - sx) * (tapOffset.x - sx) + (tapOffset.y - sy) * (tapOffset.y - sy)
                                if (d < minDist) {
                                    minDist = d
                                    closest = sys
                                }
                            }

                            closest?.let {
                                selectedTarget = it
                                onSystemSelected(it)
                                soundSynth?.playBeep(true)
                                showSystem3d = true
                            }
                        }
                    }
            ) {
                val canvasW = size.width
                val canvasH = size.height

                fun systemToCoords(sys: SystemData): Offset {
                    return if (isShortRange) {
                        val relX = (sys.x - currentSystem.x) * (canvasW / 70f) + (canvasW / 2f)
                        val relY = (sys.y - currentSystem.y) * (canvasH / 50f) + (canvasH / 2f)
                        Offset(relX, relY)
                    } else {
                        Offset((sys.x / 256f) * canvasW, (sys.y / 256f) * canvasH)
                    }
                }

                val curCoords = systemToCoords(currentSystem)
                val fuelRangePx = if (isShortRange) {
                    (commander.fuelDeciLy / 10f) * (canvasW / 28f)
                } else {
                    (commander.fuelDeciLy / 10f) * (canvasW / 45f)
                }

                // 7.0 LY Hyperspace Range boundary circle
                drawCircle(
                    color = BBC_GREEN.copy(alpha = 0.35f),
                    radius = fuelRangePx,
                    center = curCoords,
                    style = Stroke(1.2f)
                )

                // 1. Draw Strategic Trade Vectors if TRADE filter active
                if (activeFilter == MapFilter.TRADE) {
                    for (route in topTradeRoutes) {
                        val targetCoords = systemToCoords(route.targetSystem)
                        drawLine(
                            color = Color(0xFF00FF66).copy(alpha = 0.75f),
                            start = curCoords,
                            end = targetCoords,
                            strokeWidth = 2.0f
                        )
                    }
                }

                // 2. Draw Systems
                for (sys in systems) {
                    val coords = systemToCoords(sys)
                    if (isShortRange && (coords.x !in 0f..canvasW || coords.y !in 0f..canvasH)) {
                        continue
                    }

                    val isCur = sys.id == currentSystem.id
                    val isSel = sys.id == selectedTarget.id
                    val hasColony = commander.investments.any { it.systemId == sys.id }

                    val baseColor = when (activeFilter) {
                        MapFilter.DANGER -> sys.securityLevel.color
                        MapFilter.OUTPOSTS -> if (hasColony) BBC_YELLOW else Color(0xFF445566)
                        else -> {
                            if (isCur) BBC_GREEN
                            else if (isSel) BBC_YELLOW
                            else sys.planetType.wireColor.copy(alpha = 0.85f)
                        }
                    }

                    val radius = if (isCur || isSel) 4.5f else if (hasColony) 3.5f else 2.2f
                    drawCircle(color = baseColor, radius = radius, center = coords)

                    // If Anarchy hazard and danger filter is active, draw alert halo
                    if (activeFilter == MapFilter.DANGER && sys.government == 0) {
                        drawCircle(
                            color = Color(0xFFFF2222).copy(alpha = 0.35f),
                            radius = 6f,
                            center = coords,
                            style = Stroke(1.0f)
                        )
                    }

                    // If has colony outpost, draw diamond frame
                    if (hasColony) {
                        drawCircle(
                            color = BBC_YELLOW.copy(alpha = 0.8f),
                            radius = 5.5f,
                            center = coords,
                            style = Stroke(1.0f)
                        )
                    }
                }

                // 3. Selection Reticle
                val selCoords = systemToCoords(selectedTarget)
                if (!isShortRange || (selCoords.x in 0f..canvasW && selCoords.y in 0f..canvasH)) {
                    val rLen = 10f
                    drawLine(color = BBC_YELLOW, start = Offset(selCoords.x - rLen, selCoords.y), end = Offset(selCoords.x + rLen, selCoords.y), strokeWidth = 1.4f)
                    drawLine(color = BBC_YELLOW, start = Offset(selCoords.x, selCoords.y - rLen), end = Offset(selCoords.x, selCoords.y + rLen), strokeWidth = 1.4f)
                }
            }

            // 3D Space Animation Zoom-in Overlay for Selected Planet / System
            androidx.compose.animation.AnimatedVisibility(
                visible = showSystem3d,
                enter = scaleIn(initialScale = 0.25f, animationSpec = tween(350)) + fadeIn(animationSpec = tween(350)),
                exit = scaleOut(targetScale = 0.25f, animationSpec = tween(250)) + fadeOut(animationSpec = tween(250)),
                modifier = Modifier.fillMaxSize()
            ) {
                PlanetarySystem3dView(
                    system = selectedTarget,
                    commander = commander,
                    allSystems = systems,
                    soundSynth = soundSynth,
                    onClose = { showSystem3d = false },
                    onHyperspaceJump = { sys ->
                        showSystem3d = false
                        onHyperspaceJump(sys, fuelNeeded)
                    },
                    onInvest = onInvest,
                    onAcceptContract = onAcceptContract,
                    onSelectTarget = { sys ->
                        selectedTarget = sys
                        onSystemSelected(sys)
                    },
                    hasFuel = hasFuel,
                    distDeciLy = distDeciLy,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Bottom Target Strategic Summary Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF07140B))
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "TARGET: ${selectedTarget.name.uppercase()}",
                        color = BBC_YELLOW,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "[${selectedTarget.planetType.displayName}]",
                        color = selectedTarget.planetType.wireColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "${selectedTarget.starType.displayName} • ${selectedTarget.faction.displayName} (${selectedTarget.securityLevel.title})",
                    color = Color(0xFFAABBCC),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "DIST: ${"%.1f".format(distDeciLy / 10.0)} LY  |  TECH: ${selectedTarget.techLevel}  |  POP: ${"%.1f".format(selectedTarget.population)}B",
                    color = if (hasFuel) BBC_GREEN else BBC_RED,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = {
                        soundSynth?.playBeep(true)
                        showSystem3d = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF162B3D)),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier.testTag("inspect_3d_system_btn")
                ) {
                    Text(
                        text = "3D STRATEGY",
                        color = BBC_CYAN,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        if (hasFuel && selectedTarget.id != currentSystem.id) {
                            onHyperspaceJump(selectedTarget, fuelNeeded)
                        }
                    },
                    enabled = hasFuel && selectedTarget.id != currentSystem.id,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF005518),
                        disabledContainerColor = Color(0xFF1E2822)
                    ),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier.testTag("hyperspace_jump_button")
                ) {
                    Text(
                        text = "HYPERSPACE",
                        color = if (hasFuel && selectedTarget.id != currentSystem.id) BBC_WHITE else BBC_GREY,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
