---
title: Presence
---

# Presence

106 events.

## `dragon_flyover`

Only this player's client is told of an ender dragon flying a straight 240-block line 40 blocks above them, up to 30 to one side, at a random bearing, in 10 s, with its growl at the start (its wing flaps are the client's own). Nothing on the server

Runs when: Overworld, sky visible

## `edge_figure`

A silent, unhurtable mannequin with the player's own skin stands there facing them. Gone, with no sound or particles, when the player looks within 25° of it with a clear line, or after 30 s

Runs when: No such figure already, standing room 16 blocks away, 45–65° to a side of where the player looks

## `fog_silhouette`

A mannequin in the default skin stands far off. Gone, silently, when the player comes within 24 blocks, or after 60 s

Runs when: Open sky and rain or darkness outside, standing room 40 blocks away within 25° of where the player looks

## `distant_player`

A silent, unhurtable mannequin in a random default skin (Steve, Alex and the rest) stands there facing them. Gone, silently, when the player comes within 35 blocks, or after 60 s

Runs when: Standing room 60 blocks away within 40° of where the player looks (up to 16 bearings tried; grass and flowers count as room)

## `friend_double`

A silent, unhurtable mannequin with the skin and name of another player who stands near (within 16 blocks, same dimension, not a spectator; one at random) stands 50 blocks away facing the player, its name shown over its head. Gone, silently, when the player comes within 30 blocks, or after 60 s

Runs when: No such figure already, another such player near, standing room 50 blocks away within 40° of where the player looks (up to 16 bearings tried)

## `distant_walker`

A silent, unhurtable mannequin in a random default skin walks across the player's view at walking pace, or (40%) sprints at 0.28 blocks/tick and jumps now and then; it jumps up one-block steps, falls off ledges up to 2 deep, goes through grass and flowers, and turns up to 80° off to go round what is in its way. Gone, silently, when the player comes within 25 blocks, when no way on is left, or after 30 s

Runs when: Standing room 45 blocks away within 40° of where the player looks (up to 16 bearings tried; grass and flowers count as room)

## `nametag`

A nameplate (text display, seen through walls) with a made-up nickname (as spirits get, at most 16 characters) floats 3 blocks under the player's feet or at head height over that standing room, with nobody under it. Gone after 10 s

Runs when: Half the time standing room 30 blocks away within 40° of where the player looks; otherwise none

## `pillar`

