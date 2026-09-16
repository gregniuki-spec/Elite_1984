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
import com.example.elite.market.MarketEngine
import com.example.elite.model.Commodity
import com.example.elite.model.CommanderState
import com.example.elite.model.GameScreen
import com.example.elite.model.SystemData
import com.example.elite.sourceviewer.SourceRepository
import com.example.elite.universe.GalaxyGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EliteViewModel(application: Application) : AndroidViewModel(application) {

    val soundSynth = BbcSoundSynth(viewModelScope)
    val sourceRepository = SourceRepository(application)

    // Commander Profile
    val commander = CommanderState()

    // Galaxy 1 procedural data (256 systems)
    val systems: List<SystemData> = GalaxyGenerator.generateGalaxy(1)

    // Current and targeted systems (Default to Lave in Galaxy 1)
    var currentSystem by mutableStateOf(systems.find { it.name == "Lave" } ?: systems[0])
    var targetSystem by mutableStateOf(systems.find { it.name == "Lave" } ?: systems[0])

    // Market items
    val currentMarket = mutableStateListOf<Commodity>()

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
                refreshMarket()
                currentScreen = GameScreen.SPACE_FLIGHT
            }
        )

        refreshMarket()
        startFlightLoop()
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
        currentMarket.clear()
        currentMarket.addAll(MarketEngine.createMarketForSystem(currentSystem))
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

                if (currentScreen == GameScreen.SPACE_FLIGHT || flightEngine.isDockingComputerActive) {
                    flightEngine.update(dt)
                }

                delay(16L) // Target ~60 FPS update
            }
        }
    }
}
