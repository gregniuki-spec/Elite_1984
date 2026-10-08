package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
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
import com.example.elite.model.GameScreen
import com.example.elite.ui.BBC_CYAN
import com.example.elite.ui.BBC_GREEN
import com.example.elite.ui.BBC_WHITE
import com.example.elite.ui.BBC_YELLOW
import com.example.elite.ui.BattleArenaView
import com.example.elite.ui.CommanderStatusView
import com.example.elite.ui.DashboardView
import com.example.elite.ui.FlightManualView
import com.example.elite.ui.GalacticChartView
import com.example.elite.ui.MarketTradingView
import com.example.elite.ui.RedKeyNavigationBar
import com.example.elite.ui.ResourceExplorationView
import com.example.elite.ui.ShipEncyclopediaView
import com.example.elite.ui.ShipyardView
import com.example.elite.ui.SourceInspectorView
import com.example.elite.ui.SpaceFlightView
import com.example.elite.ui.SystemDataView
import com.example.elite.viewmodel.EliteViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: EliteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF000000))
                ) { innerPadding ->
                    EliteMainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun EliteMainScreen(
    viewModel: EliteViewModel,
    modifier: Modifier = Modifier
) {
    val commander = viewModel.commander
    val currentSystem = viewModel.currentSystem
    val targetSystem = viewModel.targetSystem
    val marketEntities by viewModel.marketEntities.collectAsStateWithLifecycle()
    val economyState by viewModel.currentEconomyState.collectAsStateWithLifecycle()
    val priceHistory by viewModel.selectedCommodityHistory.collectAsStateWithLifecycle()
    val shipUpgradeState by viewModel.shipUpgradeState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // Top System Bar & Docking Station Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF08120B))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ELITE - BBC MICRO 6502",
                    color = BBC_YELLOW,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (commander.isDocked) "DOCKED: CORIOLIS (${currentSystem.name})" else "SPACE: ${currentSystem.name}",
                    color = if (commander.isDocked) BBC_GREEN else BBC_CYAN,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (commander.isDocked) {
                Button(
                    onClick = { viewModel.launchFromStation() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005518)),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier.size(width = 90.dp, height = 28.dp).testTag("launch_button")
                ) {
                    Text("LAUNCH", color = BBC_WHITE, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "CR: ${"%.1f".format(commander.cashDeciCredits / 10.0)}",
                    color = BBC_GREEN,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // BBC Micro Iconic Red Function Key Bar
        RedKeyNavigationBar(
            currentScreen = viewModel.currentScreen,
            onScreenSelected = { screen -> viewModel.selectScreen(screen) }
        )

        // Main Center Display
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (viewModel.currentScreen) {
                GameScreen.SPACE_FLIGHT -> {
                    SpaceFlightView(
                        flightEngine = viewModel.flightEngine,
                        onOpenBattleArena = { viewModel.selectScreen(GameScreen.BATTLE_ARENA) },
                        onOpenFleetStrategy = { viewModel.selectScreen(GameScreen.FLEET_STRATEGY) },
                        onOpenResourceExploration = { viewModel.selectScreen(GameScreen.RESOURCE_EXPLORATION) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                GameScreen.BATTLE_ARENA -> {
                    BattleArenaView(
                        flightEngine = viewModel.flightEngine,
                        commander = commander,
                        soundSynth = viewModel.soundSynth,
                        onReturnToFlight = { viewModel.selectScreen(GameScreen.SPACE_FLIGHT) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                GameScreen.FLEET_STRATEGY -> {
                    com.example.elite.ui.FleetStrategySimulationView(
                        sectorEngine = viewModel.sectorWarEngine,
                        commander = commander,
                        soundSynth = viewModel.soundSynth,
                        onLaunchDogfight = {
                            viewModel.flightEngine.spawnBattleWave(com.example.elite.flight.CombatScenario.SECTOR_WAR_100_BOTS)
                            viewModel.selectScreen(GameScreen.BATTLE_ARENA)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                GameScreen.RESOURCE_EXPLORATION -> {
                    ResourceExplorationView(
                        system = currentSystem,
                        commander = commander,
                        flightEngine = viewModel.flightEngine,
                        soundSynth = viewModel.soundSynth,
                        onLaunchAsteroidBelt = { viewModel.selectScreen(GameScreen.SPACE_FLIGHT) },
                        onClose = { viewModel.selectScreen(GameScreen.SPACE_FLIGHT) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                GameScreen.FLIGHT_MANUAL -> {
                    FlightManualView(
                        soundSynth = viewModel.soundSynth,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                GameScreen.MARKET_PRICES -> {
                    MarketTradingView(
                        commander = commander,
                        currentSystem = currentSystem,
                        marketGoods = viewModel.currentMarket,
                        commodityEntities = marketEntities,
                        economyState = economyState,
                        priceHistory = priceHistory,
                        selectedCommodityId = viewModel.selectedCommodityForHistoryId,
                        soundSynth = viewModel.soundSynth,
                        onBuy = { item -> viewModel.buyCommodity(item) },
                        onSell = { item -> viewModel.sellCommodity(item) },
                        onSelectCommodityForHistory = { id -> viewModel.selectCommodityForHistory(id) },
                        onTriggerFluctuation = { viewModel.triggerMarketFluctuation() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                GameScreen.GALACTIC_CHART -> {
                    GalacticChartView(
                        commander = commander,
                        systems = viewModel.systems,
                        currentSystem = currentSystem,
                        isShortRange = false,
                        soundSynth = viewModel.soundSynth,
                        onSystemSelected = { sys -> viewModel.selectTargetSystem(sys) },
                        onHyperspaceJump = { sys, fuel -> viewModel.jumpToTarget(sys, fuel) },
                        onInvest = { sys, type -> viewModel.investInSystem(sys, type) },
                        onAcceptContract = { contract -> viewModel.acceptContract(contract) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                GameScreen.SHORT_RANGE_CHART -> {
                    GalacticChartView(
                        commander = commander,
                        systems = viewModel.systems,
                        currentSystem = currentSystem,
                        isShortRange = true,
                        soundSynth = viewModel.soundSynth,
                        onSystemSelected = { sys -> viewModel.selectTargetSystem(sys) },
                        onHyperspaceJump = { sys, fuel -> viewModel.jumpToTarget(sys, fuel) },
                        onInvest = { sys, type -> viewModel.investInSystem(sys, type) },
                        onAcceptContract = { contract -> viewModel.acceptContract(contract) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                GameScreen.SYSTEM_DATA -> {
                    val dist = com.example.elite.universe.GalaxyGenerator.distanceInDeciLy(currentSystem, targetSystem)
                    SystemDataView(
                        system = targetSystem,
                        commander = commander,
                        soundSynth = viewModel.soundSynth,
                        onOpen3dView = { viewModel.selectScreen(GameScreen.SHORT_RANGE_CHART) },
                        onInvest = { type -> viewModel.investInSystem(targetSystem, type) },
                        onHyperspaceJump = { viewModel.jumpToTarget(targetSystem, dist) },
                        hasFuel = commander.fuelDeciLy >= dist,
                        distDeciLy = dist,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                GameScreen.STATUS -> {
                    CommanderStatusView(
                        commander = commander,
                        currentSystem = currentSystem,
                        targetSystem = targetSystem,
                        isInventoryMode = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                GameScreen.INVENTORY -> {
                    CommanderStatusView(
                        commander = commander,
                        currentSystem = currentSystem,
                        targetSystem = targetSystem,
                        isInventoryMode = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                GameScreen.EQUIP_SHIP -> {
                    ShipyardView(
                        commander = commander,
                        currentSystem = currentSystem,
                        soundSynth = viewModel.soundSynth,
                        shipUpgrade = shipUpgradeState,
                        onPurchaseHull = { level -> viewModel.purchaseHullReinforcement(level) },
                        onRepairHull = { viewModel.repairHull() },
                        onPurchaseCargo = { tier -> viewModel.purchaseCargoCapacity(tier) },
                        onPurchaseFrontWeapon = { code -> viewModel.purchaseFrontWeapon(code) },
                        onPurchaseRearWeapon = { code -> viewModel.purchaseRearWeapon(code) },
                        onPurchaseCooling = { tier -> viewModel.purchaseCoolingUpgrade(tier) },
                        onPurchaseMissilePylons = { tier -> viewModel.purchaseMissilePylons(tier) },
                        onArmMissile = { viewModel.armMissile() },
                        onPurchaseEquipment = { id, cost, minTech -> viewModel.purchaseEquipment(id, cost, minTech) },
                        onRefuel = { viewModel.refuelShip() },
                        onEquipChanged = { /* Recomposition handles updates */ },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                GameScreen.SHIP_ENCYCLOPEDIA -> {
                    ShipEncyclopediaView(
                        soundSynth = viewModel.soundSynth,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                GameScreen.ASM_INSPECTOR -> {
                    SourceInspectorView(
                        repository = viewModel.sourceRepository,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                GameScreen.AI_DATASET_TRAINING -> {
                    com.example.elite.ui.AiDatasetTrainingView(
                        datasetCollector = viewModel.datasetCollector,
                        flightEngine = viewModel.flightEngine,
                        commander = commander,
                        onSignInWithGoogleClick = {
                            viewModel.authManager.signInWithGoogle()
                        },
                        onSignOutClick = {
                            viewModel.authManager.signOut()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Bottom Dashboard (DIALS: 3D Scanner Radar Stalks, Compass, Shields, Speed, Energy Banks)
        DashboardView(
            commander = commander,
            flightEngine = viewModel.flightEngine
        )
    }
}
