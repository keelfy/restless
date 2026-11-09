# Events index

Map of every haunt event id to the class that defines it: 638 events + 104 scenarios. Player-facing
descriptions live in the wiki (`apps/frontend/content/<lang>/events/*.md`); this file is for finding code.

## How an event is wired

Paths below are under `apps/mod/core/src/main/java/dev/keelfy/restless/haunt/` unless they start with `apps/`.

| What | Where |
|---|---|
| Contract (`id`, `tier`, `tags`, `canRun`, `run`, weight from config) | `HauntEvent.java` |
| Registry: 6 class events + `Catalog`, then scenarios | `HauntEvents.java` |
| Theme groups (each has `static List<HauntEvent> all()`) | `event/Catalog.java` → `event/<Group>.java` |
| Lambda event helper (`SimpleEvent.light` / `.harmful`) | `event/SimpleEvent.java` |
| Tier: `LIGHT` scare / `HARMFUL` hurts | `HauntTier.java` |
| Tags (spirit affinity, wiki category) | `EventTag.java` |
| Picks which event runs | `Director.java` |
| Damage bands: safe / threat / brink / lethal | `Band.java`, `Bands.java`, `docs/assaults.md` |
| World changes undone later | `Restore.java`, `event/Swaps.java` (client-only blocks), `event/PacketEntity.java` (client-only mobs) |
| Tunables, `events.<namespace:id>` (`weight` + event keys) | `apps/mod/core/src/main/resources/restless-config/<group>.json`, read via `Config.event(id)` |
| Scenarios: scripts of beats | `restless-config/scenarios.json` (`script`), loaded by `Scenarios.java`; `docs/scenarios.md` |
| Wiki page per tag | `apps/frontend/content/{en,ru}/events/<tag>.md` |
| Debug run | `/haunt force <id>` |

Adding an event: define it in the fitting group, add its config entry with `weight`, document it on the wiki page of
its tag in every language, update counts in the wiki `events/index.md` and here.

Most ids are plain (`held_light_out`, parsed as `minecraft:` namespace, so config keys are `minecraft:<id>`); some groups
use `restless:<id>`. Grep both `"<id>"` and `"restless:<id>"`.

## Events by class

`[wiki tag]` is the wiki page the event is listed on.

### `event/Sounds.java` (67)

Sounds with no source; never touch the world. [sound]

`footsteps`, `creeper_hiss`, `three_knocks`, `crying_child`, `block_breaking`, `eating_behind_wall`, `enderman_hiss`, `chest_opens`, `breathing`, `distant_bell`, `phantom_jukebox`, `music_cut`, `whisper`, `echo`, `death_cry`, `dripping`, `distant_mob`, `hostile_animal`, `approaching_steps`, `distant_mining`, `many_breaks`, `ladder_steps`, `spider_overhead`, `cracking_floor`, `bottle_shatter`, `shrieker`, `fake_subtitle`, `phantom_heartbeat`, `note_block_voice`, `ghost_laugh`, `ghost_breath`, `close_whispers`, `circling_moan`, `cut_off_scream`, `long_scream`, `banshee`, `distant_warden`, `sky_dragon`, `distant_wither`, `end_portal_open`, `sky_ghast`, `fox_screech`, `rabbit_death`, `phantom_hurt`, `phantom_tool_break`, `elytra_overhead`, `nether_ambience`, `equip_behind`, `phantom_fishing`, `zombie_at_door`, `phantom_anchor`, `bucket_spill`, `lost_villager`, `phantom_arrow`, `phantom_glass`, `pen_cries`, `sniffing_behind`, `distant_goat_horn`, `false_raid_horn`, `heavy_splash`, `distant_ravager`, `golem_circling`, `burp_behind`, `bubble_columns`, `lava_pops`, `chain_rattle`, `phantom_break`

### `event/MobEvents.java` (36)

Mobs reacting to the unseen or turning on player. [mobs, water]

