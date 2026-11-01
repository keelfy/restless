---
title: Blocks
---

# Blocks

112 events.

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

**Harmful.**
A ragged patch of the floor (radius 2, a handful of blocks, always the one underfoot) turns into another kind of the same material for 1.5 s

Runs when: On the ground, and the block under the player has a variant (see below)

## `broken_bed`

**Harmful.**
Breaks it; it drops as an item

Runs when: A bed within 16

## `wall_hole`

**Harmful.**
Two blocks vanish for 30 s, no drops

Runs when: A one-block wall 2–6 blocks away with standing room behind it

## `cross`

A plus of bone blocks for 1 min

Runs when: Room for a plus of five free blocks on solid ground 3–6 blocks away

## `infested_stone`

**Harmful.**
The block is shown as its infested kind (a swap, see `swaps.mode`); restored after 30 s

Runs when: Stone, cobblestone, deepslate or stone bricks in sight within 6

## `ceiling_crack`

Crack progress and particles on it, as if mined from above, silent; the block never breaks

Runs when: A solid block 1–4 above the eyes

## `ice_over`

Up to 24 of them become ice (a swap); restored after 30 s

Runs when: Source water within 8

## `portal_out`

**Harmful.**
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

## `lava_cauldron`

It turns to a lava cauldron with a sizzle, and back after half a minute

Runs when: A water cauldron within 10

## `cauldron_fills`

It fills to a random level with drip particles, and runs dry again after a minute

Runs when: Indoors, and a dry cauldron within 10

## `turning_terracotta`

Every piece turns a quarter and stands back after a minute

Runs when: Glazed terracotta within 10

## `end_rod_gaze`

One rod turns to point at the player and stands back after a minute

Runs when: An end rod within 12 that does not already face the player

## `bricked_door`

Only this player's client is told that the door's two blocks are the full block most of the blocks around it are (bricks if none are). 10 s later it gets the real blocks back. Nothing on the server

Runs when: A wooden door within 10

## `boarded_windows`

Only this player's client is told that the 48 nearest glass blocks and panes are oak planks. 15 s later it gets the real blocks back. Nothing on the server

Runs when: Sky visible at the player's feet, and glass or a glass pane within 12

## `stair_wall`

Only this player's client is told that the two blocks above that step are the floor block ahead of it, so the stairs lead into a wall. 15 s later it gets the real blocks back. Nothing on the server

Runs when: The last step of a flight of stairs within 10: a bottom stair with another below it behind, solid floor ahead and open space above

## `yard_pit`

Only this player's client is told that a 3×3 patch of ground two blocks deep there is air (and the plants over it). 15 s later it gets the real blocks back. Nothing on the server

Runs when: Standing room under open sky 8 to 14 blocks away

## `unlit_torches`

Only this player's client is told that the 24 nearest torches are the burnt-out `unlit_` ones. 30 s later it gets the real blocks back. Nothing on the server

Runs when: A torch within 12

## `fake_ore`

Only this player's client is told that one such block is a diamond ore (deepslate diamond ore in deepslate). A click on it shows the stone. After 1 min it gets the real block back. Nothing on the server

Runs when: No sky overhead, and stone or deepslate touching air within 8

## `ladder_stub`

Only this player's client is told that the upper half of the column is air. 20 s later it gets the real blocks back. Nothing on the server

Runs when: A column of at least 3 ladders within 10

## `second_door`

Only this player's client is told of an oak door in the wall. 20 s later it gets the real blocks back. Nothing on the server

Runs when: A wall one block thick and two high in front of the player with free room behind it

## `false_harvest`

Only this player's client is told that the 40 nearest such crops are fully grown. 20 s later it gets the real blocks back. Nothing on the server

Runs when: A crop not fully grown within 10

## `lava_pond`

Only this player's client is told that the 200 nearest such water blocks are lava. 1 s later it gets the real blocks back. Nothing on the server

Runs when: A source of water with open air above within 12

## `lava_as_water`

A sizzle sounds at the lava, and only this player's client is told that the 300 nearest lava blocks are water. They show as lava again when the player comes within 8 blocks, or after 30 s, or at once on a click. Always an illusion, never real, in any swaps mode

Runs when: Health > 5 hearts, at least 6 lava blocks within 16 and none within 9

## `roof_snow`

Only this player's client is told of two layers of snow on the 80 nearest roof blocks. 1 min later it gets the real blocks back. Nothing on the server

Runs when: Sky visible at the player's feet, and a roof of planks, cobblestone, stone bricks, bricks, terracotta or deepslate within 8, at least 2 blocks above the player

## `black_glass`

Only this player's client is told that the 48 nearest glass blocks are tinted glass and the panes black stained glass panes. 15 s later it gets the real blocks back. Nothing on the server

Runs when: Glass or a glass pane within 10

