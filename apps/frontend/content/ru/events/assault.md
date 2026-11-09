---
title: Assault
---

# Assault

> Эта страница пока на английском: ивенты ещё не переведены.

65 events.

Events come in three bands. A **threat** event never takes the player below `assaults.safe_health` (3 hearts). A **brink** event
may leave half a heart, needs power 0.6 and is followed by a quiet spell. A **lethal** event can kill, and runs only for an
evolved spirit on its list, at power 0.9, at most once per 3 days and only after a brink event. See the `assaults` settings to
turn lethal events off. A player killed this way lowers the spirit's anger. Every one gives a warning and a way out.

## `cold_touch`

**Harmful.**
1–2 hearts of magic damage (2–4 HP), never lethal (leaves at least 0.5 heart), freezing sound

Runs when: Player health > 1 heart

## `trip`

**Harmful.**
Stops dead for 1 s

Runs when: Not slowed

## `scorch`

**Harmful.**
On fire for 1 s

Runs when: Health > 3 hearts, dry, not burning

## `wrath`

**Harmful.**
2–3.5 hearts of magic damage, never lethal

Runs when: Health > 4 hearts

## `snatch`

**Harmful.**
The item is thrown ~5 blocks away

Runs when: Item in main hand

## `shove`

**Harmful.**
Pushes the player toward the drop (a fall of 1–4 HP). Runs only at a ledge

Runs when: Health > 5 hearts, on the ground at the edge of a 4–7 block drop onto solid ground

## `toss`

**Harmful.**
Thrown straight up about 5 blocks; the landing costs a couple of HP

Runs when: Health > 5 hearts, on the ground, not in water or lava

## `suffocation`

**Harmful.**
Heartbeat, 2 s later the air bar drains (about 7 s to empty), then 1 HP a second. Stops in light, in water, at 3 hearts, or after 15 s

Runs when: Health > 4 hearts, not in water, light level \< 8

## `thunder_knocks`

**Harmful.**
Three knocks; 3 s later a bolt strikes nearby; 2 s after that a second bolt hits the player if they have moved less than 2 blocks since (6 HP, never lethal). Bolts set nothing on fire

Runs when: Health > 5 hearts

## `embrace`

**Harmful.**
Growing damage once a second (0.5, 1, 1.5 HP…) until the player is 10 blocks from where it began. Stops at 3 hearts or after 20 hits

Runs when: Health > 5 hearts

## `the_call`

**Harmful.**
A warden moan, then after 2 s the player on the ground is drawn toward the lava (or void) at about 1.5 blocks/s, felt as a velocity nudge every 0.1 s, with soul particles at the feet. Walking 6 blocks further away than the start breaks it; it also ends on arrival, in lava, at 3 hearts, or after 30 s. Lava itself is not capped

Runs when: Night in the overworld (any time elsewhere), health > 5 hearts, lava within 12 blocks or a bottomless column 10 blocks away

## `shadow`

**Harmful.**
A wither skeleton with no AI and no gear (invulnerable, silent) stands there. Every 0.1 s it takes a 0.3-block stride toward the player, but only while the player is not looking at it (within 45° and in line of sight). Warden heartbeat once a second. A touch (1.2 blocks) deals up to 7 hearts but never below 3 hearts, then it vanishes in smoke; it also vanishes after 60 s or after 40 blocked strides

Runs when: Not peaceful, no shadow already following the player, health > 7 hearts, standing room 14–20 blocks away

## `snow_snowball`

**Harmful.**
A snowball thrown from a point 10 blocks away at the player's eyes at 1.2 blocks/tick; it does no damage

Runs when: In a snowy biome

## `snow_powder_path`

**Harmful.**
The block under standing room 5 blocks ahead within 30° of where the player looks becomes powder snow, one block deep; restored after 1 min (stays under hardcore)

Runs when: In a snowy biome, health above the safe floor + 2 hearts, and standing room 5 blocks ahead with solid ground two blocks under it

## `mountain_wind_push`

**Harmful.**
Three wind howls 0.5 s apart, then a push of 0.3 toward the edge the player stands at

Runs when: In a mountain biome, health above the safe floor + 2 hearts, and the player at the edge of a drop

