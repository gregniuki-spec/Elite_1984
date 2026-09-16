package com.example.elite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elite.audio.BbcSoundSynth
import com.example.elite.model.CommanderState
import com.example.elite.model.LaserType
import com.example.elite.model.SystemData

data class EquipmentItem(
    val id: String,
    val name: String,
    val priceDeciCredits: Long,
    val minTech: Int,
    val isOwned: (CommanderState) -> Boolean,
    val canAfford: (CommanderState) -> Boolean,
    val buyAction: (CommanderState) -> Unit
)

@Composable
fun ShipyardView(
    commander: CommanderState,
    currentSystem: SystemData,
    soundSynth: BbcSoundSynth,
    onEquipChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val equipmentCatalog = listOf(
        EquipmentItem(
            id = "fuel",
            name = "Refuel (Full 7.0 LY)",
            priceDeciCredits = ((70 - commander.fuelDeciLy) * 1L).coerceAtLeast(0L),
            minTech = 1,
            isOwned = { it.fuelDeciLy >= 70 },
            canAfford = { it.cashDeciCredits >= ((70 - it.fuelDeciLy) * 1L) },
            buyAction = {
                val cost = (70 - it.fuelDeciLy) * 1L
                it.cashDeciCredits -= cost
                it.fuelDeciLy = 70
            }
        ),
        EquipmentItem(
            id = "missile",
            name = "Missile (Arm 1x)",
            priceDeciCredits = 300L,
            minTech = 2,
            isOwned = { it.missiles >= 4 },
            canAfford = { it.cashDeciCredits >= 300L },
            buyAction = {
                it.cashDeciCredits -= 300L
                it.missiles++
            }
        ),
        EquipmentItem(
            id = "large_bay",
            name = "Large Cargo Bay (30t)",
            priceDeciCredits = 4000L,
            minTech = 3,
            isOwned = { it.cargoCapacity >= 30 },
            canAfford = { it.cashDeciCredits >= 4000L },
            buyAction = {
                it.cashDeciCredits -= 4000L
                it.cargoCapacity = 30
            }
        ),
        EquipmentItem(
            id = "beam_laser",
            name = "Beam Laser (Front)",
            priceDeciCredits = 10000L,
            minTech = 4,
            isOwned = { it.laserFront == LaserType.BEAM },
            canAfford = { it.cashDeciCredits >= 10000L },
            buyAction = {
                it.cashDeciCredits -= 10000L
                it.laserFront = LaserType.BEAM
            }
        ),
        EquipmentItem(
            id = "military_laser",
            name = "Military Laser (Front)",
            priceDeciCredits = 60000L,
            minTech = 10,
            isOwned = { it.laserFront == LaserType.MILITARY },
            canAfford = { it.cashDeciCredits >= 60000L },
            buyAction = {
                it.cashDeciCredits -= 60000L
                it.laserFront = LaserType.MILITARY
            }
        ),
        EquipmentItem(
            id = "ecm",
            name = "E.C.M. System",
            priceDeciCredits = 6000L,
            minTech = 2,
            isOwned = { it.hasEcm },
            canAfford = { it.cashDeciCredits >= 6000L },
            buyAction = {
                it.cashDeciCredits -= 6000L
                it.hasEcm = true
            }
        ),
        EquipmentItem(
            id = "scoops",
            name = "Fuel Scoops",
            priceDeciCredits = 5250L,
            minTech = 5,
            isOwned = { it.hasFuelScoops },
            canAfford = { it.cashDeciCredits >= 5250L },
            buyAction = {
                it.cashDeciCredits -= 5250L
                it.hasFuelScoops = true
            }
        ),
        EquipmentItem(
            id = "docking_comp",
            name = "Docking Computers",
            priceDeciCredits = 15000L,
            minTech = 9,
            isOwned = { it.hasDockingComputer },
            canAfford = { it.cashDeciCredits >= 15000L },
            buyAction = {
                it.cashDeciCredits -= 15000L
                it.hasDockingComputer = true
            }
        ),
        EquipmentItem(
            id = "energy_bomb",
            name = "Energy Bomb",
            priceDeciCredits = 9000L,
            minTech = 7,
            isOwned = { it.hasEnergyBomb },
            canAfford = { it.cashDeciCredits >= 9000L },
            buyAction = {
                it.cashDeciCredits -= 9000L
                it.hasEnergyBomb = true
            }
        ),
        EquipmentItem(
            id = "galactic_hyper",
            name = "Galactic Hyperdrive",
            priceDeciCredits = 50000L,
            minTech = 10,
            isOwned = { it.hasGalacticHyperdrive },
            canAfford = { it.cashDeciCredits >= 50000L },
            buyAction = {
                it.cashDeciCredits -= 50000L
                it.hasGalacticHyperdrive = true
            }
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(10.dp)
            .testTag("shipyard_outfitting_screen")
    ) {
        Text(
            text = "EQUIP SHIP - ${currentSystem.name.uppercase()} (TECH: ${currentSystem.techLevel})",
            color = BBC_YELLOW,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Text(
            text = "CASH: ${"%.1f".format(commander.cashDeciCredits / 10.0)} CR",
            color = BBC_GREEN,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(equipmentCatalog) { item ->
                val owned = item.isOwned(commander)
                val techOk = currentSystem.techLevel >= item.minTech
                val affordable = item.canAfford(commander)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF061109))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            color = if (owned) BBC_CYAN else if (!techOk) BBC_GREY else BBC_WHITE,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "TECH: ${item.minTech}  |  PRICE: ${"%.1f".format(item.priceDeciCredits / 10.0)} CR",
                            color = BBC_YELLOW,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = {
                            item.buyAction(commander)
                            soundSynth.playBeep(true)
                            onEquipChanged()
                        },
                        enabled = !owned && techOk && affordable,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF005518),
                            disabledContainerColor = Color(0xFF142018)
                        ),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.size(width = 72.dp, height = 30.dp)
                    ) {
                        Text(
                            text = if (owned) "FITTED" else if (!techOk) "LOW TECH" else "BUY",
                            color = if (!owned && techOk && affordable) BBC_GREEN else BBC_GREY,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
