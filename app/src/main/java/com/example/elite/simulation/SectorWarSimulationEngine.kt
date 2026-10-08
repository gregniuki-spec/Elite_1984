package com.example.elite.simulation

import com.example.elite.math3d.Vector3
import com.example.elite.model.BotStrategy
import com.example.elite.model.CombatRank
import com.example.elite.model.FleetFaction
import com.example.elite.model.FleetWarStatistics
import com.example.elite.model.SimulatedPilot
import com.example.elite.model.TacticalBotOpponent
import com.example.elite.ships.ShipBlueprint
import com.example.elite.ships.ShipBlueprints
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real-time Sector Simulation Engine supporting up to 100 AI Bot Opponents
 * and simulated real-time networked players.
 *
 * Implements:
 * - Autonomous multi-agent strategic decision trees (fleeing, wolfpacks, convoy escort, intercept, alpha strikes).
 * - Multi-faction sector tactical warfare (Navy vs Pirates vs Traders vs Thargoid Swarm).
 * - Real-time simulated players alongside 100 bot opponents with ping, ranking, and combat chatter.
 * - Spatial querying and dogfight physics in 3D coordinate space.
 * - Dynamic event logs, combat kill feeds, and tactical telemetry.
 */
class SectorWarSimulationEngine {

    companion object {
        const val MAX_BOTS = 100

        private val HUMAN_CALLSIGNS = listOf(
            "CMDR JAMESON", "CMDR VANCE", "CMDR AVALON", "CMDR STARFIRE",
            "CMDR BRABEN", "CMDR BELL", "CMDR CORIOLIS", "CMDR NOVA_7",
            "CMDR HELIOS", "CMDR ECLIPSE", "CMDR VALKYRIE", "CMDR ZEPHYR"
        )

        private val BOT_FIRST_NAMES = listOf(
            "Alpha", "Bravo", "Viper", "Ghost", "Stalker", "Phantom", "Marauder",
            "Reaper", "Predator", "Iron", "Kestrel", "Vector", "Shadow", "Rogue",
            "Titan", "Blaze", "Fury", "Striker", "Storm", "Raven", "Spectre",
            "Talon", "Havoc", "Vortex", "Grim", "Apex", "Zenith", "Dagger",
            "Scythe", "Hydra", "Wraith", "Blade", "Echo", "Thunder", "Inferno"
        )

        private val CARGO_ITEMS = listOf(
            "Food", "Textiles", "Computers", "Rare Minerals", "Firearms",
            "Radioactives", "Liquor/Wines", "Gold", "Platinum", "Gem-Stones"
        )
    }

    private val _botOpponents = mutableListOf<TacticalBotOpponent>()
    val botOpponents: List<TacticalBotOpponent> get() = _botOpponents

    private val _combatCombatFeed = mutableListOf<String>()
    val combatCombatFeed: List<String> get() = _combatCombatFeed

    private val _statsFlow = MutableStateFlow(
        FleetWarStatistics(
            totalBots = 0,
            activeCount = 0,
            destroyedCount = 0,
            navyCount = 0,
            outlawCount = 0,
            traderCount = 0,
            alienCount = 0,
            realPlayersCount = 0,
            topAceCallsign = "None",
            topAceKills = 0
        )
    )
    val statsFlow: StateFlow<FleetWarStatistics> = _statsFlow.asStateFlow()

    // Simulation settings
    var isSimulationRunning: Boolean = true
    var simulationSpeed: Float = 1.0f
    var playerControlledEntityId: Int? = 0 // Player Jameson ID
    var isAutoPlayEnabled: Boolean = true

    fun toggleAutoPlay(): Boolean {
        isAutoPlayEnabled = !isAutoPlayEnabled
        val status = if (isAutoPlayEnabled) "ENABLED (AUTONOMOUS AI COMMAND)" else "DISABLED (MANUAL OBSERVER)"
        addFeedMessage("COMMANDER JAMESON AUTO-PLAY: $status")
        return isAutoPlayEnabled
    }

    init {
        initializeSectorFleet(targetCount = MAX_BOTS)
    }