`animals_stare`, `pen_corner`, `cat_hiss`, `cat_flee`, `dog_barks`, `villager_stares`, `villager_refuses`, `bats`, `open_pen`, `wild_pet`, `copycat`, `golem_flower`, `golem_rage`, `wolf_rage`, `dog_growls`, `angry_bees`, `leash_snaps`, `goat_rams`, `minecart_rolls`, `enderman_takes`, `parrot_echo`, `allay_bone`, `fox_snatch`, `armadillo_curls`, `sheep_blacken`, `horse_bolts`, `axolotl_plays_dead`, `cat_gift`, `chickens_look_up`, `cows_face_anchor`, `villager_nitwit`, `villager_prices`, `bees_stay_out`, `fish_face`, `snowman_melts`, `boat_drifts`

### `event/Lies.java` (34)

Client-only false packets, truth resent later. [affliction, presence, sound]

`false_wound`, `false_hearts`, `false_hunger`, `guardian_face`, `false_totem`, `subliminal_title`, `red_vignette`, `false_death`, `private_rain`, `silent_lightning`, `false_night`, `false_slot`, `false_advancement`, `turned_head`, `stranger_joins`, `stranger_death`, `false_whisper`, `pumpkin_head`, `xp_drain`, `phantom_flames`, `phantom_drowning`, `phantom_frost`, `moon_phase`, `false_boss_bar`, `action_bar_whisper`, `false_absorption`, `empty_armor_bar`, `hurt_tilt`, `own_hand_swings`, `night_eyes`, `all_on_cooldown`, `noon_moon`, `own_voice`, `dead_silence`

### `event/BlockEvents.java` (32)

Small block changes, restored or hand-undoable. [blocks]

`wilted_flowers`, `withering_crops`, `lost_growth`, `turning_frame`, `new_sign`, `new_painting`, `glass_wall`, `dry_cauldron`, `dead_ground`, `shifting_floor`, `broken_bed`, `wall_hole`, `cross`, `infested_stone`, `ceiling_crack`, `ice_over`, `portal_out`, `block_blink`, `leak`, `lava_leak`, `mock_grave`, `cave_sign`, `item_trail`, `repaint`, `lava_cauldron`, `cauldron_fills`, `turning_terracotta`, `end_rod_gaze`, `composter_swing`, `banner_skull`, `stairs_flip`, `lectern_swap`

### `event/ItemEvents.java` (33)

Inventory and nearby chests. [items]

`swap_slots`, `hotbar_shift`, `book_page`, `shuffle_chest`, `wear`, `rot`, `tool_look`, `armor_off`, `vanishing_item`, `false_compass`, `map_marker`, `elytra_flicker`, `stew_bowl`, `bucket_pufferfish`, `torch_burn`, `bundle_bone`, `shield_ghost`, `item_throwback`, `phantom_horn`, `armor_dye`, `item_glint`, `item_lore`, `tool_mirage`, `lodestone_lure`, `ghost_author`, `potion_swap`, `vanishing_arrows`, `crossbow_misfire`, `map_elsewhere`, `shy_item`, `orb_scatter`, `hand_drop`, `inventory_drop`

### `event/Mechanisms.java` (23)

Doors, gates, buttons, levers. [doors]

`breathing_door`, `trapdoor`, `fence_gate`, `button`, `lever`, `pressure_plate`, `iron_door`, `open_house`, `chest_lid`, `barrel_opens`, `jukebox_pop`, `locked_door`, `hook_click`, `daylight_flip`, `rod_sparks`, `torch_burnout`, `chests_open`, `piston_twitch`, `hopper_swallows`, `sculk_chain`, `observer_pulse`, `bed_missing`, `ender_note`

### `event/Afflictions.java` (22)

Status effects on the body; never kill. [affliction]

`nausea`, `heaviness`, `fatigue`, `hunger_pang`, `darkness`, `shudder`, `poison`, `weakness`, `levitation`, `frostbite`, `drained`, `bad_luck`, `sanity_crash`, `sanity_drain`, `insomnia`, `dream`, `sleepwalk`, `ravenous`, `infested`, `oozing`, `weaving`, `drift`

### `event/Household.java` (16)

Furniture and redstone acting alone. [doors]

