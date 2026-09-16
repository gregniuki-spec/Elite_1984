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
    var isDestroyed: Boolean = false
)

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

class FlightEngine(
    val commander: CommanderState,
    val soundSynth: BbcSoundSynth,
    var onDocked: () -> Unit = {},
    var onHyperspaceComplete: (SystemData) -> Unit = {}
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

    // Autopilot & Hyperspace
    var isDockingComputerActive: Boolean = false
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

        // 1. Spawn Coriolis Space Station
        val station = SpaceEntity(
            id = 1L,
            blueprint = ShipBlueprints.CORIOLIS,
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
            ShipBlueprints.VIPER to false,
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

        // Docking computer autopilot
        if (isDockingComputerActive) {
            updateAutopilot(dt)
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

            // Coriolis Station rotation
            if (e.blueprint == ShipBlueprints.CORIOLIS) {
                e.rotation = Vector3(
                    e.rotation.x,
                    e.rotation.y,
                    e.rotation.z + 0.4f * dt
                )

                // Check manual or autopilot docking into Coriolis entrance slot
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
            } else if (e.blueprint == ShipBlueprints.CANISTER) {
                // Interactive Cargo Scooping!
                val dist = e.position.length()
                if (dist < 65f && speed <= 25f) {
                    if (commander.freeCargoSpace() > 0) {
                        // Scoop canister
                        e.isDestroyed = true
                        val scoopedId = Random.nextInt(17)
                        commander.cargo[scoopedId] = (commander.cargo[scoopedId] ?: 0) + 1
                        soundSynth.playBeep(true)
                        val commodityNames = listOf(
                            "Food", "Textiles", "Radioactives", "Slaves", "Liquor/Wines",
                            "Luxuries", "Narcotics", "Computers", "Machinery", "Alloys",
                            "Firearms", "Furs", "Minerals", "Gold", "Platinum", "Gem-Stones", "Alien Items"
                        )
                        val name = commodityNames.getOrElse(scoopedId) { "Minerals" }
                        showMessage("CARGO SCOOPED: 1t $name!")
                    } else {
                        if (Random.nextFloat() < 0.1f) {
                            showMessage("CARGO HOLD FULL")
                        }
                    }
                }
            } else {
                // AI behavior
                if (e.isHostile) {
                    // Turn towards player and fire lasers
                    val dir = (Vector3(0f, 0f, 0f) - relPos).normalized()
                    e.position = e.position + dir * (e.blueprint.maxSpeed * 0.5f * dt)

                    // Enemy shooting chance
                    if (relPos.z in 100f..800f && Random.nextFloat() < 0.03f) {
                        commander.forwardShield = (commander.forwardShield - 8f).coerceAtLeast(0f)
                        if (commander.forwardShield <= 0f) {
                            commander.energyBanks = (commander.energyBanks - 10f).coerceAtLeast(0f)
                        }
                        soundSynth.playExplosion()
                        showMessage("WARNING: HOSTILE LASER HIT!")
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

        // Recharge shields and cool lasers
        if (commander.energyBanks > 20f) {
            if (commander.forwardShield < 100f) commander.forwardShield = (commander.forwardShield + 3f * dt).coerceAtMost(100f)
            if (commander.aftShield < 100f) commander.aftShield = (commander.aftShield + 3f * dt).coerceAtMost(100f)
        }
        if (commander.laserTemp > 0f) {
            commander.laserTemp = (commander.laserTemp - 8f * dt).coerceAtLeast(0f)
        }

        // Incoming hostile missile countdown check
        if (incomingMissileCountdownMs > 0L && System.currentTimeMillis() > incomingMissileCountdownMs) {
            incomingMissileCountdownMs = 0L
            soundSynth.playExplosion()
            if (commander.forwardShield > 40f) {
                commander.forwardShield -= 40f
            } else {
                val rem = 40f - commander.forwardShield
                commander.forwardShield = 0f
                commander.energyBanks = (commander.energyBanks - rem).coerceAtLeast(0f)
            }
            showMessage("MISSILE IMPACT ON SHIELDS!")
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
                    showMessage("${target.blueprint.name} DESTROYED! BOUNTY: ${bounty}.0 CR")
                } else if (target.blueprint == ShipBlueprints.ASTEROID) {
                    showMessage("ASTEROID DESTROYED")
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
