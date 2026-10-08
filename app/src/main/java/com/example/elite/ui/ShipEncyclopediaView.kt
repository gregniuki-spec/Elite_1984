package com.example.elite.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elite.audio.BbcSoundSynth
import com.example.elite.math3d.ColorTheme
import com.example.elite.math3d.Matrix3x3
import com.example.elite.math3d.RenderStyle
import com.example.elite.math3d.Vector3
import com.example.elite.math3d.WireframeProjectionEngine
import com.example.elite.ships.ShipBlueprints
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

enum class CraftCategory(val label: String) {
    ALL("ALL CRAFT"),
    STATIONS("SPACE STATIONS"),
    ENEMY_SHIPS("COMBAT & PIRATES"),
    CELESTIAL("CELESTIAL & MISC")
}

/**
 * 3D Wireframe Projection Engine & Hologram Studio.
 * Renders the iconic rotating space stations (Coriolis, Dodecahedron) and enemy
 * combat craft (Sidewinder, Viper, Mamba, Krait, Gecko, Asp Mk II, Fer-de-Lance,
 * Thargoid Mothership, etc.) faithfully using Compose Canvas.
 *
 * Features:
 * - Real-time 3D rotation physics with adjustable spin rates along X, Y, and Z axes
 * - 1984 Elite authentic Backface Culling with face normals
 * - Arcade CRT vector glow and phosphor bloom rendering modes
 * - Flashing navigation corridor lights on rotating station docking apertures
 * - Complete technical specifications, blueprints, and lore
 */
