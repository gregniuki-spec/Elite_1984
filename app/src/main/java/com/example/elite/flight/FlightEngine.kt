package com.example.elite.flight

import com.example.elite.audio.BbcSoundSynth
import com.example.elite.math3d.Matrix3x3
import com.example.elite.math3d.Vector3
import com.example.elite.model.CombatRank
import com.example.elite.model.CommanderState
import com.example.elite.model.LegalStatus
import com.example.elite.model.SystemData
import com.example.elite.ships.ShipBlueprint
import com.example.elite.ships.ShipBlueprints
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class SpaceEntity(
    val id: Long,
    val blueprint: ShipBlueprint,
    var position: Vector3,
    var rotation: Vector3 = Vector3(0f, 0f, 0f), // pitch, yaw, roll
    var velocity: Vector3 = Vector3(0f, 0f, 0f),
    var isHostile: Boolean = false,
    var shields: Float = 100f,
    var isDestroyed: Boolean = false,
    var cargoCommodityId: Int? = null
)

enum class CombatScenario(
    val title: String,
    val description: String,
    val rewardCr: Int,
    val difficulty: String
) {
    PIRATE_AMBUSH("PIRATE RAIDERS", "Sidewinder & Mamba pirate wing hunting merchant convoys.", 180, "STANDARD"),
    THARGOID_INVASION("THARGOID MOTHERSHIP", "Terrifying alien Mothership deploying swarming Thargons.", 450, "EXTREME"),
    VIPER_POLICE_CHALLENGE("POLICE INTERCEPT", "Galactic Patrol Vipers responding to illegal activity.", 240, "HARD"),
    ANACONDA_CRUISER_SIEGE("HEAVY CRUSADER", "Armored Anaconda heavy battlecruiser with fighter wing.", 550, "VERY HARD"),
    SECTOR_WAR_100_BOTS("100-BOT SECTOR WAR", "Massive multi-faction war with up to 100 AI bots and players.", 1000, "WARZONE")
}

data class Star(
    var x: Float,
    var y: Float,
    var z: Float
)

data class LaserShot(
    val startLeft: Vector3,
    val startRight: Vector3,
    val target: Vector3,
    var lifetimeMs: Long = 120L
)

enum class CockpitView(val label: String, val yawAngle: Float) {
    FRONT("FRONT VIEW", 0f),
    REAR("REAR VIEW", Math.PI.toFloat()),
    LEFT("LEFT VIEW", (Math.PI / 2.0).toFloat()),
    RIGHT("RIGHT VIEW", (-Math.PI / 2.0).toFloat())
}

enum class NavTarget(val title: String) {
    STATION("STATION"),
    PLANET("PLANET"),
    SUN("SUN")
}

enum class AutoPlayMode(val title: String, val badge: String) {
    OFF("MANUAL", "OFF"),
    COMBAT_ACE("AUTO COMBAT", "ACE"),
    TRADER_EXPLORER("AUTO TRADE/MINE", "TRADER"),
    SURVIVAL_DEFENSE("AUTO DEFEND", "DEFEND"),
    TRAINED_NEURAL_BOT("TRAINED AI BRAIN", "NEURAL")
}

