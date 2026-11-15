---
title: Water
---

# Water

8 events.

## `undertow`

**Harmful.**
Every 0.1 s the player is pushed down by a 0.12 downward velocity nudge, so at the surface they are kept from coming up. The event ends once the player is out of the water, at 3 hearts, or after 10 s. Drowning itself is vanilla's and is not capped.

Runs when: the player's health is above 5 hearts and the player is in water.

## `water_call`

**Harmful.**
An underwater swell sounds, and after 2 s the player on the ground is drawn toward the nearest water at about 1.5 blocks/s, with splash particles at their feet. The event ends on arrival, once the player is in water, at 3 hearts, or after 20 s.

Runs when: the player's health is above 5 hearts, the player is out of water, and there is water within 12 blocks.

## `drowned_fish`

An elder guardian's curse sounds, and up to 12 squid and fish within 24 blocks die with a puff of ink. They are lost for good, and they drop nothing.

Runs when: a squid or fish is within 24 blocks.

## `broken_boat`

A splintering sound is heard, and one of the boats breaks as if hit hard and drops its item. A boat with anyone aboard is left alone.

Runs when: an empty boat or raft is within 24 blocks.

## `boat_drifts`

An empty boat is pushed steadily away from the player for 10 seconds.

Runs when: an empty boat is floating within 24 blocks.

## `swamp_lily_drift`

A lily pad within 10 steps moves one block toward the player every second, at most 6 times, and stops 2.5 blocks away. After 1 min everything is put back (under hardcore it stays where it is).

Runs when: the player is in a swamp or mangrove swamp, and a lily pad within 10 blocks has a free water block on the side facing the player.

## `water_guardian`

Only this player's client is shown a guardian 2 blocks under the water of a pond, with its beam set on the player and its attack sound playing, for 4 s. Nothing happens on the server.

Runs when: there is source water at least 4 blocks deep with air above it, within 20 blocks of the player and at least 6 blocks away.

## `sea_lights`

Only this player's client is shown a sea lantern that moves along a straight run of water, one block every 0.2 s, with 2 lit at a time, and each one is put back behind it. Nothing happens on the server.

Runs when: the player is in an ocean biome and there is a straight run of at least 8 water blocks, 3 blocks under the surface, starting 6 to 18 blocks away.