@Composable
fun ShipEncyclopediaView(
    soundSynth: BbcSoundSynth,
    modifier: Modifier = Modifier
) {
    val catalog = ShipBlueprints.SHIP_CATALOG
    var selectedCategory by remember { mutableStateOf(CraftCategory.ALL) }
    var selectedLore by remember { mutableStateOf(catalog[0]) }

    // 3D Engine Euler Angles
    var rotX by remember { mutableFloatStateOf(0.25f) }
    var rotY by remember { mutableFloatStateOf(0.45f) }
    var rotZ by remember { mutableFloatStateOf(0.0f) }

    // Spin dynamics
    var isAutoRotate by remember { mutableStateOf(true) }
    var spinSpeedMultiplier by remember { mutableFloatStateOf(1.0f) }
    var spinAxisX by remember { mutableStateOf(false) }
    var spinAxisY by remember { mutableStateOf(true) }
    var spinAxisZ by remember { mutableStateOf(false) }

    // Render parameters
    var renderStyle by remember { mutableStateOf(RenderStyle.AUTHENTIC_BACKFACE_CULLED) }
    var colorTheme by remember { mutableStateOf(ColorTheme.BBC_WHITE) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var showTelemetry by remember { mutableStateOf(true) }
    var enableBeaconLights by remember { mutableStateOf(true) }

    // Auto-rotation engine loop (~60 FPS)
    LaunchedEffect(isAutoRotate, spinSpeedMultiplier, spinAxisX, spinAxisY, spinAxisZ, selectedLore) {
        while (isActive && isAutoRotate) {
            val isStation = selectedLore.blueprint.name.contains("Station")
            if (isStation && !spinAxisX && !spinAxisZ) {
                // Stations rotate iconically along their docking axis (Z) and slightly yaw
                rotZ += 0.022f * spinSpeedMultiplier
                rotY += 0.004f * spinSpeedMultiplier
            } else {
                if (spinAxisY) rotY += 0.018f * spinSpeedMultiplier
                if (spinAxisX) rotX += 0.008f * spinSpeedMultiplier
                if (spinAxisZ) rotZ += 0.015f * spinSpeedMultiplier
            }
            delay(16L)
        }
    }

    // Filter catalog based on selected category
    val filteredCatalog = remember(selectedCategory) {
        when (selectedCategory) {
            CraftCategory.ALL -> catalog
            CraftCategory.STATIONS -> catalog.filter { it.blueprint.name.contains("Station") }
            CraftCategory.ENEMY_SHIPS -> catalog.filter {
                it.blueprint.name in listOf(
                    "Sidewinder", "Mamba", "Krait", "Gecko", "Viper", "Asp Mk II",
                    "Fer-de-Lance", "Moray Star Boat", "Python", "Anaconda",
                    "Thargoid", "Thargon", "Cobra Mk III"
                )
            }
            CraftCategory.CELESTIAL -> catalog.filter {
                it.blueprint.name in listOf("Planet", "Sun", "Asteroid", "Cargo Canister", "Escape Pod")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020609))
            .padding(6.dp)
            .testTag("ship_encyclopedia_view")
    ) {
        // 1. Top Header & Primary Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "3D WIREFRAME PROJECTION ENGINE",
                    color = BBC_YELLOW,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "BBC MICRO 6502 VECTOR GRAPHICS",
                    color = BBC_CYAN,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = {
                        isAutoRotate = !isAutoRotate
                        soundSynth.playBeep(true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAutoRotate) Color(0xFF006622) else Color(0xFF333333)
                    ),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.height(24.dp).testTag("autorotate_toggle")
                ) {
                    Text(
                        if (isAutoRotate) "SPIN: ON" else "SPIN: OFF",
                        color = BBC_WHITE,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = {
                        rotX = 0.25f
                        rotY = 0.45f
                        rotZ = 0.0f
                        zoomScale = 1.0f
                        soundSynth.playBeep(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3345)),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.height(24.dp).testTag("reset_view_button")
                ) {
                    Text("RESET", color = BBC_CYAN, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = {
                        // Align camera directly to front docking aperture!
                        rotX = 0.0f
                        rotY = 0.0f
                        rotZ = 0.0f
                        zoomScale = 1.15f
                        soundSynth.playBeep(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF553311)),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.height(24.dp).testTag("docking_align_button")
                ) {
                    Text("ALIGN DOCK", color = BBC_YELLOW, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Category Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CraftCategory.entries.forEach { cat ->
                val isSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isSelected) Color(0xFF881111) else Color(0xFF111E28), RoundedCornerShape(2.dp))
                        .border(1.dp, if (isSelected) BBC_YELLOW else Color(0xFF223E55), RoundedCornerShape(2.dp))
                        .clickable {
                            selectedCategory = cat
                            if (filteredCatalog.isNotEmpty() && selectedLore !in filteredCatalog) {
                                selectedLore = filteredCatalog.first()
                            }
                            soundSynth.playBeep(true)
                        }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat.label,
                        color = if (isSelected) BBC_WHITE else Color(0xFF99BBCC),
                        fontSize = 8.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Craft Selector Horizontal Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filteredCatalog) { lore ->
                val isSelected = lore == selectedLore
                val isStation = lore.blueprint.name.contains("Station")
                val isHostile = lore.blueprint.bounty > 0

                val bgCol = when {
                    isSelected -> Color(0xFFCC1111)
                    isStation -> Color(0xFF2B2605)
                    isHostile -> Color(0xFF280B0B)
                    else -> Color(0xFF0F1E29)
                }

                Button(
                    onClick = {
                        selectedLore = lore
                        soundSynth.playBeep(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = bgCol),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .height(26.dp)
                        .testTag("catalog_item_${lore.blueprint.name.lowercase().replace(' ', '_')}")
                ) {
                    Text(
                        text = lore.blueprint.name.uppercase(),
                        color = if (isSelected) BBC_WHITE else if (isStation) BBC_YELLOW else Color(0xFFAABBCC),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 4. Interactive 3D Wireframe Canvas Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.15f)
                .background(Color(0xFF000508), RoundedCornerShape(4.dp))
                .border(1.dp, BBC_GREEN.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isAutoRotate = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            rotY += dragAmount.x * 0.012f
                            rotX -= dragAmount.y * 0.012f
                        }
                    )
                }
                .testTag("hologram_canvas_box")
        ) {
            // Render 3D Model with Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val center = Offset(width / 2f, height / 2f)

                // Subtle coordinate grid rings
                drawCircle(
                    color = BBC_GREEN.copy(alpha = 0.08f),
                    radius = (height * 0.44f).coerceAtLeast(30f),
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1f)
                )
                drawCircle(
                    color = BBC_GREEN.copy(alpha = 0.04f),
                    radius = (height * 0.28f).coerceAtLeast(20f),
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1f)
                )

                // Render model using the 3D Wireframe Projection Engine
                val bp = selectedLore.blueprint
                val isStation = bp.name.contains("Station")
                val wireColor = when (colorTheme) {
                    ColorTheme.BBC_WHITE -> Color(0xFFFFFFFF)
                    ColorTheme.BBC_MODE1 -> when {
                        isStation -> BBC_YELLOW
                        bp.bounty > 0 -> Color(0xFFFF2244)
                        bp.name == "Planet" -> BBC_CYAN
                        bp.name == "Sun" -> Color(0xFFFF9900)
                        else -> BBC_GREEN
                    }
                    ColorTheme.AMBER_CRT -> colorTheme.primary
                    ColorTheme.PHOSPHOR_GREEN -> colorTheme.primary
                    ColorTheme.CYBER_NEON -> if (isStation) Color(0xFFFFEE00) else Color(0xFF00F0FF)
                }

                WireframeProjectionEngine.renderModel(
                    drawScope = this,
                    blueprint = bp,
                    worldPos = Vector3(0f, 0f, 280f),
                    rotationXYZ = Vector3(rotX, rotY, rotZ),
                    camMatrix = Matrix3x3.IDENTITY,
                    center = center,
                    viewScale = zoomScale,
                    focalLength = 360f,
                    renderStyle = renderStyle,
                    colorTheme = colorTheme,
                    overrideColor = wireColor,
                    strokeWidth = 1.9f,
                    enableDepthFade = false,
                    enableBeaconLights = enableBeaconLights
                )
            }

            // Top Telemetry Overlay (Vertex count, Edges, Faces, Live Rotation)
            if (showTelemetry) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(Color(0xCC000508), RoundedCornerShape(2.dp))
                        .padding(4.dp)
                ) {
                    val bp = selectedLore.blueprint
                    Text(
                        text = "MODEL: ${bp.name.uppercase()}",
                        color = BBC_YELLOW,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "VERTICES: ${bp.vertices.size}  EDGES: ${bp.edges.size}  FACES: ${bp.faces.size}",
                        color = BBC_WHITE,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "PITCH: ${"%.1f".format(Math.toDegrees(rotX.toDouble()) % 360)}°  YAW: ${"%.1f".format(Math.toDegrees(rotY.toDouble()) % 360)}°  ROLL: ${"%.1f".format(Math.toDegrees(rotZ.toDouble()) % 360)}°",
                        color = BBC_CYAN,
                        fontSize = 7.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Bottom Drag Helper & Zoom Buttons
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOUCH & DRAG TO ORBIT",
                    color = BBC_GREEN.copy(alpha = 0.65f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Zoom Buttons
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { zoomScale = (zoomScale + 0.2f).coerceAtMost(2.6f) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2830)),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.size(26.dp)
                ) {
                    Text("+", color = BBC_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { zoomScale = (zoomScale - 0.2f).coerceAtLeast(0.4f) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2830)),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.size(26.dp)
                ) {
                    Text("-", color = BBC_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 5. Engine Controls Panel: Render Style, Color Theme, Spin Axis
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF09141E), RoundedCornerShape(3.dp))
                .border(1.dp, Color(0xFF1B3245), RoundedCornerShape(3.dp))
                .padding(6.dp)
        ) {
            // Render Style Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RENDER:",
                    color = Color(0xFFAABBCC),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    RenderStyle.entries.forEach { style ->
                        val isSelected = renderStyle == style
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) Color(0xFF005577) else Color(0xFF0E1A24), RoundedCornerShape(2.dp))
                                .border(1.dp, if (isSelected) BBC_CYAN else Color(0xFF1F3547), RoundedCornerShape(2.dp))
                                .clickable {
                                    renderStyle = style
                                    soundSynth.playBeep(true)
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = style.label.take(9),
                                color = if (isSelected) BBC_WHITE else Color(0xFF88AABB),
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Palette Theme & Spin Axis
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color Theme Selector
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PALETTE:",
                        color = Color(0xFFAABBCC),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    ColorTheme.entries.forEach { theme ->
                        val isSelected = colorTheme == theme
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(theme.primary, RoundedCornerShape(2.dp))
                                .border(1.dp, if (isSelected) BBC_WHITE else Color.Black, RoundedCornerShape(2.dp))
                                .clickable {
                                    colorTheme = theme
                                    soundSynth.playBeep(true)
                                }
                        )
                    }
                }

                // Spin Axis Toggles
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "AXIS:", color = Color(0xFFAABBCC), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    AxisToggle("X", spinAxisX) { spinAxisX = !spinAxisX }
                    AxisToggle("Y", spinAxisY) { spinAxisY = !spinAxisY }
                    AxisToggle("Z", spinAxisZ) { spinAxisZ = !spinAxisZ }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 6. Technical Specifications & Lore Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.75f)
                .background(Color(0xFF061118), RoundedCornerShape(4.dp))
                .border(1.dp, BBC_YELLOW.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .padding(6.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedLore.blueprint.name.uppercase()} BLUEPRINT SPECS",
                    color = BBC_YELLOW,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "TECH LEVEL: ${if (selectedLore.blueprint.name.contains("Dodecahedron")) "10+ (CORPORATE)" else "7+ (STANDARD)"}",
                    color = BBC_CYAN,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecItem("MAX SPEED", selectedLore.maxSpeedMps)
                SpecItem("DIMENSIONS", selectedLore.dimensions)
                SpecItem("CARGO CAP", selectedLore.cargoCapacity)
                SpecItem("BOUNTY", if (selectedLore.blueprint.bounty > 0) "${selectedLore.blueprint.bounty} CR" else "NONE")
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "MANUFACTURER: ${selectedLore.manufacturer}",
                color = BBC_WHITE,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "TACTICAL BRIEFING: ${selectedLore.description}",
                color = Color(0xFFAABBCC),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 11.sp
            )
        }
    }
}

@Composable
private fun AxisToggle(axis: String, active: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (active) Color(0xFF006622) else Color(0xFF1E2830), RoundedCornerShape(2.dp))
            .border(1.dp, if (active) BBC_GREEN else Color(0xFF334455), RoundedCornerShape(2.dp))
            .clickable { onToggle() }
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = axis,
            color = if (active) BBC_WHITE else Color(0xFF8899AA),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SpecItem(label: String, value: String) {
    Column {
        Text(text = label, color = Color(0xFF7799AA), fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = BBC_WHITE, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