    /**
     * Initializes up to 100 AI Bot Opponents with diverse ship blueprints,
     * factions, ranks, strategy profiles, and positions across the space arena.
     */
    fun initializeSectorFleet(targetCount: Int = MAX_BOTS) {
        _botOpponents.clear()
        _combatCombatFeed.clear()
        _combatCombatFeed.add("SECTOR SIMULATION INITIALIZED: $targetCount UNITS ONLINE")

        val shipPool = listOf(
            ShipBlueprints.COBRA_MK_3,
            ShipBlueprints.VIPER,
            ShipBlueprints.MAMBA,
            ShipBlueprints.SIDEWINDER,
            ShipBlueprints.KRAIT,
            ShipBlueprints.ASP_MK_2,
            ShipBlueprints.GECKO,
            ShipBlueprints.FER_DE_LANCE,
            ShipBlueprints.PYTHON,
            ShipBlueprints.ANACONDA,
            ShipBlueprints.MORAY,
            ShipBlueprints.THARGOID,
            ShipBlueprints.THARGON
        )

        val ranks = CombatRank.entries.toTypedArray()
        val factions = FleetFaction.entries.toTypedArray()

        for (i in 0 until targetCount) {
            val isHuman = i < HUMAN_CALLSIGNS.size
            val faction = when {
                i == 0 -> FleetFaction.GALACTIC_NAVY // Player Jameson
                i in 1..4 -> FleetFaction.GALACTIC_NAVY
                i in 5..8 -> FleetFaction.PIRATE_CLAN
                i in 9..11 -> FleetFaction.MERCHANT_GUILD
                i % 5 == 0 -> FleetFaction.THARGOID_SWARM
                i % 4 == 0 -> FleetFaction.PIRATE_CLAN
                i % 3 == 0 -> FleetFaction.GALACTIC_NAVY
                i % 2 == 0 -> FleetFaction.MERCHANT_GUILD
                else -> FleetFaction.MERCENARY_CORPS
            }

            val pilotCallsign = if (isHuman) {
                HUMAN_CALLSIGNS[i]
            } else {
                "${BOT_FIRST_NAMES[Random.nextInt(BOT_FIRST_NAMES.size)]}-${100 + i}"
            }

            val pilotRank = ranks[Random.nextInt(ranks.size)]
            val pilot = SimulatedPilot(
                id = "pilot_$i",
                callsign = pilotCallsign,
                isRealtimeHumanPlayer = isHuman,
                rating = pilotRank,
                faction = faction
            )

            // Select ship blueprint matching faction & role
            val blueprint = when (faction) {
                FleetFaction.THARGOID_SWARM -> if (Random.nextBoolean()) ShipBlueprints.THARGOID else ShipBlueprints.THARGON
                FleetFaction.GALACTIC_NAVY -> if (Random.nextBoolean()) ShipBlueprints.VIPER else ShipBlueprints.ASP_MK_2
                FleetFaction.PIRATE_CLAN -> listOf(ShipBlueprints.MAMBA, ShipBlueprints.SIDEWINDER, ShipBlueprints.KRAIT)[Random.nextInt(3)]
                FleetFaction.MERCHANT_GUILD -> listOf(ShipBlueprints.COBRA_MK_3, ShipBlueprints.PYTHON, ShipBlueprints.ANACONDA)[Random.nextInt(3)]
                FleetFaction.MERCENARY_CORPS -> listOf(ShipBlueprints.FER_DE_LANCE, ShipBlueprints.MORAY, ShipBlueprints.GECKO)[Random.nextInt(3)]
            }

            // Assign strategy directive
            val strategy = when (faction) {
                FleetFaction.THARGOID_SWARM -> BotStrategy.BERSERK_ASSAULT
                FleetFaction.PIRATE_CLAN -> if (Random.nextBoolean()) BotStrategy.AGGRESSIVE_INTERCEPT else BotStrategy.SWARM_AMBUSH
                FleetFaction.GALACTIC_NAVY -> if (Random.nextBoolean()) BotStrategy.PATROL_RECON else BotStrategy.DEFENSIVE_ESCORT
                FleetFaction.MERCHANT_GUILD -> BotStrategy.CONVOY_TRADER
                FleetFaction.MERCENARY_CORPS -> if (Random.nextBoolean()) BotStrategy.HIT_AND_RUN else BotStrategy.AGGRESSIVE_INTERCEPT
            }

            // Cluster initial positions in sector space rings (-3500 to +3500 m)
            val ringRadius = when (faction) {
                FleetFaction.THARGOID_SWARM -> Random.nextFloat() * 1400f + 2000f
                FleetFaction.PIRATE_CLAN -> Random.nextFloat() * 1200f + 1200f
                FleetFaction.GALACTIC_NAVY -> Random.nextFloat() * 1000f + 600f
                FleetFaction.MERCHANT_GUILD -> Random.nextFloat() * 800f + 400f
                FleetFaction.MERCENARY_CORPS -> Random.nextFloat() * 1500f + 1000f
            }

            val angle = Random.nextFloat() * (Math.PI * 2.0).toFloat()
            val elevation = (Random.nextFloat() - 0.5f) * 900f
            val posX = cos(angle) * ringRadius
            val posZ = sin(angle) * ringRadius + 800f
            val posY = elevation

            val maxShield = when (blueprint) {
                ShipBlueprints.ANACONDA -> 400f
                ShipBlueprints.PYTHON -> 280f
                ShipBlueprints.THARGOID -> 260f
                ShipBlueprints.FER_DE_LANCE -> 180f
                ShipBlueprints.ASP_MK_2 -> 160f
                ShipBlueprints.VIPER -> 140f
                ShipBlueprints.COBRA_MK_3 -> 120f
                ShipBlueprints.MAMBA -> 100f
                ShipBlueprints.SIDEWINDER -> 60f
                ShipBlueprints.THARGON -> 40f
                else -> 90f
            }

            val maxHull = maxShield * 0.8f

            _botOpponents.add(
                TacticalBotOpponent(
                    id = i,
                    name = pilotCallsign,
                    pilot = pilot,
                    blueprint = blueprint,
                    faction = faction,
                    strategy = strategy,
                    position = Vector3(posX, posY, posZ),
                    shields = maxShield,
                    hull = maxHull,
                    maxShields = maxShield,
                    maxHull = maxHull,
                    cargoType = if (faction == FleetFaction.MERCHANT_GUILD) CARGO_ITEMS[Random.nextInt(CARGO_ITEMS.size)] else null
                )
            )
        }

        recalculateStats()
    }

