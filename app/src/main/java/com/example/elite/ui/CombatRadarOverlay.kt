package com.example.elite.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elite.flight.FlightEngine
import com.example.elite.flight.SpaceEntity
import com.example.elite.math3d.Matrix3x3
import com.example.elite.math3d.Vector3
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Dynamic Combat Overlay and Wireframe Radar Blip Indicator.
 *
 * Provides authentic 1984 Elite-inspired combat tactical telemetry:
 * 1. Wireframe 3D tactical radar scanner with vertical stalks in the cockpit viewport.
 * 2. Dynamic 3D off-screen wireframe chevrons / pointer carats for hostile ships outside the FOV.
 * 3. Tactical wireframe diamond targeting brackets with range, closing velocity, and shield status.
 * 4. Lock-on lead reticle (fire solution vector) for dogfight deflection shooting.
 * 5. Audio-visual proximity alert warnings when enemy ships are within firing range.
 */

data class RadarBlipData(
    val entity: SpaceEntity,
    val relPos: Vector3,
    val distance: Float,
    val isHostile: Boolean,
    val isLocked: Boolean,
    val isAhead: Boolean,
    val screenX: Float?,
    val screenY: Float?,
    val isOffScreen: Boolean,
    val offScreenAngleRad: Float?
)

@Composable
fun CombatRadarOverlay(
    flightEngine: FlightEngine,
    camMatrix: Matrix3x3,
    radarRange: Float = 2500f,
    showMiniRadar: Boolean = true,
    showOffScreenIndicators: Boolean = true,
    showLeadReticle: Boolean = true,
    modifier: Modifier = Modifier
) {
    val entities = flightEngine.entities
    val lockedTarget = flightEngine.lockedTarget
    val hostileEntities = remember(entities) {
        entities.filter { it.isHostile && !it.isDestroyed }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("dynamic_combat_overlay")
    ) {
        // Fullscreen Canvas for 3D In-Cockpit HUD Indicators (Reticles, Lead Indicators, Off-screen Chevrons)
        Canvas(modifier = Modifier.fillMaxSize().testTag("combat_hud_canvas")) {
            val width = size.width
            val height = size.height
            val cx = width / 2f
            val cy = height / 2f
            val focalLength = 360f

            val margin = 32f
            val minX = margin
            val maxX = width - margin
            val minY = margin + 20f
            val maxY = height - margin - 50f

            for (entity in entities) {
                if (entity.isDestroyed) continue
                // Transform position to camera space
                val camPos = camMatrix.transform(entity.position)
                val dist = camPos.length()
                val isAhead = camPos.z > 15f
                val isHostile = entity.isHostile
                val isLocked = entity == lockedTarget

                if (isAhead) {
                    val scale = focalLength / camPos.z
                    val sx = cx + camPos.x * scale
                    val sy = cy - camPos.y * scale

                    val inBounds = sx in minX..maxX && sy in minY..maxY

                    if (inBounds) {
                        // Render 3D dynamic wireframe targeting diamond around hostile / locked craft
                        if (isHostile || isLocked) {
                            drawTacticalBracket(
                                entity = entity,
                                center = Offset(sx, sy),
                                camZ = camPos.z,
                                isHostile = isHostile,
                                isLocked = isLocked
                            )

                            // Lead Computing Sight (Gunnery deflection pip) for dogfighting
                            if (showLeadReticle && (isHostile || isLocked) && camPos.z in 50f..1400f) {
                                drawLeadReticle(
                                    camPos = camPos,
                                    targetEntity = entity,
                                    camMatrix = camMatrix,
                                    playerSpeed = flightEngine.speed,
                                    screenCenter = Offset(cx, cy),
                                    focalLength = focalLength
                                )
                            }
                        }
                    } else if (showOffScreenIndicators && isHostile) {
                        // Off-screen indicator on screen perimeter for hostiles in forward hemisphere
                        val dx = sx - cx
                        val dy = sy - cy
                        val angle = atan2(dy, dx)
                        drawOffScreenChevron(
                            center = Offset(cx, cy),
                            angle = angle,
                            dist = dist,
                            isRear = false,
                            width = width,
                            height = height,
                            margin = margin
                        )
                    }
                } else if (showOffScreenIndicators && isHostile) {
                    // Enemy is BEHIND player! Indicate reverse threat vector with pulsing red diamond
                    val dx = -camPos.x
                    val dy = camPos.y
                    val angle = atan2(dy, dx)
                    drawOffScreenChevron(
                        center = Offset(cx, cy),
                        angle = angle,
                        dist = dist,
                        isRear = true,
                        width = width,
                        height = height,
                        margin = margin
                    )
                }
            }
        }

        // Top-Left Tactical Threat Ticker (Blinking danger warning if hostiles present)
        if (hostileEntities.isNotEmpty()) {
            TacticalThreatBanner(
                hostileCount = hostileEntities.size,
                nearestHostileDist = hostileEntities.minOfOrNull { it.position.length() }?.toInt() ?: 0,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 56.dp)
            )
        }

        // Top-Right In-Cockpit Wireframe 3D Radar Blip Scanner Widget
        if (showMiniRadar) {
            CockpitRadarWidget(
                flightEngine = flightEngine,
                camMatrix = camMatrix,
                radarRange = radarRange,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 56.dp)
            )
        }
    }
}

