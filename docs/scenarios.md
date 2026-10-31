# Scenarios

Raw backlog. A scenario is a short scripted mix of existing events, played as one haunt by an older or angrier spirit.
Single events stay the vocabulary (see [design.md](design.md)); a scenario only orders them, so it needs no new event
code, only a sequencer on top of `Scheduler`.

## How a scenario plays

- **Gate.** A scenario is picked only when the spirit's `power` is at least the gate (`0.4`, `0.6`, `0.7`, `0.8`),
  or once it has evolved (`evo`). Below the gate it never runs, so early haunts stay single events.
- **Pick.** The `Director` rolls a scenario like an event: weight from the spirit's affinity for the beats' tags, the
  `Where` condition must hold, and the first beat's `canRun` must pass. It counts as one event for the cooldown and
  for `Recent`.
- **Beats.** `+N` is seconds after the previous beat; `∥` runs with the previous beat; `×N` repeats; `→` waits for a
  condition (the player waking, reaching water). A later beat whose `canRun` fails is skipped, not retried.
- **Harm last.** A harmful beat never opens a scenario: an earlier light beat always announces it, as the single
  harmful events already do. The existing stops (3 hearts, light, water) still apply.
- **Abort.** Steps live in `Scheduler`, so death, logout or a closed world drops the rest of the scenario.
- **Hardcore.** Each beat follows its own hardcore rule; the scenario adds none.

`Spirits` lists the types whose affinities fit best; any spirit may roll it.

## Engine

`haunt/Scenario.java` is a `HauntEvent`, so the director, `/haunt fire`, the debug menu, wards and `Recent` treat it
as one event. Tier is harmful if any beat is; tags are every beat's. Beats keep the spirit's point and voice.

To add one: a line in `haunt/Scenarios.java` (script in the notation below), an entry in `scenarios.json`
(`weight`, `min_power`, `evolved`) and a name in `lang/en.json` and `lang/ru.json`. An unknown id, a harmful or
delayed opening, or an id taken by an event fails at startup. Done: all. `→` is written `>gone`, `>waking` or `>water` before the event. Scenario 12 has the id `unlocked_house` (`open_house` is an event).

The script knows `+N` and the waits; write `∥` as `+0` and `×N` as the beat N times. Not built yet, add when a scenario needs
it: `Where` beyond the first beat's `canRun`, the ×3 weight of a signature for its evolved type.

## Darkness

| # | Scenario | Beats | Gate | Where | Spirits |
|---|---|---|---|---|---|
| 1 | Lights out, one by one | `lantern_flicker` +2 `torch_chain` +3 `breathing` +3 `blackout` | 0.7 | indoors, night | shade, wraith |
| 2 | Playing with candles | `candles_out` +4 `candles_light` +4 `candles_out` +2 `ghost_laugh` | 0.4 | lit candles | trickster, shade |
| 3 | Behind you | `torches_behind` +3 `footsteps` +2 `ghost_breath` +1 `turned_head` | 0.7 | underground | wraith, shade |
| 4 | The hearth dies | `furnace_dies` ∥ `campfire_out` +3 `cold_breath` +3 `frostbite` | 0.7 | night | shade, wraith |
| 5 | Between blinks | `dimming` +4 `blinking` ∥ `edge_figure` | 0.4 | any | shade, phantom |
| 6 | Lamp code | `lamp_off` ×3 (2 s apart) +3 `three_knocks` +2 `lever` | 0.4 | redstone lamps | poltergeist, domovoi |
| 7 | Soul fire | `torch_swap` +2 `lantern_flicker` +2 `subliminal_title` +3 `sanity_drain` | 0.6 | torches, lanterns | trickster, mimic |
| 8 | Pocket dark | `held_light_out` +1 `darkness` +2 `close_whispers` +3 `cold_touch` | 0.7 | underground, light in hand | wraith |
| 9 | The path of lights | `torch_line` +6 `music_cut` +4 `fog_silhouette` | 0.4 | forest, night | lost_one, trickster |
| 10 | Jack o'lanterns | `new_face` ×3 (2 s apart) +3 `distant_walker` | 0.4 | carved pumpkins, night | trickster |

## The house

