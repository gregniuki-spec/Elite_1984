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
import com.example.elite.flight.CombatScenario
import com.example.elite.flight.FlightEngine
import com.example.elite.math3d.Matrix3x3
import com.example.elite.math3d.Vector3
import com.example.elite.math3d.WireframeRenderer
import com.example.elite.model.CommanderState
import com.example.elite.ships.ShipBlueprints

@Composable
fun BattleArenaView(
    flightEngine: FlightEngine,
    commander: CommanderState,
    soundSynth: BbcSoundSynth,
    onReturnToFlight: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedScenario by remember { mutableStateOf(CombatScenario.PIRATE_AMBUSH) }
    var waveCount by remember { mutableStateOf(1) }

    // Count hostiles remaining
    val hostiles = flightEngine.entities.filter { it.isHostile && !it.isDestroyed }
    val lockedTarget = flightEngine.lockedTarget

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("battle_arena_view")
    ) {
        // 1. 3D Combat Wireframe Viewport
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            flightEngine.pitchRate = 0f
                            flightEngine.rollRate = 0f
                        },
                        onDragCancel = {
                            flightEngine.pitchRate = 0f
                            flightEngine.rollRate = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        flightEngine.rollRate = (dragAmount.x * 0.04f).coerceIn(-1.8f, 1.8f)
                        flightEngine.pitchRate = (dragAmount.y * 0.04f).coerceIn(-1.8f, 1.8f)
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height / 2f)
            val camMat = flightEngine.getCameraMatrix()

            // Starfield
            for (star in flightEngine.stars) {
                if (star.z > 10f) {
                    val proj = 280f / star.z
                    val sx = center.x + star.x * proj
                    val sy = center.y + star.y * proj
                    if (sx in 0f..width && sy in 0f..height) {
                        val brightness = ((1f - (star.z / 1500f)) * 255).toInt().coerceIn(60, 255)
                        drawCircle(
                            color = Color(brightness, brightness, brightness),
                            radius = if (star.z < 400f) 1.5f else 1.0f,
                            center = Offset(sx, sy)
                        )
                    }
                }
            }

            // 3D Wireframe Ships & Entities
            val sorted = flightEngine.entities.filter { it.position.z > 20f && !it.isDestroyed }
                .sortedByDescending { it.position.z }

            for (entity in sorted) {
                val proj = 280f / entity.position.z
                val sx = center.x + (entity.position.x * proj)
                val sy = center.y - (entity.position.y * proj)
                val wireColor = when {
                    entity.isHostile -> BBC_RED
                    entity.blueprint == ShipBlueprints.ASTEROID -> Color(0xFFAAAAAA)
                    entity.blueprint == ShipBlueprints.CANISTER -> BBC_YELLOW
                    else -> BBC_WHITE
                }

                WireframeRenderer.renderBlueprintAt(
                    drawScope = this,
                    blueprint = entity.blueprint,
                    rotX = entity.rotation.x,
                    rotY = entity.rotation.y,
                    rotZ = entity.rotation.z,
                    center = Offset(sx, sy),
                    viewScale = proj * 0.65f,
                    wireColor = wireColor,
                    strokeWidth = if (entity == lockedTarget) 2.2f else 1.3f
                )

                // Tactical Reticle over locked hostile target
                if (entity == lockedTarget && sx in 0f..width && sy in 0f..height) {
                    val boxSize = (4500f / entity.position.z).coerceIn(24f, 85f)
                    drawRect(
                        color = BBC_YELLOW,
                        topLeft = Offset(sx - boxSize / 2f, sy - boxSize / 2f),
                        size = androidx.compose.ui.geometry.Size(boxSize, boxSize),
                        style = Stroke(1.5f)
                    )
                    // Lead indicator dot
                    drawCircle(
                        color = BBC_RED,
                        radius = 3.5f,
                        center = Offset(sx, sy)
                    )
                }
            }

            // Active 3D Laser Beams
            for (laser in flightEngine.activeLasers) {
                if (System.currentTimeMillis() < laser.lifetimeMs) {
                    val pLeft = center + Offset(-35f, 25f)
                    val pRight = center + Offset(35f, 25f)
                    drawLine(color = BBC_RED, start = pLeft, end = center, strokeWidth = 2.8f)
                    drawLine(color = BBC_RED, start = pRight, end = center, strokeWidth = 2.8f)
                }
            }

            // Explosion Shards
            if (flightEngine.debrisShards.isNotEmpty()) {
                WireframeRenderer.renderDebris(
                    drawScope = this,
                    shards = flightEngine.debrisShards,
                    viewWidth = size.width,
                    viewHeight = size.height,
                    color = BBC_YELLOW,
                    viewMatrix = camMat
                )
            }

            // Central Crosshair
            drawCircle(color = BBC_CYAN.copy(alpha = 0.5f), radius = 16f, center = center, style = Stroke(1.2f))
            drawLine(color = BBC_CYAN, start = Offset(center.x - 24f, center.y), end = Offset(center.x - 8f, center.y), strokeWidth = 1.4f)
            drawLine(color = BBC_CYAN, start = Offset(center.x + 8f, center.y), end = Offset(center.x + 24f, center.y), strokeWidth = 1.4f)
            drawLine(color = BBC_CYAN, start = Offset(center.x, center.y - 24f), end = Offset(center.x, center.y - 8f), strokeWidth = 1.4f)
            drawLine(color = BBC_CYAN, start = Offset(center.x, center.y + 8f), end = Offset(center.x, center.y + 24f), strokeWidth = 1.4f)
        }

        // 2. Top Header: Combat Scenario Selector & Telemetry
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color(0xD9060B12))
                .border(1.dp, BBC_RED.copy(alpha = 0.4f))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "3D BATTLE ARENA",
                            color = BBC_RED,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "[${commander.combatRank.displayName.uppercase()}]",
                            color = BBC_YELLOW,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "KILLS: ${commander.killCount}  |  HOSTILES: ${hostiles.size}  |  WAVE: $waveCount",
                        color = BBC_CYAN,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { flightEngine.toggleAutoPlay() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (flightEngine.isAutoPlayActive) Color(0xFF6B1B6B) else Color(0xFF222830)
                        ),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.height(28.dp).testTag("arena_auto_play_btn")
                    ) {
                        Text(
                            text = if (flightEngine.isAutoPlayActive) "AUTO: ${flightEngine.autoPlayMode.badge}" else "AUTO: OFF",
                            color = if (flightEngine.isAutoPlayActive) BBC_YELLOW else BBC_WHITE,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            waveCount++
                            flightEngine.spawnBattleWave(selectedScenario)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6E1010)),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.height(28.dp).testTag("spawn_wave_btn")
                    ) {
                        Text("SPAWN WAVE", color = BBC_WHITE, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onReturnToFlight,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2C3B)),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.height(28.dp).testTag("exit_battle_btn")
                    ) {
                        Text("FLIGHT", color = BBC_CYAN, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Scenario Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (sc in CombatScenario.values()) {
                    val isSel = selectedScenario == sc
                    Button(
                        onClick = {
                            selectedScenario = sc
                            soundSynth.playBeep(true)
                            flightEngine.spawnBattleWave(sc)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSel) Color(0xFF7A1B1B) else Color(0xFF141F28)
                        ),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.weight(1f).height(24.dp)
                    ) {
                        Text(
                            text = sc.title.take(8),
                            color = if (isSel) BBC_YELLOW else Color.LightGray,
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Target Tactical Readout (Top Left)
        if (lockedTarget != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 80.dp)
                    .background(Color(0xCC000000))
                    .border(1.dp, BBC_YELLOW.copy(alpha = 0.5f))
                    .padding(6.dp)
            ) {
                Text(
                    text = "TARGET: ${lockedTarget.blueprint.name}",
                    color = BBC_YELLOW,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "DIST: ${"%.1f".format(lockedTarget.position.length())}M",
                    color = BBC_WHITE,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "SHIELD: ${lockedTarget.shields.toInt()}%",
                    color = if (lockedTarget.shields > 40f) BBC_GREEN else BBC_RED,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "BOUNTY: ${lockedTarget.blueprint.bounty}.0 CR",
                    color = BBC_CYAN,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 4. Incoming Threat Alert Banner
        if (flightEngine.incomingMissileCountdownMs > 0L) {
            val remSec = ((flightEngine.incomingMissileCountdownMs - System.currentTimeMillis()).coerceAtLeast(0L) / 1000.0)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xEE880000))
                    .border(2.dp, BBC_YELLOW)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "MISSILE INCOMING! (${"%.1f".format(remSec)}s) - USE ECM!",
                    color = BBC_WHITE,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 5. Bottom Combat Controls: Lasers, Missiles, ECM, Throttle
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Auto-Play Autonomous Mode Active Banner
            if (flightEngine.isAutoPlayActive) {
                Text(
                    text = "AUTO-PLAY [${flightEngine.autoPlayMode.title}]: ${flightEngine.autoPlayStatusText}",
                    color = Color(0xFFFFCCFF),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .background(Color(0xDD3A0D3A), RoundedCornerShape(3.dp))
                        .border(1.dp, Color(0xFFFF88FF), RoundedCornerShape(3.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            // Flight messages
            if (System.currentTimeMillis() < flightEngine.statusMessageTimeMs && flightEngine.statusMessage.isNotEmpty()) {
                Text(
                    text = flightEngine.statusMessage,
                    color = BBC_YELLOW,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .background(Color(0xCC000000))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Throttle Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { flightEngine.speed = (flightEngine.speed + 3f).coerceAtMost(30f) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF143044)),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.size(width = 46.dp, height = 32.dp)
                    ) {
                        Text("ACC", color = BBC_CYAN, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { flightEngine.speed = (flightEngine.speed - 3f).coerceAtLeast(0f) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF143044)),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.size(width = 46.dp, height = 32.dp)
                    ) {
                        Text("DEC", color = BBC_CYAN, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Weapons: LASER, MISSILE, ECM, ENERGY BOMB
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { flightEngine.fireLaser() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF881111)),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.size(width = 62.dp, height = 36.dp).testTag("arena_fire_laser_btn")
                    ) {
                        Text("LASER", color = BBC_WHITE, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

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
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.size(width = 48.dp, height = 36.dp).testTag("arena_missile_btn")
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16384C)),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.size(width = 44.dp, height = 36.dp).testTag("arena_ecm_btn")
                    ) {
                        Text("ECM", color = BBC_CYAN, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }

                    if (commander.hasEnergyBomb) {
                        Button(
                            onClick = { flightEngine.triggerEnergyBomb() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B1B6B)),
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier.size(width = 46.dp, height = 36.dp)
                        ) {
                            Text("BOMB", color = BBC_WHITE, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
