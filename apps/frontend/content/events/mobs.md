---
title: Mobs
---

# Mobs

84 events.

## `double`

**Harmful.**
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

## `pen_corner`

They walk to a random inside corner and, within 2.5 blocks of it, stand facing into it; let go after 15 s

Runs when: Two farm animals (not tamable, not leashed) within 16 in a pen fenced on four sides, up to 16 across each way

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

**Harmful.**
Opens the gate

Runs when: A closed gate with an animal within 5 of it

## `wild_pet`

**Harmful.**
Loses its owner and targets the player

Runs when: A tamed pet of the player within 32

## `copycat`

**Harmful.**
A zombie in a copy of the player's armor, targeting them

Runs when: Not peaceful, standing room 3 blocks behind

## `golem_flower`

It holds out its poppy for 15 s, to nobody

Runs when: An iron golem within 24 with no target

## `dog_growls`

It sits, looks at the empty space 2 blocks from the player and growls for 6 s, then stands

Runs when: A tamed standing wolf within 16

## `angry_bees`

**Harmful.**
The bees come out angry at the player, as if it had been broken

Runs when: A bee nest or hive within 16 with bees in it

## `leash_snaps`

**Harmful.**
The leash comes loose with a snap and drops as an item

Runs when: A mob tied to a fence post within 16

## `goat_rams`

**Harmful.**
The goat's ram memory is set to the player's position, so it charges and knocks them back

Runs when: An adult goat 4 to 14 blocks away

## `minecart_rolls`

A steady push toward the player for 5 seconds; the rail steers it

Runs when: An empty minecart on a rail within 24

## `enderman_takes`

**Harmful.**
The block turns to air and the enderman holds it; after 60 seconds the block is back and the enderman's hands are empty

Runs when: An enderman with empty hands and a placed block (planks, wool, stone bricks, bricks, bookshelf) with an open face, both within 16

## `parrot_echo`

