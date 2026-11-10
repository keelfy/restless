# Scary events

Backlog of hard scares built on the `Silhouette` figure: slow suspense, jump scares, and both. Only what is not built
yet is listed. Built events live in `event/Jolts.java`, `event/Scares.java` and `event/Frights.java` and are in
[events-index.md](events-index.md); a built event is deleted from the tables here.

Built so far (first batch): `mined_face`, `dig_down`, `eating`, `container_close`, `slow_turn`, `echo_answer`,
`name_left_right`, `door_slam_face`, `first_light`, `hide`. Two deviations from the plan: a door that has just shut
flies open on the figure (vanilla doors have no window to see through), and `dig_down` fires for a floor block beside
the player, not the one under their feet (the player would fall into rock the client shows as air).

Second batch: `third_person`, `pit_face`, `surface_break`, `shield_knocks`, `behind_trunk`, `tree_line`,
`stairs_sitter`. Deviations: `behind_trunk` fires for the base log of a trunk (a log above, none below), and shows the
log above as air so that all of the figure is seen; `third_person` stands the figure in the pulled-out view too, and
repeats it with the bang when the view ends; `stairs_sitter` has no jump (no `startle`), so it does not use up the
pause. `shield_knocks` and `surface_break` have no gametest yet (they need a player with a raised shield or a head in water).

Most ideas sit on what exists: `PacketEntity` (a figure only this player sees), `Swaps` (client-only block swaps),
sounds sent to this player only, look tests from `Jolts.watched`, and polling inside `Scheduler.repeat`.

**Type:** S = suspense, J = jump scare, S→J = tension, then the scare. **Diff:** 1 = existing pieces (polling counts),
2 = a new shared piece or a new server hook, 3 = a client hook.

## Limits

What every idea here must respect:

- **The server sees:** look direction, sneaking, sprinting, shield block, bow draw, eating, sleeping, climbing, head
  under water, the open container, door state, dimension. Hooks exist for block break, left/right click on a block,
  wake, can-sleep, death, respawn, damage and chat.
- **The server does not see:** F1, F2, F5, the pause menu, the player's own inventory screen. No idea may hang on them.
- **The silhouette** has 12 fixed shapes: ten any scare may pick, a bare head (`SilhouetteRenderer.FACE`, for a hole
  in a wall) and one lying on its back (`LYING`). A new shape is a new entry in `SHAPE_PARTS`; to keep it out of the
  random pick put it after the first `ANY`. It cannot sit, climb, crawl on walls, hold items, tilt or scale.
- **No** hijacking mob AI, scaled models, faked lag or input freezes, fake Tab list rows.

## Rules

1. **The player sets off the scare.** It comes when the player walks up, looks, opens or turns round, never on a
   timer. Going to look is the tension.
2. **The lure is faint, repeats and can be found:** quiet or small, every few seconds, from one spot.
3. **The scare is fast and gone at once:** at most 5 ticks to reach the player, the loud sound on contact, gone within
   about 10 ticks.
4. **If ignored, the event ends quietly** after `max_ticks`, or when the player walks farther than `leave_distance`.
5. **Some lures end with nothing (duds).** `dud_chance` about 0.3, so a rustle is not always a scare.
6. **The jump-scare pause counts from the scare, not from the roll.** An ignored or dud lure does not use up
   `startle_gap_ticks`. Built: an event that waits (`HauntEvent.waits`) calls `Director.landed` when its scare comes,
   and only then do the history, the pause, the menace band and the sanity loss count it.
7. **Only this player sees or hears it.** The server keeps nothing.
8. **Escalation.** A young spirit's silhouette only shows far off (`tree_line`, `path_blocker`); as it ages it comes
   closer (`slow_turn`, `stairs_sitter`); at its peak, jump scares and harm (`dragged`, `hold_still`, `hide`).

## Shared pieces

| Piece | What | Diff |
|---|---|---|
| `Lure` | `jolt_lunge`'s `ambush` as a helper: a spot, a cue every `cue_gap_ticks`, a trigger test, a scare, `max_ticks`, `leave_distance`, `dud_chance`. Calls `Startle.record` when the scare runs. | 2 |
| Scare moves | Paths over `PacketEntity.move`: `lunge` (built), `rise` (out of the ground, water or void), `drop` (from the ceiling), `appear` (point blank, no travel), `slam` (onto glass). | 1 each |
| Triggers | `near(d)`, `looks_at(spot, angle)`, `looks_down`/`looks_up`, `turned_back`, `container_closed` (polled), `door_toggled(pos)` (polled), `block_broken`, `ate`, `climbing`. | 1 |
| Hidden spots | Finders in `Place`: behind a wall (built: `Jolts.lair`), under the floor (client swap to air), a stair run, a drop ≥8 below an edge. | 2 |

