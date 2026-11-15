---
title: Doors
---

# Doors

45 events.

## `door`

A closed door opens, or an open one slams shut.

Runs when: a wooden door is within 12 blocks.

## `breathing_door`

The door toggles once a second, six times, and ends as it began. A faint dark haze puffs at the door each time it moves.

Runs when: a wooden door is within 12 blocks.

## `trapdoor`

The trapdoor toggles with a slam.

Runs when: a wooden trapdoor is within 12 blocks.

## `fence_gate`

The fence gate toggles.

Runs when: a fence gate is within 12 blocks.

## `button`

The button is pressed.

Runs when: an unpressed button is within 12 blocks.

## `lever`

The lever is flipped.

Runs when: a lever is within 12 blocks.

## `pressure_plate`

The pressure plate is triggered for a second.

Runs when: an unpressed pressure plate is within 12 blocks.

## `iron_door`

The iron door toggles without any redstone.

Runs when: an iron door is within 12 blocks.

## `open_house`

**Harmful.**
Every such door opens. A faint dark haze puffs at each door that it opens.

Runs when: it is night and a closed wooden door is within 20 blocks.

## `chest_lid`

The lid opens and closes with sound, and the contents are left untouched.

Runs when: a chest or trapped chest is within 10 blocks.

## `barrel_opens`

The barrel opens for 1.5 s and then closes.

Runs when: a closed barrel is within 10 blocks.

## `jukebox_pop`

The music cuts out and the disc pops out of the jukebox.

Runs when: a jukebox within 20 blocks is playing a disc.

## `lectern_pages`

The book turns to another page with a page-turn sound. This goes through the lectern's own menu, so a comparator sees the change.

Runs when: a lectern within 12 blocks holds a book of more than one page.

## `pot_wobble`

The pot wobbles as if it had refused an item.

Runs when: a decorated pot is within 12 blocks.

## `bell_swing`

The bell swings and rings toward a random side.

Runs when: a bell is within 20 blocks.

## `redstone_drift`

**Harmful.**
A repeater gets another delay and a comparator the other mode, with no block update. After 2 min the change is set back if nothing touched it, and under hardcore it stays.

Runs when: a repeater or comparator is within 12 blocks.

## `occupied_bed`

Both halves of the bed read as occupied for 1 min, so the vanilla "This bed is occupied" message shows. The change is always put back, hardcore or not. A faint dark haze hangs over the bed for as long as it reads as occupied.

Runs when: an unoccupied bed is within 12 blocks.

## `shuffled_books`

The books take other slots. After 2 min they are put back if untouched, and under hardcore they stay.

Runs when: a chiseled bookshelf with a book in it is within 12 blocks.

## `dispenser_shot`

**Harmful.**
The dispenser clicks as if it were empty, and after 1 s an arrow leaves its face toward the player. Nothing is taken from its contents, and the arrow cannot be picked up.

Runs when: a dispenser is within 12 blocks.

## `chest_blast`

**Harmful.**
A creeper hiss comes from the chest, and after 1 s an explosion of power 1 follows that breaks no blocks.

Runs when: a chest is within 10 blocks and the player's health is above 10.

## `wrong_smelt`

The output becomes charcoal or rotten flesh of the same count. After 2 min it is put back unless it was taken or changed (under hardcore it stays).

Runs when: a furnace within 10 blocks has something in its output.

## `furniture_moves`

**Harmful.**
The piece of furniture is moved to stand 2 to 5 blocks away on free floor, with its block entity and contents carried over through its save format and a bed moved with both halves. After 5 min it is moved back if both spots are unchanged, and under hardcore it stays.

Runs when: a single chest, crafting table, furnace or empty bed is within 12 blocks.

## `doorstep_gift`

A poppy, dandelion, bone or raw rabbit lies outside the door, on the side away from the player, and it is a real item.

Runs when: a door within 12 blocks has free floor on its far side.

## `muddy_footprints`

Pairs of muddy prints appear one after another on every first or second block from the player to the bed or chest, each with a mud step sound. Each print is wiped after 2 min if it is still there, and under hardcore they stay.

