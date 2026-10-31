---
title: Mobs
---

# Mobs

20 events.

## `double`

Harmful.
A vanilla mannequin with the player's skin, armor and held item (invulnerable, silent) walks up at 0.2 blocks/tick, slower than a sprint, jumping up one-block steps, falling off ledges up to 3 deep and turning up to 80° off to go round what is in its way. Within 1.8 blocks it hits every 0.5 s for up to 3 hearts, never below 3 hearts; it vanishes in smoke after 4 hits, 30 s, 40 blocked strides, or when the player is at 3 hearts

Runs when: Not peaceful, no double already after the player, health > 7 hearts, standing room 12–18 blocks away

## `hallucination`

Only this player's client is told of a mob from a list (iron golem, villager, wandering trader, witch, enderman) standing there facing them, with an id no server entity has, so attacks on it do nothing. Gone when the player comes within 10 blocks, or after 30 s. Nothing on the server

Runs when: Standing room 20 blocks away within 40° of where the player looks (up to 16 bearings tried)

## `disguise`

One of them is hidden from this player's client (only its invisible flag is sent) and an entity of another kind (cow→sheep, sheep→pig, pig→cow, chicken→wolf, horse→cow; `looks`) is shown in its place, following it about for 30 s, when the real one is shown again. Sounds and hitbox stay the real animal's; the head turn and limb poses of the copy are the client's own. Under hardcore the animal is really converted into the other kind

Runs when: A cow, sheep, pig, chicken or horse within 24 blocks

## `animals_stare`

They stop and face the player for 3 s

Runs when: Two animals within 16

## `cat_hiss`

The cat hisses

Runs when: A cat within 16

## `cat_flee`

The cats run 8 blocks away from the spirit's point for 5 s; one hisses

Runs when: A standing cat within 16

## `dog_barks`

Turns away and makes its ambient sound

Runs when: A wolf within 16

## `villager_stares`

One villager stops and faces the player for 5 s

Runs when: A villager within 16

## `villager_refuses`

The villager shakes its head for 2 s with the "no" sound

Runs when: A villager within 16

## `bats`

Five bats appear

Runs when: Free air 4 blocks away

## `open_pen`

Harmful.
Opens the gate

Runs when: A closed gate with an animal within 5 of it

## `wild_pet`

Harmful.
Loses its owner and targets the player

Runs when: A tamed pet of the player within 32

## `copycat`

Harmful.
A zombie in a copy of the player's armor, targeting them

Runs when: Not peaceful, standing room 3 blocks behind

## `golem_flower`

It holds out its poppy for 15 s, to nobody

Runs when: An iron golem within 24 with no target

## `dog_growls`

It sits, looks at the empty space 2 blocks from the player and growls for 6 s, then stands

Runs when: A tamed standing wolf within 16

## `angry_bees`

Harmful.
The bees come out angry at the player, as if it had been broken

Runs when: A bee nest or hive within 16 with bees in it

## `leash_snaps`

Harmful.
The leash comes loose with a snap and drops as an item

Runs when: A mob tied to a fence post within 16

## `goat_rams`

Harmful.
The goat's ram memory is set to the player's position, so it charges and knocks them back

Runs when: An adult goat 4 to 14 blocks away

## `minecart_rolls`

A steady push toward the player for 5 seconds; the rail steers it

Runs when: An empty minecart on a rail within 24

## `enderman_takes`

Harmful.
The block turns to air and the enderman holds it; after 60 seconds the block is back and the enderman's hands are empty

Runs when: An enderman with empty hands and a placed block (planks, wool, stone bricks, bricks, bookshelf) with an open face, both within 16
