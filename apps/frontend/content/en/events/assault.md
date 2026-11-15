---
title: Assault
---

# Assault

65 events.

Events come in three bands. A **threat** event never takes the player below `assaults.safe_health` (3 hearts). A **brink** event may leave half a heart, needs power 0.6, sets the player's heart pounding and is followed by a quiet spell. A **lethal** event can kill, and it runs only for an evolved spirit that has it on its list, at power 0.9, at most once per 3 days and only after a brink event. The `assaults` settings can turn lethal events off. A player killed this way lowers the spirit's anger. Every event gives a warning and a way out.

## `cold_touch`

**Harmful.**
The player takes 1–2 hearts of magic damage (2–4 HP) with a freezing sound. The damage is never lethal and leaves at least 0.5 heart.

Runs when: the player's health is above 1 heart.

## `trip`

**Harmful.**
The player stops dead for 1 s.

Runs when: the player is not slowed.

## `scorch`

**Harmful.**
The player is set on fire for 1 s.

Runs when: health is above 3 hearts and the player is dry and not burning.

## `wrath`

**Harmful.**
The player takes 2–3.5 hearts of magic damage, which is never lethal.

Runs when: health is above 4 hearts.

## `snatch`

**Harmful.**
The item in the player's hand is thrown about 5 blocks away.

Runs when: the player holds an item in the main hand.

## `shove`

**Harmful.**
The player is pushed toward the drop, and the fall costs 1–4 HP. The event runs only at a ledge.

Runs when: health is above 5 hearts and the player stands on the ground at the edge of a 4–7 block drop onto solid ground.

## `toss`

**Harmful.**
The player is thrown straight up about 5 blocks, and the landing costs a couple of HP.

Runs when: health is above 5 hearts and the player is on the ground, not in water or lava.

## `suffocation`

**Harmful.**
A heartbeat sounds, and 2 s later the air bar begins to drain, taking about 7 s to empty, after which the player loses 1 HP a second. The event stops in light, in water, at 3 hearts, or after 15 s.

Runs when: health is above 4 hearts, the player is not in water, and the light level is below 8.

## `thunder_knocks`

**Harmful.**
Three knocks sound, and 3 s later a bolt strikes nearby. After another 2 s a second bolt hits the player if they have moved less than 2 blocks since (6 HP, never lethal). The bolts set nothing on fire.

Runs when: health is above 5 hearts.

## `embrace`

**Harmful.**
The player takes growing damage once a second (0.5, 1, 1.5 HP…) until they are 10 blocks from where it began. The event stops at 3 hearts or after 20 hits.

Runs when: health is above 5 hearts.

## `the_call`

**Harmful.**
A warden moan sounds, and after 2 s the player on the ground is drawn toward the lava (or void) at about 1.5 blocks/s, felt as a velocity nudge every 0.1 s, with soul particles at their feet. Walking 6 blocks further away than the start breaks the call, and it also ends on arrival, in lava, at 3 hearts, or after 30 s. Lava itself is not capped.

Runs when: it is night in the overworld (at any time elsewhere), health is above 5 hearts, and there is lava within 12 blocks or a bottomless column 10 blocks away.

## `shadow`

**Harmful.**
A wither skeleton with no AI and no gear (invulnerable, silent) stands at the chosen spot. Every 0.1 s it takes a 0.3-block stride toward the player, but only while the player is not looking at it (within 45° and in line of sight), and a warden heartbeat sounds once a second. A touch (1.2 blocks) deals up to 7 hearts but never takes the player below 3 hearts, after which the skeleton vanishes in smoke; it also vanishes after 60 s or after 40 blocked strides.

Runs when: the difficulty is not peaceful, no shadow is already following the player, health is above 7 hearts, and there is standing room 14–20 blocks away.

## `snow_snowball`

**Harmful.**
A snowball is thrown from a point 10 blocks away at the player's eyes at 1.2 blocks/tick, and it does no damage.

Runs when: the player is in a snowy biome.

## `snow_powder_path`

**Harmful.**
The block under a spot of standing room 5 blocks ahead, within 30° of where the player looks, becomes powder snow one block deep. After 1 min it is restored (under hardcore it stays).

Runs when: the player is in a snowy biome, health is above the safe floor plus 2 hearts, and there is standing room 5 blocks ahead with solid ground two blocks under it.

## `mountain_wind_push`

**Harmful.**
The wind howls three times, 0.5 s apart, and then pushes the player with a force of 0.3 toward the edge they stand at.

Runs when: the player is in a mountain biome, health is above the safe floor plus 2 hearts, and the player is at the edge of a drop.

## `hurled_block`

**Harmful, threat band: leaves at least 3 hearts.**
A loose block nearby shivers and lifts in a cloud of dust, then flies at the player. The way out is to step aside.

