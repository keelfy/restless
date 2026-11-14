package dev.keelfy.restless.banish;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.block.Altar;
import dev.keelfy.restless.content.block.UnlitLights;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.event.kit.Nearby;
import dev.keelfy.restless.haunt.event.kit.Taken;
import dev.keelfy.restless.haunt.harm.Band;
import dev.keelfy.restless.spirit.Focus;
import dev.keelfy.restless.spirit.Traits;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How each nature fights in the fury: objects throw the offering off the altar, darkness puts out the other lights,
 * malice strikes the player, deceit shows the player their candles as out. The censer changes none of it.
 */
public final class Natures {
    private Natures() {}

    /** The fury wave's nature act: with the configured chance, the spirit acts by its nature. */
    static void fight(Circle circle) {
        if (circle.player.getRandom().nextDouble() >= Config.section("ritual").num("nature_chance")) {
            return;
        }
        Focus nature = Traits.weakness(Traits.of(circle.spirit.haunting().traits()));
        switch (nature) {
            case IRON -> objects(circle);
            case GLOWSTONE -> darkness(circle);
            case HOLY_WATER -> malice(circle);
            case MIRROR -> deceit(circle);
        }
    }

    /** The offering leaves the altar and lies a couple of blocks off; the player must lay it back. */
    public static void objects(Circle circle) {
        ServerLevel level = circle.level;
        BlockState state = level.getBlockState(circle.altar);
        if (state.getValue(Altar.HELD) != circle.held) {
            return;
        }
        level.setBlock(circle.altar, state.setValue(Altar.HELD, Altar.Held.EMPTY), 3);
        var random = circle.player.getRandom();
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = Config.section("ritual").num("throw_distance");
        ItemEntity item = new ItemEntity(
                level,
                circle.altar.getX() + 0.5 + Math.cos(angle) * distance,
                circle.altar.getY() + 1.2,
                circle.altar.getZ() + 0.5 + Math.sin(angle) * distance,
                circle.held.stack());
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        circle.player.sendSystemMessage(Component.translatable("message.restless.offering_thrown"), true);
    }

    /** Every torch and lantern around the altar goes out for a while; the ritual's candles burn on. */
    public static void darkness(Circle circle) {
        ServerLevel level = circle.level;
        Config.Section config = Config.section("ritual");
        for (BlockPos pos : Nearby.all(
                level,
                circle.altar,
                config.integer("darkness_radius"),
                s -> UnlitLights.isTorch(s) || UnlitLights.isLantern(s))) {
            Restore.swap(
                    level, pos, UnlitLights.extinguished(level.getBlockState(pos)), config.integer("darkness_ticks"));
        }
        circle.player.sendSystemMessage(Component.translatable("message.restless.lights_out"), true);
    }

    /** A blow to the player, never past what the threat band leaves them. */
    public static void malice(Circle circle) {
        float allowed = Math.min(
                (float) Config.section("ritual").num("malice_damage"), Band.THREAT.room(circle.player.getHealth()));
        if (allowed > 0.0f) {
            circle.player.hurtServer(circle.level, Taken.by(circle.level, Taken.TAKEN), allowed);
        }
    }

    /** Some candles look out to this player only; the server keeps them lit, and the client is told the truth later. */
    public static void deceit(Circle circle) {
        Config.Section config = Config.section("ritual");
        List<BlockPos> shown = new ArrayList<>(circle.candles);
        var random = circle.player.getRandom();
        while (shown.size() > config.integer("deceit_candles")) {
            shown.remove(random.nextInt(shown.size()));
        }
        ServerLevel level = circle.level;
        for (BlockPos pos : shown) {
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(AbstractCandleBlock.LIT)) {
                circle.player.connection.send(
                        new ClientboundBlockUpdatePacket(pos, state.setValue(AbstractCandleBlock.LIT, false)));
            }
        }
        Scheduler.later(circle.player, config.integer("deceit_ticks"), p -> {
            if (p.level() == level) {
                shown.forEach(pos -> p.connection.send(new ClientboundBlockUpdatePacket(level, pos)));
            }
        });
    }
}
