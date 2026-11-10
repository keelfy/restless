package dev.keelfy.restless.content.item.charm;

import dev.keelfy.restless.ward.Wards;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * The mara's dream with the house spirit's peace: sanity to full without anger, but only inside a shut, roofed room.
 */
public final class HearthDream extends BorrowedDream {
    public HearthDream(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean allowed(ServerPlayer player) {
        if (Wards.housed(player.level(), player.blockPosition())) {
            return true;
        }
        player.sendSystemMessage(Component.translatable("message.restless.hearth_open"), true);
        return false;
    }

    @Override
    protected void pay(ServerPlayer player) {}
}