class FlightEngine(
    val commander: CommanderState,
    val soundSynth: BbcSoundSynth,
    var onDocked: () -> Unit = {},
    var onHyperspaceComplete: (SystemData) -> Unit = {},
    var onLaserFiredCallback: (Boolean) -> Unit = {},
    var onMissileFiredCallback: () -> Unit = {},
    var onEcmSuccessCallback: () -> Unit = {},
    var onBountyEarnedCallback: (Double) -> Unit = {}
) {
    // Flight state
    var speed: Float = 0f
    val maxSpeed: Float = 40f
    var rollRate: Float = 0f
    var pitchRate: Float = 0f
    var currentPitch: Float = 0f
    var currentRoll: Float = 0f

    // 4-Way Cockpit Views (Front, Rear, Left, Right)
    var currentView: CockpitView = CockpitView.FRONT

    // 3D Stardust (subroutine STARS in elite-source.asm)
    val stars = Array(64) {
        Star(
            x = (Random.nextFloat() * 800f) - 400f,
            y = (Random.nextFloat() * 600f) - 300f,
            z = Random.nextFloat() * 1000f + 50f
        )
    }

    // Active entities in space
    val entities = mutableListOf<SpaceEntity>()
    val activeLasers = mutableListOf<LaserShot>()
    val debrisShards = mutableListOf<com.example.elite.math3d.WireframeShard>()

    // Combat & Lock-on
    var lockedTarget: SpaceEntity? = null
    var isMissileArmed: Boolean = false
    var incomingMissileCountdownMs: Long = 0L
    var isWitchSpace: Boolean = false
    var stationEntity: SpaceEntity? = null
    var planetEntity: SpaceEntity? = null
    var sunEntity: SpaceEntity? = null
    var navTarget: NavTarget = NavTarget.STATION

    // Autopilot, Auto-Play AI & Hyperspace
    var isDockingComputerActive: Boolean = false
    var autoPlayMode: AutoPlayMode = AutoPlayMode.OFF
    val isAutoPlayActive: Boolean get() = autoPlayMode != AutoPlayMode.OFF
    var autoPlayStatusText: String = ""
    var autoPlayLaserCooldown: Float = 0f
    var autoPlayMissileCooldown: Float = 0f
    var hyperspaceCountdown: Int = -1

    var statusMessage: String = ""
    var statusMessageTimeMs: Long = 0L

    // Screen flash effects (Energy bomb, Re-entry friction)
    var energyBombFlashAlpha: Float = 0f
    var isAtmosphericFriction: Boolean = false
    var isSolarScooping: Boolean = false

    init {
        resetSpaceEnvironment()
    }

    fun getCameraMatrix(): Matrix3x3 {
        return if (currentView.yawAngle == 0f) {
            Matrix3x3.IDENTITY
        } else {
            Matrix3x3.rotationXYZ(0f, currentView.yawAngle, 0f)
        }
    }

    fun cycleCockpitView() {
        currentView = when (currentView) {
            CockpitView.FRONT -> CockpitView.REAR
            CockpitView.REAR -> CockpitView.LEFT
            CockpitView.LEFT -> CockpitView.RIGHT
            CockpitView.RIGHT -> CockpitView.FRONT
        }
        soundSynth.playBeep(true)
        showMessage(currentView.label)
    }

    fun setCockpitView(view: CockpitView) {
        currentView = view
        soundSynth.playBeep(true)
        showMessage(view.label)
    }

    fun cycleNavTarget() {
        navTarget = when (navTarget) {
            NavTarget.STATION -> NavTarget.PLANET
            NavTarget.PLANET -> NavTarget.SUN
            NavTarget.SUN -> NavTarget.STATION
        }
        soundSynth.playBeep(true)
        showMessage("COMPASS TARGET: ${navTarget.title}")
    }

    fun getNavTargetEntity(): SpaceEntity? {
        return when (navTarget) {
            NavTarget.STATION -> stationEntity
            NavTarget.PLANET -> planetEntity
            NavTarget.SUN -> sunEntity
        }
    }

    fun showMessage(msg: String) {
        statusMessage = msg
        statusMessageTimeMs = System.currentTimeMillis() + 3000L
    }

    fun resetSpaceEnvironment() {
        entities.clear()
        activeLasers.clear()
        debrisShards.clear()
        speed = 12f
        rollRate = 0f
        pitchRate = 0f
        isDockingComputerActive = false
        energyBombFlashAlpha = 0f
        isAtmosphericFriction = false
        isSolarScooping = false
        soundSynth.stopBlueDanube()

        // 1. Spawn Space Station (Dodecahedron in high-tech systems, Coriolis otherwise)
        val stationBp = if ((commander.currentSystemId % 3) == 0) ShipBlueprints.DODEC else ShipBlueprints.CORIOLIS
        val station = SpaceEntity(
            id = 1L,
            blueprint = stationBp,
            position = Vector3(0f, 0f, 1600f),
            shields = 1000f
        )
        stationEntity = station
        entities.add(station)

        // 2. Spawn 3D Wireframe Planet
        val planet = SpaceEntity(
            id = 2L,
            blueprint = ShipBlueprints.PLANET,
            position = Vector3(1400f, 500f, 3800f),
            shields = 99999f
        )
        planetEntity = planet
        entities.add(planet)

        // 3. Spawn 3D Wireframe Sun
        val sun = SpaceEntity(
            id = 3L,
            blueprint = ShipBlueprints.SUN,
            position = Vector3(-3200f, -800f, 5200f),
            shields = 99999f
        )
        sunEntity = sun
        entities.add(sun)

        // 4. Spawn diverse ships, traders, outlaws, and alien craft
        val trafficPool = listOf(
            ShipBlueprints.SIDEWINDER to true,
            ShipBlueprints.MAMBA to true,
            ShipBlueprints.KRAIT to true,
            ShipBlueprints.GECKO to true,
            ShipBlueprints.VIPER to false,
            ShipBlueprints.ASP_MK_2 to false,
            ShipBlueprints.MORAY to false,
            ShipBlueprints.FER_DE_LANCE to false,
            ShipBlueprints.PYTHON to false,
            ShipBlueprints.ANACONDA to false,
            ShipBlueprints.THARGOID to true,
            ShipBlueprints.ASTEROID to false,
            ShipBlueprints.CANISTER to false
        )

        for (i in 0 until 5) {
            val (bp, defaultHostile) = trafficPool[Random.nextInt(trafficPool.size)]
            val isHostile = if (bp == ShipBlueprints.VIPER || bp == ShipBlueprints.CANISTER || bp == ShipBlueprints.ASTEROID) {
                false
            } else if (bp == ShipBlueprints.THARGOID || bp == ShipBlueprints.MAMBA) {
                true
            } else {
                defaultHostile && Random.nextBoolean()
            }

            entities.add(
                SpaceEntity(
                    id = Random.nextLong(),
                    blueprint = bp,
                    position = Vector3(
                        (Random.nextFloat() - 0.5f) * 1400f,
                        (Random.nextFloat() - 0.5f) * 800f,
                        Random.nextFloat() * 1200f + 600f
                    ),
                    isHostile = isHostile,
                    shields = when (bp) {
                        ShipBlueprints.ASTEROID -> 40f
                        ShipBlueprints.ANACONDA -> 200f
                        ShipBlueprints.PYTHON -> 150f
                        ShipBlueprints.FER_DE_LANCE -> 120f
                        ShipBlueprints.THARGOID -> 160f
                        else -> 80f
                    }
                )
            )
        }
    }

    /**
     * Tap-to-Target: Tests touch coordinates against 3D projected entities.
     */
    fun selectTargetAt(tapX: Float, tapY: Float, viewWidth: Float, viewHeight: Float): SpaceEntity? {
        val camMatrix = getCameraMatrix()
        val cx = viewWidth / 2f
        val cy = viewHeight / 2f
        val focalLength = 320f

        var bestEntity: SpaceEntity? = null
        var bestDist = 80f // radius in pixels

        for (e in entities) {
            val camPos = camMatrix.transform(e.position)
            if (camPos.z > 20f) {
                val projScale = focalLength / camPos.z
                val sx = cx + camPos.x * projScale
                val sy = cy - camPos.y * projScale
                val d = kotlin.math.hypot(tapX - sx, tapY - sy)
                if (d < bestDist) {
                    bestDist = d
                    bestEntity = e
                }
            }
        }

        if (bestEntity != null) {
            lockedTarget = bestEntity
            soundSynth.playBeep(true)
            showMessage("TARGET: ${bestEntity.blueprint.name}")
        }
        return bestEntity
    }

    fun spawnExplosionDebris(center: Vector3, count: Int = 12) {
        val now = System.currentTimeMillis()
        for (i in 0 until count) {
            val angle = Random.nextFloat() * (Math.PI * 2).toFloat()
            val elevation = (Random.nextFloat() - 0.5f) * Math.PI.toFloat()
            val spd = Random.nextFloat() * 120f + 40f
            val vx = cos(angle) * cos(elevation) * spd
            val vy = sin(elevation) * spd
            val vz = sin(angle) * cos(elevation) * spd
            debrisShards.add(
                com.example.elite.math3d.WireframeShard(
                    pos = center,
                    velocity = Vector3(vx, vy, vz),
                    angle = Random.nextFloat() * 6.28f,
                    angularVel = (Random.nextFloat() - 0.5f) * 12f,
                    length = Random.nextFloat() * 25f + 10f,
                    expireTimeMs = now + 1800L
                )
            )
        }
    }

    fun update(deltaSeconds: Float) {
        val dt = deltaSeconds.coerceIn(0.001f, 0.1f)
        val now = System.currentTimeMillis()

        // Fade energy bomb flash
        if (energyBombFlashAlpha > 0f) {
            energyBombFlashAlpha = (energyBombFlashAlpha - 2f * dt).coerceAtLeast(0f)
        }

        // Update player pitch & roll
        currentPitch += pitchRate * dt
        currentRoll += rollRate * dt

        // Docking computer autopilot & Auto-Play AI
        if (isDockingComputerActive) {
            updateAutopilot(dt)
        } else if (isAutoPlayActive) {
            updateAutoPlay(dt)
        }

        // Stardust animation (STARS routine)
        val speedFactor = (speed + 2f) * 18f
        for (star in stars) {
            star.z -= speedFactor * dt

            // Parallax from pitch & roll
            val pX = star.x
            val pY = star.y
            val rCos = cos(rollRate * dt * 0.8f)
            val rSin = sin(rollRate * dt * 0.8f)
            star.x = pX * rCos - pY * rSin
            star.y = pX * rSin + pY * rCos + (pitchRate * 400f * dt)

            // Respawn star if it passes camera
            if (star.z < 20f || abs(star.x) > 600f || abs(star.y) > 500f) {
                star.z = 1000f + Random.nextFloat() * 200f
                star.x = (Random.nextFloat() * 800f) - 400f
                star.y = (Random.nextFloat() * 600f) - 300f
            }
        }

        // Clean expired laser shots
        activeLasers.removeAll { now > it.lifetimeMs }

        // Update debris shards
        debrisShards.removeAll { now > it.expireTimeMs }
        for (shard in debrisShards) {
            shard.pos = shard.pos + shard.velocity * dt - Vector3(0f, 0f, speed * 20f * dt)
            shard.angle += shard.angularVel * dt
        }

        // Planetary altitude and atmospheric friction
        planetEntity?.let { planet ->
            val distToPlanetCenter = planet.position.length()
            val planetRadius = 400f
            val altitudeDist = (distToPlanetCenter - planetRadius).coerceAtLeast(0f)
            commander.altitude = (altitudeDist / 30f).coerceIn(0f, 100f)

            if (commander.altitude < 15f && speed > 5f) {
                isAtmosphericFriction = true
                commander.cabinTemp = (commander.cabinTemp + 10f * dt).coerceAtMost(100f)
                if (Random.nextFloat() < 0.05f) {
                    showMessage("WARNING: ATMOSPHERIC FRICTION!")
                }
            } else {
                isAtmosphericFriction = false
            }
        }

        // Solar proximity & Fuel Scooping
        sunEntity?.let { sun ->
            val distToSun = sun.position.length()
            if (distToSun < 1500f) {
                isSolarScooping = true
                commander.cabinTemp = (commander.cabinTemp + 8f * dt).coerceAtMost(100f)
                if (commander.hasFuelScoops) {
                    if (commander.fuelDeciLy < 70) {
                        val fuelGain = (4f * dt).toInt()
                        commander.fuelDeciLy = (commander.fuelDeciLy + fuelGain.coerceAtLeast(1)).coerceAtMost(70)
                        if (Random.nextFloat() < 0.08f) {
                            showMessage("SOLAR FUEL SCOOP ACTIVE (+0.2 LY)")
                        }
                    }
                } else if (commander.cabinTemp > 80f) {
                    showMessage("WARNING: CABIN OVERHEATING!")
                }
            } else {
                isSolarScooping = false
                // Cool down cabin temp when away from stars/atmosphere
                if (commander.cabinTemp > 10f) {
                    commander.cabinTemp = (commander.cabinTemp - 3f * dt).coerceAtLeast(10f)
                }
            }
        }

        // Update entities
        val toRemove = mutableListOf<SpaceEntity>()
        val rotMatrix = Matrix3x3.rotationXYZ(-pitchRate * dt, 0f, -rollRate * dt)

        for (e in entities) {
            if (e.isDestroyed) {
                toRemove.add(e)
                continue
            }

            // Move entities relative to player speed and rotation
            var relPos = e.position - Vector3(0f, 0f, speed * 20f * dt)
            relPos = rotMatrix.transform(relPos)
            e.position = relPos

            // Space Station continuous axial rotation (Coriolis & Dodec)
            if (e.blueprint == ShipBlueprints.CORIOLIS || e.blueprint == ShipBlueprints.DODEC) {
                e.rotation = Vector3(
                    e.rotation.x,
                    e.rotation.y,
                    e.rotation.z + 0.38f * dt
                )

                // Check manual or autopilot docking into space station entrance slot
                if (relPos.z in 20f..180f && abs(relPos.x) < 45f && abs(relPos.y) < 45f) {
                    performDocking()
                    return
                }
            } else if (e.blueprint == ShipBlueprints.PLANET || e.blueprint == ShipBlueprints.SUN) {
                // Rotate planet / sun slowly
                e.rotation = Vector3(
                    e.rotation.x,
                    e.rotation.y + 0.05f * dt,
                    e.rotation.z
                )
            } else if (e.blueprint == ShipBlueprints.ASTEROID) {
                // 3D tumbling tumbling rocky body
                e.rotation = Vector3(
                    e.rotation.x + 0.25f * dt,
                    e.rotation.y + 0.45f * dt,
                    e.rotation.z + 0.18f * dt
                )
            } else if (e.blueprint == ShipBlueprints.CANISTER) {
                // Drifting tumbling cargo canister
                e.rotation = Vector3(
                    e.rotation.x + 0.15f * dt,
                    e.rotation.y + 0.3f * dt,
                    e.rotation.z
                )
                // Interactive Cargo & Mineral Scooping!
                val dist = e.position.length()
                if (dist < 75f && speed <= 28f) {
                    val scoopedId = e.cargoCommodityId ?: Random.nextInt(17)
                    val isPrecious = scoopedId in listOf(13, 14, 15)
                    if (isPrecious || commander.freeCargoSpace() > 0) {
                        e.isDestroyed = true
                        commander.cargo[scoopedId] = (commander.cargo[scoopedId] ?: 0) + 1
                        soundSynth.playBeep(true)
                        val commodityNames = listOf(
                            "Food", "Textiles", "Radioactives", "Slaves", "Liquor/Wines",
                            "Luxuries", "Narcotics", "Computers", "Machinery", "Alloys",
                            "Firearms", "Furs", "Minerals", "Gold", "Platinum", "Gem-Stones", "Alien Items"
                        )
                        val unit = if (scoopedId == 13 || scoopedId == 14) "kg" else if (scoopedId == 15) "g" else "t"
                        val name = commodityNames.getOrElse(scoopedId) { "Minerals" }
                        showMessage("SCOOPED: 1$unit $name!")
                    } else {
                        if (Random.nextFloat() < 0.1f) {
                            showMessage("CARGO HOLD FULL")
                        }
                    }
                }
            } else {
                // AI behavior & 3D Flight Maneuvers
                if (e.isHostile) {
                    // Turn towards player and fire lasers
                    val dir = (Vector3(0f, 0f, 0f) - relPos).normalized()
                    e.position = e.position + dir * (e.blueprint.maxSpeed * 0.5f * dt)

                    // Authentic 3D banking, pitch, and roll combat maneuvers
                    val targetPitch = kotlin.math.atan2(relPos.y, relPos.z).coerceIn(-1.2f, 1.2f)
                    val targetYaw = -kotlin.math.atan2(relPos.x, relPos.z).coerceIn(-1.5f, 1.5f)
                    val targetRoll = (targetYaw * 1.35f).coerceIn(-1.1f, 1.1f)

                    e.rotation = Vector3(
                        e.rotation.x + (targetPitch - e.rotation.x) * (2.2f * dt),
                        e.rotation.y + (targetYaw - e.rotation.y) * (2.2f * dt),
                        e.rotation.z + (targetRoll - e.rotation.z) * (2.2f * dt)
                    )

                    // Enemy shooting chance
                    if (relPos.z in 100f..800f && Random.nextFloat() < 0.03f) {
                        val incomingLaser = 8f
                        if (commander.forwardShield >= incomingLaser) {
                            commander.forwardShield -= incomingLaser
                        } else {
                            val shieldBleed = incomingLaser - commander.forwardShield
                            commander.forwardShield = 0f
                            // Armor plating mitigates hull damage
                            val armorMitigation = (commander.armorRatingPercent / 100f).coerceIn(0f, 0.5f)
                            val hullHit = shieldBleed * (1f - armorMitigation)
                            commander.currentHullIntegrity = (commander.currentHullIntegrity - hullHit).coerceAtLeast(0f)
                            commander.energyBanks = (commander.energyBanks - 6f).coerceAtLeast(0f)
                        }
                        soundSynth.playExplosion()
                        showMessage("WARNING: HOSTILE LASER HIT! [HULL ${commander.currentHullIntegrity.toInt()}/${commander.maxHullIntegrity.toInt()}]")
                    }

                    // Enemy missile launch chance
                    if (incomingMissileCountdownMs == 0L && relPos.length() in 300f..1500f && Random.nextFloat() < 0.005f) {
                        incomingMissileCountdownMs = System.currentTimeMillis() + 6000L
                        soundSynth.playMissileLaunch()
                        showMessage("WARNING: INCOMING MISSILE! USE ECM!")
                    }
                }
            }

            // Check distance limit for ships/debris
            if (e.position.z < -400f || e.position.length() > 6000f) {
                if (e.blueprint != ShipBlueprints.CORIOLIS && e.blueprint != ShipBlueprints.PLANET && e.blueprint != ShipBlueprints.SUN) {
                    toRemove.add(e)
                }
            }
        }
        entities.removeAll(toRemove)

        // Recharge shields and cool lasers (enhanced by Weapon Cooling Radiator Tier)
        if (commander.energyBanks > 20f) {
            if (commander.forwardShield < 100f) commander.forwardShield = (commander.forwardShield + 3f * dt).coerceAtMost(100f)
            if (commander.aftShield < 100f) commander.aftShield = (commander.aftShield + 3f * dt).coerceAtMost(100f)
        }
        if (commander.laserTemp > 0f) {
            val coolingSpeed = 8f * commander.weaponCoolingMultiplier
            commander.laserTemp = (commander.laserTemp - coolingSpeed * dt).coerceAtLeast(0f)
        }

        // Incoming hostile missile countdown check
        if (incomingMissileCountdownMs > 0L && System.currentTimeMillis() > incomingMissileCountdownMs) {
            incomingMissileCountdownMs = 0L
            soundSynth.playExplosion()
            val missileDmg = 40f
            if (commander.forwardShield >= missileDmg) {
                commander.forwardShield -= missileDmg
            } else {
                val rem = missileDmg - commander.forwardShield
                commander.forwardShield = 0f
                val armorMitigation = (commander.armorRatingPercent / 100f).coerceIn(0f, 0.5f)
                val hullHit = rem * (1f - armorMitigation)
                commander.currentHullIntegrity = (commander.currentHullIntegrity - hullHit).coerceAtLeast(0f)
                commander.energyBanks = (commander.energyBanks - rem * 0.5f).coerceAtLeast(0f)
            }
            showMessage("MISSILE IMPACT ON HULL! [${commander.currentHullIntegrity.toInt()}/${commander.maxHullIntegrity.toInt()}]")
        }

        // Auto target closest entity if none currently selected
        if (lockedTarget == null || lockedTarget?.isDestroyed == true) {
            lockedTarget = entities.filter {
                it.blueprint != ShipBlueprints.CORIOLIS &&
                it.blueprint != ShipBlueprints.PLANET &&
                it.blueprint != ShipBlueprints.SUN &&
                it.position.z > 50f
            }.minByOrNull { it.position.length() }
        }
    }

    fun fireLaser() {
        if (commander.laserTemp > 90f) {
            showMessage("LASER OVERHEATED!")
            soundSynth.playBeep(false)
            return
        }

        soundSynth.playLaser()
        commander.laserTemp = (commander.laserTemp + 12f).coerceAtMost(100f)

        // Add visual 3D laser vectors
        val targetPos = Vector3(0f, 0f, 800f)
        activeLasers.add(
            LaserShot(
                startLeft = Vector3(-40f, -25f, 20f),
                startRight = Vector3(40f, -25f, 20f),
                target = targetPos,
                lifetimeMs = System.currentTimeMillis() + 100L
            )
        )

        // Hit detection on target nearest crosshair
        val target = entities.filter { it.position.z in 50f..1200f }
            .firstOrNull {
                val screenX = (it.position.x / it.position.z) * 300f
                val screenY = (it.position.y / it.position.z) * 300f
                abs(screenX) < 60f && abs(screenY) < 60f
            }

        if (target != null) {
            onLaserFiredCallback(true)
            val laserDamage = commander.laserFront.power.toFloat()
            target.shields -= laserDamage
            soundSynth.playExplosion()

            if (target.shields <= 0f) {
                target.isDestroyed = true
                spawnExplosionDebris(target.position, 14)
                soundSynth.playExplosion()
                val bounty = target.blueprint.bounty
                if (bounty > 0) {
                    commander.cashDeciCredits += bounty * 10L
                    commander.killCount++
                    updateCombatRank()
                    onBountyEarnedCallback(bounty.toDouble())
                    showMessage("${target.blueprint.name} DESTROYED! BOUNTY: ${bounty}.0 CR")
                } else if (target.blueprint == ShipBlueprints.ASTEROID) {
                    val mineralPool = listOf(
                        12 to "Minerals",
                        9 to "Alloys",
                        13 to "Gold",
                        14 to "Platinum",
                        15 to "Gem-Stones"
                    )
                    val chosen = mineralPool[Random.nextInt(mineralPool.size)]
                    val spawnCount = if (commander.laserFront.power >= 45) 3 else 2
                    for (i in 0 until spawnCount) {
                        val offset = Vector3(
                            (Random.nextFloat() - 0.5f) * 70f,
                            (Random.nextFloat() - 0.5f) * 70f,
                            (Random.nextFloat() - 0.5f) * 70f
                        )
                        entities.add(
                            SpaceEntity(
                                id = Random.nextLong(),
                                blueprint = ShipBlueprints.CANISTER,
                                position = target.position + offset,
                                shields = 20f,
                                cargoCommodityId = chosen.first
                            )
                        )
                    }
                    showMessage("ASTEROID FRACTURED: ${chosen.second.uppercase()} DETECTED!")
                } else if (target.blueprint == ShipBlueprints.CANISTER) {
                    showMessage("CARGO DESTROYED")
                }

                // Drop cargo canister on ship kill
                if (bounty > 0 && Random.nextBoolean()) {
                    entities.add(
                        SpaceEntity(
                            id = Random.nextLong(),
                            blueprint = ShipBlueprints.CANISTER,
                            position = target.position,
                            shields = 20f
                        )
                    )
                }
            } else {
                showMessage("HIT ON ${target.blueprint.name}!")
            }
        } else {
            onLaserFiredCallback(false)
        }
    }

    fun armMissile() {
        if (commander.missiles <= 0) {
            showMessage("NO MISSILES IN PYLONS")
            soundSynth.playBeep(false)
            return
        }
        isMissileArmed = !isMissileArmed
        if (isMissileArmed) {
            soundSynth.playBeep(true)
            if (lockedTarget != null) {
                showMessage("MSL ARMED: TARGET LOCKED")
            } else {
                showMessage("MSL ARMED: SEARCHING")
            }
        } else {
            showMessage("MSL UNARMED")
        }
    }

    fun fireMissile() {
        if (commander.missiles <= 0) {
            showMessage("NO MISSILES IN PYLONS")
            soundSynth.playBeep(false)
            return
        }

        val target = lockedTarget
        if (target == null) {
            showMessage("NO TARGET LOCKED")
            soundSynth.playBeep(false)
            return
        }

        commander.missiles--
        isMissileArmed = false
        soundSynth.playMissileLaunch()
        onMissileFiredCallback()
        showMessage("MISSILE LAUNCHED!")

        // Direct hit on target
        target.shields -= 80f
        if (target.shields <= 0f) {
            target.isDestroyed = true
            spawnExplosionDebris(target.position, 16)
            soundSynth.playExplosion()
            val bounty = target.blueprint.bounty
            if (bounty > 0) {
                commander.cashDeciCredits += bounty * 10L
                commander.killCount++
                updateCombatRank()
                showMessage("TARGET DESTROYED! +${bounty}.0 CR")
            }
        }
    }

    fun triggerEnergyBomb() {
        if (!commander.hasEnergyBomb) {
            showMessage("ENERGY BOMB NOT FITTED")
            soundSynth.playBeep(false)
            return
        }

        commander.hasEnergyBomb = false
        energyBombFlashAlpha = 1.0f
        soundSynth.playExplosion()
        showMessage("ENERGY BOMB DETONATED!")

        // Destroy all hostile ships and asteroids within 3000m
        val targets = entities.filter {
            (it.isHostile || it.blueprint == ShipBlueprints.ASTEROID) &&
            it.position.length() < 3000f
        }
        for (v in targets) {
            v.isDestroyed = true
            spawnExplosionDebris(v.position, 16)
            if (v.blueprint.bounty > 0) {
                commander.cashDeciCredits += v.blueprint.bounty * 10L
                commander.killCount++
            }
        }
        updateCombatRank()
    }

    fun triggerEcm() {
        if (!commander.hasEcm) {
            showMessage("ECM SYSTEM NOT FITTED")
            soundSynth.playBeep(false)
            return
        }
        soundSynth.playHyperspace()
        if (incomingMissileCountdownMs > 0L) {
            incomingMissileCountdownMs = 0L
            onEcmSuccessCallback()
            showMessage("ECM: INCOMING MISSILE DESTROYED!")
        } else {
            showMessage("ECM COUNTERMEASURE TRANSMITTED")
        }
    }

    fun engageDockingComputer() {
        if (!commander.hasDockingComputer) {
            showMessage("DOCKING COMPUTERS NOT FITTED")
            soundSynth.playBeep(false)
            return
        }

        isDockingComputerActive = !isDockingComputerActive
        if (isDockingComputerActive) {
            showMessage("DOCKING COMPUTERS ON")
            soundSynth.startBlueDanube()
        } else {
            showMessage("MANUAL PILOT ENGAGED")
            soundSynth.stopBlueDanube()
        }
    }

    fun toggleAutoPlay(): AutoPlayMode {
        autoPlayMode = when (autoPlayMode) {
            AutoPlayMode.OFF -> AutoPlayMode.COMBAT_ACE
            AutoPlayMode.COMBAT_ACE -> AutoPlayMode.TRADER_EXPLORER
            AutoPlayMode.TRADER_EXPLORER -> AutoPlayMode.SURVIVAL_DEFENSE
            AutoPlayMode.SURVIVAL_DEFENSE -> AutoPlayMode.TRAINED_NEURAL_BOT
            AutoPlayMode.TRAINED_NEURAL_BOT -> AutoPlayMode.OFF
        }
        if (autoPlayMode != AutoPlayMode.OFF) {
            soundSynth.playBeep(true)
            showMessage("AUTO-PLAY: ${autoPlayMode.title}")
            autoPlayStatusText = "ENGAGED: ${autoPlayMode.title}"
        } else {
            soundSynth.playBeep(false)
            showMessage("AUTO-PLAY: DISENGAGED (MANUAL)")
            autoPlayStatusText = ""
            pitchRate = 0f
            rollRate = 0f
        }
        return autoPlayMode
    }

    fun setAutoPlay(mode: AutoPlayMode) {
        autoPlayMode = mode
        if (mode != AutoPlayMode.OFF) {
            soundSynth.playBeep(true)
            showMessage("AUTO-PLAY: ${mode.title}")
            autoPlayStatusText = "ENGAGED: ${mode.title}"
        } else {
            soundSynth.playBeep(false)
            showMessage("AUTO-PLAY: DISENGAGED (MANUAL)")
            autoPlayStatusText = ""
            pitchRate = 0f
            rollRate = 0f
        }
    }

    private fun updateAutoPlay(dt: Float) {
        if (autoPlayMode == AutoPlayMode.OFF || isDockingComputerActive) return

        if (autoPlayMode == AutoPlayMode.TRAINED_NEURAL_BOT) {
            val decision = com.example.elite.ai.TrainedTacticalBotEngine.evaluateTactics(this, commander, dt)
            speed = (speed + (decision.desiredSpeed - speed) * 0.4f * dt).coerceIn(0f, maxSpeed)
            pitchRate = decision.desiredPitchRate
            rollRate = decision.desiredRollRate
            autoPlayStatusText = "NEURAL AI: ${decision.action.title} (${(decision.confidence * 100).toInt()}%)"

            if (decision.triggerEcm && commander.hasEcm) {
                triggerEcm()
            }
            if (decision.fireLaser && commander.laserTemp < 85f) {
                autoPlayLaserCooldown -= dt
                if (autoPlayLaserCooldown <= 0f) {
                    fireLaser()
                    autoPlayLaserCooldown = 0.2f
                }
            }
            if (decision.fireMissile && commander.missiles > 0) {
                autoPlayMissileCooldown -= dt
                if (autoPlayMissileCooldown <= 0f) {
                    if (!isMissileArmed) armMissile() else fireMissile()
                    autoPlayMissileCooldown = 2.0f
                }
            }
            return
        }

        // 1. Threat & Survival Reaction (Auto-ECM & Auto-Energy Bomb)
        if (incomingMissileCountdownMs > 0L) {
            if (commander.hasEcm) {
                triggerEcm()
                autoPlayStatusText = "AUTO-ECM DEPLOYED"
                return
            }
        }

        // Auto Emergency Bomb if heavily damaged and surrounded
        val criticalDanger = (commander.forwardShield < 15f && commander.aftShield < 15f && commander.currentHullIntegrity < 40f)
        val surroundingHostiles = entities.count { it.isHostile && it.position.length() < 1200f }
        if (criticalDanger && surroundingHostiles >= 2 && commander.hasEnergyBomb) {
            triggerEnergyBomb()
            autoPlayStatusText = "AUTO EMERGENCY BOMB DETONATED"
            return
        }

        // 2. Select Auto-Play Target according to Mode
        val target: SpaceEntity? = when (autoPlayMode) {
            AutoPlayMode.COMBAT_ACE -> {
                // Focus on closest hostile or high-value threat
                entities.filter { !it.isDestroyed && it.isHostile && it.position.z > -100f }
                    .minByOrNull { it.position.length() }
                    ?: entities.filter { !it.isDestroyed && it.blueprint == ShipBlueprints.ASTEROID && it.position.z > 20f }
                        .minByOrNull { it.position.length() }
            }
            AutoPlayMode.TRADER_EXPLORER -> {
                // Priority 1: Floating cargo canisters to scoop
                val canister = entities.filter { !it.isDestroyed && it.blueprint == ShipBlueprints.CANISTER && it.position.z > -50f }
                    .minByOrNull { it.position.length() }
                // Priority 2: Mineable asteroids
                val asteroid = entities.filter { !it.isDestroyed && it.blueprint == ShipBlueprints.ASTEROID && it.position.z > 20f }
                    .minByOrNull { it.position.length() }
                // Priority 3: Defend against aggressive hostiles if attacked
                val hostileAttacker = entities.filter { !it.isDestroyed && it.isHostile && it.position.length() < 800f }
                    .minByOrNull { it.position.length() }

                canister ?: (hostileAttacker ?: (asteroid ?: stationEntity))
            }
            AutoPlayMode.SURVIVAL_DEFENSE -> {
                // Defend only if threatened; otherwise orbit safely or dock
                val nearThreat = entities.filter { !it.isDestroyed && it.isHostile && it.position.length() < 1200f }
                    .minByOrNull { it.position.length() }
                nearThreat ?: stationEntity
            }
            AutoPlayMode.TRAINED_NEURAL_BOT -> null // Handled early above
            AutoPlayMode.OFF -> null
        }

        lockedTarget = target

        // 3. Autonomous Flight Steering towards or orbiting target
        if (target != null && !target.isDestroyed) {
            val tPos = target.position
            val dist = tPos.length()

            // If target is behind us (z <= 10f), execute quick 180 turnaround maneuver
            if (tPos.z <= 10f) {
                rollRate = if (tPos.x >= 0f) 0.85f else -0.85f
                pitchRate = if (tPos.y >= 0f) -0.65f else 0.65f
                speed = (speed + 2f * dt).coerceIn(12f, 24f)
                autoPlayStatusText = "TURNING TOWARDS ${target.blueprint.name}"
            } else {
                // Target is in front hemisphere: compute angular tracking offsets
                val screenX = (tPos.x / tPos.z) * 300f
                val screenY = (tPos.y / tPos.z) * 300f

                val deadzone = 12f
                val desiredRoll = (-screenX * 0.015f).coerceIn(-1.0f, 1.0f)
                val desiredPitch = (screenY * 0.015f).coerceIn(-1.0f, 1.0f)

                rollRate = if (abs(screenX) > deadzone) desiredRoll else desiredRoll * 0.2f
                pitchRate = if (abs(screenY) > deadzone) desiredPitch else desiredPitch * 0.2f

                // Autonomous throttle control
                val optimalDist = if (target.blueprint == ShipBlueprints.CANISTER) 40f else 350f
                if (dist > optimalDist + 150f) {
                    speed = (speed + 8f * dt).coerceAtMost(maxSpeed)
                } else if (dist < optimalDist - 50f) {
                    speed = (speed - 12f * dt).coerceAtLeast(6f)
                } else {
                    speed = (speed + (20f - speed) * 0.5f * dt).coerceIn(8f, 25f)
                }

                // Autonomous Cargo Scooping speed limit
                if (target.blueprint == ShipBlueprints.CANISTER && dist < 120f) {
                    speed = speed.coerceAtMost(22f) // Safe scooping speed is <= 28f
                    autoPlayStatusText = "SCOOPING CARGO (${dist.toInt()}M)"
                } else if (target.isHostile) {
                    autoPlayStatusText = "TRACKING ${target.blueprint.name} (${dist.toInt()}M)"
                } else {
                    autoPlayStatusText = "APPROACHING ${target.blueprint.name}"
                }

                // 4. Autonomous Weapon Discharge
                val isAimed = abs(screenX) < 45f && abs(screenY) < 45f && tPos.z in 60f..1100f
                if (isAimed) {
                    // Auto-Laser Fire
                    autoPlayLaserCooldown -= dt
                    if (autoPlayLaserCooldown <= 0f && commander.laserTemp < 85f) {
                        fireLaser()
                        autoPlayLaserCooldown = 0.22f // Burst firing cadence
                    }

                    // Auto-Missile Launch against armored/heavy hostiles
                    val isHeavyTarget = target.blueprint.bounty >= 100 || target.blueprint == ShipBlueprints.ANACONDA || target.blueprint == ShipBlueprints.THARGOID
                    autoPlayMissileCooldown -= dt
                    if (autoPlayMissileCooldown <= 0f && target.isHostile && isHeavyTarget && commander.missiles > 0 && dist in 250f..850f) {
                        if (!isMissileArmed) {
                            armMissile()
                        } else {
                            fireMissile()
                            autoPlayMissileCooldown = 6.0f
                        }
                    }
                }
            }
        } else {
            // No current target: cruise steadily forward, level wings
            rollRate = -currentRoll * 0.4f
            pitchRate = -currentPitch * 0.4f
            speed = 18f
            autoPlayStatusText = "PATROLLING SECTOR (SEARCHING TARGETS)"
        }
    }

    private fun updateAutopilot(dt: Float) {
        val station = stationEntity ?: return
        val pos = station.position

        // Align ship with station entrance
        val targetX = 0f
        val targetY = 0f
        val dx = pos.x - targetX
        val dy = pos.y - targetY

        rollRate = -dx * 0.003f
        pitchRate = dy * 0.003f
        speed = 20f

        // Smoothly adjust station relative position towards docking slot
        station.position = Vector3(
            pos.x - dx * 1.5f * dt,
            pos.y - dy * 1.5f * dt,
            pos.z
        )

        // If close enough to slot, trigger docking
        if (pos.z in 20f..150f && abs(dx) < 30f && abs(dy) < 30f) {
            performDocking()
        }
    }

    fun performDocking() {
        isDockingComputerActive = false
        soundSynth.stopBlueDanube()
        soundSynth.playBeep(true)
        commander.isDocked = true
        commander.forwardShield = 100f
        commander.aftShield = 100f
        commander.energyBanks = 100f
        commander.cabinTemp = 10f
        commander.laserTemp = 0f
        showMessage("DOCKED AT CORIOLIS STATION")
        onDocked()
    }

    fun startHyperspaceJump(targetSystem: SystemData, fuelNeeded: Int) {
        if (commander.fuelDeciLy < fuelNeeded) {
            showMessage("INSUFFICIENT FUEL FOR HYPERSPACE")
            soundSynth.playBeep(false)
            return
        }

        commander.fuelDeciLy -= fuelNeeded
        soundSynth.playHyperspace()
        showMessage("HYPERSPACE JUMP: ${targetSystem.name}...")

        commander.currentSystemId = targetSystem.id
        resetSpaceEnvironment()
        onHyperspaceComplete(targetSystem)
    }

    fun spawnBattleWave(scenario: CombatScenario) {
        entities.removeAll {
            it.blueprint != ShipBlueprints.CORIOLIS &&
            it.blueprint != ShipBlueprints.PLANET &&
            it.blueprint != ShipBlueprints.SUN
        }
        lockedTarget = null
        soundSynth.playMissileLaunch()
        showMessage("ALERT: ${scenario.title} DETECTED!")

        when (scenario) {
            CombatScenario.PIRATE_AMBUSH -> {
                entities.add(
                    SpaceEntity(
                        id = Random.nextLong(),
                        blueprint = ShipBlueprints.MAMBA,
                        position = Vector3(0f, 40f, 650f),
                        isHostile = true,
                        shields = 100f
                    )
                )
                entities.add(
                    SpaceEntity(
                        id = Random.nextLong(),
                        blueprint = ShipBlueprints.SIDEWINDER,
                        position = Vector3(-220f, -30f, 850f),
                        isHostile = true,
                        shields = 60f
                    )
                )
                entities.add(
                    SpaceEntity(
                        id = Random.nextLong(),
                        blueprint = ShipBlueprints.SIDEWINDER,
                        position = Vector3(220f, -30f, 850f),
                        isHostile = true,
                        shields = 60f
                    )
                )
            }
            CombatScenario.THARGOID_INVASION -> {
                entities.add(
                    SpaceEntity(
                        id = Random.nextLong(),
                        blueprint = ShipBlueprints.THARGOID,
                        position = Vector3(0f, 60f, 750f),
                        isHostile = true,
                        shields = 240f
                    )
                )
                for (i in 0 until 3) {
                    val angle = (i * 2.0 * Math.PI / 3.0).toFloat()
                    val tx = cos(angle) * 160f
                    val ty = sin(angle) * 160f
                    entities.add(
                        SpaceEntity(
                            id = Random.nextLong(),
                            blueprint = ShipBlueprints.THARGON,
                            position = Vector3(tx, ty + 60f, 650f + i * 50f),
                            isHostile = true,
                            shields = 40f
                        )
                    )
                }
            }
            CombatScenario.VIPER_POLICE_CHALLENGE -> {
                for (i in 0 until 3) {
                    val px = (i - 1) * 220f
                    entities.add(
                        SpaceEntity(
                            id = Random.nextLong(),
                            blueprint = ShipBlueprints.VIPER,
                            position = Vector3(px, 20f, 700f + abs(px)),
                            isHostile = true,
                            shields = 120f
                        )
                    )
                }
            }
            CombatScenario.ANACONDA_CRUISER_SIEGE -> {
                entities.add(
                    SpaceEntity(
                        id = Random.nextLong(),
                        blueprint = ShipBlueprints.ANACONDA,
                        position = Vector3(0f, 0f, 880f),
                        isHostile = true,
                        shields = 350f
                    )
                )
                entities.add(
                    SpaceEntity(
                        id = Random.nextLong(),
                        blueprint = ShipBlueprints.MAMBA,
                        position = Vector3(-250f, 50f, 720f),
                        isHostile = true,
                        shields = 80f
                    )
                )
                entities.add(
                    SpaceEntity(
                        id = Random.nextLong(),
                        blueprint = ShipBlueprints.FER_DE_LANCE,
                        position = Vector3(250f, 50f, 720f),
                        isHostile = true,
                        shields = 110f
                    )
                )
            }
            CombatScenario.SECTOR_WAR_100_BOTS -> {
                // Spawn a massive skirmish wing of 18 active 3D dogfight craft in close camera radius
                val warBlueprints = listOf(
                    ShipBlueprints.COBRA_MK_3, ShipBlueprints.VIPER, ShipBlueprints.MAMBA,
                    ShipBlueprints.SIDEWINDER, ShipBlueprints.KRAIT, ShipBlueprints.ASP_MK_2,
                    ShipBlueprints.FER_DE_LANCE, ShipBlueprints.PYTHON, ShipBlueprints.THARGOID,
                    ShipBlueprints.THARGON
                )
                for (i in 0 until 18) {
                    val bp = warBlueprints[i % warBlueprints.size]
                    val angle = (i * 2.0 * Math.PI / 18.0).toFloat()
                    val dist = Random.nextFloat() * 1100f + 400f
                    val elev = (Random.nextFloat() - 0.5f) * 600f
                    val isPirateOrAlien = (i % 2 == 0)
                    entities.add(
                        SpaceEntity(
                            id = Random.nextLong(),
                            blueprint = bp,
                            position = Vector3(cos(angle) * dist, elev, sin(angle) * dist + 700f),
                            isHostile = isPirateOrAlien,
                            shields = 80f + (i * 10f)
                        )
                    )
                }
            }
        }
    }

    fun spawnAsteroidField(count: Int = 10) {
        entities.removeAll { it.blueprint == ShipBlueprints.ASTEROID || it.blueprint == ShipBlueprints.CANISTER }
        lockedTarget = null
        soundSynth.playBeep(true)
        showMessage("ASTEROID BELT: $count RESOURCE ROCKS DETECTED")

        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2.0f * Math.PI.toFloat()
            val dist = Random.nextFloat() * 1200f + 300f
            val elevation = (Random.nextFloat() - 0.5f) * 450f
            entities.add(
                SpaceEntity(
                    id = Random.nextLong(),
                    blueprint = ShipBlueprints.ASTEROID,
                    position = Vector3(cos(angle) * dist, elevation, sin(angle) * dist + 400f),
                    shields = if (commander.laserFront.power >= 45) 45f else 75f
                )
            )
        }
    }

    private fun updateCombatRank() {
        val kills = commander.killCount
        val rank = when {
            kills >= CombatRank.ELITE.scoreNeeded -> CombatRank.ELITE
            kills >= CombatRank.DEADLY.scoreNeeded -> CombatRank.DEADLY
            kills >= CombatRank.DANGEROUS.scoreNeeded -> CombatRank.DANGEROUS
            kills >= CombatRank.COMPETENT.scoreNeeded -> CombatRank.COMPETENT
            kills >= CombatRank.ABOVE_AVERAGE.scoreNeeded -> CombatRank.ABOVE_AVERAGE
            kills >= CombatRank.AVERAGE.scoreNeeded -> CombatRank.AVERAGE
            kills >= CombatRank.POOR.scoreNeeded -> CombatRank.POOR
            kills >= CombatRank.MOSTLY_HARMLESS.scoreNeeded -> CombatRank.MOSTLY_HARMLESS
            else -> CombatRank.HARMLESS
        }
        commander.combatRank = rank
    }
}
