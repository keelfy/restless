package dev.keelfy.restless.neoforge;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.debug.DebugMenu;
import dev.keelfy.restless.debug.DebugPayload;
import dev.keelfy.restless.debug.DebugScreen;
import dev.keelfy.restless.debug.FirePayload;
import dev.keelfy.restless.haunt.DimPayload;
import dev.keelfy.restless.haunt.Dimming;
import dev.keelfy.restless.haunt.DoorLockPayload;
import dev.keelfy.restless.haunt.DoorLocks;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(Restless.MOD_ID)
public final class RestlessNeoForge {
    public RestlessNeoForge(IEventBus modBus) {
        Restless.init();
        modBus.addListener((RegisterPayloadHandlersEvent e) -> e.registrar("1")
            .playToClient(DoorLockPayload.TYPE, DoorLockPayload.CODEC,
                (payload, context) -> DoorLocks.put(context.player().level(), payload.pos(), payload.ticks()))
            .playToClient(DimPayload.TYPE, DimPayload.CODEC, (payload, context) -> Dimming.start(payload.ticks()))
            .playToClient(DebugPayload.TYPE, DebugPayload.CODEC, (payload, context) -> DebugScreen.open(payload))
            .playToServer(FirePayload.TYPE, FirePayload.CODEC, (payload, context) -> DebugMenu.fire((ServerPlayer) context.player(), payload)));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> Restless.onServerTick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> Restless.registerCommands(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> Restless.onServerStopping());
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> {
            if (Restless.onUseBlock(e.getEntity(), e.getHand(), e.getPos())) {
                e.setCanceled(true);
            }
        });
    }
}
