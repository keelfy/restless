---
title: Blocks
---

# Blocks

115 events.

## `wilted_flowers`

Every such flower turns into a potted dead bush, and after 2 min everything is restored.

Runs when: a potted flower is within 10 blocks.

## `withering_crops`

Every crop within 6 blocks loses one growth stage.

Runs when: a grown crop is within 6 blocks.

## `lost_growth`

One of those plants is set back to age 0.

Runs when: a crop, stem, berry bush or cocoa past its first stage is within 8 blocks.

## `false_windows`

Only this player's client is told that the 64 nearest glass blocks and panes are, all alike, either air, an oak fence (connected to the walls) or the full block that most of their neighbours are. After 10 s it gets the real blocks back, and nothing happens on the server.

Runs when: glass or a glass pane is within 10 blocks.

## `false_looks`

Only this player's client is told that each of the 16 nearest such blocks is another random block from the list, keeping its facing. After 10 s it gets the real blocks back, and nothing happens on the server.

Runs when: a block from the list `blocks` is within 10 blocks (workstations, containers and other blocks that people use: crafting table, furnaces, chests, barrel, stonecutter, fletching, smithing and cartography tables, loom, grindstone, anvil, lectern, brewing stand and so on).

## `ghost_tree`

Only this player's client is told of a small round tree standing there: pale oak, oak or birch, with a trunk 4 to 6 blocks tall and the vanilla blob crown. It is built from `ghost_<wood>_log` and `ghost_<wood>_leaves` blocks, which look and tint like the real ones but have no collision and drop nothing (leaves are placed only on air), so the player walks straight through it. A minute later the client gets the real blocks back, and nothing happens on the server.

Runs when: there is standing room 12 blocks away, within 40° of where the player looks (up to 16 bearings are tried), and open space (air, grass, flowers) for a 6-block trunk.

## `moving_tree`

It takes a log standing on dirt, the logs joined to it and the non-persistent leaves that grew from them, and moves them 2 to 4 blocks in a random direction if every spot is open (air, grass, flowers) and the base stands on dirt, with a rustle. A leaf belongs to the tree if it is no farther from these logs than from its own nearest log, so touching crowns stay in place, and the tree has at most 300 blocks and must have a crown. After 30 s the tree moves back and the grass and flowers it stood in return, unless it was cut down or built on in the meantime; under hardcore it stays in its new place.

Runs when: a log on dirt is within 16 blocks.

## `turning_frame`

The item in the frame rotates.

Runs when: a filled item frame is within 12 blocks.

## `new_sign`

The front text becomes one creepy phrase of up to three short lines, at most eight letters each so that it fits a hanging sign. After 1 min the old text is restored, unless the player has edited it.

Runs when: a sign is within 10 blocks.

## `new_painting`

The painting turns into another one and is restored after 1 min.

Runs when: a painting within 12 blocks has another variant of the same size.

## `glass_wall`

The wall's two blocks in front of the player turn to glass for 4 s, and behind them a silent, motionless husk stands facing the player.

Runs when: the difficulty is not peaceful and a one-block wall 2–6 blocks away has standing room behind it.

## `dry_cauldron`

The cauldron is emptied.

Runs when: a water cauldron is within 10 blocks.

## `dead_ground`

A ragged patch of grass (radius 3, dense in the middle and sparse at the edge) turns into dirt or coarse dirt.

Runs when: the player is standing on grass.

## `shifting_floor`

**Harmful.**
A ragged patch of the floor (radius 2, a handful of blocks, always including the one underfoot) turns into another kind of the same material for 1.5 s.

Runs when: the player is on the ground and the block under them has a variant (see below).

## `broken_bed`

**Harmful.**
The bed breaks and drops as an item.

Runs when: a bed is within 16 blocks.

## `wall_hole`

**Harmful.**
Two blocks of the wall vanish for 30 s and drop nothing.

Runs when: a one-block wall 2–6 blocks away has standing room behind it.

## `cross`

A plus of bone blocks appears for 1 min.

Runs when: there is room for a plus of five free blocks on solid ground 3–6 blocks away.

