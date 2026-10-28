package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;

/** Sounds with no source. Always runnable, never touch the world. */
final class Sounds {
    private static final double WEIGHT = 0.4;
    private static final SoundType[] BREAKS = {SoundType.STONE, SoundType.WOOD, SoundType.GLASS, SoundType.GRAVEL};

    private Sounds() {}

    static List<HauntEvent> all() {
        return List.of(
            // 24
            sound("footsteps", player -> {
                Vec3 at = Nearby.around(player, 2.0, 180.0);
                var below = player.level().getBlockState(player.blockPosition().below());
                Nearby.sound(player, below.getSoundType().getStepSound(), at.with(Direction.Axis.Y, player.getY()), 1.0F, 0.9F);
            }),
            // 25
            at("creeper_hiss", SoundEvents.CREEPER_PRIMED, 3.0, 1.0F, 1.0F),
            // 27
            at("crying_child", SoundEvents.VILLAGER_HURT, 6.0, 1.0F, 1.8F),
            // 28
            sound("block_breaking", player -> {
                SoundType type = BREAKS[player.getRandom().nextInt(BREAKS.length)];
                Nearby.sound(player, type.getBreakSound(), Nearby.randomAround(player, 5.0), 1.0F, 0.8F);
            }),
            // 29
            at("eating_behind_wall", SoundEvents.GENERIC_EAT.value(), 3.0, 1.0F, 0.8F),
            // 30
            at("enderman_hiss", SoundEvents.ENDERMAN_STARE, 6.0, 0.8F, 0.8F),
            // 31
            at("chest_opens", SoundEvents.CHEST_OPEN, 4.0, 1.0F, 0.9F),
            // 33: loud enough to carry, so it reads as distant
            at("distant_bell", SoundEvents.BELL_BLOCK, 40.0, 4.0F, 0.6F),
            // 34
            at("phantom_jukebox", SoundEvents.MUSIC_DISC_11.value(), 8.0, 2.0F, 1.0F),
            // 35
            sound("whisper", player -> player.sendSystemMessage(
                Component.literal("<???> " + player.getName().getString()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))),
            // 37
            at("death_cry", SoundEvents.PLAYER_DEATH, 7.0, 0.8F, 1.0F),
            // 38
            at("dripping", SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, 3.0, 1.0F, 1.0F)
        );
    }

    private static HauntEvent sound(String id, Consumer<ServerPlayer> action) {
        return SimpleEvent.light(id, EventTag.SOUND, WEIGHT, player -> true, action);
    }

    private static HauntEvent at(String id, SoundEvent sound, double distance, float volume, float pitch) {
        return sound(id, player -> Nearby.sound(player, sound, Nearby.randomAround(player, distance), volume, pitch));
    }
}
