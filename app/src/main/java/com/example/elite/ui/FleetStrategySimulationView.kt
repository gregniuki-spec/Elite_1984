package com.example.elite.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elite.audio.BbcSoundSynth
import com.example.elite.model.BotStrategy
import com.example.elite.model.CommanderState
import com.example.elite.model.FleetFaction
import com.example.elite.model.TacticalBotOpponent
import com.example.elite.simulation.SectorWarSimulationEngine
import kotlin.math.cos
import kotlin.math.sin

/**
 * Real-time Sector Strategic Fleet Simulation View.
 *
 * Simulates up to 100 AI Bot Opponents and realtime simulated networked players:
 * 1. 2D/3D Sector Tactical War Map: Real-time radar radar positions, factions, dogfights, and laser fire.
 * 2. Strategy Command Dispatch: Change strategies for individual bots or entire fleet wings
 *    (Aggressive Intercept, Swarm Ambush, Defensive Escort, Convoy Trader, Sector Patrol, Hit-and-Run, Berserk Assault).
 * 3. Realtime Simulated Players: Pilots with callsigns, latencies (ping ms), ranks, and live kill counts.
 * 4. Sector War Killfeed & Telemetry: Live combat events, bounties, and faction casualty counters.
 */
@Composable
fun FleetStrategySimulationView(
    sectorEngine: SectorWarSimulationEngine,
    commander: CommanderState,
    soundSynth: BbcSoundSynth,
    onLaunchDogfight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by sectorEngine.statsFlow.collectAsState()
    val bots = sectorEngine.botOpponents
    val feed = sectorEngine.combatCombatFeed

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Tactical Radar Map, 1 = Fleet Roster (100 Bots), 2 = Strategy Dispatch
    var selectedBotId by remember { mutableStateOf<Int?>(null) }
    var selectedFactionFilter by remember { mutableStateOf<FleetFaction?>(null) }

    val activeSelectedBot = bots.find { it.id == selectedBotId }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF03070C))
            .testTag("fleet_strategy_view")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: Real-time Sector Telemetry
            SectorWarHeader(
                stats = stats,
                simulationSpeed = sectorEngine.simulationSpeed,
                isRunning = sectorEngine.isSimulationRunning,
                isAutoPlayEnabled = sectorEngine.isAutoPlayEnabled,
                onToggleAutoPlay = {
                    sectorEngine.toggleAutoPlay()
                    soundSynth.playBeep(sectorEngine.isAutoPlayEnabled)
                },
                onToggleSimulation = {
                    sectorEngine.isSimulationRunning = !sectorEngine.isSimulationRunning
                    soundSynth.playBeep(sectorEngine.isSimulationRunning)
                },
                onReinforce = {
                    sectorEngine.reinforceFleet(25)
                    soundSynth.playMissileLaunch()
                },
                onLaunchDogfight = onLaunchDogfight
            )

            // Navigation Tabs
            val tabTitles = listOf("TACTICAL MAP", "FLEET ROSTER (100)", "STRATEGY DISPATCH")
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF0A1017),
                contentColor = Color(0xFF00E5FF),
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF00E5FF),
                        height = 2.dp
                    )
                },
                modifier = Modifier.fillMaxWidth().height(34.dp)
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            soundSynth.playBeep(true)
                        },
                        text = {
                            Text(
                                text = title,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) Color(0xFF00E5FF) else Color(0xFF88A0B0)
                            )
                        }
                    )
                }
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> TacticalSectorRadarMap(
                        bots = bots,
                        stats = stats,
                        selectedBot = activeSelectedBot,
                        feed = feed,
                        onSelectBot = { bot ->
                            selectedBotId = bot.id
                            soundSynth.playBeep(true)
                        }
                    )
                    1 -> FleetRosterView(
                        bots = bots,
                        selectedFaction = selectedFactionFilter,
                        selectedBot = activeSelectedBot,
                        onFilterFaction = { f -> selectedFactionFilter = f },
                        onSelectBot = { bot ->
                            selectedBotId = bot.id
                            soundSynth.playBeep(true)
                        }
                    )
                    2 -> StrategyDispatchView(
                        sectorEngine = sectorEngine,
                        bots = bots,
                        selectedBot = activeSelectedBot,
                        soundSynth = soundSynth
                    )
                }
            }

            // Persistent Bottom Bar: Selected Unit Quick Telemetry
            if (activeSelectedBot != null) {
                SelectedBotQuickBar(
                    bot = activeSelectedBot,
                    onStrategySelected = { strat ->
                        sectorEngine.setBotStrategy(activeSelectedBot.id, strat)
                        soundSynth.playBeep(true)
                    },
                    onDeselect = { selectedBotId = null }
                )
            }
        }
    }
}

