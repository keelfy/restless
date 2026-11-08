# Essence expansion

What spirit essences become after the first twelve charms (see [spirit-rewards.md](spirit-rewards.md)): a rebalance of
the charms, a working essence economy, a consumable per spirit, advanced charms and relics made of several essences,
and the **Requiem**, the finale that lets the player go. Phases 0 to 4 are built (see the notes at their heads); the rest is not. Numbers are
starting values, all in config.

## Goals

1. **Every essence has a use.** The second essence of a grave, the third banishing of a type: each one buys something.
2. **Every spirit is worth meeting.** No type gives a dead reward; the hardest to meet gives a reward worth the trip.
3. **Rewards stay inside the haunt.** A reward costs sanity or anger, works against or with spirits, or needs a clue
   the player learned. A plain vanilla buff in a new skin is not a reward.
4. **A goal at the end.** Collecting all fourteen types is the long arc; the Requiem closes it.

## Rules (extend the charm rules)

1. Charms are permanent, consumables are spent, relics are permanent and rare. One spirit, three tiers of answer.
2. Passive charms work from the off hand, unless a charm says it works from anywhere in the inventory.
3. Long rests (an hour or more) are **saved with the item**, not kept as a vanilla cooldown, so a relog does not reset
   them (today a relog resets Borrowed Dream and Death Wail).
4. An upgrade consumes the base charm and keeps what it holds (a stash's stack, a key's wear).
5. Nothing makes a spirit unbanishable or a ward useless; nothing makes a player untouchable for more than a few
   seconds.

---

## Part A. Rebalance of the twelve charms

Twelve charms plus Telekinesis and Dark Eye: fourteen items. Changes, by priority.

### Too strong

| Charm | Today | Change | Why |
|---|---|---|---|
| Wraith Shroud | 6 s: every damage without `bypasses_invulnerability` is cancelled (fall, lava, fire, drowning, suffocation too); 60 s rest; 0.15 sanity | Only blows pass through: damage with a causing or direct entity. The world still hurts. 5 s; 90 s rest; 0.2 sanity | A free totem every minute. A shade passes through bodies, not through lava |
| Borrowed Face | 60 s unseen, 120 s rest | 25 s unseen, 120 s rest; also ends when the holder opens a container or breaks a block within 6 blocks of a hostile that would see them | Half of all time spent unseen trivializes nights and caves |
| Lost Step | Passes any wall of 1–2 blocks | Stops at blocks in the tag `restless:step_proof` (obsidian, crying obsidian, reinforced deepslate, iron door, iron bars, iron trapdoor, bedrock, barrier by default) | On a server it opens any base; a builder must be able to answer it |

### Too weak

| Charm | Today | Change | Why |
|---|---|---|---|
| Death Wail | Off hand; below 3 hearts throws hostiles back, once a day | **Works from anywhere in the inventory.** Keeps the scream, and adds Slowness III and Darkness for 3 s on those thrown. New passive: **honest warning**. 3 s before an `ASSAULT` event lands on the carrier, a low keening plays from the spirit's position. Rest saved with the item | The banshee is the honest warner of the design. Today the charm loses to the totem for the off hand at every turn |
| Drowned Stone | Off hand: water breathing in water, slowness on land | In water also: Dolphin's Grace, full submerged mining speed (`submerged_mining_speed` attribute modifier), and **a nix does not drown its own**: `WATER` events do not run on the holder. Slowness on land stays | Beats the turtle shell and stands next to the conduit. It also counters the nix it came from |
| Trickster's Coin | Next kill: double or nothing at even odds, which is EV neutral | **Push your luck.** The coin keeps a streak (a data component). A win pays ×(2 + streak) drops and xp and raises the streak. A loss takes the drops and resets it. Streak cap 3 (×5). Not tossing keeps the streak; it decays by 1 per in-game day unused | An even bet is no choice. A streak makes "stop or go on" the decision, and the target matters: a nether star at streak 2 |

### Fine, small fixes

| Charm | Change |
|---|---|
| Telekinesis Amulet | Also pulls experience orbs; skips items a hopper would not see, such as items in lava |
| Dark Eye | Also marks the **roaming position** of each spirit within its radius (`Location.roaming`) with a short particle column, so the eye is a detection tool and not just night vision |
| Stash | What it saved on death goes into `HauntData` (saved), not `Session` memory, so a server stop no longer loses it. Not spent when `keepInventory` is on |
| House Key | 10 → 16 uses; repaired in an anvil with Domovoi's Porridge (Part C) |
| Borrowed Dream | Rest saved with the item (rule 3) |
| Phantom Double, Grudge, Bargain | No change |

---

## Part B. Essence economy

### Sources

| Source | Essences | Note |
|---|---|---|
| Banishing at the altar | 1 | as today |
| Grave put right | 2 | as today |
| Banishing an **evolved** spirit | +1 | its ritual is longer (`evolution.extra_waves`); the reward should say so |
| Cleansing a territorial spirit whose blight covers ≥ 64 spots | +1 | rewards fighting a spirit that was left to spread |

### Sinks

| Sink | Cost |
|---|---|
| Charm | 1 essence of its type |
| Consumable | 1 essence → 2–8 uses (Part C) |
| Advanced charm | base charm + 1–2 essences of other types (Part D) |
| Relic | 3 essences of different types + rare items (Part E) |
| Reliquary (Requiem) | 1 of each of the 14 types (Part F) |
| Lure | 1 essence |