## Look and camera

| Id | Setup → scare | Type | Diff |
|---|---|---|---|
| `peripheral` | Extends `edge_figure`: looked at, it vanishes and shows 50–65° to the other side; the third time it is at 1.2 blocks in the centre, bang | S→J | 1 |
| `feet` | Pitch >70 on solid ground: the block underfoot shows as glass and the 2 below as air, silhouette below looking up, 8 ticks, swapped back | J | 1 |
| `upside_down` | Indoors, pitch < -45: the hanging shape under the ceiling 3 blocks ahead drops to face height in 3 ticks, bang | J | 1 |
| `sneak_mimic` | Steps 4 blocks behind copy each player step; the player stops, 2 more steps, then breath at 1 block | S | 1 |

## Containers, signs, death

| Id | Setup → scare | Type | Diff |
|---|---|---|---|
| `sign_prefilled` | Placing a sign: the editor already reads "I'm behind you"; on the sign update, silhouette on the far side of the sign | S→J | 2 |
| `death_screen` | On death, silhouette 1.5 blocks from the body facing the camera until respawn | S | 1 |
| `respawn_watcher` | Respawning at a bed: silhouette at the foot of the bed for 20 ticks, gone when looked at | S | 1 |

## Sleep and home

| Id | Setup → scare | Type | Diff |
|---|---|---|---|
| `pillow` | 20 ticks into sleep, silhouette at the foot of the bed facing the player, a soft breath, gone in 15 ticks; sleep not broken | S | 1 |
| `monsters_nearby` | Bed at night with no hostiles near: the vanilla "monsters nearby" refusal, then scuttling 2 blocks behind | S | 1 |
| `woke_door` | On waking: the closed bedroom door shows open (client swap), steps walk away past it | S | 1 |
| `basement_lamp` | A torch, lantern or campfire in view goes out (client swap) for 5 ticks, 3 times; each dark moment the silhouette is 3 blocks closer; the 3rd is at the face, bang | S→J | 1 |
| `door_knock_answer` | Twist on `three_knocks`: 3 knocks on a door near the player; opening it shows nothing; on closing, the same 3 knocks 2 blocks behind | S→J | 1 |
| `dark_doorway` | The player opens a door into a room with light ≤3: silhouette 3 blocks inside steps back into the dark and is gone | S | 1 |
| `window_slam` | ≤2 blocks from glass, looking out at night ≥60 ticks: 2 `phantom_glass` taps, then the silhouette slams onto the outer face of the glass, bang | S→J | 1 |
| `furnace_eyes` | ≤3 blocks from a lit furnace ≥100 ticks: it shows unlit (client swap), eyes particle in the mouth, blink, relit | S | 1 |

## Caves, mining, building

| Id | Setup → scare | Type | Diff |
|---|---|---|---|
| `ladder_follower` | On a ladder, `ladder_steps` below; pitch >50 shows a silhouette 4 blocks below rising 1 block every 2 ticks; gone at 1 block | S→J | 1 |
| `ladder_top` | Climbing a ladder ≥6: at the top hole, silhouette leans in 1 block above the head, bang | J | 1 |
| `pillar_base` | After pillaring up ≥5 blocks, pitch >60: silhouette at the pillar's base looking up; gone in 20 ticks | S | 1 |
| `dropped_torch` | A torch the player placed ≥30 s ago, out of view: the block is removed, the item pops at their feet, a silhouette stands in the new dark there until looked at | S | 2 |
| `ore_glint` | End of `fake_ore`: within 3 blocks the fake ore swaps back to stone, eyes particle at that face, bang | J | 1 |
| `cave_mouth` | Walking out of a cave toward a daylit opening: silhouette backlit in the opening; gone at 8 blocks | S | 1 |

## Water

| Id | Setup → scare | Type | Diff |
|---|---|---|---|
| `boat_seat` | In a boat, yaw >120° from its heading: a fake passengers packet puts the silhouette in the back seat; one blink, the real packet is resent | J | 2 |
| `rod_reel` | Reel-in after a bite: silhouette rises at the bobber and lunges along the line to the face | J | 1 |
| `bucket_reflection` | Filling a bucket from water: silhouette rises waist-deep from the next water block 1.5 blocks off, splash, sinks | J | 2 |

## Open world and night

