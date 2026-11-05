# Improvement plan

Result of a code and architecture review of `apps/mod` (2026-11-03). Ordered by priority. Line numbers are as of commit
`9446372`. Steps 1, 3 (mostly), 4 (mostly), 5 (mostly) and 7 are done, step 2 is partly done; the rest is not built yet.

What stays as it is: `core` on vanilla only behind `Platform`, versioned config with migrations and the key coverage
test, `optionalFieldOf` codecs for old saves, payload validation, `Hardcore` / `Restore.revert` as one policy.

## 1. Isolate event failures (done)

Built as `haunt/Faults`; tests in `gametest/FaultGameTests`. Scheduled steps and undos are logged and dropped, not
tied to their event yet (needs the context from step 4).

**Problem.** `Director.fire` runs `event.run` (`haunt/Director.java:274`) and `Director.pick` calls `canRun` (`:357`)
with no `try`; same for `Scheduler.tick` and `Restore.tick`. One exception in one of ~300 lambda events crashes the
server tick.

**Do.**
- Catch `RuntimeException` around `canRun`, `run`, each `Scheduler` step and each `Restore` undo.
- Log once with the event id, disable the event until restart, keep ticking.
- GameTest smoke test: every event in `HauntEvents.all()` passes `canRun` and `run` on an empty plot without throwing.

**Done when.** A deliberately throwing test event is logged and skipped, the server keeps running.

## 2. Make the event roll cheap (partly done)

Done: `Nearby` skips every chunk section whose palette has no matching state and `any` stops at the first hit;
`Director.pick` checks `opensAt` and `Bands.open` before `canRun`, so closed events are not scanned (and no longer feed
`Rarity`). Measured with a GameTest on a 10x10 stone plot: all `canRun`s took ~100 ms, now ~8 ms. Not at the 1 ms goal;
left is the shared neighbourhood snapshot (last bullet) and the weights-first option. Real terrain is not measured.

**Problem.** `pick` calls `canRun` on every event (`Director.java:349`). ~190 `canRun`s use `Nearby.any`, which scans
the whole cube and collects every hit before picking one (`haunt/event/Nearby.java:48-60`). Radii in config: 16 for
63 events (35k blocks), 24 for 25 (117k), up to 48 (940k). Tens of millions of `getBlockState` per due spirit.

**Do.**
- `Nearby.any` returns on the first match, allocates nothing.
- In `weight`, check cheap gates first (tier, `opensAt`, `Bands.open`, base weight > 0), `canRun` last.
- Option to decide consciously: roll on weights first, call `canRun` only on the winner, reroll on failure. Changes the
  distribution (weights stop depending on readiness), `Rarity` then needs another source.
- Later, if still heavy: one neighbourhood snapshot per roll shared by all events.

**Done when.** A profiled roll for one spirit at default config stays well under 1 ms.

## 3. Per-server session instead of static state (mostly done)

Done: `haunt/Session` has a `SERVER` and a `CLIENT` scope; the 28 runtime collections (`Scheduler`, `Restore`, `Recent`,
`Rarity`, the event `ACTIVE` maps, `Followers`, ...) are created through `Session.SERVER.keep(...)` and emptied by
`Restless.onServerStopping`. `DoorLocks` has a map per side; the client one is emptied on logout. The statics stay
statics: no `HauntSession` object, no per-server instances. Test: `stopping_forgets_the_world`, `SessionTest`. Left: the
client overlays (`SanityOverlay`, `Dimming`, `HeadTurn`, ...) keep their last value across worlds; the `Afflictions`
sets are still plain `HashSet`s; a collection added later has to be wrapped by hand.

**Problem.** ~40 static maps and lists (`Scheduler.TASKS`, `ShadowEvent` / `DoubleEvent` / `ArmorStandEvent.ACTIVE`,
`DoorLocks`, `Candle.OUT`, `Echoes`, `Recent`, `History`, `Afflictions.DREAMERS`, `Rarity`, `Machines`,
`Lethals.AVENGED`, ...). `Restless.onServerStopping` clears only `Restore`, `Location`, `Reactions`.

- In single player, after leaving a world the statics hold `ServerPlayer`s and entities, and through them the old
  `ServerLevel`: a leak until the next world.
- Bug: `DoorLocks.LOCKED` keeps absolute game time. Lock a door in a world at tick 1 000 000, quit, open a world at
  tick 1 000: the door at the same position stays locked for ~14 hours of play (`haunt/DoorLocks.java:33-47`).
- `DoorLocks` is one map for client and server in single player.
- `Afflictions.DREAMERS` / `SLEEPWALKERS` are plain `HashSet`, neighbours use `ConcurrentHashMap`.

**Do.**
- `HauntSession` created on server start, dropped on stop; runtime maps become its fields.
- Client-side state (`DoorLocks` client copy, overlays) in a separate client holder, reset on disconnect.

**Done when.** Quitting and opening another world leaves no mod object referencing the old server.

## 4. Explicit haunt context (mostly done)

Done: `haunt/HauntContext(spirit, at, scenario)` captures `Said`, `Location` and `ScenarioContext` and runs work inside
them. `Said.during` and `Location.during` restore the outer value. `Scheduler.repeat` and `Restore.after` capture it, so
every delayed step and undo keeps its spirit and point; `Scenario` no longer carries them by hand. Tests:
`HauntContextTest`, GameTest `delayed_step_keeps_the_spirit_point`. Left: the context is still three statics, not
passed to `run` explicitly (the "Later" item); `Wards` and other callers that read `Said.current` are unchanged.

