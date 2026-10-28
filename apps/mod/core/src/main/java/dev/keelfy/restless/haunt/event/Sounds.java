package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
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
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;

/** Sounds with no source. Always runnable, never touch the world. */
final class Sounds {
    private static final double BEHIND = 180.0;
    private static final SoundType[] BREAKS = {SoundType.STONE, SoundType.WOOD, SoundType.GLASS, SoundType.GRAVEL};

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
                Nearby.sound(player, type.getBreakSound(), Nearby.randomAround(player, config.num("distance")), config.tone("sound"));
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
            // 37
            at("death_cry", SoundEvents.PLAYER_DEATH),
            // 38
            at("dripping", SoundEvents.POINTED_DRIPSTONE_DRIP_WATER)
        );
    }

    /** On the nearest wooden door if there is one, otherwise out of thin air nearby. */
    static Vec3 knockSpot(ServerPlayer player) {
        Config.Section config = Config.event("three_knocks");
        BlockPos door = Nearby.all(player, config.integer("radius"), s -> s.getBlock() instanceof DoorBlock && DoorBlock.isWoodenDoor(s)).stream()
            .min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(player.position())))
            .orElse(null);
        return door != null ? Vec3.atCenterOf(door) : Nearby.randomAround(player, config.num("fallback_distance"));
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

    private static HauntEvent sound(String id, Consumer<ServerPlayer> action) {
        return SimpleEvent.light(id, EventTag.SOUND, player -> true, action);
    }

    private static HauntEvent at(String id, SoundEvent sound) {
        return sound(id, player -> {
            Config.Section config = Config.event(id);
            Nearby.sound(player, sound, Nearby.randomAround(player, config.num("distance")), config.tone("sound"));
        });
    }
}