## `hurled_block`

**Harmful, threat band: leaves at least 3 hearts.**
A loose block nearby shivers and lifts in a cloud of dust, then flies at you. Step aside.

Runs when: Not peaceful, health above 5 hearts, a loose block (gravel, cobblestone and the like) within reach

## `flung_tools`

**Harmful, threat band: leaves at least 3 hearts.**
A chest nearby rattles and opens, and the tools inside fly at you one by one. Dodge them or empty the chest.

Runs when: Not peaceful, health above 5 hearts, a chest with tools in it nearby

## `volley`

**Harmful, threat band: leaves at least 3 hearts.**
A skeleton rattles somewhere in the dark, then a few arrows come from that one direction. Take cover.

Runs when: Not peaceful, health above 5 hearts, light level below `dark_below`

## `cave_in`

**Harmful, threat band: leaves at least 3 hearts.**
Dust and creaking overhead, then gravel falls on the spot where you stand. Step out from under it.

Runs when: Not peaceful, health above 5 hearts, a solid roof overhead

## `dripstone_rain`

**Harmful, threat band: leaves at least 3 hearts.**
Pointed dripstone breaks off the ceiling around you, wave after wave. Keep moving.

Runs when: Not peaceful, health above 5 hearts, a solid roof overhead

## `possessed_mob`

**Harmful, threat band: leaves at least 3 hearts.**
A hostile mob nearby is lit by spirit fire, then turns fast and strong and comes for you. Kill it. A faint dark haze gathers around the mob, thicker as the turn nears.

Runs when: Not peaceful, health above 5 hearts, a hostile mob nearby

## `frenzy`

**Harmful, threat band: leaves at least 3 hearts.**
Every hostile mob nearby is seized by spirit fire, then turns fast and strong. Fall back.

Runs when: Not peaceful, health above 5 hearts, at least `min_mobs` hostile mobs nearby

## `turned_pets`

**Harmful, threat band: leaves at least 3 hearts.**
Your tamed wolves and cats stand and growl, then go for you for a while. Walk away.

Runs when: Not peaceful, health above 5 hearts, the player has a tamed wolf or cat nearby

## `vex_swarm`

**Harmful, threat band: leaves at least 3 hearts.**
A cold wail, then a few short-lived vexes named "?" come out of the air. Fight back.

Runs when: Not peaceful, health above 5 hearts

## `ghost_creeper`

**Harmful, threat band: leaves at least 3 hearts.**
A hiss behind you, then a small blast there that breaks no blocks. Run on the hiss.

Runs when: Not peaceful, health above 5 hearts

## `stalker`

**Harmful, threat band: leaves at least 3 hearts.**
A figure appears far off and stands watching, then runs at you, hits once and is gone. Outrun it. A faint dark haze trails it as it runs and stays where it falls for 2 s.

Runs when: Not peaceful, health above 5 hearts, standing room for the figure in line of sight

## `hail_of_bones`

**Harmful, threat band: leaves at least 3 hearts.**
Rattling in the night sky, then blocks of bone fall around you. Take cover under a roof.

Runs when: Not peaceful, health above 5 hearts, overworld, night, open sky

## `rake`

**Harmful, threat band: leaves at least 3 hearts.**
Three scratches in the dark, then three blows. Moving between them is enough to be missed.

Runs when: Health above 5 hearts

## `teeth_in_dark`

**Harmful, threat band: leaves at least 3 hearts.**
A growl where the light is gone, then a bite every few seconds. Find light. A faint dark haze hangs just off to one side, where the jaws are.

Runs when: Health above 5 hearts, light level 0

## `searing_armor`

**Harmful, threat band: leaves at least 3 hearts.**
The chestplate glows and burns every second. Take it off or jump into water.

Runs when: Health above 5 hearts, a non-leather chestplate worn, dry, not fire resistant

## `frost_grip`

**Harmful, threat band: leaves at least 3 hearts.**
A cold like powder snow on dry land, and it bites. Get to fire or a light.

Runs when: Health above 5 hearts, can freeze, not warm (no fire or lava nearby, not in water)

## `pinned`

**Harmful, threat band: leaves at least 3 hearts.**
Heavy limbs, and every hostile nearby turns on you. Fight or wait it out.

