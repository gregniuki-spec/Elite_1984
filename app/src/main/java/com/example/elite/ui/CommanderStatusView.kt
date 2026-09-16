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
            .padding(12.dp)
            .testTag(if (isInventoryMode) "inventory_screen" else "commander_status_screen")
    ) {
        if (!isInventoryMode) {
            // Status Screen (f8)
            Text(
                text = "COMMANDER ${commander.name.uppercase()}",
                color = BBC_YELLOW,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val statusItems = listOf(
                Pair("PRESENT SYSTEM", currentSystem.name.uppercase()),
                Pair("HYPERSPACE SYSTEM", targetSystem.name.uppercase()),
                Pair("CONDITION", if (commander.isDocked) "DOCKED (GREEN)" else "FLYING (YELLOW)"),
                Pair("FUEL", "${"%.1f".format(commander.fuelDeciLy / 10.0)} LIGHT YEARS"),
                Pair("CASH", "${"%.1f".format(commander.cashDeciCredits / 10.0)} CR"),
                Pair("LEGAL STATUS", commander.legalStatus.displayName.uppercase()),
                Pair("RATING", commander.combatRank.displayName.uppercase()),
                Pair("KILLS", "${commander.killCount}")
            )

            for ((k, v) in statusItems) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$k:",
                        color = BBC_GREEN,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = v,
                        color = if (k == "RATING" && v == "ELITE") BBC_YELLOW else BBC_WHITE,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "EQUIPMENT FITTED:",
                color = BBC_YELLOW,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            val eqList = mutableListOf<String>()
            eqList.add("FRONT LASER: ${commander.laserFront.displayName.uppercase()}")
            if (commander.cargoCapacity > 20) eqList.add("LARGE CARGO BAY (${commander.cargoCapacity}t)")
            if (commander.hasEcm) eqList.add("ECM SYSTEM")
            if (commander.hasFuelScoops) eqList.add("FUEL SCOOPS")
            if (commander.hasEscapePod) eqList.add("ESCAPE POD")
            if (commander.hasEnergyBomb) eqList.add("ENERGY BOMB")
            if (commander.hasDockingComputer) eqList.add("DOCKING COMPUTERS")
            if (commander.hasGalacticHyperdrive) eqList.add("GALACTIC HYPERDRIVE")
            eqList.add("${commander.missiles} MISSILE${if (commander.missiles == 1) "" else "S"}")

            Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (eq in eqList) {
                    Text(
                        text = "- $eq",
                        color = BBC_CYAN,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
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