/**
 * Draws an authentic wireframe tactical diamond bracket with corner ticks and range tag.
 */
private fun DrawScope.drawTacticalBracket(
    entity: SpaceEntity,
    center: Offset,
    camZ: Float,
    isHostile: Boolean,
    isLocked: Boolean
) {
    val size = (4200f / camZ).coerceIn(24f, 72f)
    val half = size / 2f
    val bracketColor = when {
        isLocked && isHostile -> BBC_YELLOW
        isHostile -> BBC_RED
        isLocked -> BBC_CYAN
        else -> BBC_GREEN
    }

    // Outer wireframe diamond
    val path = Path().apply {
        moveTo(center.x, center.y - half)
        lineTo(center.x + half, center.y)
        lineTo(center.x, center.y + half)
        lineTo(center.x - half, center.y)
        close()
    }
    drawPath(path = path, color = bracketColor, style = Stroke(width = if (isLocked) 2.2f else 1.4f))

    // Inner reticle ticks for locked target
    if (isLocked) {
        val tick = half * 0.4f
        drawLine(bracketColor, Offset(center.x - tick, center.y), Offset(center.x + tick, center.y), strokeWidth = 1.5f)
        drawLine(bracketColor, Offset(center.x, center.y - tick), Offset(center.x, center.y + tick), strokeWidth = 1.5f)
    }

    // Range bar at bottom of diamond (indicating distance / shield level)
    val barWidth = size * 0.8f
    val shieldRatio = (entity.shields / 100f).coerceIn(0f, 1f)
    val barY = center.y + half + 4f
    drawRect(
        color = Color(0x66000000),
        topLeft = Offset(center.x - barWidth / 2f, barY),
        size = Size(barWidth, 3f)
    )
    drawRect(
        color = if (shieldRatio < 0.35f) BBC_RED else BBC_CYAN,
        topLeft = Offset(center.x - barWidth / 2f, barY),
        size = Size(barWidth * shieldRatio, 3f)
    )
}

/**
 * Calculates and draws the deflection lead gunnery pip (predictive intercept solution).
 */
private fun DrawScope.drawLeadReticle(
    camPos: Vector3,
    targetEntity: SpaceEntity,
    camMatrix: Matrix3x3,
    playerSpeed: Float,
    screenCenter: Offset,
    focalLength: Float
) {
    // Laser speed in BBC Micro Elite is near-instantaneous or high velocity (~2000m/s)
    val laserSpeed = 1600f
    val timeToImpact = (camPos.z / laserSpeed).coerceIn(0.05f, 0.9f)

    // Projected position after flight time taking relative motion into account
    val leadWorld = targetEntity.position + (targetEntity.velocity * timeToImpact) - Vector3(0f, 0f, playerSpeed * 10f * timeToImpact)
    val leadCam = camMatrix.transform(leadWorld)

    if (leadCam.z > 20f) {
        val scale = focalLength / leadCam.z
        val lx = screenCenter.x + leadCam.x * scale
        val ly = screenCenter.y - leadCam.y * scale

        // Draw predictive circular pip with crosshair
        drawCircle(
            color = BBC_YELLOW.copy(alpha = 0.85f),
            radius = 6f,
            center = Offset(lx, ly),
            style = Stroke(1.5f)
        )
        drawCircle(
            color = BBC_YELLOW,
            radius = 1.5f,
            center = Offset(lx, ly)
        )
    }
}