    /**
     * Executes real-time update tick for all 100 AI bots and simulated players.
     */
    fun update(dtRaw: Float) {
        if (!isSimulationRunning) return
        val dt = (dtRaw * simulationSpeed).coerceIn(0.005f, 0.15f)

        // 1. Process Strategy Behaviors, Target Selection, and Dogfight Maneuvers
        for (bot in _botOpponents) {
            if (!bot.isAlive) continue

            // If this is the player's ship and auto-play is disabled, skip AI autonomy
            if (bot.id == playerControlledEntityId && !isAutoPlayEnabled) {
                continue
            }

            // Passive Shield Regeneration
            bot.healOrRecharge(2.5f * dt)

            // Reduce laser cooldown
            if (bot.laserCooldown > 0f) {
                bot.laserCooldown = (bot.laserCooldown - dt).coerceAtLeast(0f)
                if (bot.laserCooldown <= 0f) {
                    bot.isFiringLaser = false
                }
            }

            // Find or validate combat target based on faction strategy
            updateTargetForBot(bot)

            // Execute tactical movement according to strategy
            executeStrategyMovement(bot, dt)
        }

        // 2. Process Combat Intercepts & Laser Engagements
        for (bot in _botOpponents) {
            if (!bot.isAlive || bot.currentTargetId == null) continue

            // If player ship and auto-play disabled, skip auto-fire
            if (bot.id == playerControlledEntityId && !isAutoPlayEnabled) {
                continue
            }

            val target = _botOpponents.find { it.id == bot.currentTargetId && it.isAlive }
            if (target == null) {
                bot.currentTargetId = null
                continue
            }

            val dist = bot.position.distanceTo(target.position)
            val weaponRange = 900f

            if (dist < weaponRange && bot.laserCooldown <= 0f) {
                // Fire laser burst
                bot.isFiringLaser = true
                bot.laserCooldown = Random.nextFloat() * 0.6f + 0.4f

                val hitChance = (0.55f + (bot.pilot.rating.scoreNeeded / 8000f)).coerceIn(0.40f, 0.90f)
                if (Random.nextFloat() < hitChance) {
                    val damage = Random.nextFloat() * 18f + 8f
                    val destroyed = target.takeDamage(damage)

                    target.lastCombatEvent = "HIT by ${bot.name} [-${damage.toInt()}]"

                    if (destroyed) {
                        bot.kills++
                        bot.creditsScore += target.blueprint.bounty * 10
                        addFeedMessage("${bot.name} [${bot.faction.callsign}] destroyed ${target.name} [${target.faction.callsign}] (+${target.blueprint.bounty} CR)")
                        bot.currentTargetId = null
                    }
                }
            }
        }

        recalculateStats()
    }

