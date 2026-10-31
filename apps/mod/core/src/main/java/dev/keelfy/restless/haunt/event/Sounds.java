package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Echoes;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.Hardcore;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntSounds;
import dev.keelfy.restless.haunt.Said;
import dev.keelfy.restless.haunt.ScenarioContext;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Sounds with no real source. They never touch the world. */
final class Sounds {
    private static final double BEHIND = 180.0;
    private static final SoundType[] BREAKS = {SoundType.STONE, SoundType.WOOD, SoundType.GLASS, SoundType.GRAVEL};
    private static final SoundEvent[] MOBS = {
        SoundEvents.ZOMBIE_AMBIENT,
        SoundEvents.SKELETON_AMBIENT,
        SoundEvents.SPIDER_AMBIENT,
        SoundEvents.ENDERMAN_AMBIENT,
        SoundEvents.WITCH_AMBIENT,
        SoundEvents.HUSK_AMBIENT,
        SoundEvents.DROWNED_AMBIENT,
        SoundEvents.PILLAGER_AMBIENT,
        HauntSounds.MOAN
    };
    private static final SoundEvent[] HOSTILE = {
        SoundEvents.ZOMBIE_AMBIENT, SoundEvents.CREEPER_PRIMED, SoundEvents.SKELETON_AMBIENT, SoundEvents.WITCH_AMBIENT
    };
    private static final SoundEvent[] VOICES = {
        SoundEvents.VILLAGER_HURT,
        SoundEvents.PLAYER_HURT,
        SoundEvents.GHAST_SCREAM,
        SoundEvents.ZOMBIE_VILLAGER_AMBIENT,
        SoundEvents.VEX_AMBIENT,
        HauntSounds.MOAN
    };
    /** Equip sounds of the armor materials, so a set is heard going on piece by piece. */
    private static final SoundEvent[] ARMOR = {
        SoundEvents.ARMOR_EQUIP_IRON.value(),
        SoundEvents.ARMOR_EQUIP_DIAMOND.value(),
        SoundEvents.ARMOR_EQUIP_GOLD.value(),
        SoundEvents.ARMOR_EQUIP_CHAIN.value(),
        SoundEvents.ARMOR_EQUIP_NETHERITE.value()
    };
    /** Death cries of a sheep, a piglet and a cow, for a pen. */
    private static final SoundEvent[] PEN = {
        SoundEvents.SHEEP_DEATH,
        SoundEvents.PIG_DEATH_BABY.value(),
        SoundEvents.COW_SOUNDS.values().iterator().next().deathSound().value()
    };

    private static final SoundEvent[] HORNS =
            SoundEvents.GOAT_HORN_SOUND_VARIANTS.stream().map(h -> h.value()).toArray(SoundEvent[]::new);
    private static final Predicate<BlockState> WOODEN_DOOR =
            s -> s.getBlock() instanceof DoorBlock && DoorBlock.isWoodenDoor(s);
    private static final Predicate<BlockState> WATER = s -> s.is(Blocks.WATER);
    private static final Predicate<BlockState> GLASS = s -> s.getSoundType() == SoundType.GLASS && !s.hasBlockEntity();
    /** Splash colours of a few potions, so the shatter reads as a real one. */
    private static final int[] POTION_COLORS = {0x8889A6, 0x87A363, 0x4E9331, 0x1F1FA1, 0xF82423, 0x484D48};

