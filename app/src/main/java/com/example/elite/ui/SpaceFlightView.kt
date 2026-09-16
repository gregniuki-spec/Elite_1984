package com.example.elite.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elite.flight.CockpitView
import com.example.elite.flight.FlightEngine
import com.example.elite.math3d.WireframeRenderer

/**
 * Authentic 3D Space Flight Viewport.
 * Renders 3D wireframe ships, planets, stars, debris, tap-targeting HUD, and 4-way cockpit views.
 */
@Composable
fun SpaceFlightView(
    flightEngine: FlightEngine,
    modifier: Modifier = Modifier
) {
    var stickOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000508))
            .testTag("space_flight_viewport")
    ) {
        val camMatrix = flightEngine.getCameraMatrix()

        // 3D Canvas Rendering (Wireframe ships, Celestial bodies, Stars, Lasers, Debris)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        flightEngine.selectTargetAt(offset.x, offset.y, size.width.toFloat(), size.height.toFloat())
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val cx = width / 2f
            val cy = height / 2f

            // 1. Draw 3D Stardust particles (STARS) transformed by camera view
            for (star in flightEngine.stars) {
                val worldStar = com.example.elite.math3d.Vector3(star.x, star.y, star.z)
                val camStar = camMatrix.transform(worldStar)
                if (camStar.z > 15f) {
                    val scale = 360f / camStar.z
                    val sx = cx + camStar.x * scale
                    val sy = cy + camStar.y * scale
                    if (sx in 0f..width && sy in 0f..height) {
                        val brightness = ((1000f - camStar.z) / 1000f).coerceIn(0.2f, 1f)
                        val starColor = Color(brightness, brightness, brightness)
                        val radius = if (camStar.z < 250f) 2.2f else 1.2f
                        drawCircle(color = starColor, radius = radius, center = Offset(sx, sy))
                    }
                }
            }

            // 2. Draw 3D Wireframe Ships, Planet, Sun & Coriolis Station
            for (entity in flightEngine.entities) {
                val color = when {
                    entity.isHostile -> BBC_RED
                    entity.blueprint.name.contains("Station") -> BBC_YELLOW
                    entity.blueprint.name.contains("Planet") -> BBC_CYAN
                    entity.blueprint.name.contains("Sun") -> Color(0xFFFF9900)
                    entity.blueprint.name.contains("Canister") -> BBC_WHITE
                    else -> BBC_WHITE
                }

                WireframeRenderer.renderShip(
                    drawScope = this,
                    entity = entity,
                    viewWidth = width,
                    viewHeight = height,
                    wireColor = color,
                    viewMatrix = camMatrix
                )
            }

            // 3. Draw 3D Explosion Debris Shards
            if (flightEngine.debrisShards.isNotEmpty()) {
                WireframeRenderer.renderDebris(
                    drawScope = this,
                    shards = flightEngine.debrisShards,
                    viewWidth = width,
                    viewHeight = height,
                    color = BBC_YELLOW,
                    viewMatrix = camMatrix
                )
            }

            // 4. Draw Active Laser Beams
            for (laser in flightEngine.activeLasers) {
                // Left beam
                drawLine(
                    color = BBC_RED,
                    start = Offset(cx - 120f, height),
                    end = Offset(cx, cy),
                    strokeWidth = 3f
                )
                // Right beam
                drawLine(
                    color = BBC_RED,
                    start = Offset(cx + 120f, height),
                    end = Offset(cx, cy),
                    strokeWidth = 3f
                )
            }

            // 5. Center Flight Crosshairs
            val crossSize = 14f
            val crossColor = BBC_YELLOW.copy(alpha = 0.85f)
            // Horizontal ticks
            drawLine(color = crossColor, start = Offset(cx - crossSize * 2f, cy), end = Offset(cx - crossSize * 0.5f, cy), strokeWidth = 1.5f)
            drawLine(color = crossColor, start = Offset(cx + crossSize * 0.5f, cy), end = Offset(cx + crossSize * 2f, cy), strokeWidth = 1.5f)
            // Vertical ticks
            drawLine(color = crossColor, start = Offset(cx, cy - crossSize * 2f), end = Offset(cx, cy - crossSize * 0.5f), strokeWidth = 1.5f)
            drawLine(color = crossColor, start = Offset(cx, cy + crossSize * 0.5f), end = Offset(cx, cy + crossSize * 2f), strokeWidth = 1.5f)

            // 6. Target lock reticle on targeted entity
            flightEngine.lockedTarget?.let { target ->
                val camPos = camMatrix.transform(target.position)
                if (camPos.z in 20f..3500f) {
                    val scale = 360f / camPos.z
                    val tx = cx + camPos.x * scale
                    val ty = cy - camPos.y * scale
                    if (tx in 0f..width && ty in 0f..height) {
                        val boxSize = (6000f / camPos.z).coerceIn(24f, 90f)
                        val reticleColor = if (target.isHostile) BBC_RED else BBC_GREEN
                        drawRect(
                            color = reticleColor,
                            topLeft = Offset(tx - boxSize / 2f, ty - boxSize / 2f),
                            size = Size(boxSize, boxSize),
                            style = Stroke(1.8f)
                        )
                        val halfBox = boxSize / 2f
                        val bracketLen = boxSize * 0.25f
                        // Top-left
                        drawLine(reticleColor, Offset(tx - halfBox, ty - halfBox), Offset(tx - halfBox + bracketLen, ty - halfBox), 2f)
                        drawLine(reticleColor, Offset(tx - halfBox, ty - halfBox), Offset(tx - halfBox, ty - halfBox + bracketLen), 2f)
                        // Top-right
                        drawLine(reticleColor, Offset(tx + halfBox, ty - halfBox), Offset(tx + halfBox - bracketLen, ty - halfBox), 2f)
                        drawLine(reticleColor, Offset(tx + halfBox, ty - halfBox), Offset(tx + halfBox, ty - halfBox + bracketLen), 2f)
                    }
                }
            }

            // 7. Atmospheric friction glow
            if (flightEngine.isAtmosphericFriction) {
                drawRect(
                    color = BBC_RED.copy(alpha = 0.15f),
                    size = size
                )
            }

            // 8. Energy Bomb Screen Flash
            if (flightEngine.energyBombFlashAlpha > 0f) {
                drawRect(
                    color = BBC_WHITE.copy(alpha = flightEngine.energyBombFlashAlpha),
                    size = size
                )
            }
        }

        // Top-Left: Interactive Locked Target Card
        flightEngine.lockedTarget?.let { target ->
            val dist = target.position.length().toInt()
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 8.dp)
                    .background(Color(0xCC051016), RoundedCornerShape(4.dp))
                    .border(1.dp, if (target.isHostile) BBC_RED else BBC_GREEN, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("target_hud_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = target.blueprint.name.uppercase(),
                            color = if (target.isHostile) BBC_RED else BBC_CYAN,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${dist}m",
                            color = BBC_YELLOW,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = if (target.isHostile) "HOSTILE [${target.blueprint.bounty} CR]" else if (target.blueprint.name == "Canister") "CARGO CANISTER" else "CIVILIAN",
                        color = if (target.isHostile) Color(0xFFFF6666) else BBC_WHITE,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Top-Right: 4-Way Cockpit View Buttons
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val views = listOf(
                CockpitView.FRONT to "F",
                CockpitView.REAR to "R",
                CockpitView.LEFT to "L",
                CockpitView.RIGHT to "RGT"
            )
            for ((view, label) in views) {
                val isSelected = flightEngine.currentView == view
                Button(
                    onClick = { flightEngine.setCockpitView(view) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFFCC1111) else Color(0xFF221111)
                    ),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .size(width = 34.dp, height = 26.dp)
                        .testTag("cockpit_view_${label.lowercase()}")
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) BBC_WHITE else Color(0xFFAAAAAA),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Center-Top Status Text Banner (BBC Micro Yellow on Black)
        if (System.currentTimeMillis() < flightEngine.statusMessageTimeMs) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = flightEngine.statusMessage,
                    color = BBC_YELLOW,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Active solar scooping banner
        if (flightEngine.isSolarScooping) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
                    .background(Color(0xDD3A1D00), RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "SOLAR PLASMA CORONA: RECHARGING FUEL",
                    color = Color(0xFFFF9900),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Hostile Incoming Missile Warning Banner (BBC Micro Manual Combat Protocol)
        if (flightEngine.incomingMissileCountdownMs > 0L) {
            val secLeft = ((flightEngine.incomingMissileCountdownMs - System.currentTimeMillis()) / 1000f).coerceAtLeast(0f)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 95.dp)
                    .background(Color(0xDDAA0000), RoundedCornerShape(4.dp))
                    .border(1.dp, BBC_YELLOW, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("incoming_missile_alert")
            ) {
                Text(
                    text = "MISSILE INCOMING (%.1fs) - DEPLOY ECM!".format(secLeft),
                    color = BBC_YELLOW,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // On-screen Flight & Combat Controls overlay
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Left: Virtual Flight Stick (Pitch / Roll)
            Box(
                modifier = Modifier
                    .size(105.dp)
                    .background(Color(0x55002211), CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val diff = offset - center
                                stickOffset = diff
                                flightEngine.rollRate = (diff.x / 50f).coerceIn(-1f, 1f)
                                flightEngine.pitchRate = -(diff.y / 50f).coerceIn(-1f, 1f)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                stickOffset += dragAmount
                                flightEngine.rollRate = (stickOffset.x / 50f).coerceIn(-1f, 1f)
                                flightEngine.pitchRate = -(stickOffset.y / 50f).coerceIn(-1f, 1f)
                            },
                            onDragEnd = {
                                stickOffset = Offset.Zero
                                flightEngine.rollRate = 0f
                                flightEngine.pitchRate = 0f
                            },
                            onDragCancel = {
                                stickOffset = Offset.Zero
                                flightEngine.rollRate = 0f
                                flightEngine.pitchRate = 0f
                            }
                        )
                    }
                    .testTag("flight_stick"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = BBC_GREEN.copy(alpha = 0.3f), style = Stroke(1.5f))
                    // Stick knob
                    drawCircle(
                        color = BBC_GREEN,
                        radius = 16f,
                        center = Offset(size.width / 2f + stickOffset.x.coerceIn(-38f, 38f), size.height / 2f + stickOffset.y.coerceIn(-38f, 38f))
                    )
                }
                Text(
                    text = "PITCH/ROLL",
                    color = BBC_GREEN.copy(alpha = 0.6f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                )
            }

            // Center: Speed controls, Autopilot & Nav Compass Selector
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            flightEngine.speed = (flightEngine.speed + 4f).coerceAtMost(flightEngine.maxSpeed)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B382B)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.size(width = 54.dp, height = 34.dp).testTag("speed_up_button")
                    ) {
                        Text("ACC", color = BBC_GREEN, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            flightEngine.speed = (flightEngine.speed - 4f).coerceAtLeast(0f)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B382B)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.size(width = 54.dp, height = 34.dp).testTag("speed_down_button")
                    ) {
                        Text("DEC", color = BBC_YELLOW, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { flightEngine.engageDockingComputer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (flightEngine.isDockingComputerActive) Color(0xFF005522) else Color(0xFF1E2830)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.size(width = 72.dp, height = 30.dp).testTag("docking_comp_button")
                    ) {
                        Text(
                            if (flightEngine.isDockingComputerActive) "DOCK ON" else "DOCK",
                            color = BBC_CYAN,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { flightEngine.cycleNavTarget() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF182A3A)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.size(width = 64.dp, height = 30.dp).testTag("nav_target_button")
                    ) {
                        Text(
                            "NAV:${flightEngine.navTarget.name.take(3)}",
                            color = when (flightEngine.navTarget) {
                                com.example.elite.flight.NavTarget.STATION -> BBC_YELLOW
                                com.example.elite.flight.NavTarget.PLANET -> BBC_CYAN
                                com.example.elite.flight.NavTarget.SUN -> Color(0xFFFF9900)
                            },
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Right: Combat Weapons (Laser, Missile, ECM, Energy Bomb)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (flightEngine.commander.hasEnergyBomb) {
                        Button(
                            onClick = { flightEngine.triggerEnergyBomb() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF880000)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.size(width = 46.dp, height = 38.dp).testTag("energy_bomb_button")
                        ) {
                            Text("BOMB", color = BBC_YELLOW, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { flightEngine.fireLaser() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF550000)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.size(width = if (flightEngine.commander.hasEnergyBomb) 54.dp else 90.dp, height = 38.dp).testTag("fire_laser_button")
                    ) {
                        Text("LASER", color = BBC_WHITE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            if (!flightEngine.isMissileArmed) {
                                flightEngine.armMissile()
                            } else {
                                flightEngine.fireMissile()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (flightEngine.isMissileArmed) Color(0xFFAA2200) else Color(0xFF4A3800)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.size(width = 46.dp, height = 32.dp).testTag("fire_missile_button")
                    ) {
                        Text(
                            if (flightEngine.isMissileArmed) "FIRE" else "MSL",
                            color = if (flightEngine.isMissileArmed) BBC_WHITE else BBC_YELLOW,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { flightEngine.triggerEcm() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3344)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.size(width = 46.dp, height = 32.dp).testTag("trigger_ecm_button")
                    ) {
                        Text("ECM", color = BBC_CYAN, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

