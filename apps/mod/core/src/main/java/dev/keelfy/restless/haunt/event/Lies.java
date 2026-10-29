package dev.keelfy.restless.haunt.event;

import com.mojang.authlib.GameProfile;
import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.Hardcore;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntTier;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Ticks;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;

/**
 * The server tells this one client something untrue, and the truth a moment later. Nothing changes on the server, so a
 * lie that is cut short (the player leaves, dies, changes dimension) is corrected by the resync vanilla does then. The
 * false advancement is the exception: it is granted for real, for two ticks, so the client shows its toast. Under
 * {@linkplain Hardcore hardcore} the lies that have a real counterpart (a wound, hearts, hunger, night, a slot, lightning)
 * come true instead.
 */
final class Lies {
    /** The one criterion of the mod's false advancements, which nothing in the game can meet. */
    private static final String IMPOSSIBLE = "impossible";
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
                    if (Hardcore.on(player.level())) {
                        // Real, and still never below the floor.
                        player.hurtServer(player.level(), player.damageSources().magic(), player.getHealth() - shown);
                        return;
                    }
                    player.connection.send(new ClientboundSetHealthPacket(shown, player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel()));
                    Scheduler.later(player, config.integer("ticks"), Lies::resendHealth);
                }),
            // 154: wither or poison hearts with no effect behind them
            SimpleEvent.light("false_hearts", EventTag.AFFLICTION,
                player -> FAKE_EFFECTS.stream().noneMatch(player::hasEffect),
                player -> {
                    Config.Section config = Config.event("false_hearts");
                    Holder<MobEffect> effect = FAKE_EFFECTS.get(player.getRandom().nextInt(FAKE_EFFECTS.size()));
                    if (Hardcore.on(player.level())) {
                        player.addEffect(new MobEffectInstance(effect, config.integer("ticks")));
                        return;
                    }
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
                if (Hardcore.on(player.level())) {
                    player.getFoodData().setFoodLevel(Math.max(config.integer("shown_food"), player.getFoodData().getFoodLevel() - config.integer("hardcore_loss")));
                    return;
                }
                player.connection.send(new ClientboundSetHealthPacket(player.getHealth(), config.integer("shown_food"), 0));
                Scheduler.later(player, config.integer("ticks"), Lies::resendHealth);
            }),
            // 156: the vanilla elder guardian jump scare, far from any monument
            light("guardian_face", EventTag.PRESENCE, player -> {
                player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT, 1));
                // As the elder guardian itself gives.
                if (Hardcore.on(player.level())) {
                    Config.Section config = Config.event("guardian_face");
                    player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, config.integer("hardcore_fatigue_ticks"), config.integer("hardcore_fatigue_level")));
                }
            }),
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
                player -> Assaults.strike(player.level(), BlockPos.containing(Nearby.randomAround(player, Config.event("silent_lightning").num("distance"))),
                    !Hardcore.on(player.level()))),
            // 159: night, with the sky stopped, for this player alone; the real time comes back after
            SimpleEvent.light("false_night", EventTag.PRESENCE,
                player -> player.level().dimension() == Level.OVERWORLD && !player.level().isDarkOutside(),
                player -> {
                    Config.Section config = Config.event("false_night");
                    ServerClockManager clocks = player.level().getServer().clockManager();
                    Holder<WorldClock> clock = player.level().getServer().registryAccess().getOrThrow(WorldClocks.OVERWORLD);
                    long now = clocks.getInstance(clock).totalTicks();
                    long night = now - Math.floorMod(now, config.integer("day_ticks")) + config.integer("time_of_day");
                    if (Hardcore.on(player.level())) {
                        // For everyone, as lost time is; the cycle goes on from there.
                        clocks.addTicks(clock, (int) (night - now));
                        return;
                    }
                    player.connection.send(new ClientboundSetTimePacket(player.level().getGameTime(), Map.of(clock, new ClockNetworkState(night, 0.0F, 0.0F))));
                    Scheduler.later(player, Ticks.SECOND * config.integer("seconds"), p -> p.connection.send(p.level().getServer().clockManager().createFullSyncPacket()));
                }),
            // 161: an empty slot shows an item that is not there; any click on the inventory shows the truth sooner
            SimpleEvent.light("false_slot", EventTag.PRESENCE, player -> !emptySlots(player).isEmpty(), player -> {
                Config.Section config = Config.event("false_slot");
                List<Integer> empty = emptySlots(player);
                int slot = empty.get(player.getRandom().nextInt(empty.size()));
                ItemStack shown = new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse(pick(player, config.texts("items")))).orElse(Items.BONE));
                if (shown.is(Items.PLAYER_HEAD)) {
                    shown.set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.getGameProfile()));
                }
                InventoryMenu menu = player.inventoryMenu;
                if (Hardcore.on(player.level())) {
                    menu.getSlot(slot).set(shown);
                    return;
                }
                player.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.getStateId(), slot, shown));
                Scheduler.later(player, Ticks.SECOND * config.integer("seconds"),
                    p -> p.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.getStateId(), slot, menu.getSlot(slot).getItem().copy())));
            }),
            // 165: an advancement toast, gone from the advancements as soon as it is shown
            SimpleEvent.light("false_advancement", EventTag.PRESENCE, player -> true, player -> {
                AdvancementHolder advancement = player.level().getServer().getAdvancements()
                    .get(Identifier.fromNamespaceAndPath(Restless.MOD_ID, pick(player, Config.event("false_advancement").texts("advancements"))));
                if (advancement != null && player.getAdvancements().award(advancement, IMPOSSIBLE)) {
                    Scheduler.later(player, Config.event("false_advancement").integer("revoke_ticks"), p -> p.getAdvancements().revoke(advancement, IMPOSSIBLE));
                }
            }),
            // 166: a step where the spirit is, and the head turns toward it, slowly, by itself
            SimpleEvent.harmful("turned_head", EventTag.PRESENCE, player -> true, player -> {
                Config.Section config = Config.event("turned_head");
                Vec3 at = Nearby.source(player, config.num("distance"));
                Nearby.sound(player, player.level().getBlockState(BlockPos.containing(at).below()).getSoundType().getStepSound(), at, config.tone("sound"));
                float toward = (float) (Mth.atan2(at.z - player.getZ(), at.x - player.getX()) * Mth.RAD_TO_DEG) - 90.0F;
                int steps = config.integer("steps");
                float step = Mth.wrapDegrees(toward - player.getYRot()) / steps;
                Scheduler.repeat(player, config.integer("delay_ticks"), 1, (p, n) -> {
                    p.connection.send(new ClientboundPlayerRotationPacket(step, true, 0.0F, true));
                    return n + 1 < steps;
                });
            }),
            // 124: someone joins, is in the player list (which then opens even in single player), and leaves a while later
            stranger("stranger_joins", player -> {
                Config.Section config = Config.event("stranger_joins");
                GameProfile profile = Figure.nobody(player, pick(player, config.texts("names")));
                player.connection.send(listed(player, profile, config.integer("latency")));
                player.sendSystemMessage(Component.translatable("multiplayer.player.joined", profile.name()).withStyle(ChatFormatting.YELLOW));
                Scheduler.later(player, Ticks.SECOND * config.integer("seconds"), p -> {
                    p.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(profile.id())));
                    p.sendSystemMessage(Component.translatable("multiplayer.player.left", profile.name()).withStyle(ChatFormatting.YELLOW));
                });
            }),
            // 124: someone else's death, in the player's own language
            stranger("stranger_death", player -> {
                Config.Section config = Config.event("stranger_death");
                player.sendSystemMessage(Component.translatable(pick(player, config.texts("deaths")), pick(player, config.texts("names"))));
            })
        );
    }

    /** An event that can always run. */
    private static HauntEvent light(String id, EventTag tag, Consumer<ServerPlayer> action) {
        return SimpleEvent.light(id, tag, player -> true, action);
    }

    /** Inventory and hotbar slots, as the inventory menu numbers them, with nothing in them. */
    private static List<Integer> emptySlots(ServerPlayer player) {
        List<Integer> empty = new ArrayList<>();
        for (int slot = InventoryMenu.INV_SLOT_START; slot < InventoryMenu.USE_ROW_SLOT_END; slot++) {
            if (player.inventoryMenu.getSlot(slot).getItem().isEmpty()) {
                empty.add(slot);
            }
        }
        return empty;
    }

    /** An event about another player, that can always run. */
    private static HauntEvent stranger(String id, Consumer<ServerPlayer> action) {
        return new SimpleEvent(Identifier.parse(id), HauntTier.LIGHT, Set.of(EventTag.PRESENCE, EventTag.PLAYER), player -> true, action);
    }

    private static String pick(ServerPlayer player, List<String> texts) {
        return texts.get(player.getRandom().nextInt(texts.size()));
    }

    /**
     * A player list entry for {@code profile}. The packet can only be built from a real player, so it is read back from
     * its own wire format, written with its own codecs.
     */
    private static ClientboundPlayerInfoUpdatePacket listed(ServerPlayer player, GameProfile profile, int latency) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        try {
            // Each entry's fields come in the actions' declared order.
            buffer.writeEnumSet(EnumSet.of(Action.ADD_PLAYER, Action.UPDATE_GAME_MODE, Action.UPDATE_LISTED, Action.UPDATE_LATENCY), Action.class);
            buffer.writeVarInt(1);
            buffer.writeUUID(profile.id());
            ByteBufCodecs.PLAYER_NAME.encode(buffer, profile.name());
            ByteBufCodecs.GAME_PROFILE_PROPERTIES.encode(buffer, profile.properties());
            GameType.STREAM_CODEC.encode(buffer, GameType.SURVIVAL);
            buffer.writeBoolean(true);
            buffer.writeVarInt(latency);
            return ClientboundPlayerInfoUpdatePacket.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
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