**Problem.** `Said.during` and `Location.during` reset to `null` instead of restoring the previous value
(`haunt/Said.java:22`, `haunt/Location.java:57`); `ScenarioContext.during` restores correctly. A nested
`Director.act` inside an event clears the outer speaker and point. `Scheduler.repeat` carries only `ScenarioContext`
into later steps; `Said` and `Location` are carried by hand, only `Scenario` does it (`Scenario.java:169`). Delayed
steps of other events lose the spirit's voice and source point.

**Do.**
- One record `HauntContext(spirit, at, scenario)`, saved and restored as a stack.
- `Scheduler` and `Restore` capture the whole context.
- Later: pass it to `run` explicitly instead of a static.

**Done when.** A delayed step of a plain event speaks as its spirit and sounds from its point; a nested act does not
clear the outer context.

## 5. Scale `HauntData` before structure births fill worlds (mostly done)

Done, with the save format unchanged (so no migration): `HauntData` keeps territorial spirits in a map by uuid (saved
order kept) with a grid by dimension and 64-block square, and personal spirits have an owner index. `find` / `change`
are O(1); `territorialReaching` / `territorialCovering` / `territorialWithin` replace the walks in `Sanity`, `Sense`,
`Births`, `Anger`, `Wards` and the three `anchor` helpers; `tickTerritorial` reschedules one spirit at a time instead
of rewriting the list; a removed spirit takes its `said` entry with it. GameTest `ten_thousand_territorial_spirits`:
100 `Sense.of` + 1000 `find`+`change` + 100 births + 100 bells took ~330 ms, now ~3 ms; `territorial_index_follows_changes`
covers the index. Left: blight is still one map in the same file (no chunk attachments); `menace` is keyed by player and
never dropped; saving still encodes every spirit (not measured).

**Problem.** One `SavedData` holds personal and territorial spirits, graves, blight, sanity, notebooks, `said`,
`menace`.
- `territorial` is a flat list: walked whole by `tickTerritorial` and `Corruption.tick`, copied by every
  `putTerritorial`.
- `Corruption.tick` calls `data.find()` (linear) for every blight (`Corruption.java:37`): O(B·N).
- `Births.generated` measures distance to every anchor of the dimension on every new structure.
- Blight keeps up to `max_blocks` spots with their old `BlockState` per spirit, all in one file rewritten on save.
- `said` and `menace` are never cleaned when a spirit or player is gone.

Structure births (`Restless.onNewChunk`) make N grow with explored area.

**Do.**
- `Map<UUID, Haunting>` index for `find` / `change`.
- Territorial spirits indexed by dimension and region (chunk or region coordinates) for births, ticks and reach.
- Blight in chunk attachments or a per-dimension `SavedData`.
- Drop `said` / `menace` entries with their spirit.
- Needs a save migration: do it before structure births ship.

**Done when.** A world with 10 000 territorial spirits ticks and saves without visible cost.

## 6. Typed, cached config

**Problem.** 967 `Config.section("x").integer("y")` calls, each splitting the path and walking the JSON tree, also in
the hot `pick` loop. No reload. The client reads its own local copy (`Restless.onUseBlock` →
`Config.event("locked_door")`), so on a dedicated server with a different config the sides disagree.

**Do.**
- Cache `Section` by path (cheap first step), later parse once into immutable records per section.
- Send the values the client needs on join.

## 7. GameTests in CI (done)

Added a `:neoforge:runGameTestServer` step to `mod.yml`, after `build`. `event_goat_rams` failed once in a baseline run
(timing); if it flakes in CI, fix the test.

**Problem.** `.github/workflows/mod.yml:26` runs only `./gradlew build`; `runGameTestServer` is not part of `check`.
12 GameTest classes never run in CI. Unit tests in `core` cover mostly pure math.

**Do.** Add a `:neoforge:runGameTestServer` step; include the smoke test from step 1.

## Medium

- **`Restore` is memory only** (`Restore.java:14`): after a crash changed blocks stay forever. A light persisted undo
  log (pos, old state) in `SavedData`.
- **`ReadPayload` has no rate limit**: each request sends the whole journal with photos (16 KB each) (`Journal.sync`).
  Rate-limit per player.
- **Light of unloaded anchors**: `Director.cooldown` → `conditions()` reads light and sky at an anchor whose chunk may
  be unloaded (`Director.java:301-319`); likely a false `DARK` / `NIGHT`. Unverified, check first.
- **Huge theme classes**: `ItemEvents`, `BlockEvents`, `MobEvents` (1100–1200 lines), `Lies`, `Household`, `Sounds`
  (800+). Lambdas in `List.of(...)` give stack traces like `lambda$all$17`. Named methods per event, a file per large
  event.
- **No client/server boundary**: `Dimming`, `HeadTurn`, `JournalScreen`, `SanityOverlay` sit next to `Director` in
  `haunt`. Today server code mentions them only in javadoc. A `client` package plus a test that server classes do not
  import `net.minecraft.client`.
- **`HauntEvents.byId`** is a linear scan: use a `Map`.
- **`Haunting` withers**: 10 fields, 6 methods each listing all of them; a wrong argument order silently corrupts
  saves. One copy helper.