| # | Scenario | Beats | Gate | Where | Spirits |
|---|---|---|---|---|---|
| 11 | Knock, knock | `three_knocks` +3 `door` +2 `approaching_steps` +2 `breathing` | 0.4 | house, night | domovoi, poltergeist |
| 12 | Open house | `breathing_door` +5 `open_house` +2 `distant_mob` | 0.7 | house, night | poltergeist |
| 13 | Locked in | `three_knocks` +2 `locked_door` +1 `breathing` +3 `candles_out` +2 `cold_touch` | 0.7 | house | demon, wraith |
| 14 | Rearranged | `pot_wobble` +2 `shuffled_books` +3 `new_painting` +3 `furniture_moves` +2 `ghost_laugh` | 0.7 | house | poltergeist, domovoi |
| 15 | Midnight kitchen | `chest_lid` +2 `eating_behind_wall` +3 `wrong_smelt` +2 `hunger_pang` | 0.4 | house with a furnace | hoarder, domovoi |
| 16 | A visitor | `doorstep_gift` +2 `muddy_footprints` +4 `three_knocks` +4 `door` | 0.4 | house | domovoi, lost_one |
| 17 | Trap room | `pressure_plate` +1 `dispenser_shot` +2 `ghost_laugh` | 0.7 | dispensers, plates | trickster, poltergeist |
| 18 | Ticking chest | `chest_opens` +3 `chest_lid` +3 `chest_blast` | 0.7 | storage room | demon, trickster |
| 19 | Nothing works | `lever` +1 `button` +1 `iron_door` +2 `redstone_drift` +3 `jukebox_pop` | 0.7 | redstone base | poltergeist |
| 20 | Last song | `phantom_jukebox` +6 `jukebox_pop` +3 `crying_child` | 0.4 | house, jukebox | banshee, domovoi |

## Strangers

| # | Scenario | Beats | Gate | Where | Spirits |
|---|---|---|---|---|---|
| 21 | Company | `stranger_joins` +20 `distant_player` +30 `pillar` +20 `stranger_death` | 0.4 | outdoors | phantom, lost_one |
| 22 | Someone at my crops | `stranger_joins` +10 `harvest` +5 `lost_growth` | 0.4 | farm | phantom |
| 23 | Closing in | `distant_walker` +5 `approaching_steps` +3 `edge_figure` | 0.6 | outdoors | lost_one, phantom |
| 24 | Gone in the fog | `fog_silhouette` → gone +2 `footsteps` +2 `ghost_breath` | 0.4 | rain or night | lost_one |
| 25 | A name below | `nametag` +4 `stranger_joins` +3 `whisper` | 0.4 | underground | phantom |
| 26 | The other builder | `pillar` +8 `many_breaks` +3 `cave_sign` | 0.4 | outdoors | phantom, lost_one |
| 27 | Died out there | `distant_walker` +10 `cut_off_scream` +2 `stranger_death` +5 `mock_grave` | 0.7 | outdoors | banshee, phantom |
| 28 | The dead party | `stranger_joins` ×3 (5 s apart) +10 `distant_player` ∥ `fog_silhouette` +20 `stranger_death` ×3 | 0.8 | night | phantom |
| 29 | Friend far off | `friend_double` +4 `whisper` +3 `fake_subtitle` | 0.6 | another player near | phantom, mimic |
| 30 | Statue game | `footsteps` +3 `armor_stand` +6 `footsteps` +4 `turned_head` | 0.7 | any | mimic, phantom |

## The double

| # | Scenario | Beats | Gate | Where | Spirits |
|---|---|---|---|---|---|
| 31 | Your armor fits it | `ghost_breath` +2 `armor_off` +3 `ghost_laugh` +3 `copycat` | 0.7 | wearing armor | revenant, mimic |
| 32 | Who said that | `own_voice` +3 `echo` +2 `own_voice` +2 `subliminal_title` | 0.4 | any | mimic |
| 33 | Your grave | `mock_grave` +3 `edge_figure` +4 `false_death` | 0.7 | outdoors | banshee, revenant |
| 34 | Behind the glass | `false_windows` +3 `glass_wall` +3 `edge_figure` | 0.6 | house with windows | mimic |
| 35 | Same steps | `footprints` +3 `fake_subtitle` +3 `edge_figure` | 0.4 | any | phantom, mimic |

## Sleep and night

| # | Scenario | Beats | Gate | Where | Spirits |
|---|---|---|---|---|---|
| 36 | Someone in my bed | `breathing` +3 `occupied_bed` +4 `heaviness` +3 `phantom_heartbeat` | 0.6 | bed near, night | mara |
| 37 | Walking in sleep | `close_whispers` +3 `sleepwalk` → waking `muddy_footprints` +3 `cave_sign` | 0.7 | bed | mara |
| 38 | Bad dream | `dream` → waking `false_night` +4 `close_whispers` | 0.4 | bed | mara, banshee |
| 39 | No rest | `phantom_heartbeat` +2 `insomnia` +2 `whisper` +5 `occupied_bed` | 0.7 | night | mara |
| 40 | Shattered sleep | `phantom_heartbeat` +3 `broken_bed` +2 `ghost_laugh` | 0.7 | bed | poltergeist, mara |
| 41 | Night at noon | `false_night` +2 `circling_moan` +4 `silent_lightning` +3 `distant_mob` | 0.4 | day, outdoors | shade |
| 42 | Lost hour | `blinking` +1 `lost_time` +2 `whisper` +3 `sanity_drain` | 0.7 | any | lost_one, mara |
| 43 | Sleep paralysis | `darkness` +1 `trip` +2 `ghost_breath` +2 `subliminal_title` | 0.7 | night | mara |