@Composable
private fun SectorWarHeader(
    stats: com.example.elite.model.FleetWarStatistics,
    simulationSpeed: Float,
    isRunning: Boolean,
    isAutoPlayEnabled: Boolean,
    onToggleAutoPlay: () -> Unit,
    onToggleSimulation: () -> Unit,
    onReinforce: () -> Unit,
    onLaunchDogfight: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E1620))
            .border(1.dp, Color(0xFF1B2838))
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
                        text = "REALTIME SECTOR WAR",
                        color = Color(0xFFFF2222),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "[100 AI BOTS + MULTIPLAYER]",
                        color = Color(0xFFFFCC00),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "ACTIVE: ${stats.activeCount}/${stats.totalBots} | LOSSES: ${stats.destroyedCount} | REALTIME PLAYERS: ${stats.realPlayersCount} | ACE: ${stats.topAceCallsign} (${stats.topAceKills} K)",
                    color = Color(0xFF00E5FF),
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = onToggleAutoPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAutoPlayEnabled) Color(0xFF6B1B6B) else Color(0xFF222830)
                    ),
                    shape = RoundedCornerShape(3.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(26.dp).testTag("toggle_strategy_autoplay_btn")
                ) {
                    Text(
                        text = if (isAutoPlayEnabled) "AUTO-PLAY: ON" else "AUTO-PLAY: OFF",
                        color = if (isAutoPlayEnabled) Color(0xFFFFCC00) else Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onReinforce,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1010)),
                    shape = RoundedCornerShape(3.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(26.dp).testTag("reinforce_fleet_btn")
                ) {
                    Text("+25 HYPER", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onToggleSimulation,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) Color(0xFF144D29) else Color(0xFF6B4500)),
                    shape = RoundedCornerShape(3.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(26.dp).testTag("toggle_sim_btn")
                ) {
                    Text(if (isRunning) "SIM: RUN" else "SIM: PAUSE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onLaunchDogfight,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005518)),
                    shape = RoundedCornerShape(3.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(26.dp).testTag("enter_cockpit_dogfight_btn")
                ) {
                    Text("FLY DOGFIGHT", color = Color(0xFF00FF66), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Faction balance gauge
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "FACTIONS:", color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
            Text(text = "NAVY: ${stats.navyCount}", color = Color(0xFF00E5FF), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(text = "OUTLAWS: ${stats.outlawCount}", color = Color(0xFFFF2222), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(text = "TRADERS: ${stats.traderCount}", color = Color(0xFF00FF66), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(text = "SWARM: ${stats.alienCount}", color = Color(0xFFFFCC00), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Real-time Sector 2D Tactical Radar Scanner with 100 ship blips,
 * lasers, velocities, and dogfight trajectories.
 */
@Composable
private fun TacticalSectorRadarMap(
    bots: List<TacticalBotOpponent>,
    stats: com.example.elite.model.FleetWarStatistics,
    selectedBot: TacticalBotOpponent?,
    feed: List<String>,
    onSelectBot: (TacticalBotOpponent) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        val mapWidth = maxWidth
        val mapHeight = maxHeight

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000508))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f))
                .pointerInput(bots) {
                    detectTapGestures { tapOffset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val scale = (size.width.coerceAtMost(size.height) * 0.44f) / 4200f

                        // Find closest bot within tap tolerance
                        val clicked = bots.asSequence()
                            .filter { it.isAlive }
                            .map { b ->
                                val bx = centerX + (b.position.x * scale)
                                val by = centerY + (b.position.z * scale)
                                b to (Offset(bx, by) - tapOffset).getDistance()
                            }
                            .filter { it.second < 28f }
                            .minByOrNull { it.second }

                        if (clicked != null) {
                            onSelectBot(clicked.first)
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height / 2f)
            val maxRadarRadius = (width.coerceAtMost(height) * 0.44f)
            val scale = maxRadarRadius / 4200f // Sector boundary radius 4200m

            // Tactical Grid & Concentric Distance Rings
            drawCircle(color = Color(0xFF0A2233), radius = maxRadarRadius, center = center, style = Stroke(1.0f))
            drawCircle(color = Color(0xFF061825), radius = maxRadarRadius * 0.66f, center = center, style = Stroke(0.8f))
            drawCircle(color = Color(0xFF061825), radius = maxRadarRadius * 0.33f, center = center, style = Stroke(0.8f))

            // Crosshairs
            drawLine(color = Color(0xFF0A2233), start = Offset(center.x - maxRadarRadius, center.y), end = Offset(center.x + maxRadarRadius, center.y), strokeWidth = 1f)
            drawLine(color = Color(0xFF0A2233), start = Offset(center.x, center.y - maxRadarRadius), end = Offset(center.x, center.y + maxRadarRadius), strokeWidth = 1f)

            // Render all 100 AI Bots & Real-time Players
            for (bot in bots) {
                val bx = center.x + (bot.position.x * scale)
                val by = center.y + (bot.position.z * scale)

                if (!bot.isAlive) {
                    // Small faint tombstone cross
                    val crossSize = 3f
                    drawLine(color = Color(0xFF552222), start = Offset(bx - crossSize, by - crossSize), end = Offset(bx + crossSize, by + crossSize), strokeWidth = 1f)
                    drawLine(color = Color(0xFF552222), start = Offset(bx - crossSize, by + crossSize), end = Offset(bx + crossSize, by - crossSize), strokeWidth = 1f)
                    continue
                }

                val blipColor = bot.faction.color

                // Velocity vector trail
                val vx = bot.velocity.x * scale * 1.5f
                val vy = bot.velocity.z * scale * 1.5f
                drawLine(
                    color = blipColor.copy(alpha = 0.4f),
                    start = Offset(bx, by),
                    end = Offset(bx + vx, by + vy),
                    strokeWidth = 1.2f
                )

                // Laser Fire Vector
                if (bot.isFiringLaser && bot.currentTargetId != null) {
                    val target = bots.find { it.id == bot.currentTargetId && it.isAlive }
                    if (target != null) {
                        val tx = center.x + (target.position.x * scale)
                        val ty = center.y + (target.position.z * scale)
                        drawLine(
                            color = if (bot.faction == FleetFaction.THARGOID_SWARM) Color(0xFFFFCC00) else Color(0xFFFF2222),
                            start = Offset(bx, by),
                            end = Offset(tx, ty),
                            strokeWidth = 1.4f
                        )
                    }
                }

                // Blip Shape: Diamond for Real Players, Triangle/Circle for Bots
                if (bot.pilot.isRealtimeHumanPlayer) {
                    val path = Path().apply {
                        moveTo(bx, by - 5f)
                        lineTo(bx + 4f, by)
                        lineTo(bx, by + 5f)
                        lineTo(bx - 4f, by)
                        close()
                    }
                    drawPath(path = path, color = blipColor)
                } else {
                    drawCircle(
                        color = blipColor,
                        radius = if (bot.blueprint == com.example.elite.ships.ShipBlueprints.ANACONDA) 4.5f else 2.6f,
                        center = Offset(bx, by)
                    )
                }

                // Highlight selected bot
                if (bot == selectedBot) {
                    drawCircle(
                        color = Color(0xFFFFCC00),
                        radius = 8.5f,
                        center = Offset(bx, by),
                        style = Stroke(1.5f)
                    )
                    // Target line to current engagement
                    if (bot.currentTargetId != null) {
                        val t = bots.find { it.id == bot.currentTargetId && it.isAlive }
                        if (t != null) {
                            val tx = center.x + (t.position.x * scale)
                            val ty = center.y + (t.position.z * scale)
                            drawLine(color = Color(0xFFFFCC00).copy(alpha = 0.6f), start = Offset(bx, by), end = Offset(tx, ty), strokeWidth = 1.2f)
                        }
                    }
                }
            }
        }

        // Tactical HUD Overlay: Mini Real-time Killfeed (Bottom Left)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(Color(0xDD000810))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f))
                .padding(6.dp)
                .width(220.dp)
        ) {
            Text(
                text = "REALTIME COMBAT FEED",
                color = Color(0xFF00E5FF),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(3.dp))
            feed.take(4).forEach { msg ->
                Text(
                    text = msg,
                    color = Color.LightGray,
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }

        // Tactical Legend (Top Right)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color(0xDD000810))
                .border(1.dp, Color(0xFF1B2838))
                .padding(6.dp)
        ) {
            Text(text = "MAP LEGEND", color = Color(0xFFFFCC00), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(text = "◆ Realtime Human", color = Color(0xFF00E5FF), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            Text(text = "● Outlaw Pirate", color = Color(0xFFFF2222), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            Text(text = "● Galactic Navy", color = Color(0xFF00E5FF), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            Text(text = "● Merchant Convoy", color = Color(0xFF00FF66), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            Text(text = "● Alien Swarm", color = Color(0xFFFFCC00), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            Text(text = "✕ Destroyed Wreck", color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

/**
 * 100 AI Bots & Players Roster Table with filter, health, strategy, and kills.
 */
@Composable
private fun FleetRosterView(
    bots: List<TacticalBotOpponent>,
    selectedFaction: FleetFaction?,
    selectedBot: TacticalBotOpponent?,
    onFilterFaction: (FleetFaction?) -> Unit,
    onSelectBot: (TacticalBotOpponent) -> Unit
) {
    val filteredBots = if (selectedFaction == null) bots else bots.filter { it.faction == selectedFaction }

    Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        // Faction Filter Pills
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { onFilterFaction(null) },
                colors = ButtonDefaults.buttonColors(containerColor = if (selectedFaction == null) Color(0xFF005577) else Color(0xFF0E1620)),
                shape = RoundedCornerShape(2.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text("ALL (${bots.size})", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            FleetFaction.entries.forEach { f ->
                val isSel = selectedFaction == f
                val count = bots.count { it.faction == f }
                Button(
                    onClick = { onFilterFaction(if (isSel) null else f) },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSel) f.color.copy(alpha = 0.5f) else Color(0xFF0E1620)),
                    shape = RoundedCornerShape(2.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("${f.callsign} ($count)", color = if (isSel) Color.White else f.color, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Table Header
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF14202C)).padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("CALLSIGN / SHIP", color = Color(0xFF00E5FF), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f))
            Text("FACTION", color = Color(0xFF00E5FF), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("STRATEGY", color = Color(0xFF00E5FF), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
            Text("SHIELDS", color = Color(0xFF00E5FF), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f))
            Text("KILLS", color = Color(0xFF00E5FF), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f))
        }

        // Scrollable List of Up to 100 Bots
        LazyColumn(
            modifier = Modifier.fillMaxSize().border(1.dp, Color(0xFF1B2838)),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(filteredBots, key = { it.id }) { bot ->
                val isSelected = bot.id == selectedBot?.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) Color(0xFF283B4D) else if (bot.isAlive) Color(0xFF090F16) else Color(0xFF140707))
                        .clickable { onSelectBot(bot) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.8f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = bot.name,
                                color = if (bot.isAlive) Color.White else Color.Gray,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            if (bot.pilot.isRealtimeHumanPlayer) {
                                Text(
                                    text = "[P:${bot.pilot.pingMs}ms]",
                                    color = Color(0xFF00FF66),
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text(
                            text = "${bot.blueprint.name} | ${bot.pilot.rating.title}",
                            color = Color(0xFF88A0B0),
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = bot.faction.callsign,
                        color = bot.faction.color,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = bot.strategy.title.take(10),
                        color = Color(0xFFFFCC00),
                        fontSize = 7.5.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1.3f)
                    )

                    Column(modifier = Modifier.weight(1.1f)) {
                        LinearProgressIndicator(
                            progress = { (bot.shields / bot.maxShields).coerceIn(0f, 1f) },
                            color = if (bot.shields > 40f) Color(0xFF00FF66) else Color(0xFFFF2222),
                            trackColor = Color(0xFF1B2838),
                            modifier = Modifier.fillMaxWidth().height(4.dp)
                        )
                        Text(
                            text = if (bot.isAlive) "${bot.shields.toInt()}% S" else "DESTROYED",
                            color = if (bot.isAlive) Color.LightGray else Color(0xFFFF3333),
                            fontSize = 6.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "${bot.kills}",
                        color = if (bot.kills > 0) Color(0xFFFFCC00) else Color.Gray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Strategy Dispatcher: Issue tactical orders to individual craft or whole faction fleets.
 */
@Composable
private fun StrategyDispatchView(
    sectorEngine: SectorWarSimulationEngine,
    bots: List<TacticalBotOpponent>,
    selectedBot: TacticalBotOpponent?,
    soundSynth: BbcSoundSynth
) {
    var selectedOrderFaction by remember { mutableStateOf(FleetFaction.GALACTIC_NAVY) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .background(Color(0xFF090F16))
            .border(1.dp, Color(0xFF1B2838))
            .padding(8.dp)
    ) {
        Text(
            text = "FLEET STRATEGY COMMAND & DISPATCH",
            color = Color(0xFFFF2222),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Broadcast autonomous combat strategy directives across 100 AI bots & realtime wings.",
            color = Color(0xFF88A0B0),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Faction Selector for Global Wing Orders
        Text(text = "SELECT TARGET FLEET WING:", color = Color(0xFF00E5FF), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FleetFaction.entries.forEach { faction ->
                val isSel = selectedOrderFaction == faction
                Button(
                    onClick = {
                        selectedOrderFaction = faction
                        soundSynth.playBeep(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSel) faction.color else Color(0xFF14202C)),
                    shape = RoundedCornerShape(2.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(26.dp)
                ) {
                    Text(
                        text = faction.callsign,
                        color = if (isSel) Color.Black else faction.color,
                        fontSize = 7.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Strategy Directive Action Grid
        Text(text = "ASSIGN TACTICAL STRATEGY DIRECTIVE:", color = Color(0xFFFFCC00), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(BotStrategy.entries.toTypedArray()) { strategy ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF101B27))
                        .border(1.dp, Color(0xFF1E2E40))
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = strategy.title,
                                color = Color(0xFF00E5FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "AGGR: ${(strategy.aggression * 100).toInt()}% | SPD: ${"%.1f".format(strategy.preferredSpeedMult)}x",
                                color = Color(0xFFFFCC00),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = strategy.description,
                            color = Color(0xFF88A0B0),
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = {
                                sectorEngine.setStrategyForFaction(selectedOrderFaction, strategy)
                                soundSynth.playBeep(true)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005577)),
                            shape = RoundedCornerShape(2.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("ORDER WING", color = Color.White, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        if (selectedBot != null) {
                            Button(
                                onClick = {
                                    sectorEngine.setBotStrategy(selectedBot.id, strategy)
                                    soundSynth.playBeep(true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF553300)),
                                shape = RoundedCornerShape(2.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("ORDER UNIT", color = Color(0xFFFFCC00), fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedBotQuickBar(
    bot: TacticalBotOpponent,
    onStrategySelected: (BotStrategy) -> Unit,
    onDeselect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1824))
            .border(1.dp, Color(0xFFFFCC00).copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "TARGET LOCK: ${bot.name}",
                    color = Color(0xFFFFCC00),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "[${bot.faction.callsign}] ${bot.blueprint.name}",
                    color = bot.faction.color,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = "STRATEGY: ${bot.strategy.title} | SHIELDS: ${bot.shields.toInt()}% | HULL: ${bot.hull.toInt()}% | KILLS: ${bot.kills}",
                color = Color.LightGray,
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(
                onClick = { onStrategySelected(BotStrategy.AGGRESSIVE_INTERCEPT) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A1B1B)),
                shape = RoundedCornerShape(2.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier.height(22.dp)
            ) {
                Text("INTERCEPT", color = Color.White, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            }
            Button(
                onClick = { onStrategySelected(BotStrategy.DEFENSIVE_ESCORT) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E4D2A)),
                shape = RoundedCornerShape(2.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier.height(22.dp)
            ) {
                Text("ESCORT", color = Color.White, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            }
            Button(
                onClick = onDeselect,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF223344)),
                shape = RoundedCornerShape(2.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier.height(22.dp)
            ) {
                Text("✕", color = Color.White, fontSize = 8.sp)
            }
        }
    }
}
