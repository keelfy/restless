---
title: Doors
---

# Doors

45 events.

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

**Harmful.**
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

**Harmful.**
A repeater gets another delay, a comparator the other mode, with no update. Set back after 2 min if unchanged; under hardcore it stays

Runs when: A repeater or comparator within 12

## `occupied_bed`

Both halves read as occupied for 1 min, so the vanilla "This bed is occupied" shows. Always put back, hardcore or not

Runs when: An unoccupied bed within 12

## `shuffled_books`

The books take other slots. Put back after 2 min if untouched; under hardcore they stay

Runs when: A chiseled bookshelf within 12 with a book

## `dispenser_shot`

**Harmful.**
It clicks as if empty, then after 1 s an arrow leaves its face toward the player. Takes nothing from its contents, the arrow cannot be picked up

Runs when: A dispenser within 12

## `chest_blast`

**Harmful.**
A creeper hiss from the chest, then after 1 s an explosion of power 1 that breaks no blocks

Runs when: A chest within 10, health above 10

## `wrong_smelt`

The output becomes charcoal or rotten flesh of the same count; put back after 2 min unless taken or changed (stays under hardcore)

Runs when: A furnace within 10 with something in its output

## `furniture_moves`

**Harmful.**
It stands 2 to 5 blocks away on free floor, block entity and contents carried through its save format, a bed both halves. Moved back after 5 min if both spots are unchanged; under hardcore it stays

Runs when: A single chest, crafting table, furnace or empty bed within 12

## `doorstep_gift`

A poppy, dandelion, bone or raw rabbit lies outside, on the side away from the player; a real item

Runs when: A door within 12 with free floor on its far side

## `muddy_footprints`

Pairs of muddy prints appear one by one on every first or second block from the player to it, each with a mud step. Each is wiped after 2 min if still there; under hardcore they stay

Runs when: Indoors, a bed or chest within 10 on the player's level, reachable over free floor

## `locked_door`

**Harmful.**
Slams shut with a thud and cannot be opened for 10 s. The server tells clients within 64 blocks, so the click is refused locally (locked-chest sound) with no open-and-snap-back

Runs when: A wooden door within 12

## `hook_click`

The hook clicks on, then off a moment later, with no string to trip it

Runs when: A tripwire hook within 12

## `daylight_flip`

It turns inverted, so lamps on it light at noon and go dark at night; set back after a minute

Runs when: A daylight detector within 12

## `rod_sparks`

It is powered for a moment and throws sparks, with no storm

Runs when: An unpowered lightning rod within 16

## `torch_burnout`

It burns out with the vanilla smoke and fizz and lights again after 8 s, or sooner on any update beside it

Runs when: A lit redstone torch within 12

## `chests_open`

Every lid in the room opens at once and shuts 2 s later, contents untouched

Runs when: Indoors, and two or more chests within 10

## `anvil_lands`

The sound of an anvil landing; under hardcore the anvil also gains one damage stage

Runs when: Night, and an anvil within 16

## `cake_bite`

One more slice is gone with an eating sound; it comes back after a minute

Runs when: A cake within 12 with a slice left

## `tnt_hiss`

The primed-TNT hiss comes from it, with nothing lit

Runs when: A block of TNT, or a chest holding TNT, within 12

## `pot_holds`

A bone, a skull sherd or an echo shard sits in it with a wobble; taken out again after two minutes

Runs when: An empty decorated pot within 12

## `piston_twitch`

A piston twitches out and back, only on your screen, and pushes nothing

Runs when: A piston within 12 with air in front

## `hopper_swallows`

An item lying near a hopper slides over it and the hopper takes it

Runs when: A hopper within 12 with a dropped item near it

## `sculk_chain`

Sculk sensors click one after another, the farthest first, toward you

Runs when: Two or more sculk sensors within 16

## `observer_pulse`

An observer pulses a few times and runs whatever it is wired to

Runs when: An observer within 12

## `bed_missing`

"Your home bed was missing or obstructed", while the spawn point stays as it is

Runs when: You have a respawn point

## `ender_note`

A note you never wrote shows in an empty slot of the open ender chest, gone at the first click

Runs when: An ender chest is open with an empty slot

## `brewing_swap`

One bottle becomes another potion (water, awkward, night vision, swiftness, healing, harming, poison, weakness, slowness or invisibility); put back after 5 min if untouched (stays under hardcore)

Runs when: A brewing stand with a potion bottle within 10

## `brewing_bubbles`

The brewing sound and bubbles above it, 6 times, 0.4 s apart. Nothing changes

Runs when: A brewing stand with nothing in any slot within 10

## `statue_pose`

Its pose becomes another one while the player is not looking; set back after 2 min (stays under hardcore)

Runs when: A copper golem statue within 12, outside 60° of where the player looks

## `open_door_view`

The view moves for 3 s into the doorway of the nearest open door, looking in toward the player; then back. Same early resets as `mob_eyes`

Runs when: Player on the ground, not in a fluid, and an open door 3 to 12 blocks away

## `claw_marks`

Claw marks appear on the player's side of both halves of the door with a scrape. Removed after 3 min; under hardcore they stay

Runs when: Indoors, a closed door within 8 with free blocks on the player's side