## Underground

| # | Scenario | Beats | Gate | Where | Spirits |
|---|---|---|---|---|---|
| 44 | The other miner | `distant_mining` +5 `many_breaks` +3 `cave_sign` | 0.4 | underground | lost_one |
| 45 | Overhead | `spider_overhead` +3 `ceiling_crack` +3 `cracking_floor` | 0.7 | underground | wraith, demon |
| 46 | Floor gives way | `block_breaking` +2 `cracking_floor` +2 `shifting_floor` +2 `shove` | 0.7 | at a ledge | demon |
| 47 | Deep calling | `ladder_steps` +3 `whisper` +4 `the_call` | 0.8 | below y 0, lava near | lost_one, demon |
| 48 | Nest | `block_breaking` +2 `infested_stone` +3 `infested` | 0.7 | underground | demon |
| 49 | Lava tears | `lava_leak` +3 `scorch` +3 `burning_hand` | 0.7 | near lava | demon |
| 50 | No way back | `torches_behind` +2 `approaching_steps` +2 `sealed_tunnel` | 0.7 | tunnel | wraith, hoarder |
| 51 | Buried alive | `cave_sign` +4 `walled_in` +3 `suffocation` | 0.8 | underground, dark | demon, wraith |
| 52 | Ancient city | `shrieker` +3 `phantom_heartbeat` +3 `darkness` | 0.4 | deep dark | wraith |
| 53 | Hole in the wall | `block_blink` +2 `wall_hole` +2 `distant_mob` | 0.7 | underground | trickster |

## Water

| # | Scenario | Beats | Gate | Where | Spirits |
|---|---|---|---|---|---|
| 54 | Dead lake | `drowned_fish` +3 `guardian_face` +3 `undertow` | 0.7 | in water | nix |
| 55 | Adrift | `boat_drifts` +4 `local_rain` +3 `broken_boat` | 0.4 | boats near | nix |
| 56 | Voice from the water | `dripping` +2 `close_whispers` +3 `water_call` | 0.7 | near water | nix |
| 57 | Frozen pond | `ice_over` +2 `cold_breath` +2 `frostbite` | 0.7 | near water | nix, wraith |
| 58 | Leaky roof | `leak` +3 `dripping` +3 `dry_cauldron` | 0.4 | house | nix, domovoi |

## Animals and the village

| # | Scenario | Beats | Gate | Where | Spirits |
|---|---|---|---|---|---|
| 59 | Into the corner | `animals_stare` +3 `pen_corner` +15 `open_pen` | 0.7 | farm pen | trickster |
| 60 | The dog sees it | `dog_growls` +3 `footsteps` +2 `cold_touch` | 0.7 | tamed wolf | revenant |
| 61 | Cats know | `cat_flee` +2 `cat_hiss` +3 `edge_figure` | 0.4 | cats | shade |
| 62 | Not my dog | `dog_barks` +3 `dog_growls` +4 `wild_pet` | 0.8 | pet | revenant |
| 63 | Swarm | `bats` +3 `spider_overhead` +3 `angry_bees` | 0.7 | hive near | demon, revenant |
| 64 | Wrong herd | `disguise` +3 `animals_stare` +3 `hostile_animal` | 0.4 | animals | mimic |
| 65 | The golem mourns | `golem_flower` +4 `distant_bell` +3 `villager_stares` | 0.4 | village | banshee, lost_one |
| 66 | The golem turns | `villager_refuses` +3 `golem_flower` +4 `golem_rage` | 0.8 | village | revenant, demon |
| 67 | Stolen block | `enderman_hiss` +3 `enderman_takes` +3 `wall_hole` | 0.7 | enderman near | hoarder, trickster |
| 68 | Runaway cart | `distant_mining` +3 `minecart_rolls` +2 `trip` | 0.7 | rails | poltergeist |
| 69 | Pack | `circling_moan` +3 `wolf_rage` | 0.8 | wolves near | revenant, demon |
| 70 | Billy goat | `animals_stare` +2 `leash_snaps` +2 `goat_rams` | 0.7 | goats, leashed mob | trickster |

## Lies

