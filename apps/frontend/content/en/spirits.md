---
title: Spirits
---

# Spirits

Every player is haunted by personal spirits. A spirit's type decides which events it favors most, so watching the events tells you who is near you. Besides personal spirits, there are territorial ones, bound to a place.

To tell which one is near you, keep it off or banish it, see [Tools](/en/items/tools).

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

## Meeting a type

Each type shows up in its own way, and the journal's bestiary page says how once you have a photograph of one. A banshee comes when a villager or a pet dies before you, a lost one when you die far from home or go two days awake, a revenant after a slaughter or the killing of your own pet. Territorial spirits are born with structures.

An **effigy** calls any type on purpose: stuff it with one essence of that type and light it. See [Essences](/en/items/essences#the-effigy).

## Rewards

Banishing a spirit at an altar leaves one of its essence, and one more if it had evolved, and one more if its blight had grown to 64 spots; putting its grave right leaves two. Each type's essence makes a charm, an advanced charm and a consumable of its own:

| Spirit | Charm | Consumable |
| --- | --- | --- |
| Poltergeist | [Telekinesis Amulet](/en/items/charms#poltergeist-telekinesis-amulet) | [Restless Knot](/en/items/consumables#poltergeist-restless-knot) |
| Shade | [Dark Eye](/en/items/charms#shade-dark-eye) | [Dusk Salt](/en/items/consumables#shade-dusk-salt) |
| Nix | [Drowned Stone](/en/items/charms#nix-drowned-stone) | [Still Water](/en/items/consumables#nix-still-water) |
| Phantom | [Phantom Double](/en/items/charms#phantom-phantom-double) | [Sighting Mark](/en/items/consumables#phantom-sighting-mark) |
| Mimic | [Borrowed Face](/en/items/charms#mimic-borrowed-face) | [Mimic Clay](/en/items/consumables#mimic-mimic-clay) |
| Wraith | [Wraith Shroud](/en/items/charms#wraith-wraith-shroud) | [Wraith Oil](/en/items/consumables#wraith-wraith-oil) |
| Mara | [Borrowed Dream](/en/items/charms#mara-borrowed-dream) | [Dreamless Draught](/en/items/consumables#mara-dreamless-draught) |
| Banshee | [Death Wail](/en/items/charms#banshee-death-wail) | [Omen Candle](/en/items/consumables#banshee-omen-candle) |
| Revenant | [Grudge](/en/items/charms#revenant-grudge) | [Bloodied Rag](/en/items/consumables#revenant-bloodied-rag) |
| Lost one | [Lost Step](/en/items/charms#lost-one-lost-step) | [Wayfinder's Ash](/en/items/consumables#lost-one-wayfinders-ash) |
| Domovoi | [House Key](/en/items/charms#domovoi-house-key) | [Domovoi's Porridge](/en/items/consumables#domovoi-domovois-porridge) |
| Hoarder | [Stash](/en/items/charms#hoarder-stash) | [Magpie Feather](/en/items/consumables#hoarder-magpie-feather) |
| Demon | [Bargain](/en/items/charms#demon-bargain) | [Infernal Contract](/en/items/consumables#demon-infernal-contract) |
| Trickster | [Trickster's Coin](/en/items/charms#trickster-tricksters-coin) | [Changeling Dust](/en/items/consumables#trickster-changeling-dust) |

Collect one essence of every type in a [Reliquary](/en/items/requiem#the-reliquary) and sing the [Requiem](/en/items/requiem): the last reward releases you from your personal spirits for good, and the Requiem Bell can give it back.

See [Items](/en/items) for how they work, [Advanced charms](/en/items/advanced) for what two essences make, [Relics](/en/items/relics) for what three make, and [Essences](/en/items/essences) for where each type is met.
