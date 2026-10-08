package com.example.elite.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elite.audio.BbcSoundSynth
import com.example.elite.market.EconomyCondition
import com.example.elite.market.data.MarketPriceHistoryEntity
import com.example.elite.market.data.SystemCommodityEntity
import com.example.elite.market.data.SystemEconomyEntity
import com.example.elite.model.Commodity
import com.example.elite.model.CommanderState
import com.example.elite.model.SystemData

@Composable
fun MarketTradingView(
    commander: CommanderState,
    currentSystem: SystemData,
    marketGoods: List<Commodity>,
    commodityEntities: List<SystemCommodityEntity>,
    economyState: SystemEconomyEntity?,
    priceHistory: List<MarketPriceHistoryEntity>,
    selectedCommodityId: Int?,
    soundSynth: BbcSoundSynth,
    onBuy: (Commodity) -> Unit,
    onSell: (Commodity) -> Unit,
    onSelectCommodityForHistory: (Int?) -> Unit,
    onTriggerFluctuation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entityMap = remember(commodityEntities) {
        commodityEntities.associateBy { it.commodityId }
    }

    val selectedCommodity = remember(selectedCommodityId, marketGoods) {
        marketGoods.find { it.id == selectedCommodityId }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(8.dp)
            .testTag("market_trading_screen")
    ) {
        // System & Market Economy Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${currentSystem.name.uppercase()} MARKET PRICES",
                    color = BBC_YELLOW,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "ECONOMY: ${currentSystem.economyName.uppercase()} (TL: ${currentSystem.techLevel})",
                    color = BBC_CYAN,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Macro-economic Condition Pill & Fluctuate Cycle button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val boomCode = economyState?.activeBoomState ?: "NORMAL"
                val boomColor = when (boomCode) {
                    "BOOM" -> BBC_GREEN
                    "WAR_MOBILIZATION" -> BBC_RED
                    "DROUGHT" -> BBC_YELLOW
                    "MINERAL_RUSH" -> BBC_CYAN
                    "RECESSION" -> Color(0xFFFF55AA)
                    "PIRATE_BLOCKADE" -> Color(0xFFFF5555)
                    else -> BBC_WHITE
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFF15221B), RoundedCornerShape(4.dp))
                        .border(1.dp, boomColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = boomCode.replace("_", " "),
                        color = boomColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        soundSynth.playBeep(true)
                        onTriggerFluctuation()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF223322)),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier
                        .height(24.dp)
                        .testTag("fluctuate_cycle_button")
                ) {
                    Text("CYCLE", fontSize = 9.sp, color = BBC_GREEN, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Economy Description Banner
        if (economyState != null) {
            Text(
                text = "${economyState.boomDescription} | Cycle #${economyState.marketCycle} [Room Persisted]",
                color = BBC_GREY,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        // Status Row: Cash & Hold space
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CASH: ${"%.1f".format(commander.cashDeciCredits / 10.0)} CR",
                color = BBC_GREEN,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "HOLD: ${commander.currentCargoUsed()}/${commander.cargoCapacity}t FREE: ${commander.freeCargoSpace()}t",
                color = BBC_CYAN,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A1A10))
                .padding(vertical = 4.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PRODUCT", color = BBC_WHITE, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.8f))
            Text("UNIT", color = BBC_WHITE, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.6f))
            Text("PRICE (CR)", color = BBC_WHITE, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.3f))
            Text("DIFF", color = BBC_WHITE, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.9f))
            Text("QTY", color = BBC_WHITE, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.7f))
            Text("HOLD", color = BBC_WHITE, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.7f))
            Text("TRADE", color = BBC_WHITE, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.8f))
        }

        // Table List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(marketGoods) { item ->
                val inHold = commander.cargoHold[item.id] ?: 0
                val priceFormatted = "%.1f".format(item.price / 10.0)
                val entity = entityMap[item.id]

                // Price difference calculation based on previous Room fluctuation
                val priceDiff = if (entity != null && entity.previousPriceDeciCr > 0) {
                    (entity.currentPriceDeciCr - entity.previousPriceDeciCr) / 10.0
                } else 0.0

                val isSelected = item.id == selectedCommodityId
                val rowBg = if (isSelected) Color(0xFF142E1B) else Color(0xFF040B07)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .clickable {
                            if (isSelected) {
                                onSelectCommodityForHistory(null)
                            } else {
                                soundSynth.playBeep(true)
                                onSelectCommodityForHistory(item.id)
                            }
                        }
                        .padding(vertical = 3.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Commodity Name & History indicator
                    Text(
                        item.name,
                        color = if (isSelected) BBC_YELLOW else BBC_GREEN,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1.8f)
                    )
                    Text(
                        item.units,
                        color = BBC_CYAN,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.6f)
                    )
                    Text(
                        priceFormatted,
                        color = BBC_YELLOW,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1.3f)
                    )

                    // Price Fluctuation Trend indicator
                    val diffText = if (priceDiff > 0) {
                        "+%.1f".format(priceDiff)
                    } else if (priceDiff < 0) {
                        "%.1f".format(priceDiff)
                    } else {
                        "0.0"
                    }
                    val diffColor = if (priceDiff > 0) BBC_GREEN else if (priceDiff < 0) BBC_RED else BBC_GREY

                    Text(
                        text = diffText,
                        color = diffColor,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.9f)
                    )

                    Text(
                        "${item.quantity}",
                        color = if (item.quantity > 0) BBC_WHITE else BBC_GREY,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.7f)
                    )
                    Text(
                        "$inHold",
                        color = if (inHold > 0) BBC_GREEN else BBC_GREY,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.7f)
                    )

                    // Buy and Sell Buttons
                    Row(
                        modifier = Modifier.weight(1.8f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val canBuy = item.quantity > 0 && commander.cashDeciCredits >= item.price &&
                                (item.units != "t" || commander.freeCargoSpace() >= 1)

                        // BUY
                        Button(
                            onClick = { onBuy(item) },
                            enabled = canBuy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF004411),
                                disabledContainerColor = Color(0xFF152018)
                            ),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier
                                .size(width = 42.dp, height = 24.dp)
                                .testTag("buy_button_${item.id}")
                        ) {
                            Text("BUY", fontSize = 8.sp, color = BBC_GREEN, fontWeight = FontWeight.Bold)
                        }

                        // SELL
                        Button(
                            onClick = { onSell(item) },
                            enabled = inHold > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF442200),
                                disabledContainerColor = Color(0xFF201815)
                            ),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier
                                .size(width = 42.dp, height = 24.dp)
                                .testTag("sell_button_${item.id}")
                        ) {
                            Text("SELL", fontSize = 8.sp, color = BBC_YELLOW, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Price History Sparkline & Trends Drawer
        if (selectedCommodity != null) {
            PriceHistorySection(
                commodity = selectedCommodity,
                history = priceHistory,
                onClose = { onSelectCommodityForHistory(null) }
            )
        }
    }
}

