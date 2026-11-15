---
title: Relics
---

# Relics

These are the rarest things I know of. A relic is made of the essences of **three different types** that share a theme, and of one rare find from the late game. They are crafted in a crafting grid, in a shape, and each is one of a kind: the world has no second recipe for it, and nothing here is spent.

| Relic | Essences | Rare finds |
| --- | --- | --- |
| [Hollow Lantern](#hollow-lantern) | shade, banshee, domovoi | Hallowed Lantern, 2 echo shards |
| [Ferryman's Coin](#ferrymans-coin) | lost one, nix, trickster | totem of undying, gold block |
| [Exorcist's Censer](#exorcists-censer) | revenant, demon, wraith | soul lantern, blaze rod, 2 iron chains |
| [Hoarder's Hollow](#hoarders-hollow) | hoarder, poltergeist, mimic | ender chest, shulker shell |
| [Mara's Cradle](#maras-cradle) | mara, phantom, lost one | any bed, creaking heart |

## Hollow Lantern

A lantern that wards a wide place and teaches while it does.

- **Craft:** shade essence (S), banshee essence (B), domovoi essence (D), 2 echo shards (E), Hallowed Lantern (L). Grids are read top row first:

```
S B D
E L E
```

- **Ward:** like the [Hallowed Lantern](/en/items/tools#hallowed-lantern), no spirit's event runs on a player within **24 blocks**, four times the reach of the old one. The event is lost, not put off.
- **Clue:** each event it stops writes one habit of a spirit nearby into the journal, in the words the ouija board uses. Protection that teaches.
- **One to a chunk.** A second one cannot be placed in the same chunk.
- It does not add to a Hallowed Lantern's ward: near both, a player is warded once, and the hollow one still leaves its clue.

## Ferryman's Coin

Craft: lost one essence (L), nix essence (N), trickster essence (T), totem of undying (U), gold block (G):

```
L N T
. U .
. G .
```

The coin is worn in the **charm slot** with Curios, or carried in the **off hand** without. When a blow would kill the wearer, they **fade** instead:

- They come back at half their health, with all the effects on them gone and the fire put out, and their sanity falls to 0 at once.
- For 10 seconds the wearer is faded: invisible, nothing can hurt them, they can hurt nothing, they cannot use blocks, and hostile mobs lose track of them. They are not moved: they fade where they fell, with all their things.
- The coin then **rests for 3 days**. The rest is saved with it, so a relog does not skip it.
- A totem of undying is used first. The void and `/kill` are not turned.

## Exorcist's Censer

Craft: revenant essence (R), demon essence (D), wraith essence (W), 2 iron chains (C), soul lantern (L), blaze rod (B):

```
R D W
C L C
. B .
```

When the censer is held in the **off hand** as a player offers at an [altar](/en/items/tools#altar), the ritual changes:

- The fury runs **2 waves** instead of 6, so the ritual takes 8 waves instead of 12. A spirit that has evolved still adds its 4, and a form it does not take still adds 6.
- The spirit cannot snuff the candles.
- The spirit's fury in the ritual is made of harmful events only.
- The banished spirit leaves **one more essence**.
- The spirit's anger rises by 0.1 each time it is used.

## Hoarder's Hollow

A block that is a chest, and looks like a plain one until it is given a face.

- **Craft:** hoarder essence (H), poltergeist essence (P), mimic essence (M), ender chest (C), shulker shell (S):

```
H P M
. C .
. S .
```

- **Store:** when used, it opens a store of 27 slots. It is **one store per player**, the same in every hollow they have, like an ender chest. It is kept in the saved data and not dropped when the block is broken, so nothing is lost by moving it.
- **Disguise:** when [Mimic Clay](/en/items/consumables#mimic-mimic-clay) that has taken a look is used on it sneaking, it draws as that block from then on. Breaking it gives the clay back, still pressed.
- The [Dark Lens](/en/items/charms#shade-dark-lens) shows it as it is.
- **Theft:** when a hoarder's event on a player's belongings runs, there is a 1 in 5 chance that it also takes one stack from their hollows.

## Mara's Cradle

Craft: mara essence (M), phantom essence (P), lost one essence (L), any bed (B), creaking heart (H):

```
M P L
. B .
. H .
```

A bed of two blocks, set like a bed and slept in like one (it asks for the same dark and the same quiet). It also sets the sleeper's spawn point.

- The sleeper wakes with **all their sanity**.
- **The night does not pass** when everyone asleep is in a cradle. The sleeper wakes in the same night, rested, and the world has not moved. If anyone sleeps in an ordinary bed, the night goes as it does with any bed.
- Mara may come: if one haunts the sleeper, once in four times she acts on them a few seconds after they lie down, with a light event.
- **The spawn holds once.** If the cradle is broken, the next time the player comes back from death they return where it stood, and not to the world's spawn. Then it is gone.