### The Lure: meeting a spirit on purpose

**Effigy** (block): a scarecrow stuffed with one essence and a candle. It burns down over a night and calls a spirit of
that type:

- personal type: born on the player who lit it, through `BirthRules`. The cooldown and `max_personal` still apply. Start
  anger 0.5 and a short grace, as a deed birth;
- territorial type: anchored at the effigy, radius as for a structure, `structure_spacing` still applies.

This makes farming a type a choice of risk (design stage 3 had it planned). It also answers the worst gap: a type the
player cannot meet without a grim deed.

### Fairer births

| Type | Today | Added |
|---|---|---|
| Banshee | only `pet_lost` (a tamed pet dies away from its owner) | deed `witness_death`: a villager or tamed animal dies within 16 blocks of the player, not by their hand, chance 0.2 |
| Lost one | `far_death` | deed `lost_long`: 2 in-game days without sleeping in a bed or setting a spawn point, chance 0.3 |
| Revenant | `slaughter`, `pet_slain` | no change |

The journal gets one line per type on **where it is met**: structure, deed or effigy. It is shown once the type has been
identified once. Without these lines the finale is a wiki hunt.

---

## Part C. Consumables, one per spirit

One essence and a few common items give several uses. Each one is thematic, does something vanilla cannot do in the
same way, and most of them touch the haunt.

| Spirit | Consumable | Recipe → yield | Effect | Limits |
|---|---|---|---|---|
| Poltergeist | **Restless Knot** | essence + string + 2 chain → 4 | Used on a container: for 10 min, items dropped within 8 blocks fly into it. The poltergeist's wireless hopper | One knot per container; a full container lets items fall |
| Shade | **Dusk Salt** | essence + salt + coal → 4 | Scattered on a block: no hostile mob spawns naturally within 12 blocks for one in-game day. A dark room stays safe to build in | Spawners and events still spawn; shown by faint dark motes |
| Nix | **Still Water** | essence + water bottle + prismarine shard → 2 | Drunk: clears rain and thunder now and keeps clear for one in-game day. Sneak-drunk: calls a storm for 10 min (for channeling and fishing) | Overworld only |
| Phantom | **Sighting Mark** | essence + paper + glowstone dust → 4 | Placed: a pale figure only players see marks the spot through walls up to 256 blocks away for 3 in-game days. Named by an anvil-renamed mark | Max 8 per player; the figure is cosmetic and never targeted |
| Mimic | **Mimic Clay** | essence + 4 clay balls → 8 | Used on a block: copies its look. Placed: a block that looks like the copy, with the copy's collision shape kept full. Hides chests, doors and redstone | Not a container itself; breaks like the copied block; shows its true face to the Dark Eye |
| Wraith | **Wraith Oil** | essence + glass bottle + ink sac → 2 | Applied to a weapon: 30 hits that, in light 7 or less, deal +50% and ignore armor (as `bypasses_armor`) | Charges shown on the item; no effect in light |
| Mara | **Dreamless Draught** | essence + honey bottle + poppy → 3 | Drunk before bed: sleep is allowed with monsters nearby, and the drinker counts as all players for `playersSleepingPercentage`, so the night is skipped. They wake with +0.3 sanity, and no Mara event runs that night | Server config `draught_skips_alone` (true by default) |
| Banshee | **Omen Candle** | essence + candle + bone meal → 4 | Placed and lit: for one in-game day, any `HARMFUL` tier event aimed at a player within 16 blocks is announced by the candle's wail 5 s before it runs | Does not stop the event; snuffed by `LIGHTS` events like any candle |
| Revenant | **Bloodied Rag** | essence + 2 rotten flesh + red dye → 4 | Thrown at a mob: for 15 s, every mob of its type within 24 blocks targets it | Does nothing on bosses; ends early if the mob dies |
| Lost one | **Wayfinder's Ash** | essence + ash + bone → 4 | Used: a trail of footprints (the ash footprint entity) leads to the last death spot for 3 min. Sneak-used: to the respawn point | One trail at a time |
| Domovoi | **Domovoi's Porridge** | essence + bowl + wheat + milk bucket (bucket returned) → 4 | Placed in a shut, roofed room (`Wards.housed`): for one in-game day, crops, saplings and young animals within 16 blocks get 3× random ticks and growth, and breeding rests half as long. Also repairs a House Key in an anvil (+8 uses) | One bowl per room; eaten by the domovoi at the end with a small sound |
| Hoarder | **Magpie Feather** | essence + feather + gold nugget → 4 | Used: for 2 min, points (a particle stream) to the nearest unopened loot container within 128 blocks: a chest, barrel, decorated pot or vault with a loot table | Only containers whose loot was never rolled |
| Demon | **Infernal Contract** | essence + paper + blaze powder → 1 | Crafted with an enchanted item: one enchantment goes **one level past its maximum** (Sharpness VI). The item gains Curse of Vanishing, and the crafter's spirits gain 0.3 anger | Once per item; a book cannot take it |
| Trickster | **Changeling Dust** | essence + gold nugget + redstone → 4 | Used on a villager who has never traded: rerolls their trades. Used on a block: turns it into a random block of the same family (wool colors, log kinds, stone variants, terracotta) | Ores, spawners and block entities are excluded |