| # | Scenario | Beats | Gate | Where | Spirits |
|---|---|---|---|---|---|
| 71 | You died | `false_wound` +1 `false_hearts` +2 `false_death` | 0.7 | any | mimic, trickster |
| 72 | Saved by nothing | `false_wound` +2 `false_totem` +3 `ghost_laugh` | 0.4 | any | trickster |
| 73 | Achievement get | `false_advancement` +2 `subliminal_title` +2 `own_voice` | 0.4 | any | trickster |
| 74 | Starving | `false_hunger` +2 `eating_behind_wall` +3 `ravenous` | 0.4 | any | hoarder |
| 75 | What's in my pocket | `false_slot` +3 `swap_slots` +3 `hotbar_shift` | 0.4 | any | trickster, hoarder |
| 76 | Silence | `music_cut` +3 `fake_subtitle` +3 `breathing` | 0.4 | any | banshee, shade |
| 77 | Seeing red | `red_vignette` +2 `phantom_heartbeat` +3 `wrath` | 0.7 | any | demon |
| 78 | The sky lies | `dragon_flyover` +5 `silent_lightning` +3 `distant_mob` | 0.4 | outdoors | mimic |
| 79 | Mind slipping | `blinking` +2 `sanity_drain` +3 `false_looks` +3 `sanity_crash` | 0.8 | any | shade, mara |

## Belongings and land

| # | Scenario | Beats | Gate | Where | Spirits |
|---|---|---|---|---|---|
| 80 | Hoard | `shuffle_chest` +3 `vanishing_item` +3 `chest_opens` | 0.7 | storage room | hoarder |
| 81 | Butterfingers | `hotbar_shift` +2 `snatch` +2 `ghost_laugh` | 0.7 | item in hand | hoarder, poltergeist |
| 82 | X marks the spot | `map_marker` +3 `false_compass` +3 `item_trail` | 0.7 | map and compass | lost_one |
| 83 | The journal | `book_page` +3 `lectern_pages` +2 `close_whispers` | 0.4 | lectern | banshee, domovoi |
| 84 | Blight | `dead_ground` +3 `withering_crops` +3 `rot` | 0.7 | farm | demon |
| 85 | Funeral | `wilted_flowers` +3 `cross` +3 `death_cry` | 0.4 | potted flowers | banshee |
| 86 | Walking woods | `ghost_tree` +5 `moving_tree` +3 `footsteps` | 0.4 | forest | trickster, mimic |

## Evolved signatures

One per evolved type: its longest and worst scenario, `evo` only and weighted ×3 for that type.

| # | Scenario | Beats | Gate | Where | Spirits |
|---|---|---|---|---|---|
| 87 | Tantrum | `breathing_door` +2 `open_house` +2 `furniture_moves` +3 `snatch` +2 `chest_blast` | evo | house | raging poltergeist |
| 88 | Supper | `extinguish_light` +1 `eating_behind_wall` +2 `torch_chain` +1 `eating_behind_wall` +3 `blackout` | evo | indoors or underground | light-eater (shade) |
| 89 | Replacement | `footprints` +3 `own_voice` +2 `edge_figure` +3 `false_death` +2 `double` | evo | any | doppelganger (phantom) |
| 90 | Taken under | `close_whispers` +3 `water_call` → in water `undertow` +4 `drowned_fish` | evo | near water | drowner (nix) |
| 91 | Wearing faces | `disguise` +3 `hostile_animal` +3 `hallucination` +3 `armor_off` +2 `copycat` | evo | animals near | skinwalker (mimic) |
| 92 | Lights at your back | `torches_behind` +2 `held_light_out` +2 `approaching_steps` +2 `shadow` | evo | underground | dread wraith |
| 93 | Rider | `occupied_bed` +3 `breathing` +3 `heaviness` +2 `darkness` +3 `embrace` | evo | bed near, night | night hag (mara) |
| 94 | Foretold | `distant_bell` +4 `banshee` +6 `cut_off_scream` +3 `false_death` +5 `mock_grave` | evo | any | keening banshee |
| 95 | Blood for blood | `death_cry` +2 `red_vignette` +2 `copycat` ∥ `wild_pet` +3 `wrath` | evo | after a kill | avenger (revenant) |
| 96 | The way they went | `muddy_footprints` +3 `item_trail` +5 `torch_line` +10 `stranger_death` +2 `lost_time` | evo | outdoors | forsaken (lost one) |
| 97 | House rules | `door` +1 `trapdoor` +1 `fence_gate` +1 `button` +1 `lever` +2 `locked_door` +3 `shuffled_books` | evo | house | house master (domovoi) |
| 98 | Feast | `false_hunger` +2 `eating_behind_wall` +2 `rot` +3 `wrong_smelt` +3 `vanishing_item` | evo | house | glutton (hoarder) |
| 99 | The debt | `red_vignette` +2 `blackout` +3 `wrath` +3 `the_call` | evo | near lava or void | archdemon |
| 100 | Punchline | `false_advancement` +1 `false_totem` +2 `chest_lid` +2 `chest_blast` +2 `ghost_laugh` | evo | storage room | jester (trickster) |
