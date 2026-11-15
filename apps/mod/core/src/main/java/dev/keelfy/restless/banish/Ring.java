package dev.keelfy.restless.banish;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.event.kit.Nearby;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

/**
 * The ring breaks: once per ritual, at a fury wave picked when the fury first acts, the spirit wipes one salt tile of
 * the ring. Salt must be laid on that tile again before the next wave, or the ritual fails as it does for a dark
 * candle.
 */
public final class Ring {
    private final Circle circle;
    private int breakAt = -1;
    private boolean wiped;
    private @Nullable BlockPos gap;

    Ring(Circle circle) {
        this.circle = circle;
    }

    /** A fury wave: the spirit wipes the ring if this is its wave. */
    void wave() {
        int fury = circle.wave() - circle.plan.call() - circle.plan.bargain();
        if (breakAt < 0) {
            breakAt = fury + circle.player.getRandom().nextInt(Math.max(1, circle.plan.fury() - fury));
        }
        if (!wiped && fury >= breakAt) {
            wipe();
        }
    }

    /** Wipes one salt tile of the ring, level with the altar. @return whether there was one to wipe */
    public boolean wipe() {
        if (wiped) {
            return false;
        }
        wiped = true;
        List<BlockPos> salt = Nearby.all(
                        circle.level,
                        circle.altar,
                        Config.section("ritual").integer("ring_radius"),
                        s -> s.is(Content.SALT_LINE.get()))
                .stream()
                .filter(pos -> pos.getY() == circle.altar.getY())
                .toList();
        if (salt.isEmpty()) {
            return false;
        }
        gap = salt.get(circle.player.getRandom().nextInt(salt.size()));
        circle.level.setBlock(gap, Blocks.AIR.defaultBlockState(), 3);
        circle.level.playSound(null, gap, SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 1.0f, 0.6f);
        circle.player.sendSystemMessage(Component.translatable("message.restless.ring_broken"), true);
        return true;
    }

    /** The tile the spirit wiped, while the ring is still open. */
    public Optional<BlockPos> gap() {
        return Optional.ofNullable(gap);
    }

    /** Whether the ring holds: nothing to check until it was wiped, then the wiped tile must be salt again. */
    boolean holds() {
        if (gap == null) {
            return true;
        }
        if (!circle.level.getBlockState(gap).is(Content.SALT_LINE.get())) {
            return false;
        }
        gap = null;
        return true;
    }
}
