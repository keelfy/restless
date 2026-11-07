# Spirit rewards

Banishing a spirit at an altar leaves one essence; putting its grave right leaves two. An essence is crafted with one
common item into that spirit's charm.

## Rules (the balance)

1. Passive charms work only from the **off hand**, so one is active at a time. Used charms (right click) rest after.
2. Every effect is situational, costs a charge, or costs something the mod already tracks (sanity, anger).
3. No long "switch on and forget" buffs.

## Charms

| Spirit | Charm | Effect | Limit / cost |
|---|---|---|---|
| Nix | Drowned Stone | no air used in water | a weight (slowness) on dry ground |
| Phantom | Borrowed Face | hostile mobs take you for their own | breaks on your first hit; 2 min rest |
| Mimic | Double | a false copy of you draws mobs for 8 s | other players see it too; 1 min rest |
| Wraith | Cloak of Dark | light <= 4: mobs notice you only within 6 | weakness in bright light |
| Mara | Borrowed Dream | restores sanity to full | your spirits grow angrier; once a day |
| Banshee | Death Wail | at < 3 hearts, a scream throws mobs back | one charge, back after a day (24000 ticks) |
| Revenant | Grudge | stores damage taken in 10 s; next hit adds half (cap 10) | must be hurt first |
| Lost one | Way Back | use to mark a point; use again within 5 min to return | same dimension; sanity cost; ash footprints |
| Domovoi | House Key | in a shut, roofed room: spirits do not act | wears one use per event stopped (10) |
| Hoarder | Stash | on death, one chosen slot stays with you | single use |
| Demon | Bargain | restores all health at once | -4 max hearts for 10 min, and anger rises |
| Trickster | Coin | next mob kill: 50% double drops, else none | pure gamble; 1 min rest |

## Order

Phase 1: Phantom, Banshee, Hoarder, Demon, Lost one (they cover condition, charge and cost, and bring the hooks).
Phase 2: the rest.

## Status

Both phases are built, all twelve charms. Phase 2 left out: the Cloak of Dark has no quiet steps; the House Key is not
bound to one house (any shut, roofed room does, and a gap in it or its roof stops it) and wears out rather than breaking
when the house opens; the Drowned Stone does not hold the bearer down, its cost is slowness on land. Known gaps:

- Way Back leaves no ash footprints (they exist only on ash blocks); its cost is sanity alone.
- Stash: what it saved is held in memory between death and respawn; a server stop in that window loses it. It is spent
  even where `keepInventory` makes it needless.
- Death Wail's charge is a cooldown, so it is not saved across a relog.
