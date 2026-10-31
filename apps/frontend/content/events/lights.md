---
title: Lights
---

# Lights

15 events.

## `extinguish_light`

Snuffs a lit candle; a torch is knocked off and drops as an item

Runs when: A lit candle or a torch / soul torch (floor or wall) is within 10 blocks

## `candles_out`

Every lit candle within 10 blocks goes out

Runs when: A lit candle within 10 blocks

## `torch_chain`

Up to 8 torches knock off one by one, every 0.3 s, nearest to the player last

Runs when: A torch within 10

## `torches_behind`

Up to 8 torches behind the player's facing turn unlit one by one, every 0.75 s, farthest first; relit after 2 min unless changed (hardcore: stay out)

Runs when: Underground, a torch within 16 behind the player

## `lamp_off`

Switches it off for 1 s, back on if still powered

Runs when: A lit redstone lamp within 10

## `lantern_flicker`

Out and back on three times in 1.2 s, ends in place (stays out in hardcore)

Runs when: A lantern or soul lantern within 10

## `campfire_out`

Snuffs one campfire

Runs when: A lit campfire within 10

## `furnace_dies`

The fire goes out and the fuel is taken out for 5 s, then returned (dropped if the slot was filled meanwhile or the furnace is gone)

Runs when: A burning furnace, blast furnace or smoker within 10 with fuel in the fuel slot

## `dimming`

The player's view fades toward black over 9 s (peak 80%) and back over 6 s; client-side overlay under the HUD, no block or light level changes

Runs when: Always

## `blinking`

The dimming overlay, 3–6 quick cycles (0.15 s down, 0.2 s held at 95%, 0.25 s up)

Runs when: Always

## `candles_light`

Lights one candle

Runs when: An unlit candle within 10

## `new_face`

Turns it to face another way

Runs when: A jack o'lantern within 10

## `blackout`

**Harmful.**
Torches break, candles, campfires and lit lamps within 16 go out; 0.75 s later a zombie or skeleton appears 5–9 blocks away and targets the player

Runs when: Not peaceful, not daylight under open sky, a light within 16, standing room 5–9 blocks away

## `burning_hand`

**Harmful.**
The item drops out of the hand

Runs when: Torch or lantern in main hand

## `torch_swap`

Becomes another kind (torch, soul, copper; lantern, soul lantern), facing kept; swapped back after 60 seconds

Runs when: A torch, wall torch or lantern within 10