Runs when: the difficulty is not peaceful and health is above 5 hearts, with a loose block (gravel, cobblestone and the like) within reach.

## `flung_tools`

**Harmful, threat band: leaves at least 3 hearts.**
A chest nearby rattles and opens, and the tools inside fly at the player one by one. The way out is to dodge them or empty the chest.

Runs when: the difficulty is not peaceful and health is above 5 hearts, with a chest with tools in it nearby.

## `volley`

**Harmful, threat band: leaves at least 3 hearts.**
A skeleton rattles somewhere in the dark, then a few arrows come from that one direction. The way out is to take cover.

Runs when: the difficulty is not peaceful, health is above 5 hearts, and the light level is below `dark_below`.

## `cave_in`

**Harmful, threat band: leaves at least 3 hearts.**
Dust and creaking sound overhead, then gravel falls on the spot where the player stands. The way out is to step out from under it.

Runs when: the difficulty is not peaceful and health is above 5 hearts, with a solid roof overhead.

## `dripstone_rain`

**Harmful, threat band: leaves at least 3 hearts.**
Pointed dripstone breaks off the ceiling around the player, wave after wave. The way out is to keep moving.

Runs when: the difficulty is not peaceful and health is above 5 hearts, with a solid roof overhead.

## `possessed_mob`

**Harmful, threat band: leaves at least 3 hearts.**
A hostile mob nearby is lit by spirit fire, then turns fast and strong and comes for the player. The way out is to kill it. A faint dark haze gathers around the mob, thicker as the turn nears.

Runs when: the difficulty is not peaceful and health is above 5 hearts, with a hostile mob nearby.

## `frenzy`

**Harmful, threat band: leaves at least 3 hearts.**
Every hostile mob nearby is seized by spirit fire, then turns fast and strong. The way out is to fall back.

Runs when: the difficulty is not peaceful and health is above 5 hearts, with at least `min_mobs` hostile mobs nearby.

## `turned_pets`

**Harmful, threat band: leaves at least 3 hearts.**
The player's tamed wolves and cats stand and growl, then go for the player for a while. The way out is to walk away.

Runs when: the difficulty is not peaceful and health is above 5 hearts, and the player has a tamed wolf or cat nearby.

## `vex_swarm`

**Harmful, threat band: leaves at least 3 hearts.**
A cold wail sounds, then a few short-lived vexes named "?" come out of the air. The way out is to fight back.

Runs when: the difficulty is not peaceful and health is above 5 hearts.

## `ghost_creeper`

**Harmful, threat band: leaves at least 3 hearts.**
A hiss sounds behind the player, then a small blast goes off there that breaks no blocks. The way out is to run as soon as the hiss sounds.

Runs when: the difficulty is not peaceful and health is above 5 hearts.

## `stalker`

**Harmful, threat band: leaves at least 3 hearts.**
A figure appears far off and stands watching, then runs at the player, hits once and is gone. The way out is to outrun it. A faint dark haze trails it as it runs and stays where it falls for 2 s.

Runs when: the difficulty is not peaceful and health is above 5 hearts, with standing room for the figure in line of sight.

## `hail_of_bones`

**Harmful, threat band: leaves at least 3 hearts.**
Rattling is heard in the night sky, and then blocks of bone fall around the player. The way out is to take cover under a roof.

Runs when: the difficulty is not peaceful, health is above 5 hearts, and the player is in the overworld at night under open sky.

## `rake`

**Harmful, threat band: leaves at least 3 hearts.**
Three scratches are heard in the dark, followed by three blows. Moving between them is enough for the player to be missed.

Runs when: health is above 5 hearts.

## `teeth_in_dark`

**Harmful, threat band: leaves at least 3 hearts.**
A growl sounds where the light is gone, followed by a bite every few seconds. The way out is to find light. A faint dark haze hangs just off to one side, where the jaws are.

Runs when: health is above 5 hearts and the light level is 0.

## `searing_armor`

**Harmful, threat band: leaves at least 3 hearts.**
The chestplate glows and burns every second. The way out is to take it off or jump into water.

Runs when: health is above 5 hearts, the player wears a non-leather chestplate, is dry, and is not fire resistant.

## `frost_grip`

**Harmful, threat band: leaves at least 3 hearts.**
A cold like that of powder snow settles on the player on dry land, and it bites. The way out is to get to fire or a light.

Runs when: health is above 5 hearts, and the player can freeze and is not warm (no fire or lava nearby, and not in water).

## `pinned`

**Harmful, threat band: leaves at least 3 hearts.**
The player's limbs grow heavy, and every hostile mob nearby turns on them. The way out is to fight or wait it out.

Runs when: health is above 5 hearts, the player is not slowed, and a hostile mob is nearby.