| Id | Setup → scare | Type | Diff |
|---|---|---|---|
| `path_blocker` | Each time the player turns >60°, the silhouette is re-placed 20 blocks ahead (`standingAhead`); the 4th time at 3 blocks, then gone | S | 1 |
| `grass_crawler` | Tall grass: rustle and leaf particles 10 blocks off creep 1 block/s toward the player; at 2 blocks the all-fours shape lunges, bang | S→J | 1 |
| `scarecrow_field` | ≥9 farmland: a fake armor stand with a carved pumpkin at the field edge; each look-away 3 blocks closer along the rows; at 2 blocks it becomes the silhouette, bang | S→J | 1 |
| `farm_row` | Harvesting crops: an all-fours silhouette crouches 6 blocks down the row; on the next harvest it lunges | S→J | 1 |
| `sand_rise` | Sand, gravel or dirt: silhouette rises out of the ground 8 blocks ahead to the shoulders over 40 ticks, holds, sinks | S | 1 |

## Items and actions

| Id | Setup → scare | Type | Diff |
|---|---|---|---|
| `bow_scope` | Bow drawn ≥30 ticks with nothing living on the aim line: silhouette 16 blocks out on that line charges to the face in 6 ticks | J | 1 |
| `pearl_landing` | Ender pearl in flight: silhouette at the landing spot facing it; the player arrives face to face, bang | J | 1 |
| `portal_follow` | 60 ticks after a portal arrival, silhouette steps out of the portal, walks 8 blocks into the dark, gone | S | 1 |

## Sound and long suspense

| Id | Setup → scare | Type | Diff |
|---|---|---|---|
| `hold_still` | Silhouette at 4 blocks + action bar "don't move", 100 ticks; moving >0.5 block → lunge and a threat-band hit; still → it walks off | S | 1 |
| `blindfold` | Scenario: `blindness` + `circling_moan`; when blindness ends, silhouette at 1.2 blocks ahead, bang | S→J | 1 |
| `dragged` | In the dark, velocity nudges pull the player 3 blocks back (as `the_call`), silhouette behind for 4 ticks. Harmful tier | J (harm) | 1 |
| `quiet_mobs` | Scenario: `animals_stare`, then the player turns ≥150° and back → silhouette ahead at 1.2 blocks, bang | S→J | 1 |

## Dimensions

| Id | Setup → scare | Type | Diff |
|---|---|---|---|
| `void_below` | End, pitch >60 at an island edge: silhouette 12 blocks down in the void rises 1 block per 4 ticks; gone at 4 blocks | S | 1 |
| `bastion_gold` | Nether, 20–30 blocks ahead: 6 fake piglins in a ring facing a silhouette; at 12 blocks all turn to the player; on the next look-away all are gone | S | 1 |

## Cut

Do not bring back without a new mechanic.

- **The server can't see it:** `fake_lag`, `pause_menu`, `clean_frame`, `screenshot`, `inventory_doll`, `held_map_back`.
- **Needs model, pose or render work:** `dolly_zoom`, `under_hull`, `pig_saddle`, `ghast_face`, `trader_hood`,
  `ravine_crawler`, `pumpkin_mask`, `wingman`, `ice_under`, `bonemeal`.
- **Needs mob AI we don't control:** `villager_swap`, `horse_rears`, `zombie_crowd`, `bells`.
- **Too rare to trigger:** `crawlspace`, `drowned_choir`, `rail_stand`, `map_dot`, `powder_hand`, `night_flock`.
- **Already built:** `sleep_abort` (`night_terror`), `someone_home` (`table_figure`, `a_visitor`), `storm_flash`
  (`flash_figure`), `hilltop` and `fog_wave` (`hill_waver`), `parallel_runner` (`trailing_figure`), `sprint_at`
  (`stalker`), `blizzard` and `basalt_haze` (`shadow`), `branch_mine` (`wall_miner`), `held_door` (`locked_door`),
  `herd_extra` (`cow_herd_blink`), `friend_echo` (`friend_double`), `tab_ghost` (`stranger_joins`), `cave_air`
  (`jolt_behind`), `hot_cold` (`jolt_lunge`).
- **Merged:** `breath_sync` into `sneak_mimic`, `campfire_ring` and `fridge_light` into `basement_lamp` and
  `container_close`, `corner_turned` into `slow_turn`.
- **Unreadable:** `bed_count`.

## Order

Next: `peripheral`, `feet`, `ladder_follower`, `pillar_base`, `grass_crawler`, `blindfold`, `quiet_mobs`: diff 1, each
plays on a habit every player has.
