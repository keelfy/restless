package dev.keelfy.restless.haunt;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.resources.Identifier;

/**
 * Every scenario, written as in {@code docs/scenarios.md} (number there in the comment). Each needs an entry in
 * {@code scenarios.json} (weight and gate) and a name in every language. A typo or a harmful opening fails at startup.
 */
final class Scenarios {
    private Scenarios() {}

    /** @param events the plain events, which are all a beat may name */
    static List<Scenario> all(List<HauntEvent> events) {
        Map<Identifier, HauntEvent> byId = events.stream().collect(Collectors.toMap(HauntEvent::id, e -> e));
        Function<Identifier, HauntEvent> lookup = id -> {
            HauntEvent event = byId.get(id);
            if (event == null) {
                throw new IllegalStateException("A scenario names no such event: " + id);
            }
            return event;
        };
        List<Scenario> all = List.of(
                // 1
                scenario("lights_out", "lantern_flicker +2 torch_chain +3 breathing +3 blackout", lookup),
                // 2
                scenario("playing_with_candles", "candles_out +4 candles_light +4 candles_out +2 ghost_laugh", lookup),
                // 3
                scenario("behind_you", "torches_behind +3 footsteps +2 ghost_breath +1 turned_head", lookup),
                // 4
                scenario("hearth_dies", "furnace_dies +0 campfire_out +3 cold_breath +3 frostbite", lookup),
                // 5
                scenario("between_blinks", "dimming +4 blinking +0 edge_figure", lookup),
                // 6
                scenario("lamp_code", "lamp_off +2 lamp_off +2 lamp_off +3 three_knocks +2 lever", lookup),
                // 7
                scenario("soul_fire", "torch_swap +2 lantern_flicker +2 subliminal_title +3 sanity_drain", lookup),
                // 8
                scenario("pocket_dark", "held_light_out +1 darkness +2 close_whispers +3 cold_touch", lookup),
                // 9
                scenario("the_path_of_lights", "torch_line +6 music_cut +4 fog_silhouette", lookup),
                // 10
                scenario("jack_o_lanterns", "new_face +2 new_face +2 new_face +3 distant_walker", lookup),
                // 11
                scenario("knock_knock", "three_knocks +3 door +2 approaching_steps +2 breathing", lookup),
                // 12
                scenario("unlocked_house", "breathing_door +5 open_house +2 distant_mob", lookup),
                // 13
                scenario("locked_in", "three_knocks +2 locked_door +1 breathing +3 candles_out +2 cold_touch", lookup),
                // 14
                scenario(
                        "rearranged",
                        "pot_wobble +2 shuffled_books +3 new_painting +3 furniture_moves +2 ghost_laugh",
                        lookup),
                // 15
                scenario("midnight_kitchen", "chest_lid +2 eating_behind_wall +3 wrong_smelt +2 hunger_pang", lookup),
                // 16
                scenario("a_visitor", "doorstep_gift +2 muddy_footprints +4 three_knocks +4 door", lookup),
                // 17
                scenario("trap_room", "pressure_plate +1 dispenser_shot +2 ghost_laugh", lookup),
                // 18
                scenario("ticking_chest", "chest_opens +3 chest_lid +3 chest_blast", lookup),
                // 19
                scenario("nothing_works", "lever +1 button +1 iron_door +2 redstone_drift +3 jukebox_pop", lookup),
                // 20
                scenario("last_song", "phantom_jukebox +6 jukebox_pop +3 crying_child", lookup),
                // 21
                scenario("company", "stranger_joins +20 distant_player +30 pillar +20 stranger_death", lookup),
                // 22
                scenario("someone_at_my_crops", "stranger_joins +10 harvest +5 lost_growth", lookup),
                // 23
                scenario("closing_in", "distant_walker +5 approaching_steps +3 edge_figure", lookup),
                // 24
                scenario("gone_in_the_fog", "fog_silhouette >gone +2 footsteps +2 ghost_breath", lookup),
                // 25
                scenario("a_name_below", "nametag +4 stranger_joins +3 whisper", lookup),
                // 26
                scenario("the_other_builder", "pillar +8 many_breaks +3 cave_sign", lookup),
                // 27
                scenario("died_out_there", "distant_walker +10 cut_off_scream +2 stranger_death +5 mock_grave", lookup),
                // 28
                scenario(
                        "the_dead_party",
                        "stranger_joins +5 stranger_joins +5 stranger_joins +10 distant_player +0 fog_silhouette +20 stranger_death +0 stranger_death +0 stranger_death",
                        lookup),
                // 29
                scenario("friend_far_off", "friend_double +4 whisper +3 fake_subtitle", lookup),
                // 30
                scenario("statue_game", "footsteps +3 armor_stand +6 footsteps +4 turned_head", lookup),
                // 31
                scenario("your_armor_fits_it", "ghost_breath +2 armor_off +3 ghost_laugh +3 copycat", lookup),
                // 32
                scenario("who_said_that", "own_voice +3 echo +2 own_voice +2 subliminal_title", lookup),
                // 33
                scenario("your_grave", "mock_grave +3 edge_figure +4 false_death", lookup),
                // 34
                scenario("behind_the_glass", "false_windows +3 glass_wall +3 edge_figure", lookup),
                // 35
                scenario("same_steps", "footprints +3 fake_subtitle +3 edge_figure", lookup),
                // 36
                scenario("someone_in_my_bed", "breathing +3 occupied_bed +4 heaviness +3 phantom_heartbeat", lookup),
                // 37
                scenario(
                        "walking_in_sleep",
                        "close_whispers +3 sleepwalk >waking muddy_footprints +3 cave_sign",
                        lookup),
                // 38
                scenario("bad_dream", "dream >waking false_night +4 close_whispers", lookup),
                // 39
                scenario("no_rest", "phantom_heartbeat +2 insomnia +2 whisper +5 occupied_bed", lookup),
                // 40
                scenario("shattered_sleep", "phantom_heartbeat +3 broken_bed +2 ghost_laugh", lookup),
                // 41
                scenario("night_at_noon", "false_night +2 circling_moan +4 silent_lightning +3 distant_mob", lookup),
                // 42
                scenario("lost_hour", "blinking +1 lost_time +2 whisper +3 sanity_drain", lookup),
                // 43
                scenario("sleep_paralysis", "darkness +1 trip +2 ghost_breath +2 subliminal_title", lookup),
                // 44
                scenario("the_other_miner", "distant_mining +5 many_breaks +3 cave_sign", lookup),
                // 45
                scenario("overhead", "spider_overhead +3 ceiling_crack +3 cracking_floor", lookup),
                // 46
                scenario("floor_gives_way", "block_breaking +2 cracking_floor +2 shifting_floor +2 shove", lookup),
                // 47
                scenario("deep_calling", "ladder_steps +3 whisper +4 the_call", lookup),
                // 48
                scenario("nest", "block_breaking +2 infested_stone +3 infested", lookup),
                // 49
                scenario("lava_tears", "lava_leak +3 scorch +3 burning_hand", lookup),
                // 50
                scenario("no_way_back", "torches_behind +2 approaching_steps +2 sealed_tunnel", lookup),
                // 51
                scenario("buried_alive", "cave_sign +4 walled_in +3 suffocation", lookup),
                // 52
                scenario("ancient_city", "shrieker +3 phantom_heartbeat +3 darkness", lookup),
                // 53
                scenario("hole_in_the_wall", "block_blink +2 wall_hole +2 distant_mob", lookup),
                // 54
                scenario("dead_lake", "drowned_fish +3 guardian_face +3 undertow", lookup),
                // 55
                scenario("adrift", "boat_drifts +4 local_rain +3 broken_boat", lookup),
                // 56
                scenario("voice_from_the_water", "dripping +2 close_whispers +3 water_call", lookup),
                // 57
                scenario("frozen_pond", "ice_over +2 cold_breath +2 frostbite", lookup),
                // 58
                scenario("leaky_roof", "leak +3 dripping +3 dry_cauldron", lookup),
                // 59
                scenario("into_the_corner", "animals_stare +3 pen_corner +15 open_pen", lookup),
                // 60
                scenario("the_dog_sees_it", "dog_growls +3 footsteps +2 cold_touch", lookup),
                // 61
                scenario("cats_know", "cat_flee +2 cat_hiss +3 edge_figure", lookup),
                // 62
                scenario("not_my_dog", "dog_barks +3 dog_growls +4 wild_pet", lookup),
                // 63
                scenario("swarm", "bats +3 spider_overhead +3 angry_bees", lookup),
                // 64
                scenario("wrong_herd", "disguise +3 animals_stare +3 hostile_animal", lookup),
                // 65
                scenario("the_golem_mourns", "golem_flower +4 distant_bell +3 villager_stares", lookup),
                // 66
                scenario("the_golem_turns", "villager_refuses +3 golem_flower +4 golem_rage", lookup),
                // 67
                scenario("stolen_block", "enderman_hiss +3 enderman_takes +3 wall_hole", lookup),
                // 68
                scenario("runaway_cart", "distant_mining +3 minecart_rolls +2 trip", lookup),
                // 69
                scenario("pack", "circling_moan +3 wolf_rage", lookup),
                // 70
                scenario("billy_goat", "animals_stare +2 leash_snaps +2 goat_rams", lookup),
                // 71
                scenario("you_died", "false_wound +1 false_hearts +2 false_death", lookup),
                // 72
                scenario("saved_by_nothing", "false_wound +2 false_totem +3 ghost_laugh", lookup),
                // 73
                scenario("achievement_get", "false_advancement +2 subliminal_title +2 own_voice", lookup),
                // 74
                scenario("starving", "false_hunger +2 eating_behind_wall +3 ravenous", lookup),
                // 75
                scenario("what_s_in_my_pocket", "false_slot +3 swap_slots +3 hotbar_shift", lookup),
                // 76
                scenario("silence", "music_cut +3 fake_subtitle +3 breathing", lookup),
                // 77
                scenario("seeing_red", "red_vignette +2 phantom_heartbeat +3 wrath", lookup),
                // 78
                scenario("the_sky_lies", "dragon_flyover +5 silent_lightning +3 distant_mob", lookup),
                // 79
                scenario("mind_slipping", "blinking +2 sanity_drain +3 false_looks +3 sanity_crash", lookup),
                // 80
                scenario("hoard", "shuffle_chest +3 vanishing_item +3 chest_opens", lookup),
                // 81
                scenario("butterfingers", "hotbar_shift +2 snatch +2 ghost_laugh", lookup),
                // 82
                scenario("x_marks_the_spot", "map_marker +3 false_compass +3 item_trail", lookup),
                // 83
                scenario("the_journal", "book_page +3 lectern_pages +2 close_whispers", lookup),
                // 84
                scenario("blight", "dead_ground +3 withering_crops +3 rot", lookup),
                // 85
                scenario("funeral", "wilted_flowers +3 cross +3 death_cry", lookup),
                // 86
                scenario("walking_woods", "ghost_tree +5 moving_tree +3 footsteps", lookup),
                // 87
                scenario("tantrum", "breathing_door +2 open_house +2 furniture_moves +3 snatch +2 chest_blast", lookup),
                // 88
                scenario(
                        "supper",
                        "extinguish_light +1 eating_behind_wall +2 torch_chain +1 eating_behind_wall +3 blackout",
                        lookup),
                // 89
                scenario("replacement", "footprints +3 own_voice +2 edge_figure +3 false_death +2 double", lookup),
                // 90
                scenario("taken_under", "close_whispers +3 water_call >water undertow +4 drowned_fish", lookup),
                // 91
                scenario(
                        "wearing_faces", "disguise +3 hostile_animal +3 hallucination +3 armor_off +2 copycat", lookup),
                // 92
                scenario(
                        "lights_at_your_back",
                        "torches_behind +2 held_light_out +2 approaching_steps +2 shadow",
                        lookup),
                // 93
                scenario("rider", "occupied_bed +3 breathing +3 heaviness +2 darkness +3 embrace", lookup),
                // 94
                scenario("foretold", "distant_bell +4 banshee +6 cut_off_scream +3 false_death +5 mock_grave", lookup),
                // 95
                scenario("blood_for_blood", "death_cry +2 red_vignette +2 copycat +0 wild_pet +3 wrath", lookup),
                // 96
                scenario(
                        "the_way_they_went",
                        "muddy_footprints +3 item_trail +5 torch_line +10 stranger_death +2 lost_time",
                        lookup),
                // 97
                scenario(
                        "house_rules",
                        "door +1 trapdoor +1 fence_gate +1 button +1 lever +2 locked_door +3 shuffled_books",
                        lookup),
                // 98
                scenario("feast", "false_hunger +2 eating_behind_wall +2 rot +3 wrong_smelt +3 vanishing_item", lookup),
                // 99
                scenario("the_debt", "red_vignette +2 blackout +3 wrath +3 the_call", lookup),
                // 100
                scenario(
                        "punchline",
                        "false_advancement +1 false_totem +2 chest_lid +2 chest_blast +2 ghost_laugh",
                        lookup));
        for (Scenario scenario : all) {
            if (byId.containsKey(scenario.id())) {
                throw new IllegalStateException("A scenario takes the id of an event: " + scenario.id());
            }
        }
        return all;
    }

    private static Scenario scenario(String id, String script, Function<Identifier, HauntEvent> lookup) {
        return Scenario.parse(Identifier.parse(id), script, lookup);
    }
}
