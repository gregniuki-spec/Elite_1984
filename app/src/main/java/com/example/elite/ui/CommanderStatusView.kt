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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elite.model.CommanderState
import com.example.elite.model.SystemData

@Composable
fun CommanderStatusView(
    commander: CommanderState,
    currentSystem: SystemData,
    targetSystem: SystemData,
    isInventoryMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(10.dp)
            .testTag(if (isInventoryMode) "inventory_screen" else "commander_status_screen")
    ) {
        if (!isInventoryMode) {
            // Status Screen (f8)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "COMMANDER ${commander.name.uppercase()}",
                        color = BBC_YELLOW,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                item {
                    val statusItems = listOf(
                        Pair("PRESENT SYSTEM", currentSystem.name.uppercase()),
                        Pair("HYPERSPACE TARGET", targetSystem.name.uppercase()),
                        Pair("CONDITION", if (commander.isDocked) "DOCKED (GREEN)" else "FLYING (YELLOW)"),
                        Pair("FUEL", "${"%.1f".format(commander.fuelDeciLy / 10.0)} LIGHT YEARS"),
                        Pair("CASH", "${"%.1f".format(commander.cashDeciCredits / 10.0)} CR"),
                        Pair("LEGAL STATUS", commander.legalStatus.displayName.uppercase()),
                        Pair("RATING", commander.combatRank.displayName.uppercase()),
                        Pair("KILLS", "${commander.killCount}"),
                        Pair("LAST DIVIDENDS", "+${"%.1f".format(commander.lastDividendEarningsDeciCr / 10.0)} CR")
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF03090F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BBC_GREEN.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for ((k, v) in statusItems) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$k:",
                                        color = BBC_GREEN,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = v,
                                        color = if (k == "RATING" && v == "ELITE") BBC_YELLOW else BBC_WHITE,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Equipment Fitted
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF061019)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BBC_CYAN.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "EQUIPMENT FITTED:",
                                color = BBC_YELLOW,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            val eqList = mutableListOf<String>()
                            eqList.add("HULL REINFORCEMENT: LVL ${commander.hullReinforcementLevel} (${commander.currentHullIntegrity.toInt()}/${commander.maxHullIntegrity.toInt()} HP, ${commander.armorRatingPercent}% ARMOR)")
                            eqList.add("CARGO CAPACITY: ${commander.cargoCapacity}t (TIER ${commander.cargoCapacityTier})")
                            eqList.add("FRONT WEAPON: ${commander.laserFront.displayName.uppercase()}")
                            if (commander.laserRear != null) {
                                eqList.add("REAR TURRET: ${commander.laserRear?.displayName?.uppercase()}")
                            }
                            if (commander.weaponCoolingTier > 0) {
                                eqList.add("COOLING RADIATOR: TIER ${commander.weaponCoolingTier} (${"%.1f".format(commander.weaponCoolingMultiplier)}x RATE)")
                            }
                            eqList.add("MISSILE PYLONS: ${commander.missiles}/${commander.missileCapacity} ARMED")
                            if (commander.hasEcm) eqList.add("ECM SYSTEM")
                            if (commander.hasFuelScoops) eqList.add("FUEL SCOOPS")
                            if (commander.hasEscapePod) eqList.add("ESCAPE POD")
                            if (commander.hasEnergyBomb) eqList.add("ENERGY BOMB")
                            if (commander.hasDockingComputer) eqList.add("DOCKING COMPUTERS")
                            if (commander.hasGalacticHyperdrive) eqList.add("GALACTIC HYPERDRIVE")
                            if (commander.hasEnergyUnit) eqList.add("NAVAL ENERGY UNIT")

                            Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                for (eq in eqList) {
                                    Text(
                                        text = "- $eq",
                                        color = BBC_CYAN,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Colonial Outpost Empire
                item {
                    val totalDividends = commander.investments.sumOf { it.currentDividendCredits() }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF07190F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BBC_GREEN.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "COLONY OUTPOST HOLDINGS (${commander.investments.size})",
                                    color = BBC_GREEN,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "+$totalDividends CR / JUMP",
                                    color = BBC_YELLOW,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (commander.investments.isEmpty()) {
                                Text(
                                    text = "No outposts constructed. Tap 3D STRATEGY in the Galactic Chart to build automated mining rigs and trade relays!",
                                    color = Color(0xFFAABBCC),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else {
                                Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    for (inv in commander.investments) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "• ${inv.systemName}: ${inv.type.title} (Lvl ${inv.level})",
                                                color = BBC_WHITE,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "+${inv.currentDividendCredits()} CR",
                                                color = BBC_CYAN,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Strategic Contracts
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1720)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BBC_CYAN.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "ACTIVE STRATEGIC CONTRACTS (${commander.activeContracts.size}/5)",
                                color = BBC_YELLOW,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            if (commander.activeContracts.isEmpty()) {
                                Text(
                                    text = "No contracts active. Review local missions on the 3D System Strategy board.",
                                    color = Color(0xFFAABBCC),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else {
                                Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (c in commander.activeContracts) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "• ${c.title} -> ${c.targetSystemName}",
                                                color = if (c.isCompleted) BBC_GREEN else BBC_WHITE,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = if (c.isCompleted) "DONE (+${c.rewardCredits} CR)" else "${c.rewardCredits} CR",
                                                color = if (c.isCompleted) BBC_GREEN else BBC_YELLOW,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Inventory Screen (f9)
            Text(
                text = "INVENTORY / CARGO MANIFEST",
                color = BBC_YELLOW,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "CAPACITY: ${commander.cargoCapacity}t | USED: ${commander.currentCargoUsed()}t | FREE: ${commander.freeCargoSpace()}t",
                color = BBC_CYAN,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val holdEntries = commander.cargoHold.filter { it.value > 0 }.toList()

            if (holdEntries.isEmpty()) {
                Text(
                    text = "CARGO HOLD IS EMPTY.",
                    color = BBC_GREY,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 20.dp)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(holdEntries) { entry ->
                        val itemId = entry.first
                        val qty = entry.second
                        val itemNames = listOf(
                            "Food", "Textiles", "Radioactives", "Slaves",
                            "Liquor/Wines", "Luxuries", "Narcotics", "Computers",
                            "Machinery", "Alloys", "Firearms", "Furs",
                            "Minerals", "Gold", "Platinum", "Gem-Stones", "Alien Items"
                        )
                        val units = if (itemId == 13 || itemId == 14) "kg" else if (itemId == 15) "g" else "t"
                        val name = itemNames.getOrElse(itemId) { "Item $itemId" }

                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF0A150D)).padding(6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, color = BBC_GREEN, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("$qty $units", color = BBC_WHITE, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}