    /**
     * Resolves target for a bot opponent based on faction alignments and active strategy.
     */
    private fun updateTargetForBot(bot: TacticalBotOpponent) {
        // If current target is dead or out of range, clear
        if (bot.currentTargetId != null) {
            val curr = _botOpponents.find { it.id == bot.currentTargetId }
            if (curr == null || !curr.isAlive || bot.position.distanceTo(curr.position) > 2800f) {
                bot.currentTargetId = null
            }
        }

        if (bot.currentTargetId != null) return

        // Strategy-driven targeting
        when (bot.strategy) {
            BotStrategy.AGGRESSIVE_INTERCEPT,
            BotStrategy.BERSERK_ASSAULT,
            BotStrategy.HIT_AND_RUN -> {
                // Find closest hostile faction craft
                val candidate = _botOpponents.asSequence()
                    .filter { it.isAlive && it.id != bot.id && bot.faction.isHostileTo(it.faction) }
                    .minByOrNull { bot.position.distanceTo(it.position) }
                bot.currentTargetId = candidate?.id
            }

            BotStrategy.SWARM_AMBUSH -> {
                // Prefer isolated freighters or weak targets
                val candidate = _botOpponents.asSequence()
                    .filter { it.isAlive && it.id != bot.id && bot.faction.isHostileTo(it.faction) }
                    .filter { it.shields < 80f || it.blueprint == ShipBlueprints.PYTHON || it.blueprint == ShipBlueprints.ANACONDA }
                    .minByOrNull { bot.position.distanceTo(it.position) }
                bot.currentTargetId = candidate?.id
            }

            BotStrategy.DEFENSIVE_ESCORT -> {
                // Defend nearby allied merchants under fire
                val threatenedAlly = _botOpponents.asSequence()
                    .filter { it.isAlive && it.id != bot.id && it.faction == bot.faction && it.shields < it.maxShields }
                    .firstOrNull()

                if (threatenedAlly != null) {
                    val attacker = _botOpponents.find { it.isAlive && it.currentTargetId == threatenedAlly.id }
                    bot.currentTargetId = attacker?.id
                } else {
                    // Otherwise engage any hostiles threatening patrol zone
                    val candidate = _botOpponents.asSequence()
                        .filter { it.isAlive && it.id != bot.id && bot.faction.isHostileTo(it.faction) }
                        .minByOrNull { bot.position.distanceTo(it.position) }
                    bot.currentTargetId = candidate?.id
                }
            }

            BotStrategy.CONVOY_TRADER -> {
                // Traders only retaliate if attacked directly
                val attacker = _botOpponents.find { it.isAlive && it.currentTargetId == bot.id }
                bot.currentTargetId = attacker?.id
            }

            BotStrategy.PATROL_RECON -> {
                // Intercept any illegal outlaws or alien invaders
                val candidate = _botOpponents.asSequence()
                    .filter { it.isAlive && it.id != bot.id && (it.faction == FleetFaction.PIRATE_CLAN || it.faction == FleetFaction.THARGOID_SWARM) }
                    .minByOrNull { bot.position.distanceTo(it.position) }
                bot.currentTargetId = candidate?.id
            }
        }
    }

