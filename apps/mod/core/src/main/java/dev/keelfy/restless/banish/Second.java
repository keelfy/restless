package dev.keelfy.restless.banish;

import dev.keelfy.restless.config.Config;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Two at the altar: another player standing in the circle helps the one who holds it. Holding a bell or a flint and
 * steel there in the fury cuts the fury short once a ritual; in return, the spirit's strikes land on everyone in the
 * circle.
 */
public final class Second {
    private Second() {}

    /** Every other living player within reach of the altar. */
    public static List<ServerPlayer> inside(Circle circle) {
        double reach = Config.section("ritual").num("second_radius");
        return circle
                .level
                .getPlayers(p -> p != circle.player
                        && p.isAlive()
                        && !p.isSpectator()
                        && p.position().closerThan(Vec3.atBottomCenterOf(circle.altar), reach))
                .stream()
                .toList();
    }

    /** Whom the spirit's strikes land on: the one who holds the ritual, then every helper in the circle. */
    public static List<ServerPlayer> targets(Circle circle) {
        List<ServerPlayer> all = new ArrayList<>();
        all.add(circle.player);
        all.addAll(inside(circle));
        return all;
    }

    /** A fury wave: a helper with a bell or a flint and steel cuts the fury, once a ritual. */
    static void wave(Circle circle) {
        if (circle.seconded) {
            return;
        }
        for (ServerPlayer helper : inside(circle)) {
            if (holds(helper, Items.BELL) || holds(helper, Items.FLINT_AND_STEEL)) {
                circle.seconded = true;
                circle.plan = circle.plan.longerFury(-Config.section("ritual").integer("second_fury_waves"));
                Component line = Component.translatable("message.restless.ritual_seconded", helper.getDisplayName());
                circle.player.sendSystemMessage(line, true);
                helper.sendSystemMessage(line, true);
                return;
            }
        }
    }

    private static boolean holds(ServerPlayer player, Item item) {
        return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }
}
