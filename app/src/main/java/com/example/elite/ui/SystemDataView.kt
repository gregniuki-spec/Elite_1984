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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elite.audio.BbcSoundSynth
import com.example.elite.math3d.WireframeRenderer
import com.example.elite.model.CommanderState
import com.example.elite.model.InvestmentType
import com.example.elite.model.SystemData
import com.example.elite.ships.PlanetWireframeFactory

@Composable
fun SystemDataView(
    system: SystemData,
    commander: CommanderState? = null,
    soundSynth: BbcSoundSynth? = null,
    onOpen3dView: (() -> Unit)? = null,
    onInvest: ((InvestmentType) -> Unit)? = null,
    onHyperspaceJump: (() -> Unit)? = null,
    hasFuel: Boolean = true,
    distDeciLy: Int = 0,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "planet_data_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    val planetBlueprint = remember(system.planetType) {
        PlanetWireframeFactory.createPlanetBlueprint(system.planetType, 160f)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(10.dp)
            .testTag("system_data_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- Header with 3D Mini Wireframe Visualizer ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF041018)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, system.planetType.wireColor.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DATA ON ${system.name.uppercase()}",
                            color = BBC_YELLOW,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "[${system.planetType.displayName.uppercase()}]",
                            color = system.planetType.wireColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "STAR: ${system.starType.displayName} (${system.starType.spectralClass})",
                            color = system.starType.stellarColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "FACTION: ${system.faction.displayName}",
                            color = system.faction.color,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Interactive Rotating Mini 3D Wireframe Planet
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFF02070C), RoundedCornerShape(4.dp))
                            .border(1.dp, system.planetType.wireColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            WireframeRenderer.renderBlueprintAt(
                                drawScope = this,
                                blueprint = planetBlueprint,
                                rotX = 0.3f,
                                rotY = spinAngle,
                                rotZ = 0f,
                                center = center,
                                viewScale = 0.22f,
                                wireColor = system.planetType.wireColor,
                                strokeWidth = 1.3f
                            )
                        }
                    }
                }
            }
        }

        // --- Core Planetary Statistics ---
        item {
            val stats = listOf(
                Pair("ECONOMY", system.economyName),
                Pair("GOVERNMENT", system.governmentName),
                Pair("TECH LEVEL", "${system.techLevel}"),
                Pair("POPULATION", "${"%.1f".format(system.population)} BILLION"),
                Pair("GROSS PRODUCTIVITY", "${system.productivity} M CR"),
                Pair("AVERAGE RADIUS", "${system.radius} KM"),
                Pair("SPECIES", system.species.uppercase()),
                Pair("ATMOSPHERE", system.planetType.atmosphere),
                Pair("SURFACE TEMP", "${system.planetType.surfaceTempC} °C"),
                Pair("GRAVITY", "${system.planetType.gravityG} G"),
                Pair("PRIMARY EXPORT", system.planetType.primaryResource),
                Pair("SECURITY LEVEL", system.securityLevel.title)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF03090F)),
                border = androidx.compose.foundation.BorderStroke(1.dp, BBC_GREEN.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for ((label, value) in stats) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$label:",
                                color = BBC_GREEN,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = value,
                                color = BBC_WHITE,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // --- Colonial Holdings in this System ---
        if (commander != null) {
            item {
                val sysInvestments = commander.investments.filter { it.systemId == system.id }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF06180E)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BBC_GREEN.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "COLONIAL OUTPOST HOLDINGS: ${sysInvestments.size} ACTIVE",
                            color = BBC_GREEN,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (sysInvestments.isEmpty()) {
                            Text(
                                text = "No autonomous outposts constructed in ${system.name}. Build Mining Rigs or Trade Relays to generate jump revenue!",
                                color = Color(0xFFAABBCC),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            for (inv in sysInvestments) {
                                Text(
                                    text = "• ${inv.type.title} (Lvl ${inv.level}) - Yield: +${inv.currentDividendCredits()} CR / Jump",
                                    color = BBC_YELLOW,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Action Buttons ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onOpen3dView != null) {
                    Button(
                        onClick = {
                            soundSynth?.playBeep(true)
                            onOpen3dView()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152A3C)),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("3D STRATEGY SUITE", color = BBC_CYAN, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (onHyperspaceJump != null && distDeciLy > 0) {
                    Button(
                        onClick = {
                            if (hasFuel) onHyperspaceJump()
                        },
                        enabled = hasFuel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF005518),
                            disabledContainerColor = Color(0xFF1E2822)
                        ),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text(
                            text = if (hasFuel) "HYPERSPACE JUMP" else "NO FUEL",
                            color = if (hasFuel) BBC_WHITE else Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- BBC Micro 6502 Seeds Debug ---
        item {
            Text(
                text = "6502 SEEDS (QQ15): &${Integer.toHexString(system.seed0).uppercase()} &${Integer.toHexString(system.seed1).uppercase()} &${Integer.toHexString(system.seed2).uppercase()}",
                color = BBC_CYAN.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
