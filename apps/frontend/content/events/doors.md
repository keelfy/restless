---
title: Doors
---

# Doors

25 events.

## `door`

Opens a closed door or slams an open one

Runs when: A wooden door is within 12 blocks

## `breathing_door`

Toggles once a second, six times, ending as it began

Runs when: A wooden door within 12

## `trapdoor`

Toggles it with a slam

Runs when: A wooden trapdoor within 12

## `fence_gate`

Toggles it

Runs when: A fence gate within 12

## `button`

Presses it

Runs when: An unpressed button within 12

## `lever`

Flips it

Runs when: A lever within 12

## `pressure_plate`

Triggers it for a second

Runs when: An unpressed pressure plate within 12

## `iron_door`

Toggles it, no redstone

Runs when: An iron door within 12

## `open_house`

Harmful.
Opens every such door

Runs when: Night, and a closed wooden door within 20

## `chest_lid`

Lid opens and closes with sound, contents untouched

Runs when: A chest or trapped chest within 10

## `barrel_opens`

Opens for 1.5 s, then closes

Runs when: A closed barrel within 10

## `jukebox_pop`

The music cuts out and the disc pops out of it

Runs when: A jukebox within 20 is playing a disc

## `lectern_pages`

The book turns to another page with a page-turn sound, through the lectern's own menu so a comparator sees the change

Runs when: A lectern within 12 holds a book of more than one page

## `pot_wobble`

It wobbles as if an item were refused

Runs when: A decorated pot within 12

## `bell_swing`

It swings and rings toward a random side

Runs when: A bell within 20

## `redstone_drift`

Harmful.
A repeater gets another delay, a comparator the other mode, with no update. Set back after 2 min if unchanged; under hardcore it stays

Runs when: A repeater or comparator within 12

## `occupied_bed`

Both halves read as occupied for 1 min, so the vanilla "This bed is occupied" shows. Always put back, hardcore or not

Runs when: An unoccupied bed within 12

## `shuffled_books`

The books take other slots. Put back after 2 min if untouched; under hardcore they stay

Runs when: A chiseled bookshelf within 12 with a book

## `dispenser_shot`

Harmful.
It clicks as if empty, then after 1 s an arrow leaves its face toward the player. Takes nothing from its contents, the arrow cannot be picked up

Runs when: A dispenser within 12

## `chest_blast`

Harmful.
A creeper hiss from the chest, then after 1 s an explosion of power 1 that breaks no blocks

Runs when: A chest within 10, health above 10

## `wrong_smelt`

The output becomes charcoal or rotten flesh of the same count; put back after 2 min unless taken or changed (stays under hardcore)

Runs when: A furnace within 10 with something in its output

## `furniture_moves`

Harmful.
It stands 2 to 5 blocks away on free floor, block entity and contents carried through its save format, a bed both halves. Moved back after 5 min if both spots are unchanged; under hardcore it stays

Runs when: A single chest, crafting table, furnace or empty bed within 12

## `doorstep_gift`

A poppy, dandelion, bone or raw rabbit lies outside, on the side away from the player; a real item

Runs when: A door within 12 with free floor on its far side

## `muddy_footprints`

Pairs of muddy prints appear one by one on every first or second block from the player to it, each with a mud step. Each is wiped after 2 min if still there; under hardcore they stay

Runs when: Indoors, a bed or chest within 10 on the player's level, reachable over free floor

## `locked_door`

Harmful.
Slams shut with a thud and cannot be opened for 10 s. The server tells clients within 64 blocks, so the click is refused locally (locked-chest sound) with no open-and-snap-back

Runs when: A wooden door within 12
