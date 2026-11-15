---
title: Items
---

# Items

42 events.

## `swap_slots`

Two inventory slots swap places.

Runs when: the inventory is not empty.

## `hotbar_shift`

The selected hotbar slot changes.

Runs when: at any time.

## `book_page`

One creepy page is appended to the book, and nothing is erased.

Runs when: the player has a book and quill or a written book.

## `shuffle_chest`

The contents of the chest are shuffled.

Runs when: a non-empty chest is within 10 blocks.

## `wear`

**Harmful.**
The item loses 10% durability but never breaks.

Runs when: the player has a damageable tool with durability to spare.

## `rot`

**Harmful.**
One food stack becomes rotten flesh.

Runs when: the inventory holds food.

## `tool_look`

The tool is drawn as another tool of the same kind for 5 min, though it still works the same. Under hardcore or real swaps it actually becomes that tool.

Runs when: the inventory holds a wooden, stone, iron or diamond tool.

## `armor_off`

**Harmful.**
1–4 random worn pieces come off, from a random set, so sometimes all of the armor. Each piece goes to the inventory, or to the floor at the player's feet if the inventory is full, and nothing is lost.

Runs when: the player wears any armor.

## `vanishing_item`

**Harmful.**
One stack moves to the other chest.

Runs when: two chests (not the halves of one) are within 10 blocks, and one of them is non-empty while the other has space.

## `false_compass`

**Harmful.**
The compass points at a spot 30–60 blocks away, and it stays that way.

Runs when: the inventory holds a plain compass.

## `map_marker`

A red X appears on the map somewhere in its area. The marker is permanent, because it is stored in the map item.

Runs when: the inventory holds a filled map that has no marker from this event.

## `elytra_flicker`

Worn elytra look almost broken for 2 s, and only the player sees it. Under hardcore they are worn down for real.

Runs when: the player wears elytra with durability to spare, 12 or more blocks above the ground.

## `stew_bowl`

**Harmful.**
One stew becomes an empty bowl.

Runs when: the inventory holds mushroom, beetroot, rabbit or suspicious stew.

## `bucket_pufferfish`

A water bucket looks like a pufferfish bucket for 5 s, and only the player sees it. Under hardcore it is swapped for real.

Runs when: the inventory holds a water bucket.

## `torch_burn`

**Harmful.**
Half of a stack of torches is gone, but at least one is left.

Runs when: the inventory holds a stack of 2 or more torches or soul torches.

## `bundle_bone`

A bone is added to the bundle's contents, and nothing is taken out.

Runs when: the player has a bundle with room for a bone.

## `shield_ghost`

The sound of a shield blocking a hit is heard, though nothing is there.

Runs when: a shield is in either hand.

## `item_throwback`

A dropped item is thrown at the player, slowly enough to be dodged.

Runs when: a dropped item lies 3–12 blocks away.

## `phantom_horn`

The goat horn's own tune sounds at the player.

Runs when: the inventory holds a goat horn with a tune.

## `armor_dye`

A piece of leather armor is dyed black, and its old colour returns after 5 min. Under hardcore it stays black.

Runs when: the player wears or carries a piece of leather armor that is not black.

## `item_glint`

An item glints as if it were enchanted for 10 s.

Runs when: the player has an item that does not already glint.

## `item_lore`

A lore line such as "it remembers you" is added to the item and removed after 5 min. Under hardcore it stays.

Runs when: the player has any item in the inventory or worn.

## `tool_mirage`

**Harmful.**
A tool shows full durability, then almost broken, and then the truth, and only the player sees it. Under hardcore it is worn down for real.

Runs when: the player has a damageable tool with durability to spare.

## `lodestone_lure`

A lodestone compass points at the anchor of the nearby spirit for 5 min. Under hardcore it stays that way.

Runs when: the inventory holds a compass with a lodestone target, and the player is within the reach of a territorial spirit.

## `ghost_author`

A written book signed with the player's own name turns up, with one creepy page.

Runs when: the inventory has a free slot and holds no book signed by the player yet.

## `potion_swap`

A potion becomes another one for 5 min. Under hardcore it stays that way.

Runs when: the player has a potion, splash potion or lingering potion that has a base potion and no custom effects.

## `vanishing_arrows`

**Harmful.**
Up to 6 arrows leave the inventory one by one and come back after 3 min. Under hardcore they stay gone.

Runs when: the inventory holds an arrow.

## `crossbow_misfire`

**Harmful.**
A loaded crossbow fires straight up with reduced power.

Runs when: a loaded crossbow is in either hand.

## `map_elsewhere`

The map in the hand shows the area around the anchor of the nearby spirit for 5 min. Under hardcore it stays that way.

Runs when: a map is in either hand, and the player is within the reach of a territorial spirit.

## `shy_item`

One dropped item slides away from the player.

Runs when: a dropped item lies within 5 blocks.

## `orb_scatter`

Experience orbs fly away from the player.

Runs when: an experience orb is within 8 blocks.

## `hand_drop`

The item in the main hand falls out of it, as if the player had dropped it.

Runs when: the main hand holds something.

## `inventory_drop`

A stack from the inventory, other than the one in hand, falls out of it.

Runs when: the inventory holds something besides the held item.

## `line_pulls`

**Harmful.**
The player is shoved toward the bobber, and a splash appears there.

Runs when: the player's bobber floats in water.

## `odd_catch`

A player head in the player's own likeness, a bone, a skeleton skull or rotten flesh is thrown out of the water at the player, as a real catch is. If it is not picked up, it vanishes after 1 min.

Runs when: the player's bobber floats in water.

## `allay_thief`

Only this player's client is told of an allay at the player's side, holding a copy of one of their stacks. It flies 45 blocks straight away and is gone. Nothing is taken, and nothing exists on the server.

Runs when: the player carries anything.

## `holy_water_cloudy`

**Harmful.**
Every holy water stack gets a "cloudy" mark and a lore line. The altar refuses such water until the next dawn, when the mark is removed, and nothing else undoes the change.

Runs when: the inventory holds clear holy water.

## `mirror_names`

A soft chime marks the mirror. The next time a mirror is placed on the altar, the name of a spirit around the player is shown in chat, once. The mark lapses after 30 min.

Runs when: the inventory holds a mirror, a spirit senses the player, and no mirror is marked already.

## `pearl_strays`

For 5 min, the first pearl the player throws is moved above standing room 8 to 24 blocks away and dropped there, so the player lands in that spot (with the usual 5 damage).

Runs when: the inventory holds an ender pearl.

## `whispering_item`

The item in the player's hand gains the Curse of the Restless and whispers now and then while it is held. The curse is lifted after 3 min (under hardcore it stays).

Runs when: the player holds an item without the curse.

## `hollow_trim`

A worn piece of armor shows a hollow trim for 2 min (under hardcore it stays).

Runs when: the player wears trimmable armor that has no trim.

## `photo_figure`

A pale, hollow-eyed figure 34–60 px tall is painted into the picture of one developed photo, and the change is kept.

Runs when: the inventory holds a developed photo.
