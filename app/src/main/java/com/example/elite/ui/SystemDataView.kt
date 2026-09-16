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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elite.model.SystemData

@Composable
fun SystemDataView(
    system: SystemData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(14.dp)
            .testTag("system_data_screen")
    ) {
        Text(
            text = "DATA ON ${system.name.uppercase()}",
            color = BBC_YELLOW,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        val fields = listOf(
            Pair("ECONOMY", system.economyName),
            Pair("GOVERNMENT", system.governmentName),
            Pair("TECH LEVEL", "${system.techLevel}"),
            Pair("POPULATION", "${"%.1f".format(system.population)} BILLION"),
            Pair("GROSS PRODUCTIVITY", "${system.productivity} M CR"),
            Pair("AVERAGE RADIUS", "${system.radius} KM"),
            Pair("SPECIES", system.species.uppercase())
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for ((label, value) in fields) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$label:",
                        color = BBC_GREEN,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = value,
                        color = BBC_WHITE,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // System seeds debug from BBC Micro 6502
        Text(
            text = "6502 SEEDS (QQ15): &${Integer.toHexString(system.seed0).uppercase()}  &${Integer.toHexString(system.seed1).uppercase()}  &${Integer.toHexString(system.seed2).uppercase()}",
            color = BBC_CYAN.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