## `infested_stone`

**Harmful.**
The block is shown as its infested kind (a swap, see the `swaps` setting) and is restored after 30 s.

Runs when: stone, cobblestone, deepslate or stone bricks are in sight within 6 blocks.

## `ceiling_crack`

Crack progress and particles appear on the block as if it were being mined from above, without a sound, and the block never breaks.

Runs when: a solid block is 1–4 blocks above the eyes.

## `ice_over`

Up to 24 of those blocks turn into ice (a swap) and are restored after 30 s.

Runs when: source water is within 8 blocks.

## `portal_out`

**Harmful.**
The whole portal (every connected block) is broken for good in every mode, with a fizzle, and the frame stays.

Runs when: nether portal blocks are within 12 blocks.

## `block_blink`

The block breaks with its own sound and dust (a swap to air) and comes back after 2 s. Under hardcore it stays gone and drops nothing.

Runs when: the block the player looks at is within 5 blocks, has no block entity and is not below the feet.

## `leak`

Water drips from the spot for 8 s, with particles and sound only, in every mode.

Runs when: there is air just under a ceiling 1–4 blocks above the eyes.

## `lava_leak`

Lava drips from the spot for 6 s, with particles and sound only, in every mode.

Runs when: the conditions are the same as for `leak`.

## `mock_grave`

A gravestone with a sign (R.I.P., the player's name and tomorrow's date) appears and is removed after 5 min; under hardcore it stays.

Runs when: there is open standing room 5–10 blocks away with a free cell in front of it.

## `cave_sign`

A wall sign appears with a short warning (go back, turn around, too late) and is removed after 5 min; under hardcore it stays.

Runs when: the player is underground (no sky above) and there is a wall 4–8 blocks away in one of the four directions.

## `item_trail`

Bones, rotten flesh and string lie on the ground every 3 blocks from the player to a standing sign with a short note, and none of these items can be picked up. After 5 min the items are cleared and the sign is removed if it is unedited; under hardcore the items can be taken and the sign stays.

Runs when: there is standing room 10–16 blocks away.

## `repaint`

Up to 40 blocks of that one kind turn into the neighbouring colour (dyes form a ring), planks become the stripped log of their wood, stone bricks become stone, cobblestone and stone bricks become their mossy or cracked kind and back again, and one kind of deepslate becomes another. After 60 seconds everything is swapped back.

Runs when: a dyed block (wool, concrete, terracotta or stained glass, including wool and concrete stairs and slabs), planks, cobblestone, stone bricks, or deepslate bricks, deepslate tiles or cobbled deepslate (with their stairs, slabs and walls) are within 10 blocks.

## `lava_cauldron`

The cauldron turns into a lava cauldron with a sizzle and changes back after half a minute.

Runs when: a water cauldron is within 10 blocks.

## `cauldron_fills`

The cauldron fills to a random level with drip particles and runs dry again after a minute.

Runs when: the player is indoors and a dry cauldron is within 10 blocks.

## `turning_terracotta`

Every piece of glazed terracotta turns by a quarter and stands back after a minute.

Runs when: glazed terracotta is within 10 blocks.

## `end_rod_gaze`

One rod turns to point at the player and turns back after a minute.

Runs when: an end rod is within 12 blocks and does not already face the player.

## `bricked_door`

Only this player's client is told that the door's two blocks are the full block that most of the blocks around them are (bricks if there is none). After 10 s it gets the real blocks back, and nothing happens on the server.

Runs when: a wooden door is within 10 blocks.

## `boarded_windows`

Only this player's client is told that the 48 nearest glass blocks and panes are oak planks. After 15 s it gets the real blocks back, and nothing happens on the server.

Runs when: the sky is visible at the player's feet and glass or a glass pane is within 12 blocks.

## `stair_wall`

Only this player's client is told that the two blocks above that step are the floor block ahead of it, so the stairs lead into a wall. After 15 s it gets the real blocks back, and nothing happens on the server.

Runs when: the last step of a flight of stairs is within 10 blocks, meaning a bottom stair with another one below it behind, solid floor ahead and open space above.

