package com.example.elite.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elite.flight.FlightEngine
import com.example.elite.flight.SpaceEntity
import com.example.elite.model.CommanderState
import kotlin.math.cos
import kotlin.math.sin

/**
 * Authentic BBC Micro Mode 5 & Mode 4 Split-Screen Dashboard (DIALS routine).
 * Renders the 3D space scanner with dual-plane vertical stalks, compass,
 * forward/aft shields, 4 energy banks, speed, roll, pitch, and fuel gauges.
 */

val BBC_BLACK = Color(0xFF000000)
val BBC_GREEN = Color(0xFF00FF33)
val BBC_YELLOW = Color(0xFFFFEE00)
val BBC_RED = Color(0xFFFF2222)
val BBC_CYAN = Color(0xFF00E5FF)
val BBC_WHITE = Color(0xFFFFFFFF)
val BBC_SCANNER_BG = Color(0xFF021208)
val BBC_GREY = Color(0xFF445544)

@Composable
fun DashboardView(
    commander: CommanderState,
    flightEngine: FlightEngine,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(Color(0xFF04090C))
            .testTag("dashboard_view")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Dashboard border separator line
            drawLine(
                color = BBC_YELLOW,
                start = Offset(0f, 1f),
                end = Offset(width, 1f),
                strokeWidth = 2.5f
            )

            val colWidth = width * 0.28f
            val scannerWidth = width * 0.44f

            // Left Section: Shields, Fuel, Cabin/Laser Temp, Altitude
            drawLeftGauges(commander, Offset(8f, 10f), Size(colWidth - 16f, height - 16f))

            // Center Section: Dual-Plane 3D Radar Scanner & Compass
            drawScannerAndCompass(
                entities = flightEngine.entities,
                targetNavEntity = flightEngine.getNavTargetEntity(),
                navTarget = flightEngine.navTarget,
                center = Offset(width * 0.5f, height * 0.55f),
                radiusX = scannerWidth * 0.46f,
                radiusY = (height - 24f) * 0.44f
            )

            // Right Section: Speed, Roll, Pitch, 4 Energy Banks, Missiles
            drawRightGauges(
                commander = commander,
                flightEngine = flightEngine,
                origin = Offset(width - colWidth + 8f, 10f),
                size = Size(colWidth - 16f, height - 16f)
            )
        }
    }
}

private fun DrawScope.drawLeftGauges(
    commander: CommanderState,
    origin: Offset,
    size: Size
) {
    val barHeight = 8f
    val gap = 12f
    var curY = origin.y

    // FS - Forward Shield
    drawGaugeBar(
        label = "FS",
        value = commander.forwardShield / 100f,
        origin = Offset(origin.x, curY),
        width = size.width,
        height = barHeight,
        color = BBC_GREEN
    )
    curY += gap

    // AS - Aft Shield
    drawGaugeBar(
        label = "AS",
        value = commander.aftShield / 100f,
        origin = Offset(origin.x, curY),
        width = size.width,
        height = barHeight,
        color = BBC_GREEN
    )
    curY += gap

    // FU - Fuel (7.0 LY capacity = 70 deci-ly)
    drawGaugeBar(
        label = "FU",
        value = (commander.fuelDeciLy / 70f).coerceIn(0f, 1f),
        origin = Offset(origin.x, curY),
        width = size.width,
        height = barHeight,
        color = BBC_CYAN
    )
    curY += gap

    // CT - Cabin Temp
    drawGaugeBar(
        label = "CT",
        value = (commander.cabinTemp / 100f).coerceIn(0f, 1f),
        origin = Offset(origin.x, curY),
        width = size.width,
        height = barHeight,
        color = if (commander.cabinTemp > 75f) BBC_RED else BBC_GREEN
    )
    curY += gap

    // LT - Laser Temp
    drawGaugeBar(
        label = "LT",
        value = (commander.laserTemp / 100f).coerceIn(0f, 1f),
        origin = Offset(origin.x, curY),
        width = size.width,
        height = barHeight,
        color = if (commander.laserTemp > 75f) BBC_RED else BBC_YELLOW
    )
    curY += gap

    // ALT - Altitude
    drawGaugeBar(
        label = "AL",
        value = (commander.altitude / 100f).coerceIn(0f, 1f),
        origin = Offset(origin.x, curY),
        width = size.width,
        height = barHeight,
        color = BBC_GREEN
    )
}

