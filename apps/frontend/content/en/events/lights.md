---
title: Lights
---

# Lights

19 events.

## `extinguish_light`

A lit candle is snuffed, or a torch is knocked off the wall or floor and drops as an item.

Runs when: a lit candle, or a torch or soul torch (on the floor or on a wall), is within 10 blocks.

## `held_light_out`

The torch or lantern stack in one hand goes out, and the whole stack becomes its unlit item, with a fire hiss (volume 0.6, pitch 1.4). If both hands hold one, a hand is chosen at random.

Runs when: either hand holds a torch or lantern that can be snuffed.

## `candles_out`

Every lit candle within 10 blocks goes out. A faint dark haze hangs for 2 s where each candle was.

Runs when: a lit candle is within 10 blocks.

## `torch_chain`

Up to 8 torches are knocked off one after another, every 0.3 s, with the one nearest to the player going last.

Runs when: a torch is within 10 blocks.

## `torches_behind`

Up to 8 torches behind the player's facing turn unlit one after another, every 0.75 s, starting with the farthest. After 2 min they are lit again unless they were changed in the meantime (in hardcore they stay out).

Runs when: the player is underground and a torch is within 16 blocks behind them.

## `lamp_off`

The lamp switches off for 1 s and then comes back on if it is still powered.

Runs when: a lit redstone lamp is within 10 blocks.

## `lantern_flicker`

The lantern goes out and comes back on three times within 1.2 s and ends up where it was (in hardcore it stays out).

Runs when: a lantern or soul lantern is within 10 blocks.

## `campfire_out`

One campfire is snuffed.

Runs when: a lit campfire is within 10 blocks.

## `furnace_dies`

The fire goes out and the fuel is taken out for 5 s, after which it is returned (or dropped, if the slot was filled in the meantime or the furnace is gone).

Runs when: a burning furnace, blast furnace or smoker with fuel in its fuel slot is within 10 blocks.

## `dimming`

The player's view fades toward black over 9 s (peak 80%) and returns over 6 s. This is an overlay on the client, beneath the HUD; no block or light level changes.

Runs when: at any time.

## `blinking`

The dimming overlay is shown as 3–6 quick cycles, each going down in 0.15 s, held at 95% for 0.2 s and rising again in 0.25 s.

Runs when: at any time.

## `candles_light`

One candle is lit.

Runs when: an unlit candle is within 10 blocks.

## `new_face`

The jack o'lantern turns to face another way.

Runs when: a jack o'lantern is within 10 blocks.

## `blackout`

**Harmful.**
Torches break, and candles, campfires and lit lamps within 16 blocks go out. Then, 0.75 s later, a zombie or skeleton appears 5–9 blocks away and targets the player. A faint dark haze puffs where each light was.

Runs when: the difficulty is not peaceful, it is not daylight under open sky, a light is within 16 blocks, and there is standing room 5–9 blocks away.

## `burning_hand`

**Harmful.**
The item drops out of the player's hand.

Runs when: the main hand holds a torch or lantern.

## `torch_swap`

The block becomes another kind (torch, soul or copper torch; lantern or soul lantern) with its facing kept, and is swapped back after 60 seconds.

Runs when: a torch, wall torch or lantern is within 10 blocks.

## `spirit_candle_out`

The candle's flame, its lean and its blue warning stop for 30 s, with the extinguish sound and a puff of smoke.

Runs when: a spirit candle is in a hand and is not already out.

## `far_house`

The 9×9×5 volume around the player is shown as fake blocks on open ground far off, with a lantern lit on the floor. Only this player's client sees it, and everything is put back after 30 s.

Runs when: it is night in the Overworld, the player is under a roof, and there is open ground 28–44 blocks away.

## `black_candle`

The candle turns black and lights itself (or a lit black candle is set down), burning with soul fire flames for 20 s. After 2 min everything is put back (under hardcore it stays).

Runs when: the player is indoors and either an unlit candle is within 10 blocks or there is free floor to set one on.
