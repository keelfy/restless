---
title: Water
---

# Water

9 events.

## `undertow`

**Harmful.**
Every 0.1 s the player is pushed down (a 0.12 downward velocity nudge, so at the surface they are kept from coming up). Ends out of the water, at 3 hearts, or after 10 s. Drowning itself is vanilla's and is not capped

Runs when: Health > 5 hearts, the player in water

## `water_call`

**Harmful.**
An underwater swell, then after 2 s the player on the ground is drawn toward the nearest water at about 1.5 blocks/s, with splash particles at the feet. Ends on arrival, in water, at 3 hearts, or after 20 s

Runs when: Health > 5 hearts, the player out of water, water within 12 blocks

## `drowned_fish`

An elder guardian's curse sounds, and up to 12 squid and fish within 24 blocks die with a puff of ink. They are lost for good (no drops)

Runs when: A squid or fish within 24 blocks

## `broken_boat`

A splintering sound and one of them breaks as if hit hard, dropping its item. A boat with anyone aboard is left alone

Runs when: An empty boat or raft within 24 blocks

## `boat_drifts`

A steady push away from the player for 10 seconds

Runs when: An empty boat floating within 24

## `swamp_lily_drift`

A lily pad within 10 steps one block toward the player every second, 6 times at most, stopping 2.5 blocks away; restored after 1 min (stays under hardcore)

Runs when: In a swamp or mangrove swamp, with a lily pad within 10 that has a free water block toward the player

## `water_guardian`

Only this player's client is told of a guardian 2 blocks under the water of a pond, with its beam set on the player and its attack sound, for 4 s. Nothing on the server

Runs when: Source water at least 4 deep with air above it, within 20 blocks and at least 6 away

## `sea_lights`

Only this player's client is told of a sea lantern that moves along the run, one block every 0.2 s, 2 lit at a time, each put back behind it. Nothing on the server

Runs when: In an ocean biome, a straight run of at least 8 water blocks 3 under the surface, starting 6 to 18 away

## `surface_break`

When the player's head comes out of the water after at least 2 s under it, a black figure stands on the water 1.5 blocks ahead for 10 ticks, with a splash and a loud bang. It does nothing if there is no open water with free air above it there. Only this player's client is told. A jump scare. Watches for 5 min

Runs when: The player is in water