## `yard_pit`

Only this player's client is told that a 3×3 patch of ground two blocks deep there is air, along with the plants over it. After 15 s it gets the real blocks back, and nothing happens on the server.

Runs when: there is standing room under open sky 8 to 14 blocks away.

## `unlit_torches`

Only this player's client is told that the 24 nearest torches are the burnt-out `unlit_` ones. After 30 s it gets the real blocks back, and nothing happens on the server.

Runs when: a torch is within 12 blocks.

## `torch_line`

Only this player's client is told that up to 8 torches stand in a line along where they look, the first 10 blocks away and the rest one every 3, following the ground up and down. Each is gone for them once they come within 4 blocks (checked every 0.25 s), and the remaining ones after 60 s. Nothing happens on the server, except in `real` mode, where the torches are really placed and put back after 60 s, and under hardcore they stay.

Runs when: the player is outdoors at night under open sky, or under leaves with a log within 8 blocks, and there is room for a torch at 3 or more spots in a row.

## `fake_ore`

Only this player's client is told that one such block is a diamond ore (deepslate diamond ore in deepslate). Once the player starts to break it, the stone shows instead. After 1 min the client gets the real block back, and nothing happens on the server.

Runs when: there is no sky overhead and stone or deepslate touching air is within 8 blocks.

## `ladder_stub`

Only this player's client is told that the upper half of the column is air. After 20 s it gets the real blocks back, and nothing happens on the server.

Runs when: a column of at least 3 ladders is within 10 blocks.

## `second_door`

Only this player's client is told of an oak door in the wall. After 20 s it gets the real blocks back, and nothing happens on the server.

Runs when: there is a wall one block thick and two high in front of the player, with free room behind it.

## `false_harvest`

Only this player's client is told that the 40 nearest such crops are fully grown. After 20 s it gets the real blocks back, and nothing happens on the server.

Runs when: a crop that is not fully grown is within 10 blocks.

## `lava_pond`

Only this player's client is told that the 200 nearest such water blocks are lava. After 1 s it gets the real blocks back, and nothing happens on the server.

Runs when: a water source with open air above it is within 12 blocks.

## `lava_as_water`

A sizzle sounds at the lava, and only this player's client is told that the 300 nearest lava blocks are water. They show as lava again when the player comes within 8 blocks, after 30 s, or at once when the player starts to break one. It is always an illusion and never real, in any `swaps` mode.

Runs when: the player has more than 5 hearts of health, and at least 6 lava blocks lie within 16 blocks with none within 9.

## `roof_snow`

Only this player's client is told of two layers of snow on the 80 nearest roof blocks. After 1 min it gets the real blocks back, and nothing happens on the server.

Runs when: the sky is visible at the player's feet, and a roof of planks, cobblestone, stone bricks, bricks, terracotta or deepslate within 8 blocks lies at least 2 blocks above the player.

## `black_glass`

Only this player's client is told that the 48 nearest glass blocks are tinted glass and the panes are black stained glass panes. After 15 s it gets the real blocks back, and nothing happens on the server.

Runs when: glass or a glass pane is within 10 blocks.

## `red_wool`

Only this player's client is told that the 64 nearest wool blocks and carpets are red. After 15 s it gets the real blocks back, and nothing happens on the server.

Runs when: wool or a carpet that is not red is within 10 blocks.

## `sign_whisper`

Only this player's client gets the sign's block entity with one of the listed texts on its front. After 15 s it gets the real text, and nothing happens on the server.

Runs when: a sign is within 10 blocks.

## `wither_roses`

Only this player's client is told that the 48 nearest small flowers are wither roses. After 15 s it gets the real flowers back, and nothing happens on the server.

Runs when: a small flower that is not a wither rose is within 12 blocks.

## `block_flicker`

Only this player's client is told that up to 200 of the nearest blocks of one kind found there are one random block from the list `to`. One tick later it gets the real blocks back, and nothing happens on the server.

Runs when: a block from the list `from` (stone, dirt, grass, cobblestone, deepslate, sand, oak planks) is within 8 blocks.

## `lone_gravestone`

