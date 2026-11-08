---
title: Spirits
---

# Spirits

Every player is haunted by personal spirits. A spirit's type decides which events it favors most, so watching the events tells you who is near you. Besides personal spirits, there are territorial ones, bound to a place.

## Personal

Personal spirits follow one player.

### Shade

Quiet and oppressive. Snuffs lights, whispers and afflicts, turns violent as its power grows.

- Favors: lights ×3, sounds ×2.5, afflictions ×2, presence ×2
- Avoids: doors ×0.3, items ×0.5
- Activity: 0.8

### Phantom

Likes to be seen, as other players: far-off strangers, a name in the player list, someone pillaring or taking the crops. Seldom hurts.

- Favors: player ×6, presence ×2
- Avoids: doors ×0.5, lights ×0.5, afflictions ×0.5, items ×0.5, assaults ×0.3
- Activity: 1.0

### Mimic

Fakes things: blocks that are not there, strangers, mobs that vanish.

- Favors: blocks ×3, player ×2, presence ×1.5, mobs ×1.5
- Avoids: lights ×0.5, doors ×0.5, assaults ×0.5
- Activity: 1.0

### Wraith

Hunts in the dark: snuffs lights and hurts.

- Favors: assaults ×2.5, lights ×2, afflictions ×1.5, presence ×1.5
- Avoids: doors ×0.5, blocks ×0.5, items ×0.3
- Activity: 2.0 in the dark (`when: dark`), 1 elsewhere

### Mara

Rides the sleeper: afflictions and presence at night, may act when the player uses a bed (`reacts: bed`).

- Favors: afflictions ×3, presence ×2, sounds ×1.5
- Avoids: doors ×0.5, items ×0.3, blocks ×0.3
- Activity: 1.8 at night (`when: night`), 1 by day

### Banshee

A voice: mostly sounds, rarely touches anything.

- Favors: sounds ×5, presence ×1.5
- Avoids: doors ×0.5, items ×0.3, blocks ×0.3, assaults ×0.3
- Activity: 0.9

### Revenant

Vengeful: hurts and sends mobs, may act after the player kills a mob (`reacts: kill`).

- Favors: assaults ×3, mobs ×2.5
- Avoids: lights ×0.5, doors ×0.3, items ×0.3
- Activity: 1.0

### Lost one

Wanders: figures, strangers and steps, may act when the player goes below y 0 (`reacts: deep`).

- Favors: presence ×3, player ×2, sounds ×1.5
- Avoids: blocks ×0.5, items ×0.5, assaults ×0.2
- Activity: 0.9

## Territorial

Territorial spirits stay at one place and act on players nearby. Some are born with the world: a newly generated woodland mansion, shipwreck, ancient city, swamp hut, desert temple, village or ruined portal may hold one, and two such places never lie within 96 blocks of each other.

### Poltergeist

Noisy and physical. Mostly messes with doors and belongings; rarely hurts at first.

- Favors: doors ×4, items ×3, blocks ×1.5
- Avoids: afflictions ×0.5, assaults ×0.5, sounds ×0.5, presence ×0.5
- Activity: 1.3

### Nix

Territorial, bound to water. Keeps swimmers under, calls players to the water, kills fish and breaks boats. Place it by a lake or the sea with `/haunt spirit add nix` while standing there.

- Favors: water ×8
- Avoids: assaults ×0.5, sounds ×0.5, blocks ×0.3, presence ×0.3
- Activity: 1.0

### Domovoi

Territorial house spirit: doors, belongings and household noises, hardly harmful.

- Favors: doors ×2, items ×2, sounds ×1.5
- Avoids: mobs ×0.5, afflictions ×0.3, assaults ×0.2
- Activity: 0.7

### Hoarder

Territorial: takes and moves items.

- Favors: items ×5, blocks ×1.5, doors ×1.5
- Avoids: lights ×0.5, presence ×0.5, assaults ×0.3
- Activity: 1.2

### Demon

Territorial and dangerous: assaults, afflictions and mobs.

- Favors: assaults ×3, afflictions ×2, mobs ×2, lights ×1.5
- Avoids: doors ×0.5, items ×0.5
- Activity: 1.1

### Trickster

Territorial prankster: changes blocks and items, shows things.

- Favors: blocks ×2.5, items ×2, mobs ×1.5, presence ×1.5
- Avoids: assaults ×0.2
- Activity: 1.2

## Rewards

Banishing a spirit at an altar leaves one of its essence; putting its grave right leaves two. Each essence is crafted with one common item into a charm. Charms that work while carried work from the off hand only, so one answers at a time; the Death Wail is the exception and works from anywhere in the inventory.

| Spirit | Essence + | Charm | Does |
| --- | --- | --- | --- |
| Poltergeist | gold ingot, string | Telekinesis Amulet | pulls dropped items and experience to you |
| Shade | ender eye | Dark Eye | night vision; living things glow through walls and the spirits around you are marked by columns of soul fire |
| Nix | nautilus shell | Drowned Stone | in the off hand: endless breath, Dolphin's Grace and full mining speed in water, and water spirits' events do not run on you; but a weight (slowness) on dry ground |
| Phantom | phantom membrane | Phantom Double | a false copy of you stands for 8 seconds and hostiles within 16 blocks go for it; other players see it too (1 min rest) |
| Mimic | leather | Borrowed Face | hostile mobs ignore you for 25 seconds, until you strike, or until you open a chest or break a block under a hostile's eyes (2 min rest) |
| Wraith | iron ingot | Wraith Shroud | 5 seconds as a shade: blows pass through you and yours through them, but a fall or fire still hurts; costs sanity (90 s rest) |
| Mara | honey bottle | Borrowed Dream | sanity to full; your spirits grow angrier (once a day, even across a relog) |
| Banshee | bell | Death Wail | anywhere in your inventory: below 3 hearts it throws hostiles back, slowed and blinded, once a day; it keens 3 seconds before a spirit's assault |
| Revenant | iron sword | Grudge | in the off hand: stores the damage you take for 10 seconds; your next blow adds half of it, up to 10 |
| Lost one | lantern | Lost Step | slip up to 8 blocks ahead, through a wall of a block or two, but not through obsidian, iron doors or bars, or bedrock; costs sanity (20 s rest) |
| Domovoi | campfire | House Key | in the off hand, inside a shut, roofed room: spirits' events do not run; each one stopped wears the key (16 uses) |
| Hoarder | emerald | Stash | holds a stack; in the off hand when you die, the stack stays with you, even through a server stop, and the stash is spent (not with `keepInventory`) |
| Demon | blaze powder | Bargain | all health at once and 30 seconds of strength II, resistance II and fire proof; then 10 minutes with two hearts fewer and angrier spirits |
| Trickster | gold nugget | Trickster's Coin | wagers your next mob kill: its drops and experience are doubled or lost, evenly; each win in a row pays one more (up to x5), a loss starts over |
