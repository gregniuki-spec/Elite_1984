package com.example.elite.ui

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.elite.math3d.WireframeRenderer
import com.example.elite.ships.ShipBlueprints
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 3D Hologram Wireframe Encyclopedia.
 * Allows interactive 3D rotation, inspection, and technical specifications
 * of all ships, planets, stars, and celestial bodies in the Elite universe.
 */
@Composable
fun ShipEncyclopediaView(
    soundSynth: BbcSoundSynth,
    modifier: Modifier = Modifier
) {
    val catalog = ShipBlueprints.SHIP_CATALOG
    var selectedLore by remember { mutableStateOf(catalog[0]) }
    var rotX by remember { mutableFloatStateOf(0.35f) }
    var rotY by remember { mutableFloatStateOf(0.6f) }
    var rotZ by remember { mutableFloatStateOf(0.0f) }
    var isAutoRotate by remember { mutableStateOf(true) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    // Auto-rotation coroutine
    LaunchedEffect(isAutoRotate) {
        while (isActive && isAutoRotate) {
            rotY += 0.015f
            rotX += 0.005f
            delay(16L)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF03070A))
            .padding(8.dp)
            .testTag("ship_encyclopedia_view")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GALACTIC 3D HOLOGRAM VIEWER",
                color = BBC_YELLOW,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { isAutoRotate = !isAutoRotate },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAutoRotate) Color(0xFF005522) else Color(0xFF333333)
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
                        rotX = 0.35f
                        rotY = 0.6f
                        rotZ = 0.0f
                        zoomScale = 1.0f
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF223344)),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.height(24.dp).testTag("reset_view_button")
                ) {
                    Text(
                        "RESET",
                        color = BBC_CYAN,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Catalog Selector Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(catalog) { lore ->
                val isSelected = lore == selectedLore
                Button(
                    onClick = {
                        selectedLore = lore
                        soundSynth.playBeep(true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFF881111) else Color(0xFF16202A)
                    ),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("catalog_item_${lore.blueprint.name.lowercase().replace(' ', '_')}")
                ) {
                    Text(
                        text = lore.blueprint.name.uppercase(),
                        color = if (isSelected) BBC_WHITE else Color(0xFFAABBDD),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Split view: 3D Hologram Wireframe Canvas on top, Spec Sheet on bottom
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Interactive 3D Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f)
                    .background(Color(0xFF000508), RoundedCornerShape(4.dp))
                    .border(1.dp, BBC_GREEN.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isAutoRotate = false },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                rotY += dragAmount.x * 0.01f
                                rotX -= dragAmount.y * 0.01f
                            }
                        )
                    }
                    .testTag("hologram_canvas_box")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val cx = width / 2f
                    val cy = height / 2f

                    // Subtle coordinate grid ring
                    drawCircle(
                        color = BBC_GREEN.copy(alpha = 0.12f),
                        radius = (height * 0.42f).coerceAtLeast(30f),
                        center = Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1f)
                    )

                    // Render wireframe blueprint
                    WireframeRenderer.renderBlueprintAt(
                        drawScope = this,
                        blueprint = selectedLore.blueprint,
                        rotX = rotX,
                        rotY = rotY,
                        rotZ = rotZ,
                        center = Offset(cx, cy),
                        viewScale = zoomScale,
                        wireColor = when {
                            selectedLore.blueprint.name == "Sun" -> Color(0xFFFF9900)
                            selectedLore.blueprint.name == "Planet" -> BBC_CYAN
                            selectedLore.blueprint.bounty > 0 -> BBC_RED
                            selectedLore.blueprint.name == "Coriolis Station" -> BBC_YELLOW
                            else -> BBC_GREEN
                        }
                    )
                }

                // Interactive touch instruction badge
                Text(
                    text = "DRAG TO ROTATE 3D MODEL",
                    color = BBC_GREEN.copy(alpha = 0.5f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                )

                // Zoom controls
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { zoomScale = (zoomScale + 0.2f).coerceAtMost(2.5f) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2830)),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+", color = BBC_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { zoomScale = (zoomScale - 0.2f).coerceAtLeast(0.4f) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2830)),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("-", color = BBC_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Technical Specifications & Lore Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
                    .background(Color(0xFF061118), RoundedCornerShape(4.dp))
                    .border(1.dp, BBC_YELLOW.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "${selectedLore.blueprint.name.uppercase()} SPECIFICATIONS",
                    color = BBC_YELLOW,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SpecItem("MAX SPEED", selectedLore.maxSpeedMps)
                    SpecItem("DIMENSIONS", selectedLore.dimensions)
                    SpecItem("CARGO CAP", selectedLore.cargoCapacity)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SpecItem("HYPERDRIVE", if (selectedLore.hyperdriveCapable) "YES (CLASS 1)" else "NO")
                    SpecItem("BOUNTY REWARD", if (selectedLore.blueprint.bounty > 0) "${selectedLore.blueprint.bounty}.0 CR" else "CLEAN / NONE")
                    SpecItem("WEAPONS", selectedLore.weaponMounts)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "MANUFACTURER / ORIGIN:",
                    color = BBC_CYAN,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = selectedLore.manufacturer,
                    color = BBC_WHITE,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "PILOT'S FEDERATION BRIEFING:",
                    color = BBC_CYAN,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = selectedLore.description,
                    color = BBC_WHITE,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
private fun SpecItem(label: String, value: String) {
    Column {
        Text(text = label, color = Color(0xFF88AABB), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = BBC_WHITE, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

