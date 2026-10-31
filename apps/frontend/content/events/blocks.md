---
title: Blocks
---

# Blocks

28 events.

## `wilted_flowers`

Every one becomes a potted dead bush; restored after 2 min

Runs when: A potted flower within 10

## `withering_crops`

Every crop within 6 loses one growth stage

Runs when: A grown crop within 6

## `lost_growth`

One of them is set back to age 0

Runs when: A crop, stem, berry bush or cocoa past its first stage within 8

## `false_windows`

Only this player's client is told that the 64 nearest glass blocks and panes are, all alike, air, an oak fence (connected to the walls) or the full block most of its neighbours are. 10 s later it gets the real blocks back. Nothing on the server

Runs when: Glass or a glass pane within 10

## `false_looks`

Only this player's client is told that the 16 nearest of them are each another random block of the list, keeping the facing. 10 s later it gets the real blocks back. Nothing on the server

Runs when: A block from `blocks` within 10 (workstations, containers and other blocks one uses: crafting table, furnaces, chests, barrel, stonecutter, fletching, smithing and cartography tables, loom, grindstone, anvil, lectern, brewing stand and so on)

## `ghost_tree`

Only this player's client is told of a small round tree there, pale oak, oak or birch, with a 4 to 6 block trunk and the vanilla blob crown, of `ghost_<wood>_log` and `ghost_<wood>_leaves` (same look and leaf tint, no collision, no drops; leaves only on air), so they walk through it. A minute later it gets the real blocks back. Nothing on the server

Runs when: Standing room 12 blocks away within 40° of where the player looks (up to 16 bearings tried), with open space (air, grass, flowers) for a 6-block trunk

## `moving_tree`

Takes a log standing on dirt, the logs joined to it and the non-persistent leaves that grew from them (a leaf no nearer to these logs than its own distance to a log, so touching crowns stay; up to 300 blocks, with a crown), and moves them 2 to 4 blocks in a random direction if every spot is open (air, grass, flowers) and the foot stands on dirt; a rustle sounds. 30 s later it moves back, and the grass and flowers it stood in come back, unless it was cut or built on meanwhile. Under hardcore it stays

Runs when: A log on dirt within 16 blocks

## `turning_frame`

Rotates its item

Runs when: A filled item frame within 12

## `new_sign`

The front text becomes one creepy phrase of up to three short lines (at most eight letters each, so it fits a hanging sign); restored after 1 min unless the player has edited it

Runs when: A sign within 10

## `new_painting`

Becomes another painting; restored after 1 min

Runs when: A painting within 12 with another variant of the same size

## `glass_wall`

The wall's two blocks in front of the player become glass for 4 s; a silent, motionless husk stands behind them facing the player

Runs when: Not peaceful; a one-block wall 2–6 blocks away with standing room behind it

## `dry_cauldron`

Empties it

Runs when: A water cauldron within 10

## `dead_ground`

A ragged patch of grass (radius 3, dense in the middle, sparse at the edge) becomes dirt or coarse dirt

Runs when: Player on grass

## `shifting_floor`

Harmful.
A ragged patch of the floor (radius 2, a handful of blocks, always the one underfoot) turns into another kind of the same material for 1.5 s

Runs when: On the ground, and the block under the player has a variant (see below)

## `broken_bed`

Harmful.
Breaks it; it drops as an item

Runs when: A bed within 16

## `wall_hole`

Harmful.
Two blocks vanish for 30 s, no drops

Runs when: A one-block wall 2–6 blocks away with standing room behind it

## `cross`

A plus of bone blocks for 1 min

Runs when: Room for a plus of five free blocks on solid ground 3–6 blocks away

## `infested_stone`

Harmful.
The block is shown as its infested kind (a swap, see `swaps.mode`); restored after 30 s

Runs when: Stone, cobblestone, deepslate or stone bricks in sight within 6

## `ceiling_crack`

Crack progress and particles on it, as if mined from above, silent; the block never breaks

Runs when: A solid block 1–4 above the eyes

## `ice_over`

Up to 24 of them become ice (a swap); restored after 30 s

Runs when: Source water within 8

## `portal_out`

Harmful.
The whole portal (every connected block) is broken for good in every mode, with a fizzle; the frame stays

Runs when: Nether portal blocks within 12

## `block_blink`

Breaks with its sound and dust (a swap to air), back after 2 s; under hardcore it stays gone, no drop

Runs when: The block looked at within 5, no block entity, not below the feet

## `leak`

Water drips from it for 8 s, particles and sound only, in every mode

Runs when: Air just under a ceiling 1–4 above the eyes

## `lava_leak`

Lava drips from it for 6 s, particles and sound only, in every mode

Runs when: Same

## `mock_grave`

A gravestone with a sign (R.I.P., the player's name, tomorrow's date); removed after 5 min, stays under hardcore

Runs when: Open standing room 5–10 away with a free cell in front

## `cave_sign`

A wall sign with a short warning (go back, turn around, too late); removed after 5 min, stays under hardcore

Runs when: Underground (no sky above), a wall 4–8 blocks out in one of the four directions

## `item_trail`

Bones, rotten flesh and string every 3 blocks along the ground from the player to a standing sign with a short note; the items cannot be picked up. Items cleared and sign removed (if unedited) after 5 min; under hardcore the items can be taken and the sign stays

Runs when: Standing room 10–16 away

## `repaint`

Up to 40 blocks of that one kind turn into the neighbouring colour (dyes form a ring), planks into the stripped log of their wood, stone bricks into stone, cobblestone and stone bricks into their mossy or cracked kind and back, one deepslate kind into another; swapped back after 60 seconds

Runs when: A dyed block (wool, concrete, terracotta, stained glass; stairs and slabs of wool and concrete too), planks, cobblestone, stone bricks or deepslate bricks, tiles and cobbled deepslate (with their stairs, slabs and walls) within 10
