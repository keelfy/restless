package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Echoes;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Sounds with no real source. They never touch the world. */
final class Sounds {
    private static final double BEHIND = 180.0;
    private static final SoundType[] BREAKS = {SoundType.STONE, SoundType.WOOD, SoundType.GLASS, SoundType.GRAVEL};
    private static final SoundEvent[] MOBS = {
        SoundEvents.ZOMBIE_AMBIENT, SoundEvents.SKELETON_AMBIENT, SoundEvents.SPIDER_AMBIENT, SoundEvents.ENDERMAN_AMBIENT,
        SoundEvents.WITCH_AMBIENT, SoundEvents.HUSK_AMBIENT, SoundEvents.DROWNED_AMBIENT, SoundEvents.PILLAGER_AMBIENT};
    private static final SoundEvent[] HOSTILE = {SoundEvents.ZOMBIE_AMBIENT, SoundEvents.CREEPER_PRIMED, SoundEvents.SKELETON_AMBIENT, SoundEvents.WITCH_AMBIENT};
    private static final SoundEvent[] VOICES = {
        SoundEvents.VILLAGER_HURT, SoundEvents.PLAYER_HURT, SoundEvents.GHAST_SCREAM, SoundEvents.ZOMBIE_VILLAGER_AMBIENT, SoundEvents.VEX_AMBIENT};
    /** Splash colours of a few potions, so the shatter reads as a real one. */
    private static final int[] POTION_COLORS = {0x8889A6, 0x87A363, 0x4E9331, 0x1F1FA1, 0xF82423, 0x484D48};

    private Sounds() {}

