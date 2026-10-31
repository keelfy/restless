---
title: Affliction
---

# Affliction

23 events.

## `blindness`

Blindness for 6 s and a low cave ambient sound

Runs when: Player is not already blind

## `nausea`

Nausea 5 s

Runs when: No nausea

## `heaviness`

Slowness II 8 s

Runs when: No slowness

## `fatigue`

Mining fatigue II 10 s

Runs when: No mining fatigue

## `hunger_pang`

−3 food points

Runs when: Food > 3 bars

## `darkness`

Darkness 6 s

Runs when: No darkness

## `shudder`

Small random shove

Runs when: Always

## `poison`

**Harmful.**
Poison 5 s (cannot kill)

Runs when: Not poisoned

## `weakness`

**Harmful.**
Weakness 30 s

Runs when: No weakness

## `levitation`

**Harmful.**
Levitation 2 s, then the fall

Runs when: No levitation

## `frostbite`

**Harmful.**
Powder-snow freeze for about 3 s

Runs when: Health > 3 hearts, not freezing

## `drained`

**Harmful.**
Loses 5–14 XP points

Runs when: Player has XP

## `bad_luck`

**Harmful.**
Bad luck 5 min

Runs when: No bad luck

## `insomnia`

**Harmful.**
The player's "time since rest" is set to 144000 (past three days), so phantoms come the next night; sleeping resets it

Runs when: Not peaceful, fewer than 144000 ticks since rest

## `dream`

On the next waking (within 1 h) a line in chat and the journal: a nearby spirit's story ("who drowned in a flooded cave") or one of its traits

Runs when: A spirit senses the player, and no dream is waiting

## `sleepwalk`

**Harmful.**
On the next waking after at least 5 s in bed (within 1 h) the player is moved to standing room 4–20 blocks away (solid floor, not magma, no lava next to it), outdoors if one of 4 tries finds sky

Runs when: No walk is waiting

## `ravenous`

Hunger II for 20 s

Runs when: No hunger effect

## `infested`

**Harmful.**
Infested for 30 s: silverfish come out when the player is hurt

Runs when: No infested effect

## `oozing`

Oozing for 30 s: slimes spill out if the player dies

Runs when: No oozing effect

## `weaving`

Weaving for 30 s: cobwebs form if the player dies

Runs when: No weaving effect

## `drift`

For 3 s a small push each tick in one random direction, as by a current

Runs when: On the ground

## `false_wound`

The client is told health is 4 hearts lower (never below 1 heart) for 1.5 s, then the real value

Runs when: Health above 1 heart

## `false_hearts`

The client shows wither or poison for 5 s; the server has no effect

Runs when: No wither or poison

## `false_hunger`

The client is told the food bar is empty for 2 s

Runs when: Always
