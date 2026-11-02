# Assaults plan

Spirits mostly scare. Out of about 530 events, 16 carry `ASSAULT`, and all of them stop at `assaults.safe_health`.
The result: no spirit ever really threatens the player. This plan adds real danger in three bands. The last band can
kill, but only the cruelest spirits use it, and only as a last resort.

## How many

- **Now:** 16 `ASSAULT` (3 %), 18 `AFFLICTION`, and 89 `HARMFUL` events in total.
- **Target:** 9–12 % of the catalog. That is enough for an angry, mature spirit to feel dangerous, because
  `Power.tierFactor` already moves weight to `HARMFUL` as power grows. A calm spirit still mostly scares.
- **Plan: +50 events, for 66 in total.** That is 12 % today and about 9 % once ambient events reach about 700. When
  the catalog passes 800, add about 1 assault for every 10 new ambient events.

| Band | New | Lowest health it leaves | Who uses it |
|---|---|---|---|
| Threat | 28 | `safe_health` (3 hearts), as now | any spirit, weighted by its `ASSAULT` affinity |
| Brink | 14 | `brink_health` (half a heart) | power ≥ 0.6 |
| Lethal | 8 | none: it can kill | evolved spirits at power ≥ 0.9, on a whitelist |

There are also 4 hunt scenarios. Scenarios do not count toward the catalog: they chain these events.

## Rules

1. **Telegraph and counter.** Every brink or lethal event first gives a warning the player can read: a sound, particles,
   a screen effect or a journal line. After that the player has at least 3 s to act, and there is always a clear
   counter: run, step into light, look at it, hit it, leave the water.
2. **Floors in one place.** Each event names its band in the config (`"band": "threat" | "brink" | "lethal"`).
   `Assaults.room(player, band)` returns how much damage is still allowed. Events no longer compute
   `getHealth() - safeHealth()` themselves.
3. **Spirit mobs and projectiles are capped too.** A mob, arrow or falling block that a spirit spawns gets a tag with
   its band. A single damage hook caps the damage at `room(band)`. Without this, a real zombie or arrow could kill the
   player in the threat band.
4. **Lethal is a last resort:**
   - gated by `evolved: true`, `min_power: 0.9` and `spirits: [...]`. Power already includes anger: with full maturity,
     0.9 needs anger ≥ 0.75. The event gets the same `opensAt` check as `Scenario`, so no new gate is needed;
   - at most once every `lethal_cooldown_days` (3 Minecraft days) per player. The time is stored in `HauntData`, so a
     relog does not reset it;
   - a brink event from the same spirit must have happened since the last lethal one. This is the final warning;
   - after it kills, the spirit is satisfied: its anger drops by `death_relief`, and the journal records the death and
     names the spirit;
   - `assaults.lethal`: `on | off | not_hardcore`, default `on`. Hardcore is not exempt by default, so a hardcore
     player is advised to protect themselves with wards.
5. **Mercy after a brink.** After a brink event, no `HARMFUL` event runs for `mercy_ticks`. Two brink events in a row
   would be a lethal one in disguise.
6. **Death messages.** Lethal and brink events use the `Taken` damage types and their death messages, such as
   "was taken by the drowner".

## Infrastructure (phase 1)

| Piece | Where | Note |
|---|---|---|
| `Band`, `room`, `brink_health`, `mercy_ticks` | `Assaults` + `assaults.json` | replaces the scattered `safeHealth` math in Assaults, ShadowEvent and ColdTouchEvent |
| `opensAt` for single events | `SimpleEvent` (the code `Scenario` uses for the same check) | `min_power`, `evolved`, `spirits` |
| Damage cap for spirit-made damage | a loader damage hook plus an entity tag | capped by band; lethal is not capped |
| Lethal cooldown, last brink, relief | `HauntData` | persisted |
| `Taken` kinds and per-spirit death messages | `Taken`, lang files | en and ru |
| Tests | `Power`/`Assaults` unit tests | band floors, lethal gate, cooldown math |

## Threat band (28, phase 2)

They leave the player at `safe_health`. Each is a distinct moment.

| id | What happens | Counter |
|---|---|---|
| `rake` | Three scratch sounds, then three stepped hits with a red vignette pulse | move away between hits |
| `teeth_in_dark` | A growl at light 0. Every 2 s in the dark, a bite | reach light |
| `hurled_block` | A loose block nearby (gravel, cobble) lifts with dust, then flies at the player as a falling block | step aside |
| `flung_tools` | A nearby chest opens and the tools in it fly at the player | dodge or close the chest |
| `volley` | A bow draws in the dark, then 3–5 arrows come from one direction | take cover |
| `cave_in` | Dust and creaking overhead, then gravel falls over the player | step out from under it |
| `dripstone_rain` | Pointed dripstone breaks off the ceiling | keep moving |
| `searing_armor` | The chestplate glows and burns every second until it is taken off | take it off or jump into water |
| `frost_grip` | Freezing like powder snow, but on dry land | reach fire or a light source |
| `pinned` | Heavy slowness and mining fatigue; nearby hostiles become aggressive | fight or wait it out |
| `possessed_mob` | One mob nearby turns: soul eyes, speed and strength, targets the player | kill it |
| `frenzy` | All hostiles within 16 blocks get speed and strength for a short time | fall back |
| `turned_pets` | Your tamed wolves or cats growl and attack for 10 s | walk away |
| `vex_swarm` | 2–3 short-lived vexes, named "?" | fight back |
| `ghost_creeper` | A hiss behind the player, then a small blast that breaks no blocks | run on the hiss |
| `hand_below` | A soul circle under the player's feet; staying 2 s means a hit and rooting | step off the circle |
| `blade_behind` | A sweep sound behind the player; turning away from the spot means a hit | face the spot |
| `door_bite` | The door slams on the player as they pass through | use another way |
| `night_terror` | The player wakes from bed with damage and a scream (mara, sleep_thief) | wards |
| `throat` | Air drains on dry land in any light; a whisper says "hush" | crouch and wait, or drink milk |
| `undertow_dry` | The player is pulled backwards while sprinting | stop sprinting |
| `lantern_burst` | A lantern or torch near the player bursts into fire | stay away from the lights |
| `stalker` | A figure runs at the player and hits once, then disappears | outrun it |
| `stone_fists` | Blows from inside the wall the player is leaning on | step away from the wall |
| `hail_of_bones` | Bones fall from the sky and hit at night | take cover |
| `blood_cough` | Hits of 1 every 3 s with particles, 5 in total | drink milk |
| `wrong_step` | A block under the player's next step becomes a fake hole, and they fall 1–3 blocks (real holes in hardcore) | watch where you step |
| `ash_storm` (Nether) | Fire and hits from a soul fire wind | take shelter |