/**
 * Off-screen chevron indicator pinned to the viewport perimeter, showing the direction
 * of off-screen hostile targets with distance reading.
 */
private fun DrawScope.drawOffScreenChevron(
    center: Offset,
    angle: Float,
    dist: Float,
    isRear: Boolean,
    width: Float,
    height: Float,
    margin: Float
) {
    // Calculate perimeter intersection
    val rx = (width / 2f) - margin
    val ry = (height / 2f) - margin

    val cosA = cos(angle)
    val sinA = sin(angle)

    val scaleX = if (cosA != 0f) rx / kotlin.math.abs(cosA) else Float.MAX_VALUE
    val scaleY = if (sinA != 0f) ry / kotlin.math.abs(sinA) else Float.MAX_VALUE
    val minScale = minOf(scaleX, scaleY)

    val edgeX = center.x + cosA * minScale
    val edgeY = center.y + sinA * minScale

    val color = if (isRear) Color(0xFFFF2222) else BBC_YELLOW
    val chevronSize = 10f

    // Draw directional pointer arrow
    val tipX = edgeX
    val tipY = edgeY
    val leftX = edgeX - cos(angle - 0.45f) * chevronSize * 1.4f
    val leftY = edgeY - sin(angle - 0.45f) * chevronSize * 1.4f
    val rightX = edgeX - cos(angle + 0.45f) * chevronSize * 1.4f
    val rightY = edgeY - sin(angle + 0.45f) * chevronSize * 1.4f

    val chevronPath = Path().apply {
        moveTo(tipX, tipY)
        lineTo(leftX, leftY)
        lineTo(edgeX - cosA * chevronSize * 0.7f, edgeY - sinA * chevronSize * 0.7f)
        lineTo(rightX, rightY)
        close()
    }
    drawPath(path = chevronPath, color = color)

    // Distance tick marks (closer = more ticks)
    val pips = when {
        dist < 400f -> 3
        dist < 1000f -> 2
        else -> 1
    }
    for (i in 1..pips) {
        val pipOffset = i * 4.5f
        val pipX = edgeX - cosA * (chevronSize + pipOffset)
        val pipY = edgeY - sinA * (chevronSize + pipOffset)
        drawCircle(color = color, radius = 1.6f, center = Offset(pipX, pipY))
    }
}

/**
 * Compact In-Cockpit Wireframe 3D Radar Blip Indicator Widget.
 *
 * Emulates the authentic BBC Micro 3D Scanner radar:
 * - Green wireframe elliptical ground plane with dual-plane axes.
 * - Vertical stalks protruding up (positive Y) or down (negative Y).
 * - Color-coded blip pips at stalk tips:
 *   Red = Hostile ship
 *   Yellow = Space Station (Coriolis / Dodec)
 *   Cyan = Planet / Nav Target
 *   White = Cargo Canister / Mineral Rock
 *   Green = Neutral Civilian / Police Viper
 * - Distinct halo pulse on currently locked target!
 */