Only this player's client is told of a gravestone there. After 30 s it gets the real block back, and nothing happens on the server.

Runs when: there is standing room 9 blocks ahead (within 40 degrees) under the open sky.

## `broken_portal`

Only this player's client is told that 3 random obsidian blocks touching the portal are air. After 15 s it gets the real blocks back, and nothing happens on the server.

Runs when: a nether portal block is within 16 blocks.

## `dark_circle`

Only this player's client is told of light blocks (level 12) in a ring of radius 3 and in its middle. They go out when the player comes within 3 blocks of the middle or after 30 s, and nothing happens on the server.

Runs when: the player is underground (no sky) with block light 5 or less, and there is standing room 8 blocks ahead with at least 6 of the 12 ring points free.

## `fence_gaps`

Only this player's client is told that 3 random fence posts there are air. After 20 s it gets the real posts back, and nothing happens on the server.

Runs when: at least 4 fence posts are within 12 blocks.

## `corner_cobwebs`

Only this player's client is told that up to 12 random such corners hold a cobweb. After 20 s it gets the real blocks back, and nothing happens on the server.

Runs when: at least 3 empty corners where the ceiling meets two walls are within 8 blocks.

## `redstone_trail`

Only this player's client is told of redstone dust, following the ground up and down. After 20 s it gets the real blocks back, and nothing happens on the server.

Runs when: there is room on the floor for at least 5 blocks of a straight line in one of the four directions, 3 to 15 blocks away.

## `composter_swing`

An empty composter looks full, or a full one looks emptied, for about a minute.

Runs when: a composter is within 12 blocks.

## `banner_skull`

A black skull appears on the banner and is taken off again after a couple of minutes.

Runs when: a banner within 12 blocks has room for another pattern.

## `stairs_flip`

A few stairs behind the player's back turn upside down and then right side up again after a minute.

Runs when: stairs are within 12 blocks behind where the player is looking.

## `lectern_swap`

The book on the lectern is replaced by another one, signed by the spirit, and the first one comes back later.

Runs when: a lectern with a book is within 12 blocks.

## `skulls_turn`

Every such skull is shown turned to face the player (a swap, see the `swaps` setting) and is restored after 30 s.

Runs when: any floor or wall skull is within 12 blocks, in any dimension.

## `anchor_charge`

One of the anchors shows one more charge, with the charging sound (a swap, see the `swaps` setting), and is restored after 30 s; under hardcore it stays.

Runs when: the player is in the Nether and a respawn anchor below full charge is within 10 blocks.

## `vines_reach`

The vine nearest the player grows up to 3 blocks down into the air, ending in a full-grown head, and is put back after 30 s; under hardcore it stays.

Runs when: the player is in the Nether and a weeping vine end with air below it is within 12 blocks.

## `void_wind`

A wind-burst howl sounds, and 2 s later the player, if on the ground, is nudged outward at about 2.4 blocks/s every 0.1 s for 5 s, with the howl repeated each second. The push ends the moment ground is missing within 10 blocks along the way, so the player never reaches the edge.

Runs when: the player is in the End, has more than 5 hearts of health, stands on the ground and is not riding, is 24 or more blocks from the middle, and has ground (within 4 blocks below) at every second block out to 10 blocks along the way outward.

## `gateway_beam`

The gateway's own beam event is sent, as when an entity enters it, so it shows its beam for 2 s. Nobody is moved.

Runs when: the player is in the End and a gateway lies in the 9×9 chunks around them.

## `chorus_shift`

Only this player's client is told that the 24 nearest such blocks are one block over in a single direction (air where they were). After 10 s it gets the real blocks back, and nothing happens on the server.

Runs when: the player is in the End and a chorus plant or flower within 12 blocks has free air beside it.

## `phantom_egg`

Only this player's client is told of a dragon egg standing there. Once the player starts to break it, the truth shows. After 10 s it gets the real block back, and nothing happens on the server.

Runs when: the player is in the End and there is standing room 4–12 blocks away.

## `ender_chests_open`

Five ender chest open sounds play 3–8 blocks away at random bearings, 0.4 s apart, each followed by the close sound 1.5 s later. None of it exists in the world.