`lectern_pages`, `pot_wobble`, `bell_swing`, `redstone_drift`, `occupied_bed`, `shuffled_books`, `dispenser_shot`, `chest_blast`, `wrong_smelt`, `furniture_moves`, `doorstep_gift`, `muddy_footprints`, `anvil_lands`, `cake_bite`, `tnt_hiss`, `pot_holds`

### `event/Torments.java` (16)

Threat-band assaults down to `assaults.safe_health`. [assault]

`rake`, `teeth_in_dark`, `searing_armor`, `frost_grip`, `pinned`, `throat`, `undertow_dry`, `lantern_burst`, `blood_cough`, `wrong_step`, `ash_storm`, `hand_below`, `blade_behind`, `door_bite`, `night_terror`, `stone_fists`

### `event/Lights.java` (15)

Fire, torches, candles, furnaces. [lights]

`held_light_out`, `candles_out`, `torch_chain`, `torches_behind`, `lamp_off`, `lantern_flicker`, `campfire_out`, `furnace_dies`, `dimming`, `blinking`, `candles_light`, `new_face`, `blackout`, `burning_hand`, `torch_swap`

### `event/Brinks.java` (14)

Down to `assaults.brink_health`; strong spirits only; never kill. [assault]

`final_blow`, `the_hanging`, `held_under`, `pyre`, `withering`, `count`, `the_hollow`, `ledge_push`, `circle_of_knives`, `smother_brink`, `embrace_brink`, `red_room`, `drain`, `bound`

### `event/Creatures.java` (14)

Animals/monsters acting without cause. [mobs]

`frogs_croak`, `ink_cloud`, `rabbits_stream`, `witch_giggles`, `hostiles_retreat`, `creeper_calms`, `endermen_stare`, `llama_spits`, `goat_screams`, `bats_hang`, `sniffer_digs_up`, `cat_at_anchor`, `wind_burst`, `mobs_go_still`

### `event/Nether.java` (14)

Nether-only (`Where#nether`). [blocks, mobs]

`skulls_turn`, `anchor_charge`, `vines_reach`, `piglins_stare`, `piglin_barter`, `ghast_cry`, `lava_figure`, `strider_shiver`, `soul_fire_lean`, `still_blaze`, `magma_hop`, `steady_compass`, `soul_sand_drag`, `piglins_angered`

### `event/Strangers.java` (14)

Fake other player + chat. [presence]

`roof_figure`, `hill_waver`, `wall_miner`, `table_figure`, `cave_torches`, `own_walker`, `night_runner`, `bed_sleeper`, `shrieker_figure`, `hello_chat`, `ghost_teleport`, `stranger_advancement`, `grave_figure`, `bedside_figure`

### `event/Mirages.java` (13)

Blocks not what they seem (Swaps). [blocks]

`bricked_door`, `boarded_windows`, `stair_wall`, `yard_pit`, `unlit_torches`, `fake_ore`, `ladder_stub`, `second_door`, `false_harvest`, `lava_pond`, `lava_as_water`, `roof_snow`, `black_glass`

### `event/Relics.java` (13)

Mod data: paintings, banners, trims, songs. [blocks, items, mobs, sound]

`haunted_painting`, `painting_darkens`, `sigil_banner`, `whispering_item`, `hollow_trim`, `pale_pet`, `pale_herd`, `spirit_song`, `spirit_horn`, `counting_children`, `wall_scratching`, `chest_knocks`, `bedroom_lullaby`

### `event/Ambushes.java` (12)

Threat-band assaults with warning + counter. [assault]

`hurled_block`, `flung_tools`, `volley`, `cave_in`, `dripstone_rain`, `possessed_mob`, `frenzy`, `turned_pets`, `vex_swarm`, `ghost_creeper`, `stalker`, `hail_of_bones`

### `event/Fixtures.java` (12)

Stands, beacons, spawners, vaults, beds (block-entity lies). [blocks, doors, presence, sound]

`beacon_out`, `campfire_cooks`, `spawner_shows`, `trial_ominous`, `vault_head`, `black_bed`, `black_carpets`, `brewing_swap`, `brewing_bubbles`, `statue_pose`, `black_ash`, `note_tune`