    /**
     * Not registered: the packet carries it whole, and the client's {@code sounds.json} gives it no audible sound and a
     * subtitle.
     */
    private static final SoundEvent BEHIND_YOU = SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath(Restless.MOD_ID, "subtitle.behind_you"));

    private Sounds() {}

    static List<HauntEvent> all() {
        return List.of(
                // 24
                sound("footsteps", player -> {
                    Config.Section config = Config.event("footsteps");
                    Vec3 at = Nearby.around(player, config.num("distance"), BEHIND);
                    var below =
                            player.level().getBlockState(player.blockPosition().below());
                    Nearby.sound(
                            player,
                            below.getSoundType().getStepSound(),
                            at.with(Direction.Axis.Y, player.getY()),
                            config.tone("sound"));
                }),
                // 199: the shrieker's own call, which a vibration would normally cause; no player is given, so nothing
                // is warned
                SimpleEvent.light(
                        "shrieker",
                        EventTag.SOUND,
                        player -> Nearby.any(player, "shrieker", s -> s.is(Blocks.SCULK_SHRIEKER)),
                        player -> {
                            BlockPos pos = Nearby.random(player, "shrieker", s -> s.is(Blocks.SCULK_SHRIEKER));
                            if (pos != null
                                    && player.level().getBlockEntity(pos)
                                            instanceof SculkShriekerBlockEntity shrieker) {
                                shrieker.tryShriek(player.level(), null);
                            }
                        }),
                // 199: a heart with no warden, quickening
                SimpleEvent.light("phantom_heartbeat", EventTag.SOUND, SimpleEvent.ALWAYS, player -> {
                    Config.Section config = Config.event("phantom_heartbeat");
                    Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
                        Nearby.sound(p, SoundEvents.WARDEN_HEARTBEAT, p.position(), config.tone("sound"));
                        return n + 1 < config.integer("beats");
                    });
                }),
                // 164: nothing to hear, but with subtitles on, something breathes behind the player
                sound("fake_subtitle", player -> {
                    Config.Section config = Config.event("fake_subtitle");
                    Nearby.sound(
                            player,
                            BEHIND_YOU,
                            Nearby.around(player, config.num("distance"), BEHIND),
                            config.tone("sound"));
                }),
                // 133: steps closing in from some bearing, walking or running pace
                sound("approaching_steps", Sounds::approach),
                // 134: rhythmic hits and a break below the surface, from a tunnel that is not there
                SimpleEvent.light(
                        "distant_mining",
                        EventTag.SOUND,
                        player -> player.level().canSeeSky(player.blockPosition()),
                        Sounds::mine),
                // 137: one material breaking in quick succession at real blocks nearby; under hardcore they are gone,
                // with no drops
                SimpleEvent.light(
                        "many_breaks",
                        EventTag.SOUND,
                        player -> !breakables(player).isEmpty(),
                        Sounds::breakMany),
                // 205: someone climbs a ladder nearby, or a point rises out of nowhere when there is none
                sound("ladder_steps", Sounds::climb),
                // 25
                at("creeper_hiss", SoundEvents.CREEPER_PRIMED),
                // 26
                sound("three_knocks", player -> knocks(player, knockSpot(player))),
                // 27
                at("crying_child", SoundEvents.VILLAGER_HURT),
                // 28
                sound("block_breaking", player -> {
                    Config.Section config = Config.event("block_breaking");
                    SoundType type = BREAKS[player.getRandom().nextInt(BREAKS.length)];
                    Nearby.sound(
                            player,
                            type.getBreakSound(),
                            Nearby.source(player, config.num("distance")),
                            config.tone("sound"));
                }),
                // 29
                at("eating_behind_wall", SoundEvents.GENERIC_EAT.value()),
                // 30
                at("enderman_hiss", SoundEvents.ENDERMAN_STARE),
                // 31
                at("chest_opens", SoundEvents.CHEST_OPEN),
                // 32: right beside the player, a little louder each time
                sound("breathing", Sounds::breathe),
                // 33: loud enough to carry, so it reads as distant
                at("distant_bell", SoundEvents.BELL_BLOCK),
                // 34
                at("phantom_jukebox", SoundEvents.MUSIC_DISC_11.value()),
                // 102: the game's own background music cuts out; it resumes on its usual schedule
                sound(
                        "music_cut",
                        player -> player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC))),
                // 35
                // Said once, then the spirit rests from it; see Said
                SimpleEvent.light(
                        "whisper",
                        EventTag.SOUND,
                        player -> Said.left(
                                player,
                                "whisper",
                                List.of(Config.event("whisper").text("format"))),
                        player -> {
                            String format = Said.pick(
                                    player,
                                    "whisper",
                                    List.of(Config.event("whisper").text("format")));
                            if (format != null) {
                                player.sendSystemMessage(Component.literal(format.replace(
                                                        "???", ScenarioContext.speaker("???"))
                                                .formatted(player.getName().getString()))
                                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                            }
                        }),
                // 36: the last step or splash the player made comes back from where it was made
                SimpleEvent.light("echo", EventTag.SOUND, Echoes::canReplay, Echoes::replay),
                // 37
                at("death_cry", HauntSounds.SCREAM),
                // 38
                at("dripping", SoundEvents.POINTED_DRIPSTONE_DRIP_WATER),
                // 131: some monster, anywhere from right beside the player to the edge of hearing
                sound("distant_mob", player -> {
                    Config.Section config = Config.event("distant_mob");
                    double distance = config.num("min_distance")
                            + player.getRandom().nextDouble()
                                    * (config.num("max_distance") - config.num("min_distance"));
                    Config.Tone tone = config.tone("sound");
                    // Volume above 1 only widens the range a sound carries (16 blocks per unit), so far ones stay
                    // audible.
                    float volume = Math.max(tone.volume(), (float) (distance / config.num("blocks_per_volume")));
                    Nearby.sound(
                            player,
                            MOBS[player.getRandom().nextInt(MOBS.length)],
                            Nearby.randomAround(player, distance),
                            new Config.Tone(volume, tone.pitch()));
                }),
                // 132: a cow that groans like a zombie; only this player hears it, and it follows the animal
                SimpleEvent.light(
                        "hostile_animal",
                        EventTag.SOUND,
                        player -> !animals(player).isEmpty(),
                        player -> {
                            List<Animal> animals = animals(player);
                            Animal animal = animals.get(player.getRandom().nextInt(animals.size()));
                            Config.Tone tone = Config.event("hostile_animal").tone("sound");
                            player.connection.send(new ClientboundSoundEntityPacket(
                                    BuiltInRegistries.SOUND_EVENT.wrapAsHolder(
                                            HOSTILE[player.getRandom().nextInt(HOSTILE.length)]),
                                    SoundSource.HOSTILE,
                                    animal,
                                    tone.volume(),
                                    tone.pitch(),
                                    player.getRandom().nextLong()));
                        }),
                // 136: something crawls on the ceiling right above
                SimpleEvent.light(
                        "spider_overhead", EventTag.SOUND, player -> ceiling(player) != null, Sounds::spiderOverhead),
                // 138: the floor sounds and looks as if it gives way, and holds
                SimpleEvent.harmful(
                        "cracking_floor",
                        EventTag.SOUND,
                        player -> !player.level()
                                .getBlockState(player.blockPosition().below())
                                .isAir(),
                        player -> {
                            BlockPos below = player.blockPosition().below();
                            ServerLevel level = player.level();
                            // Under hardcore it gives way for real, with nothing dropped, but only where the fall is
                            // one block.
                            if (Hardcore.on(level)
                                    && Place.solid(level, below)
                                    && level.getBlockState(below.below()).isSolidRender()) {
                                level.destroyBlock(below, false);
                            } else {
                                level.levelEvent(
                                        LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK,
                                        below,
                                        Block.getId(level.getBlockState(below)));
                            }
                        }),
                // 139
                sound(
                        "bottle_shatter",
                        player -> player.level()
                                .levelEvent(
                                        LevelEvent.PARTICLES_SPELL_POTION_SPLASH,
                                        BlockPos.containing(Nearby.source(
                                                player,
                                                Config.event("bottle_shatter").num("distance"))),
                                        POTION_COLORS[player.getRandom().nextInt(POTION_COLORS.length)])),
                // 140: a note block that plays a voice instead of a note
                SimpleEvent.light(
                        "note_block_voice",
                        EventTag.SOUND,
                        player -> Nearby.any(player, "note_block_voice", s -> s.is(Blocks.NOTE_BLOCK)),
                        player -> {
                            BlockPos block = Nearby.random(player, "note_block_voice", s -> s.is(Blocks.NOTE_BLOCK));
                            Config.Tone tone = Config.event("note_block_voice").tone("sound");
                            player.level()
                                    .playSound(
                                            null,
                                            block,
                                            VOICES[player.getRandom().nextInt(VOICES.length)],
                                            SoundSource.RECORDS,
                                            tone.volume(),
                                            tone.pitch());
                            player.level()
                                    .sendParticles(
                                            ParticleTypes.NOTE,
                                            block.getX() + 0.5,
                                            block.getY() + 1.2,
                                            block.getZ() + 0.5,
                                            0,
                                            player.getRandom().nextInt(25) / 24.0,
                                            0,
                                            0,
                                            1);
                        }),
                // 135: a laugh from nowhere
                at("ghost_laugh", HauntSounds.LAUGH),
                // 135: a breath at the nape of the neck
                sound("ghost_breath", player -> {
                    Config.Section config = Config.event("ghost_breath");
                    Nearby.sound(
                            player,
                            HauntSounds.BREATH,
                            Nearby.around(player, config.num("distance"), BEHIND),
                            config.tone("sound"));
                }),
                // 135: whispers at one ear, then the other
                sound("close_whispers", Sounds::whisperEars),
                // 135: a moan that comes round the player from one bearing to the next
                sound("circling_moan", Sounds::circle),
                // 135: a scream, and then nothing, as if a hand closed over it
                at("cut_off_scream", HauntSounds.CUT_OFF_SCREAM),
                // A sustained scream from nearby.
                at("long_scream", HauntSounds.LONG_SCREAM),
                // 150: a shriek from far off and high up, loud enough to carry
                sound("banshee", player -> {
                    Config.Section config = Config.event("banshee");
                    Nearby.sound(
                            player,
                            HauntSounds.SCREECH,
                            Nearby.source(player, config.num("distance")).add(0, config.num("height"), 0),
                            config.tone("sound"));
                }),
                // 312: a warden roars far off, then its sonic boom
                sequence("distant_warden", SoundEvents.WARDEN_ROAR, SoundEvents.WARDEN_SONIC_BOOM),
                // 313: the dragon growls somewhere above the overworld
                above("sky_dragon", SoundEvents.ENDER_DRAGON_GROWL),
                // 314: the wither's spawn cry, from far off
                at("distant_wither", SoundEvents.WITHER_SPAWN),
                // 315: an end portal opens somewhere
                at("end_portal_open", SoundEvents.END_PORTAL_SPAWN),
                // 316: a ghast screams high overhead
                above("sky_ghast", SoundEvents.GHAST_SCREAM),
                // 317: a fox screeches in the dark
                atWhen("fox_screech", player -> !player.level().isBrightOutside(), SoundEvents.FOX_SCREECH),
                // 318: a rabbit dies in the grass
                at("rabbit_death", SoundEvents.RABBIT_DEATH),
                // 319: the player's own hurt sound, no damage
                self("phantom_hurt", SoundEvents.PLAYER_HURT),
                // 320: a tool breaks, the tool is whole
                self("phantom_tool_break", SoundEvents.ITEM_BREAK.value()),
                // 321: elytra wind sweeps across the sky overhead
                sound("elytra_overhead", Sounds::flyOver),
                // 322: a soul sand valley or basalt delta mood plays for about ten seconds
                sound("nether_ambience", Sounds::netherMood),
                // 323: a set of armor goes on piece by piece behind the player
                sound("equip_behind", player -> {
                    Config.Section config = Config.event("equip_behind");
                    SoundEvent piece = ARMOR[player.getRandom().nextInt(ARMOR.length)];
                    chain(
                            player,
                            "equip_behind",
                            Nearby.around(player, config.num("distance"), BEHIND),
                            piece,
                            piece,
                            piece,
                            piece);
                }),
                // 324: a bobber is cast, splashes and is reeled in, wherever there is no water
                sequence(
                        "phantom_fishing",
                        SoundEvents.FISHING_BOBBER_THROW,
                        SoundEvents.FISHING_BOBBER_SPLASH,
                        SoundEvents.FISHING_BOBBER_RETRIEVE),
                // 325: a zombie batters a wooden door nearby, and breaks through
                SimpleEvent.light(
                        "zombie_at_door",
                        EventTag.SOUND,
                        player -> Nearby.any(player, "zombie_at_door", WOODEN_DOOR),
                        player -> {
                            BlockPos door = Nearby.random(player, "zombie_at_door", WOODEN_DOOR);
                            SoundEvent hit = SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR;
                            if (door != null) {
                                chain(
                                        player,
                                        "zombie_at_door",
                                        Vec3.atCenterOf(door),
                                        hit,
                                        hit,
                                        hit,
                                        hit,
                                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR);
                            }
                        }),
                // 326: a respawn anchor charges or runs dry
                at("phantom_anchor", SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value()),
                // 327: a bucket of water is emptied indoors
                atWhen(
                        "bucket_spill",
                        player -> !player.level().canSeeSky(player.blockPosition()),
                        SoundEvents.BUCKET_EMPTY),
                // 328: a villager's "hmm" right behind the player
                behind("lost_villager", SoundEvents.VILLAGER_AMBIENT),
                // 329: an arrow hits the wall in front of the player, and is not there
                SimpleEvent.light("phantom_arrow", EventTag.SOUND, player -> Place.wall(player) != null, player -> {
                    Place.Wall wall = Place.wall(player);
                    if (wall != null) {
                        Nearby.sound(
                                player,
                                SoundEvents.ARROW_HIT,
                                Vec3.atCenterOf(wall.low()),
                                Config.event("phantom_arrow").tone("sound"));
                    }
                }),
                // 330: a real glass block or pane sounds and shows as if it broke, and stays
                SimpleEvent.light(
                        "phantom_glass",
                        EventTag.SOUND,
                        player -> Nearby.any(player, "phantom_glass", GLASS),
                        player -> {
                            BlockPos pos = Nearby.random(player, "phantom_glass", GLASS);
                            if (pos != null) {
                                player.level()
                                        .levelEvent(
                                                LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK,
                                                pos,
                                                Block.getId(player.level().getBlockState(pos)));
                            }
                        }),
                // 331: an animal in a pen cries out as if it were dying
                SimpleEvent.light(
                        "pen_cries",
                        EventTag.SOUND,
                        player -> !animals(player, "pen_cries").isEmpty(),
                        player -> {
                            List<Animal> animals = animals(player, "pen_cries");
                            Animal animal = animals.get(player.getRandom().nextInt(animals.size()));
                            Nearby.sound(
                                    player,
                                    PEN[player.getRandom().nextInt(PEN.length)],
                                    animal.position(),
                                    Config.event("pen_cries").tone("sound"));
                        }),
                // 332: something sniffs right behind the player
                behind("sniffing_behind", SoundEvents.SNIFFER_SNIFFING),
                // 333: a goat horn calls from far off
                at("distant_goat_horn", HORNS),
                // 334: a raid horn, with no raid
                at("false_raid_horn", SoundEvents.RAID_HORN.value()),
                // 335: something heavy falls into water nearby
                SimpleEvent.light(
                        "heavy_splash", EventTag.SOUND, player -> Nearby.any(player, "heavy_splash", WATER), player -> {
                            BlockPos pos = Nearby.random(player, "heavy_splash", WATER);
                            if (pos != null) {
                                Nearby.sound(
                                        player,
                                        SoundEvents.GENERIC_SPLASH,
                                        Vec3.atCenterOf(pos),
                                        Config.event("heavy_splash").tone("sound"));
                            }
                        }),
                // 336: a ravager roars far off
                at("distant_ravager", SoundEvents.RAVAGER_ROAR),
                // 337: an iron golem walks round the house
                orbit("golem_circling", SoundEvents.IRON_GOLEM_STEP),
                // 338: a burp behind the player
                behind("burp_behind", SoundEvents.PLAYER_BURP),
                // 339: bubbles rise from the floor
                sequence(
                        "bubble_columns",
                        SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,
                        SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE,
                        SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                        SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                        SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE,
                        SoundEvents.BUBBLE_COLUMN_BUBBLE_POP),
                // 340: lava pops close by, with no lava
                sequence(
                        "lava_pops",
                        SoundEvents.LAVA_AMBIENT,
                        SoundEvents.LAVA_POP,
                        SoundEvents.LAVA_POP,
                        SoundEvents.LAVA_POP),
                // 341: chains rattle round the player
                orbit("chain_rattle", SoundEvents.CHAIN_STEP));
    }

    /** On the nearest wooden door if there is one, otherwise out of thin air nearby. */
    static Vec3 knockSpot(ServerPlayer player) {
        Config.Section config = Config.event("three_knocks");
        BlockPos door = Nearby.all(
                        player,
                        config.integer("radius"),
                        s -> s.getBlock() instanceof DoorBlock && DoorBlock.isWoodenDoor(s))
                .stream()
                .min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(player.position())))
                .orElse(null);
        return door != null ? Vec3.atCenterOf(door) : Nearby.source(player, config.num("fallback_distance"));
    }

    static void knocks(ServerPlayer player, Vec3 at) {
        Config.Section config = Config.event("three_knocks");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            Nearby.sound(p, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, at, config.tone("sound"));
            return n + 1 < config.integer("knocks");
        });
    }

    private static void breathe(ServerPlayer player) {
        Config.Section config = Config.event("breathing");
        double yaw = player.getRandom().nextDouble() * Nearby.FULL_TURN;
        Config.Tone start = config.tone("sound");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            Nearby.sound(
                    p,
                    SoundEvents.PLAYER_BREATH,
                    Nearby.around(p, config.num("distance"), yaw),
                    new Config.Tone(start.volume() + (float) config.num("volume_step") * n, start.pitch()));
            return n + 1 < config.integer("breaths");
        });
    }

    private static void whisperEars(ServerPlayer player) {
        Config.Section config = Config.event("close_whispers");
        double side = player.getRandom().nextBoolean() ? 90.0 : -90.0;
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            Nearby.sound(
                    p,
                    HauntSounds.WHISPER,
                    Nearby.around(p, config.num("distance"), n % 2 == 0 ? side : -side),
                    config.tone("sound"));
            return n + 1 < config.integer("whispers");
        });
    }

    private static void circle(ServerPlayer player) {
        Config.Section config = Config.event("circling_moan");
        double start = player.getRandom().nextDouble() * Nearby.FULL_TURN;
        double turn = player.getRandom().nextBoolean() ? config.num("step_degrees") : -config.num("step_degrees");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            Nearby.sound(
                    p,
                    HauntSounds.MOAN,
                    Nearby.around(p, config.num("distance"), start + turn * n),
                    config.tone("sound"));
            return n + 1 < config.integer("moans");
        });
    }

    private static void approach(ServerPlayer player) {
        Config.Section config = Config.event("approaching_steps");
        double yaw = player.getRandom().nextDouble() * Nearby.FULL_TURN;
        var below = player.level().getBlockState(player.blockPosition().below());
        int steps = config.integer("steps");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            double distance = config.num("start_distance")
                    + (config.num("end_distance") - config.num("start_distance")) * n / Math.max(1, steps - 1);
            Nearby.sound(
                    p,
                    below.getSoundType().getStepSound(),
                    Nearby.around(p, distance, yaw).with(Direction.Axis.Y, p.getY()),
                    config.tone("sound"));
            return n + 1 < steps;
        });
    }

    private static void mine(ServerPlayer player) {
        Config.Section config = Config.event("distant_mining");
        Vec3 at = player.position()
                .add(
                        (player.getRandom().nextDouble() - 0.5) * 2 * config.num("spread"),
                        -config.num("depth"),
                        (player.getRandom().nextDouble() - 0.5) * 2 * config.num("spread"));
        int hits = config.integer("hits");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            Nearby.sound(
                    p,
                    n + 1 < hits ? SoundType.STONE.getHitSound() : SoundType.STONE.getBreakSound(),
                    at,
                    config.tone("sound"));
            return n + 1 < hits;
        });
    }

    /**
     * Glass or stone blocks within reach that nothing stands on or holds; the spot below the player's feet is left
     * alone.
     */
    private static List<BlockPos> breakables(ServerPlayer player) {
        Config.Section config = Config.event("many_breaks");
        List<BlockPos> glass = blocksOf(player, SoundType.GLASS);
        List<BlockPos> stone = blocksOf(player, SoundType.STONE);
        List<BlockPos> best = glass.size() >= stone.size() ? glass : stone;
        return best.size() >= config.integer("min_blocks") ? best : List.of();
    }

    private static List<BlockPos> blocksOf(ServerPlayer player, SoundType type) {
        ServerLevel level = player.level();
        BlockPos feet = player.blockPosition();
        return Nearby.all(player, "many_breaks", s -> s.getSoundType() == type && !s.hasBlockEntity()).stream()
                .filter(pos -> level.getBlockState(pos).getDestroySpeed(level, pos) >= 0
                        && !(Math.abs(pos.getX() - feet.getX()) <= 1
                                && Math.abs(pos.getZ() - feet.getZ()) <= 1
                                && pos.getY() < feet.getY()))
                .toList();
    }

    private static void breakMany(ServerPlayer player) {
        Config.Section config = Config.event("many_breaks");
        ServerLevel level = player.level();
        List<BlockPos> blocks = new ArrayList<>(breakables(player));
        Collections.shuffle(blocks, new Random(player.getRandom().nextLong()));
        List<BlockPos> chosen = blocks.subList(0, Math.min(blocks.size(), config.integer("count")));
        Config.Tone tone = config.tone("sound");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            BlockPos pos = chosen.get(n);
            if (Hardcore.on(level)) {
                level.destroyBlock(pos, false);
            } else {
                level.playSound(
                        null,
                        pos,
                        level.getBlockState(pos).getSoundType().getBreakSound(),
                        SoundSource.BLOCKS,
                        tone.volume(),
                        tone.pitch());
            }
            return n + 1 < chosen.size();
        });
    }

    private static void climb(ServerPlayer player) {
        Config.Section config = Config.event("ladder_steps");
        BlockPos ladder = Nearby.random(player, "ladder_steps", s -> s.is(Blocks.LADDER));
        boolean up = player.getRandom().nextBoolean();
        Vec3 start;
        int steps = config.integer("steps");
        if (ladder != null) {
            // Along the whole column, capped, from whichever end the climber starts at.
            BlockPos bottom = ladder;
            while (player.level().getBlockState(bottom.below()).is(Blocks.LADDER)
                    && ladder.getY() - bottom.getY() < steps) {
                bottom = bottom.below();
            }
            BlockPos top = ladder;
            while (player.level().getBlockState(top.above()).is(Blocks.LADDER) && top.getY() - ladder.getY() < steps) {
                top = top.above();
            }
            steps = Math.max(1, top.getY() - bottom.getY() + 1);
            start = Vec3.atCenterOf(up ? bottom : top);
        } else {
            start = Nearby.source(player, config.num("fallback_distance"));
        }
        int count = steps;
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            Nearby.sound(p, SoundType.LADDER.getStepSound(), start.add(0, up ? n : -n, 0), config.tone("sound"));
            return n + 1 < count;
        });
    }

    private static List<Animal> animals(ServerPlayer player) {
        return animals(player, "hostile_animal");
    }

    private static List<Animal> animals(ServerPlayer player, String event) {
        return player.level()
                .getEntitiesOfClass(Animal.class, new AABB(player.blockPosition()).inflate(Nearby.radius(event)));
    }

    /** The solid block 2 or 3 above the player's head, if the space between is open. */
    private static @Nullable BlockPos ceiling(ServerPlayer player) {
        BlockPos head = BlockPos.containing(player.getEyePosition());
        for (int up = 2; up <= 3; up++) {
            BlockPos pos = head.above(up);
            if (player.level().getBlockState(head.above(up - 1)).isAir()
                    && player.level().getBlockState(pos).isSolidRender()) {
                return pos;
            }
        }
        return null;
    }

    private static void spiderOverhead(ServerPlayer player) {
        Config.Section config = Config.event("spider_overhead");
        BlockPos start = ceiling(player);
        Config.Tone step = config.tone("sound");
        Vec3 drift = Nearby.randomAround(player, 1.0)
                .subtract(player.position())
                .multiply(1, 0, 1)
                .normalize()
                .scale(config.num("step_blocks"));
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            Vec3 at = Vec3.atBottomCenterOf(start).add(drift.scale(n));
            Nearby.sound(p, n == 0 ? SoundEvents.SPIDER_AMBIENT : SoundEvents.SPIDER_STEP, at, step);
            return n + 1 < config.integer("steps");
        });
    }

    /** Plays the sounds in turn from one point, {@code interval_ticks} apart. */
    private static void chain(ServerPlayer player, String id, Vec3 at, SoundEvent... sounds) {
        Config.Section config = Config.event(id);
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            Nearby.sound(p, sounds[n], at, config.tone("sound"));
            return n + 1 < sounds.length;
        });
    }

    /** A moving point round the player at ground level, {@code step_degrees} further on each of {@code steps}. */
    private static HauntEvent orbit(String id, SoundEvent sound) {
        return sound(id, player -> {
            Config.Section config = Config.event(id);
            double start = player.getRandom().nextDouble() * Nearby.FULL_TURN;
            double turn = player.getRandom().nextBoolean() ? config.num("step_degrees") : -config.num("step_degrees");
            Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
                Nearby.sound(
                        p,
                        sound,
                        Nearby.around(p, config.num("distance"), start + turn * n)
                                .with(Direction.Axis.Y, p.getY()),
                        config.tone("sound"));
                return n + 1 < config.integer("steps");
            });
        });
    }

    private static void flyOver(ServerPlayer player) {
        Config.Section config = Config.event("elytra_overhead");
        double yaw = Math.toRadians(player.getRandom().nextDouble() * Nearby.FULL_TURN);
        int passes = config.integer("passes");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            double along = config.num("span") * (2.0 * n / Math.max(1, passes - 1) - 1.0);
            Nearby.sound(
                    p,
                    SoundEvents.ELYTRA_FLYING,
                    p.position().add(-Math.sin(yaw) * along, config.num("height"), Math.cos(yaw) * along),
                    config.tone("sound"));
            return n + 1 < passes;
        });
    }

    private static void netherMood(ServerPlayer player) {
        Config.Section config = Config.event("nether_ambience");
        boolean valley = player.getRandom().nextBoolean();
        SoundEvent mood =
                (valley ? SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD : SoundEvents.AMBIENT_BASALT_DELTAS_MOOD).value();
        SoundEvent extra = (valley
                        ? SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS
                        : SoundEvents.AMBIENT_BASALT_DELTAS_ADDITIONS)
                .value();
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            Nearby.sound(p, n % 2 == 0 ? mood : extra, p.position(), config.tone("sound"));
            return n + 1 < config.integer("sounds");
        });
    }

    /** One sound at the player, as if their own. */
    private static HauntEvent self(String id, SoundEvent sound) {
        return sound(
                id,
                player -> Nearby.sound(
                        player, sound, player.position(), Config.event(id).tone("sound")));
    }

    /** The sounds in turn from one point {@code distance} blocks away. */
    private static HauntEvent sequence(String id, SoundEvent... sounds) {
        return sound(
                id,
                player ->
                        chain(player, id, Nearby.source(player, Config.event(id).num("distance")), sounds));
    }

    private static HauntEvent behind(String id, SoundEvent sound) {
        return sound(id, player -> {
            Config.Section config = Config.event(id);
            Nearby.sound(player, sound, Nearby.around(player, config.num("distance"), BEHIND), config.tone("sound"));
        });
    }

    /** From {@code distance} blocks away and {@code height} up. */
    private static HauntEvent above(String id, SoundEvent sound) {
        return sound(id, player -> {
            Config.Section config = Config.event(id);
            Nearby.sound(
                    player,
                    sound,
                    Nearby.source(player, config.num("distance")).add(0, config.num("height"), 0),
                    config.tone("sound"));
        });
    }

    private static HauntEvent sound(String id, Consumer<ServerPlayer> action) {
        return SimpleEvent.light(id, EventTag.SOUND, SimpleEvent.ALWAYS, action);
    }

    /** One of the sounds, at a point {@code distance} blocks away. */
    private static HauntEvent at(String id, SoundEvent... sounds) {
        return atWhen(id, SimpleEvent.ALWAYS, sounds);
    }

    private static HauntEvent atWhen(String id, Predicate<ServerPlayer> when, SoundEvent... sounds) {
        return SimpleEvent.light(id, EventTag.SOUND, when, player -> {
            Config.Section config = Config.event(id);
            Nearby.sound(
                    player,
                    sounds[player.getRandom().nextInt(sounds.length)],
                    Nearby.source(player, config.num("distance")),
                    config.tone("sound"));
        });
    }
}
