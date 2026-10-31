---
title: Items
---

# Items

11 events.

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