---

## Part D. Advanced charms (base charm + other essences)

Each one consumes a base charm and adds new behaviour, not just bigger numbers. Shapeless recipes through one custom
recipe class (`UpgradeRecipe`, the same seam as `RelightingRecipe`) that copies the base's components.

| Advanced charm | Recipe | Effect | Cost / limit |
|---|---|---|---|
| **Fetch's Path** | Lost Step + phantom essence | The step leaves a Phantom Double at the start for 4 s; hostiles within 16 go for it | Lost Step cost + 0.05 sanity; 25 s rest |
| **Changeling Mask** | Borrowed Face + trickster essence | While unseen, the first blow does not break the face: the struck mob is **framed**, and the nearest hostile turns on it. A second blow breaks it | Same rest as Borrowed Face |
| **Nightwalk Shroud** | Wraith Shroud + shade essence | A toggle: while on and in light 4 or less, the holder passes through hostiles and they lose track of them. Light above 4 ends it | 0.01 sanity per second while on; no blows pass |
| **Blood Debt** | Grudge + demon essence | No cap on the stored damage. If the released blow kills, the holder heals the stored amount | Each release costs 0.05 sanity; window stays 10 s |
| **Requital Wail** | Death Wail + revenant essence | Those thrown back take +50% damage for 10 s and glow | Keeps the daily rest |
| **Tidecaller** | Drowned Stone + lost one essence | Use in water or rain: a riptide-like dash without a trident. Off hand: the nix's call no longer pulls the holder | 3 s rest; no dash on dry ground |
| **Magpie's Hook** | Telekinesis Amulet + hoarder essence | Off hand: items within 6 blocks come to the holder on their own. Sneak-use on a container links it, and items that do not fit go there, within 64 blocks and in the same dimension | The link breaks if the container is broken |
| **Truthseer's Eye** | Dark Eye + banshee essence | Once per in-game day, the next clue the holder reads cannot lie (`lie_chance` 0 for that reveal) | Rest saved with the item |
| **Hearth Dream** | Borrowed Dream + domovoi essence | Sanity to full **without** anger, but only inside a shut, roofed room | Once a day, saved |
| **Keeper's Key** | House Key + hoarder essence | Bound to the first room it shelters; there it never wears. Elsewhere it does nothing | Rebind by crafting it with a campfire |
| **Grave Satchel** | Stash + lost one essence | Holds 3 stacks; they stay with the player on death. Not spent: rests 3 in-game days after it triggers | Saved in `HauntData` like the Stash |
| **Devil's Coin** | Trickster's Coin + demon essence | The wager covers every kill for 30 s, not just one. Each kill settles on its own at the streak's odds | A loss ends the window and the streak; 0.1 anger per toss |
| **Twin's Mirror** | Phantom Double + mimic essence | The double can be placed where the holder looks (up to 16 blocks) and copies the holder's held item and armor | Same rest |
| **Black Bargain** | Bargain + wraith essence | The fury lasts 45 s. The debt is paid in sanity (−0.4 at once) instead of max health | 10 min rest; anger as Bargain |

Fourteen advanced charms, one per base charm. Every type's essence is used at least once as an addition.

---

## Part E. Relics (three essences)

Permanent, one per player in effect, made of three types with one theme. Each needs a rare vanilla item, so relics
belong to the late game.

| Relic | Recipe | Effect | Cost / limit |
|---|---|---|---|
| **Hollow Lantern** (block) | shade + banshee + domovoi essence + Hallowed Lantern + echo shard | A ward of radius 24 (the lantern's ×4 area). An event it blocks **leaves a clue**: the blocked spirit's trait is written to the journal, as from the spirit box. Protection that teaches | One per chunk; does not stack with Hallowed Lanterns |
| **Ferryman's Coin** | lost one + nix + trickster essence + nautilus shell + gold block | On a lethal blow (not the void), the holder fades instead of dying. 10 s later they come back where they fell with half health and their items, sanity set to 0 | Off hand; rests 3 in-game days, saved. A totem outranks it, so the totem is used first |
| **Exorcist's Censer** | revenant + demon + wraith essence + soul lantern + blaze rod | Held during a ritual: 8 waves instead of 12, and candles within the circle cannot be snuffed. The spirit's fury events in the ritual are of `HARMFUL` tier only | The banished spirit drops +1 essence only if no candle went out; anger 0.1 per use |
| **Hoarder's Hollow** (block) | hoarder + poltergeist + mimic essence + ender chest + shulker shell | A chest that looks like any block (Mimic Clay) and shares one inventory per player across all hollows, like an ender chest that hides | The Dark Eye shows it; one hoarder event in 5 that targets items may steal one stack from it |
| **Mara's Cradle** (block) | mara + phantom + lost one essence + bed + amethyst block | A bed: sleeping in it skips the night for its owner alone (owner's time-of-day sanity, no world skip), and the owner wakes with full sanity. The spawn point it sets survives a broken bed once | A Mara event runs on the sleeper 1 night in 4, always light tier |

---

## Part F. The Requiem (the finale)

### What it is

A spirit is a dead person with unfinished business. The Requiem is the player's own unfinished business: they carry one
essence of every type to the altar and sing the dead out. A player who completes it is **released**: no spirit is born
on them or acts on them again. They keep their charms and their knowledge. Released is a state, not an item, so it
cannot be lost by dying, and it can be given back.

