package com.example.elite.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.elite.math3d.WireframeRenderer
import com.example.elite.model.CommanderState
import com.example.elite.model.InvestmentType
import com.example.elite.model.StrategicContract
import com.example.elite.model.SystemData
import com.example.elite.model.TradeArbitrageEngine
import com.example.elite.ships.PlanetWireframeFactory
import com.example.elite.ships.ShipBlueprints
import kotlin.math.cos
import kotlin.math.sin

enum class SystemViewMode(val label: String) {
    PLANET("PLANET GLOBE"),
    SYSTEM("SOLAR SYSTEM"),
    STATION("CORIOLIS ORBIT")
}

enum class StrategyTab(val title: String) {
    ORBITAL_3D("3D ORBIT"),
    STRATEGY_SCAN("TACTICAL SCAN"),
    TRADE_ARBITRAGE("TRADE ROUTES"),
    COLONIZATION("COLONY EMPIRE"),
    CONTRACTS("CONTRACTS")
}

@Composable
fun PlanetarySystem3dView(
    system: SystemData,
    commander: CommanderState,
    allSystems: List<SystemData> = emptyList(),
    soundSynth: BbcSoundSynth? = null,
    onClose: () -> Unit,
    onHyperspaceJump: ((SystemData) -> Unit)? = null,
    onInvest: ((SystemData, InvestmentType) -> Unit)? = null,
    onAcceptContract: ((StrategicContract) -> Unit)? = null,
    onSelectTarget: ((SystemData) -> Unit)? = null,
    hasFuel: Boolean = true,
    distDeciLy: Int = 0,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(StrategyTab.ORBITAL_3D) }
    var viewMode by remember { mutableStateOf(SystemViewMode.PLANET) }
    var rotX by remember { mutableFloatStateOf(0.35f) }
    var rotY by remember { mutableFloatStateOf(0f) }
    var autoSpin by remember { mutableStateOf(true) }
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "system_spin")
    val animatedAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    val currentRotY = if (autoSpin) rotY + animatedAngle else rotY
    val planetWireColor = system.planetType.wireColor
    val starColor = system.starType.stellarColor

    // Pre-create 3D blueprints for this planet and star
    val planetBlueprint = remember(system.planetType) {
        PlanetWireframeFactory.createPlanetBlueprint(system.planetType, 220f)
    }
    val starBlueprint = remember(system.starType) {
        PlanetWireframeFactory.createStarBlueprint(system.starType, 240f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFA01070A))
            .border(1.5.dp, planetWireColor.copy(alpha = 0.6f))
            .testTag("planetary_system_3d_view")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- TOP HEADER: Title, Badges & Close ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF030D14))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "SYSTEM: ${system.name.uppercase()}",
                            color = BBC_YELLOW,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "[${system.planetType.displayName.uppercase()}]",
                            color = planetWireColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "${system.starType.displayName} • ${system.faction.displayName} (${system.securityLevel.title})",
                        color = Color(0xFFAABBCC),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = {
                        soundSynth?.playBeep(false)
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF661111)),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier
                        .size(width = 65.dp, height = 28.dp)
                        .testTag("close_3d_system_btn")
                ) {
                    Text("CLOSE", color = BBC_WHITE, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            // --- STRATEGIC NAVIGATION TABS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF07141E))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (tab in StrategyTab.values()) {
                    val isSel = activeTab == tab
                    Button(
                        onClick = {
                            activeTab = tab
                            soundSynth?.playBeep(true)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSel) Color(0xFF005528) else Color(0xFF0E1E2B)
                        ),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isSel) BBC_WHITE else Color(0xFFAABBCC),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
            }

            // --- TAB CONTENT AREA ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    StrategyTab.ORBITAL_3D -> {
                        // 3D Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        rotY += dragAmount.x * 0.008f
                                        rotX = (rotX - dragAmount.y * 0.008f).coerceIn(-1.4f, 1.4f)
                                    }
                                }
                        ) {
                            val width = size.width
                            val height = size.height
                            val center = Offset(width / 2f, height / 2f)

                            drawCircle(
                                color = planetWireColor.copy(alpha = 0.08f),
                                radius = minOf(width, height) * 0.46f,
                                center = center,
                                style = Stroke(1.0f)
                            )

                            when (viewMode) {
                                SystemViewMode.PLANET -> {
                                    // 1. Render distinct planet type 3D wireframe mesh
                                    WireframeRenderer.renderBlueprintAt(
                                        drawScope = this,
                                        blueprint = planetBlueprint,
                                        rotX = rotX,
                                        rotY = currentRotY,
                                        rotZ = 0f,
                                        center = center,
                                        viewScale = 0.52f * zoomLevel,
                                        wireColor = planetWireColor,
                                        strokeWidth = 1.4f
                                    )

                                    // 2. Render Coriolis orbital station
                                    val orbitRadius = 175f * zoomLevel
                                    val stationAngle = currentRotY * 1.8f
                                    val stX = cos(stationAngle) * orbitRadius
                                    val stY = sin(stationAngle) * 22f

                                    drawOval(
                                        color = BBC_CYAN.copy(alpha = 0.22f),
                                        topLeft = Offset(center.x - orbitRadius, center.y - (orbitRadius * 0.45f)),
                                        size = androidx.compose.ui.geometry.Size(orbitRadius * 2f, orbitRadius * 0.9f),
                                        style = Stroke(1.0f)
                                    )

                                    WireframeRenderer.renderBlueprintAt(
                                        drawScope = this,
                                        blueprint = ShipBlueprints.CORIOLIS,
                                        rotX = 0.2f,
                                        rotY = currentRotY * 2.5f,
                                        rotZ = 0f,
                                        center = Offset(center.x + stX, center.y + stY),
                                        viewScale = 0.35f * zoomLevel,
                                        wireColor = BBC_WHITE,
                                        strokeWidth = 1.6f
                                    )
                                }

                                SystemViewMode.SYSTEM -> {
                                    val sunCenter = Offset(center.x - 70f * zoomLevel, center.y)

                                    // Render Central Star with its specific spectral classification mesh
                                    WireframeRenderer.renderBlueprintAt(
                                        drawScope = this,
                                        blueprint = starBlueprint,
                                        rotX = rotX * 0.4f,
                                        rotY = currentRotY * 0.4f,
                                        rotZ = 0f,
                                        center = sunCenter,
                                        viewScale = 0.42f * zoomLevel,
                                        wireColor = starColor,
                                        strokeWidth = 1.7f
                                    )

                                    // Orbit ellipse
                                    val pOrbitRx = 185f * zoomLevel
                                    val pOrbitRy = 85f * zoomLevel
                                    drawOval(
                                        color = BBC_YELLOW.copy(alpha = 0.25f),
                                        topLeft = Offset(sunCenter.x - pOrbitRx, sunCenter.y - pOrbitRy),
                                        size = androidx.compose.ui.geometry.Size(pOrbitRx * 2f, pOrbitRy * 2f),
                                        style = Stroke(1.2f)
                                    )

                                    val pAngle = currentRotY * 0.7f
                                    val pX = sunCenter.x + cos(pAngle) * pOrbitRx
                                    val pY = sunCenter.y + sin(pAngle) * pOrbitRy
                                    WireframeRenderer.renderBlueprintAt(
                                        drawScope = this,
                                        blueprint = planetBlueprint,
                                        rotX = rotX,
                                        rotY = currentRotY * 2f,
                                        rotZ = 0f,
                                        center = Offset(pX, pY),
                                        viewScale = 0.22f * zoomLevel,
                                        wireColor = planetWireColor,
                                        strokeWidth = 1.2f
                                    )
                                }

                                SystemViewMode.STATION -> {
                                    WireframeRenderer.renderBlueprintAt(
                                        drawScope = this,
                                        blueprint = ShipBlueprints.CORIOLIS,
                                        rotX = rotX,
                                        rotY = currentRotY,
                                        rotZ = 0f,
                                        center = center,
                                        viewScale = 0.95f * zoomLevel,
                                        wireColor = BBC_YELLOW,
                                        strokeWidth = 1.9f
                                    )

                                    val bgOffset = Offset(center.x + 135f * zoomLevel, center.y - 100f * zoomLevel)
                                    WireframeRenderer.renderBlueprintAt(
                                        drawScope = this,
                                        blueprint = planetBlueprint,
                                        rotX = 0.2f,
                                        rotY = currentRotY * 0.3f,
                                        rotZ = 0f,
                                        center = bgOffset,
                                        viewScale = 0.18f * zoomLevel,
                                        wireColor = planetWireColor.copy(alpha = 0.45f),
                                        strokeWidth = 1.1f
                                    )
                                }
                            }
                        }

                        // Mode Selector (Left)
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (mode in SystemViewMode.values()) {
                                val isSel = viewMode == mode
                                Button(
                                    onClick = {
                                        viewMode = mode
                                        soundSynth?.playBeep(true)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSel) Color(0xFF005522) else Color(0xFF14222E)
                                    ),
                                    shape = RoundedCornerShape(3.dp),
                                    modifier = Modifier.size(width = 80.dp, height = 28.dp)
                                ) {
                                    Text(
                                        text = mode.name,
                                        color = if (isSel) BBC_WHITE else Color(0xFFAABBCC),
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Right Zoom Controls
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Button(
                                onClick = {
                                    zoomLevel = (zoomLevel + 0.25f).coerceAtMost(2.5f)
                                    soundSynth?.playBeep(true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152838)),
                                shape = RoundedCornerShape(3.dp),
                                modifier = Modifier.size(width = 46.dp, height = 28.dp)
                            ) {
                                Text("ZOOM+", color = BBC_CYAN, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    zoomLevel = (zoomLevel - 0.25f).coerceAtLeast(0.6f)
                                    soundSynth?.playBeep(true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152838)),
                                shape = RoundedCornerShape(3.dp),
                                modifier = Modifier.size(width = 46.dp, height = 28.dp)
                            ) {
                                Text("ZOOM-", color = BBC_CYAN, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    autoSpin = !autoSpin
                                    soundSynth?.playBeep(true)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (autoSpin) Color(0xFF005522) else Color(0xFF333333)
                                ),
                                shape = RoundedCornerShape(3.dp),
                                modifier = Modifier.size(width = 46.dp, height = 28.dp)
                            ) {
                                Text(if (autoSpin) "SPIN" else "HOLD", color = BBC_WHITE, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    StrategyTab.STRATEGY_SCAN -> {
                        // Tactical Planetary & System Readout
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                TacticalInfoCard(
                                    title = "PLANETARY CLASSIFICATION: ${system.planetType.displayName.uppercase()}",
                                    color = planetWireColor
                                ) {
                                    Text(
                                        text = system.planetType.description,
                                        color = BBC_WHITE,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("ATMOSPHERE: ${system.planetType.atmosphere}", color = BBC_CYAN, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        Text("TEMP: ${system.planetType.surfaceTempC}°C", color = BBC_YELLOW, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("GRAVITY: ${system.planetType.gravityG}G", color = BBC_WHITE, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        Text("PRIMARY YIELD: ${system.planetType.primaryResource}", color = BBC_GREEN, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }

                            item {
                                TacticalInfoCard(
                                    title = "STELLAR STAR: ${system.starType.displayName.uppercase()} (${system.starType.spectralClass})",
                                    color = starColor
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("RADIATION: ${system.starType.radiationLevel}", color = BBC_WHITE, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        Text("FUEL SCOOP FACTOR: ${system.starType.solarScoopRate}X", color = BBC_GREEN, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }

                            item {
                                TacticalInfoCard(
                                    title = "FACTION & LAW: ${system.faction.displayName.uppercase()}",
                                    color = system.faction.color
                                ) {
                                    Text("\"${system.faction.motto}\"", color = BBC_WHITE, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("SECURITY RATING: ${system.securityLevel.title}", color = system.securityLevel.color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text("POLICE PATROLS: ${system.faction.securitySupport}", color = BBC_CYAN, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text("MARKET TARIFF RATE: ${system.faction.tariffRate}%", color = BBC_YELLOW, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    StrategyTab.TRADE_ARBITRAGE -> {
                        // Live Trade Routes & Market Arbitrage
                        val routes = remember(system.id) {
                            if (allSystems.isNotEmpty()) {
                                TradeArbitrageEngine.findTopArbitrageRoutes(system, allSystems, 7.0f, commander.cargoCapacity)
                            } else emptyList()
                        }

                        if (routes.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "SCANNING HYPERSPACE MARKET CHANNELS...\nNO EXPORT ARBITRAGE DETECTED WITHIN 7.0 LY",
                                    color = BBC_CYAN,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(routes) { route ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1720)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BBC_CYAN.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "BUY: ${route.commodityName.uppercase()}  ->  SELL AT: ${route.targetSystem.name.uppercase()}",
                                                    color = BBC_YELLOW,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = "${"%.1f".format(route.distanceLy)} LY",
                                                    color = BBC_CYAN,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(
                                                    text = "PROFIT: +${"%.1f".format(route.profitPerTonneDeciCr / 10.0)} CR/t  (Est. +${"%.1f".format(route.potentialTotalProfitCredits)} CR)",
                                                    color = BBC_GREEN,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = "ROI: +${route.roiPercent}% | RISK: ${route.riskRating}",
                                                    color = Color(0xFFAABBCC),
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            if (onSelectTarget != null) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Button(
                                                    onClick = {
                                                        onSelectTarget(route.targetSystem)
                                                        soundSynth?.playBeep(true)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005524)),
                                                    shape = RoundedCornerShape(2.dp),
                                                    modifier = Modifier.fillMaxWidth().height(24.dp)
                                                ) {
                                                    Text("LOCK ${route.targetSystem.name.uppercase()} AS HYPERSPACE TARGET", color = BBC_WHITE, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    StrategyTab.COLONIZATION -> {
                        // Colony Investments & Outpost Construction
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF081C10)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BBC_GREEN.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "COLONIAL OUTPOST STRATEGY & INVESTMENTS",
                                            color = BBC_GREEN,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Invest credits to establish autonomous outposts in ${system.name}. Outposts yield recurring dividends on every hyperspace jump!",
                                            color = BBC_WHITE,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        val sysInvestments = commander.investments.filter { it.systemId == system.id }
                                        val totalSysDiv = sysInvestments.sumOf { it.currentDividendCredits() }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "ACTIVE OUTPOSTS IN ${system.name}: ${sysInvestments.size} | DIVIDEND YIELD: +$totalSysDiv CR/JUMP",
                                            color = BBC_YELLOW,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            items(InvestmentType.values().toList()) { invType ->
                                val existing = commander.investments.find { it.systemId == system.id && it.type == invType }
                                val cost = existing?.upgradeCostCredits() ?: invType.baseCostCredits
                                val canAfford = commander.cashDeciCredits >= (cost * 10L)

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A24)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (existing != null) BBC_GREEN else Color(0xFF263D52)),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = invType.title.uppercase(),
                                                color = BBC_YELLOW,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = if (existing != null) "LEVEL ${existing.level}" else "UNBUILT",
                                                color = if (existing != null) BBC_GREEN else Color.Gray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Text(
                                            text = invType.description,
                                            color = Color(0xFFAABBCC),
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "YIELD: +${existing?.currentDividendCredits() ?: invType.dividendPerJumpCredits} CR / Jump",
                                                color = BBC_CYAN,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )

                                            Button(
                                                onClick = {
                                                    onInvest?.invoke(system, invType)
                                                },
                                                enabled = canAfford,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF006622),
                                                    disabledContainerColor = Color(0xFF222822)
                                                ),
                                                shape = RoundedCornerShape(3.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text(
                                                    text = if (existing != null) "UPGRADE (${cost} CR)" else "CONSTRUCT (${cost} CR)",
                                                    color = if (canAfford) BBC_WHITE else Color.Gray,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    StrategyTab.CONTRACTS -> {
                        // System Contracts & Mercenary Missions
                        val contracts = remember(system.id) {
                            if (allSystems.isNotEmpty()) {
                                TradeArbitrageEngine.generateContractsForSystem(system, allSystems)
                            } else emptyList()
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    text = "LOCAL SYSTEM STRATEGIC MISSIONS BOARD",
                                    color = BBC_YELLOW,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            items(contracts) { contract ->
                                val isAccepted = commander.activeContracts.any { it.id == contract.id }
                                val activeItem = commander.activeContracts.find { it.id == contract.id }
                                val isComplete = activeItem?.isCompleted == true

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101C26)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isComplete) BBC_GREEN else BBC_CYAN.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${contract.type.label}: ${contract.title}",
                                                color = BBC_YELLOW,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "REWARD: ${contract.rewardCredits} CR",
                                                color = BBC_GREEN,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = contract.requirementDescription,
                                            color = BBC_WHITE,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "DESTINATION: ${contract.targetSystemName}",
                                                color = BBC_CYAN,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )

                                            Button(
                                                onClick = {
                                                    onAcceptContract?.invoke(contract)
                                                },
                                                enabled = !isAccepted,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF005522),
                                                    disabledContainerColor = Color(0xFF22332A)
                                                ),
                                                shape = RoundedCornerShape(2.dp),
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Text(
                                                    text = when {
                                                        isComplete -> "COMPLETED"
                                                        isAccepted -> "IN PROGRESS"
                                                        else -> "ACCEPT CONTRACT"
                                                    },
                                                    color = BBC_WHITE,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- BOTTOM STATUS & HYPERSPACE JUMP ACTION ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF030A0F))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TARGET: ${system.name} | DIST: ${"%.1f".format(distDeciLy / 10.0)} LY | FUEL: ${"%.1f".format(commander.fuelDeciLy / 10.0)} LY",
                    color = BBC_GREEN,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (onHyperspaceJump != null && system.id != commander.currentSystemId) {
                    Button(
                        onClick = { onHyperspaceJump(system) },
                        enabled = hasFuel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00661E),
                            disabledContainerColor = Color(0xFF222822)
                        ),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("jump_from_3d_view_btn")
                    ) {
                        Text(
                            text = if (hasFuel) "JUMP HYPERSPACE" else "NO FUEL",
                            color = if (hasFuel) BBC_WHITE else Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TacticalInfoCard(
    title: String,
    color: Color,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF08141E)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}
