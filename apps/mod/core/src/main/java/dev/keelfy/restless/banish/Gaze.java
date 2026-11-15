package dev.keelfy.restless.banish;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.divine.Body;
import dev.keelfy.restless.haunt.event.Jolts;
import dev.keelfy.restless.haunt.event.kit.PacketEntity;
import dev.keelfy.restless.spirit.Focus;
import dev.keelfy.restless.spirit.Traits;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.phys.Vec3;

/**
 * The spirit shows itself in the fury and the leaving: its body (see {@link Body}) walks the salt ring around the
 * altar, and the player must keep a rule of gaze set by its nature. Darkness and malice: never look at it longer than
 * the limit. Objects and deceit: never look away from it longer than the limit. Breaking the rule snuffs a candle, not
 * the ritual, and the count starts over.
 */
public final class Gaze {
    private final Circle circle;
    private PacketEntity figure;
    private double angle;
    private Vec3 at = Vec3.ZERO;
    private int count;

    Gaze(Circle circle) {
        this.circle = circle;
    }

    /** Where the figure's head is now, empty of meaning before it first shows. */
    public Vec3 head() {
        return at.add(0, 1.5, 0);
    }

    public boolean shown() {
        return figure != null;
    }

    void tick() {
        var phase = circle.phase();
        if (phase.isEmpty() || (phase.get() != Phase.FURY && phase.get() != Phase.LEAVING)) {
            return;
        }
        Config.Section config = Config.section("ritual");
        walk(config);
        boolean watched = Jolts.watched(circle.player, head(), config.num("gaze_angle"));
        boolean broken = stares() ? watched : !watched;
        count = broken ? count + 1 : 0;
        if (count >= config.integer("gaze_ticks")) {
            count = 0;
            snuff();
        }
    }

    void clear() {
        if (figure != null) {
            figure.remove(circle.player);
            figure = null;
        }
    }

    /** Darkness and malice punish the stare; objects and deceit punish the glance away. */
    private boolean stares() {
        Focus nature = Traits.weakness(Traits.of(circle.spirit.haunting().traits()));
        return nature == Focus.GLOWSTONE || nature == Focus.HOLY_WATER;
    }

    private void walk(Config.Section config) {
        angle += Math.toRadians(config.num("gaze_walk_degrees"));
        double radius = config.num("gaze_ring_radius");
        at = new Vec3(
                circle.altar.getX() + 0.5 + Math.cos(angle) * radius,
                circle.altar.getY(),
                circle.altar.getZ() + 0.5 + Math.sin(angle) * radius);
        // faces the way it walks
        float yaw = PacketEntity.yawToward(at, at.add(-Math.sin(angle), 0, Math.cos(angle)));
        if (figure == null) {
            figure = Body.show(circle.player, circle.spirit.haunting(), at, yaw);
        } else {
            figure.move(circle.player, at, yaw);
        }
    }

    private void snuff() {
        for (BlockPos candle : circle.candles) {
            var state = circle.level.getBlockState(candle);
            if (AbstractCandleBlock.isLit(state)) {
                AbstractCandleBlock.extinguish(null, state, circle.level, candle);
                circle.player.sendSystemMessage(Component.translatable("message.restless.gaze_broken"), true);
                return;
            }
        }
    }
}