### `event/Wilds.java` (12)

Biome-bound: desert, swamp, snow, mountains, sea. [assault, blocks, mobs, sound, water]

`snow_snowball`, `snow_powder_path`, `mountain_wind_push`, `desert_sand_trickle`, `cauldron`, `fireflies_dark`, `snow_door_drift`, `desert_husk_haze`, `desert_plate_click`, `mountain_avalanche`, `ocean_ship_bell`, `swamp_lily_drift`

### `event/Habitats.java` (11)

Jungle, pale garden, dark forest, deep dark. [blocks, mobs, presence, sound]

`window_vines`, `eyeblossoms_open`, `resin_clumps`, `canopy_closes`, `sculk_creep`, `berries_dark`, `boat_grabbed`, `parrots_scatter`, `mooshroom_brown`, `black_petals`, `creaking_heart_call`

### `event/Hallucinations.java` (11)

Client-only mobs (PacketEntity). [mobs]

`creeper_behind`, `drowned_rises`, `ghost_horse`, `bed_cat`, `child_runs`, `doorway_enderman`, `forest_wolves`, `ceiling_spider`, `bat_swarm`, `window_zombie`, `ghast_drift`

### `event/Assaults.java` (10)

Direct physical harm; player survives. [assault]

`trip`, `scorch`, `wrath`, `snatch`, `shove`, `toss`, `suffocation`, `thunder_knocks`, `embrace`, `the_call`

### `event/Followers.java` (10)

Fake other player as a mannequin. [presence]

`lake_fisher`, `window_figure`, `trailing_figure`, `deep_swimmer`, `note_dropper`, `horizon_choir`, `blade_stalker`, `cliff_faller`, `lake_ferry`, `cave_cart`

### `event/Rites.java` (10)

The mod's own wards, tools, gravestone, journal, altar misbehaving. [blocks]

`scarecrow_closer`, `ouija_hi`, `spirit_box_word`, `salt_gap`, `grave_name`, `journal_page_torn`, `dreamcatcher_fails`, `lantern_soul_fire`, `charm_falls`, `altar_midnight`

### `event/Jolts.java` (3)

Jump scares: a loud bang, then a dark figure for a second. Weights are the lowest of any group. [presence]

`jolt_behind`, `jolt_side`, `jolt_dark`

### `event/Sightings.java` (10)

Client-only entities in bad spots. [items, mobs, presence, water]

`allay_thief`, `roof_skeleton`, `far_warden`, `swamp_witch`, `wall_shulker`, `forest_creaking`, `cow_herd_blink`, `spyglass_figure`, `flash_figure`, `water_guardian`

### `event/TheEnd.java` (10)

End wrongness + one Overworld portal. [blocks, mobs]

`void_wind`, `gateway_beam`, `chorus_shift`, `phantom_egg`, `ender_chests_open`, `nowhere_portal`, `endermen_turn`, `shulker_peeks`, `empty_pillar_crystal`, `enderman_home_block`

### `event/Veils.java` (10)

More client-only block disguises (Swaps). [blocks]

`red_wool`, `sign_whisper`, `wither_roses`, `block_flicker`, `lone_gravestone`, `broken_portal`, `dark_circle`, `fence_gaps`, `corner_cobwebs`, `redstone_trail`

### `event/Fittings.java` (9)

Decals and props in the house. [blocks, doors, lights, presence, sound]

`rocking_chair`, `rotten_planks`, `cracked_mirror`, `claw_marks`, `black_candle`, `frost_prints`, `frost_writing`, `doll`, `music_box`

### `event/Machines.java` (9)

Crafter, golem, jukebox, map, candle, mirror, last chat line. [blocks, items, lights, mobs, sound]

`crafter_crafts`, `scaffold_collapses`, `wall_map_shows`, `holy_water_cloudy`, `mirror_names`, `spirit_candle_out`, `golem_wrong_chest`, `jukebox_quiet`, `chat_repeats`

### `event/Herds.java` (8)

Animals/villagers gathering where they should not. [items, mobs]

