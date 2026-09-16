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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

/**
 * The Space Trader's Flight Training Manual
 * Official pilot handbook transcribed from the 1984 Acornsoft BBC Micro publication
 * by Robert Holdstock (http://www.elitehomepage.org/manual.htm).
 */
data class ManualChapter(
    val id: String,
    val title: String,
    val subtitle: String,
    val sections: List<ManualSection>
)

data class ManualSection(
    val heading: String,
    val body: String
)

val ELITE_MANUAL_CHAPTERS = listOf(
    ManualChapter(
        id = "CH1",
        title = "1. COBRA MK III",
        subtitle = "VESSEL SPECIFICATIONS & ANATOMY",
        sections = listOf(
            ManualSection(
                heading = "PILOT'S FEDERATION WELCOME",
                body = "Greetings, Commander. As a newly certified pilot of the Galactic Co-operative of Worlds, you take command of a classic Faulcon deLacy Cobra Mark III trading and combat craft. You begin your voyage at the orbital station of Lave, carrying 100.0 Credits, 7.0 Light-Years of Witch-Space fuel, and 3 homing missiles."
            ),
            ManualSection(
                heading = "DRIVE & POWER SECTOR",
                body = "The Cobra Mk III is driven by twin ion-induction drive units with emergency Witch-Space hyperspace condensors. Power is maintained by four main energy banks. When shields take hostile laser or collision damage, energy is diverted from the banks to recharge them. If all four energy banks deplete, your ship will be destroyed."
            ),
            ManualSection(
                heading = "DEFENSIVE SHIELDING",
                body = "Protected by dual Zieman Energy Deflection Shields: Forward Shield (FS) and Aft Shield (AS). Each shield absorbs incoming laser and plasma fire. An Extra Energy Unit can be fitted at advanced technological worlds (Tech Level 10+) to double the rate of energy recharge."
            )
        )
    ),
    ManualChapter(
        id = "CH2",
        title = "2. INSTRUMENTS",
        subtitle = "FLIGHT CONSOLE & 3D SCANNER",
        sections = listOf(
            ManualSection(
                heading = "THE 3D SPACE SCANNER",
                body = "The central instrument of the flight console is the dual-plane 3D radar scanner. Objects in front, behind, above, and below your ship appear as blips with vertical stalks. A stalk extending upwards indicates an object above your flight plane; a stalk downwards indicates an object below. Blip colors indicate target status: Green for stations/celestial bodies, Yellow/Red for spacecraft, and Cyan for canisters."
            ),
            ManualSection(
                heading = "THE ASTROGATION COMPASS",
                body = "Located at the upper-left of the radar display. The compass shows a glowing blip indicating the direction to your navigation target (Coriolis station, Planet, or Sun). When the blip is green, the target is in your forward hemisphere. When red, the target is behind you. Center the blip in the crosshairs to head directly toward it."
            ),
            ManualSection(
                heading = "DASHBOARD GAUGES",
                body = "• FS / AS: Forward and Aft Shield levels.\n• FU: Hyperspace Fuel reserves (7.0 LY maximum).\n• CT: Cabin Temperature (rises near stars or planetary re-entry).\n• LT: Laser Temperature (continuous fire causes laser overheating).\n• AL: Altitude above planetary surface or star corona.\n• SP: Forward Flight Velocity.\n• RL / DC: Roll and Pitch attitude indicators.\n• BANKS 1-4: Primary ship power reserves."
            )
        )
    ),
    ManualChapter(
        id = "CH3",
        title = "3. DOCKING",
        subtitle = "CORIOLIS STATION DOCKING ETIQUETTE",
        sections = listOf(
            ManualSection(
                heading = "THE MANUAL DOCKING PROCEDURE",
                body = "Docking at a rotating Coriolis space station requires precision flying. The entrance slot is located on the face oriented toward the planet and rotates anti-clockwise at 0.4 radians/second. 1: Locate the station via your compass. 2: Fly around to the front docking face at a distance of 5 to 10 kilometers. 3: Align your ship with the rotation axis. 4: Roll your ship to match the station's rotation rate. 5: Throttle down to under 12 LM and fly cleanly through the rectangular slot."
            ),
            ManualSection(
                heading = "AUTOMATED DOCKING COMPUTER",
                body = "Pilots who purchase the Docking Computer (Tech Level 9, 1500.0 CR) can engage autopilot by pressing 'DOCK'. The computer assumes control of thrusters and rotational attitude, playing Johann Strauss's 'The Blue Danube' waltz while smoothly guiding your Cobra Mk III safely into the docking bay."
            )
        )
    ),
    ManualChapter(
        id = "CH4",
        title = "4. COMBAT",
        subtitle = "TACTICS, MISSILES & COUNTERMEASURES",
        sections = listOf(
            ManualSection(
                heading = "LASER MOUNTS",
                body = "The Cobra Mk III can be fitted with four weapon mounts: Front, Rear, Left, and Right. Available lasers include:\n• Pulse Laser (standard civilian armament)\n• Beam Laser (continuous high-energy beam)\n• Mining Laser (heavy caliber for fracturing asteroids)\n• Military Laser (Galactic Navy specification for rapid hull destruction)"
            ),
            ManualSection(
                heading = "HOMING MISSILES & LOCK-ON",
                body = "Equipped with powerful Faulcon deLacy homing missiles. Target an enemy ship using your crosshairs or tap-targeting. Arm the missile (MSL); the target reticle turns armed red with an audio tone. Press FIRE to release the missile. A direct hit causes 80 MJ of explosive kinetic damage."
            ),
            ManualSection(
                heading = "ELECTRONIC COUNTERMEASURES (ECM)",
                body = "When an enemy pirate or naval vessel launches a homing missile at you, an urgent 'MISSILE INCOMING' warning will flash on your HUD. Activating your ECM system broadcasts a high-frequency jamming signal that detonates all incoming hostile missiles before they strike your shields."
            ),
            ManualSection(
                heading = "THE ENERGY BOMB",
                body = "A devastating, single-use superweapon. When detonated, it releases an omnidirectional EMP shockwave that obliterates all hostile pirate craft, thargons, and asteroid debris within a 3-kilometer radius."
            )
        )
    ),
    ManualChapter(
        id = "CH5",
        title = "5. ASTROGATION",
        subtitle = "HYPERSPACE, SCOOPING & WITCH-SPACE",
        sections = listOf(
            ManualSection(
                heading = "HYPERSPACE TRANSIT",
                body = "Standard hyperspace jumps can bridge distances of up to 7.0 Light-Years between stars. Jumps consume 1.0 Light-Year of Witch-Space fuel per light-year traveled. Fuel can be replenished at spaceports or skimmed directly from suns using Fuel Scoops."
            ),
            ManualSection(
                heading = "SOLAR PLASMA SCOOPING",
                body = "Equip Fuel Scoops to scoop fuel from the primary star of any system. Dive close to the star's glowing corona while monitoring your Cabin Temperature (CT) and Altitude (AL) gauges. Avoid getting too close to prevent thermal hull breach!"
            ),
            ManualSection(
                heading = "WITCH-SPACE MALFUNCTIONS & THARGOIDS",
                body = "Occasionally, a hyperspace jump suffers an interdiction by Thargoid warships. The ship is pulled into Witch-Space—a starless void. Thargoid mothercraft deploy swarms of remote-piloted Thargon drones. Destroy the alien invaders or engage your hyperdrive to break back into normal space!"
            ),
            ManualSection(
                heading = "GALACTIC HYPERDRIVE",
                body = "A specialized single-use drive system (Tech Level 10, 5000.0 CR) capable of crossing the intergalactic void, transporting your vessel to the next galaxy (Galaxy 1 through Galaxy 8)."
            )
        )
    ),
    ManualChapter(
        id = "CH6",
        title = "6. TRADING",
        subtitle = "COMMODITIES, ECONOMIES & CONTRABAND",
        sections = listOf(
            ManualSection(
                heading = "THE 17 GALACTIC COMMODITIES",
                body = "Trading is the lifeblood of every commander. Buy goods where they are abundant and cheap, then jump to systems where they are scarce to sell at high profit. The standard market commodities are: Food, Textiles, Radioactives, Slaves, Liquor/Wines, Luxuries, Narcotics, Computers, Machinery, Alloys, Firearms, Furs, Minerals, Gold, Platinum, Gem-Stones, and Alien Items."
            ),
            ManualSection(
                heading = "ECONOMIC CYCLES",
                body = "Agricultural worlds produce plentiful Food, Textiles, Furs, and Liquor at low prices, while desperately demanding Machinery and Computers. Industrial worlds mass-produce Computers, Alloys, and Machinery, but pay top credits for raw Food and Agricultural products."
            ),
            ManualSection(
                heading = "LEGAL STATUS & CONTRABAND",
                body = "Carrying Slaves, Narcotics, or Firearms is strictly prohibited by Galactic Law. If scanned by Police Vipers near a Coriolis station while hauling contraband, or if you fire upon clean vessels, your legal rating changes from CLEAN to OFFENDER or FUGITIVE. Police Vipers will attack Fugitives on sight!"
            )
        )
    ),
    ManualChapter(
        id = "CH7",
        title = "7. THE ORDER OF ELITE",
        subtitle = "COMBAT RANKS & REPUTATION",
        sections = listOf(
            ManualSection(
                heading = "COMBAT RATINGS",
                body = "Your combat prowess is evaluated by the Pilot's Federation based on verified kills of pirate, naval, and alien craft:\n• HARMLESS (0 kills)\n• MOSTLY HARMLESS (8 kills)\n• POOR (16 kills)\n• AVERAGE (32 kills)\n• ABOVE AVERAGE (64 kills)\n• COMPETENT (128 kills)\n• DANGEROUS (512 kills)\n• DEADLY (2560 kills)\n• ELITE (6400+ kills)\nOnly the most disciplined commanders survive to join the legendary Order of Elite."
            ),
            ManualSection(
                heading = "ESSENTIAL UPGRADES",
                body = "Invest your trading profits into your vessel:\n• Cargo Bay Extension (increases hold from 20t to 35t)\n• Military Laser (fore mount for superior combat)\n• Shield Boosters & Extra Energy Unit\n• Escape Pod (preserves commander life in case of catastrophic destruction)"
            )
        )
    )
)