Runs when: the player is indoors, and a bed or chest on the player's level within 10 blocks can be reached over free floor.

## `locked_door`

**Harmful.**
The door slams shut with a thud and cannot be opened for 10 s. The server tells clients within 64 blocks, so an attempt to open it is refused locally (with the locked-chest sound), with no opening and snapping back. A faint dark haze hangs at the door while it is locked.

Runs when: a wooden door is within 12 blocks.

## `hook_click`

The hook clicks on and then off a moment later, with no string to trip it.

Runs when: a tripwire hook is within 12 blocks.

## `daylight_flip`

The daylight detector turns inverted, so lamps on it light at noon and go dark at night. A minute later it is set back.

Runs when: a daylight detector is within 12 blocks.

## `rod_sparks`

The lightning rod is powered for a moment and throws sparks, though there is no storm.

Runs when: an unpowered lightning rod is within 16 blocks.

## `torch_burnout`

The torch burns out with the vanilla smoke and fizz and lights again after 8 s, or sooner on any update beside it.

Runs when: a lit redstone torch is within 12 blocks.

## `chests_open`

Every lid in the room opens at once and shuts 2 s later, with the contents untouched.

Runs when: the player is indoors and two or more chests are within 10 blocks.

## `anvil_lands`

The sound of an anvil landing is heard. Under hardcore the anvil also gains one damage stage.

Runs when: it is night and an anvil is within 16 blocks.

## `cake_bite`

One more slice of the cake is gone, with an eating sound, and it comes back after a minute.

Runs when: a cake with a slice left is within 12 blocks.

## `tnt_hiss`

The primed-TNT hiss comes from the block, though nothing is lit.

Runs when: a block of TNT, or a chest holding TNT, is within 12 blocks.

## `pot_holds`

A bone, a skull sherd or an echo shard sits in the pot with a wobble, and it is taken out again after two minutes.

Runs when: an empty decorated pot is within 12 blocks.

## `piston_twitch`

A piston twitches out and back, only on the player's screen, and pushes nothing.

Runs when: a piston with air in front of it is within 12 blocks.

## `hopper_swallows`

An item lying near a hopper slides over it, and the hopper takes it.

Runs when: a hopper within 12 blocks has a dropped item near it.

## `sculk_chain`

Sculk sensors click one after another, with the farthest first, toward the player.

Runs when: two or more sculk sensors are within 16 blocks.

## `observer_pulse`

An observer pulses a few times and runs whatever it is wired to.

Runs when: an observer is within 12 blocks.

## `bed_missing`

The player receives the vanilla message that the home bed was missing or obstructed, while the spawn point stays as it is.

Runs when: the player has a respawn point.

## `ender_note`

A note the player never wrote shows in an empty slot of the open ender chest, and it is gone at the first action in that inventory.

Runs when: an ender chest is open and has an empty slot.

## `brewing_swap`

One bottle becomes another potion (water, awkward, night vision, swiftness, healing, harming, poison, weakness, slowness or invisibility). After 5 min it is put back if untouched, and under hardcore it stays.

Runs when: a brewing stand holding a potion bottle is within 10 blocks.

## `brewing_bubbles`

The brewing sound and bubbles are played above the stand, 6 times, 0.4 s apart. Nothing changes.

Runs when: a brewing stand with nothing in any slot is within 10 blocks.

## `statue_pose`

The statue's pose becomes another one while the player is not looking. After 2 min it is set back, and under hardcore it stays.

Runs when: a copper golem statue is within 12 blocks and lies outside 60° of where the player looks.

## `open_door_view`

The player's view moves for 3 s into the doorway of the nearest open door, looking in toward the player, and then moves back. It ends early in the same cases as `mob_eyes`.

Runs when: the player is on the ground and not in a fluid, and an open door is 3 to 12 blocks away.

## `claw_marks`

Claw marks appear with a scrape on the player's side of both halves of the door. They are removed after 3 min, and under hardcore they stay.

Runs when: the player is indoors, and a closed door within 8 blocks has free blocks on the player's side.
