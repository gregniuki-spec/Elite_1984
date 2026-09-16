package com.example.elite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elite.model.GameScreen

data class RedKeyItem(
    val screen: GameScreen,
    val keyLabel: String,
    val title: String
)

@Composable
fun RedKeyNavigationBar(
    currentScreen: GameScreen,
    onScreenSelected: (GameScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        RedKeyItem(GameScreen.SPACE_FLIGHT, "f0", "SPACE"),
        RedKeyItem(GameScreen.FLIGHT_MANUAL, "f1", "MANUAL"),
        RedKeyItem(GameScreen.SHIP_ENCYCLOPEDIA, "f2", "SHIPS"),
        RedKeyItem(GameScreen.MARKET_PRICES, "f7", "MARKET"),
        RedKeyItem(GameScreen.GALACTIC_CHART, "f4", "GALAXY"),
        RedKeyItem(GameScreen.SHORT_RANGE_CHART, "f5", "LOCAL"),
        RedKeyItem(GameScreen.SYSTEM_DATA, "f6", "DATA"),
        RedKeyItem(GameScreen.STATUS, "f8", "STATUS"),
        RedKeyItem(GameScreen.INVENTORY, "f9", "HOLD"),
        RedKeyItem(GameScreen.EQUIP_SHIP, "f3", "EQUIP"),
        RedKeyItem(GameScreen.ASM_INSPECTOR, "6502", "SOURCE")
    )

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF140202))
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .testTag("red_key_navigation_bar"),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        items(keys) { item ->
            val isSelected = currentScreen == item.screen
            // BBC Micro Red Function Key styling
            val bgCol = if (isSelected) Color(0xFFCC1111) else Color(0xFF6B0E0E)

            Button(
                onClick = { onScreenSelected(item.screen) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = bgCol
                ),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier
                    .height(30.dp)
                    .testTag("key_${item.keyLabel.lowercase()}")
            ) {
                Text(
                    text = "${item.keyLabel}: ${item.title}",
                    color = if (isSelected) BBC_WHITE else Color(0xFFFFCCCC),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