@Composable
fun FlightManualView(
    soundSynth: BbcSoundSynth,
    modifier: Modifier = Modifier
) {
    var selectedChapterIndex by remember { mutableIntStateOf(0) }
    val chapter = ELITE_MANUAL_CHAPTERS[selectedChapterIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF02070B))
            .padding(8.dp)
            .testTag("flight_manual_view")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "THE SPACE TRADER'S FLIGHT TRAINING MANUAL",
                    color = BBC_YELLOW,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "ACORNSOFT 1984 • PILOT'S FEDERATION HANDBOOK",
                    color = Color(0xFF88AABB),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Chapter Navigation Selector Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(ELITE_MANUAL_CHAPTERS) { index, ch ->
                val isSelected = index == selectedChapterIndex
                Button(
                    onClick = {
                        selectedChapterIndex = index
                        soundSynth.playBeep(true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFF881111) else Color(0xFF14202B)
                    ),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("manual_ch_btn_${index + 1}")
                ) {
                    Text(
                        text = ch.title,
                        color = if (isSelected) BBC_WHITE else Color(0xFFAABBCC),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Chapter Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF091622), RoundedCornerShape(4.dp))
                .border(1.dp, BBC_GREEN.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column {
                Text(
                    text = chapter.title,
                    color = BBC_GREEN,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = chapter.subtitle,
                    color = BBC_CYAN,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Content Sections Scroll
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF040A0F), RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF1E2D3A), RoundedCornerShape(4.dp))
                .padding(10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            for (sec in chapter.sections) {
                Text(
                    text = sec.heading,
                    color = BBC_YELLOW,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = sec.body,
                    color = BBC_WHITE,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Quick navigation footer buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (selectedChapterIndex > 0) {
                    Button(
                        onClick = {
                            selectedChapterIndex--
                            soundSynth.playBeep(true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15222E)),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("◀ PREV CHAPTER", color = BBC_CYAN, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (selectedChapterIndex < ELITE_MANUAL_CHAPTERS.size - 1) {
                    Button(
                        onClick = {
                            selectedChapterIndex++
                            soundSynth.playBeep(true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15222E)),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("NEXT CHAPTER ▶", color = BBC_CYAN, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