Runs when: Health above 5 hearts, not slowed, a hostile mob nearby

## `throat`

**Harmful, threat band: leaves at least 3 hearts.**
Air drains on dry land and a whisper says hush. Crouch and wait, or drink milk.

Runs when: Health above 5 hearts, not in water, no bad luck effect

## `undertow_dry`

**Harmful, threat band: leaves at least 3 hearts.**
Something drags you back while you sprint. Stop running.

Runs when: Health above 5 hearts, sprinting

## `lantern_burst`

**Harmful, threat band: leaves at least 3 hearts.**
A torch or lantern sputters, then bursts into fire. Stay away from the lights.

Runs when: Health above 5 hearts, a torch or lantern within reach

## `blood_cough`

**Harmful, threat band: leaves at least 3 hearts.**
A wet cough every few seconds, five in all. Milk ends it.

Runs when: Health above 5 hearts, no nausea

## `wrong_step`

**Harmful, threat band: leaves at least 3 hearts.**
The ground ahead crumbles into a hole, a fall of one to three blocks. Watch where you step.

Runs when: On the ground, standing room 1 block ahead

## `ash_storm`

**Harmful, threat band: leaves at least 3 hearts.**
A wind of soul fire in the Nether. Take shelter behind a wall.

Runs when: Health above 5 hearts, in the Nether

## `hand_below`

**Harmful, threat band: leaves at least 3 hearts.**
A circle of souls under your feet. Stay on it and a hand takes hold. Step off. A faint dark haze rises from the ring, thicker the longer you stay.

Runs when: Health above 5 hearts, on the ground

## `blade_behind`

**Harmful, threat band: leaves at least 3 hearts.**
A sweep sounds at your back. Turn to face it before it lands.

Runs when: Health above 5 hearts

## `door_bite`

**Harmful, threat band: leaves at least 3 hearts.**
A door swings open by itself and slams on whoever passes. Use another way.

Runs when: Health above 5 hearts, a door in the player's path

## `night_terror`

**Harmful, threat band: leaves at least 3 hearts.**
A heartbeat in your sleep, then you are thrown awake screaming. Wards hold it off. A faint dark haze gathers at the bedside while you sleep and is still there when you wake.

Runs when: Health above 5 hearts, the player is asleep with at least a second of the night left

## `stone_fists`

**Harmful, threat band: leaves at least 3 hearts.**
The wall you lean on rumbles and strikes. Step away from it.

Runs when: Health above 5 hearts, leaning on a wall

## `final_blow`

**Harmful, brink band: can leave half a heart.**
A roar and a ring of souls. Whoever is still inside it when it ends is struck down to half a heart.

Runs when: Power ≥ 0.6, not peaceful, health above 2 hearts, health above `min_health`

## `the_hanging`

**Harmful, brink band: can leave half a heart.**
You are lifted high into the air, then let go. Slow falling, water, or a block under you softens the drop.

Runs when: Power ≥ 0.6, not peaceful, health above 2 hearts, on the ground, not in water, no slow falling

## `held_under`

**Harmful, brink band: can leave half a heart.**
In deep water something pushes you down until your breath is gone. Swim sideways out of the column.

Runs when: Power ≥ 0.6, not peaceful, health above 2 hearts, under water, no water breathing

## `pyre`

**Harmful, brink band: can leave half a heart.**
A fire that will not go out, and fire resistance fails. Water puts it out.

Runs when: Power ≥ 0.6, not peaceful, health above 2 hearts, not in water, not fire immune

## `withering`

**Harmful, brink band: can leave half a heart.**
A wither that goes on and on. Milk ends it.

Runs when: Power ≥ 0.6, not peaceful, health above 2 hearts, no wither effect

## `count`

**Harmful, brink band: can leave half a heart.**
A countdown on the screen: three, two, one. Reach light or holy ground before it ends.

Runs when: Power ≥ 0.6, not peaceful, health above 2 hearts, not in light

## `the_hollow`

**Harmful, brink band: can leave half a heart.**
A named creature of the spirit hunts you, and its blows cannot kill. Kill it and the spirit calms.

Runs when: Power ≥ 0.6, not peaceful, health above 2 hearts, standing room for a hostile

