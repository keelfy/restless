package dev.keelfy.restless.banish;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.spirit.Story;
import dev.keelfy.restless.state.HauntData;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * The name: in the fury or the leaving the spirit listens to what its player says. Its name, as the gravestone gives
 * it, cuts the fury short, or in the leaving lets it go at once; it works once a ritual. Anything else said then is a
 * wrong name and angers it.
 */
public final class Name {
    private Name() {}

    /** A chat line from {@code player}; does nothing unless their ritual is in the fury or the leaving. */
    public static void said(ServerPlayer player, String line) {
        Circle.of(player).ifPresent(circle -> said(circle, line));
    }

    static void said(Circle circle, String line) {
        Phase phase = circle.phase().orElse(Phase.CALL);
        if (phase != Phase.FURY && phase != Phase.LEAVING) {
            return;
        }
        Config.Section config = Config.section("ritual");
        if (!plain(line).equals(plain(Story.of(circle.spirit.haunting().seed()).name()))) {
            HauntData.get(circle.level.getServer())
                    .change(circle.spirit.haunting().uuid(), h -> h.angered(config.num("name_anger")));
            circle.level.playSound(null, circle.altar, SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 0.6f, 1.4f);
            circle.player.sendSystemMessage(Component.translatable("message.restless.name_wrong"), true);
            return;
        }
        if (circle.named) {
            return;
        }
        circle.named = true;
        circle.level.playSound(null, circle.altar, SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.0f, 0.5f);
        circle.player.sendSystemMessage(Component.translatable("message.restless.name_right"), true);
        if (phase == Phase.LEAVING) {
            circle.release();
        } else {
            circle.plan = circle.plan.longerFury(-config.integer("name_waves"));
        }
    }

    /** Lower case letters and digits only, single spaces: " Agnes Wilde!" says "agnes wilde". */
    static String plain(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }
}