`pearl_strays`, `villagers_at_windows`, `strange_trader`, `golem_in_corner`, `dolphins_lead`, `spiders_at_window`, `squid_goes_dark`, `hoglins_charge`

### `event/Lethals.java` (8)

Can kill; cruelest evolved spirits only; telegraphed. [assault]

`judgement`, `reaping`, `last_call`, `drowner`, `the_avenger`, `hellring`, `the_double`, `foretold`

### `event/Illusions.java` (7)

Client-only blocks/mobs (real under hardcore). [blocks, mobs, presence]

`false_windows`, `false_looks`, `ghost_tree`, `torch_line`, `hallucination`, `disguise`, `dragon_flyover`

### `event/Remodels.java` (7)

Surroundings rebuilt out of view (Swaps). [blocks, water]

`netherrack_behind`, `roof_gone`, `path_rearranged`, `landmark_tree_gone`, `tunnel_filled`, `ceiling_furniture`, `sea_lights`

### `event/Tricks.java` (5)

Client tricks: border, toast, FOV, fishing rod. [items, presence]

`line_pulls`, `odd_catch`, `border_closes`, `false_recipe`, `view_narrows`

### `event/Apparitions.java` (5)

Client-only mobs (PacketEntity). [mobs]

`vex_through_wall`, `phantom_swoop`, `dead_on_path`, `llama_caravan`, `giant_slime`

### `event/Presence.java` (5)

The sense that something is there. [presence]

`nametag`, `footprints`, `cold_breath`, `local_rain`, `lost_time`

### `event/PresenceFigure.java` (5)

Figure glimpsed at the edge (Presence helper). [presence]

`edge_figure`, `fog_silhouette`, `distant_player`, `friend_double`, `distant_walker`

### `event/Screens.java` (5)

Overlays on view and photos (needs client mod). [items, presence]

`photo_figure`, `screen_handprint`, `screen_frost`, `screen_face`, `eyes_in_dark`

### `event/Shrines.java` (5)

Camps/huts/shrines left behind (Templates). [blocks, lights, presence]

`abandoned_camp`, `dirt_hut`, `far_house`, `overnight_shrine`, `ash_sign`

### `event/Cameras.java` (4)

View jumps to another entity (Eyes). [doors, presence]

`open_door_view`, `mob_eyes`, `out_of_body`, `spirit_sight`

### `event/Waters.java` (4)

Water spirit: drowning, lure, empty water. [water]

`undertow`, `water_call`, `drowned_fish`, `broken_boat`

### `event/Seals.java` (2)

Player walled in or way back sealed. [blocks]

`walled_in`, `sealed_tunnel`

### `event/ArmorStandEvent.java` (1)

[presence]

`armor_stand`

### `event/BlindnessEvent.java` (1)

[affliction]

`blindness`

### `event/ColdTouchEvent.java` (1)

[assault]

`cold_touch`

### `event/DoorEvent.java` (1)

[doors]

`door`

### `event/DoubleEvent.java` (1)

[mobs]

`double`

### `event/ExtinguishLightEvent.java` (1)

[lights]

`extinguish_light`

### `event/HarvestEvent.java` (1)

[presence]

`harvest`

### `event/PillarEvent.java` (1)

[presence]

`pillar`

### `event/ShadowEvent.java` (1)

[assault]

`shadow`

### `event/Trees.java` (1)

A whole tree moved. [blocks]

`moving_tree`

### `event/Signatures.java` (10), and the groups it gathers (33)

Events only one spirit type rolls (`spirit` in the config); see [signature-events.md](signature-events.md). `Signatures.all()`
returns its own ten and each group's. [various]

`phantom_self_join`, `nix_catch`, `nix_call`, `mimic_voice`, `wraith_taken`, `domovoi_supper`, `domovoi_hearth`, `hoarder_payment`, `demon_offer`, `lost_one_sign`

### `event/Suitors.java` (4)

The banshee: letters and gifts on the bed, a song at the window, jealousy. [ITEMS, SOUND, MOBS]

`banshee_letter`, `banshee_gift`, `banshee_serenade`, `banshee_jealous`

### `event/Pranks.java` (6)

