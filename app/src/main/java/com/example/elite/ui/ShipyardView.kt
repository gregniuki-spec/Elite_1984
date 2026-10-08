package com.example.elite.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elite.audio.BbcSoundSynth
import com.example.elite.model.CommanderState
import com.example.elite.model.SystemData
import com.example.elite.ships.data.ShipUpgradeCatalog
import com.example.elite.ships.data.ShipUpgradeEntity

enum class ShipyardTab(val label: String) {
    HULL("HULL"),
    CARGO("CARGO"),
    WEAPONS("WEAPONS"),
    AVIONICS("AVIONICS")
}

@Composable
fun ShipyardView(
    commander: CommanderState,
    currentSystem: SystemData,
    soundSynth: BbcSoundSynth,
    shipUpgrade: ShipUpgradeEntity? = null,
    onPurchaseHull: (Int) -> Unit = {},
    onRepairHull: () -> Unit = {},
    onPurchaseCargo: (Int) -> Unit = {},
    onPurchaseFrontWeapon: (String) -> Unit = {},
    onPurchaseRearWeapon: (String) -> Unit = {},
    onPurchaseCooling: (Int) -> Unit = {},
    onPurchaseMissilePylons: (Int) -> Unit = {},
    onArmMissile: () -> Unit = {},
    onPurchaseEquipment: (String, Long, Int) -> Unit = { _, _, _ -> },
    onRefuel: () -> Unit = {},
    onEquipChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(ShipyardTab.HULL) }

    val hullLevel = shipUpgrade?.hullReinforcementLevel ?: commander.hullReinforcementLevel
    val currentHull = shipUpgrade?.currentHullIntegrity ?: commander.currentHullIntegrity
    val maxHull = shipUpgrade?.maxHullIntegrity ?: commander.maxHullIntegrity
    val armorRating = shipUpgrade?.armorRatingPercent ?: commander.armorRatingPercent

    val cargoTier = shipUpgrade?.cargoCapacityTier ?: commander.cargoCapacityTier
    val cargoTonnes = shipUpgrade?.cargoHoldMaxTonnes ?: commander.cargoCapacity

    val frontWeapon = shipUpgrade?.frontWeapon ?: commander.laserFront.name
    val rearWeapon = shipUpgrade?.rearWeapon ?: commander.laserRear?.name
    val coolingTier = shipUpgrade?.weaponCoolingTier ?: commander.weaponCoolingTier
    val missilePylons = shipUpgrade?.missileCapacity ?: commander.missileCapacity
    val missilesArmed = shipUpgrade?.missilesArmed ?: commander.missiles

    val repairCostDeci = ShipUpgradeCatalog.calculateRepairCostDeciCr(currentHull, maxHull)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(8.dp)
            .testTag("shipyard_outfitting_screen")
    ) {
        // --- HEADER BAR ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CORIOLIS SHIPYARD - ${currentSystem.name.uppercase()}",
                    color = BBC_YELLOW,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "TECH LEVEL: ${currentSystem.techLevel}  |  SHIP: COBRA MK III",
                    color = BBC_CYAN,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF051C0C)),
                border = androidx.compose.foundation.BorderStroke(1.dp, BBC_GREEN),
                shape = RoundedCornerShape(2.dp)
            ) {
                Text(
                    text = "CREDITS: ${"%.1f".format(commander.cashDeciCredits / 10.0)} CR",
                    color = BBC_GREEN,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- SPEC SUMMARY BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF061019))
                .border(1.dp, Color(0xFF1B3347))
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "HULL: ${currentHull.toInt()}/${maxHull.toInt()} HP",
                    color = if (currentHull < maxHull) BBC_YELLOW else BBC_WHITE,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ARMOR: $armorRating% DEFLECTION",
                    color = BBC_CYAN,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column {
                Text(
                    text = "CARGO: ${cargoTonnes}t CAPACITY",
                    color = BBC_WHITE,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "FREE: ${commander.freeCargoSpace()}t / ${commander.currentCargoUsed()}t USED",
                    color = BBC_GREEN,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column {
                Text(
                    text = "LASER: $frontWeapon",
                    color = BBC_WHITE,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "COOL: ${(1.0f + coolingTier * 0.5f)}x | MISSILES: $missilesArmed/$missilePylons",
                    color = BBC_YELLOW,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- CATEGORY TABS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (tab in ShipyardTab.values()) {
                val isSelected = activeTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isSelected) BBC_YELLOW else Color(0xFF111E26))
                        .clickable {
                            activeTab = tab
                            soundSynth.playBeep(true)
                        }
                        .padding(vertical = 6.dp)
                        .testTag("shipyard_tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        color = if (isSelected) Color(0xFF000000) else BBC_WHITE,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- TAB CONTENT ---
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (activeTab) {
                // ==================== HULL REINFORCEMENT TAB ====================
                ShipyardTab.HULL -> {
                    item {
                        // Hull Repair Service Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF140D05)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF884400)),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.fillMaxWidth().testTag("hull_repair_card")
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
                                        text = "DRYDOCK HULL REPAIR SERVICE",
                                        color = BBC_YELLOW,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    val damage = (maxHull - currentHull).toInt()
                                    Text(
                                        text = if (damage > 0) "DAMAGE: -$damage HP  |  REPAIR COST: ${"%.1f".format(repairCostDeci / 10.0)} CR"
                                        else "HULL STRUCTURAL INTEGRITY AT 100% NOMINAL",
                                        color = if (damage > 0) Color(0xFFFF9944) else BBC_GREEN,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Button(
                                    onClick = {
                                        onRepairHull()
                                        onEquipChanged()
                                    },
                                    enabled = repairCostDeci > 0 && commander.cashDeciCredits >= repairCostDeci,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF994400),
                                        disabledContainerColor = Color(0xFF221100)
                                    ),
                                    shape = RoundedCornerShape(2.dp),
                                    modifier = Modifier.testTag("repair_hull_button")
                                ) {
                                    Text(
                                        text = if (repairCostDeci == 0L) "PRISTINE" else "REPAIR",
                                        color = BBC_WHITE,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    items(ShipUpgradeCatalog.HULL_TIERS) { tier ->
                        val isFitted = hullLevel == tier.level
                        val isPrevious = hullLevel > tier.level
                        val techOk = currentSystem.techLevel >= tier.minTechLevel
                        val affordable = commander.cashDeciCredits >= tier.priceDeciCredits
                        val canUpgrade = !isFitted && !isPrevious && techOk && affordable

                        UpgradeOptionCard(
                            title = tier.name,
                            subtitle = tier.description,
                            stats = "MAX HULL: ${tier.maxHull.toInt()} HP  |  ARMOR DEFLECTION: ${tier.armorPercent}%",
                            techReq = tier.minTechLevel,
                            currentTech = currentSystem.techLevel,
                            priceDeciCr = tier.priceDeciCredits,
                            isFitted = isFitted,
                            isOutdated = isPrevious,
                            canPurchase = canUpgrade,
                            testTag = "upgrade_hull_level_${tier.level}",
                            onPurchase = {
                                onPurchaseHull(tier.level)
                                onEquipChanged()
                            }
                        )
                    }
                }

                // ==================== CARGO CAPACITY TAB ====================
                ShipyardTab.CARGO -> {
                    item {
                        Text(
                            text = "MODULAR CARGO EXPANSION BAYS",
                            color = BBC_YELLOW,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    items(ShipUpgradeCatalog.CARGO_TIERS) { tier ->
                        val isFitted = cargoTier == tier.tier
                        val isPrevious = cargoTier > tier.tier
                        val techOk = currentSystem.techLevel >= tier.minTechLevel
                        val affordable = commander.cashDeciCredits >= tier.priceDeciCredits
                        val canUpgrade = !isFitted && !isPrevious && techOk && affordable

                        UpgradeOptionCard(
                            title = tier.name,
                            subtitle = tier.description,
                            stats = "CAPACITY: ${tier.maxTonnes} TONNES (+${tier.maxTonnes - 20}t OVER BASE)",
                            techReq = tier.minTechLevel,
                            currentTech = currentSystem.techLevel,
                            priceDeciCr = tier.priceDeciCredits,
                            isFitted = isFitted,
                            isOutdated = isPrevious,
                            canPurchase = canUpgrade,
                            testTag = "upgrade_cargo_tier_${tier.tier}",
                            onPurchase = {
                                onPurchaseCargo(tier.tier)
                                onEquipChanged()
                            }
                        )
                    }
                }

                // ==================== WEAPON SYSTEMS TAB ====================
                ShipyardTab.WEAPONS -> {
                    item {
                        Text(
                            text = "PRIMARY FORWARD WEAPON MOUNT",
                            color = BBC_YELLOW,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    items(ShipUpgradeCatalog.FRONT_WEAPONS) { weapon ->
                        val isFitted = frontWeapon.equals(weapon.code, ignoreCase = true)
                        val techOk = currentSystem.techLevel >= weapon.minTechLevel
                        val affordable = commander.cashDeciCredits >= weapon.priceDeciCredits
                        val canPurchase = !isFitted && techOk && affordable

                        UpgradeOptionCard(
                            title = weapon.name,
                            subtitle = weapon.description,
                            stats = "BEAM POWER: ${weapon.power} DMG  |  HARDPOINT: FORWARD",
                            techReq = weapon.minTechLevel,
                            currentTech = currentSystem.techLevel,
                            priceDeciCr = weapon.priceDeciCredits,
                            isFitted = isFitted,
                            isOutdated = false,
                            canPurchase = canPurchase,
                            testTag = "weapon_front_${weapon.code.lowercase()}",
                            onPurchase = {
                                onPurchaseFrontWeapon(weapon.code)
                                onEquipChanged()
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "TURRET AFT REAR HARDPOINT",
                            color = BBC_YELLOW,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    items(ShipUpgradeCatalog.REAR_WEAPONS) { weapon ->
                        val isFitted = if (weapon.code == "NONE") rearWeapon == null else rearWeapon.equals(weapon.code, ignoreCase = true)
                        val techOk = currentSystem.techLevel >= weapon.minTechLevel
                        val affordable = commander.cashDeciCredits >= weapon.priceDeciCredits
                        val canPurchase = !isFitted && techOk && affordable

                        UpgradeOptionCard(
                            title = weapon.name,
                            subtitle = weapon.description,
                            stats = if (weapon.power > 0) "AFT POWER: ${weapon.power} DMG" else "HARDPOINT UNMOUNTED",
                            techReq = weapon.minTechLevel,
                            currentTech = currentSystem.techLevel,
                            priceDeciCr = weapon.priceDeciCredits,
                            isFitted = isFitted,
                            isOutdated = false,
                            canPurchase = canPurchase,
                            testTag = "weapon_rear_${weapon.code.lowercase()}",
                            onPurchase = {
                                onPurchaseRearWeapon(weapon.code)
                                onEquipChanged()
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "WEAPON COOLING RADIATORS & HEAT SINKS",
                            color = BBC_YELLOW,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    items(ShipUpgradeCatalog.COOLING_TIERS) { tier ->
                        val isFitted = coolingTier == tier.tier
                        val isPrevious = coolingTier > tier.tier
                        val techOk = currentSystem.techLevel >= tier.minTechLevel
                        val affordable = commander.cashDeciCredits >= tier.priceDeciCredits
                        val canPurchase = !isFitted && !isPrevious && techOk && affordable

                        UpgradeOptionCard(
                            title = tier.name,
                            subtitle = tier.description,
                            stats = "HEAT DISSIPATION: ${tier.multiplier}x BASE COOLING RATE",
                            techReq = tier.minTechLevel,
                            currentTech = currentSystem.techLevel,
                            priceDeciCr = tier.priceDeciCredits,
                            isFitted = isFitted,
                            isOutdated = isPrevious,
                            canPurchase = canPurchase,
                            testTag = "cooling_tier_${tier.tier}",
                            onPurchase = {
                                onPurchaseCooling(tier.tier)
                                onEquipChanged()
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "MISSILE PYLONS & MUNITIONS",
                            color = BBC_YELLOW,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    items(ShipUpgradeCatalog.MISSILE_PYLON_TIERS) { tier ->
                        val isFitted = missilePylons == tier.capacity
                        val isPrevious = missilePylons > tier.capacity
                        val techOk = currentSystem.techLevel >= tier.minTechLevel
                        val affordable = commander.cashDeciCredits >= tier.priceDeciCredits
                        val canPurchase = !isFitted && !isPrevious && techOk && affordable

                        UpgradeOptionCard(
                            title = tier.name,
                            subtitle = "Hardpoint expansion allowing simultaneous mounting of ${tier.capacity} missiles.",
                            stats = "CAPACITY: ${tier.capacity} WARHEADS",
                            techReq = tier.minTechLevel,
                            currentTech = currentSystem.techLevel,
                            priceDeciCr = tier.priceDeciCredits,
                            isFitted = isFitted,
                            isOutdated = isPrevious,
                            canPurchase = canPurchase,
                            testTag = "missile_rack_${tier.capacity}",
                            onPurchase = {
                                onPurchaseMissilePylons(tier.tier)
                                onEquipChanged()
                            }
                        )
                    }

                    item {
                        // Arm individual missile
                        val canArm = missilesArmed < missilePylons && commander.cashDeciCredits >= 300L
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C161C)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BBC_CYAN.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.fillMaxWidth().testTag("arm_missile_card")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ARM HOMING MISSILE (1X)",
                                        color = BBC_WHITE,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "STATUS: $missilesArmed/$missilePylons ARMED  |  PRICE: 30.0 CR",
                                        color = BBC_YELLOW,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Button(
                                    onClick = {
                                        onArmMissile()
                                        onEquipChanged()
                                    },
                                    enabled = canArm,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF005577),
                                        disabledContainerColor = Color(0xFF10222B)
                                    ),
                                    shape = RoundedCornerShape(2.dp),
                                    modifier = Modifier.testTag("arm_missile_button")
                                ) {
                                    Text(
                                        text = if (missilesArmed >= missilePylons) "MAX ARMED" else "ARM (30 CR)",
                                        color = BBC_WHITE,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // ==================== AVIONICS & UTILITIES TAB ====================
                ShipyardTab.AVIONICS -> {
                    // Refuel
                    item {
                        val missingFuel = (70 - commander.fuelDeciLy).coerceAtLeast(0)
                        val refuelCost = missingFuel * 1L
                        val canRefuel = missingFuel > 0 && commander.cashDeciCredits >= refuelCost

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF04180A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BBC_GREEN.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "HYDROGEN HYPERSPACE REFUEL (FULL 7.0 LY)",
                                        color = BBC_GREEN,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "CURRENT FUEL: ${"%.1f".format(commander.fuelDeciLy / 10.0)} LY  |  COST: ${"%.1f".format(refuelCost / 10.0)} CR",
                                        color = BBC_YELLOW,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Button(
                                    onClick = {
                                        onRefuel()
                                        onEquipChanged()
                                    },
                                    enabled = canRefuel,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF005518),
                                        disabledContainerColor = Color(0xFF102216)
                                    ),
                                    shape = RoundedCornerShape(2.dp),
                                    modifier = Modifier.testTag("refuel_button")
                                ) {
                                    Text(
                                        text = if (missingFuel == 0) "FULL" else "REFUEL",
                                        color = BBC_WHITE,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Specialized avionics equipment list
                    val avionicsList = listOf(
                        AvionicsItem("ecm", "E.C.M. Countermeasures System", "Electronic Counter-Measures to neutralize hostile guided warheads.", 6000L, 2, commander.hasEcm),
                        AvionicsItem("scoops", "Fuel Scoops", "Enables hydrogen skimming from stellar coronas & container salvage.", 5250L, 5, commander.hasFuelScoops),
                        AvionicsItem("docking_comp", "Naval Docking Computers", "Automated rotation-matching autopilot for hands-free station docking.", 15000L, 9, commander.hasDockingComputer),
                        AvionicsItem("energy_bomb", "Energy Bomb", "Single-use pulse discharge clearing hostile ships within visual scanner.", 9000L, 7, commander.hasEnergyBomb),
                        AvionicsItem("galactic_hyper", "Galactic Hyperdrive", "Single-jump gateway drive leaping into adjacent procedural Galaxies (1-8).", 50000L, 10, commander.hasGalacticHyperdrive),
                        AvionicsItem("escape_pod", "Life-Support Escape Pod", "Emergency ejection pod safeguarding commander license upon destruction.", 10000L, 6, commander.hasEscapePod),
                        AvionicsItem("naval_energy", "Naval Energy Unit", "Doubles shield regeneration rate and power bank reservoir capacity.", 15000L, 8, commander.hasEnergyUnit)
                    )

                    items(avionicsList) { item ->
                        val techOk = currentSystem.techLevel >= item.minTech
                        val affordable = commander.cashDeciCredits >= item.priceDeciCr
                        val canBuy = !item.isFitted && techOk && affordable

                        UpgradeOptionCard(
                            title = item.name,
                            subtitle = item.description,
                            stats = "TECH LEVEL REQUIRED: ${item.minTech}",
                            techReq = item.minTech,
                            currentTech = currentSystem.techLevel,
                            priceDeciCr = item.priceDeciCr,
                            isFitted = item.isFitted,
                            isOutdated = false,
                            canPurchase = canBuy,
                            testTag = "avionics_${item.id}",
                            onPurchase = {
                                onPurchaseEquipment(item.id, item.priceDeciCr, item.minTech)
                                onEquipChanged()
                            }
                        )
                    }
                }
            }
        }
    }
}

data class AvionicsItem(
    val id: String,
    val name: String,
    val description: String,
    val priceDeciCr: Long,
    val minTech: Int,
    val isFitted: Boolean
)

@Composable
fun UpgradeOptionCard(
    title: String,
    subtitle: String,
    stats: String,
    techReq: Int,
    currentTech: Int,
    priceDeciCr: Long,
    isFitted: Boolean,
    isOutdated: Boolean,
    canPurchase: Boolean,
    testTag: String,
    onPurchase: () -> Unit
) {
    val techOk = currentTech >= techReq
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isFitted) Color(0xFF03141E) else Color(0xFF060B10)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isFitted) BBC_CYAN else if (!techOk) Color(0xFF333333) else Color(0xFF1E384D)
        ),
        shape = RoundedCornerShape(2.dp),
        modifier = Modifier.fillMaxWidth().testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = if (isFitted) BBC_CYAN else if (!techOk) Color(0xFF888888) else BBC_WHITE,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (isFitted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[INSTALLED]",
                            color = BBC_GREEN,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Text(
                    text = subtitle,
                    color = Color(0xFFAABBCC),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stats,
                        color = BBC_YELLOW,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "TECH: $techReq  |  ${"%.1f".format(priceDeciCr / 10.0)} CR",
                        color = if (techOk) BBC_GREEN else Color(0xFFFF5555),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Button(
                onClick = onPurchase,
                enabled = canPurchase,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF005518),
                    disabledContainerColor = Color(0xFF101A14)
                ),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.size(width = 82.dp, height = 32.dp).testTag("${testTag}_btn")
            ) {
                Text(
                    text = if (isFitted) "FITTED"
                    else if (isOutdated) "OWNED"
                    else if (!techOk) "TECH $techReq"
                    else "BUY",
                    color = if (canPurchase) BBC_GREEN else Color(0xFF667788),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