## `ledge_push`

**Harmful, brink band: can leave half a heart.**
A strong shove at the edge of a drop. Crouch when the warning comes.

Runs when: Power ≥ 0.6, not peaceful, health above 2 hearts, at the edge of a drop

## `circle_of_knives`

**Harmful, brink band: can leave half a heart.**
Arrows hang in a ring around you, then close in. Break out of the ring.

Runs when: Power ≥ 0.6, not peaceful, health above 2 hearts

## `smother_brink`

**Harmful, brink band: can leave half a heart.**
The air is taken from you in the dark, past the safe point. Find light.

Runs when: Power ≥ 0.6, not peaceful, health above 2 hearts, not in water, light level below `dark_below`

## `embrace_brink`

**Harmful, brink band: can leave half a heart.**
Cold hands close around you, harder each time, past the safe point. Get ten blocks away.

Runs when: Power ≥ 0.6, not peaceful, health above 2 hearts

## `red_room`

**Harmful, brink band: can leave half a heart.**
Darkness, a heartbeat, and a figure in sight that strikes every second you look at it. Look away.

Runs when: Power ≥ 0.6, not peaceful, health above 2 hearts, standing room for a figure

## `drain`

**Harmful, brink band: can leave half a heart.**
Your health drains a heart a second and your hunger bar empties. Eat.

Runs when: Power ≥ 0.6, not peaceful, health above 2 hearts

## `bound`

**Harmful, brink band: can leave half a heart.**
You are held in place while a hostile is dragged to you. Kill it.

Runs when: Power ≥ 0.6, not peaceful, health above 2 hearts, standing room for a hostile

## `judgement`

**Harmful, lethal band: can kill.**
Three knocks, a bolt that misses, then a bolt that kills whoever has not moved.

Runs when: Evolved `demon` at power ≥ 0.9, not peaceful, lethal setting allows it, warned by a brink event, off cooldown

## `reaping`

**Harmful, lethal band: can kill.**
A shadow stands far off and walks closer whenever the player looks away. If it reaches them, its touch kills. It freezes while watched.

Runs when: Evolved `shade`/`wraith` at power ≥ 0.9, not peaceful, lethal setting allows it, warned by a brink event, off cooldown, standing room for the shadow

## `last_call`

**Harmful, lethal band: can kill.**
A moan, then a voice draws the player toward lava or the void and does not stop at any floor. Walking far enough away breaks it.

Runs when: Evolved `lost_one` at power ≥ 0.9, not peaceful, lethal setting allows it, warned by a brink event, off cooldown, night, lava or a bottomless column near

## `drowner`

**Harmful, lethal band: can kill.**
In water, bubbles and a gurgle, then the player is held under until the water takes them. Leaving the water in time, or breathing under it, saves them.

Runs when: Evolved `nix` at power ≥ 0.9, not peaceful, lethal setting allows it, warned by a brink event, off cooldown, in water

## `the_avenger`

**Harmful, lethal band: can kill.**
After a killing spree, a named avenger rises and hunts the player to the death. Beating it spends the spirit's anger.

Runs when: Evolved `revenant` at power ≥ 0.9, not peaceful, lethal setting allows it, warned by a brink event, off cooldown, a killing spree: `min_kills` fresh mob kills, and standing room for it

## `hellring`

**Harmful, lethal band: can kill.**
A roar and a ring of fire that closes in over ten seconds. Whoever is still inside when it shuts burns to death.

Runs when: Evolved `demon` at power ≥ 0.9, not peaceful, lethal setting allows it, warned by a brink event, off cooldown

## `the_double`

**Harmful, lethal band: can kill.**
A copy of the player walks up and kills with one touch. Three blows from the player drive it away.

Runs when: Evolved `phantom` at power ≥ 0.9, not peaceful, lethal setting allows it, warned by a brink event, off cooldown, standing room for the double

## `foretold`

**Harmful, lethal band: can kill.**
A bell and a true omen in the journal: at dusk the spirit takes the player, unless they are in a bed or inside a ward by then.

Runs when: Evolved `banshee` at power ≥ 0.9, not peaceful, lethal setting allows it, warned by a brink event, off cooldown