## Brink band (14, phase 3)

Each takes the player down to half a heart, then gives them a mercy window.

| id | Warning | Counter |
|---|---|---|
| `final_blow` | 3 s roar and a soul ring on the ground, then one blow down to half a heart | leave the ring |
| `the_hanging` | The player is lifted up, then dropped from a height that leaves half a heart | water, a block under them, or slow falling |
| `held_under` | Under water, the player is pushed down until half a heart | respiration, or swim sideways out of the column |
| `pyre` | Fire that will not go out, and fire resistance is removed | water |
| `withering` | Wither that goes on until half a heart | milk |
| `count` | A screen countdown: "3… 2… 1…" | light or holy ground |
| `the_hollow` | A spirit-bound named zombie or vindicator hunts the player; its damage is capped at the brink | kill it (anger −0.1) |
| `ledge_push` | A strong push at a cliff edge; the fall leaves half a heart | crouch on the warning |
| `circle_of_knives` | Arrows in a ring around the player, closing in | break out of the ring |
| `smother_brink` | `suffocation` that goes past the safe floor | light |
| `embrace_brink` | `embrace` that goes past the safe floor | 10 blocks of distance |
| `red_room` | Darkness, a heartbeat, then a hit every second while it lasts | look away from the figure |
| `drain` | Health drains, one heart per second (exhaustion visible) | eat |
| `bound` | The player is rooted, and a hostile is pulled to them | kill the hostile |

## Lethal band (8, phase 4)

There is one signature death for each of the most violent evolved spirits, plus a few general ones. All are on a
whitelist.

| id | Spirit | Warning → death | Counter |
|---|---|---|---|
| `reaping` | light-eater shade, wraith | `shadow` (idea 61): it freezes while watched; its touch kills | do not look away; light |
| `judgement` | archdemon | knocks, a bolt that misses, then a full bolt with no cap | do not stand still |
| `last_call` | forsaken (lost one) | `the_call` to lava or the void; it does not stop at the safe floor | walk away far enough |
| `drowner` | drowner (nix) | held under until death | leave the water within 10 s, or respiration |
| `the_avenger` | avenger (revenant) | after a killing spree, a named avenger fights to the death | win the fight (banishes the anger) |
| `hellring` | archdemon | a ring of fire closing in for 10 s | step out of the ring |
| `the_double` | doppelganger (phantom) | your double walks to you; its touch kills | hit it 3 times |
| `foretold` | keening banshee | a truthful omen in the journal and a bell; death comes at dusk unless the player sleeps or is warded | a bed or wards before dusk |

## Hunt scenarios (4, phase 4)

| Scenario | Beats |
|---|---|
| The hunt | `volley` +5 `stalker` +5 `the_hollow` |
| The last night (evo) | `count` +10 `reaping` |
| Debt | `possessed_mob` +5 `final_blow` +20 `the_avenger` (only after a brink) |
| Drowning season | `undertow` +3 `held_under` +30 `drowner` |

## Not doing

- No damage at all on peaceful.
- Lethal events never run within the first `evolution.days` of a world. A spirit evolves no sooner than that anyway.
- No new spirit types.

## Done when

- Phase 1: tests pass. A threat event cannot drop the player below `safe_health`, including damage from spirit mobs.
  A lethal event cannot open for a spirit that is not evolved, is below 0.9 power, is not on the whitelist, is on
  cooldown, or has had no brink event since its last lethal one.
- Each phase: every event has a config entry, en and ru lang lines and a journal name, and it runs through
  `/haunt event <id>`.

## Status

All four phases are in code: 50 new events (12 `ambushes`, 16 `torments`, 14 `brinks`, 8 `lethals`) and 4 hunt
scenarios (`the_hunt`, `the_last_night`, `debt`, `drowning_season`). Unit and config tests pass; nothing has been run in a
live world yet.

Where the code differs from the plan above:

- The warning before a lethal event is per player, not per spirit: any brink event arms every lethal event.
- The whitelist key is `spirit_ids`.
- `the_avenger` counts mob kills in memory (resets on restart); `foretold` kills at game time 14000, not exact dusk;
  `reaping` has no light counter.
- Scenarios open with a light event, so each has one the plan lacks. `the_hunt` and `debt` use `final_blow` in place of
  `the_hollow`, `the_last_night` uses a light event in place of `count`, and `drowning_season` has no `held_under`.
- `death_relief` (0.5) is applied when the player dies within `relief_window_ticks` (2400) of the last lethal event
  (`Bands.died`, called from `Restless.onKill`); `foretold` stamps its time when the bell kills, not when it warns. It
  lowers the player's own spirits and the territorial ones covering the death. There is no journal line for the death.
- The new events are listed in `docs/spirits.md` and on the frontend `assault.md` page.