    static List<HauntEvent> all() {
        return List.of(
            // 24
            sound("footsteps", player -> {
                Config.Section config = Config.event("footsteps");
                Vec3 at = Nearby.around(player, config.num("distance"), BEHIND);
                var below = player.level().getBlockState(player.blockPosition().below());
                Nearby.sound(player, below.getSoundType().getStepSound(), at.with(Direction.Axis.Y, player.getY()), config.tone("sound"));
            }),
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
                Nearby.sound(player, type.getBreakSound(), Nearby.source(player, config.num("distance")), config.tone("sound"));
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
            sound("music_cut", player -> player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC))),
            // 35
            sound("whisper", player -> player.sendSystemMessage(
                Component.literal(Config.event("whisper").text("format").formatted(player.getName().getString()))
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))),
            // 36: the last step or splash the player made comes back from where it was made
            SimpleEvent.light("echo", EventTag.SOUND, Echoes::canReplay, Echoes::replay),
            // 37
            at("death_cry", SoundEvents.PLAYER_DEATH),
            // 38
            at("dripping", SoundEvents.POINTED_DRIPSTONE_DRIP_WATER),
            // 131: some monster, anywhere from right beside the player to the edge of hearing
            sound("distant_mob", player -> {
                Config.Section config = Config.event("distant_mob");
                double distance = config.num("min_distance") + player.getRandom().nextDouble() * (config.num("max_distance") - config.num("min_distance"));
                Config.Tone tone = config.tone("sound");
                // Volume above 1 only widens the range a sound carries (16 blocks per unit), so far ones stay audible.
                float volume = Math.max(tone.volume(), (float) (distance / config.num("blocks_per_volume")));
                Nearby.sound(player, MOBS[player.getRandom().nextInt(MOBS.length)], Nearby.randomAround(player, distance), new Config.Tone(volume, tone.pitch()));
            }),
            // 132: a cow that groans like a zombie; only this player hears it, and it follows the animal
            SimpleEvent.light("hostile_animal", EventTag.SOUND, player -> !animals(player).isEmpty(), player -> {
                List<Animal> animals = animals(player);
                Animal animal = animals.get(player.getRandom().nextInt(animals.size()));
                Config.Tone tone = Config.event("hostile_animal").tone("sound");
                player.connection.send(new ClientboundSoundEntityPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(HOSTILE[player.getRandom().nextInt(HOSTILE.length)]),
                    SoundSource.HOSTILE, animal, tone.volume(), tone.pitch(), player.getRandom().nextLong()));
            }),
            // 136: something crawls on the ceiling right above
            SimpleEvent.light("spider_overhead", EventTag.SOUND, player -> ceiling(player) != null, Sounds::spiderOverhead),
            // 138: the floor sounds and looks as if it gives way, and holds
            SimpleEvent.harmful("cracking_floor", EventTag.SOUND, player -> !player.level().getBlockState(player.blockPosition().below()).isAir(), player -> {
                BlockPos below = player.blockPosition().below();
                player.level().levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, below, Block.getId(player.level().getBlockState(below)));
            }),
            // 139
            sound("bottle_shatter", player -> player.level().levelEvent(LevelEvent.PARTICLES_SPELL_POTION_SPLASH,
                BlockPos.containing(Nearby.source(player, Config.event("bottle_shatter").num("distance"))),
                POTION_COLORS[player.getRandom().nextInt(POTION_COLORS.length)])),
            // 140: a note block that plays a voice instead of a note
            SimpleEvent.light("note_block_voice", EventTag.SOUND, player -> Nearby.any(player, "note_block_voice", s -> s.is(Blocks.NOTE_BLOCK)), player -> {
                BlockPos block = Nearby.random(player, "note_block_voice", s -> s.is(Blocks.NOTE_BLOCK));
                Config.Tone tone = Config.event("note_block_voice").tone("sound");
                player.level().playSound(null, block, VOICES[player.getRandom().nextInt(VOICES.length)], SoundSource.RECORDS, tone.volume(), tone.pitch());
                player.level().sendParticles(ParticleTypes.NOTE, block.getX() + 0.5, block.getY() + 1.2, block.getZ() + 0.5, 0, player.getRandom().nextInt(25) / 24.0, 0, 0, 1);
            })
        );
    }

    /** On the nearest wooden door if there is one, otherwise out of thin air nearby. */
    static Vec3 knockSpot(ServerPlayer player) {
        Config.Section config = Config.event("three_knocks");
        BlockPos door = Nearby.all(player, config.integer("radius"), s -> s.getBlock() instanceof DoorBlock && DoorBlock.isWoodenDoor(s)).stream()
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
            Nearby.sound(p, SoundEvents.PLAYER_BREATH, Nearby.around(p, config.num("distance"), yaw),
                new Config.Tone(start.volume() + (float) config.num("volume_step") * n, start.pitch()));
            return n + 1 < config.integer("breaths");
        });
    }

    private static List<Animal> animals(ServerPlayer player) {
        return player.level().getEntitiesOfClass(Animal.class, new AABB(player.blockPosition()).inflate(Nearby.radius("hostile_animal")));
    }

    /** The solid block 2 or 3 above the player's head, if the space between is open. */
    private static @Nullable BlockPos ceiling(ServerPlayer player) {
        BlockPos head = BlockPos.containing(player.getEyePosition());
        for (int up = 2; up <= 3; up++) {
            BlockPos pos = head.above(up);
            if (player.level().getBlockState(head.above(up - 1)).isAir() && player.level().getBlockState(pos).isSolidRender()) {
                return pos;
            }
        }
        return null;
    }

    private static void spiderOverhead(ServerPlayer player) {
        Config.Section config = Config.event("spider_overhead");
        BlockPos start = ceiling(player);
        Config.Tone step = config.tone("sound");
        Vec3 drift = Nearby.randomAround(player, 1.0).subtract(player.position()).multiply(1, 0, 1).normalize().scale(config.num("step_blocks"));
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            Vec3 at = Vec3.atBottomCenterOf(start).add(drift.scale(n));
            Nearby.sound(p, n == 0 ? SoundEvents.SPIDER_AMBIENT : SoundEvents.SPIDER_STEP, at, step);
            return n + 1 < config.integer("steps");
        });
    }

    private static HauntEvent sound(String id, Consumer<ServerPlayer> action) {
        return SimpleEvent.light(id, EventTag.SOUND, player -> true, action);
    }

    private static HauntEvent at(String id, SoundEvent sound) {
        return sound(id, player -> {
            Config.Section config = Config.event(id);
            Nearby.sound(player, sound, Nearby.source(player, config.num("distance")), config.tone("sound"));
        });
    }
}