private fun DrawScope.drawRightGauges(
    commander: CommanderState,
    flightEngine: FlightEngine,
    origin: Offset,
    size: Size
) {
    val barHeight = 8f
    val gap = 12f
    var curY = origin.y

    // SP - Speed
    drawGaugeBar(
        label = "SP",
        value = (flightEngine.speed / flightEngine.maxSpeed).coerceIn(0f, 1f),
        origin = Offset(origin.x, curY),
        width = size.width,
        height = barHeight,
        color = BBC_YELLOW
    )
    curY += gap

    // RL - Roll indicator (centered zero)
    drawCenterGauge(
        label = "RL",
        value = flightEngine.rollRate,
        origin = Offset(origin.x, curY),
        width = size.width,
        height = barHeight,
        color = BBC_CYAN
    )
    curY += gap

    // PT - Pitch indicator (centered zero)
    drawCenterGauge(
        label = "PT",
        value = flightEngine.pitchRate,
        origin = Offset(origin.x, curY),
        width = size.width,
        height = barHeight,
        color = BBC_CYAN
    )
    curY += gap

    // 4 Energy Banks (four separate segments representing 4 banks)
    val energyTotal = commander.energyBanks // 0..100
    val bankWidth = (size.width - 24f) / 4f
    for (bank in 0 until 4) {
        val bankEnergy = (energyTotal - (bank * 25f)).coerceIn(0f, 25f) / 25f
        val bankX = origin.x + 16f + (bank * (bankWidth + 2f))
        drawRect(
            color = BBC_GREY,
            topLeft = Offset(bankX, curY),
            size = Size(bankWidth, barHeight)
        )
        if (bankEnergy > 0f) {
            drawRect(
                color = if (bank == 0 && energyTotal < 25f) BBC_RED else BBC_YELLOW,
                topLeft = Offset(bankX, curY),
                size = Size(bankWidth * bankEnergy, barHeight)
            )
        }
    }
    curY += gap + 2f

    // Missiles (1..4 square pips)
    for (m in 0 until 4) {
        val mX = origin.x + 18f + (m * 14f)
        val isArmed = m < commander.missiles
        drawRect(
            color = if (isArmed) BBC_GREEN else BBC_GREY,
            topLeft = Offset(mX, curY),
            size = Size(10f, 8f),
            style = if (isArmed) androidx.compose.ui.graphics.drawscope.Fill else Stroke(1.5f)
        )
    }
}

private fun DrawScope.drawGaugeBar(
    label: String,
    value: Float,
    origin: Offset,
    width: Float,
    height: Float,
    color: Color
) {
    val barX = origin.x + 18f
    val barWidth = width - 20f

    // Gauge background trough
    drawRect(
        color = BBC_GREY.copy(alpha = 0.5f),
        topLeft = Offset(barX, origin.y),
        size = Size(barWidth, height)
    )

    // Filled bar
    val fillWidth = barWidth * value.coerceIn(0f, 1f)
    if (fillWidth > 0f) {
        drawRect(
            color = color,
            topLeft = Offset(barX, origin.y),
            size = Size(fillWidth, height)
        )
    }

    // Border
    drawRect(
        color = BBC_GREEN.copy(alpha = 0.7f),
        topLeft = Offset(barX, origin.y),
        size = Size(barWidth, height),
        style = Stroke(1f)
    )
}

private fun DrawScope.drawCenterGauge(
    label: String,
    value: Float,
    origin: Offset,
    width: Float,
    height: Float,
    color: Color
) {
    val barX = origin.x + 18f
    val barWidth = width - 20f
    val midX = barX + barWidth / 2f

    // Background
    drawRect(
        color = BBC_GREY.copy(alpha = 0.5f),
        topLeft = Offset(barX, origin.y),
        size = Size(barWidth, height)
    )

    // Center tick
    drawLine(
        color = BBC_WHITE,
        start = Offset(midX, origin.y - 2f),
        end = Offset(midX, origin.y + height + 2f),
        strokeWidth = 1.5f
    )

    // Indicator deflection
    val maxDeflection = barWidth / 2f
    val deflection = (value * maxDeflection).coerceIn(-maxDeflection, maxDeflection)
    val left = if (deflection >= 0) midX else midX + deflection
    val w = kotlin.math.abs(deflection)

    if (w > 0f) {
        drawRect(
            color = color,
            topLeft = Offset(left, origin.y),
            size = Size(w, height)
        )
    }
}

