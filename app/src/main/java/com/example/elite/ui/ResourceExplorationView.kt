package com.example.elite.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.elite.flight.FlightEngine
import com.example.elite.math3d.Matrix3x3
import com.example.elite.math3d.Vector3
import com.example.elite.math3d.WireframeRenderer
import com.example.elite.model.CommanderState
import com.example.elite.model.SystemData
import com.example.elite.ships.ShipBlueprints
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class SurveySector(val title: String, val richDeposit: String, val mineralId: Int, val purity: Float) {
    EQUATORIAL_BASIN("EQUATORIAL BASIN", "TITANIUM ALLOYS", 9, 0.82f),
    VOLCANIC_RIFT("VOLCANIC RIFT", "MOLTEN GOLD VEIN", 13, 0.94f),
    POLAR_ICE_CAP("POLAR ICE CRUST", "RADIOACTIVE ISOTOPES", 2, 0.76f),
    ASTEROID_RING("PLANETARY RING", "CRYSTAL GEM-STONES", 15, 0.88f)
}

@Composable
fun ResourceExplorationView(
    system: SystemData,
    commander: CommanderState,
    flightEngine: FlightEngine,
    soundSynth: BbcSoundSynth,
    onLaunchAsteroidBelt: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pitchAngle by remember { mutableStateOf(0.35f) }
    var yawAngle by remember { mutableStateOf(0.5f) }
    var autoSpin by remember { mutableStateOf(true) }
    var selectedSector by remember { mutableStateOf(SurveySector.EQUATORIAL_BASIN) }
    var probeDeploying by remember { mutableStateOf(false) }
    var probeResultText by remember { mutableStateOf<String?>(null) }
    var scanProgress by remember { mutableStateOf(0.65f) }

    // Auto-spin animation
    LaunchedEffect(autoSpin) {
        while (autoSpin) {
            yawAngle += 0.015f
            delay(16L)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF03070D))
            .border(1.dp, BBC_CYAN.copy(alpha = 0.5f))
            .testTag("resource_exploration_view")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC001428))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "3D RESOURCE & MINERAL PROSPECTOR",
                        color = BBC_CYAN,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${system.name} SYSTEM  |  TECH LVL ${system.techLevel}  |  ${system.economyName.uppercase()}",
                        color = BBC_YELLOW,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { autoSpin = !autoSpin },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (autoSpin) Color(0xFF0F3B4C) else Color(0xFF1B242C)
                        ),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(if (autoSpin) "SPIN ON" else "PAUSED", color = BBC_WHITE, fontSize = 7.sp)
                    }

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF331111)),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.height(26.dp).testTag("close_resource_view_btn")
                    ) {
                        Text("CLOSE", color = BBC_RED, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Main Content Area: 3D Wireframe Globe on Left, Spectrometer on Right
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Left Column: Interactive 3D Wireframe Globe with Ore Hotspots
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxSize()
                        .background(Color.Black)
                        .border(1.dp, BBC_CYAN.copy(alpha = 0.3f))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                autoSpin = false
                                yawAngle += dragAmount.x * 0.015f
                                pitchAngle = (pitchAngle - dragAmount.y * 0.015f).coerceIn(-1.3f, 1.3f)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val radius = (size.width.coerceAtMost(size.height) * 0.36f)

                        val rotMat = Matrix3x3.rotationXYZ(pitchAngle, yawAngle, 0f)

                        // Draw Wireframe Latitude Parallels
                        val latSteps = 7
                        for (i in -latSteps..latSteps) {
                            val phi = (i.toFloat() / latSteps) * (Math.PI / 2.0).toFloat() * 0.9f
                            val rRing = radius * cos(phi)
                            val yRing = radius * sin(phi)

                            var prevPt: Offset? = null
                            val segs = 28
                            for (j in 0..segs) {
                                val theta = (j.toFloat() / segs) * (2.0 * Math.PI).toFloat()
                                val local = Vector3(rRing * cos(theta), yRing, rRing * sin(theta))
                                val rot = rotMat.transform(local)

                                if (rot.z > -radius * 0.1f) {
                                    val pt = Offset(cx + rot.x, cy - rot.y)
                                    if (prevPt != null) {
                                        val alpha = ((rot.z + radius) / (2f * radius)).coerceIn(0.2f, 0.9f)
                                        drawLine(
                                            color = BBC_CYAN.copy(alpha = alpha),
                                            start = prevPt,
                                            end = pt,
                                            strokeWidth = 1.0f
                                        )
                                    }
                                    prevPt = pt
                                } else {
                                    prevPt = null
                                }
                            }
                        }

                        // Draw Wireframe Longitudinal Meridians
                        val merSteps = 8
                        for (i in 0 until merSteps) {
                            val theta = (i.toFloat() / merSteps) * Math.PI.toFloat()
                            var prevPt: Offset? = null
                            val segs = 28
                            for (j in 0..segs) {
                                val phi = (j.toFloat() / segs) * (2.0 * Math.PI).toFloat()
                                val local = Vector3(radius * cos(phi) * cos(theta), radius * sin(phi), radius * cos(phi) * sin(theta))
                                val rot = rotMat.transform(local)

                                if (rot.z > -radius * 0.1f) {
                                    val pt = Offset(cx + rot.x, cy - rot.y)
                                    if (prevPt != null) {
                                        val alpha = ((rot.z + radius) / (2f * radius)).coerceIn(0.2f, 0.9f)
                                        drawLine(
                                            color = BBC_GREEN.copy(alpha = alpha),
                                            start = prevPt,
                                            end = pt,
                                            strokeWidth = 1.0f
                                        )
                                    }
                                    prevPt = pt
                                } else {
                                    prevPt = null
                                }
                            }
                        }

                        // Draw Planetary Mineral Ring
                        val ringInner = radius * 1.35f
                        val ringOuter = radius * 1.75f
                        val ringSegs = 36
                        for (r in listOf(ringInner, (ringInner + ringOuter) / 2f, ringOuter)) {
                            var prevPt: Offset? = null
                            for (j in 0..ringSegs) {
                                val theta = (j.toFloat() / ringSegs) * (2.0 * Math.PI).toFloat()
                                val local = Vector3(r * cos(theta), 0f, r * sin(theta))
                                val rot = rotMat.transform(local)
                                val pt = Offset(cx + rot.x, cy - rot.y)
                                if (prevPt != null) {
                                    val alpha = ((rot.z + ringOuter) / (2f * ringOuter)).coerceIn(0.25f, 0.9f)
                                    drawLine(
                                        color = BBC_YELLOW.copy(alpha = alpha),
                                        start = prevPt,
                                        end = pt,
                                        strokeWidth = 1.2f
                                    )
                                }
                                prevPt = pt
                            }
                        }

                        // Draw Active Hotspot Target
                        val hotspotAngle = when (selectedSector) {
                            SurveySector.EQUATORIAL_BASIN -> 0f
                            SurveySector.VOLCANIC_RIFT -> 1.57f
                            SurveySector.POLAR_ICE_CAP -> 3.14f
                            SurveySector.ASTEROID_RING -> 4.71f
                        }
                        val hLocal = Vector3(radius * 0.9f * cos(hotspotAngle), 0f, radius * 0.9f * sin(hotspotAngle))
                        val hRot = rotMat.transform(hLocal)
                        if (hRot.z > -radius * 0.4f) {
                            val hPt = Offset(cx + hRot.x, cy - hRot.y)
                            drawCircle(color = BBC_RED, radius = 7f, center = hPt, style = Stroke(1.8f))
                            drawCircle(color = BBC_YELLOW, radius = 2.5f, center = hPt)
                        }
                    }

                    Text(
                        text = "TOUCH TO ROTATE PLANET",
                        color = Color.Gray,
                        fontSize = 7.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                    )
                }

                // Right Column: Spectrometer & Sector Analysis
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxSize()
                        .background(Color(0xD9060B12))
                        .border(1.dp, BBC_YELLOW.copy(alpha = 0.4f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SURVEY SECTOR SELECT:",
                        color = BBC_YELLOW,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Sector Grid Selection
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (sec in SurveySector.values()) {
                            val isSel = selectedSector == sec
                            Button(
                                onClick = {
                                    selectedSector = sec
                                    soundSynth.playBeep(true)
                                    scanProgress = 0.5f + Random.nextFloat() * 0.45f
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) Color(0xFF003855) else Color(0xFF131920)
                                ),
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.fillMaxWidth().height(26.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sec.title.take(16),
                                        color = if (isSel) BBC_CYAN else Color.LightGray,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${(sec.purity * 100).toInt()}%",
                                        color = if (isSel) BBC_YELLOW else Color.Gray,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Spectrographic Analysis
                    Text(
                        text = "CORE COMPOSITION SPECTROMETER:",
                        color = BBC_CYAN,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        CompositionRow("RICH DEPOSIT", selectedSector.richDeposit, BBC_YELLOW, selectedSector.purity)
                        CompositionRow("HEAVY ALLOYS", "42.5% ORE CRUST", BBC_WHITE, 0.68f)
                        CompositionRow("RARE MINERALS", "28.0% BASIN VEIN", BBC_CYAN, 0.55f)
                        CompositionRow("CRYSTAL GEMS", "14.2% VOLCANIC", BBC_GREEN, 0.40f)
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Probe Extraction Log or Status
                    if (probeResultText != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xDD002200))
                                .border(1.dp, BBC_GREEN)
                                .padding(4.dp)
                        ) {
                            Text(
                                text = probeResultText!!,
                                color = BBC_GREEN,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Bottom Action Bar: Launch Probe, Drop to Asteroid Belt, Solar Skim
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xD9000C18))
                    .border(1.dp, BBC_CYAN.copy(alpha = 0.3f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Launch Prospecting Probe
                Button(
                    onClick = {
                        soundSynth.playMissileLaunch()
                        probeDeploying = true
                        val commodityId = selectedSector.mineralId
                        val unit = if (commodityId in listOf(13, 14)) "kg" else if (commodityId == 15) "g" else "t"
                        val qty = if (commodityId == 15) 500 else if (commodityId in listOf(13, 14)) 10 else 2
                        commander.cargo[commodityId] = (commander.cargo[commodityId] ?: 0) + qty
                        val depositName = selectedSector.richDeposit
                        probeResultText = "PROBE SUCCESS: EXTRACTED $qty$unit $depositName TO CARGO!"
                        soundSynth.playBeep(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005518)),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier.weight(1f).height(34.dp).testTag("launch_probe_btn")
                ) {
                    Text(
                        text = "DEPLOY PROBE",
                        color = BBC_WHITE,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 2. Drop into real 3D Asteroid Belt for Manual Mining
                Button(
                    onClick = {
                        soundSynth.playBeep(true)
                        flightEngine.spawnAsteroidField(12)
                        onLaunchAsteroidBelt()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6E4000)),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier.weight(1.2f).height(34.dp).testTag("enter_asteroid_belt_btn")
                ) {
                    Text(
                        text = "DROP TO ASTEROID BELT",
                        color = BBC_YELLOW,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun CompositionRow(label: String, value: String, valueColor: Color, progress: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = valueColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = valueColor,
            trackColor = Color(0xFF142028),
        )
    }
}