Runs when: the player is in the End.

## `nowhere_portal`

Only this player's client is told of a lit nether portal in a frame of obsidian (without corners) there, facing them, with the portal ambient sound. Once the player starts to break it, air shows instead. After 15 s it gets the real blocks back, and nothing happens on the server.

Runs when: the player is in the Overworld and a 4×5 empty space on solid ground lies 14 blocks away, within 40° of where the player looks.

## `desert_sand_trickle`

Falling sand particles trickle from under the first solid block above the player's head, in 12 pulses 0.25 s apart, with a sand-breaking sound. Nothing in the world changes.

Runs when: the player is in a desert and a solid block is within 6 blocks above the player's head.

## `cauldron`

Brewing bubble sounds and bubble particles come from a cauldron within 12 blocks, in 8 pulses 0.5 s apart.

Runs when: a cauldron is within 12 blocks, in any biome.

## `fireflies_dark`

Only this player's client is told that every firefly bush within 12 blocks is a plain bush. After 30 s it gets the real blocks back, and nothing happens on the server.

Runs when: a firefly bush is within 12 blocks, in any biome.

## `snow_door_drift`

Four layers of real snow settle on the outside of a wooden door within 12 blocks, and three layers on each side along the wall. After 2 min the snow is removed, and under hardcore it stays.

Runs when: the player is in a snowy biome, and a wooden door within 12 blocks has open sky and free ground outside it.

## `window_vines`

Real vines grow on up to 24 of the nearest air blocks beside the glass, hanging toward it. After 60 s each one goes away unless it was changed in the meantime, and under hardcore they stay.

Runs when: the player is in a jungle, swamp or mangrove swamp biome, and glass or a glass pane within 12 blocks has air beside it.

## `eyeblossoms_open`

Only this player's client is told that up to 12 of the nearest such flowers are open, and an opening sound plays. After 30 s it gets the real blocks back, and nothing happens on the server.

Runs when: a closed eyeblossom is within 12 blocks, regardless of biome or view.

## `resin_clumps`

Real resin clumps grow on up to 8 air blocks beside the walls, clustered around a random spot, with a placing sound. After 2 min each one goes away unless it was changed in the meantime, and under hardcore they stay.

Runs when: the player is in a forest, flower forest, birch forest, old growth birch forest, dark forest or pale garden, and a plank, cobblestone, stone brick, brick or wool wall within 12 blocks has air beside it.

## `canopy_closes`

Only this player's client is told that the 40 nearest such blocks are persistent dark oak leaves. After 15 s it gets the real blocks back, and nothing happens on the server.

Runs when: the player is in the dark forest or pale garden, and at least 6 air blocks touching leaves at least 3 above the player lie within 8 blocks.

## `sculk_creep`

Real sculk spreads in a trail of up to 12 blocks from far ground toward the player, one block every second, each next to the last and nearer, and stops 3 blocks away. After 2 min each one goes away unless it was changed in the meantime, and under hardcore it stays.

Runs when: the player is in the deep dark and there is sculk-replaceable ground with air above it, 8 to 14 blocks away.

## `berries_dark`

Up to 10 of the nearest bunches lose their berries, one per second, and 1 min later each has them again. Under hardcore they stay dark.

Runs when: glow berries are within 14 blocks, in any biome.

## `scarecrow_closer`

A scarecrow stands a step closer than before and goes back after a minute.

Runs when: a scarecrow is within 14 blocks and at least 4 away, with room to step toward the player.

## `ouija_hi`

The planchette spells "HI" on its own.

Runs when: an ouija board is within 8 blocks.

## `spirit_box_word`

The spirit box says a word nobody asked for.

Runs when: a spirit box is within 6 blocks.

## `salt_gap`

One cell of a salt line, the one nearest the spirit, disappears and returns after two minutes.

Runs when: at least 4 salt cells are within 8 blocks.

## `grave_name`

A gravestone reads the player's own name.

Runs when: a gravestone is within 16 blocks.

## `journal_page_torn`

