package dev.keelfy.restless.haunt.event;

import com.mojang.authlib.GameProfile;
import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.Hardcore;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntTier;
import dev.keelfy.restless.haunt.Said;
import dev.keelfy.restless.haunt.ScenarioContext;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Ticks;
import dev.keelfy.restless.haunt.TurnPayload;
import dev.keelfy.restless.haunt.VignettePayload;
import dev.keelfy.restless.platform.Platform;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSwingAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;

/**
 * The server tells this one client something untrue, and the truth a moment later. Nothing changes on the server, so a
 * lie that is cut short (the player leaves, dies, changes dimension) is corrected by the resync vanilla does then. The
 * false advancement is the exception: it is granted for real, for two ticks, so the client shows its toast. Under
 * {@linkplain Hardcore hardcore} the lies that have a real counterpart (a wound, hearts, hunger, night, a slot,
 * lightning) come true instead.
 */
final class Lies {
    /** The one criterion of the mod's false advancements, which nothing in the game can meet. */
    private static final String IMPOSSIBLE = "impossible";

    private static final List<Holder<MobEffect>> FAKE_EFFECTS = List.of(MobEffects.WITHER, MobEffects.POISON);

    private Lies() {}

    static List<HauntEvent> all() {
        return List.of(
                // 153: never shown below one heart, or the client would open the death screen
                SimpleEvent.light(
                        "false_wound",
                        EventTag.AFFLICTION,
                        player ->
                                player.getHealth() > Config.event("false_wound").num("min_health"),
                        player -> {
                            Config.Section config = Config.event("false_wound");
                            float shown = Math.max(
                                    (float) config.num("min_health"), player.getHealth() - (float) config.num("loss"));
                            if (Hardcore.on(player.level())) {
                                // Real, and still never below the floor.
                                player.hurtServer(
                                        player.level(), player.damageSources().magic(), player.getHealth() - shown);
                                return;
                            }
                            player.connection.send(new ClientboundSetHealthPacket(
                                    shown,
                                    player.getFoodData().getFoodLevel(),
                                    player.getFoodData().getSaturationLevel()));
                            Scheduler.later(player, config.integer("ticks"), Lies::resendHealth);
                        }),
                // 154: wither or poison hearts with no effect behind them
                SimpleEvent.light(
                        "false_hearts",
                        EventTag.AFFLICTION,
                        player -> FAKE_EFFECTS.stream().noneMatch(player::hasEffect),
                        player -> {
                            Config.Section config = Config.event("false_hearts");
                            Holder<MobEffect> effect =
                                    FAKE_EFFECTS.get(player.getRandom().nextInt(FAKE_EFFECTS.size()));
                            if (Hardcore.on(player.level())) {
                                player.addEffect(new MobEffectInstance(effect, config.integer("ticks")));
                                return;
                            }
                            player.connection.send(new ClientboundUpdateMobEffectPacket(
                                    player.getId(), new MobEffectInstance(effect, config.integer("ticks")), false));
                            Scheduler.later(player, config.integer("ticks"), p -> {
                                // A real effect of the same kind may have started meanwhile; then the client already
                                // has the truth.
                                if (!p.hasEffect(effect)) {
                                    p.connection.send(new ClientboundRemoveMobEffectPacket(p.getId(), effect));
                                }
                            });
                        }),
                // 155
                light("false_hunger", EventTag.AFFLICTION, player -> {
                    Config.Section config = Config.event("false_hunger");
                    if (Hardcore.on(player.level())) {
                        player.getFoodData()
                                .setFoodLevel(Math.max(
                                        config.integer("shown_food"),
                                        player.getFoodData().getFoodLevel() - config.integer("hardcore_loss")));
                        return;
                    }
                    player.connection.send(
                            new ClientboundSetHealthPacket(player.getHealth(), config.integer("shown_food"), 0));
                    Scheduler.later(player, config.integer("ticks"), Lies::resendHealth);
                }),
                // 156: the vanilla elder guardian jump scare, far from any monument
                light("guardian_face", EventTag.PRESENCE, player -> {
                    player.connection.send(
                            new ClientboundGameEventPacket(ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT, 1));
                    // As the elder guardian itself gives.
                    if (Hardcore.on(player.level())) {
                        Config.Section config = Config.event("guardian_face");
                        player.addEffect(new MobEffectInstance(
                                MobEffects.MINING_FATIGUE,
                                config.integer("hardcore_fatigue_ticks"),
                                config.integer("hardcore_fatigue_level")));
                    }
                }),
                // 157: with no totem in the inventory the client shows a plain one
                light(
                        "false_totem",
                        EventTag.PRESENCE,
                        player -> player.connection.send(
                                new ClientboundEntityEventPacket(player, EntityEvent.PROTECTED_FROM_DEATH))),
                // 158: a title gone almost before it is read
                saying("subliminal_title", EventTag.PRESENCE, config -> config.texts("texts"), (player, text) -> {
                    Config.Section config = Config.event("subliminal_title");
                    player.connection.send(new ClientboundSetTitlesAnimationPacket(0, config.integer("ticks"), 0));
                    player.connection.send(new ClientboundSetTitleTextPacket(
                            Component.literal(text).withStyle(ChatFormatting.DARK_RED)));
                    // Back to the vanilla fade times once it is gone, so the next real title is not cut short.
                    Scheduler.later(
                            player,
                            config.integer("ticks") + 1,
                            p -> p.connection.send(new ClientboundClearTitlesPacket(true)));
                }),
                // 160: the screen's edges go red, as at the world border; see RedVignette. A client without the mod is
                // shown the
                // border warning tint instead, with the border where it always was.
                light("red_vignette", EventTag.PRESENCE, player -> {
                    Config.Section config = Config.event("red_vignette");
                    if (Platform.get().canReceive(player, VignettePayload.TYPE)) {
                        player.connection.send(new ClientboundCustomPayloadPacket(new VignettePayload(
                                (float) config.num("strength"), Ticks.SECOND * config.integer("seconds"))));
                        return;
                    }
                    double distance = player.level().getWorldBorder().getDistanceToBorder(player);
                    WorldBorder fake = new WorldBorder();
                    fake.setWarningBlocks((int) Math.min(Integer.MAX_VALUE, distance / (1 - config.num("strength"))));
                    player.connection.send(new ClientboundSetBorderWarningDistancePacket(fake));
                    Scheduler.later(
                            player,
                            Ticks.SECOND * config.integer("seconds"),
                            p -> p.connection.send(new ClientboundSetBorderWarningDistancePacket(
                                    p.level().getWorldBorder())));
                }),
                // 162: a chat line in their own name that they never wrote
                saying(
                        "own_voice",
                        EventTag.SOUND,
                        config -> config.texts("lines"),
                        (player, line) -> player.sendSystemMessage(
                                Component.literal("<" + player.getName().getString() + "> " + line))),
                // 163: their own death message, in their own language, killed by nobody
                saying(
                        "false_death",
                        EventTag.PRESENCE,
                        config -> List.of(config.text("killer")),
                        (player, killer) -> player.sendSystemMessage(Component.translatable(
                                "death.attack.mob",
                                player.getDisplayName(),
                                Component.literal(ScenarioContext.speaker(killer))))),
                // 167: rain or a storm that only this player sees and hears
                SimpleEvent.light(
                        "private_rain",
                        EventTag.PRESENCE,
                        player -> player.level().canHaveWeather()
                                && !player.level().isRaining()
                                && player.level().canSeeSky(player.blockPosition()),
                        Lies::privateRain),
                // 168: lightning that sets nothing on fire
                SimpleEvent.light(
                        "silent_lightning",
                        EventTag.PRESENCE,
                        player -> player.level().canHaveWeather()
                                && player.level().canSeeSky(player.blockPosition()),
                        player -> Assaults.strike(
                                player.level(),
                                BlockPos.containing(Nearby.randomAround(
                                        player, Config.event("silent_lightning").num("distance"))),
                                !Hardcore.on(player.level()))),
                // 159: night, with the sky stopped, for this player alone; the real time comes back after
                SimpleEvent.light(
                        "false_night",
                        EventTag.PRESENCE,
                        player -> player.level().dimension() == Level.OVERWORLD
                                && !player.level().isDarkOutside(),
                        player -> {
                            Config.Section config = Config.event("false_night");
                            ServerClockManager clocks =
                                    player.level().getServer().clockManager();
                            Holder<WorldClock> clock =
                                    player.level().getServer().registryAccess().getOrThrow(WorldClocks.OVERWORLD);
                            long now = clocks.getInstance(clock).totalTicks();
                            long night = now
                                    - Math.floorMod(now, config.integer("day_ticks"))
                                    + config.integer("time_of_day");
                            if (Hardcore.on(player.level())) {
                                // For everyone, as lost time is; the cycle goes on from there.
                                clocks.addTicks(clock, (int) (night - now));
                                return;
                            }
                            player.connection.send(new ClientboundSetTimePacket(
                                    player.level().getGameTime(),
                                    Map.of(clock, new ClockNetworkState(night, 0.0F, 0.0F))));
                            Scheduler.later(
                                    player,
                                    Ticks.SECOND * config.integer("seconds"),
                                    p -> p.connection.send(
                                            p.level().getServer().clockManager().createFullSyncPacket()));
                        }),
                // 161: an empty slot shows an item that is not there; any click on the inventory shows the truth sooner
                SimpleEvent.light(
                        "false_slot",
                        EventTag.PRESENCE,
                        player -> !emptySlots(player).isEmpty(),
                        player -> {
                            Config.Section config = Config.event("false_slot");
                            List<Integer> empty = emptySlots(player);
                            int slot = empty.get(player.getRandom().nextInt(empty.size()));
                            ItemStack shown = new ItemStack(BuiltInRegistries.ITEM
                                    .getOptional(Identifier.parse(pick(player, config.texts("items"))))
                                    .orElse(Items.BONE));
                            if (shown.is(Items.PLAYER_HEAD)) {
                                shown.set(
                                        DataComponents.PROFILE,
                                        ResolvableProfile.createResolved(player.getGameProfile()));
                            }
                            InventoryMenu menu = player.inventoryMenu;
                            if (Hardcore.on(player.level())) {
                                menu.getSlot(slot).set(shown);
                                return;
                            }
                            player.connection.send(new ClientboundContainerSetSlotPacket(
                                    menu.containerId, menu.getStateId(), slot, shown));
                            Scheduler.later(
                                    player,
                                    Ticks.SECOND * config.integer("seconds"),
                                    p -> p.connection.send(new ClientboundContainerSetSlotPacket(
                                            menu.containerId,
                                            menu.getStateId(),
                                            slot,
                                            menu.getSlot(slot).getItem().copy())));
                        }),
                // 165: an advancement toast, gone from the advancements as soon as it is shown
                saying(
                        "false_advancement",
                        EventTag.PRESENCE,
                        config -> config.texts("advancements"),
                        (player, name) -> {
                            AdvancementHolder advancement = player.level()
                                    .getServer()
                                    .getAdvancements()
                                    .get(Identifier.fromNamespaceAndPath(Restless.MOD_ID, name));
                            if (advancement != null && player.getAdvancements().award(advancement, IMPOSSIBLE)) {
                                Scheduler.later(
                                        player,
                                        Config.event("false_advancement").integer("revoke_ticks"),
                                        p -> p.getAdvancements().revoke(advancement, IMPOSSIBLE));
                            }
                        }),
                // 166: a step where the spirit is, and the head turns toward it, slowly, by itself
                SimpleEvent.harmful("turned_head", EventTag.PRESENCE, SimpleEvent.ALWAYS, player -> {
                    Config.Section config = Config.event("turned_head");
                    Vec3 at = Nearby.source(player, config.num("distance"));
                    Nearby.sound(
                            player,
                            player.level()
                                    .getBlockState(BlockPos.containing(at).below())
                                    .getSoundType()
                                    .getStepSound(),
                            at,
                            config.tone("sound"));
                    // Turned smoothly on the client, see HeadTurn; from wherever the player looks by then.
                    Scheduler.later(player, config.integer("delay_ticks"), p -> {
                        if (!Platform.get().canReceive(p, TurnPayload.TYPE)) {
                            return;
                        }
                        float toward = (float) (Mth.atan2(at.z - p.getZ(), at.x - p.getX()) * Mth.RAD_TO_DEG) - 90.0F;
                        // Up or down as well, so a player looking at the floor ends up looking at the spirit, not at
                        // the floor.
                        double flat = Math.hypot(at.x - p.getX(), at.z - p.getZ());
                        float up = (float) (-Mth.atan2(at.y + 1.0 - p.getEyeY(), flat) * Mth.RAD_TO_DEG) - p.getXRot();
                        p.connection.send(new ClientboundCustomPayloadPacket(
                                new TurnPayload(Mth.wrapDegrees(toward - p.getYRot()), up, config.integer("ticks"))));
                    });
                }),
                // 124: someone joins, is in the player list (which then opens even in single player), and leaves a
                // while later
                stranger("stranger_joins", player -> {
                    Config.Section config = Config.event("stranger_joins");
                    GameProfile profile = Figure.nobody(player, Figure.nickname(player));
                    player.connection.send(listed(player, profile, config.integer("latency")));
                    player.sendSystemMessage(Component.translatable("multiplayer.player.joined", profile.name())
                            .withStyle(ChatFormatting.YELLOW));
                    Scheduler.later(player, Ticks.SECOND * config.integer("seconds"), p -> {
                        p.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(profile.id())));
                        p.sendSystemMessage(Component.translatable("multiplayer.player.left", profile.name())
                                .withStyle(ChatFormatting.YELLOW));
                    });
                }),
                // 202: a whisper, styled as /msg, from a real player online who never sent it; off by default, as it
                // puts words in a real person's mouth
                new SimpleEvent(
                        Identifier.parse("false_whisper"),
                        HauntTier.LIGHT,
                        Set.of(EventTag.PRESENCE, EventTag.PLAYER),
                        player -> !others(player).isEmpty()
                                && Said.left(
                                        player,
                                        "false_whisper",
                                        Config.event("false_whisper").texts("lines")),
                        player -> {
                            List<ServerPlayer> others = others(player);
                            String line = Said.pick(
                                    player,
                                    "false_whisper",
                                    Config.event("false_whisper").texts("lines"));
                            if (line != null && !others.isEmpty()) {
                                ServerPlayer from =
                                        others.get(player.getRandom().nextInt(others.size()));
                                player.sendSystemMessage(Component.translatable(
                                                "commands.message.display.incoming",
                                                from.getDisplayName(),
                                                Component.literal(line))
                                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                            }
                        }),
                // 124: someone else's death, in the player's own language
                new SimpleEvent(
                        Identifier.parse("stranger_death"),
                        HauntTier.LIGHT,
                        Set.of(EventTag.PRESENCE, EventTag.PLAYER),
                        player -> Said.left(
                                player,
                                "stranger_death",
                                Config.event("stranger_death").texts("deaths")),
                        player -> {
                            String death = Said.pick(
                                    player,
                                    "stranger_death",
                                    Config.event("stranger_death").texts("deaths"));
                            if (death != null) {
                                player.sendSystemMessage(Component.translatable(death, Figure.nickname(player)));
                            }
                        }),
                // 342: the carved pumpkin's blur over the screen
                light("pumpkin_head", EventTag.PRESENCE, Lies::pumpkinHead),
                // 343: the experience bar drops to zero
                SimpleEvent.light(
                        "xp_drain",
                        EventTag.PRESENCE,
                        player -> player.experienceLevel > 0 || player.experienceProgress > 0,
                        player -> {
                            player.connection.send(new ClientboundSetExperiencePacket(0.0F, 0, 0));
                            Scheduler.later(
                                    player,
                                    Ticks.SECOND * Config.event("xp_drain").integer("seconds"),
                                    Lies::resendExperience);
                        }),
                // 344: on fire on screen, with nothing burning
                SimpleEvent.light(
                        "phantom_flames",
                        EventTag.PRESENCE,
                        player -> !player.isOnFire() && !player.isInWater(),
                        Lies::phantomFlames),
                // 345: air bubbles on dry land, draining
                SimpleEvent.light(
                        "phantom_drowning",
                        EventTag.PRESENCE,
                        player -> player.getAirSupply() >= player.getMaxAirSupply(),
                        Lies::phantomDrowning),
                // 346: frost creeping over the screen, with no freezing
                SimpleEvent.light(
                        "phantom_frost", EventTag.PRESENCE, player -> player.getTicksFrozen() == 0, Lies::phantomFrost),
                // 350: another moon, for this player alone; the real time comes back after
                SimpleEvent.light(
                        "moon_phase",
                        EventTag.PRESENCE,
                        player -> player.level().dimension() == Level.OVERWORLD
                                && player.level().isDarkOutside(),
                        Lies::moonPhase),
                // 352: a boss bar with a name and no boss, filling and emptying
                light("false_boss_bar", EventTag.PRESENCE, Lies::falseBossBar),
                // 353: a line on the action bar, gone in a few ticks
                saying("action_bar_whisper", EventTag.PRESENCE, config -> config.texts("lines"), (player, line) -> {
                    player.connection.send(new ClientboundSetActionBarTextPacket(
                            Component.literal(line).withStyle(ChatFormatting.DARK_RED)));
                    Scheduler.later(
                            player,
                            Config.event("action_bar_whisper").integer("ticks"),
                            p -> p.connection.send(new ClientboundSetActionBarTextPacket(Component.empty())));
                }),
                // 356: golden hearts, draining
                SimpleEvent.light(
                        "false_absorption",
                        EventTag.PRESENCE,
                        player -> player.getAbsorptionAmount() <= 0,
                        Lies::falseAbsorption),
                // 357: the armor bar disappears
                SimpleEvent.light(
                        "empty_armor_bar",
                        EventTag.PRESENCE,
                        player -> player.getArmorValue() > 0,
                        Lies::emptyArmorBar),
                // 359: the hurt tilt, with no damage
                light(
                        "hurt_tilt",
                        EventTag.PRESENCE,
                        player -> player.connection.send(new ClientboundHurtAnimationPacket(
                                player.getId(), player.getRandom().nextFloat() * 360.0F - 180.0F))),
                // 360: the player's own hand swings by itself
                light("own_hand_swings", EventTag.PRESENCE, Lies::ownHandSwings),
                // 361: a flash of night vision, then darkness
                SimpleEvent.light(
                        "night_eyes",
                        EventTag.PRESENCE,
                        player -> !player.hasEffect(MobEffects.NIGHT_VISION) && !player.hasEffect(MobEffects.DARKNESS),
                        Lies::nightEyes),
                // 362: every sound stopped, again and again
                light("dead_silence", EventTag.SOUND, Lies::deadSilence),
                // 363: everything held or on the hotbar on cooldown
                SimpleEvent.light(
                        "all_on_cooldown",
                        EventTag.PRESENCE,
                        player -> !heldItems(player).isEmpty(),
                        Lies::allOnCooldown),
                // 365: a full moon at midnight, in the middle of the day
                SimpleEvent.light(
                        "noon_moon",
                        EventTag.PRESENCE,
                        player -> player.level().dimension() == Level.OVERWORLD
                                && !player.level().isDarkOutside(),
                        Lies::noonMoon));
    }

    private static List<ServerPlayer> others(ServerPlayer player) {
        return player.level().getServer().getPlayerList().getPlayers().stream()
                .filter(p -> p != player)
                .toList();
    }

    /** An event that can always run. */
    private static HauntEvent light(String id, EventTag tag, Consumer<ServerPlayer> action) {
        return SimpleEvent.light(id, tag, SimpleEvent.ALWAYS, action);
    }

    /** An event that says one of its {@code variants}, never the same one twice for a spirit; see {@link Said}. */
    private static HauntEvent saying(
            String id,
            EventTag tag,
            Function<Config.Section, List<String>> variants,
            BiConsumer<ServerPlayer, String> action) {
        return SimpleEvent.light(id, tag, player -> Said.left(player, id, variants.apply(Config.event(id))), player -> {
            String said = Said.pick(player, id, variants.apply(Config.event(id)));
            if (said != null) {
                action.accept(player, said);
            }
        });
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
        return new SimpleEvent(
                Identifier.parse(id),
                HauntTier.LIGHT,
                Set.of(EventTag.PRESENCE, EventTag.PLAYER),
                SimpleEvent.ALWAYS,
                action);
    }

    private static String pick(ServerPlayer player, List<String> texts) {
        return texts.get(player.getRandom().nextInt(texts.size()));
    }

    /**
     * A player list entry for {@code profile}. The packet can only be built from a real player, so it is read back from
     * its own wire format, written with its own codecs.
     */
    static ClientboundPlayerInfoUpdatePacket listed(ServerPlayer player, GameProfile profile, int latency) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        try {
            // Each entry's fields come in the actions' declared order.
            buffer.writeEnumSet(
                    EnumSet.of(Action.ADD_PLAYER, Action.UPDATE_GAME_MODE, Action.UPDATE_LISTED, Action.UPDATE_LATENCY),
                    Action.class);
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

    private static final int SHARED_FLAGS = 0;
    private static final int AIR_SUPPLY = 1;
    private static final int TICKS_FROZEN = 7;
    /** Entity data ids run Entity 0-7, LivingEntity 8-14, Avatar 15-16, and then the player's own. */
    private static final int ABSORPTION = 17;

    private static final byte ON_FIRE = 1;

    /** Runs {@code lie} on each of {@code ticks} ticks, then {@code truth} once. */
    private static void hold(
            ServerPlayer player, int ticks, BiConsumer<ServerPlayer, Integer> lie, Consumer<ServerPlayer> truth) {
        Scheduler.repeat(player, 0, 1, (p, n) -> {
            if (n >= ticks) {
                truth.accept(p);
                return false;
            }
            lie.accept(p, n);
            return true;
        });
    }

    private static void tell(ServerPlayer player, SynchedEntityData.DataValue<?> value) {
        player.connection.send(new ClientboundSetEntityDataPacket(player.getId(), List.of(value)));
    }

    private static void tellFlags(ServerPlayer player, byte flags) {
        tell(player, new SynchedEntityData.DataValue<>(SHARED_FLAGS, EntityDataSerializers.BYTE, flags));
    }

    private static void tellAir(ServerPlayer player, int air) {
        tell(player, new SynchedEntityData.DataValue<>(AIR_SUPPLY, EntityDataSerializers.INT, air));
    }

    private static void tellFrozen(ServerPlayer player, int ticks) {
        tell(player, new SynchedEntityData.DataValue<>(TICKS_FROZEN, EntityDataSerializers.INT, ticks));
    }

    private static void tellAbsorption(ServerPlayer player, float amount) {
        tell(player, new SynchedEntityData.DataValue<>(ABSORPTION, EntityDataSerializers.FLOAT, amount));
    }

    /** The player's shared flags (on fire, sneaking, sprinting and the rest) as they are now. */
    private static byte sharedFlags(ServerPlayer player) {
        List<SynchedEntityData.DataValue<?>> values = player.getEntityData().getNonDefaultValues();
        if (values == null) {
            return 0;
        }
        return (byte) values.stream()
                .filter(value -> value.id() == SHARED_FLAGS)
                .map(value -> (Byte) value.value())
                .findFirst()
                .orElse((byte) 0);
    }

    private static void pumpkinHead(ServerPlayer player) {
        int ticks = Ticks.SECOND * Config.event("pumpkin_head").integer("seconds");
        if (Hardcore.on(player.level())
                && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            // A real one, taken off again after.
            ItemStack pumpkin = new ItemStack(Items.CARVED_PUMPKIN);
            player.setItemSlot(EquipmentSlot.HEAD, pumpkin);
            Scheduler.later(player, ticks, p -> {
                if (p.getItemBySlot(EquipmentSlot.HEAD) == pumpkin) {
                    p.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                }
            });
            return;
        }
        InventoryMenu menu = player.inventoryMenu;
        // The first armor slot is the head.
        int slot = InventoryMenu.ARMOR_SLOT_START;
        player.connection.send(new ClientboundContainerSetSlotPacket(
                menu.containerId, menu.getStateId(), slot, new ItemStack(Items.CARVED_PUMPKIN)));
        Scheduler.later(
                player,
                ticks,
                p -> p.connection.send(new ClientboundContainerSetSlotPacket(
                        menu.containerId,
                        menu.getStateId(),
                        slot,
                        menu.getSlot(slot).getItem().copy())));
    }

    private static void resendExperience(ServerPlayer player) {
        player.connection.send(new ClientboundSetExperiencePacket(
                player.experienceProgress, player.totalExperience, player.experienceLevel));
    }

    private static void phantomFlames(ServerPlayer player) {
        Config.Section config = Config.event("phantom_flames");
        if (Hardcore.on(player.level())) {
            player.setRemainingFireTicks(Ticks.SECOND);
            return;
        }
        // The flags are told afresh each tick, so a sprint or a sneak in the meantime is not undone.
        hold(
                player,
                config.integer("ticks"),
                (p, n) -> tellFlags(p, (byte) (sharedFlags(p) | ON_FIRE)),
                p -> tellFlags(p, sharedFlags(p)));
    }

    private static void phantomDrowning(ServerPlayer player) {
        Config.Section config = Config.event("phantom_drowning");
        int max = player.getMaxAirSupply();
        int ticks = config.integer("ticks");
        double drained = config.num("drained");
        hold(
                player,
                ticks,
                (p, n) -> tellAir(p, max - (int) (max * drained * n / ticks)),
                p -> tellAir(p, p.getAirSupply()));
    }

    private static void phantomFrost(ServerPlayer player) {
        Config.Section config = Config.event("phantom_frost");
        int required = player.getTicksRequiredToFreeze();
        if (Hardcore.on(player.level())) {
            player.setTicksFrozen(required + config.integer("hardcore_extra_ticks"));
            return;
        }
        int ramp = config.integer("ramp_ticks");
        hold(
                player,
                config.integer("ticks"),
                (p, n) -> tellFrozen(p, (int) Math.min(required, (long) required * (n + 1) / ramp)),
                p -> tellFrozen(p, p.getTicksFrozen()));
    }

    private static void falseAbsorption(ServerPlayer player) {
        Config.Section config = Config.event("false_absorption");
        int ticks = config.integer("ticks");
        float amount = (float) config.num("amount");
        hold(
                player,
                ticks,
                (p, n) -> tellAbsorption(p, amount * (ticks - n) / ticks),
                p -> tellAbsorption(p, p.getAbsorptionAmount()));
    }

    private static void emptyArmorBar(ServerPlayer player) {
        AttributeInstance bare = new AttributeInstance(Attributes.ARMOR, instance -> {});
        bare.setBaseValue(0);
        player.connection.send(new ClientboundUpdateAttributesPacket(player.getId(), List.of(bare)));
        Scheduler.later(player, Ticks.SECOND * Config.event("empty_armor_bar").integer("seconds"), p -> {
            AttributeInstance real = p.getAttributes().getInstance(Attributes.ARMOR);
            if (real != null) {
                p.connection.send(new ClientboundUpdateAttributesPacket(p.getId(), List.of(real)));
            }
        });
    }

    private static void ownHandSwings(ServerPlayer player) {
        Config.Section config = Config.event("own_hand_swings");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            p.connection.send(
                    new ClientboundSwingAnimationPacket(p, InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT));
            return n + 1 < config.integer("swings");
        });
    }

    private static void nightEyes(ServerPlayer player) {
        Config.Section config = Config.event("night_eyes");
        int flash = config.integer("flash_ticks");
        int dark = config.integer("dark_ticks");
        player.connection.send(new ClientboundUpdateMobEffectPacket(
                player.getId(), new MobEffectInstance(MobEffects.NIGHT_VISION, flash), false));
        Scheduler.later(player, flash, p -> {
            restoreEffect(p, MobEffects.NIGHT_VISION);
            p.connection.send(new ClientboundUpdateMobEffectPacket(
                    p.getId(), new MobEffectInstance(MobEffects.DARKNESS, dark), false));
        });
        Scheduler.later(player, flash + dark, p -> restoreEffect(p, MobEffects.DARKNESS));
    }

    /** Tells the client what the server really has of {@code effect}: the effect itself, or nothing. */
    private static void restoreEffect(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance real = player.getEffect(effect);
        if (real == null) {
            player.connection.send(new ClientboundRemoveMobEffectPacket(player.getId(), effect));
        } else {
            player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), real, false));
        }
    }

    private static void deadSilence(ServerPlayer player) {
        hold(
                player,
                Ticks.SECOND * Config.event("dead_silence").integer("seconds"),
                (p, n) -> p.connection.send(new ClientboundStopSoundPacket(null, null)),
                p -> {});
    }

    /** What is in the hands and on the hotbar, without the empty ones. */
    private static List<ItemStack> heldItems(ServerPlayer player) {
        List<ItemStack> held = new ArrayList<>();
        for (int slot = InventoryMenu.USE_ROW_SLOT_START; slot < InventoryMenu.USE_ROW_SLOT_END; slot++) {
            held.add(player.inventoryMenu.getSlot(slot).getItem());
        }
        held.add(player.getOffhandItem());
        held.removeIf(ItemStack::isEmpty);
        return held;
    }

    private static void allOnCooldown(ServerPlayer player) {
        int ticks = Ticks.SECOND * Config.event("all_on_cooldown").integer("seconds");
        // The client counts the cooldown down by itself, so nothing is sent after.
        heldItems(player).stream()
                .map(stack -> player.getCooldowns().getCooldownGroup(stack))
                .distinct()
                .forEach(group -> player.connection.send(new ClientboundCooldownPacket(group, ticks)));
    }

    private static void moonPhase(ServerPlayer player) {
        Config.Section config = Config.event("moon_phase");
        ServerClockManager clocks = player.level().getServer().clockManager();
        Holder<WorldClock> clock = player.level().getServer().registryAccess().getOrThrow(WorldClocks.OVERWORLD);
        ServerClockManager.ServerClockInstance now = clocks.getInstance(clock);
        // Whole days on, so the time of day stays and only the phase moves.
        long shifted = now.totalTicks()
                + (long) (1 + player.getRandom().nextInt(MoonPhase.COUNT - 1)) * MoonPhase.PHASE_LENGTH;
        skyTold(
                player,
                clock,
                new ClockNetworkState(shifted, now.partialTick(), now.rate()),
                config.integer("seconds"));
    }

    private static void noonMoon(ServerPlayer player) {
        Config.Section config = Config.event("noon_moon");
        ServerClockManager clocks = player.level().getServer().clockManager();
        Holder<WorldClock> clock = player.level().getServer().registryAccess().getOrThrow(WorldClocks.OVERWORLD);
        long now = clocks.getInstance(clock).totalTicks();
        // The full moon is the first of the cycle's phases.
        long midnight = now
                - Math.floorMod(now, (long) MoonPhase.PHASE_LENGTH * MoonPhase.COUNT)
                + config.integer("time_of_day");
        skyTold(player, clock, new ClockNetworkState(midnight, 0.0F, 0.0F), config.integer("seconds"));
    }

    /** Tells only this client that the {@code clock} reads {@code state}, and the real clock again after. */
    private static void skyTold(ServerPlayer player, Holder<WorldClock> clock, ClockNetworkState state, int seconds) {
        player.connection.send(new ClientboundSetTimePacket(player.level().getGameTime(), Map.of(clock, state)));
        Scheduler.later(
                player,
                Ticks.SECOND * seconds,
                p -> p.connection.send(p.level().getServer().clockManager().createFullSyncPacket()));
    }

    private static void falseBossBar(ServerPlayer player) {
        Config.Section config = Config.event("false_boss_bar");
        ServerBossEvent bar = new ServerBossEvent(
                UUID.randomUUID(),
                Component.literal(pick(player, config.texts("names"))),
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.PROGRESS);
        bar.setProgress(0.0F);
        player.connection.send(ClientboundBossEventPacket.createAddPacket(bar));
        int fill = config.integer("fill_ticks");
        int empty = config.integer("empty_ticks");
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            if (n >= fill + empty) {
                p.connection.send(ClientboundBossEventPacket.createRemovePacket(bar.getId()));
                return false;
            }
            bar.setProgress(n < fill ? (n + 1) / (float) fill : 1.0F - (n + 1 - fill) / (float) empty);
            p.connection.send(ClientboundBossEventPacket.createUpdateProgressPacket(bar));
            return true;
        });
    }

    private static void resendHealth(ServerPlayer player) {
        player.connection.send(new ClientboundSetHealthPacket(
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel()));
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
            p.connection.send(new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, level.getRainLevel(1)));
            p.connection.send(new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, level.getThunderLevel(1)));
        });
    }
}