/**
 * Renders the iconic 3D Dual-Plane Scanner Radar (DIALS routine).
 * Objects are projected onto an elliptical ground plane with vertical stalks!
 */
private fun DrawScope.drawScannerAndCompass(
    entities: List<SpaceEntity>,
    targetNavEntity: SpaceEntity?,
    navTarget: com.example.elite.flight.NavTarget,
    center: Offset,
    radiusX: Float,
    radiusY: Float
) {
    // Scanner Ellipse background
    drawOval(
        color = BBC_SCANNER_BG,
        topLeft = Offset(center.x - radiusX, center.y - radiusY),
        size = Size(radiusX * 2f, radiusY * 2f)
    )

    // Dual-plane crosshairs
    drawLine(
        color = BBC_GREEN.copy(alpha = 0.4f),
        start = Offset(center.x - radiusX, center.y),
        end = Offset(center.x + radiusX, center.y),
        strokeWidth = 1f
    )
    drawLine(
        color = BBC_GREEN.copy(alpha = 0.4f),
        start = Offset(center.x, center.y - radiusY),
        end = Offset(center.x, center.y + radiusY),
        strokeWidth = 1f
    )

    // Scanner Outer Rim
    drawOval(
        color = BBC_GREEN,
        topLeft = Offset(center.x - radiusX, center.y - radiusY),
        size = Size(radiusX * 2f, radiusY * 2f),
        style = Stroke(1.8f)
    )

    // Center player pip
    drawCircle(
        color = BBC_YELLOW,
        radius = 2.5f,
        center = center
    )

    // Draw 3D radar stalks for all entities
    val radarScale = 0.08f
    for (e in entities) {
        val relX = e.position.x * radarScale
        val relZ = e.position.z * radarScale
        val relY = e.position.y * radarScale

        // Ground plane projection on ellipse
        val scanX = center.x + (relX * (radiusX / 120f)).coerceIn(-radiusX * 0.9f, radiusX * 0.9f)
        val scanZ = center.y + (relZ * (radiusY / 120f)).coerceIn(-radiusY * 0.9f, radiusY * 0.9f)

        // Height stalk (up or down depending on Y)
        val stalkHeight = (relY * 0.35f).coerceIn(-radiusY * 0.7f, radiusY * 0.7f)
        val blipY = scanZ - stalkHeight

        val blipColor = when {
            e.isHostile -> BBC_RED
            e.blueprint.name.contains("Station") -> BBC_YELLOW
            e.blueprint.name.contains("Planet") -> BBC_CYAN
            e.blueprint.name.contains("Sun") -> Color(0xFFFF9900)
            e.blueprint.name.contains("Canister") -> BBC_WHITE
            else -> BBC_GREEN
        }

        // Vertical stalk line
        drawLine(
            color = blipColor.copy(alpha = 0.6f),
            start = Offset(scanX, scanZ),
            end = Offset(scanX, blipY),
            strokeWidth = 1.2f
        )

        // Target blip pip at tip of stalk
        drawCircle(
            color = blipColor,
            radius = if (e.blueprint.name.contains("Planet") || e.blueprint.name.contains("Sun")) 5f else 3.5f,
            center = Offset(scanX, blipY)
        )
    }

    // Target Compass (top center of dashboard, circular pip indicating nav target direction)
    val compassCenter = Offset(center.x, center.y - radiusY - 8f)
    drawCircle(
        color = when (navTarget) {
            com.example.elite.flight.NavTarget.STATION -> BBC_YELLOW
            com.example.elite.flight.NavTarget.PLANET -> BBC_CYAN
            com.example.elite.flight.NavTarget.SUN -> Color(0xFFFF9900)
        },
        radius = 7f,
        center = compassCenter,
        style = Stroke(1.2f)
    )
    targetNavEntity?.let { st ->
        val dir = st.position.normalized()
        val dotX = compassCenter.x + dir.x * 5f
        val dotY = compassCenter.y - dir.y * 5f
        drawCircle(
            color = if (dir.z >= 0) BBC_GREEN else BBC_RED, // Green if ahead, red if behind
            radius = 2.2f,
            center = Offset(dotX, dotY)
        )
    }
}