One journal entry is gone for a few minutes and then returns to its place.

Runs when: the player carries a journal with at least 3 entries.

## `dreamcatcher_fails`

A dreamcatcher swings hard, but the dream comes anyway.

Runs when: a dreamcatcher is within 8 blocks and a dream can start.

## `lantern_soul_fire`

A hallowed lantern burns with soul fire for half a minute and does not ward during that time.

Runs when: a hallowed lantern is within 10 blocks and a spirit is about.

## `charm_falls`

A door charm drops off its hook as an item.

Runs when: a door charm is within 10 blocks.

## `altar_midnight`

At night the candles around an altar light themselves one after another.

Runs when: it is night in the Overworld and an altar within 12 blocks has unlit candles beside it.

## `beacon_out`

**Harmful.**
An obsidian or black stained glass block is shown right above the beacon, so the beam goes out or turns black. This lasts 30 s and affects this player only; under `real` swaps the block is real, and under hardcore it stays.

Runs when: an active beacon with air above it is within 16 blocks.

## `campfire_cooks`

Rotten flesh or a rabbit foot shows on the grill for 1 min, to this player only. Nothing cooks, and the real campfire is unchanged.

Runs when: a campfire with a free slot is within 10 blocks.

## `spawner_shows`

For 30 s the spinning mob inside is shown as a villager, iron golem, cat, wandering trader, chicken or witch, to this player only. The real spawner is unchanged.

Runs when: a monster spawner is within 16 blocks.

## `trial_ominous`

The spawner is shown as ominous for 10 s, to this player only.

Runs when: a calm trial spawner is within 16 blocks.

## `vault_head`

The vault's display item is the player's own head for 30 s, to this player only.

Runs when: a vault is within 16 blocks.

## `black_bed`

Both halves of the bed show as the black bed, with the same facing and part, for 1 min, to this player only; a bed is never really swapped.

Runs when: a bed that is not black is within 12 blocks.

## `black_carpets`

The 48 nearest carpets show as black carpets for 1 min. In `real` mode they are really changed, and under hardcore they stay.

Runs when: the player is indoors and a carpet that is not black is within 8 blocks.

## `netherrack_behind`

Only this player's client is told that the 40 nearest such blocks are netherrack. It gets the real blocks back when the player looks within 70° of any of them, or after 30 s. Nothing happens on the server, except that with the `swaps` setting `REAL` the blocks are really changed for 30 s.

Runs when: there is a solid block with a free face 2 to 8 blocks away, more than 100° from where the player looks.

## `roof_gone`

Only this player's client is told that every block from 2 above the player to the sky, 2 blocks to either side, is air. After 20 s it gets the real blocks back, and nothing happens on the server.

Runs when: the player is indoors with at most 6 blocks of roof over their head, and there are at least 4 blocks to hide.

## `path_rearranged`

Only this player's client is told that those floor blocks are shuffled among each other. It gets the real blocks back when the player looks within 70° of any of them, or after 30 s, and nothing happens on the server.

Runs when: there are at least 8 floor blocks of at least two kinds, 3 to 10 blocks behind the player (1 to either side).

## `landmark_tree_gone`

Only this player's client is told that the tallest such tree (logs and leaves) is air. It gets the real blocks back when the player comes within 12 blocks of it, or after 60 s, and nothing happens on the server.

Runs when: the player's bed is in this dimension, and a tree of at most 250 blocks and at least 8 leaves stands within 24 blocks of it and 18 to 64 blocks from the player.

## `tunnel_filled`

Only this player's client is told that both blocks of each such cell are stone. It gets the real blocks back when the player looks within 70° of any of them, or after 30 s. Nothing happens on the server, and the effect is not limited to tunnels the player dug.

Runs when: there is no sky, and at least 6 walkway cells lie 2 to 10 blocks behind the player (1 to either side), each with a solid block 2 above and no sky.

## `walled_in`

