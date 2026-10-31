---
title: Water
---

# Water

5 events.

## `undertow`

Harmful.
Every 0.1 s the player is pushed down (a 0.12 downward velocity nudge, so at the surface they are kept from coming up). Ends out of the water, at 3 hearts, or after 10 s. Drowning itself is vanilla's and is not capped

Runs when: Health > 5 hearts, the player in water

## `water_call`

Harmful.
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
