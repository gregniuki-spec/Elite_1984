package com.example.elite.ui

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
import com.example.elite.model.CommanderState
import com.example.elite.model.SystemData
import com.example.elite.universe.GalaxyGenerator
import kotlin.math.sqrt

@Composable
fun GalacticChartView(
    commander: CommanderState,
    systems: List<SystemData>,
    currentSystem: SystemData,
    isShortRange: Boolean = false,
    onSystemSelected: (SystemData) -> Unit,
    onHyperspaceJump: (SystemData, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTarget by remember { mutableStateOf(currentSystem) }

    val distDeciLy = GalaxyGenerator.distanceInDeciLy(currentSystem, selectedTarget)
    val fuelNeeded = distDeciLy
    val hasFuel = commander.fuelDeciLy >= fuelNeeded

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(8.dp)
            .testTag(if (isShortRange) "short_range_chart" else "galactic_chart")
    ) {
        // Chart Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isShortRange) "SHORT-RANGE CHART" else "GALACTIC CHART 1",
                color = BBC_YELLOW,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "FUEL: ${"%.1f".format(commander.fuelDeciLy / 10.0)} LY",
                color = BBC_CYAN,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Map Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(Color(0xFF020905))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isShortRange, currentSystem) {
                        detectTapGestures { tapOffset ->
                            val canvasW = size.width.toFloat()
                            val canvasH = size.height.toFloat()

                            // Find nearest system to tap
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
                            }
                        }
                    }
            ) {
                val canvasW = size.width
                val canvasH = size.height

                if (!isShortRange) {
                    // Full Galaxy Map (256 systems)
                    // Draw 7.0 LY jump range circle around current system
                    val curX = (currentSystem.x / 256f) * canvasW
                    val curY = (currentSystem.y / 256f) * canvasH
                    val rangePx = (commander.fuelDeciLy / 10f) * (canvasW / 45f)

                    drawCircle(
                        color = BBC_GREEN.copy(alpha = 0.35f),
                        radius = rangePx,
                        center = Offset(curX, curY),
                        style = Stroke(1.2f)
                    )

                    // Draw all systems
                    for (sys in systems) {
                        val px = (sys.x / 256f) * canvasW
                        val py = (sys.y / 256f) * canvasH
                        val isCur = sys.id == currentSystem.id
                        val isSel = sys.id == selectedTarget.id

                        val color = if (isCur) BBC_GREEN else if (isSel) BBC_YELLOW else BBC_WHITE
                        val rad = if (isCur || isSel) 4f else 2f
                        drawCircle(color = color, radius = rad, center = Offset(px, py))
                    }

                    // Target crosshair
                    val selX = (selectedTarget.x / 256f) * canvasW
                    val selY = (selectedTarget.y / 256f) * canvasH
                    drawLine(color = BBC_YELLOW, start = Offset(selX - 10f, selY), end = Offset(selX + 10f, selY), strokeWidth = 1.2f)
                    drawLine(color = BBC_YELLOW, start = Offset(selX, selY - 10f), end = Offset(selX, selY + 10f), strokeWidth = 1.2f)
                } else {
                    // Short-Range Magnified Chart (around current system)
                    val centerX = canvasW / 2f
                    val centerY = canvasH / 2f

                    // Fuel circle
                    val rangePx = (commander.fuelDeciLy / 10f) * (canvasW / 28f)
                    drawCircle(
                        color = BBC_GREEN.copy(alpha = 0.3f),
                        radius = rangePx,
                        center = Offset(centerX, centerY),
                        style = Stroke(1.2f)
                    )

                    // Systems in neighborhood
                    for (sys in systems) {
                        val relX = (sys.x - currentSystem.x) * (canvasW / 70f) + centerX
                        val relY = (sys.y - currentSystem.y) * (canvasH / 50f) + centerY

                        if (relX in 0f..canvasW && relY in 0f..canvasH) {
                            val isCur = sys.id == currentSystem.id
                            val isSel = sys.id == selectedTarget.id
                            val col = if (isCur) BBC_GREEN else if (isSel) BBC_YELLOW else BBC_WHITE
                            drawCircle(color = col, radius = if (isCur || isSel) 4.5f else 2.5f, center = Offset(relX, relY))
                        }
                    }

                    // Crosshair on selected
                    val selRelX = (selectedTarget.x - currentSystem.x) * (canvasW / 70f) + centerX
                    val selRelY = (selectedTarget.y - currentSystem.y) * (canvasH / 50f) + centerY
                    drawLine(color = BBC_YELLOW, start = Offset(selRelX - 12f, selRelY), end = Offset(selRelX + 12f, selRelY), strokeWidth = 1.2f)
                    drawLine(color = BBC_YELLOW, start = Offset(selRelX, selRelY - 12f), end = Offset(selRelX, selRelY + 12f), strokeWidth = 1.2f)
                }
            }
        }

        // Bottom Target Info & Hyperspace Jump Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF07140B))
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TARGET: ${selectedTarget.name.uppercase()} (${selectedTarget.governmentName})",
                    color = BBC_YELLOW,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "DISTANCE: ${"%.1f".format(distDeciLy / 10.0)} LY  |  TECH: ${selectedTarget.techLevel}",
                    color = if (hasFuel) BBC_GREEN else BBC_RED,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