## `throat`

**Harmful, threat band: leaves at least 3 hearts.**
The player's air drains on dry land, and a whisper says hush. The way out is to crouch and wait, or drink milk.

Runs when: health is above 5 hearts, the player is not in water, and has no bad luck effect.

## `undertow_dry`

**Harmful, threat band: leaves at least 3 hearts.**
Something drags the player back while sprinting. The way out is to stop running.

Runs when: health is above 5 hearts and the player is sprinting.

## `lantern_burst`

**Harmful, threat band: leaves at least 3 hearts.**
A torch or lantern sputters, then bursts into fire. The way out is to stay away from the lights.

Runs when: health is above 5 hearts and a torch or lantern is within reach.

## `blood_cough`

**Harmful, threat band: leaves at least 3 hearts.**
A wet cough comes every few seconds, five in all. Milk ends it.

Runs when: health is above 5 hearts and the player has no nausea.

## `wrong_step`

**Harmful, threat band: leaves at least 3 hearts.**
The ground ahead crumbles into a hole, and the fall is one to three blocks. The way out is to watch each step.

Runs when: the player is on the ground and there is standing room 1 block ahead.

## `ash_storm`

**Harmful, threat band: leaves at least 3 hearts.**
A wind of soul fire blows through the Nether. The way out is to take shelter behind a wall.

Runs when: health is above 5 hearts and the player is in the Nether.

## `hand_below`

**Harmful, threat band: leaves at least 3 hearts.**
A circle of souls appears under the player's feet. If the player stays on it, a hand takes hold; the way out is to step off. A faint dark haze rises from the ring, thicker the longer the player stays.

Runs when: health is above 5 hearts and the player is on the ground.

## `blade_behind`

**Harmful, threat band: leaves at least 3 hearts.**
A sweep sounds at the player's back. The way out is to turn to face it before it lands.

Runs when: health is above 5 hearts.

## `door_bite`

**Harmful, threat band: leaves at least 3 hearts.**
A door swings open by itself and slams on whoever passes. The way out is to use another way.

Runs when: health is above 5 hearts and there is a door in the player's path.

## `night_terror`

**Harmful, threat band: leaves at least 3 hearts.**
A heartbeat sounds in the player's sleep, and then the player is thrown awake screaming. Wards hold it off. A faint dark haze gathers at the bedside while the player sleeps and is still there on waking.

Runs when: health is above 5 hearts and the player is asleep with at least a second of the night left.

## `stone_fists`

**Harmful, threat band: leaves at least 3 hearts.**
The wall the player leans on rumbles and strikes. The way out is to step away from it.

Runs when: health is above 5 hearts and the player is leaning on a wall.

## `final_blow`

**Harmful, brink band: can leave half a heart.**
A roar sounds and a ring of souls appears. Whoever is still inside it when it ends is struck down to half a heart.

Runs when: power is at least 0.6, the difficulty is not peaceful, and health is above both 2 hearts and `min_health`.

## `the_hanging`

**Harmful, brink band: can leave half a heart.**
The player is lifted high into the air, then let go. Slow falling, water, or a block underneath softens the drop.

Runs when: power is at least 0.6, the difficulty is not peaceful, health is above 2 hearts, and the player is on the ground, not in water, and without slow falling.

## `held_under`

**Harmful, brink band: can leave half a heart.**
In deep water something pushes the player down until the breath is gone. The way out is to swim sideways out of the column.

Runs when: power is at least 0.6, the difficulty is not peaceful, health is above 2 hearts, and the player is under water without water breathing.

## `pyre`

**Harmful, brink band: can leave half a heart.**
A fire that will not go out takes hold of the player, and fire resistance fails. Water puts it out.

Runs when: power is at least 0.6, the difficulty is not peaceful, health is above 2 hearts, and the player is not in water and not fire immune.

## `withering`

**Harmful, brink band: can leave half a heart.**
A wither effect sets in and goes on and on. Milk ends it.

Runs when: power is at least 0.6, the difficulty is not peaceful, health is above 2 hearts, and the player has no wither effect.

## `count`

**Harmful, brink band: can leave half a heart.**
A countdown appears on the screen: three, two, one. The way out is to reach light or holy ground before it ends.

Runs when: power is at least 0.6, the difficulty is not peaceful, health is above 2 hearts, and the player is not in light.

## `the_hollow`

**Harmful, brink band: can leave half a heart.**
A named creature of the spirit hunts the player, and its blows cannot kill. Killing it calms the spirit.

Runs when: power is at least 0.6, the difficulty is not peaceful, and health is above 2 hearts, with standing room for a hostile.

## `ledge_push`

**Harmful, brink band: can leave half a heart.**
The player is shoved hard at the edge of a drop. The way out is to crouch when the warning comes.

