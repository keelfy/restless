package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Ticks;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.level.border.WorldBorder;

/**
 * The server tells this one client something untrue, and the truth a moment later. Nothing changes on the server, so a
 * lie that is cut short (the player leaves, dies, changes dimension) is corrected by the resync vanilla does then.
 */
final class Lies {
    private static final List<Holder<MobEffect>> FAKE_EFFECTS = List.of(MobEffects.WITHER, MobEffects.POISON);

    private Lies() {}

    static List<HauntEvent> all() {
        return List.of(
            // 153: never shown below one heart, or the client would open the death screen
            SimpleEvent.light("false_wound", EventTag.AFFLICTION,
                player -> player.getHealth() > Config.event("false_wound").num("min_health"),
                player -> {
                    Config.Section config = Config.event("false_wound");
                    float shown = Math.max((float) config.num("min_health"), player.getHealth() - (float) config.num("loss"));
                    player.connection.send(new ClientboundSetHealthPacket(shown, player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel()));
                    Scheduler.later(player, config.integer("ticks"), Lies::resendHealth);
                }),
            // 154: wither or poison hearts with no effect behind them
            SimpleEvent.light("false_hearts", EventTag.AFFLICTION,
                player -> FAKE_EFFECTS.stream().noneMatch(player::hasEffect),
                player -> {
                    Config.Section config = Config.event("false_hearts");
                    Holder<MobEffect> effect = FAKE_EFFECTS.get(player.getRandom().nextInt(FAKE_EFFECTS.size()));
                    player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), new MobEffectInstance(effect, config.integer("ticks")), false));
                    Scheduler.later(player, config.integer("ticks"), p -> {
                        // A real effect of the same kind may have started meanwhile; then the client already has the truth.
                        if (!p.hasEffect(effect)) {
                            p.connection.send(new ClientboundRemoveMobEffectPacket(p.getId(), effect));
                        }
                    });
                }),
            // 155
            light("false_hunger", EventTag.AFFLICTION, player -> {
                Config.Section config = Config.event("false_hunger");
                player.connection.send(new ClientboundSetHealthPacket(player.getHealth(), config.integer("shown_food"), 0));
                Scheduler.later(player, config.integer("ticks"), Lies::resendHealth);
            }),
            // 156: the vanilla elder guardian jump scare, far from any monument
            light("guardian_face", EventTag.PRESENCE, player -> player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT, 1))),
            // 157: with no totem in the inventory the client shows a plain one
            light("false_totem", EventTag.PRESENCE, player -> player.connection.send(new ClientboundEntityEventPacket(player, EntityEvent.PROTECTED_FROM_DEATH))),
            // 158: a title gone almost before it is read
            light("subliminal_title", EventTag.PRESENCE, player -> {
                Config.Section config = Config.event("subliminal_title");
                List<String> texts = config.texts("texts");
                player.connection.send(new ClientboundSetTitlesAnimationPacket(0, config.integer("ticks"), 0));
                player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal(texts.get(player.getRandom().nextInt(texts.size()))).withStyle(ChatFormatting.DARK_RED)));
                // Back to the vanilla fade times once it is gone, so the next real title is not cut short.
                Scheduler.later(player, config.integer("ticks") + 1, p -> p.connection.send(new ClientboundClearTitlesPacket(true)));
            }),
            // 160: the world border warning tint, with the border where it always was
            light("red_vignette", EventTag.PRESENCE, player -> {
                Config.Section config = Config.event("red_vignette");
                double distance = player.level().getWorldBorder().getDistanceToBorder(player);
                WorldBorder fake = new WorldBorder();
                fake.setWarningBlocks((int) Math.min(Integer.MAX_VALUE, distance / (1 - config.num("strength"))));
                player.connection.send(new ClientboundSetBorderWarningDistancePacket(fake));
                Scheduler.later(player, Ticks.SECOND * config.integer("seconds"),
                    p -> p.connection.send(new ClientboundSetBorderWarningDistancePacket(p.level().getWorldBorder())));
            }),
            // 162: a chat line in their own name that they never wrote
            light("own_voice", EventTag.SOUND, player -> {
                List<String> lines = Config.event("own_voice").texts("lines");
                player.sendSystemMessage(Component.literal("<" + player.getName().getString() + "> " + lines.get(player.getRandom().nextInt(lines.size()))));
            }),
            // 163: their own death message, in their own language, killed by nobody
            light("false_death", EventTag.PRESENCE, player -> player.sendSystemMessage(
                Component.translatable("death.attack.mob", player.getDisplayName(), Component.literal(Config.event("false_death").text("killer"))))),
            // 167: rain or a storm that only this player sees and hears
            SimpleEvent.light("private_rain", EventTag.PRESENCE,
                player -> player.level().canHaveWeather() && !player.level().isRaining() && player.level().canSeeSky(player.blockPosition()),
                Lies::privateRain),
            // 168: lightning that sets nothing on fire
            SimpleEvent.light("silent_lightning", EventTag.PRESENCE,
                player -> player.level().canHaveWeather() && player.level().canSeeSky(player.blockPosition()),
                player -> Assaults.strike(player.level(), BlockPos.containing(Nearby.randomAround(player, Config.event("silent_lightning").num("distance")))))
        );
    }

    /** An event that can always run. */
    private static HauntEvent light(String id, EventTag tag, Consumer<ServerPlayer> action) {
        return SimpleEvent.light(id, tag, player -> true, action);
    }

    private static void resendHealth(ServerPlayer player) {
        player.connection.send(new ClientboundSetHealthPacket(player.getHealth(), player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel()));
    }

    private static void privateRain(ServerPlayer player) {
        Config.Section config = Config.event("private_rain");
        boolean thunder = player.getRandom().nextDouble() < config.num("thunder_chance");
        player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0));
        player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 1));
        if (thunder) {
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 1));
        }
        Scheduler.later(player, Ticks.SECOND * config.integer("seconds"), p -> {
            ServerLevel level = p.level();
            // The real weather may have turned meanwhile; whatever it is now is what the client gets.
            if (!level.isRaining()) {
                p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0));
            }
            p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, level.getRainLevel(1)));
            p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, level.getThunderLevel(1)));
        });
    }
}
