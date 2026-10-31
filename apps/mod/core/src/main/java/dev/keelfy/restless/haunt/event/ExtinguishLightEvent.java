package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.UnlitLights;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntTier;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Snuffs candles, torches and lanterns in place. */
public final class ExtinguishLightEvent implements HauntEvent {
    @Override
    public Identifier id() {
        return Identifier.parse("extinguish_light");
    }

    @Override
    public HauntTier tier() {
        return HauntTier.LIGHT;
    }

    @Override
    public Set<EventTag> tags() {
        return Set.of(EventTag.LIGHTS);
    }

    @Override
    public boolean canRun(ServerPlayer player) {
        return find(player) != null;
    }

    @Override
    public void run(ServerPlayer player) {
        BlockPos pos = find(player);
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        BlockState state = level.getBlockState(pos);
        if (AbstractCandleBlock.isLit(state)) {
            AbstractCandleBlock.extinguish(null, state, level, pos);
        } else {
            UnlitLights.extinguish(level, pos);
            Config.Tone tone = Config.event("extinguish_light").tone("sound");
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, tone.volume(), tone.pitch());
        }
    }

    private static @Nullable BlockPos find(ServerPlayer player) {
        return Nearby.random(player, Config.event("extinguish_light").integer("radius"), ExtinguishLightEvent::isLight);
    }

    private static boolean isLight(BlockState state) {
        return AbstractCandleBlock.isLit(state) || UnlitLights.isTorch(state) || UnlitLights.isLantern(state);
    }
}
