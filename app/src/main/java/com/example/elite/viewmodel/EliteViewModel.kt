package com.example.elite.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.elite.audio.BbcSoundSynth
import com.example.elite.flight.FlightEngine
import com.example.elite.market.EconomyCondition
import com.example.elite.market.MarketEngine
import com.example.elite.market.data.MarketDatabase
import com.example.elite.market.data.MarketPriceHistoryEntity
import com.example.elite.market.data.MarketRepository
import com.example.elite.market.data.SystemCommodityEntity
import com.example.elite.market.data.SystemEconomyEntity
import com.example.elite.model.Commodity
import com.example.elite.model.CommanderState
import com.example.elite.model.GameScreen
import com.example.elite.model.SystemData
import com.example.elite.ships.data.ShipUpgradeCatalog
import com.example.elite.ships.data.ShipUpgradeEntity
import com.example.elite.ships.data.ShipUpgradeRepository
import com.example.elite.sourceviewer.SourceRepository
import com.example.elite.universe.GalaxyGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EliteViewModel(application: Application) : AndroidViewModel(application) {

    val soundSynth = BbcSoundSynth(viewModelScope)
    val sourceRepository = SourceRepository(application)

    // Room Database & Repositories
    private val marketDatabase = MarketDatabase.getDatabase(application)
    val marketRepository = MarketRepository(marketDatabase.marketDao())
    val shipUpgradeRepository = ShipUpgradeRepository(marketDatabase.shipUpgradeDao())

    // Cloud Firestore Dataset Repository & Telemetry Collector for AI Bot Training
    val datasetRepository = com.example.elite.dataset.data.DatasetRepository(application)
    val datasetCollector = com.example.elite.dataset.service.GameplayDatasetCollector(
        datasetRepository = datasetRepository,
        scope = viewModelScope
    )
    val authManager = com.example.elite.auth.GoogleAuthManager(
        context = application,
        scope = viewModelScope
    )

    // Sector War Simulation Engine (up to 100 AI bots + simulated multiplayer)
    val sectorWarEngine = com.example.elite.simulation.SectorWarSimulationEngine()

    // Commander Profile
    val commander = CommanderState()

    // Ship Upgrade Room StateFlow
    private val _shipUpgradeState = MutableStateFlow<ShipUpgradeEntity?>(null)
    val shipUpgradeState: StateFlow<ShipUpgradeEntity?> = _shipUpgradeState.asStateFlow()

    // Galaxy 1 procedural data (256 systems)
    val systems: List<SystemData> = GalaxyGenerator.generateGalaxy(1)

    // Current and targeted systems (Default to Lave in Galaxy 1)
    var currentSystem by mutableStateOf(systems.find { it.name == "Lave" } ?: systems[0])
    var targetSystem by mutableStateOf(systems.find { it.name == "Lave" } ?: systems[0])

    // Market items (Legacy in-memory list kept in sync with Room entities)
    val currentMarket = mutableStateListOf<Commodity>()

    // Room state flows for reactive UI
    private val _marketEntities = MutableStateFlow<List<SystemCommodityEntity>>(emptyList())
    val marketEntities: StateFlow<List<SystemCommodityEntity>> = _marketEntities.asStateFlow()

    private val _currentEconomyState = MutableStateFlow<SystemEconomyEntity?>(null)
    val currentEconomyState: StateFlow<SystemEconomyEntity?> = _currentEconomyState.asStateFlow()

    private val _selectedCommodityHistory = MutableStateFlow<List<MarketPriceHistoryEntity>>(emptyList())
    val selectedCommodityHistory: StateFlow<List<MarketPriceHistoryEntity>> = _selectedCommodityHistory.asStateFlow()

    var selectedCommodityForHistoryId by mutableStateOf<Int?>(null)

    private var marketObserverJob: Job? = null
    private var economyObserverJob: Job? = null
    private var historyObserverJob: Job? = null

    // Current Screen
    var currentScreen by mutableStateOf(GameScreen.SPACE_FLIGHT)

    // 3D Flight Engine
    lateinit var flightEngine: FlightEngine

    init {
        flightEngine = FlightEngine(
            commander = commander,
            soundSynth = soundSynth,
            onDocked = {
                currentScreen = GameScreen.STATUS
            },
            onHyperspaceComplete = { newSys ->
                currentSystem = newSys
                onArriveInSystem(newSys)

                // Calculate and award colony investment dividends
                var totalDividends = 0
                for (inv in commander.investments) {
                    val div = inv.currentDividendCredits()
                    totalDividends += div
                    inv.totalEarnedDeciCr += div * 10L
                }
                if (totalDividends > 0) {
                    commander.cashDeciCredits += totalDividends * 10L
                    commander.lastDividendEarningsDeciCr = totalDividends * 10L
                }

                // Check contract progress (e.g. supply delivery)
                checkContractsOnArrival(newSys)

                currentScreen = GameScreen.SPACE_FLIGHT
                if (totalDividends > 0) {
                    flightEngine.showMessage("ARRIVED ${newSys.name} | COLONY DIVIDENDS: +$totalDividends CR")
                } else {
                    flightEngine.showMessage("HYPERSPACE ARRIVAL: ${newSys.name}")
                }
            },
            onLaserFiredCallback = { hit ->
                datasetCollector.onLaserFired(hit)
            },
            onMissileFiredCallback = {
                datasetCollector.onMissileFired()
            },
            onEcmSuccessCallback = {
                datasetCollector.onEcmNeutralized()
            },
            onBountyEarnedCallback = { bounty ->
                datasetCollector.onBountyEarned(bounty)
            }
        )

        loadInitialMarketForSystem(currentSystem)
        initShipUpgradeSync()
        startFlightLoop()
        datasetCollector.startContinuousTelemetryHarvest(flightEngine, commander)

        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                if (user != null) {
                    datasetCollector.onUserAuthenticated(user.uid)
                } else {
                    datasetCollector.onUserSignedOut()
                }
            }
        }
    }

    private fun checkContractsOnArrival(arrivalSystem: SystemData) {
        for (contract in commander.activeContracts) {
            if (!contract.isCompleted && contract.targetSystemId == arrivalSystem.id) {
                if (contract.type == com.example.elite.model.ContractType.SUPPLY_DELIVERY) {
                    val commodityId = contract.commodityIdNeeded
                    val neededQty = contract.quantityNeeded
                    if (commodityId != null && (commander.cargo[commodityId] ?: 0) >= neededQty) {
                        commander.cargo[commodityId] = (commander.cargo[commodityId] ?: 0) - neededQty
                        contract.isCompleted = true
                        commander.cashDeciCredits += contract.rewardCredits * 10L
                        flightEngine.showMessage("CONTRACT COMPLETE: ${contract.title} (+${contract.rewardCredits} CR)")
                        soundSynth.playBeep(true)
                    }
                } else if (contract.type == com.example.elite.model.ContractType.MINERAL_SURVEY) {
                    // Check if player has precious minerals (Gold=13, Platinum=14, Gems=15)
                    val minerals = (commander.cargo[13] ?: 0) + (commander.cargo[14] ?: 0) + (commander.cargo[15] ?: 0)
                    if (minerals >= 2) {
                        contract.isCompleted = true
                        commander.cashDeciCredits += contract.rewardCredits * 10L
                        flightEngine.showMessage("SURVEY CONTRACT COMPLETE: +${contract.rewardCredits} CR")
                        soundSynth.playBeep(true)
                    }
                }
            }
        }
    }

    fun investInSystem(system: SystemData, type: com.example.elite.model.InvestmentType): Boolean {
        val existing = commander.investments.find { it.systemId == system.id && it.type == type }
        val cost = existing?.upgradeCostCredits() ?: type.baseCostCredits
        val costDeci = cost * 10L

        if (commander.cashDeciCredits >= costDeci) {
            commander.cashDeciCredits -= costDeci
            if (existing != null) {
                existing.level++
                flightEngine.showMessage("UPGRADED ${type.title} TO LVL ${existing.level} (-$cost CR)")
            } else {
                commander.investments.add(
                    com.example.elite.model.ColonyInvestment(
                        id = System.currentTimeMillis(),
                        systemId = system.id,
                        systemName = system.name,
                        type = type,
                        level = 1
                    )
                )
                flightEngine.showMessage("ESTABLISHED ${type.title} IN ${system.name} (-$cost CR)")
            }
            soundSynth.playBeep(true)
            return true
        } else {
            flightEngine.showMessage("INSUFFICIENT FUNDS (NEEDS $cost CR)")
            soundSynth.playBeep(false)
            return false
        }
    }

    fun acceptContract(contract: com.example.elite.model.StrategicContract): Boolean {
        if (commander.activeContracts.any { it.id == contract.id }) return false
        if (commander.activeContracts.size >= 5) {
            flightEngine.showMessage("MAX 5 ACTIVE CONTRACTS PERMITTED")
            soundSynth.playBeep(false)
            return false
        }
        commander.activeContracts.add(contract)
        flightEngine.showMessage("CONTRACT ACCEPTED: ${contract.title}")
        soundSynth.playBeep(true)
        return true
    }

    fun getArbitrageRoutes(fromSys: SystemData = currentSystem): List<com.example.elite.model.ArbitrageOpportunity> {
        return com.example.elite.model.TradeArbitrageEngine.findTopArbitrageRoutes(
            fromSystem = fromSys,
            allSystems = systems,
            maxDistanceLy = 7.0f,
            cargoCapacity = commander.cargoCapacity
        )
    }

    fun getSystemContracts(sys: SystemData = currentSystem): List<com.example.elite.model.StrategicContract> {
        return com.example.elite.model.TradeArbitrageEngine.generateContractsForSystem(sys, systems)
    }

    fun selectScreen(screen: GameScreen) {
        soundSynth.playBeep(true)
        currentScreen = screen
    }

    fun selectTargetSystem(sys: SystemData) {
        targetSystem = sys
        soundSynth.playBeep(true)
    }

    fun jumpToTarget(sys: SystemData, fuelNeeded: Int) {
        flightEngine.startHyperspaceJump(sys, fuelNeeded)
    }

    fun refreshMarket() {
        loadInitialMarketForSystem(currentSystem)
    }

    private fun loadInitialMarketForSystem(system: SystemData) {
        viewModelScope.launch {
            val goods = marketRepository.getOrInitializeMarket(system)
            currentMarket.clear()
            currentMarket.addAll(goods)
            observeSystemMarketFlows(system.id)
        }
    }

    fun onArriveInSystem(newSys: SystemData) {
        viewModelScope.launch {
            // Trigger economic cycle advancement in Room for this planet
            marketRepository.advanceEconomicCycle(newSys)
            val goods = marketRepository.getOrInitializeMarket(newSys)
            currentMarket.clear()
            currentMarket.addAll(goods)
            observeSystemMarketFlows(newSys.id)
        }
    }

    private fun observeSystemMarketFlows(systemId: Int) {
        marketObserverJob?.cancel()
        economyObserverJob?.cancel()

        marketObserverJob = viewModelScope.launch {
            marketRepository.observeCommodityEntities(systemId).collect { entities ->
                _marketEntities.value = entities
                if (entities.isNotEmpty()) {
                    currentMarket.clear()
                    currentMarket.addAll(
                        entities.map { entity ->
                            val base = MarketEngine.BASE_ITEMS.find { it.id == entity.commodityId }
                                ?: MarketEngine.BASE_ITEMS[0]
                            base.copy(
                                price = entity.currentPriceDeciCr,
                                quantity = entity.availableQty
                            )
                        }
                    )
                }
            }
        }

        economyObserverJob = viewModelScope.launch {
            marketRepository.observeEconomyForSystem(systemId).collect { econ ->
                _currentEconomyState.value = econ
            }
        }
    }

    fun selectCommodityForHistory(commodityId: Int?) {
        selectedCommodityForHistoryId = commodityId
        historyObserverJob?.cancel()
        if (commodityId != null) {
            historyObserverJob = viewModelScope.launch {
                marketRepository.observePriceHistory(currentSystem.id, commodityId).collect { history ->
                    _selectedCommodityHistory.value = history
                }
            }
        } else {
            _selectedCommodityHistory.value = emptyList()
        }
    }

    fun triggerMarketFluctuation(forcedCondition: EconomyCondition? = null) {
        viewModelScope.launch {
            val updatedEcon = marketRepository.advanceEconomicCycle(currentSystem, forcedCondition)
            flightEngine.showMessage("ECONOMIC SHIFT: ${updatedEcon.activeBoomState}")
            soundSynth.playBeep(true)
        }
    }

    fun buyCommodity(commodity: Commodity): Boolean {
        val inHold = commander.cargoHold[commodity.id] ?: 0
        val isTonnageItem = commodity.units == "t"
        if (commodity.quantity > 0 &&
            commander.cashDeciCredits >= commodity.price &&
            (!isTonnageItem || commander.freeCargoSpace() >= 1)
        ) {
            commander.cashDeciCredits -= commodity.price
            commander.cargoHold[commodity.id] = inHold + 1
            commodity.quantity--

            // Persist stock reduction into Room database
            viewModelScope.launch {
                marketRepository.updateStock(currentSystem.id, commodity.id, commodity.quantity)
            }

            soundSynth.playBeep(true)
            return true
        } else {
            soundSynth.playBeep(false)
            return false
        }
    }

    fun sellCommodity(commodity: Commodity): Boolean {
        val inHold = commander.cargoHold[commodity.id] ?: 0
        if (inHold > 0) {
            commander.cashDeciCredits += commodity.price
            commander.cargoHold[commodity.id] = inHold - 1
            commodity.quantity++

            // Persist stock replenishment into Room database
            viewModelScope.launch {
                marketRepository.updateStock(currentSystem.id, commodity.id, commodity.quantity)
            }

            soundSynth.playBeep(true)
            return true
        } else {
            soundSynth.playBeep(false)
            return false
        }
    }

    fun launchFromStation() {
        if (commander.isDocked) {
            commander.isDocked = false
            flightEngine.speed = 12f
            currentScreen = GameScreen.SPACE_FLIGHT
            flightEngine.showMessage("LAUNCHING FROM CORIOLIS STATION")
            soundSynth.playBeep(true)
        }
    }

    private fun startFlightLoop() {
        viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (isActive) {
                val now = System.nanoTime()
                val dt = (now - lastTime) / 1_000_000_000.0f
                lastTime = now

                if (currentScreen == GameScreen.SPACE_FLIGHT || currentScreen == GameScreen.BATTLE_ARENA || currentScreen == GameScreen.FLEET_STRATEGY || flightEngine.isDockingComputerActive) {
                    flightEngine.update(dt)
                }

                // Sector War AI simulation update
                sectorWarEngine.update(dt)

                delay(16L) // Target ~60 FPS update
            }
        }
    }

    private fun initShipUpgradeSync() {
        viewModelScope.launch {
            val initial = shipUpgradeRepository.getOrInitShipUpgrade()
            syncCommanderWithShipUpgrade(initial)
            _shipUpgradeState.value = initial

            shipUpgradeRepository.observeShipUpgrade().collect { upgrade ->
                if (upgrade != null) {
                    _shipUpgradeState.value = upgrade
                    syncCommanderWithShipUpgrade(upgrade)
                }
            }
        }
    }

    private fun syncCommanderWithShipUpgrade(upgrade: ShipUpgradeEntity) {
        commander.hullReinforcementLevel = upgrade.hullReinforcementLevel
        commander.maxHullIntegrity = upgrade.maxHullIntegrity
        commander.currentHullIntegrity = upgrade.currentHullIntegrity
        commander.armorRatingPercent = upgrade.armorRatingPercent
        commander.cargoCapacityTier = upgrade.cargoCapacityTier
        commander.cargoCapacity = upgrade.cargoHoldMaxTonnes
        commander.laserFront = upgrade.toFrontLaserType()
        commander.laserRear = upgrade.toRearLaserType()
        commander.weaponCoolingTier = upgrade.weaponCoolingTier
        commander.weaponCoolingMultiplier = upgrade.weaponCoolingMultiplier
        commander.missileCapacity = upgrade.missileCapacity
        commander.missiles = upgrade.missilesArmed
        commander.hasEcm = upgrade.hasEcm
        commander.hasFuelScoops = upgrade.hasFuelScoops
        commander.hasDockingComputer = upgrade.hasDockingComputer
        commander.hasGalacticHyperdrive = upgrade.hasGalacticHyperdrive
        commander.hasEscapePod = upgrade.hasEscapePod
        commander.hasEnergyBomb = upgrade.hasEnergyBomb
        commander.hasEnergyUnit = upgrade.hasNavalEnergyUnit
    }

    fun purchaseHullReinforcement(targetLevel: Int): Boolean {
        val tier = ShipUpgradeCatalog.HULL_TIERS.find { it.level == targetLevel } ?: return false
        if (commander.cashDeciCredits < tier.priceDeciCredits) {
            soundSynth.playBeep(false)
            return false
        }
        if (currentSystem.techLevel < tier.minTechLevel) {
            soundSynth.playBeep(false)
            return false
        }
        commander.cashDeciCredits -= tier.priceDeciCredits
        viewModelScope.launch {
            val updated = shipUpgradeRepository.purchaseHullUpgrade(targetLevel)
            syncCommanderWithShipUpgrade(updated)
            flightEngine.showMessage("HULL UPGRADED: ${tier.name.uppercase()} (-${"%.1f".format(tier.priceDeciCredits / 10.0)} CR)")
            soundSynth.playBeep(true)
        }
        return true
    }

    fun repairHull(): Boolean {
        val cost = ShipUpgradeCatalog.calculateRepairCostDeciCr(commander.currentHullIntegrity, commander.maxHullIntegrity)
        if (cost <= 0L) return false
        if (commander.cashDeciCredits < cost) {
            soundSynth.playBeep(false)
            return false
        }
        commander.cashDeciCredits -= cost
        viewModelScope.launch {
            val updated = shipUpgradeRepository.repairHull()
            syncCommanderWithShipUpgrade(updated)
            flightEngine.showMessage("HULL FULLY RESTORED (-${"%.1f".format(cost / 10.0)} CR)")
            soundSynth.playBeep(true)
        }
        return true
    }

    fun purchaseCargoCapacity(targetTier: Int): Boolean {
        val tier = ShipUpgradeCatalog.CARGO_TIERS.find { it.tier == targetTier } ?: return false
        if (commander.cashDeciCredits < tier.priceDeciCredits) {
            soundSynth.playBeep(false)
            return false
        }
        if (currentSystem.techLevel < tier.minTechLevel) {
            soundSynth.playBeep(false)
            return false
        }
        commander.cashDeciCredits -= tier.priceDeciCredits
        viewModelScope.launch {
            val updated = shipUpgradeRepository.purchaseCargoUpgrade(targetTier)
            syncCommanderWithShipUpgrade(updated)
            flightEngine.showMessage("CARGO EXPANDED: ${tier.maxTonnes}t HOLD (-${"%.1f".format(tier.priceDeciCredits / 10.0)} CR)")
            soundSynth.playBeep(true)
        }
        return true
    }

    fun purchaseFrontWeapon(code: String): Boolean {
        val spec = ShipUpgradeCatalog.FRONT_WEAPONS.find { it.code.equals(code, ignoreCase = true) } ?: return false
        if (commander.cashDeciCredits < spec.priceDeciCredits) {
            soundSynth.playBeep(false)
            return false
        }
        if (currentSystem.techLevel < spec.minTechLevel) {
            soundSynth.playBeep(false)
            return false
        }
        commander.cashDeciCredits -= spec.priceDeciCredits
        viewModelScope.launch {
            val updated = shipUpgradeRepository.purchaseFrontWeapon(code)
            syncCommanderWithShipUpgrade(updated)
            flightEngine.showMessage("INSTALLED FRONT: ${spec.name.uppercase()} (-${"%.1f".format(spec.priceDeciCredits / 10.0)} CR)")
            soundSynth.playBeep(true)
        }
        return true
    }

    fun purchaseRearWeapon(code: String): Boolean {
        val spec = ShipUpgradeCatalog.REAR_WEAPONS.find { it.code.equals(code, ignoreCase = true) } ?: return false
        if (commander.cashDeciCredits < spec.priceDeciCredits) {
            soundSynth.playBeep(false)
            return false
        }
        if (currentSystem.techLevel < spec.minTechLevel) {
            soundSynth.playBeep(false)
            return false
        }
        commander.cashDeciCredits -= spec.priceDeciCredits
        viewModelScope.launch {
            val updated = shipUpgradeRepository.purchaseRearWeapon(code)
            syncCommanderWithShipUpgrade(updated)
            flightEngine.showMessage("INSTALLED REAR: ${spec.name.uppercase()} (-${"%.1f".format(spec.priceDeciCredits / 10.0)} CR)")
            soundSynth.playBeep(true)
        }
        return true
    }

    fun purchaseCoolingUpgrade(tier: Int): Boolean {
        val spec = ShipUpgradeCatalog.COOLING_TIERS.find { it.tier == tier } ?: return false
        if (commander.cashDeciCredits < spec.priceDeciCredits) {
            soundSynth.playBeep(false)
            return false
        }
        if (currentSystem.techLevel < spec.minTechLevel) {
            soundSynth.playBeep(false)
            return false
        }
        commander.cashDeciCredits -= spec.priceDeciCredits
        viewModelScope.launch {
            val updated = shipUpgradeRepository.purchaseCoolingUpgrade(tier)
            syncCommanderWithShipUpgrade(updated)
            flightEngine.showMessage("WEAPON RADIATOR: ${spec.name.uppercase()} (-${"%.1f".format(spec.priceDeciCredits / 10.0)} CR)")
            soundSynth.playBeep(true)
        }
        return true
    }

    fun purchaseMissilePylons(tier: Int): Boolean {
        val spec = ShipUpgradeCatalog.MISSILE_PYLON_TIERS.find { it.tier == tier } ?: return false
        if (commander.cashDeciCredits < spec.priceDeciCredits) {
            soundSynth.playBeep(false)
            return false
        }
        if (currentSystem.techLevel < spec.minTechLevel) {
            soundSynth.playBeep(false)
            return false
        }
        commander.cashDeciCredits -= spec.priceDeciCredits
        viewModelScope.launch {
            val updated = shipUpgradeRepository.purchaseMissilePylons(tier)
            syncCommanderWithShipUpgrade(updated)
            flightEngine.showMessage("MISSILE RACK: ${spec.capacity} PYLONS (-${"%.1f".format(spec.priceDeciCredits / 10.0)} CR)")
            soundSynth.playBeep(true)
        }
        return true
    }

    fun armMissile(): Boolean {
        val price = 300L // 30.0 CR
        if (commander.missiles >= commander.missileCapacity) {
            soundSynth.playBeep(false)
            return false
        }
        if (commander.cashDeciCredits < price) {
            soundSynth.playBeep(false)
            return false
        }
        commander.cashDeciCredits -= price
        viewModelScope.launch {
            val updated = shipUpgradeRepository.armMissile(price)
            syncCommanderWithShipUpgrade(updated)
            flightEngine.showMessage("ARMED 1X MISSILE (-30.0 CR)")
            soundSynth.playBeep(true)
        }
        return true
    }

    fun purchaseEquipment(equipId: String, costDeciCr: Long, minTech: Int): Boolean {
        if (commander.cashDeciCredits < costDeciCr) {
            soundSynth.playBeep(false)
            return false
        }
        if (currentSystem.techLevel < minTech) {
            soundSynth.playBeep(false)
            return false
        }
        commander.cashDeciCredits -= costDeciCr
        viewModelScope.launch {
            val updated = shipUpgradeRepository.purchaseEquipment(equipId, costDeciCr)
            syncCommanderWithShipUpgrade(updated)
            flightEngine.showMessage("EQUIPMENT FITTED: ${equipId.uppercase()} (-${"%.1f".format(costDeciCr / 10.0)} CR)")
            soundSynth.playBeep(true)
        }
        return true
    }

    fun refuelShip(): Boolean {
        val missingFuel = (70 - commander.fuelDeciLy).coerceAtLeast(0)
        if (missingFuel <= 0) return false
        val cost = missingFuel * 1L
        if (commander.cashDeciCredits < cost) {
            soundSynth.playBeep(false)
            return false
        }
        commander.cashDeciCredits -= cost
        commander.fuelDeciLy = 70
        flightEngine.showMessage("REFUELED TO 7.0 LIGHT YEARS (-${"%.1f".format(cost / 10.0)} CR)")
        soundSynth.playBeep(true)
        return true
    }
}
