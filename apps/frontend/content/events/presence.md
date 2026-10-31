---
title: Presence
---

# Presence

26 events.

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

## `distant_walker`

A silent, unhurtable mannequin in a random default skin walks across the player's view at walking pace, or (40%) sprints at 0.28 blocks/tick and jumps now and then; it jumps up one-block steps, falls off ledges up to 2 deep, goes through grass and flowers, and turns up to 80° off to go round what is in its way. Gone, silently, when the player comes within 25 blocks, when no way on is left, or after 30 s

Runs when: Standing room 45 blocks away within 40° of where the player looks (up to 16 bearings tried; grass and flowers count as room)

## `nametag`

A nameplate (text display, seen through walls) with a made-up nickname (as spirits get, at most 16 characters) floats 3 blocks under the player's feet or at head height over that standing room, with nobody under it. Gone after 10 s

Runs when: Half the time standing room 30 blocks away within 40° of where the player looks; otherwise none

## `pillar`

A silent, unhurtable mannequin in a random default skin, cobblestone in hand, every 0.5 s looks down at its feet and jumps (a player's jump arc), puts cobblestone (place sound) where its feet were once above it, swings its arm and lands on it, 8 times; at the top it faces the player. It waits at the top and is gone 7 s after it appeared; sooner if the player comes within 10 blocks or the way up is blocked. Each block goes back to air after 2 min if still cobblestone; under hardcore the pillar stays

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

The client draws a red tint at the screen's edges, peaking at 80%, fading in and out over 5 s (`RedVignette`). A client without the mod gets its border warning distance raised instead, so the vanilla tint shows (only with the vignette setting on)

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
A step sound of the ground at the spirit's position (or 6 blocks away at a random bearing), then the camera turns toward it over 30 ticks, eased, a little every frame on the client (`TurnPayload`, `HeadTurn`); the mouse still works meanwhile

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