## `red_wool`

Only this player's client is told that the 64 nearest wool blocks and carpets are red. 15 s later it gets the real blocks back. Nothing on the server

Runs when: Wool or a carpet within 10 that is not red

## `sign_whisper`

Only this player's client gets the sign's block entity with one of the listed texts on its front. 15 s later it gets the real text. Nothing on the server

Runs when: A sign within 10

## `wither_roses`

Only this player's client is told that the 48 nearest small flowers are wither roses. 15 s later it gets the real flowers back. Nothing on the server

Runs when: A small flower within 12 that is not a wither rose

## `block_flicker`

Only this player's client is told that up to 200 nearest blocks of one kind found there are one random block of the list `to`. One tick later it gets the real blocks back. Nothing on the server

Runs when: A block of the list `from` (stone, dirt, grass, cobblestone, deepslate, sand, oak planks) within 8

## `lone_gravestone`

Only this player's client is told of a gravestone there. 30 s later it gets the real block back. Nothing on the server

Runs when: Standing room 9 blocks ahead (within 40 degrees) under the open sky

## `broken_portal`

Only this player's client is told that 3 random obsidian blocks touching the portal are air. 15 s later it gets the real blocks back. Nothing on the server

Runs when: A nether portal block within 16

## `dark_circle`

Only this player's client is told of light blocks (level 12) in a ring of radius 3 and in its middle. They go out when the player comes within 3 blocks of the middle, or after 30 s. Nothing on the server

Runs when: Underground (no sky) with block light 5 or less at the player, and standing room 8 blocks ahead with at least 6 of 12 ring points free

## `fence_gaps`

Only this player's client is told that 3 random fence posts there are air. 20 s later it gets the real posts back. Nothing on the server

Runs when: At least 4 fence posts within 12

## `corner_cobwebs`

Only this player's client is told that up to 12 random such corners hold a cobweb. 20 s later it gets the real blocks back. Nothing on the server

Runs when: At least 3 empty corners within 8 where the ceiling meets two walls

## `redstone_trail`

Only this player's client is told of the dust, following the ground up and down. 20 s later it gets the real blocks back. Nothing on the server

Runs when: Room on the floor for at least 5 blocks of a straight line in one of the four directions, 3 to 15 blocks away

## `composter_swing`

An empty composter looks full, or a full one looks emptied, for about a minute

Runs when: A composter within 12

## `banner_skull`

A black skull appears on a banner, and is taken off again after a couple of minutes

Runs when: A banner within 12 with room for another pattern

## `stairs_flip`

A few stairs behind your back turn upside down, then right again after a minute

Runs when: Stairs within 12 behind where you are looking

## `lectern_swap`

The book on a lectern is another one, signed by the spirit; the first comes back later

Runs when: A lectern with a book within 12

## `skulls_turn`

Every such skull is shown turned to face the player (a swap, see `swaps.mode`); restored after 30 s

Runs when: Any floor or wall skull within 12, in any dimension

## `anchor_charge`

One of them shows one more charge, with the charging sound (a swap, see `swaps.mode`); restored after 30 s, and under hardcore it stays

Runs when: Nether, a respawn anchor below full charge within 10

## `vines_reach`

The vine nearest the player grows up to 3 blocks down into the air, ending in a full-grown head; put back after 30 s, and under hardcore it stays

Runs when: Nether, a weeping vine end with air below within 12

## `void_wind`

A wind-burst howl, then after 2 s the player on the ground is nudged outward at about 2.4 blocks/s every 0.1 s for 5 s, with the howl repeated each second. It ends the moment ground is missing within 10 blocks along the way, so the edge is never reached

Runs when: End, health > 5 hearts, on the ground, not riding, 24 or more blocks from the middle, and ground (within 4 blocks below) at every second block out to 10 blocks along the way outward

## `gateway_beam`

The gateway's own beam event is sent, as when an entity enters it, so it shows its beam for 2 s. Nobody is moved

Runs when: End, a gateway in the 9×9 chunks around the player

## `chorus_shift`

Only this player's client is told that the 24 nearest such blocks are one block over in a single direction (air where they were). After 10 s it gets the real blocks back. Nothing on the server

Runs when: End, a chorus plant or flower within 12 blocks with free air beside it

## `phantom_egg`

Only this player's client is told of a dragon egg standing there. A click shows the truth. After 10 s it gets the real block back. Nothing on the server

Runs when: End, standing room 4–12 blocks away

## `ender_chests_open`

Five ender chest open sounds, 3–8 blocks away at random bearings, 0.4 s apart, each followed by the close sound 1.5 s after. Nothing in the world

Runs when: End

## `nowhere_portal`

Only this player's client is told of a lit nether portal in a frame of obsidian (no corners) there, facing them, with the portal ambient sound. A click shows air. After 15 s it gets the real blocks back. Nothing on the server

