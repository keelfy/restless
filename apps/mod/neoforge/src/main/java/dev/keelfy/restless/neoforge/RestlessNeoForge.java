package dev.keelfy.restless.neoforge;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.banish.RequiemPayload;
import dev.keelfy.restless.banish.client.RequiemScreen;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.EyesType;
import dev.keelfy.restless.content.Presence;
import dev.keelfy.restless.debug.DebugMenu;
import dev.keelfy.restless.debug.DebugPayload;
import dev.keelfy.restless.debug.DebugScreen;
import dev.keelfy.restless.debug.FirePayload;
import dev.keelfy.restless.divine.Divination;
import dev.keelfy.restless.divine.SpiritBoxPayload;
import dev.keelfy.restless.divine.client.SpiritBoxVoice;
import dev.keelfy.restless.haunt.effect.DimPayload;
import dev.keelfy.restless.haunt.effect.DoorLockPayload;
import dev.keelfy.restless.haunt.effect.DoorLocks;
import dev.keelfy.restless.haunt.effect.ScreenPayload;
import dev.keelfy.restless.haunt.effect.TurnPayload;
import dev.keelfy.restless.haunt.effect.VignettePayload;
import dev.keelfy.restless.haunt.effect.client.Dimming;
import dev.keelfy.restless.haunt.effect.client.HeadTurn;
import dev.keelfy.restless.haunt.effect.client.RedVignette;
import dev.keelfy.restless.haunt.effect.client.ScreenOverlay;
import dev.keelfy.restless.journal.FilePayload;
import dev.keelfy.restless.journal.Journal;
import dev.keelfy.restless.journal.JournalPayload;
import dev.keelfy.restless.journal.ReadPayload;
import dev.keelfy.restless.journal.client.JournalScreen;
import dev.keelfy.restless.neoforge.compat.VoiceAddon;
import dev.keelfy.restless.photo.DevelopPayload;
import dev.keelfy.restless.photo.ShutterPayload;
import dev.keelfy.restless.photo.client.Darkroom;
import dev.keelfy.restless.sanity.SanityPayload;
import dev.keelfy.restless.sanity.client.SanityOverlay;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(Restless.MOD_ID)
public final class RestlessNeoForge {
    public RestlessNeoForge(IEventBus modBus, ModContainer container) {
        RestlessSettings.register(modBus, container);
        Restless.init();
        EyesType.init();
        Presence.init();
        if (ModList.get().isLoaded("plasmovoice")) {
            VoiceAddon.register();
        }
        modBus.addListener((RegisterPayloadHandlersEvent e) -> e.registrar("1")
                .playToClient(
                        DoorLockPayload.TYPE,
                        DoorLockPayload.CODEC,
                        (payload, context) -> DoorLocks.put(
                                context.player().level(),
                                payload.pos(),
                                payload.ticks(),
                                new Config.Tone(payload.volume(), payload.pitch())))
                .playToClient(DimPayload.TYPE, DimPayload.CODEC, (payload, context) -> Dimming.start(payload))
                .playToClient(
                        ScreenPayload.TYPE, ScreenPayload.CODEC, (payload, context) -> ScreenOverlay.start(payload))
                .playToClient(
                        TurnPayload.TYPE,
                        TurnPayload.CODEC,
                        (payload, context) -> HeadTurn.start(payload.degrees(), payload.pitch(), payload.ticks()))
                .playToClient(
                        VignettePayload.TYPE,
                        VignettePayload.CODEC,
                        (payload, context) -> RedVignette.start(payload.strength(), payload.ticks()))
                .playToClient(
                        SanityPayload.TYPE,
                        SanityPayload.CODEC,
                        (payload, context) -> SanityOverlay.set(payload.sanity()))
                .playToClient(
                        JournalPayload.TYPE, JournalPayload.CODEC, (payload, context) -> JournalScreen.receive(payload))
                .playToClient(RequiemPayload.TYPE, RequiemPayload.CODEC, (payload, context) -> RequiemScreen.open())
                .playToClient(DebugPayload.TYPE, DebugPayload.CODEC, (payload, context) -> DebugScreen.open(payload))
                .playToClient(
                        ShutterPayload.TYPE, ShutterPayload.CODEC, (payload, context) -> Darkroom.shutter(payload))
                .playToClient(
                        SpiritBoxPayload.TYPE,
                        SpiritBoxPayload.CODEC,
                        (payload, context) -> SpiritBoxVoice.say(payload))
                .playToServer(
                        DevelopPayload.TYPE,
                        DevelopPayload.CODEC,
                        (payload, context) -> Divination.develop((ServerPlayer) context.player(), payload))
                .playToServer(
                        ReadPayload.TYPE,
                        ReadPayload.CODEC,
                        (payload, context) -> Journal.sync((ServerPlayer) context.player()))
                .playToServer(
                        FilePayload.TYPE,
                        FilePayload.CODEC,
                        (payload, context) -> Journal.file((ServerPlayer) context.player(), payload))
                .playToServer(
                        FirePayload.TYPE,
                        FirePayload.CODEC,
                        (payload, context) -> DebugMenu.fire((ServerPlayer) context.player(), payload)));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> Restless.onServerTick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Load e) -> {
            if (e.isNewChunk()
                    && e.getLevel() instanceof ServerLevel level
                    && e.getChunk() instanceof LevelChunk chunk) {
                Restless.onNewChunk(level, chunk);
            }
        });
        NeoForge.EVENT_BUS.addListener((FinalizeSpawnEvent e) -> {
            if (e.getLevel().getLevel() instanceof ServerLevel level
                    && Restless.onSpawn(level, e.getEntity(), e.getSpawnType())) {
                e.setSpawnCancelled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((AnvilUpdateEvent e) -> {
            ItemStack mended = Restless.onAnvil(e.getLeft(), e.getRight());
            if (!mended.isEmpty()) {
                e.setOutput(mended);
                e.setMaterialCost(1);
                e.setXpCost(1);
            }
        });
        NeoForge.EVENT_BUS.addListener((CanPlayerSleepEvent e) -> {
            if (e.getProblem() != null && Restless.onSleepProblem(e.getEntity())) {
                e.setProblem(null);
            }
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> Restless.registerCommands(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> Restless.onServerStopping(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayLevelSoundEvent.AtPosition e) -> {
            if (Restless.onPlaySound(
                    e.getLevel(), e.getPosition(), e.getSound(), e.getSource(), e.getNewVolume(), e.getNewPitch())) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayLevelSoundEvent.AtEntity e) -> {
            if (Restless.onPlaySound(
                    e.getLevel(),
                    e.getEntity().position(),
                    e.getSound(),
                    e.getSource(),
                    e.getNewVolume(),
                    e.getNewPitch())) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((LivingIncomingDamageEvent e) -> {
            if (Restless.onStrike(e.getEntity(), e.getSource())) {
                e.setCanceled(true);
                return;
            }
            e.setAmount(Restless.onHurt(e.getEntity(), e.getSource(), e.getAmount()));
        });
        NeoForge.EVENT_BUS.addListener((AttackEntityEvent e) -> {
            if (Restless.onMeet(e.getEntity(), e.getTarget())) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ProjectileImpactEvent e) -> {
            if (e.getRayTraceResult() instanceof EntityHitResult hit
                    && Restless.onMeet(e.getProjectile().getOwner(), hit.getEntity())) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent e) -> {
            if (Restless.onDeath(e.getEntity(), e.getSource())) {
                e.setCanceled(true);
                return;
            }
            Restless.onKill(e.getSource().getEntity(), e.getEntity());
        });
        NeoForge.EVENT_BUS.addListener((PlayerWakeUpEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                Restless.onWake(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((SleepFinishedTimeEvent e) -> {
            if (e.getLevel() instanceof ServerLevel level && Restless.onSleepFinished(level)) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerRespawnPositionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                var replaced = Restless.onRespawnPosition(player, e.getTeleportTransition());
                if (replaced != null) {
                    e.setTeleportTransition(replaced);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(
                (LivingDropsEvent e) -> Restless.onDrops(e.getSource().getEntity(), e.getEntity(), e.getDrops()));
        NeoForge.EVENT_BUS.addListener((LivingChangeTargetEvent e) -> {
            if (Restless.onTarget(e.getEntity(), e.getNewAboutToBeSetTarget())) {
                e.setNewAboutToBeSetTarget(null);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                Restless.onRespawn(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((BreakBlockEvent e) -> {
            if (e.getPlayer() instanceof ServerPlayer player) {
                Restless.onBreakBlock(player, e.getState(), e.getPos());
            }
        });
        NeoForge.EVENT_BUS.addListener(
                (LivingEntityUseItemEvent.Finish e) -> Restless.onEat(e.getEntity(), e.getItem()));
        NeoForge.EVENT_BUS.addListener((ServerChatEvent e) -> Restless.onChat(e.getPlayer(), e.getRawText()));
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock e) -> {
            if (e.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START
                    && e.getEntity() instanceof ServerPlayer player) {
                Restless.onStartBreak(player, e.getPos());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> {
            if (Restless.onUseBlock(e.getEntity(), e.getHand(), e.getPos())) {
                e.setCanceled(true);
            }
        });
    }
}
