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
import com.example.elite.model.Commodity
import com.example.elite.model.CommanderState
import com.example.elite.model.SystemData

@Composable
fun MarketTradingView(
    commander: CommanderState,
    currentSystem: SystemData,
    marketGoods: List<Commodity>,
    soundSynth: BbcSoundSynth,
    onMarketUpdated: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(8.dp)
            .testTag("market_trading_screen")
    ) {
        // Header
        Text(
            text = "${currentSystem.name.uppercase()} MARKET PRICES",
            color = BBC_YELLOW,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Status Row: Cash & Hold space
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CASH: ${"%.1f".format(commander.cashDeciCredits / 10.0)} CR",
                color = BBC_GREEN,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "HOLD: ${commander.currentCargoUsed()}/${commander.cargoCapacity}t FREE: ${commander.freeCargoSpace()}t",
                color = BBC_CYAN,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A1A10))
                .padding(vertical = 4.dp, horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("PRODUCT", color = BBC_WHITE, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.8f))
            Text("UNIT", color = BBC_WHITE, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.7f))
            Text("PRICE", color = BBC_WHITE, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.1f))
            Text("QTY", color = BBC_WHITE, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.8f))
            Text("HOLD", color = BBC_WHITE, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.8f))
            Text("TRADE", color = BBC_WHITE, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.8f))
        }

        // Table List
        LazyColumn(
            modifier = Modifier.weight(1f).padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(marketGoods) { item ->
                val inHold = commander.cargoHold[item.id] ?: 0
                val priceFormatted = "%.1f".format(item.price / 10.0)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF040B07))
                        .padding(vertical = 3.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.name,
                        color = BBC_GREEN,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1.8f)
                    )
                    Text(
                        item.units,
                        color = BBC_CYAN,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.7f)
                    )
                    Text(
                        priceFormatted,
                        color = BBC_YELLOW,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        "${item.quantity}",
                        color = if (item.quantity > 0) BBC_WHITE else BBC_GREY,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(
                        "$inHold",
                        color = if (inHold > 0) BBC_GREEN else BBC_GREY,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.8f)
                    )

                    // Buy and Sell Buttons
                    Row(
                        modifier = Modifier.weight(1.8f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // BUY
                        Button(
                            onClick = {
                                if (item.quantity > 0 && commander.cashDeciCredits >= item.price &&
                                    (item.units != "t" || commander.freeCargoSpace() >= 1)
                                ) {
                                    commander.cashDeciCredits -= item.price
                                    commander.cargoHold[item.id] = inHold + 1
                                    item.quantity--
                                    soundSynth.playBeep(true)
                                    onMarketUpdated()
                                } else {
                                    soundSynth.playBeep(false)
                                }
                            },
                            enabled = item.quantity > 0 && commander.cashDeciCredits >= item.price && (item.units != "t" || commander.freeCargoSpace() >= 1),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF004411),
                                disabledContainerColor = Color(0xFF152018)
                            ),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.size(width = 44.dp, height = 26.dp)
                        ) {
                            Text("BUY", fontSize = 8.sp, color = BBC_GREEN, fontWeight = FontWeight.Bold)
                        }

                        // SELL
                        Button(
                            onClick = {
                                if (inHold > 0) {
                                    commander.cashDeciCredits += item.price
                                    commander.cargoHold[item.id] = inHold - 1
                                    item.quantity++
                                    soundSynth.playBeep(true)
                                    onMarketUpdated()
                                } else {
                                    soundSynth.playBeep(false)
                                }
                            },
                            enabled = inHold > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF442200),
                                disabledContainerColor = Color(0xFF201815)
                            ),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.size(width = 44.dp, height = 26.dp)
                        ) {
                            Text("SELL", fontSize = 8.sp, color = BBC_YELLOW, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
