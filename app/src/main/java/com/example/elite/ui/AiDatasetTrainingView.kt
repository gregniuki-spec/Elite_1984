package com.example.elite.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.elite.dataset.model.DatasetSessionEntity
import com.example.elite.dataset.service.GameplayDatasetCollector
import com.example.elite.flight.FlightEngine
import com.example.elite.model.CommanderState

@Composable
fun AiDatasetTrainingView(
    datasetCollector: GameplayDatasetCollector,
    flightEngine: FlightEngine,
    commander: CommanderState,
    onSignInWithGoogleClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording by datasetCollector.isRecordingActive.collectAsStateWithLifecycle()
    val sessions by datasetCollector.recentSessions.collectAsStateWithLifecycle()
    val statusLog by datasetCollector.statusLog.collectAsStateWithLifecycle()
    val currentUserId = datasetCollector.currentUserId

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040609))
            .padding(12.dp)
            .testTag("ai_dataset_training_view")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "--- MULTI-PILOT AI STRATEGY DATASET REPOSITORY ---",
                    color = BBC_YELLOW,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "COLLECTING REALTIME DOGFIGHTS, AUTO-PLAY & TRADE TELEMETRY FOR BOT BRAIN TRAINING",
                    color = BBC_CYAN,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (currentUserId == null) {
                Button(
                    onClick = onSignInWithGoogleClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.testTag("google_signin_button")
                ) {
                    Text("SIGN IN WITH GOOGLE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PILOT ID: ${currentUserId.take(8)}...",
                        color = BBC_GREEN,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Button(
                        onClick = onSignOutClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF551111)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.testTag("signout_button")
                    ) {
                        Text("SIGN OUT", color = Color.White, fontSize = 9.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B141E)),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (isRecording) BBC_GREEN else Color(0xFFFF4444), shape = RoundedCornerShape(5.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRecording) "LIVE HARVEST ACTIVE" else "HARVEST PAUSED",
                            color = if (isRecording) BBC_GREEN else Color(0xFFFF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row {
                        Button(
                            onClick = {
                                flightEngine.autoPlayMode = com.example.elite.flight.AutoPlayMode.TRAINED_NEURAL_BOT
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier.padding(end = 6.dp).testTag("deploy_trained_ai_button")
                        ) {
                            Text(
                                text = if (flightEngine.autoPlayMode == com.example.elite.flight.AutoPlayMode.TRAINED_NEURAL_BOT) "AI DEPLOYED (ACTIVE)" else "DEPLOY AI TO FLIGHT",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { datasetCollector.toggleRecording() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) Color(0xFF664400) else Color(0xFF006622)
                            ),
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier.testTag("toggle_harvest_button")
                        ) {
                            Text(
                                text = if (isRecording) "PAUSE RECORDING" else "RESUME HARVEST",
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusLog,
                    color = BBC_WHITE,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Strategy Breakdown Metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StrategyStatCard(
                title = "TRAINING VECTORS",
                value = "${sessions.sumOf { it.sampleCount }} FRAMES",
                color = BBC_CYAN,
                modifier = Modifier.weight(1f)
            )
            StrategyStatCard(
                title = "AVG ACCURACY",
                value = "${if (sessions.isNotEmpty()) "%.1f".format(sessions.map { it.accuracyRate }.average()) else "0.0"}%",
                color = BBC_YELLOW,
                modifier = Modifier.weight(1f)
            )
            StrategyStatCard(
                title = "TOTAL DOGFIGHT KILLS",
                value = "${sessions.sumOf { it.totalKills }} HOSTILES",
                color = BBC_GREEN,
                modifier = Modifier.weight(1f)
            )
            StrategyStatCard(
                title = "AUTO-PLAY SAMPLES",
                value = if (flightEngine.isAutoPlayActive) "STREAMING" else "IDLE",
                color = if (flightEngine.isAutoPlayActive) BBC_GREEN else Color.Gray,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "--- CLOUD STRATEGY DATASET SESSIONS (${sessions.size} RECORDED) ---",
            color = BBC_WHITE,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E2D3D))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (currentUserId != null) {
                            "NO PREVIOUS DATASET SESSIONS FOUND FOR THIS PILOT.\nLAUNCH DOGFIGHT OR ENABLE AUTO-PLAY IN FLIGHT/WAR SIMULATION TO STREAM DATA."
                        } else {
                            "PLEASE SIGN IN WITH GOOGLE TO SYNC AND ACCUMULATE CLOUD DATASET REINFORCEMENT SESSIONS ACROSS PLAYERS."
                        },
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("dataset_sessions_list"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(sessions) { session ->
                    DatasetSessionRow(session)
                }
            }
        }
    }
}

@Composable
private fun StrategyStatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A24)),
        shape = RoundedCornerShape(3.dp)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(title, color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun DatasetSessionRow(session: DatasetSessionEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF09121B)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PILOT: ${session.pilotCallsign} | STRATEGY: ${session.bestStrategyArchetype}",
                    color = BBC_YELLOW,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "KILLS: ${session.totalKills} | ACCURACY: ${session.accuracyRate}% | SAMPLES: ${session.sampleCount} FRAMES",
                    color = BBC_CYAN,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "MISSILES: ${session.missilesFired} | ECM SAVES: ${session.ecmSuccessCount} | STATUS: ${session.status}",
                    color = Color.LightGray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .background(Color(0xFF003318), shape = RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text("READY TO TRAIN", color = BBC_GREEN, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