@Composable
fun CockpitRadarWidget(
    flightEngine: FlightEngine,
    camMatrix: Matrix3x3,
    radarRange: Float = 2500f,
    modifier: Modifier = Modifier
) {
    val entities = flightEngine.entities
    val lockedTarget = flightEngine.lockedTarget

    Box(
        modifier = modifier
            .size(width = 132.dp, height = 82.dp)
            .background(Color(0xDD020C06), RoundedCornerShape(4.dp))
            .border(1.dp, BBC_GREEN.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
            .testTag("cockpit_wireframe_radar")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val cx = width / 2f
            val cy = height / 2f + 2f

            val rx = width * 0.44f
            val ry = height * 0.36f

            // 1. Radar Grid Ellipse & Axes (BBC Mode 5 green CRT style)
            drawOval(
                color = BBC_GREEN.copy(alpha = 0.65f),
                topLeft = Offset(cx - rx, cy - ry),
                size = Size(rx * 2f, ry * 2f),
                style = Stroke(1.2f)
            )
            // Inner range range ring (half radius)
            drawOval(
                color = BBC_GREEN.copy(alpha = 0.25f),
                topLeft = Offset(cx - rx * 0.5f, cy - ry * 0.5f),
                size = Size(rx, ry),
                style = Stroke(0.8f)
            )
            // Horizontal & Vertical axes
            drawLine(
                color = BBC_GREEN.copy(alpha = 0.35f),
                start = Offset(cx - rx, cy),
                end = Offset(cx + rx, cy),
                strokeWidth = 1f
            )
            drawLine(
                color = BBC_GREEN.copy(alpha = 0.35f),
                start = Offset(cx, cy - ry),
                end = Offset(cx, cy + ry),
                strokeWidth = 1f
            )

            // Center Player Pip (Cobra Mk III)
            drawCircle(
                color = BBC_YELLOW,
                radius = 2.2f,
                center = Offset(cx, cy)
            )

            // 2. Render 3D Radar Blips with vertical stalks
            for (entity in entities) {
                if (entity.isDestroyed) continue

                // Relative position in player's forward coordinate frame
                val px = entity.position.x
                val py = entity.position.y
                val pz = entity.position.z

                // Normalize against radar range
                val normX = (px / radarRange).coerceIn(-1.2f, 1.2f)
                val normZ = (pz / radarRange).coerceIn(-1.2f, 1.2f)
                val normY = (py / radarRange).coerceIn(-1.0f, 1.0f)

                // Ground plane position (X = sideways, Z = forward/behind)
                val blipGroundX = cx + (normX * rx)
                val blipGroundY = cy - (normZ * ry)

                // Height stalk displacement: Y > 0 is above, Y < 0 is below
                val stalkLength = (normY * ry * 0.8f).coerceIn(-ry * 0.9f, ry * 0.9f)
                val blipHeadY = blipGroundY - stalkLength

                // Check if blip falls inside or near the radar boundary
                val dxNorm = (blipGroundX - cx) / rx
                val dyNorm = (blipGroundY - cy) / ry
                if (dxNorm * dxNorm + dyNorm * dyNorm > 1.35f) continue

                val isLocked = entity == lockedTarget
                val blipColor = when {
                    isLocked -> BBC_YELLOW
                    entity.isHostile -> BBC_RED
                    entity.blueprint.name.contains("Station") -> BBC_YELLOW
                    entity.blueprint.name.contains("Planet") -> BBC_CYAN
                    entity.blueprint.name.contains("Sun") -> Color(0xFFFF9900)
                    entity.blueprint.name.contains("Canister") -> BBC_WHITE
                    else -> BBC_GREEN
                }

                // Stalk line from ground plane to elevation pip
                drawLine(
                    color = blipColor.copy(alpha = if (isLocked) 0.9f else 0.5f),
                    start = Offset(blipGroundX, blipGroundY),
                    end = Offset(blipGroundX, blipHeadY),
                    strokeWidth = if (isLocked) 1.6f else 1.0f,
                    cap = StrokeCap.Round
                )

                // Ground shadow dot
                drawCircle(
                    color = blipColor.copy(alpha = 0.3f),
                    radius = 1.2f,
                    center = Offset(blipGroundX, blipGroundY)
                )

                // Target elevation pip at stalk apex
                val pipRadius = if (isLocked) 3.5f else 2.2f
                drawCircle(
                    color = blipColor,
                    radius = pipRadius,
                    center = Offset(blipGroundX, blipHeadY)
                )

                // Blinking lock-on ring
                if (isLocked) {
                    drawCircle(
                        color = BBC_YELLOW,
                        radius = 5.5f,
                        center = Offset(blipGroundX, blipHeadY),
                        style = Stroke(1.2f)
                    )
                }
            }
        }

        // Radar Overlay Header & Mode Tag
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "3D SCANNER",
                color = BBC_GREEN,
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${(radarRange / 1000f).toInt()}KM",
                color = BBC_YELLOW,
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Top warning banner displayed when hostile threats are active.
 */
@Composable
private fun TacticalThreatBanner(
    hostileCount: Int,
    nearestHostileDist: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xCC300505), RoundedCornerShape(3.dp))
            .border(1.dp, BBC_RED, RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .testTag("tactical_threat_banner")
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(BBC_RED, RoundedCornerShape(1.dp))
            )
            Text(
                text = "ALERT: $hostileCount HOSTILE${if (hostileCount > 1) "S" else ""} [${nearestHostileDist}m]",
                color = BBC_RED,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
