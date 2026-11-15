---
title: Mobs
---

# Mobs

103 events.

## `double`

**Harmful.**
A vanilla mannequin with the player's skin, armor and held item (invulnerable and silent) walks up at 0.2 blocks/tick, which is slower than a sprint. It jumps up one-block steps, drops off ledges up to 3 blocks deep and turns up to 80° off its course to go round whatever is in its way. Within 1.8 blocks it hits every 0.5 s for up to 3 hearts, but never takes the player below 3 hearts. It vanishes in smoke after 4 hits, after 30 s, after 40 blocked strides, or when the player is at 3 hearts.

Runs when: the difficulty is not peaceful, no double is already following the player, the player has more than 7 hearts of health, and there is standing room 12–18 blocks away.

## `hallucination`

Only this player's client is told of a mob from a list (iron golem, villager, wandering trader, witch, enderman) that stands there facing the player. It has an id that no server entity has, so attacks on it do nothing. It disappears once the player comes within 10 blocks, or after 30 s, and nothing exists on the server.

Runs when: there is standing room 20 blocks away, within 40° of where the player looks (up to 16 bearings are tried).

## `disguise`

One of the animals is hidden from this player's client (only its invisible flag is sent), and an entity of another kind is shown in its place (cow→sheep, sheep→pig, pig→cow, chicken→wolf, horse→cow; `looks`). The copy follows the real animal about for 30 s, after which the real one is shown again. Sounds and hitbox stay the real animal's, while the head turn and limb poses of the copy are the client's own. Under hardcore the animal is actually converted into the other kind.

Runs when: a cow, sheep, pig, chicken or horse is within 24 blocks.

## `animals_stare`

The animals stop and face the player for 3 s.

Runs when: two animals are within 16 blocks.

## `pen_corner`

The animals walk to a random inside corner and, once within 2.5 blocks of it, stand facing into it. They are let go after 15 s.

Runs when: two farm animals (not tamable and not leashed) are within 16 blocks, in a pen fenced on four sides and up to 16 blocks across each way.

## `cat_hiss`

The cat hisses.

Runs when: a cat is within 16 blocks.

## `cat_flee`

The cats run 8 blocks away from the spirit's point for 5 s, and one of them hisses.

Runs when: a standing cat is within 16 blocks.

## `dog_barks`

The wolf turns away and makes its ambient sound.

Runs when: a wolf is within 16 blocks.

## `villager_stares`

One villager stops and faces the player for 5 s.

Runs when: a villager is within 16 blocks.

## `villager_refuses`

The villager shakes its head for 2 s, with the "no" sound.

Runs when: a villager is within 16 blocks.

## `bats`

Five bats appear.

Runs when: there is free air 4 blocks away.

## `open_pen`

**Harmful.**
The gate opens.

Runs when: a closed gate has an animal within 5 blocks of it.

## `wild_pet`

**Harmful.**
The pet loses its owner and targets the player.

Runs when: a tamed pet of the player is within 32 blocks.

## `copycat`

**Harmful.**
A zombie wearing a copy of the player's armor appears and targets them.

Runs when: the difficulty is not peaceful and there is standing room 3 blocks behind the player.

## `golem_flower`

The iron golem holds out its poppy for 15 s, to nobody.

Runs when: an iron golem with no target is within 24 blocks.

## `golem_rage`

**Harmful.**
The nearest idle iron golem within 24 blocks puts away any flower it holds and turns on the player, with vanilla persistent anger. A golem the player built stays friendly.

Runs when: the player is not in creative or spectator mode, the difficulty is not peaceful, and an iron golem with no target that can attack the player is within 24 blocks.

## `wolf_rage`

**Harmful.**
Every idle wild wolf within 16 blocks turns on the player, with vanilla persistent anger. Tamed wolves stay friendly.

Runs when: the player is not in creative or spectator mode, the difficulty is not peaceful, and an untamed wolf with no target that can attack the player is within 16 blocks.

## `dog_growls`

The wolf sits, looks at the empty space 2 blocks from the player and growls for 6 s, and then stands up again.

Runs when: a tamed, standing wolf is within 16 blocks.

## `angry_bees`

**Harmful.**
The bees come out angry at the player, as if the nest had been broken.

