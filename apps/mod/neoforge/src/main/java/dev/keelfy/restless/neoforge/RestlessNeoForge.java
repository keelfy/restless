package dev.keelfy.restless.neoforge;

import dev.keelfy.restless.Restless;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(Restless.MOD_ID)
public final class RestlessNeoForge {
    public RestlessNeoForge() {
        Restless.init();
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> Restless.onServerTick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> Restless.registerCommands(e.getDispatcher()));
    }
}
