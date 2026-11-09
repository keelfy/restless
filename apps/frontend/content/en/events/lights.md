---
title: Lights
---

# Lights

19 events.

## `extinguish_light`

Snuffs a lit candle; a torch is knocked off and drops as an item

Runs when: A lit candle or a torch / soul torch (floor or wall) is within 10 blocks

## `held_light_out`

The torch or lantern stack in one hand goes out: the whole stack becomes its unlit item, with a fire hiss (volume 0.6, pitch 1.4). One hand at random if both hold one

Runs when: A torch or lantern in either hand that can be snuffed

## `candles_out`

Every lit candle within 10 blocks goes out. A faint dark haze hangs for 2 s where each candle was.

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
Torches break, candles, campfires and lit lamps within 16 go out; 0.75 s later a zombie or skeleton appears 5–9 blocks away and targets the player. A faint dark haze puffs where each light was.

Runs when: Not peaceful, not daylight under open sky, a light within 16, standing room 5–9 blocks away

## `burning_hand`

**Harmful.**
The item drops out of the hand

Runs when: Torch or lantern in main hand

## `torch_swap`

Becomes another kind (torch, soul, copper; lantern, soul lantern), facing kept; swapped back after 60 seconds

Runs when: A torch, wall torch or lantern within 10

## `spirit_candle_out`

The candle's flame, lean and blue warning stop for 30 s, with the extinguish sound and a puff of smoke

Runs when: The spirit candle in a hand, not already out

## `far_house`

The 9×9×5 volume round the player is shown as fake blocks on open ground far off, with a lantern lit on the floor; only this player's client, put back after 30 s

Runs when: Overworld at night, under a roof; open ground 28–44 away

## `black_candle`

The candle turns black and lights itself (or a lit black candle is set down), with soul fire flames for 20 s. Put back after 2 min (stays under hardcore)

Runs when: Indoors, an unlit candle within 10, or free floor to set one on