A silent, unhurtable mannequin in a random default skin, a block in hand (cobblestone, stone, dirt or any planks), every 0.5 s looks down at its feet and jumps (a player's jump arc), puts that block (its place sound) where its feet were once above it, swings its arm and lands on it, 8 times; at the top it faces the player. It waits at the top and is gone 7 s after it appeared; sooner if the player comes within 10 blocks or the way up is blocked. Each block goes back to air after 2 min if still that block; under hardcore the pillar stays

Runs when: Standing room 24 blocks away within 40° of where the player looks, open for 8 blocks above

## `harvest`

A silent, unhurtable mannequin in a random default skin stands there and every 0.4 s turns to the nearest ripe crop within 4 blocks, swings and breaks it (particles and sound, no drops). Gone when none is left, when the player comes within 5 blocks, or after 10 s. Each crop comes back at its age after 2 min if its spot is still empty; under hardcore they stay gone

Runs when: A ripe crop within 16 blocks with standing room within 4 blocks of it, at least 8 blocks from the player

## `footprints`

For 6 s, puffs of the ground and a step sound appear where the player stood a second ago. Particles only, nothing is placed

Runs when: Standing on or in sand or snow

## `cold_breath`

A breath puff at the face

Runs when: Warm biome

## `local_rain`

Rain particles and sound over the player for 6 s

Runs when: Sky visible, not raining

## `lost_time`

**Harmful.**
The overworld clock jumps 1000 ticks (affects everyone)

Runs when: Night

## `guardian_face`

The elder guardian face and sound, client only

Runs when: Always

## `false_totem`

The totem of undying animation (a plain totem if none is carried)

Runs when: Always

## `subliminal_title`

A dark red title from `texts` for 3 ticks with no fade; fade times reset after

Runs when: Always

## `red_vignette`

The client draws a red tint at the screen's edges, peaking at 80%, fading in and out over 5 s (`RedVignette`).

Runs when: Always

## `false_death`

Their own "was slain by ???" death message, sent only to them

Runs when: Always

## `private_rain`

Rain for this client only, a storm 30% of the time, for 20 s; then the real weather

Runs when: Weather dimension, not raining, sky visible

## `silent_lightning`

A visual-only bolt 24 blocks away

Runs when: Weather dimension, sky visible

## `false_night`

Only this player's client is told it is midnight, with the clock stopped. 15 s later it gets the real time back. Nothing on the server

Runs when: Overworld, daytime

## `false_slot`

Only this player's client is told that slot holds an item from a list of 25 (skulls and heads, bones, rotten flesh, spider eyes, a totem, disc 11 and more; a player head is their own). 20 s later, or at any click, the real slot comes back. Nothing on the server

Runs when: An empty inventory or hotbar slot

## `false_advancement`

Grants a hidden advancement (`restless:whisper/*`, an impossible criterion, no chat line) so the toast shows, and revokes it 2 ticks later

Runs when: None

## `turned_head`

**Harmful.**
A step sound of the ground at the spirit's position (or 6 blocks away at a random bearing), then the camera turns toward it, up or down as well, over 30 ticks, eased, a little every frame on the client (`TurnPayload`, `HeadTurn`); the mouse still works meanwhile

Runs when: None

## `stranger_joins`

Only this player gets "\<name> joined the game" and a player list entry for a made-up nickname (as spirits get, at most 16 characters), in a default skin. 40 s later the entry goes and "\<name> left the game" follows. Nothing on the server

Runs when: None

## `stranger_death`

Only this player gets a vanilla death message (fell, drowned, suffocated, fell out of the world, died) for a made-up nickname

Runs when: None

## `false_whisper`

A gray italic "X whispers to you" line from a real online player; off by default since it impersonates a real person

Runs when: Another player online

## `pumpkin_head`

Only this player's client is told the head slot holds a carved pumpkin, so the pumpkin blur covers the screen. 4 s later the real slot comes back. Nothing on the server. In hardcore, a real pumpkin is put on an empty head slot and taken off again

Runs when: Always

## `xp_drain`

The client is told the experience bar and level are zero for 3 s, then the real values

Runs when: Has experience points or levels

## `phantom_flames`

The client is told the player is on fire for 3 s, so the fire shows on screen. Nothing burns; the real flags come back after. In hardcore, the player is set on fire for 1 s

Runs when: Not on fire, not in water

## `phantom_drowning`

The client is told the air supply drains from full to 20% over 4 s, so bubbles show on land; then the real value

Runs when: Full air

## `phantom_frost`

The client is told the frozen ticks rise to full over 2.5 s, so frost creeps over the screen, held until 5 s; then the real value. In hardcore, the player really freezes

Runs when: Not freezing

## `moon_phase`

Only this player's client is shown the clock moved a random 1 to 7 whole days on, so the moon has another phase and the time of day is the same. 20 s later it gets the real time back. Nothing on the server

Runs when: Overworld, night

## `false_boss_bar`

A purple boss bar with a name from `names` is sent to this player only. It fills over 3 s, empties over 2 s and is removed. No boss exists

Runs when: Always

## `action_bar_whisper`

A dark red line from `lines` ("Don't turn around" and others) on the action bar, cleared after 2 s

Runs when: Always

## `false_absorption`

The client is told the player has 8 absorption (4 golden hearts), draining to 0 over 4 s; then the real value

Runs when: No absorption

## `empty_armor_bar`

The client is told the armor attribute is 0 for 4 s, so the armor bar vanishes; then the real attribute

Runs when: Wearing armor

## `hurt_tilt`

The hurt animation (screen tilt) from a random side, with no damage

Runs when: Always

## `own_hand_swings`

The player's own main hand swings twice, 14 ticks apart

Runs when: Always

## `night_eyes`

The client is told of 1.5 s of night vision, then 4 s of darkness; then the real effects. No effect exists on the server

Runs when: No night vision or darkness

## `all_on_cooldown`

Every item on the hotbar and in the hands is shown on a 2 s cooldown. The client counts it down by itself

Runs when: An item on the hotbar or in the hands

## `noon_moon`

Only this player's client is told it is midnight under a full moon, with the clock stopped. 10 s later it gets the real time back. Nothing on the server

Runs when: Overworld, daytime

## `black_petals`

For 10 s ash particles fall in a cloud above and around the player, like petals

Runs when: In the cherry grove

## `roof_figure`

A silent, unhurtable mannequin in a random default skin crouches on top of a roof block, facing the player. Gone, silently, when the player comes within 10 blocks or after 30 s. Where it stood a faint dark haze stays for 3 s.

Runs when: A stair, slab or planks block open to the sky within 20 blocks, at least 12 blocks from the player

## `hill_waver`

A silent, unhurtable mannequin in a random default skin stands on a hilltop facing the player and swings its arm every 0.4 s. Gone, silently, when the player comes within 20 blocks or after 20 s. Where it stood a faint dark haze stays for 3 s.

Runs when: Standing room open to the sky 45 blocks away within 40° of where the player looks, at least 6 blocks above the player

## `wall_miner`

A silent, unhurtable mannequin in a random default skin stands behind a wall and every 0.4 s swings at it: cracks run up the block over 10 steps, only on this player's client, with its digging sound, then clear. The wall is never broken. Gone when it is done, when the player comes within 2.5 blocks, or after 10 s

Runs when: A one-block-thick wall two blocks high in front of the player, with standing room behind it

## `table_figure`

A silent, unhurtable mannequin in a random default skin stands at a crafting table looking down at it. Gone, silently, when the table is gone, when the player comes within 4 blocks, or after 15 s

Runs when: A crafting table within 16 blocks with standing room within 2 blocks of it, at least 6 blocks from the player

## `cave_torches`

Only this player's client is told that a line of floor spots along a dark tunnel holds torches. 1 min later it gets the real blocks back

Runs when: Under a roof, and a bearing with at least 3 dark floor spots 6 to 28 blocks off, out of the sky

## `own_walker`

A silent, unhurtable mannequin in the player's own skin walks across their view, jumping up one-block steps, dropping off ledges up to 2 deep and going round what is in its way. Gone, silently, when the player comes within 25 blocks, when no way on is left, or after 30 s

Runs when: Standing room 45 blocks away within 40° of where the player looks

## `night_runner`

A silent, unhurtable mannequin in a random default skin sprints across the view at 0.36 blocks/tick, jumping now and then. Gone, silently, when the player comes within 7 blocks, when no way on is left, or after 15 s

Runs when: Light at the player 4 or less, and standing room 22 blocks away within 35° of where the player looks

## `bed_sleeper`

A silent, unhurtable mannequin in a random default skin lies asleep in a free bed; the bed is not marked taken. Gone, silently, when the player comes within 6 blocks or after 20 s

Runs when: A free bed within 24 blocks with room above it, at least 8 blocks from the player

## `shrieker_figure`

A silent, unhurtable mannequin in a random default skin crouches by a sculk shrieker looking at it. Gone, silently, when the shrieker shrieks, when the player comes within 6 blocks, or after 20 s

Runs when: Deep dark biome, and a sculk shrieker within 24 blocks with standing room beside it, at least 8 blocks from the player

## `hello_chat`

Only this player gets a chat line `<name> hello?` (from `lines`) under a made-up nickname

Runs when: A line is left that the spirit has not said

## `ghost_teleport`

Only this player gets the command feedback `Teleported ??? to <player>`. Nobody is moved

Runs when: A name is left that the spirit has not said

## `stranger_advancement`

Only this player gets the chat line `<name> has made the advancement [Who's There?]` under a made-up nickname

Runs when: A title is left that the spirit has not said

## `grave_figure`

A silent, unhurtable mannequin in a random default skin stands where the player last died, facing them. Gone, silently, when the player comes within 12 blocks or after 20 s. Where it stood a faint dark haze stays for 3 s.

Runs when: The last death was in this dimension, 20 to 60 blocks away, in a loaded chunk with standing room

## `bedside_figure`

Waits up to 20 min for the player to sleep and wake. Then a silent, unhurtable mannequin in a random default skin stands next to the bed for 1.25 s, facing the player. Where it stood a faint dark haze stays for 3 s.

Runs when: No such wait already

## `black_ash`

Dark ash falls around the player from 2 blocks up, 24 bursts 0.25 s apart

Runs when: Indoors

## `border_closes`

Only this player's client is shown a world border centered on them, shrinking from 80 to 40 blocks wide over 4 s and held 2 s; then the real border. Nothing on the server

Runs when: No real border within 80 blocks

## `false_recipe`

Only this player's client gets a "recipe unlocked" toast for a made-up bone and skull recipe (the result is a skull, bone block, soul lantern or wither rose); 10 s later it is removed from the recipe book. No recipe exists

Runs when: Always

## `view_narrows`

The client is told the movement speed attribute is 0.02 for 1 s, so the field of view pinches; then the real attribute

Runs when: Always

## `mob_eyes`

Only this player's client is sent to see through a spider, cave spider, creeper or enderman within 24 blocks for 2.5 s, with the vanilla shader of that mob; then back. The view also ends early if the mob dies or vanishes, the player is hurt or changes dimension; death, logout and server stop reset it too. Nothing changes on the server

Runs when: Player on the ground, not in a fluid, and one of those mobs within 24 blocks

## `out_of_body`

The view moves to a packet-only entity 3.5 blocks behind the player (nearer if blocked) and 0.6 above their eyes, looking at the back of their head, for 3 s; then back and the entity is removed. Same early resets as `mob_eyes`

Runs when: Player on the ground, not in a fluid

## `spirit_sight`

The view moves for 1 s to the spirit's point (10 blocks away at a random bearing when no spirit sent it), looking at the player; does nothing if that point is over 40 blocks away or not loaded. Same early resets as `mob_eyes`

Runs when: Player on the ground, not in a fluid

## `lake_fisher`

A silent, unhurtable mannequin in a random default skin stands on the bank with a fishing rod, facing the water, casts, and every 4th second swings and splashes (fishing particles and sounds). No bobber is drawn. Gone, silently, when the player comes within 6 blocks or after 30 s

Runs when: A lake (a water source with water on three sides for 3 blocks) within 16 blocks, with standing room on its bank at least 8 blocks from the player

## `window_figure`

A silent, unhurtable mannequin in a random default skin stands behind the glass facing the player. Gone, silently, when the player comes within 3 blocks or after 20 s

Runs when: The player is under open sky; a glass block or glass pane within 14 blocks has a roofed room behind it (standing room one block below or level with it, at least 4 blocks from the player) and open air on the player's side

## `trailing_figure`

A silent, unhurtable mannequin in a random default skin faces the player and steps toward them by as far as the player moved each 0.1 s (up to 1 block), so it keeps its distance while they walk and stops when they stop; it sprints when they do. Gone, silently, when the player looks within 12° of it, comes within 8 blocks, or after 60 s

Runs when: Standing room 30 blocks behind the player (150-180° from where they look)

## `deep_swimmer`

A silent, unhurtable mannequin in a random default skin lies in the swimming pose in the water there and swims across the view at 2.4 blocks/s, trailing bubbles. Gone, silently, when the water ends, when the player comes within 2 blocks, or after 15 s

Runs when: The player's head is underwater, with water 8 blocks away within 15-60° of where they look (12 bearings tried)

## `note_dropper`

A silent, unhurtable mannequin in a random default skin faces the player for 1.6 s, then drops a torch or (half the time) paper named with a short note, and sprints away from the player. The item is real and stays. Gone, silently, when the player comes within 2 blocks, when no way on is left, or after 20 s

Runs when: Standing room 14 blocks away within 30° of where the player looks

## `horizon_choir`

Six silent, unhurtable mannequins in random default skins stand there, all facing the player. All gone at once, silently, when the player looks within 10° of any of them, comes within 20 blocks of any, or after 30 s

Runs when: The player is under open sky and standing room is found at the surface for at least 4 of 6 points 45-51 blocks away spread over 60° of where they look

## `blade_stalker`

A silent, unhurtable mannequin in a random default skin crouches with an iron sword and creeps toward the player at 1.3 blocks/s, facing them. It does not move while the player looks within 25° of it, and is gone after 2 s of being looked at in all, or when it comes within 3 blocks, or after 30 s

Runs when: Standing room 16 blocks behind the player (140-180° from where they look)

## `cliff_faller`

A silent, unhurtable mannequin in a random default skin sprints along that line off the edge and falls; after 5 blocks of falling it makes the player death sound and is gone, silently, with no body. Gone too if the player comes within 6 blocks or after 15 s

Runs when: Standing room 12-22 blocks away with flat ground within 6 blocks in some direction ending in an 8-40 block drop onto solid ground (24 tries)

## `lake_ferry`

A silent, unhurtable mannequin in a random default skin sits in an invulnerable, silent oak boat there (real entities, removed with the figure) and sails across the player's view along the longer stretch of open water at 2.4 blocks/s, with paddle sounds. Gone, with the boat, when the player comes within 6 blocks, when the water ends, or after 30 s

Runs when: A lake (as for `lake_fisher`) within 16 blocks, with a surface point at least 10 blocks from the player

## `cave_cart`

A silent, unhurtable mannequin in a random default skin sits in an invulnerable, silent minecart (real entities, no rails needed) that rolls along the ground across the view at 6 blocks/s, going round or up and down small obstacles, with rolling sounds. Gone, with the cart, when the player comes within 5 blocks, when no way on is left, or after 20 s

Runs when: The player is below y 50 with no open sky above, and cave standing room 14 blocks away within 10-40° of where they look

## `spyglass_figure`

Waits up to 2 min for the player to raise a spyglass, then tells their client of a villager 48 blocks away along where they look. It is removed as soon as the spyglass is lowered, or after 1 min. Nothing on the server

Runs when: The player carries a spyglass

## `flash_figure`

Four visual-only lightning bolts, 2 to 4.5 s apart; at each, only this player's client is told of a wither skeleton standing on one bearing for 4 ticks, 34 blocks away at the first flash and 7 blocks closer each time, with the bolt striking beyond it. Nothing on the server

Runs when: Overworld, raining, sky visible

## `jolt_behind`

A loud bang 1.6 blocks behind the player, at 150–180° from where they look, and only this player's client is told of a black figure of no known kind standing there, one of ten wrong shapes (tall and thin with arms to the knees; low and wide on all fours; a head hung on one shoulder; a small child with arms up; many thin arms; a stooped giant; bent double with its head held up; a tall one and a small one joined by the hands; a body on a very long neck; hung off the ground with its head dropped on a shoulder). It is not shown until the player looks at it; 12 ticks after they first do it is gone (or after 6 s if they never turn). Nothing on the server. The figure twitches in sudden small jerks, its head sways, and its two pale eyes glow in the dark. A jump scare: after one runs for a player (this or any other jump scare), none rolls for them again for 10 min

Runs when: Standing room 1.6 blocks behind

## `jolt_side`

A loud bang 1.8 blocks away at 60–100° to one side, with a black figure of one of the ten shapes there for this player's client only, unseen until the player looks at it, and gone 10 ticks after (or after 5 s unseen). Nothing on the server

Runs when: Standing room 1.8 blocks to the side

## `jolt_dark`

A black figure of one of the ten shapes 2 blocks ahead, within 20° of the player's view, in a spot no brighter than light level 4, shown only to this player's client at the same instant as a loud bang. Gone 8 ticks later. Nothing on the server

Runs when: A dark standing spot 2 blocks ahead

## `jolt_lunge`

Out of sight, 8–12 blocks away, in a spot no brighter than light level 6 behind a wall or a corner, a faint rustle sounds every 2 s for up to 30 s. If the player comes within 4.5 blocks of the spot, a black figure of one of the ten shapes stands there for this player's client only and is at their face (0.7 blocks ahead) 3 ticks later, with a loud bang as it arrives. Gone 6 ticks after that. If nobody comes near, it just stops. Nothing on the server. A jump scare, so it shares the 10 min pause with the other jump scares

Runs when: A dark standing spot 8–12 blocks away that the player cannot see

## `mined_face`

The next time the player breaks a block at eye level in a wall (underground, within 6 blocks, with rock two blocks deep behind it), the rock behind the hole shows as open space for 8 ticks, and a bare black face with pale eyes looks out of it, with a loud bang. Only this player's client is told. A jump scare, so it shares the 10 min pause with the other jump scares. If they break no such block within 5 min, it lapses

Runs when: Underground, and no other waiting scare marked for the player

## `dig_down`

The next time the player breaks a floor block beside them (1–4.5 blocks off, not the one under their feet, with rock two blocks deep under it), the rock below shows as open for 8 ticks, and a black figure lies in the hole on its back, face up, with a loud bang. Only this player's client is told. A jump scare. Lapses after 5 min

Runs when: Underground, and no other waiting scare marked for the player

## `eating`

The next meal the player finishes ends with a loud bang and a black figure 1.3 blocks ahead of them for 10 ticks. Only this player's client is told. A jump scare. Lapses after 5 min

Runs when: No other waiting scare marked for the player

## `container_close`

The player has a chest, barrel, furnace or crafting table within 6 blocks. Whatever container they open and keep open for 3 s, when they close it a black figure stands 1.3 blocks ahead for 10 ticks, with a loud bang. Only this player's client is told. A jump scare. Watches for 5 min

Runs when: A chest, barrel, furnace or crafting table within 6 blocks

## `slow_turn`

A black figure stands 24 blocks ahead with its back turned. Each time the player looks at it and then away, it has turned 45° more. After four turns it faces them; the next time they look away it is gone from there and stands 1.6 blocks behind them, with a loud bang, unseen until they turn. Gone if they walk within 8 blocks of it or after 60 s. Only this player's client is told. A jump scare

Runs when: Standing room 24 blocks ahead

## `echo_answer`

In a cave (no sky overhead), each block the player breaks is answered 1.5 s later by the same break sound, from 20 blocks away, then 12, then 6; the fourth comes from 1.5 blocks right behind them. Lapses after 5 min

Runs when: Underground, and no other waiting scare marked for the player

## `name_left_right`

A whisper 2 blocks to one side, every 1.5 s. Once the player has turned 45° toward it, a black figure stands 1.5 blocks away on the other side for 8 ticks, with a loud bang. Only this player's client is told. A jump scare. Gives up after 20 s

Runs when: Always

## `door_slam_face`

The player has a door within 4 blocks. When a door that was open shuts while they are within 3.5 blocks of it, it shows open again for 10 ticks with a black figure standing in it, and a loud bang. Only this player's client is told. A jump scare. Watches for 5 min

Runs when: A door within 4 blocks

## `first_light`

The next torch the player puts up in a dark cave (light level 3 or less at their feet, no sky) lights a black figure standing within 6 blocks ahead of them for 4 ticks, with no sound. Only this player's client is told. A jump scare. Lapses after 5 min

Runs when: Underground, and no other waiting scare marked for the player

## `behind_trunk`

The next time the player breaks the bottom log of a tree trunk (a log above it, none below, within 5 blocks), a black figure stands on the ground right behind the gap, facing them, for 8 ticks, with a loud bang; the log above shows as open air meanwhile so that all of the figure can be seen. Only this player's client is told. A jump scare. Lapses after 5 min

Runs when: A log within 10 blocks, and no other waiting scare marked for the player

## `hide`

The action bar says "Hide" and a heartbeat sounds every second for 10 s. If the player moves more than 1 block, a black figure that was out of sight is at their face in 4 ticks with a loud bang and hits for 4 (it never takes them below the safe floor of the threat band). If they keep still, four heavy steps pass outside. A harmful event, so a lit omen candle warns of it

Runs when: Underground, health above the safe floor and a margin, and standing room 5 blocks away

## `third_person`

The view pulls out behind the player's head for 3 s (as in `out_of_body`), and a black figure stands 1.6 blocks behind them in it. When the view snaps back, the figure is put there again with a loud bang, unseen until the player turns round, and gone 10 ticks after they see it. If they never turn it is gone after 6 s. Only this player's client is told. A jump scare

Runs when: The player stands on the ground, and there is standing room 1.6 blocks behind them

## `pit_face`

While the player is on the ground within 3 blocks of a drop of 8 blocks or more and looks steeply down (60° below level or more), a black figure at the bottom rises to 1.5 blocks below their eyes in 5 ticks, with a loud bang as it arrives, and is gone 8 ticks later. Only this player's client is told. A jump scare. Watches for 5 min

Runs when: On the ground, with a drop of at least 8 blocks within 3 blocks

## `shield_knocks`

A shield held up for 2 s with no hostile mob within 16 blocks is knocked on three times, 0.5 s apart, from 2.5 blocks ahead. When the player lowers it after that, a black figure stands 1.2 blocks ahead for 10 ticks, with a loud bang. Only this player's client is told. A jump scare. Watches for 5 min

Runs when: A shield in either hand

## `tree_line`

A black figure stands 20 blocks ahead of the player in a dark spot (light level 7 or less). Each time the player looks at one and then away, another stands somewhere ahead of them, up to five. The look away after that removes them all and puts one 1.6 blocks behind the player, with a loud bang, unseen until they turn round, and gone 12 ticks after they see it. Watches for 2 min. Only this player's client is told. A jump scare

Runs when: Overworld, at night, with a log within 16 blocks

## `stairs_sitter`

A flight of at least three stairs, 8 to 16 blocks away and in view, has a low black figure on its top step. When the player comes within 6 blocks of it, it is gone, and three heavy steps sound 2 blocks behind them, 0.4 s apart. Only this player's client is told. If they never come near it is gone after 1 min

Runs when: A flight of three or more stairs in view 8–16 blocks away, with room on its top step

## `overnight_shrine`

A row of bone blocks with two skulls and lit candles on top and a sign with a few words in front. Phantom: only this player's client is told of it, the server keeps open ground, a click shows the real air; gone after 10 min (with the swaps setting `REAL` or under hardcore the bone, skulls and candles are real blocks, put back after the time or never under hardcore)

Runs when: Overworld; open ground for a 3×3 shrine 6–14 away; a sign text the spirit has not used

## `ash_sign`

Ash laid as a ring of radius 3 or as the initial of the spirit's name, readable from the player's side (cells on bare floor only, at least 70%); swept away after 5 min, stays under hardcore

Runs when: Under a roof; bare floor ahead (3 away, within 25° of the view) for the shape

## `screen_handprint`

A pale handprint over the player's view for 3.5 s (peak 80%), quick in and slow out; client-side overlay under the HUD

Runs when: The client has the mod

## `screen_frost`

Frost along the screen's edges for 8 s (peak 70%), in over 2.4 s and out over 4 s; client-side overlay

Runs when: The client has the mod

## `screen_face`

A pale face over the view for 2 ticks (90%); weight 0, so it never runs until a player raises it

Runs when: The client has the mod

## `eyes_in_dark`

A pair of eyes hangs 1.5 blocks above a dark spot for 2–3 s, blinks once and goes out; a client-only particle, fully lit

Runs when: Standing room 7–14 blocks ahead (within 60°) with light level at most 3

## `frost_prints`

Up to 3 frost handprints appear one by one, 1.5 s apart, on the outer face of the glass, each with a tap on glass. Each is removed after 2 min; under hardcore they stay

Runs when: Indoors, glass or a glass pane within 8 with a free block on its far side from the player

## `frost_writing`

A phrase (`let me in`, `cold`, `help`, `open`) forms word by word, 2 s apart, one word per window, read left to right from the player's side. Each word is removed after 2 min; under hardcore they stay

Runs when: Indoors, a run of glass within 8 with free blocks on the player's side, long enough for a phrase

## `doll`

A doll appears and, whenever it is not watched, turns to face the player and hops to another spot up to 4 times. It keeps still while watched. Gone after 2 min or when broken

Runs when: A spot of free floor under a roof within 10, 4+ away and outside 55° of where the player looks

## `armor_stand`

**Harmful.**
A real armor stand with arms, in one of four poses (reaching, arms raised, head lolling, lunging), stands facing the player. Every 0.2 s it steps up to 0.6 blocks toward them (climbing 1, dropping up to 3), but only while they are over 2 blocks off and not looking within 50° of it with a clear line; one move in four strikes a new pose. It breaks at the first blow and drops nothing. After 2 min it falls apart with plank particles (under hardcore it stays where it ended up)

Runs when: No such stand already, standing room 8 blocks away at 150–180° to either side of where the player looks, which is behind them (up to 16 bearings tried)