Runs when: a bee nest or hive with bees in it is within 16 blocks.

## `leash_snaps`

**Harmful.**
The leash comes loose with a snap and drops as an item.

Runs when: a mob tied to a fence post is within 16 blocks.

## `goat_rams`

**Harmful.**
The goat's ram memory is set to the player's position, so the goat charges at the player and knocks them back.

Runs when: an adult goat is 4 to 14 blocks away.

## `minecart_rolls`

The minecart is pushed steadily toward the player for 5 seconds, and the rail steers it.

Runs when: an empty minecart on a rail is within 24 blocks.

## `enderman_takes`

**Harmful.**
The block turns to air and the enderman holds it. After 60 seconds the block is back and the enderman's hands are empty again.

Runs when: an enderman with empty hands and a placed block (planks, wool, stone bricks, bricks or a bookshelf) with an open face are both within 16 blocks.

## `parrot_echo`

A sound (a whisper, or the player's own hurt sound) is bound to a nearby parrot, so the parrot seems to repeat it. Only this player hears it.

Runs when: a parrot is within 16 blocks.

## `allay_bone`

A bone is put into the empty hand of an allay, which flies to the player and drops it within 3 blocks. If the bone never arrives, it is taken back after 30 s.

Runs when: an allay with an empty hand is within 24 blocks.

## `fox_snatch`

**Harmful.**
The fox runs at the player and, within 1.8 blocks, takes the held stack into its mouth. It drops the stack 30 s later.

Runs when: it is dark outside, the player holds an item, and an adult, awake fox with an empty mouth is within 16 blocks.

## `armadillo_curls`

The armadillo nearest the spirit's point rolls up and stays rolled for 20 s, which is a clue to where the spirit is.

Runs when: an adult armadillo that is not already rolled up is within 24 blocks.

## `sheep_blacken`

Up to 8 sheep turn black, one every 2 s, and 30 s later each gets its colour back. Under hardcore they stay black.

Runs when: two sheep that are not black are within 16 blocks.

## `horse_bolts`

**Harmful.**
The rider is thrown off, and the horse rears and then runs toward the spirit's point for 10 s.

Runs when: the player is riding a horse, donkey or mule.

## `axolotl_plays_dead`

The axolotl lies still, playing dead, for 10 s although there is no danger nearby.

Runs when: an axolotl in water is within 16 blocks.

## `cat_gift`

The cat meows and leaves a bone at its feet.

Runs when: it is bright outside and a cat is within 16 blocks.

## `chickens_look_up`

The chickens stop and look straight up for 5 s.

Runs when: two chickens are within 16 blocks.

## `cows_face_anchor`

The cows stop and face the spirit's point for 10 s, which is a clue to where the spirit is.

Runs when: it is dark outside and two cows are within 16 blocks.

## `villager_nitwit`

The villager becomes a nitwit, and 1 min later its profession and trades are put back. Under hardcore its trades are lost.

Runs when: an adult villager with a profession is within 16 blocks.

## `villager_prices`

Every villager nearby gets a minor negative gossip about the player, which raises its prices until it fades on its own.

Runs when: a villager is within 16 blocks.

## `bees_stay_out`

**Harmful.**
The bees come out of the hive (not angry) and cannot go back into a hive for 1 min.

Runs when: a beehive with bees in it is within 16 blocks.

## `fish_face`

The fish stop and face the player for 5 s.

Runs when: two fish are within 16 blocks.

## `snowman_melts`

**Harmful.**
Water drips off the snow golem, and it takes 1 point of fire damage every second until it is gone, for up to 10 s.

Runs when: a snow golem is within 16 blocks.

## `frogs_croak`

Up to 8 frogs croak 0.3 s apart and are then silenced for 10 s.

Runs when: two quiet frogs are within 16 blocks.

## `ink_cloud`

Squid ink particles rise from a random block of deep water, in 4 puffs 0.4 s apart.

Runs when: there is water with water above and below it within 14 blocks.

## `rabbits_stream`

The rabbits run 10 blocks away from the player, and their direction is re-aimed every 0.5 s for 4 s.

Runs when: a rabbit is within 16 blocks.

## `witch_giggles`

**Harmful.**
The witch's laugh sounds from where she stands. 1.5 s later witch particles and a splash sound appear at the player, and Darkness is applied for 6 s.

Runs when: a witch is 6 to 24 blocks away and out of sight.

## `hostiles_retreat`

The hostile mobs drop their target and run 10 blocks away from the player, re-aimed every 0.5 s for 4 s.

Runs when: a hostile mob (with AI) is within 16 blocks.

## `creeper_calms`

The nearest creeper loses its target, deflates and walks 10 blocks away for 4 s.

Runs when: a creeper (with AI) is within 8 blocks.

## `endermen_stare`

The endermen face the player and are kept from getting angry for 10 s. One that the player looks at steps 6 to 12 blocks away with a teleport sound.

Runs when: an enderman with no target is within 24 blocks.

## `llama_spits`

The llama spits once, in a direction at least 90° off the player.

Runs when: a llama is within 16 blocks.

## `goat_screams`

A screaming goat sound plays 40 blocks away, loud enough to carry.

Runs when: the player is in the Overworld at Y 100 or above, and it is not bright outside.

## `bats_hang`

Up to 4 bats spawn resting under a ceiling and are removed after 5 min.

Runs when: there is a free block under a solid one, 3 to 10 blocks away, with light 4 or less and no sky above it.

## `sniffer_digs_up`

A drop-seed sound plays, and the player's head or a bone appears in front of the sniffer. If it is not taken, it is removed after 1 min.

Runs when: an adult sniffer is within 20 blocks.

## `cat_at_anchor`

**Harmful.**
The cat is teleported to standing room at the spirit's point (or 8 to 16 blocks from the player) and left sitting.

Runs when: one of the player's untied cats is within 32 blocks.

## `wind_burst`

A gust burst with its sound appears 2 blocks away, and loose items within 5 blocks are shoved away and up.

Runs when: there is no sky above the player.

## `mobs_go_still`

**Harmful.**
Every such mob loses its AI and sound for 10 s, and then gets both back.

Runs when: a mob within 16 blocks has AI, is not silent, rides nothing and carries nobody.

## `creeper_behind`

Only this player's client is told of a creeper that stands there facing the player, with an id that no server entity has. It disappears once 4 checks (one every tick) have found it within 35° of where the player looks, or after 10 s. Nothing exists on the server.

Runs when: there is standing room 3 blocks away, 140–180° from where the player looks (up to 16 bearings are tried).

## `drowned_rises`

Only this player's client is told of a drowned that rises, over 2 s, from 2.2 blocks down to chest height in the pond and then sinks again, facing the player. Nothing exists on the server.

Runs when: there is source water at least 3 blocks deep with air above it, 4 to 16 blocks away.

## `ghost_horse`

Only this player's client is told of a riderless horse that crosses from one side to the other in 2.5 s (about 0.8 blocks/tick), following the ground. Nothing exists on the server.

Runs when: there is ground 16 blocks away, at 60° to either side of where the player looks.

## `bed_cat`

Only this player's client is told of a cat that sits on the head of the bed, facing the headboard (the wall). It disappears once the player comes within 2.5 blocks, or after 30 s. Nothing exists on the server.

Runs when: a bed is within 12 blocks and at least 3 blocks away.

## `child_runs`

Only this player's client is told of a baby villager that runs across from one side to the other in 1.5 s, following the ground and ignoring walls. Nothing exists on the server.

Runs when: the player is under a roof (no sky above) and there is ground 5 blocks away, at 70° to either side of where the player looks.

## `doorway_enderman`

Only this player's client is told of an enderman that stands in the doorway, facing the player. Once it has been within 30° of where the player looks for 3 checks (one every tick), it vanishes with the teleport sound, and otherwise it vanishes after 20 s. Nothing exists on the server.

Runs when: a door is within 14 blocks and at least 4 blocks away.

## `forest_wolves`

Only this player's client is told of 2–4 wolves that stand there, all facing the player. They disappear once the player comes within 10 blocks, or after 20 s. Nothing exists on the server.

Runs when: the player is in a forest biome and there is standing room for at least 2 of the 4 wolves 22–28 blocks away, within 45° of where the player looks.

## `ceiling_spider`

Only this player's client is told of a spider hanging upside down under the ceiling (it is given the name Dinnerbone, not shown, which makes the client flip it). It disappears once the player comes within 3 blocks, or after 20 s. Nothing exists on the server.

Runs when: there is standing room 3–8 blocks away with a solid ceiling within 5 blocks above it.

## `bat_swarm`

Only this player's client is told of 6 bats that cross a 30-block line through the player, at a random bearing and head height and a little apart, in 2 s, walls or not. Nothing exists on the server.

Runs when: at any time.

## `window_zombie`

Only this player's client is told of a zombie that stands outside a window, facing in. It disappears once the player comes within 1.5 blocks of it, or after 20 s. Nothing exists on the server.

Runs when: a glass block or pane is within 10 blocks and at least 3 blocks away, with the player on one side of it and standing room on the other.

## `ghast_drift`

Only this player's client is told of a ghast that drifts along a straight 160-block line, 45 blocks above the player and up to 40 blocks to one side, at a random bearing, in 30 s. It gives its ambient cry at the middle of the line. Nothing exists on the server.

Runs when: the player is in the Overworld, the sky is visible and the biome is a forest.

## `vex_through_wall`

Only this player's client is told of a vex that flies a straight line 24 blocks long at eye height, centred 12 blocks away, in 3 s, through whatever stands on it, with an id that no server entity has. The line must start and end in the open and pass through a wall. Nothing exists on the server.

Runs when: there is a straight 24-block line at eye height, centred 12 blocks away at a random bearing (up to 16 are tried), that starts and ends in the open and passes through 3 to 16 solid samples (half a block each).

## `phantom_swoop`

Only this player's client is told of a phantom 24 blocks away and 14 blocks up, at a random bearing, with its swoop screech. It flies at 1 block/tick straight at the player's eyes, following their movement, and vanishes within 1.5 blocks or after 5 s. Nothing exists on the server.

Runs when: the sky is visible.

## `dead_on_path`

Only this player's client is told of a cow, sheep, pig, chicken or villager standing there and then of its death, so that it lies on its side (its death sound plays). It disappears once the player comes within 4 blocks, or after 30 s. Nothing exists on the server.

Runs when: there is standing room 12 blocks away, within 35° of where the player looks (up to 16 bearings are tried).

## `llama_caravan`

Only this player's client is told of 4 trader llamas, 2.5 blocks apart, that walk a 40-block line at right angles to where the player looks, centred on that spot, at 0.15 blocks/tick, following the ground up 2 blocks and down 4. Each one vanishes at the end of the line. Nothing exists on the server.

Runs when: there is standing room 16 blocks away, within 25° of where the player looks (up to 16 bearings are tried).

## `giant_slime`

Only this player's client is told of a size 8 slime that hops on the spot once a second, facing the player. It disappears once the player comes within 14 blocks, or after 30 s. Nothing exists on the server.

Runs when: there is standing room 32 blocks away, within 40° of where the player looks (up to 16 bearings are tried).

## `piglins_stare`

Each piglin loses its AI, faces the player (re-aimed every 0.25 s) for 8 s, and then gets its AI back.

Runs when: the player is in the Nether and a piglin or piglin brute is within 24 blocks.

## `piglin_barter`

The piglin plays its admiring sound. 2 s later it throws a player head (the player's own) or a bone toward the player, which vanishes after 30 s.

Runs when: the player is in the Nether and an adult piglin is within 16 blocks.

## `ghast_cry`

A ghast's scream sounds 40 blocks away (at the spirit's point if one sent it). 2.5 s later comes the sound of its fireball shot, and 1.25 s after that an explosion from the same spot.

Runs when: the player is in the Nether.

## `lava_figure`

A vanilla mannequin in a stranger's skin (invulnerable and silent) stands on that lava, facing the player. It vanishes once the player is within 8 blocks, or after 30 s.

Runs when: the player is in the Nether and a lava source with two free blocks above it is within 24 blocks and at least 10 blocks away.

## `strider_shiver`

Only this player's client is told that the striders are cold (the shiver) for 8 s, after which it shows the real state again. Nothing changes on the server.

Runs when: the player is in the Nether and a strider that is not already cold is within 24 blocks.

## `soul_fire_lean`

Up to 6 soul fires send soul fire flame particles streaming horizontally toward the player, 2 per pulse every 0.2 s, for 30 pulses.

Runs when: the player is in the Nether and soul fire is within 16 blocks.

## `still_blaze`

Only this player's client is told of a blaze that stands there, facing the player and not moving. It disappears once the player comes within 5 blocks, or after 20 s. Nothing exists on the server.

Runs when: the player is in the Nether and there is standing room 10 blocks away, within 40° of where the player looks.

## `magma_hop`

All of the magma cubes jump at once, with their jump sound.

Runs when: the player is in the Nether and a magma cube on the ground is within 16 blocks.

## `steady_compass`

Only this player's client is shown the compasses locked on that spirit's anchor for 30 s, after which the real stacks appear again.

Runs when: the player is in the Nether with a plain compass in the inventory, and a territorial spirit's reach holds the player.

## `soul_sand_drag`

**Harmful.**
The player gets Slowness II for 5 s, and a whisper sounds 2 blocks away.

Runs when: the player is in the Nether, standing on soul sand or soul soil, and is not slowed.

## `piglins_angered`

**Harmful.**
Idle piglins within 16 blocks turn on the player as if the player wore no gold (vanilla anger, 30 s).

Runs when: the player is in the Nether, wearing gold armor (and not in creative mode), with a piglin within 16 blocks.

## `endermen_turn`

Each such enderman faces the player and has its target cleared and its path stopped every tick, for 10 s. They stay calm however long the player looks back. There is nothing to revert.

Runs when: the player is in the End and an enderman with no target is within 32 blocks.

## `shulker_peeks`

One shulker opens its shell fully with the open sound and shuts it with the close sound 3 s later (it is also put back at shutdown).

Runs when: the player is in the End and a shulker with no target is within 24 blocks.

## `empty_pillar_crystal`

Only this player's client is told of an end crystal on the pillar top, with an id that no server entity has. It disappears once the player comes within 12 blocks, or after 20 s. Nothing exists on the server.

Runs when: the player is in the End, and a pillar top (bedrock on obsidian, with free air above and no crystal on it) is 8 or more blocks away, within the 48-block square.

## `enderman_home_block`

Only this player's client is told of an enderman that stands there, facing the player and holding one of the blocks the player carries. It vanishes with the teleport sound once it has been within 30° of where the player looks for 3 checks (one every tick), or silently once the player is within 6 blocks or after 20 s. Nothing exists on the server.

Runs when: the player is in the End, carries a block item (not one with a block entity) and has standing room 18 blocks away, within 50° of where they look.

## `desert_husk_haze`

Only this player's client is told of a husk that stands 28 blocks away within 40° of where the player looks, facing them, with an id that no server entity has. It disappears once the player comes within 14 blocks, or after 20 s. Nothing exists on the server.

Runs when: the player is in a desert, with standing room 28 blocks away within 40° of where they look.

## `boat_grabbed`

For 8 s the boat's motion is set to zero every tick and the client is told, so the boat cannot move, while drowned groans sound under it every 1.5 s.

Runs when: the player is in an ocean biome, riding a boat that floats in water.

## `parrots_scatter`

Every such parrot is thrown up if it is on the ground and is sent toward a point 24 blocks from the player and 10 up, which is repeated every 0.5 s for 8 s. A wing beat sounds.

Runs when: the player is in a jungle biome and a parrot with no owner is within 24 blocks.

## `mooshroom_brown`

Up to 8 mooshrooms turn brown, and 30 s later each is red again. Under hardcore they stay brown.

Runs when: the player is in the mushroom fields and a red mooshroom is within 24 blocks.

## `roof_skeleton`

Only this player's client is told of a skeleton that stands on a roof with its bow drawn and no arrow, facing the player. It disappears once looked at for a moment, once the player comes within 6 blocks, or after 20 s. Nothing exists on the server.

Runs when: a stair or slab with open sky and two free blocks over it is within 16 blocks, at least 8 blocks away and 2 above the player.

## `far_warden`

Only this player's client is told of a warden 30 to 42 blocks away that rises with its emerge animation and sound, stands for a few seconds, and digs back in with the dig sound. Nothing exists on the server.

Runs when: the player is in the Overworld and there is standing room 30 to 42 blocks away.

## `swamp_witch`

Only this player's client is told of a witch that stands behind a window, facing the player. It disappears once looked at, once the player comes within 2.5 blocks, or after 20 s. Nothing exists on the server.

Runs when: the player is in a swamp or mangrove swamp, outside, with a glass block or pane, or a one-block gap over a plank sill, within 12 blocks and at least 4 blocks away, and with standing room inside.

## `wall_shulker`

Only this player's client is told of a shulker on the wall of a room. It opens its shell fully with the open sound, shuts it with the close sound 3 s later, and is gone. Nothing exists on the server.

Runs when: a free, floored block under cover, against a solid wall block, lies within 8 blocks, at least 3 blocks away and within 2 blocks of the player's height.

## `forest_creaking`

Only this player's client is told of a creaking that walks to the player at 0.175 blocks/tick, following the ground, but only while the player is not looking at it. It disappears within 3 blocks, after 60 s, or when it gets stuck. Nothing exists on the server.

Runs when: the player is in a forest biome and there is standing room 24 to 36 blocks away.

## `cow_herd_blink`

Only this player's client is told of a herd of cows in a field. Once the player has looked at it for a moment, the view blinks once and the herd is gone at the darkest point. Otherwise it goes when the player approaches within 8 blocks, or after 30 s. Nothing exists on the server.

Runs when: there is grass under open sky for at least 3 cows, 20 to 26 blocks ahead.

## `golem_wrong_chest`

One stack moves from the copper chest to the other chest, 1 s apart, with the golem's get and drop animations and chest sounds. The move stays in place.

Runs when: a copper golem is within 12 blocks, with a copper chest that holds an item and another, non-adjacent chest that has a free slot.

## `villagers_at_windows`

Up to 3 villagers wake, walk (for up to 20 s) to the nearest unclaimed window, then lose their AI and face the player for 30 s. After that the AI comes back.

Runs when: it is night in the Overworld, a villager is within 20 blocks, and a window (glass or a pane with open air on both sides) with a roofed floor cell at its foot is within 12 blocks.

## `strange_trader`

A real wandering trader, with no AI and facing the player, appears with offers from `offers` (a bone for the player's head, essence for echo shards, and so on), each good for 1 use. It vanishes once the player has seen it and looks away (unless trading), or after 2 min.

Runs when: it is night in the Overworld and there is standing room 12 blocks away, within 40° of where the player looks.

## `golem_in_corner`

The golem walks there (for up to 30 s), loses its AI and faces the corner for 30 s. After that the AI comes back.

Runs when: an idle iron golem is within 40 blocks, and a roofed floor cell in a corner (two walls) lies within 6 blocks of the player and at least 2 blocks away.

## `dolphins_lead`

Up to 3 dolphins swim 16 blocks at a time toward the spirit's point (60 blocks away at random if no spirit sent it), calling now and then. A dolphin more than 12 blocks from the player stops and faces them. This runs for 30 pulses of 1 s.

Runs when: the player is in water and a dolphin in water is within 32 blocks.

## `spiders_at_window`

Up to 5 spiders walk to the ground at the foot of that window, lose their AI and face it for 30 s. After that the AI comes back.

Runs when: at least 2 idle spiders are within 24 blocks, and a window within 12 blocks has open ground under it outside (up to 3 blocks below).

## `squid_goes_dark`

Each such squid has its dark ticks set to 60 s, so the glow goes off and then returns by itself.

Runs when: a glow squid that is glowing is within 32 blocks.

## `hoglins_charge`

Each hoglin is set on the player for up to 5 s. One that comes within 3.2 blocks loses its AI and faces the player for 4 s, then gets its AI back and is told to avoid the player for 10 s.

Runs when: the player is in the Nether, with at least 2 adult hoglins within 24 blocks and farther than 3.2 blocks from the player.

## `pale_pet`

One of the player's tame wolves or cats turns pale until dawn (it stays pale under hardcore).

Runs when: it is night and the player's tame wolf or cat is within 24 blocks.

## `pale_herd`

Up to four cows, pigs or chickens turn pale for 2 min (they stay pale under hardcore).

Runs when: a cow, pig or chicken is within 24 blocks.