    /**
     * Executes 3D movement and flight maneuvering based on tactical strategy.
     */
    private fun executeStrategyMovement(bot: TacticalBotOpponent, dt: Float) {
        val speedBase = bot.blueprint.maxSpeed * bot.strategy.preferredSpeedMult
        val target = bot.currentTargetId?.let { id -> _botOpponents.find { it.id == id && it.isAlive } }

        if (target != null) {
            val delta = target.position - bot.position
            val dist = delta.length()
            val dir = delta.normalized()

            if (bot.strategy == BotStrategy.CONVOY_TRADER && bot.shields < 60f) {
                // Flee in opposite direction
                val fleeDir = dir * -1f
                bot.velocity = fleeDir * (speedBase * 1.3f)
            } else if (dist > 350f) {
                // Close the distance
                bot.velocity = dir * speedBase
            } else if (dist < 180f) {
                // Strafe / break away to avoid collision
                val perp = Vector3(-dir.y, dir.x, dir.z).normalized()
                bot.velocity = (dir * 0.3f + perp * 0.7f).normalized() * speedBase
            } else {
                // Circle target in dogfight orbit
                val perp = Vector3(-dir.z, 0f, dir.x).normalized()
                bot.velocity = perp * speedBase
            }

            // Authentic 3D banking towards movement direction
            val targetYaw = -atan2(bot.velocity.x, bot.velocity.z)
            val targetPitch = atan2(bot.velocity.y, sqrt(bot.velocity.x * bot.velocity.x + bot.velocity.z * bot.velocity.z))
            bot.rotation = Vector3(targetPitch, targetYaw, targetYaw * 0.7f)

        } else {
            // Patrol or cruise waypoint
            val wander = Vector3(
                sin(System.currentTimeMillis() * 0.0005f + bot.id) * 30f,
                cos(System.currentTimeMillis() * 0.0004f + bot.id) * 15f,
                cos(System.currentTimeMillis() * 0.0005f + bot.id) * 30f
            )
            bot.velocity = wander.normalized() * (speedBase * 0.6f)
        }

        // Apply position update and sector bounding clamp
        bot.position = bot.position + bot.velocity * (12f * dt)

        // Sector wrapping / boundary reflection
        val boundary = 4200f
        if (bot.position.length() > boundary) {
            bot.position = bot.position * 0.96f
            bot.velocity = bot.velocity * -0.5f
        }
    }

    /**
     * Changes strategy order dynamically for an individual bot or whole faction wing.
     */
    fun setStrategyForFaction(faction: FleetFaction, newStrategy: BotStrategy) {
        var count = 0
        for (bot in _botOpponents) {
            if (bot.faction == faction && bot.isAlive) {
                bot.strategy = newStrategy
                bot.currentTargetId = null
                count++
            }
        }
        addFeedMessage("TACTICAL COMMAND: Assigned ${newStrategy.title} to $count [${faction.callsign}] units")
    }

    /**
     * Commands an individual bot by ID.
     */
    fun setBotStrategy(botId: Int, newStrategy: BotStrategy) {
        val bot = _botOpponents.find { it.id == botId } ?: return
        bot.strategy = newStrategy
        bot.currentTargetId = null
        addFeedMessage("DIRECT ORDER: ${bot.name} assigned to ${newStrategy.title}")
    }

    /**
     * Respawns fallen bots or reinforces sector with additional waves up to MAX_BOTS.
     */
    fun reinforceFleet(countToAdd: Int = 20) {
        val deadBots = _botOpponents.filter { !it.isAlive }
        var revived = 0
        for (dead in deadBots) {
            if (revived >= countToAdd) break
            dead.isDestroyed = false
            dead.shields = dead.maxShields
            dead.hull = dead.maxHull
            dead.position = Vector3(
                (Random.nextFloat() - 0.5f) * 3000f,
                (Random.nextFloat() - 0.5f) * 600f,
                Random.nextFloat() * 2000f + 600f
            )
            dead.currentTargetId = null
            revived++
        }

        addFeedMessage("SECTOR REINFORCEMENTS: $revived ships hyperspaced into combat zone!")
        recalculateStats()
    }

    private fun addFeedMessage(msg: String) {
        _combatCombatFeed.add(0, msg)
        if (_combatCombatFeed.size > 25) {
            _combatCombatFeed.removeAt(_combatCombatFeed.size - 1)
        }
    }

    private fun recalculateStats() {
        val total = _botOpponents.size
        val active = _botOpponents.count { it.isAlive }
        val dead = total - active
        val navy = _botOpponents.count { it.isAlive && it.faction == FleetFaction.GALACTIC_NAVY }
        val outlaw = _botOpponents.count { it.isAlive && it.faction == FleetFaction.PIRATE_CLAN }
        val trader = _botOpponents.count { it.isAlive && it.faction == FleetFaction.MERCHANT_GUILD }
        val alien = _botOpponents.count { it.isAlive && it.faction == FleetFaction.THARGOID_SWARM }
        val realPlayers = _botOpponents.count { it.isAlive && it.pilot.isRealtimeHumanPlayer }

        val topAce = _botOpponents.maxByOrNull { it.kills }

        _statsFlow.value = FleetWarStatistics(
            totalBots = total,
            activeCount = active,
            destroyedCount = dead,
            navyCount = navy,
            outlawCount = outlaw,
            traderCount = trader,
            alienCount = alien,
            realPlayersCount = realPlayers,
            topAceCallsign = topAce?.name ?: "None",
            topAceKills = topAce?.kills ?: 0
        )
    }
}