A sound (a whisper, or the player's own hurt sound) is bound to a nearby parrot, so it seems to repeat it; only this player hears it

Runs when: A parrot within 16

## `allay_bone`

An allay with an empty hand gets a bone put in it, flies to the player and drops it within 3 blocks; the bone is taken back after 30 s if it never arrives

Runs when: An allay with an empty hand within 24

## `fox_snatch`

**Harmful.**
The fox runs at the player and, within 1.8 blocks, takes the held stack into its mouth; 30 s later it drops the stack

Runs when: Dark outside, an item in the player's hand, an adult awake fox with an empty mouth within 16

## `armadillo_curls`

The armadillo nearest the spirit's point rolls up and stays rolled for 20 s, a clue to where the spirit is

Runs when: An adult armadillo that is not already rolled up within 24

## `sheep_blacken`

Up to 8 sheep turn black, one every 2 s; 30 s later each gets its colour back. Under hardcore they stay black

Runs when: Two sheep that are not black within 16

## `horse_bolts`

**Harmful.**
The rider is thrown off, the horse rears and then runs toward the spirit's point for 10 s

Runs when: The player is riding a horse, donkey or mule

## `axolotl_plays_dead`

The axolotl lies still, playing dead, for 10 s with no danger near

Runs when: An axolotl in water within 16

## `cat_gift`

The cat meows and leaves a bone at its feet

Runs when: Bright outside, a cat within 16

## `chickens_look_up`

They stop and look straight up for 5 s

Runs when: Two chickens within 16

## `cows_face_anchor`

They stop and face the spirit's point for 10 s, a clue to where the spirit is

Runs when: Dark outside, two cows within 16

## `villager_nitwit`

The villager becomes a nitwit; 1 min later its profession and trades are put back. Under hardcore its trades are lost

Runs when: An adult villager with a profession within 16

## `villager_prices`

Every villager nearby gets a minor negative gossip about the player, which raises its prices until it fades on its own

Runs when: A villager within 16

## `bees_stay_out`

**Harmful.**
The bees come out of the hive (not angry) and cannot go back into a hive for 1 min

Runs when: A beehive with bees in it within 16

## `fish_face`

They stop and face the player for 5 s

Runs when: Two fish within 16

## `snowman_melts`

**Harmful.**
Water drips off the snow golem and it takes 1 point of fire damage every second until it is gone, for up to 10 s

Runs when: A snow golem within 16

## `frogs_croak`

Up to 8 frogs croak 0.3 s apart, then are silenced for 10 s

Runs when: Two quiet frogs within 16

## `ink_cloud`

Squid ink particles rise from a random block of deep water, 4 puffs 0.4 s apart

Runs when: Water with water above and below within 14

## `rabbits_stream`

The rabbits run 10 blocks away from the player, re-aimed every 0.5 s for 4 s

Runs when: A rabbit within 16

## `witch_giggles`

**Harmful.**
The witch's laugh from where it stands; 1.5 s later witch particles and a splash sound at the player and Darkness for 6 s

Runs when: A witch 6 to 24 blocks away and out of sight

## `hostiles_retreat`

They drop their target and run 10 blocks away from the player, re-aimed every 0.5 s for 4 s

Runs when: A hostile mob (with AI) within 16

## `creeper_calms`

The nearest creeper loses its target, deflates and walks 10 blocks away for 4 s

Runs when: A creeper (with AI) within 8

## `endermen_stare`

They face the player and are kept from getting angry for 10 s; one the player looks at steps 6 to 12 blocks away with a teleport sound

Runs when: An enderman within 24 with no target

## `llama_spits`

It spits once in a direction at least 90° off the player

Runs when: A llama within 16

## `goat_screams`

A screaming goat sound 40 blocks away, loud enough to carry

Runs when: Overworld, not bright outside, player at Y 100 or above

## `bats_hang`

Up to 4 bats spawn resting under a ceiling; removed after 5 min

Runs when: A free block under a solid one, light 4 or less, no sky, 3 to 10 blocks away

## `sniffer_digs_up`

A drop-seed sound and the player's head or a bone appears in front of it; removed after 1 min if not taken

Runs when: An adult sniffer within 20

## `cat_at_anchor`

**Harmful.**
The cat is teleported to standing room at the spirit's point (or 8 to 16 blocks from the player) and left sitting

Runs when: One of the player's untied cats within 32

## `wind_burst`

A gust burst with its sound 2 blocks away; loose items within 5 are shoved away and up

Runs when: No sky above the player

## `mobs_go_still`

**Harmful.**
Every such mob loses AI and sound for 10 s, then gets both back

Runs when: A mob within 16 that has AI, is not silent, rides nothing and carries nobody

## `creeper_behind`

Only this player's client is told of a creeper standing there facing them, with an id no server entity has. Gone after 4 checks (every tick) with it within 35° of where they look, or after 10 s. Nothing on the server

Runs when: Standing room 3 blocks away, 140–180° from where the player looks (up to 16 bearings tried)

## `drowned_rises`

Only this player's client is told of a drowned that rises from 2.2 blocks down to chest height in the pond over 2 s, then sinks again, facing them. Nothing on the server

Runs when: Source water at least 3 deep with air above it, 4 to 16 blocks away

## `ghost_horse`

Only this player's client is told of a riderless horse that crosses from one side to the other in 2.5 s (about 0.8 blocks/tick), following the ground. Nothing on the server

Runs when: Ground 16 blocks away at 60° to either side of where the player looks

## `bed_cat`

Only this player's client is told of a cat sitting on the head of the bed, facing the headboard (the wall). Gone when the player comes within 2.5 blocks, or after 30 s. Nothing on the server

Runs when: A bed within 12 blocks, at least 3 away

## `child_runs`

Only this player's client is told of a baby villager that runs across from one side to the other in 1.5 s, following the ground and ignoring walls. Nothing on the server

Runs when: Under a roof (no sky above), ground 5 blocks away at 70° to either side of where the player looks

## `doorway_enderman`

Only this player's client is told of an enderman standing in the doorway, facing them. When it has been within 30° of where they look for 3 checks (every tick) it is gone with the teleport sound; otherwise after 20 s. Nothing on the server

Runs when: A door within 14 blocks, at least 4 away

## `forest_wolves`

Only this player's client is told of 2–4 wolves standing there, all facing them. Gone when the player comes within 10 blocks, or after 20 s. Nothing on the server

Runs when: Forest biome; standing room for at least 2 of 4 wolves 22–28 blocks away within 45° of where the player looks

## `ceiling_spider`

Only this player's client is told of a spider hanging upside down under the ceiling (given the name Dinnerbone, not shown, which makes the client flip it). Gone when the player comes within 3 blocks, or after 20 s. Nothing on the server

Runs when: Standing room 3–8 blocks away with a solid ceiling within 5 blocks above it

## `bat_swarm`

Only this player's client is told of 6 bats that cross a 30-block line through the player at a random bearing and head height, a little apart, in 2 s, walls or not. Nothing on the server

Runs when: Always

## `window_zombie`

Only this player's client is told of a zombie standing outside it, facing in. Gone when the player comes within 1.5 blocks of it, or after 20 s. Nothing on the server

Runs when: A glass block or pane within 10 blocks, at least 3 away, with the player on one side and standing room on the other

## `ghast_drift`

Only this player's client is told of a ghast drifting a straight 160-block line 45 blocks above them, up to 40 to one side, at a random bearing, in 30 s, with its ambient cry at the middle of the line. Nothing on the server

Runs when: Overworld, sky visible, forest biome

## `vex_through_wall`

Only this player's client is told of a vex flying a straight line 24 blocks long at eye height, centred 12 blocks away, in 3 s, through whatever stands on it, with an id no server entity has. The line must start and end in the open and pass through a wall. Nothing on the server

Runs when: A straight 24-block line at eye height, centred 12 blocks away at a random bearing (up to 16 tried), that starts and ends in the open and passes through 3 to 16 solid samples (half a block each)

## `phantom_swoop`

Only this player's client is told of a phantom 24 blocks away and 14 up, at a random bearing, with its swoop screech; it flies at 1 block/tick straight at the player's eyes, as they move, and is gone within 1.5 blocks or after 5 s. Nothing on the server

Runs when: Sky visible

## `dead_on_path`

Only this player's client is told of a cow, sheep, pig, chicken or villager there and then of its death, so it lies on its side (its death sound plays). Gone when the player comes within 4 blocks, or after 30 s. Nothing on the server

Runs when: Standing room 12 blocks away within 35° of where the player looks (up to 16 bearings tried)

## `llama_caravan`

Only this player's client is told of 4 trader llamas, 2.5 blocks apart, walking a 40-block line at right angles to where the player looks, centred on that spot, at 0.15 blocks/tick and following the ground up 2 and down 4. Each is gone at the end of the line. Nothing on the server

Runs when: Standing room 16 blocks away within 25° of where the player looks (up to 16 bearings tried)

## `giant_slime`

Only this player's client is told of a size 8 slime there, hopping on the spot once a second and facing the player. Gone when the player comes within 14 blocks, or after 30 s. Nothing on the server

Runs when: Standing room 32 blocks away within 40° of where the player looks (up to 16 bearings tried)

## `piglins_stare`

Each piglin loses its AI, faces the player (re-aimed every 0.25 s) for 8 s, then gets its AI back

Runs when: Nether, a piglin or brute within 24

## `piglin_barter`

It plays its admiring sound; 2 s later a player head (the player's own) or a bone is thrown from it toward the player, and is gone after 30 s

Runs when: Nether, an adult piglin within 16

## `ghast_cry`

A ghast's scream 40 blocks away (the spirit's point if one sent it), 2.5 s later its fireball shot, 1.25 s after that an explosion from the same spot

Runs when: Nether

## `lava_figure`

A vanilla mannequin in a stranger's skin (invulnerable, silent) stands on that lava, facing the player; gone when the player is within 8 blocks, or after 30 s

Runs when: Nether, a lava source with two free blocks above it within 24, at least 10 away

## `strider_shiver`

Only this player's client is told the striders are cold (the shiver) for 8 s, then the real state again. Nothing changes on the server

Runs when: Nether, a strider within 24 that is not already cold

## `soul_fire_lean`

Up to 6 soul fires send soul fire flame particles streaming horizontally toward the player, 2 per pulse every 0.2 s, 30 pulses

Runs when: Nether, soul fire within 16

## `still_blaze`

Only this player's client is told of a blaze there, facing the player and not moving. Gone when the player comes within 5 blocks, or after 20 s. Nothing on the server

Runs when: Nether, standing room 10 blocks away within 40° of where the player looks, with a roof 2 or 3 blocks above it

## `magma_hop`

All of them jump at once, with their jump sound

Runs when: Nether, a magma cube on the ground within 16

## `steady_compass`

Only this player's client is shown the compasses locked on that spirit's anchor for 30 s, then the real stacks again

Runs when: Nether, a plain compass in the inventory, and a territorial spirit whose reach holds the player

## `soul_sand_drag`

**Harmful.**
Slowness II for 5 s and a whisper 2 blocks away

Runs when: Nether, standing on soul sand or soil, not slowed

## `piglins_angered`

**Harmful.**
Idle piglins within 16 turn on the player as if they wore no gold (vanilla anger, 30 s)

Runs when: Nether, the player wears gold armor (not creative), a piglin within 16

## `endermen_turn`

Each such enderman faces the player and has its target cleared and its path stopped every tick, for 10 s. They stay calm however long the player looks back. Nothing to revert

Runs when: End, an enderman with no target within 32 blocks

## `shulker_peeks`

One shulker opens its shell fully with the open sound, and shuts it with the close sound 3 s later (put back at shutdown too)

Runs when: End, a shulker with no target within 24 blocks

## `empty_pillar_crystal`

Only this player's client is told of an end crystal on it, with an id no server entity has. Gone when the player comes within 12 blocks, or after 20 s. Nothing on the server

Runs when: End, a pillar top (bedrock on obsidian, free air above, no crystal on it) 8 or more blocks away in the 48-block square

## `enderman_home_block`

Only this player's client is told of an enderman standing there, facing them, holding one of the blocks they carry. Gone with the teleport sound once it has been within 30° of where they look for 3 checks (every tick), or silently within 6 blocks or after 20 s. Nothing on the server

Runs when: End, the player carries a block item (not one with a block entity) and has standing room 18 blocks away within 50° of where they look

## `desert_husk_haze`

Only this player's client is told of a husk standing 28 blocks away within 40° of where they look, facing them, with an id no server entity has. Gone when the player comes within 14 blocks, or after 20 s. Nothing on the server

Runs when: In a desert, with standing room 28 blocks away within 40° of where the player looks

## `boat_grabbed`

For 8 s the boat's motion is set to zero every tick and the client is told, so it will not move; drowned groans sound under it every 1.5 s

Runs when: In an ocean biome, riding a boat that floats in water

## `parrots_scatter`

Every such parrot is thrown up if it is on the ground and sent toward a point 24 blocks away from the player and 10 up, again every 0.5 s for 8 s; a wing beat sounds

Runs when: In a jungle biome, a parrot with no owner within 24

## `mooshroom_brown`

Up to 8 of them turn brown; 30 s later each is red again. Under hardcore they stay brown

Runs when: In the mushroom fields, a red mooshroom within 24