Runs when: Overworld, a 4×5 empty space on solid ground 14 blocks away within 40° of where the player looks

## `desert_sand_trickle`

Falling sand particles under the first solid block above the player's head, 12 pulses 0.25 s apart, with a sand-breaking sound. Nothing in the world changes

Runs when: In a desert, with a solid block within 6 above the player's head

## `cauldron`

Brewing bubble sounds and bubble particles over a cauldron within 12, 8 pulses 0.5 s apart

Runs when: A cauldron within 12, in any biome

## `fireflies_dark`

Only this player's client is told that every firefly bush within 12 is a plain bush. 30 s later it gets the real blocks back. Nothing on the server

Runs when: A firefly bush within 12, in any biome

## `snow_door_drift`

Four layers of real snow on the outside of a wooden door within 12 and three layers on each side along the wall; removed after 2 min (stays under hardcore)

Runs when: In a snowy biome, with a wooden door within 12 that has open sky and free ground outside it

## `window_vines`

Real vines grow on up to 24 of the nearest air blocks beside the glass, hung toward it. 60 s later each goes unless it was changed meanwhile. Under hardcore they stay

Runs when: In a jungle, swamp or mangrove swamp biome, glass or a glass pane within 12 with air beside it

## `eyeblossoms_open`

Only this player's client is told that up to 12 of the nearest such flowers are open, and an opening sounds. 30 s later it gets the real blocks back. Nothing on the server

Runs when: A closed eyeblossom within 12, regardless of biome or view

## `resin_clumps`

Real resin clumps grow on up to 8 air blocks beside the walls, clustered around a random spot, with a placing sound. 2 min later each goes unless it was changed meanwhile. Under hardcore they stay

Runs when: In a forest, flower forest, birch forest, old growth birch forest, dark forest or pale garden, a plank, cobblestone, stone brick, brick or wool wall within 12 with air beside it

## `canopy_closes`

Only this player's client is told that the 40 nearest such blocks are persistent dark oak leaves. 15 s later it gets the real blocks back. Nothing on the server

Runs when: In the dark forest or pale garden, at least 6 air blocks touching leaves at least 3 above the player within 8

## `sculk_creep`

Real sculk spreads in a trail of up to 12 blocks from far ground toward the player, one every second, each next to the last and nearer, stopping 3 blocks away. 2 min later each goes unless it was changed meanwhile. Under hardcore it stays

Runs when: In the deep dark, sculk-replaceable ground with air above, 8 to 14 blocks away

## `berries_dark`

Up to 10 of the nearest bunches lose their berries, one per second; 1 min later each has them again. Under hardcore they stay dark

Runs when: Glow berries within 14, in any biome

## `scarecrow_closer`

A scarecrow stands a step closer than it did, then goes back after a minute

Runs when: A scarecrow within 14, at least 4 away, with room to step toward you

## `ouija_hi`

The planchette spells "HI" on its own

Runs when: An ouija board within 8

## `spirit_box_word`

The spirit box says a word nobody asked for

Runs when: A spirit box within 6

## `salt_gap`

One cell of a salt line disappears, the one nearest the spirit, and returns after two minutes

Runs when: At least 4 salt cells within 8

## `grave_name`

A gravestone reads your own name

Runs when: A gravestone within 16

## `journal_page_torn`

One journal entry is gone for a few minutes, then back in its place

Runs when: You carry a journal with at least 3 entries

## `dreamcatcher_fails`

A dreamcatcher swings hard, and the dream comes anyway

Runs when: A dreamcatcher within 8 and a dream that can start

## `lantern_soul_fire`

A hallowed lantern burns with soul fire for half a minute and does not ward meanwhile

Runs when: A hallowed lantern within 10 and a spirit about

## `charm_falls`

A door charm drops off its hook as an item

Runs when: A door charm within 10

## `altar_midnight`

At night the candles around an altar light themselves one after another

Runs when: Night in the overworld and an altar within 12 with unlit candles beside it

## `beacon_out`

**Harmful.**
An obsidian or black stained glass block is shown right above it, so the beam goes out or turns black. 30 s, this player only (a real block under `real` swaps; under hardcore it stays)

Runs when: An active beacon within 16 with air above it

## `campfire_cooks`

Rotten flesh or a rabbit foot shows on the grill for 1 min, to this player only. Nothing cooks and the real campfire is unchanged

Runs when: A campfire with a free slot within 10

## `spawner_shows`

The spinning mob inside is a villager, iron golem, cat, wandering trader, chicken or witch for 30 s, this player only. The real spawner is unchanged

Runs when: A monster spawner within 16

## `trial_ominous`

Shown as ominous for 10 s, this player only

Runs when: A calm trial spawner within 16

## `vault_head`

Its display item is the player's own head for 30 s, this player only

