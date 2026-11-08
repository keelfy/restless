---
title: Items
---

# Items

40 events.

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

## `lodestone_lure`

A lodestone compass points at the anchor of the spirit nearby for 5 min; under hardcore it stays that way

Runs when: A compass with a lodestone target in the inventory, with a territorial spirit's reach around the player

## `ghost_author`

A written book signed with the player's own name turns up, with one creepy page

Runs when: A free inventory slot and no book signed by the player yet

## `potion_swap`

A potion becomes another one for 5 min; under hardcore it stays

Runs when: A potion, splash or lingering potion with a base potion and no custom effects

## `vanishing_arrows`

**Harmful.**
Up to 6 arrows leave the inventory one by one and come back after 3 min; under hardcore they stay gone

Runs when: An arrow in the inventory

## `crossbow_misfire`

**Harmful.**
A loaded crossbow fires straight up with reduced power

Runs when: A loaded crossbow in either hand

## `map_elsewhere`

The map in the hand shows the area around the anchor of the spirit nearby for 5 min; under hardcore it stays

Runs when: A map in either hand, with a territorial spirit's reach around the player

## `shy_item`

One dropped item slides away from the player

Runs when: A dropped item within 5

## `orb_scatter`

Experience orbs fly away from the player

Runs when: An experience orb within 8

## `line_pulls`

**Harmful.**
A shove toward the bobber, with a splash there

Runs when: The player's bobber floats in water

## `odd_catch`

A player head in their own likeness, a bone, a skeleton skull or rotten flesh is thrown out of the water at the player, as a real catch is. It vanishes after 1 min if not picked up

Runs when: The player's bobber floats in water

## `allay_thief`

Only this player's client is told of an allay by their side holding a copy of one of their stacks, which flies 45 blocks straight away and is gone. Nothing is taken and nothing is on the server

Runs when: The player carries anything

## `holy_water_cloudy`

**Harmful.**
Every holy water stack gets a "cloudy" mark and a lore line; the altar refuses it until the next dawn, then the mark is removed. Stays

Runs when: Clear holy water in the inventory

## `mirror_names`

A soft chime marks the mirror. The next time a mirror is placed on the altar, the name of a spirit around the player is shown in chat, once. The mark lapses after 30 min

Runs when: A mirror in the inventory, a spirit that senses the player, none already marked

## `pearl_strays`

For 5 min the first pearl the player throws is moved above standing room 8 to 24 blocks away and dropped, so the player lands there (with the usual 5 damage)

Runs when: An ender pearl in the inventory

## `whispering_item`

The item in your hand gains the Curse of the Restless and whispers now and then while held; the curse is lifted after 3 min (stays under hardcore)

Runs when: You hold an item without the curse

## `hollow_trim`

A worn piece of armor shows a hollow trim for 2 min (stays under hardcore)

Runs when: You wear trimmable armor with no trim

## `photo_figure`

A pale hollow-eyed figure 34–60 px tall is painted into one developed photo's picture; the change is kept

Runs when: A developed photo in the inventory