### The Reliquary

- Crafted early, from the altar's materials (an amethyst shard, a gold block, salt, a book), as soon as the `ESSENCE`
  chapter is open.
- It holds **one essence of each of the 14 types**, one slot per type, and refuses duplicates. It is the visible
  progress: the tooltip lists the types held and greys out the missing ones, with each missing one's "where met" line
  once that type has been identified.
- When full, it glows. It also needs every type identified in the journal (a dossier per type). An essence from a type
  that was never identified is refused, so the type has to have been met and named.

### The ritual

Offered at an altar with a full salt ring, **14 lit candles** (radius 4) and a bell within 8 blocks. It lasts about
6 minutes, in 14 movements, one per type, in the order the essences were added. Each movement is a short test of the
counter the player learned against that type. The candles are the life bar: one candle out per failed movement, and the
Requiem fails when fewer than 7 are lit at the end.

| Type | Movement (about 25 s) | Passed by |
|---|---|---|
| Shade | The light dies around the altar | at least 4 light sources still lit at the end (Hallowed Lanterns count) |
| Poltergeist | Items fly from the player's inventory | picking all of them back up (Telekinesis helps) |
| Nix | Water rises in the circle | not drowning, and keeping the altar dry (Drowned Stone helps) |
| Phantom | Six strangers stand outside the ring | not looking at any for the whole movement |
| Mimic | Two altars appear | putting holy water on the real one |
| Wraith | Darkness, blows from nowhere | surviving above 3 hearts; light under 4 doubles the damage |
| Mara | The player falls asleep standing | waking before it ends: sanity above 0.3 when it starts |
| Banshee | A wail, then silence | ringing the bell during the silence (the honest warning is the cue) |
| Revenant | Mobs from the player's past kills rise | killing them all |
| Lost one | The player is moved 30 blocks away | walking back into the ring in time (Lost Step and Wayfinder's Ash help) |
| Domovoi | Doors and chests near the altar slam | closing every door within 8 blocks |
| Hoarder | The offering is stolen | finding it in one of 3 nearby chests (the Magpie Feather points) |
| Demon | A bargain is offered in chat | refusing it (accepting skips the movement but costs 2 candles) |
| Trickster | The candles swap places and colors | relighting the ones it snuffed, not the decoys |

Charms and consumables are fair to use: a movement that one of them helps is the point of having collected it.

**Failure:** the Reliquary keeps its essences except one at random, every personal spirit gains 0.3 anger, and the
altar refuses a Requiem for 3 in-game days.

**Success:** the player's personal spirits are laid to rest. Each one leaves a grave marker that names it, and its
journal dossier is closed with a last line. The player becomes **released**, the Reliquary becomes the **Requiem Bell**,
an advancement is granted, and a short closing text screen plays (the Requiem poem, en and ru, written for the mod and
not the vanilla end poem).

### Released

| System | For a released player |
|---|---|
| Births (`Births.deed`, effigies, `/haunt spirit add` without a force flag) | nothing is born |
| Personal spirits | none; existing ones were laid to rest |
| Territorial spirits | do not act on them; still act on others, still blight the land, can still be banished by them (and drop essence) |
| Sanity | held at full, the overlay off |
| Anger costs (Bargain, Dream, Contract) | still paid by territorial spirits nearby, if any |
| Charms | all work |
| Journal | a closing chapter; the dossiers stay readable |

**Going back:** sneak-ringing the Requiem Bell for 5 s gives the release back. Three fresh personal spirits are born at
start anger. The Bell stays, and a second Requiem needs a new Reliquary. Other players are never affected: the Requiem
is personal (`requiem.shared` stays false; a shared variant is an open question).

### Relation to the roadmap

The Requiem closes the loop of design stages 1–5. Stage 7 (the Other Side) stays a possible later endgame. When it
exists, a config option can gate the Requiem behind its first boss; until then the Requiem does not wait on it.

---

## Implementation plan

Order matters: shared seams first, so every later phase reuses them. Each phase leaves the mod playable and ends with
GameTests (`neoforge/src/gameTest`) and, for pure logic, unit tests (`core/src/test`).

### Phase 0. Seams

**Built:** the essence map, `Rest` (+ `rest_until`), the saved `kept` stack in `HauntData`, config values and their migration (haunt version 4). **Left to the phase that needs it:** `charges`, the `released` and `requiemWaryUntil` fields, the `consumables`/`advanced`/`relics`/`requiem`/`lure` config sections, `UpgradeRecipe` and the new Platform hooks. Each lands with its first user, so none is dead code meanwhile.

| Seam | Where | What |
|---|---|---|
| Essence lookup by type | `Content` | Replace `POLTERGEIST_ESSENCE`, `SHADE_ESSENCE` and the `ESSENCES` list with one `Map<String, Supplier<Item>> ESSENCE` keyed by spirit id. `Ritual` and `Graves` look up the essence through it instead of building the id by hand. The Reliquary and effigy need it |
| Saved rests | new `content/Rest.java` + data component `rest_until` (game time) | `Rest.ready(stack, level)`, `Rest.start(stack, level, ticks)`; also sets the vanilla cooldown for the client bar. Used by Dream, Wail, Truthseer, Hearth Dream, Satchel, Ferryman, Requiem |
| Charges | data component `charges` (int) | Wraith Oil, Death Wail if it ever needs charges; shown in the tooltip |
| Saved player state | `HauntData` per player record | Fields: `kept` (stash/satchel stacks), `released` (bool), `requiemWaryUntil`, coin `streak` stays on the item. Optional codec fields with defaults, so old saves load |
| Config | `restless-config/haunt.json` + `Migrations` | New sections `consumables`, `advanced`, `relics`, `requiem`, `lure`; bump `version`, migrate old configs by adding defaults |
| Upgrade recipe | `content/UpgradeRecipe.java` (pattern of `RelightingRecipe`) | Shapeless; result copies the base charm's components |
| Platform hooks | `platform/Platform` + NeoForge impl | **New**: natural spawn check (Dusk Salt), sleep check (Dreamless Draught), crop growth (Porridge), the lethal blow before totems (Ferryman's Coin). Existing hooks (`onHurt`, `onKill`, `onDrops`, `onTarget`, `onUseBlock`, `onRespawn`) cover the rest |

Done when: an old world loads, essences are found by type, a saved rest survives a relog (GameTest: start a rest,
re-create the player, check it), and the stash's stack survives a server stop between death and respawn.

### Phase 1. Rebalance (Part A)

**Built**, with one gap: the House Key's anvil repair with Domovoi's Porridge waits for Phase 3. The coin's run is a `Luck` data component, the charms that rest long use `Rest`, and `Director.fireUnlessWarded` now defers an assault that a carried wail has warned of.

| Item | Files |
|---|---|
| Wraith Shroud | `Charms.hurt`: the shade check only when `source.getEntity()` or `getDirectEntity()` is not null; config numbers |
| Borrowed Face | config; `Restless.onUseBlock` / break hook ends the face near a hostile |
| Lost Step | `LostStep.landing`: stop at a block in `restless:step_proof`; tag json in `data/restless/tags/block` |
| Death Wail | `Charms.hurt`: inventory search instead of off hand; `Rest`; warning hook in `Director.fire` for `ASSAULT` |
| Drowned Stone | `Charms.held`: Dolphin's Grace, attribute modifier; `Wards.stops`: `WATER` events skip the holder |
| Trickster's Coin | `TricksterCoin` + `Charms.drops`: streak component, multiplier, daily decay; pure odds and decay math in a unit-tested helper |
| Telekinesis, Dark Eye, Stash, House Key, Dream | as in the table; Dark Eye reads `Location.roaming` |

Tests: `CharmGameTests` grows one test per change (the Shroud takes fall damage but not a zombie's blow, the Wail
works from the inventory, the step stops at obsidian, a coin streak pays ×3, a WATER event skips the stone's holder).

### Phase 2. Economy (Part B)

**Built:** an evolved spirit and a blight of 64 spots (`ritual.blight_reward_spots`) each add an essence (`Ritual.essences`); the deeds `witness_death` and `lost_long`; the bestiary line "where met" (`journal.restless.met.<kind>`); the Effigy (`content/Effigy.java`, config `effigy`, a scarecrow + candle + salt recipe). The four older deeds (`slaughter`, `far_death`, `pet_slain`, `pet_lost`) had a seam and config but no hooks, so Banshee, Lost one and Revenant could not be met at all; they are hooked now, with the new ones, in `haunt/Deeds.java`. **Differences from the plan:** the effigy is lit with flint and steel or a fire charge (no candle in the block), and it calls its spirit onto the nearest player within `effigy.radius`, not onto whoever lit it (nothing about the lighter is saved); it will not light when the call could not be answered (cooldown, cap, spacing), so no essence is wasted; `lost_long` is the vanilla "time since rest" stat reaching two days, so setting a spawn point does not reset it; the model reuses the scarecrow with other wool and a jack-o'-lantern face when lit, no new art.

- Evolved +1 and blight +1: `Ritual` at the essence drop; `Corruption.size` for the blight check.
- Deeds `witness_death` and `lost_long`: `Births.Deed`, config `births.deeds`, hooks in `Restless.onKill` (nearby
  death) and in a slow tick (`lost_long`, using the player's last sleep or spawn time).
- Effigy block: `content/Effigy.java` (pattern of `WornWard`); burns over a night through `Scheduler`; calls
  `Births`/`Director.add`/`Director.anchor` through `BirthRules`, so the caps hold.
- Journal: a "where met" line per type in each dossier.

Tests: `BirthGameTests` (a witnessed death births a banshee, an effigy respects `max_personal`), `RitualGameTests`
(an evolved spirit drops 2).

### Phase 3. Consumables (Part C)

**Built:** all fourteen, in `content/` (one class each, `Consumables` for the server clock and loader hooks), config section `consumables`, recipes, lang (en, ru), textures. State that must outlast a restart is in `haunt/Spells.java` (a second `SavedData`: knots, dusk salt, the marks each player set); the rest is block state, block scheduled ticks, or entities. **New hooks** (loader side, `RestlessNeoForge`): `FinalizeSpawnEvent` (Dusk Salt), `AnvilUpdateEvent` (Porridge mends the House Key), `CanPlayerSleepEvent` (Dreamless Draught). **Differences from the plan:**

- **Wraith Oil** and **Infernal Contract** are used with the weapon or item in the other hand, not crafted with it (no custom recipe class). The oil's charges are a `charges` component with a tooltip; armor is ignored by asking for as much more damage as the armor is about to take off. The contract raises one enchantment that is at its peak, refuses an item with any enchantment already past its peak (once per item), and a book holds no enchantment of its own.
- **Mimic Clay** drops the clay (with its look) when its block breaks, not the copied block's drops: that would copy any block for the price of one clay. A mimic is drawn by a block entity renderer from the look it holds, is full-collision, breaks as slowly as what it shows, and shows a bare clay face to the Dark Eye (reveal while the eye sees). It can take the look of any block drawn from a model.
- **Sighting Mark** is a glowing entity (a billboard of the mark item), seen as far as the game sends entities: its tracking range is `consumables.mark_range` (10 chunks), not 256 blocks. It is counted per player in `Spells`, takes no blows, and fades after three days.
- **Magpie Feather** scans the loaded chunks only, for any `RandomizableContainer` with a loot table (chests, barrels, decorated pots); vaults are left out.
- **Wayfinder's Ash** lays free `AshFootprint`s (a `free` flag on the entity, no ash needed) ahead of the player toward the last death or, sneaking, the respawn point, one second at a time; the trail ends on arrival or after three minutes, and only one at a time.
- **Dreamless Draught** is an effect that lasts until its drinker has slept deep and woken. The night is skipped by lowering `playersSleepingPercentage` for one tick while the drinker is deep asleep (`draught_skips_alone`), and the drinker's mara does not act meanwhile.
- **Domovoi's Porridge** is a block eaten in eight bites over the day, working through scheduled ticks, so it works as long as its chunk is loaded; it refuses a place that is not a shut, roofed room, or a room another bowl serves.
- **Bloodied Rag** is a thrown entity (`RagEntity`); bosses (the `c:bosses` tag, the wither and the dragon) are exempt. Its work stops if the thrower dies or leaves, like every `Scheduler` task.
- **Changeling Dust** families: wool, carpets, natural logs, planks, terracotta, glazed terracotta, and three tags of the mod (stone variants, concrete, glass). A villager is rerolled only while their experience is 0.
- **Omen Candle** counts as a candle (it is in `minecraft:candles`), so the lights events snuff it; only fire in a hand lights it, and a lit one burns out after a day.

Grouped by technique, simplest first:

1. **Used items, no new hook:** Still Water (weather), Wayfinder's Ash (trail of `AshFootprint`s along a path to the
   target), Magpie Feather (scan of `RandomizableContainerBlockEntity` with a loot table; cap the scan, spread it over
   ticks), Changeling Dust (villager `offers` reset when `villagerXp == 0`; block families as block tags), Bloodied Rag
   (thrown entity; `setTarget` on mobs of the type).
2. **Data on other items:** Wraith Oil (`charges` component on the weapon, read in `Charms.hurt` with the light level),
   Infernal Contract (custom crafting recipe: enchanted item + contract).
3. **Placed blocks:** Omen Candle (block; checked in `Director.fire` before a `HARMFUL` event, schedules the wail),
   Restless Knot (block entity component on the container, ticks item pickup), Domovoi's Porridge (block entity; extra
   random ticks in its area; `Wards.housed` at placement; anvil repair of the key), Dusk Salt (saved area list in
   `HauntData` with expiry; the spawn hook), Sighting Mark (entity seen only by players, no AI, glowing through walls).
4. **Hardest:** Mimic Clay (block + block entity holding the copied `BlockState`, a renderer drawing that state, the
   full collision shape, drops of the copied block) and Dreamless Draught (sleep hook and the sleep-percentage
   override).

Tests: one GameTest per consumable at minimum; Magpie Feather and Dusk Salt with a timing check (the scan stays under
a tick budget).

### Phase 4. Advanced charms (Part D)

**Built:** all fourteen: `UpgradeRecipe` (`restless:upgrade`, shapeless, base + essence, copies the base's components), one item class each in `content/` (the base classes were opened up with small hooks, so an upgrade extends its base), what is more than a few lines in `content/Advanced.java`, config section `advanced`, recipes, lang (en, ru), textures. **Differences from the plan:**

- Nightwalk Shroud: hostiles lose track of the holder (and drop a target), but the holder does not walk through them; vanilla gives no clean no-collision with mobs. The toggle is in memory, so a server stop ends it.
- Keeper's Key: its "room" is the spot where it first sheltered, and what lies within `advanced.keeper_radius` of it while that is a shut, roofed room; not a hash of the room's cells.
- Grave Satchel: at death the whole satchel, resting for `advanced.satchel_rest_ticks`, goes into the same saved `kept` slot the Stash uses, so there is one slot per player and a server stop keeps it.
- Tidecaller: in the off hand it is a Drowned Stone in every way (the weight on dry ground too) and also dashes in water or rain.
- Truthseer's Eye: only the speaking tools (voice, planchette) can lie, so only they use the eye, and a reading spends it only when the spirit read has a lying trait.
- Requital Wail's mark, the Changeling Mask's frame state and Blood Debt's pending payout are in memory (seconds long).
- Devil's Coin and Hearth Dream: a new anger source `devil` (0.1) for the coin; the dream is Borrowed Dream with the anger and the open air taken out.

Tests: `AdvancedCharmGameTests`, one per charm, plus one that every upgrade matches and a filled Stash becomes a Satchel with its stack.

### Phase 5. Relics (Part E)

- Hollow Lantern: a `WardBlock.Kind` with its own radius; `Wards.blocked` writes the clue through the journal path that
  the spirit box uses.
- Ferryman's Coin: the lethal hook (after the totem); a short "faded" state (invisible, no AI targeting, no
  interaction) through an effect, then a teleport back; `Rest`.
- Exorcist's Censer: `Ritual` reads the held relic at `begin`: waves, the snuff rule, the tier filter.
- Hoarder's Hollow: block + per-player inventory in `HauntData`; Mimic Clay's renderer; a hoarder `ITEMS` event variant
  that may take from it.
- Mara's Cradle: a bed block; a personal skip (sanity and the owner's rest, not the world's time); the spawn point
  survives a broken bed once.

**Built:** all five, with `content/Relics.java` as their one seam (the hooks are `Restless.onDeath`, `onWake`, `onSleepFinished`, `onRespawnPosition`). Where it differs from Part E: the Ferryman's Coin fades the holder in place for `relics.ferry_fade_ticks` (no teleport back), clearing effects and setting sanity to 0 at once; the Censer's "+1 essence only if no candle went out" is always met, since a candle lost any other way fails the ritual first; the Hoarder's Hollow store is its own saved 27 slots in `HauntData`, and a hoarder's item event takes from it with `relics.hollow_theft_chance`; the Cradle's "no world skip" is a cancelled `SleepFinishedTimeEvent` when every sleeper is in a cradle, and its spared spawn is the cradle's spot, once. Tests: `RelicGameTests`.

### Phase 6. The Requiem (Part F)

1. **Reliquary** item: a component holding the set of type ids; insert by using an essence on it; refuses unknown or
   unidentified types (checks the journal dossiers); tooltip with progress and "where met" lines.
2. **Altar**: `Altar.Held` accepts the Reliquary when full; `Requiem.begin` checks 14 candles, the salt ring and the
   bell.
3. **`haunt/Requiem.java`**: the 14 movements on `Scheduler`, each a small class behind one interface
   (`start`, `tick`, `passed`), reusing events where they exist (`ExtinguishLightEvent`, `DoubleEvent`, `DoorEvent`,
   the stranger figures, `Assaults`). State in memory like `Ritual` (rule of the codebase). A server stop ends it as a
   failure without the essence loss.
4. **Released**: `HauntData` flag; checks in `Births.deed`, the effigy, `Director.add` (except a forced command),
   `Director.hauntings` (empty), territorial reach (skip), `Sanity.tick` (hold at full).
5. **Laying to rest**: each personal spirit gets a gravestone (`Place`), a closing line in its dossier, then is removed
   through `Director.remove`.
6. **Closing screen**: a server→client payload, and a client `RequiemScreen` that scrolls the poem (lang keys en and
   ru), skippable.
7. **Going back**: the Requiem Bell's sneak-use clears the flag and births three spirits through `Director.add`.
8. Journal chapter `REQUIEM`, the advancement json, the frontend docs.

**Built (Part A):** items 1, 4 (screen and payload), 5, 7 and 8 (journal chapter and advancement; the frontend docs wait for the rite), and the released state. `content/Reliquary.java` holds an `Essences` component (types in insertion order, plus the types the holder's journal has named, refreshed while carried); an essence is named once a photograph of its type is in the journal. `haunt/Requiem.java` has `released`, `release` (lays each personal spirit under a gravestone with a waxed sign naming it, files a `LAID` dossier line, turns the reliquary into the Requiem Bell, opens the `REQUIEM` chapter, grants `restless:requiem`, sends the poem) and `takeBack` (the bell, `requiem.bell_ticks` of sneaking, `requiem.bell_spirits` new spirits at the start anger). `HauntData` keeps `released` and `requiem_wary` per player. `Director.add` returns null for a released player; the command refuses them. **Left for Part B:** `Requiem.begin` (a stub that starts nothing), `Altar.Held`, the 14 movements, failure and `requiem_wary`'s use. Tests: `RequiemGameTests`.

**Built (Part B):** items 2 and 3 and the failure/abort rules. `Altar.useItemOn` takes a Reliquary and calls `Requiem.begin` (full, not released, `requiem_wary` over, no rite already running or at that altar, closed salt ring, `requiem.candles` lit candles within `requiem.candle_radius`, a bell within `requiem.bell_radius`; each refusal has its own message). `haunt/requiem/` holds `Movement` (`start`, `tick`, `passed`, plus `end` for cleanup), `Rite` (the state of one rite), `Candles` (pure life-bar bookkeeping, unit-tested) and `Movements` (the 14 classes, keyed by spirit id). `Requiem.tick` runs every rite from the server tick; each movement lasts `requiem.movement_ticks` (500, so about 5.8 minutes in all), one per type in the order the essences went into the Reliquary. A failed movement snuffs one random lit candle; the rite re-snuffs candles it counts as out if the player relights them, and fewer than `requiem.needed` (7) lit at the end is failure. Failure: one random essence leaves the Reliquary, every personal spirit gains `requiem.fail_anger` (0.3), `requiem_wary` is set to now + `requiem.wary_ticks` (3 days). Success calls `Requiem.release`. Player death, logout, leaving the dimension, or a stopping server ends the rite through `Movement.end` with no essence loss and no cooldown. Progress shows on the action bar (movement, number, candles lit); each movement opens with a chat line that says what it asks. Deviations and judgement calls:

- **The Reliquary is not laid on the altar.** The player keeps it; the rite is tied to the player, not to an altar state (`Altar.Held` has no new value), so a stopping server or a crash can never eat it. The altar refuses other offerings while a rite stands on it.
- **Own tick, not `Scheduler`.** `Scheduler` drops a task silently when its player dies or leaves, which would skip the movement cleanup; `Requiem.tick` sees that and ends the rite properly.
- **Movements are their own code, reusing helpers rather than the haunt events** (those are package-private, tuned for one-off use and read their own config). Shade uses `UnlitLights`; Hallowed Lanterns count and cannot be snuffed. Phantom reuses the mannequin `Figure`. Banshee's ring is the player using a bell block (`Requiem.rung`, the same hook as `Wards.rung`); the cue is a wail for the first 2/5 of the movement, a chime, then silence until 4/5.
- **Nix** places static water source blocks (no fluid tick, so it cannot flow into a candle) two high in the circle outside the altar's 3x3, and removes them after. "Drowning" is air at 0 with the eyes in water; the altar must have no water above it. The Drowned Stone's water breathing makes the first part pass by itself.
- **Mimic** sets two real altar blocks (cannot be mined while they stand); holy water used on the real one passes, on a decoy fails.
- **Wraith** deals magic damage that never takes the player below half a heart; passing is above 6 health. **Mara** decides at the start from sanity (above 0.3 wakes); a sleeper gets Slowness VII and Blindness for the movement.
- **Revenant** spawns up to `requiem.revenant_mobs` of the monster types in the player's kill statistics (zombies if none; no Wither or Warden). **Lost one** moves the player 30 blocks away and counts a return to the ring within 2/5 of the movement; one still out at the end is brought back to the altar.
- **Domovoi** slams every wooden door within 8 blocks open (at the start and half way) and needs them shut. It does not touch chests.
- **Hoarder** hides a tagged amethyst shard (the offering, not the Reliquary) in one of three temporary chests with an unrolled empty loot table, so the Magpie Feather points at them. The chests and the shard are removed afterward.
- **Demon** offers the bargain in chat with clickable `[Accept]` and `[Refuse]` (they run the command `/requiem accept|refuse`, open to everyone); accepting skips the movement and costs `requiem.demon_cost` candles, no answer counts as a refusal.
- **Trickster** snuffs three ring candles, swaps all ring candles to random colors, and sets three unlit decoy candles; passing is the three relit and no decoy lit. Colors and decoys are undone afterward, and candles left out are relit, so the rite alone decides what is lost.
- Items movements take from the player (Poltergeist) are returned on failure and on abort. Config keys for each movement are in the `requiem` section of `haunt.json`; no migration is needed (new keys come from the defaults).
- The tests drive each movement object directly (the rite state is a public class) and run whole rites with `Requiem.begin(..., ticks)` using 2 or 3 ticks per movement; the demon is put last in the Reliquary order for the pass/fail endings because it passes whatever the player does.

Tests: `RequiemGameTests`: a Reliquary refuses an unidentified type, a full one starts the rite, each movement passes
and fails on its own (one test per movement, driving its condition directly), a released player gets no deed births
and no territorial events, and ringing the bell gives three spirits back. A unit test for the candle bookkeeping.

### Phase 7. Text and docs

- `lang/en_us.json`, `lang/ru_ru.json`: names, tooltips, messages, journal pages, the poem.
- Models and textures for every item and block (about 40 new); see the open question on art.
- `apps/frontend/content/spirits.md`: the rewards table grows into charms, consumables, advanced charms, relics and the
  Requiem; plus `docs/spirit-rewards.md` status.

**Built:** lang (en, ru) is in step, 494 keys each; the wiki has Items pages (essences, charms, consumables, advanced, relics, tools, Requiem) in en and ru and the `spirits.md` rewards section; `docs/spirit-rewards.md` status points here. Every model and texture the code references exists. The Requiem's two item textures were replaced with generated art. No new block-entity renderer was needed: Hoarder's Hollow is a plain block model and Mimic Clay is an item. **Still open:** nothing from this phase.

### Size and order

| Phase | New items / blocks | Relative size |
|---|---|---|
| 0 Seams | 0 | M |
| 1 Rebalance | 0 | S |
| 2 Economy | 1 | M |
| 3 Consumables | 14 | L |
| 4 Advanced | 14 | L |
| 5 Relics | 5 | L |
| 6 Requiem | 2 | XL |
| 7 Text | — | M |

Phases 3, 4 and 5 are independent of each other once phase 0 is in; the Requiem needs phase 2 (meetable types) and is
best after 3 and 4, because its movements lean on those items.

## Open questions

- **Shared Requiem.** On a server, can a group sing one Requiem together (each player gives part of the Reliquary)? It
  is fun, but it splits the arc that makes the finale personal.
- **Released and the land.** Should a released player's presence slow territorial blight, as a reward that still keeps
  them in the world's story?
- **Art.** About 40 new textures; Mimic Clay and Hoarder's Hollow need block-entity renderers.
- **Dreamless Draught on servers.** Skipping the night alone is a big lever over other players; the default may be
  better false on dedicated servers.
- **Infernal Contract stacking with other mods' enchantments** past their maximum: limit to vanilla enchantments, or to
  a tag.
