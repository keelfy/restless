---
title: Items
---

# Items

23 events.

## `swap_slots`

Two inventory slots swap

Runs when: Inventory not empty

## `hotbar_shift`

Selected hotbar slot changes

Runs when: Always

## `book_page`

One creepy page is appended (nothing is erased)

Runs when: A book and quill or a written book

## `shuffle_chest`

Its contents are shuffled

Runs when: A non-empty chest within 10

## `wear`

**Harmful.**
Loses 10% durability, never breaks

Runs when: A damageable tool with durability to spare

## `rot`

**Harmful.**
One food stack becomes rotten flesh

Runs when: Food in the inventory

## `tool_look`

Drawn as another such tool for 5 min (it still works the same); under hardcore or real swaps it becomes that tool

Runs when: A wooden, stone, iron or diamond tool in the inventory

## `armor_off`

**Harmful.**
1–4 random worn pieces come off (a random set, so sometimes all of it). Each goes to the inventory, or to the floor at the player's feet if it is full; nothing is lost

Runs when: Any armor worn

## `vanishing_item`

**Harmful.**
One stack moves to the other chest

Runs when: Two chests (not halves of one) within 10, one non-empty, one with space

## `false_compass`

**Harmful.**
Points at a spot 30–60 blocks away (stays that way)

Runs when: A plain compass in the inventory

## `map_marker`

A red X appears on it somewhere in its area. Permanent: the marker is stored in the map item

Runs when: A filled map in the inventory without a marker of this event

## `elytra_flicker`

Worn elytra look almost broken for 2 s (only shown to the player); under hardcore they are worn down for real

Runs when: Elytra worn with durability to spare, 12 or more blocks above the ground

## `stew_bowl`

**Harmful.**
One stew becomes an empty bowl

Runs when: Mushroom, beetroot, rabbit or suspicious stew in the inventory

## `bucket_pufferfish`

A water bucket looks like a pufferfish bucket for 5 s (only shown to the player); under hardcore it is swapped for real

Runs when: A water bucket in the inventory

## `torch_burn`

**Harmful.**
Half of a stack of torches is gone, at least one is left

Runs when: A stack of 2 or more torches or soul torches

## `bundle_bone`

A bone is added to a bundle's contents (nothing is taken out)

Runs when: A bundle with room for a bone

## `shield_ghost`

The sound of a shield blocking a hit, with nothing there

Runs when: A shield in either hand

## `item_throwback`

A dropped item is thrown at the player, slowly enough to dodge

Runs when: A dropped item 3–12 blocks away

## `phantom_horn`

The goat horn's own tune sounds at the player

Runs when: A goat horn with a tune in the inventory

## `armor_dye`

A piece of leather armor is dyed black, and its old colour returns after 5 min; under hardcore it stays black

Runs when: A leather armor piece, worn or carried, that is not black

## `item_glint`

An item glints as if enchanted for 10 s

Runs when: An item that does not glint

## `item_lore`

A lore line such as "it remembers you" is added and removed after 5 min; under hardcore it stays

Runs when: Any item in the inventory or worn

## `tool_mirage`

**Harmful.**
A tool shows full durability, then almost broken, then the truth (only shown to the player); under hardcore it is worn down for real

Runs when: A damageable tool with durability to spare