The poltergeist's games and tantrums, and the trickster's jokes. [ITEMS, DOORS, BLOCKS, MOBS]

`poltergeist_toss`, `poltergeist_play`, `poltergeist_tantrum`, `trickster_gift`, `trickster_dig`, `trickster_dud`

### `event/Dusk.java` (4)

The shade eats light, the wraith walks in the dark. [LIGHTS, PRESENCE]

`shade_gathered`, `shade_glance`, `shade_room`, `wraith_steps`

### `event/Doubles.java` (5)

Figures and prints: the phantom's copy, the wraith at the edge of the light, the lost one's wanderer and death path. Needs
`Trail` (the last 30 s of positions). [PLAYER, PRESENCE]

`phantom_rerun`, `phantom_home`, `wraith_edge`, `lost_one_wanderer`, `lost_one_death_path`

### `event/Copies.java` (2)

The mimic gives back what it took. Needs `Overheard` (the last chat lines). [PLAYER, BLOCKS]

`mimic_parrot`, `mimic_chest`

### `event/Nights.java` (6)

The mara on waking (needs `Wakes`) and the revenant's tally of kills (needs `Kills`). [AFFLICTION, ITEMS, MOBS]

`mara_dream`, `mara_stolen_rest`, `mara_weight`, `revenant_ledger`, `revenant_tally`, `revenant_rises`

### `event/Territories.java` (6)

What the territorial spirits do to a place. The hoarder's nest remembers what it took. [WATER, DOORS, ITEMS, BLOCKS, AFFLICTION]

`nix_returns`, `domovoi_tidy`, `hoarder_shiny`, `hoarder_nest`, `demon_trail`, `demon_contract`

## Scenarios (`scenarios.json`, 104)

Each is a script of the events above (`"a +2 b +3 c"`: event, delay in seconds, event). Scripts, gates and weights in
`restless-config/scenarios.json`; numbered design notes in `docs/scenarios.md`.

`lights_out`, `playing_with_candles`, `behind_you`, `hearth_dies`, `between_blinks`, `lamp_code`, `soul_fire`, `pocket_dark`, `the_path_of_lights`, `jack_o_lanterns`, `knock_knock`, `unlocked_house`, `locked_in`, `rearranged`, `midnight_kitchen`, `a_visitor`, `trap_room`, `ticking_chest`, `nothing_works`, `last_song`, `company`, `someone_at_my_crops`, `closing_in`, `gone_in_the_fog`, `a_name_below`, `the_other_builder`, `died_out_there`, `the_dead_party`, `friend_far_off`, `statue_game`, `your_armor_fits_it`, `who_said_that`, `your_grave`, `behind_the_glass`, `same_steps`, `someone_in_my_bed`, `walking_in_sleep`, `bad_dream`, `no_rest`, `shattered_sleep`, `night_at_noon`, `lost_hour`, `sleep_paralysis`, `the_other_miner`, `overhead`, `floor_gives_way`, `deep_calling`, `nest`, `lava_tears`, `no_way_back`, `buried_alive`, `ancient_city`, `hole_in_the_wall`, `dead_lake`, `adrift`, `voice_from_the_water`, `frozen_pond`, `leaky_roof`, `into_the_corner`, `the_dog_sees_it`, `cats_know`, `not_my_dog`, `swarm`, `wrong_herd`, `the_golem_mourns`, `the_golem_turns`, `stolen_block`, `runaway_cart`, `pack`, `billy_goat`, `you_died`, `saved_by_nothing`, `achievement_get`, `starving`, `what_s_in_my_pocket`, `silence`, `seeing_red`, `the_sky_lies`, `mind_slipping`, `hoard`, `butterfingers`, `x_marks_the_spot`, `the_journal`, `blight`, `funeral`, `walking_woods`, `tantrum`, `supper`, `replacement`, `taken_under`, `wearing_faces`, `lights_at_your_back`, `rider`, `foretold`, `blood_for_blood`, `the_way_they_went`, `house_rules`, `feast`, `the_debt`, `punchline`, `the_hunt`, `the_last_night`, `debt`, `drowning_season`