@Composable
fun PriceHistorySection(
    commodity: Commodity,
    history: List<MarketPriceHistoryEntity>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A140F)),
        shape = RoundedCornerShape(4.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BBC_GREEN))
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TREND: ${commodity.name.uppercase()} (ROOM DB HISTORY)",
                    color = BBC_YELLOW,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "[CLOSE X]",
                    color = BBC_GREY,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { onClose() }
                )
            }

            if (history.isEmpty()) {
                Text(
                    text = "Awaiting trade cycle updates to render price chart...",
                    color = BBC_GREY,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val prices = history.map { it.priceDeciCr / 10f }
                    val minPrice = prices.minOrNull() ?: 0f
                    val maxPrice = prices.maxOrNull() ?: 1f
                    val currentP = prices.lastOrNull() ?: 0f

                    Text("LOW: ${"%.1f".format(minPrice)} CR", color = BBC_CYAN, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("CURRENT: ${"%.1f".format(currentP)} CR", color = BBC_YELLOW, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("HIGH: ${"%.1f".format(maxPrice)} CR", color = BBC_GREEN, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }

                // Vector Canvas Chart
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(vertical = 4.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val prices = history.map { it.priceDeciCr / 10f }
                    if (prices.size < 2) {
                        drawLine(
                            color = BBC_GREEN,
                            start = Offset(0f, h / 2),
                            end = Offset(w, h / 2),
                            strokeWidth = 2f
                        )
                        return@Canvas
                    }

                    val minP = prices.minOrNull() ?: 0f
                    val maxP = (prices.maxOrNull() ?: (minP + 1f)).let { if (it == minP) it + 1f else it }
                    val range = maxP - minP

                    val path = Path()
                    val stepX = w / (prices.size - 1)

                    prices.forEachIndexed { i, p ->
                        val x = i * stepX
                        val normalized = (p - minP) / range
                        val y = h - (normalized * (h - 8f)) - 4f
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = BBC_GREEN,
                        style = Stroke(width = 2.5f)
                    )
                }
            }
        }
    }
}
