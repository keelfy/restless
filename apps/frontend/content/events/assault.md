---
title: Assault
---

# Assault

15 events.

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
