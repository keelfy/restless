---
title: Affliction
---

# Affliction

26 events.

## `blindness`

The player is struck with Blindness for 6 s, and a ghostly moan is heard.

Runs when: the player is not already blind.

## `nausea`

The player is given Nausea for 5 s.

Runs when: the player has no nausea.

## `heaviness`

The player is given Slowness II for 8 s.

Runs when: the player has no slowness.

## `fatigue`

The player is given Mining Fatigue II for 10 s.

Runs when: the player has no mining fatigue.

## `hunger_pang`

The player loses 3 food points.

Runs when: the player's food level is above 3 bars.

## `darkness`

The player is given Darkness for 6 s.

Runs when: the player has no darkness.

## `shudder`

The player is given a small random shove.

Runs when: at any time.

## `poison`

**Harmful.**
The player is poisoned for 5 s, though the poison cannot kill.

Runs when: the player is not poisoned.

## `weakness`

**Harmful.**
The player is given Weakness for 30 s.

Runs when: the player has no weakness.

## `levitation`

**Harmful.**
The player levitates for 2 s and then falls.

Runs when: the player has no levitation.

## `frostbite`

**Harmful.**
The player is frozen as if standing in powder snow for about 3 s.

Runs when: the player's health is above 3 hearts and the player is not freezing.

## `drained`

**Harmful.**
The player loses 5–14 XP points.

Runs when: the player has XP.

## `bad_luck`

**Harmful.**
The player is given Bad Luck for 5 min.

Runs when: the player has no bad luck.

## `sanity_crash`

**Harmful.**
This takes 90 points off the sanity bar (of 100), never below 0, on top of the 4 points every event costs. Creative and spectator players are untouched.

Runs when: the player is not in creative or spectator mode and their sanity is above 0.

## `sanity_drain`

**Harmful.**
This takes a random 10 to 25 points off the sanity bar (of 100), never below 0, on top of the 4 points every event costs. Creative and spectator players are untouched.

Runs when: the player is not in creative or spectator mode and their sanity is above 0.

## `insomnia`

**Harmful.**
The player's "time since rest" is set to 144000 (past three days), so phantoms come the next night. Sleeping resets it.

Runs when: the world is not on peaceful difficulty and fewer than 144000 ticks have passed since the player last rested.

## `dream`

On the next waking (within 1 h) a line appears in the chat and the journal, giving either the story of a nearby spirit ("who drowned in a flooded cave") or one of its traits.

Runs when: a spirit senses the player and no dream is waiting.

## `sleepwalk`

**Harmful.**
On the next waking (within 1 h), provided the player has spent at least 5 s in bed, the player is moved to standing room 4–20 blocks away (solid floor, not magma, no lava next to it). The destination is outdoors if one of 4 tries finds sky.

Runs when: no walk is waiting.

## `ravenous`

The player is given Hunger II for 20 s.

Runs when: the player has no hunger effect.

## `infested`

**Harmful.**
The player is infested for 30 s, and silverfish come out whenever the player is hurt.

Runs when: the player has no infested effect.

## `oozing`

The player is oozing for 30 s, and slimes spill out if the player dies.

Runs when: the player has no oozing effect.

## `weaving`

The player is weaving for 30 s, and cobwebs form if the player dies.

Runs when: the player has no weaving effect.

## `drift`

For 3 s the player gets a small push every tick in one random direction, as by a current.

Runs when: the player is on the ground.

## `false_wound`

The client is told that health is 4 hearts lower (never below 1 heart) for 1.5 s, after which it shows the real value again.

Runs when: the player's health is above 1 heart.

## `false_hearts`

The client shows wither or poison for 5 s, but the server applies no effect.

Runs when: the player has neither wither nor poison.

## `false_hunger`

The client is told that the food bar is empty for 2 s.

Runs when: at any time.
