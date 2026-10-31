package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.spirit.Story;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * A spirit left alone long enough grows up: a poltergeist turns raging, a shade a light-eater. It keeps a power floor,
 * takes a longer ritual and, if territorial, fouls its land faster. Personal spirits grow while their player is online;
 * the players it haunts (its own, or those in its radius) are told and it goes in their journal.
 */
public final class Evolution {
    private Evolution() {}

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % Config.section("evolution").integer("check_ticks") != 0) {
            return;
        }
        HauntData data = HauntData.get(server);
        long now = server.overworld().getGameTime();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            List<HauntData.Haunting> spirits = data.get(player.getUUID());
            for (HauntData.Haunting spirit : spirits == null ? List.<HauntData.Haunting>of() : spirits) {
                if (grows(spirit, now)) {
                    data.change(spirit.uuid(), HauntData.Haunting::evolve);
                    tell(player, spirit);
                }
            }
        }
        for (HauntData.Territorial spirit : data.territorial()) {
            ServerLevel level = server.getLevel(spirit.dimension());
            if (level != null && grows(spirit.haunting(), now)) {
                data.change(spirit.haunting().uuid(), HauntData.Haunting::evolve);
                level.players().stream().filter(spirit::reaches).forEach(p -> tell(p, spirit.haunting()));
            }
        }
    }

    private static boolean grows(HauntData.Haunting spirit, long now) {
        return !spirit.evolved() && Power.grown(now - spirit.bornAt());
    }

    private static void tell(ServerPlayer player, HauntData.Haunting spirit) {
        Component was = Component.translatable("spirit.restless." + spirit.spirit());
        Component now = Component.translatable("spirit.restless." + spirit.spirit() + "_evolved");
        player.level()
                .playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0f, 0.6f);
        player.sendSystemMessage(Component.translatable("message.restless.evolved", was, now), true);
        Journal.note(
                player, Notebook.Kind.EVOLVED, spirit, Story.of(spirit.seed()).name(), was, now);
    }

    /** The spirit's type as players read it: the grown-up name once it has evolved. */
    public static Component name(HauntData.Haunting spirit) {
        return Component.translatable("spirit.restless." + spirit.spirit() + (spirit.evolved() ? "_evolved" : ""));
    }
}
