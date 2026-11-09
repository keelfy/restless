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
| Nix | Drowned Stone | no air used in water, Dolphin's Grace and full mining speed under water; water events do not run on the holder | a weight (slowness) on dry ground |
| Phantom | Phantom Double | a false copy of you draws mobs for 8 s | other players see it too; 1 min rest |
| Mimic | Borrowed Face | hostile mobs take you for their own | 25 s; ends on your first hit, or on a chest opened or a block broken under a hostile's eyes; 2 min rest |
| Wraith | Wraith's Breath | 5 s as a shade: blows pass through you, and yours through them; the world (fall, fire, lava) still hurts | costs 0.2 sanity; 90 s rest |
| Mara | Borrowed Dream | restores sanity to full | your spirits grow angrier; once a day (a rest the item keeps, a relog does not clear it) |
| Banshee | Death Wail | anywhere in the inventory: at < 3 hearts, a scream throws mobs back, slowed and blinded for 3 s; keens 3 s before a spirit's assault lands | one charge, back after a day (24000 ticks), saved with the item |
| Revenant | Grudge | stores damage taken in 10 s; next hit adds half (cap 10) | must be hurt first |
| Lost one | Lost Step | slip up to 8 blocks ahead, through a wall of a block or two, never through blocks of the tag `restless:step_proof` (obsidian, iron doors and bars, bedrock...) | costs sanity; 20 s rest |
| Domovoi | House Key | in a shut, roofed room: spirits do not act | wears one use per event stopped (16) |
| Hoarder | Stash | on death, one chosen slot stays with you, even through a server stop | single use; not spent when `keepInventory` is on |
| Demon | Bargain | all health at once, and 30 s of strength II, resistance II and fire proof | -2 max hearts for 10 min, and anger rises |
| Trickster | Coin | next mob kill: 50% a win, else no drops. A win pays x2 and each win in a row one more (x5 at most), drops and experience; a loss resets the run, and a run wears off by one a day | 1 min rest |

## Order

Phase 1: Mimic, Banshee, Hoarder, Demon, Wraith, Lost one (they cover condition, charge and cost, and bring the hooks).
Phase 2: the rest. After a first look the phantom and mimic charms were swapped, the wraith and lost one charms
replaced, and the Bargain given a short fury.

## Status

Both phases are built, all twelve charms. Left out of the plan: the House Key is not bound to one house (any shut,
roofed room does, and a gap in it or its roof stops it) and wears out rather than breaking when the house opens; the
Drowned Stone does not hold the bearer down, its cost is slowness on land. The rebalance of [essence-expansion.md](essence-expansion.md) Part A is built (the table above is as it now is), but for
the House Key's repair with Domovoi's Porridge, which is built with the consumables (see [essence-expansion.md](essence-expansion.md) Part C).

The essences feed more than the twelve charms. Everything built on top is in [essence-expansion.md](essence-expansion.md):
consumables (Part C), advanced charms (Part D, an essence added to a charm), relics (Part E) and the Requiem (Part F),
the finale that takes one essence of each of the fourteen types. The player-facing list is the wiki's Items section
(`apps/frontend/content/{en,ru}/items`) and the rewards table in `spirits.md`.