Runs when: power is at least 0.6, the difficulty is not peaceful, health is above 2 hearts, and the player is at the edge of a drop.

## `circle_of_knives`

**Harmful, brink band: can leave half a heart.**
Arrows hang in a ring around the player, then close in. The way out is to break out of the ring.

Runs when: power is at least 0.6, the difficulty is not peaceful, and health is above 2 hearts.

## `smother_brink`

**Harmful, brink band: can leave half a heart.**
The air is taken from the player in the dark, past the safe point. The way out is to find light.

Runs when: power is at least 0.6, the difficulty is not peaceful, health is above 2 hearts, the player is not in water, and the light level is below `dark_below`.

## `embrace_brink`

**Harmful, brink band: can leave half a heart.**
Cold hands close around the player, harder each time, past the safe point. The way out is to get ten blocks away.

Runs when: power is at least 0.6, the difficulty is not peaceful, and health is above 2 hearts.

## `red_room`

**Harmful, brink band: can leave half a heart.**
Darkness falls and a heartbeat sounds, and a figure in sight strikes every second the player looks at it. The way out is to look away.

Runs when: power is at least 0.6, the difficulty is not peaceful, and health is above 2 hearts, with standing room for a figure.

## `drain`

**Harmful, brink band: can leave half a heart.**
The player's health drains a heart a second and the hunger bar empties. The way out is to eat.

Runs when: power is at least 0.6, the difficulty is not peaceful, and health is above 2 hearts.

## `bound`

**Harmful, brink band: can leave half a heart.**
The player is held in place while a hostile is dragged to them. The way out is to kill it.

Runs when: power is at least 0.6, the difficulty is not peaceful, and health is above 2 hearts, with standing room for a hostile.

## `judgement`

**Harmful, lethal band: can kill.**
Three knocks sound, then a bolt misses, and then a bolt kills whoever has not moved.

Runs when: the spirit is an evolved `demon` at power 0.9 or more, the difficulty is not peaceful, the lethal setting allows it, a brink event has warned the player, and the cooldown has passed.

## `reaping`

**Harmful, lethal band: can kill.**
A shadow stands far off and walks closer whenever the player looks away. If it reaches them, its touch kills. It freezes while watched.

Runs when: the spirit is an evolved `shade`/`wraith` at power 0.9 or more, the difficulty is not peaceful, the lethal setting allows it, a brink event has warned the player, the cooldown has passed, and there is standing room for the shadow.

## `last_call`

**Harmful, lethal band: can kill.**
A moan sounds, then a voice draws the player toward lava or the void and does not stop at any floor. Walking far enough away breaks it.

Runs when: the spirit is an evolved `lost_one` at power 0.9 or more, the difficulty is not peaceful, the lethal setting allows it, a brink event has warned the player, the cooldown has passed, it is night, and there is lava or a bottomless column near.

## `drowner`

**Harmful, lethal band: can kill.**
In water, bubbles and a gurgle appear, then the player is held under until the water takes them. Leaving the water in time, or breathing under it, saves them.

Runs when: the spirit is an evolved `nix` at power 0.9 or more, the difficulty is not peaceful, the lethal setting allows it, a brink event has warned the player, the cooldown has passed, and the player is in water.

## `the_avenger`

**Harmful, lethal band: can kill.**
After a killing spree, a named avenger rises and hunts the player to the death. Beating it spends the spirit's anger.

Runs when: the spirit is an evolved `revenant` at power 0.9 or more, the difficulty is not peaceful, the lethal setting allows it, a brink event has warned the player, the cooldown has passed, a killing spree has happened (`min_kills` fresh mob kills), and there is standing room for the avenger.

## `hellring`

**Harmful, lethal band: can kill.**
A roar sounds and a ring of fire closes in over ten seconds. Whoever is still inside when it shuts burns to death.

Runs when: the spirit is an evolved `demon` at power 0.9 or more, the difficulty is not peaceful, the lethal setting allows it, a brink event has warned the player, and the cooldown has passed.

## `the_double`

**Harmful, lethal band: can kill.**
A copy of the player walks up and kills with one touch. Three blows from the player drive it away.

Runs when: the spirit is an evolved `phantom` at power 0.9 or more, the difficulty is not peaceful, the lethal setting allows it, a brink event has warned the player, the cooldown has passed, and there is standing room for the double.

## `foretold`

**Harmful, lethal band: can kill.**
A bell tolls and a true omen appears in the journal: at dusk the spirit takes the player, unless they are in a bed or inside a ward by then.

Runs when: the spirit is an evolved `banshee` at power 0.9 or more, the difficulty is not peaceful, the lethal setting allows it, a brink event has warned the player, and the cooldown has passed.