Runs when: A vault within 16

## `black_bed`

Both halves show as the black bed, same facing and part, for 1 min, this player only (a bed is never really swapped)

Runs when: A bed that is not black within 12

## `black_carpets`

The 48 nearest show as black carpets for 1 min (really changed in `real` mode; stays under hardcore)

Runs when: Indoors, a carpet that is not black within 8

## `netherrack_behind`

Only this player's client is told that the 40 nearest such blocks are netherrack. They get the real blocks back when the player looks within 70° of any of them, or after 30 s. Nothing on the server (with `swaps.mode` real, changed for 30 s)

Runs when: A solid block with a free face 2 to 8 away, more than 100° from where the player looks

## `roof_gone`

Only this player's client is told that every block from 2 above the player to the sky, 2 blocks to either side, is air. 20 s later it gets the real blocks back. Nothing on the server

Runs when: Indoors, with at most 6 blocks of roof over the player's head; at least 4 blocks to hide

## `path_rearranged`

Only this player's client is told that those floor blocks are shuffled among each other. They get the real blocks back when the player looks within 70° of any of them, or after 30 s. Nothing on the server

Runs when: At least 8 floor blocks 3 to 10 behind the player (1 to either side) of at least two kinds

## `landmark_tree_gone`

Only this player's client is told that the tallest such tree (logs and leaves) is air. It gets the real blocks back when the player comes within 12 of it, or after 60 s. Nothing on the server

Runs when: The player's bed is in this dimension; a tree of at most 250 blocks and 8 leaves within 24 of it, 18 to 64 from the player

## `tunnel_filled`

Only this player's client is told that both blocks of each such cell are stone. They get the real blocks back when the player looks within 70° of any of them, or after 30 s. Nothing on the server. Not limited to tunnels the player dug

Runs when: Under no sky; at least 6 walkway cells 2 to 10 behind the player (1 to either side) with a solid block 2 above and no sky

## `ceiling_furniture`

Only this player's client is told that up to 6 such blocks hold an upside-down stair or slab, or a bed (two blocks), at random facings. 30 s later it gets the real blocks back. Nothing on the server

Runs when: Indoors, an air block under the ceiling 6 blocks around the player, at least 1.5 from them and above their feet

## `crafter_crafts`

The crafter shows its crafting state (6 ticks) with the craft sound and particles, and ejects a bone, skeleton skull or paper named after the player from its face; the item vanishes after 1 min. Its real contents are not touched

Runs when: A crafter within 12

## `scaffold_collapses`

**Harmful.**
Three creaks, 14 ticks apart, then every scaffolding block of the column breaks and drops (only if the player still stands on it)

Runs when: Standing on a scaffolding column on solid ground, a fall of under 4 blocks

## `wall_map_shows`

The framed map is replaced by a new map of the area around the spirit's anchor; the original is put back after 2 min (stays under hardcore)

Runs when: A framed filled map within 12 and a territorial spirit whose reach holds the player, with the 128-block area around its anchor loaded

## `haunted_painting`

A painting becomes one of the mod's own (a portrait of no one, the house at night, a pair of eyes), and is put back after a minute (stays under hardcore)

Runs when: A painting within 12 that has a mod variant of the same size

## `painting_darkens`

A large painting darkens in four steps, 20 s apart, and a figure comes up the path to the house; put back after 3 min (stays under hardcore)

Runs when: A 2 by 2 painting within 12

## `sigil_banner`

A black spirit sigil appears on a banner, and is taken off after 2 min (stays under hardcore)

Runs when: A banner within 12 with room for another pattern

## `abandoned_camp`

A bed, a campfire and a chest holding a signed book in the spirit's voice (a page from a pool, never repeated for the same spirit); every block is removed after 10 min unless it was changed, a chest with the player's own items stays; stays under hardcore

Runs when: Overworld; open standing room for a 4×3 camp 6–14 away; the spirit still has a book page it has not said

## `dirt_hut`

A dirt hut with a two-high doorway and a roof; removed block by block after 7.5 min, stays under hardcore

Runs when: Overworld; open ground for a 3×3 hut 5–12 away

## `rocking_chair`

A rocking chair appears and leans forward and back 24 times with a creak, then is gone 5 s later

Runs when: A spot of free floor under a roof within 10, 3+ away and outside 55° of where the player looks

## `rotten_planks`

One plank turns into rotten planks, then the rot spreads to touching planks, 2 per second, up to 24 blocks. They are put back after 2 min; under hardcore they stay

Runs when: Indoors, planks within 10

## `cracked_mirror`

A cracked mirror appears on the wall. When the player looks at it from within 6 blocks, a dark figure shows in it for 2 s. Gone after 30 s more

Runs when: Indoors, a wall at head height within 8, 2+ away and outside 60° of where the player looks