**Harmful.**
Four knocks, 0.25 s apart, come from random sides at head height, with the hit sound of the surrounding block. 1 s after the first knock the player is pulled to the middle of their block, and every free block around their feet and head and over their head (up to 9) turns into the commonest plain block within 2 blocks (stone if none; never ores, beacon blocks, ancient debris or falling blocks). The player also gets Mining Fatigue III for 20 s, and the walls are put back after 20 s (under hardcore they stay). Nothing happens if the player is dead or in water at the 1 s mark.

Runs when: the player is on the ground, not in water and not riding, and at least 5 of those blocks are empty with nobody in them.

## `sealed_tunnel`

**Harmful.**
The cross-section of the tunnel 2 to 5 blocks straight behind the player (up to 6 air blocks) is filled in, wall to wall, with the commonest plain block of its walls (stone if none). It is put back after 60 s, and under hardcore it stays.

Runs when: the player is on the ground, not in water and under a roof, and behind them (opposite their facing) lies an open tunnel leading to a cross-section 2 to 5 blocks away that full blocks shut in and that has nobody in it.

## `ceiling_furniture`

Only this player's client is told that up to 6 such blocks hold an upside-down stair or slab, or a bed (two blocks), at random facings. After 30 s it gets the real blocks back, and nothing happens on the server.

Runs when: the player is indoors and there is an air block under the ceiling within 6 blocks around them, at least 1.5 blocks from them and above their feet.

## `crafter_crafts`

The crafter shows its crafting state (6 ticks) with the craft sound and particles, and ejects a bone, skeleton skull or paper named after the player from its face, and the item vanishes after 1 min. Its real contents are not touched.

Runs when: a crafter is within 12 blocks.

## `scaffold_collapses`

**Harmful.**
Three creaks sound 14 ticks apart, and then every scaffolding block of the column breaks and drops, but only if the player still stands on it.

Runs when: the player stands on a scaffolding column on solid ground, with a fall of under 4 blocks.

## `wall_map_shows`

The framed map is replaced by a new map of the area around the spirit's anchor. The original is put back after 2 min, and under hardcore it stays.

Runs when: a framed filled map is within 12 blocks, and a territorial spirit whose reach holds the player has the 128-block area around its anchor loaded.

## `haunted_painting`

A painting becomes one of the mod's own (a portrait of no one, the house at night, a pair of eyes) and is put back after a minute; under hardcore it stays.

Runs when: a painting within 12 blocks has a mod variant of the same size.

## `painting_darkens`

A large painting darkens in four steps, 20 s apart, and a figure comes up the path to the house. The painting is put back after 3 min, and under hardcore it stays.

Runs when: a 2 by 2 painting is within 12 blocks.

## `sigil_banner`

A black spirit sigil appears on the banner and is taken off after 2 min; under hardcore it stays.

Runs when: a banner within 12 blocks has room for another pattern.

## `abandoned_camp`

A bed, a campfire and a chest appear, the chest holding a signed book in the spirit's voice (a page from a pool, never repeated for the same spirit). Every block is removed after 10 min unless it was changed, though a chest holding the player's own items stays, and under hardcore everything stays.

Runs when: the player is in the Overworld, there is open standing room for a 4×3 camp 6–14 blocks away, and the spirit still has a book page it has not said.

## `dirt_hut`

A dirt hut with a two-high doorway and a roof appears. It is removed block by block after 7.5 min, and under hardcore it stays.

Runs when: the player is in the Overworld and there is open ground for a 3×3 hut 5–12 blocks away.

## `rocking_chair`

A rocking chair appears and leans forward and back 24 times with a creak, then vanishes 5 s later.

Runs when: there is a spot of free floor under a roof within 10 blocks, at least 3 blocks away and outside 55° of where the player looks.

## `rotten_planks`

One plank turns into rotten planks, and then the rot spreads to touching planks, 2 per second, up to 24 blocks. After 2 min the planks are put back, and under hardcore they stay.

Runs when: the player is indoors and planks are within 10 blocks.

## `cracked_mirror`

A cracked mirror appears on the wall. When the player looks at it from within 6 blocks, a dark figure shows in it for 2 s, and the mirror is gone 30 s after that.

Runs when: the player is indoors and there is a wall at head height within 8 blocks, at least 2 away and outside 60° of where the player looks.
