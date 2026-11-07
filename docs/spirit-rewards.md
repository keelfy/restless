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
| Nix | Drowned Stone | walk the bottom at normal speed, no air used | cannot surface while held |
| Phantom | Borrowed Face | hostile mobs take you for their own | breaks on your first hit; 2 min rest |
| Mimic | Double | a false copy of you draws mobs for 8 s | other players see it too |
| Wraith | Cloak of Dark | light <= 4: quiet steps, mobs notice you only within 6 | weakness in bright light |
| Mara | Borrowed Dream | restores sanity to full | your personal spirit is more active next night |
| Banshee | Death Wail | at < 3 hearts, a scream throws mobs back | one charge, back after a day (24000 ticks) |
| Revenant | Grudge | stores damage taken in 10 s; next hit adds half (cap 10) | must be hurt first |
| Lost one | Way Back | use to mark a point; use again within 5 min to return | same dimension; sanity cost; ash footprints |
| Domovoi | House Key | bound to one enclosed house: spirits do not act inside | breaks if the house opens |
| Hoarder | Stash | on death, one chosen slot stays with you | single use |
| Demon | Bargain | restores all health at once | -4 max hearts for 10 min, and anger rises |
| Trickster | Coin | next mob kill: 50% double drops, else none | pure gamble |

## Order

Phase 1: Phantom, Banshee, Hoarder, Demon, Lost one (they cover condition, charge and cost, and bring the hooks).
Phase 2: the rest. Until then they keep the plain effect charms.

## Status

Phase 1 is built: Borrowed Face, Death Wail, Stash, Bargain, Way Back. Known gaps:

- Way Back leaves no ash footprints (they exist only on ash blocks); its cost is sanity alone.
- Stash: what it saved is held in memory between death and respawn; a server stop in that window loses it. It is spent
  even where `keepInventory` makes it needless.
- Death Wail's charge is a cooldown, so it is not saved across a relog.
