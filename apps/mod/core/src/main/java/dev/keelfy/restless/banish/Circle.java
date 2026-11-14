package dev.keelfy.restless.banish;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.block.Altar;
import dev.keelfy.restless.divine.Sense;
import dev.keelfy.restless.state.Session;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * One running ritual at an altar: who holds it, what it called, and how far it has come through its {@link Plan}. Each
 * wave first checks that the circle holds (offering laid, candles lit, player near), then ends the ritual at its last
 * wave, and otherwise hands the wave to its phase's class. Lives in memory only, like every scheduled task.
 */
public final class Circle {
    private static final Map<BlockPos, Circle> RUNNING = Session.SERVER.keep(new HashMap<>());

    final ServerPlayer player;
    final ServerLevel level;
    final BlockPos altar;
    final Altar.Held held;
    final List<BlockPos> candles;
    final Sense.Spirit spirit;
    final boolean censer;
    Plan plan;
    private int wave;
    private Phase phase;
    private boolean over;
    private final Gaze gaze = new Gaze(this);
    final Ring ring = new Ring(this);

    Circle(
            ServerPlayer player,
            ServerLevel level,
            BlockPos altar,
            Altar.Held held,
            List<BlockPos> candles,
            Sense.Spirit spirit,
            boolean censer,
            Plan plan) {
        this.player = player;
        this.level = level;
        this.altar = altar.immutable();
        this.held = held;
        this.candles = List.copyOf(candles);
        this.spirit = spirit;
        this.censer = censer;
        this.plan = plan;
        RUNNING.put(this.altar, this);
    }

    /** The ritual running at this altar, if any. */
    public static Optional<Circle> at(BlockPos altar) {
        Circle circle = RUNNING.get(altar);
        return circle == null || circle.over || circle.player.isRemoved() ? Optional.empty() : Optional.of(circle);
    }

    public ServerPlayer player() {
        return player;
    }

    public Sense.Spirit spirit() {
        return spirit;
    }

    public Plan plan() {
        return plan;
    }

    /** Waves run so far. */
    public int wave() {
        return wave;
    }

    /** The player lays the thrown offering back on the altar. @return whether it was laid */
    public boolean relay(Altar.@Nullable Held offered, ItemStack stack) {
        BlockState state = level.getBlockState(altar);
        if (offered != held || state.getValue(Altar.HELD) != Altar.Held.EMPTY) {
            return false;
        }
        level.setBlock(altar, state.setValue(Altar.HELD, held), 3);
        stack.consume(1, player);
        return true;
    }

    /** One tick of the spirit's gaze rule; does nothing outside the fury and the leaving. */
    public void watch() {
        if (!over) {
            gaze.tick();
        }
    }

    public Gaze gaze() {
        return gaze;
    }

    public Ring ring() {
        return ring;
    }

    /** The phase of the last wave that ran, empty before the first. */
    public Optional<Phase> phase() {
        return Optional.ofNullable(phase);
    }

    public boolean over() {
        return over;
    }

    /** Called every tick by the ritual's task; {@code elapsed} counts from 1. @return whether it goes on */
    boolean tick(int elapsed) {
        watch();
        int waveTicks = Math.max(1, Config.section("ritual").integer("wave_ticks"));
        if (elapsed % waveTicks == 0 && !step()) {
            return false;
        }
        if (elapsed % 2 == 0) {
            Ritual.smoke(level, altar, held);
        }
        return !over;
    }

    /**
     * One wave: the ritual holds or breaks, ends after the last, and otherwise its phase acts. Public so a test can
     * step a ritual without waiting. @return whether it goes on
     */
    public boolean step() {
        if (over) {
            return false;
        }
        if (!holds()) {
            end();
            Ritual.fail(player, level, altar, spirit);
            return false;
        }
        if (wave + 1 >= plan.total()) {
            end();
            Ritual.banish(player, level, altar, spirit, censer);
            return false;
        }
        Phase now = plan.at(wave);
        if (now != phase) {
            phase = now;
            enter(now);
        }
        level.sendParticles(
                ParticleTypes.SOUL, altar.getX() + 0.5, altar.getY() + 1.2, altar.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.02);
        boolean goes = switch (now) {
            case CALL -> Call.wave(this);
            case BARGAIN -> Bargain.wave(this);
            case FURY -> Fury.wave(this);
            case LEAVING -> Leaving.wave(this);
        };
        wave++;
        if (!goes) {
            end();
        }
        return goes;
    }

    private void enter(Phase now) {
        send(Optional.of(now));
        switch (now) {
            case CALL -> {}
            case BARGAIN -> Bargain.enter(this);
            case FURY -> Fury.enter(this);
            case LEAVING -> Leaving.enter(this);
        }
    }

    /** The offering lies on a whole altar, every candle burns, the player stays near, and a wiped ring is closed. */
    private boolean holds() {
        BlockState state = level.getBlockState(altar);
        boolean laid =
                state.is(Content.ALTAR.get()) && state.getValue(Altar.HELD) == held && Altar.isComplete(level, altar);
        boolean lit = candles.stream().allMatch(pos -> AbstractCandleBlock.isLit(level.getBlockState(pos)));
        boolean near = player.level() == level
                && player.blockPosition()
                        .closerThan(altar, Config.section("ritual").integer("radius"));
        return laid && lit && near && ring.holds();
    }

    private void end() {
        over = true;
        gaze.clear();
        RUNNING.remove(altar, this);
        send(Optional.empty());
    }

    private void send(Optional<Phase> now) {
        player.connection.send(new ClientboundCustomPayloadPacket(RitualPayload.of(now)));
    }
}
