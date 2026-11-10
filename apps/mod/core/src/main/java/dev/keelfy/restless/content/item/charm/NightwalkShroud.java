package dev.keelfy.restless.content.item.charm;

import dev.keelfy.restless.platform.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The wraith's shroud with the shade's dark: a toggle, or simply worn in an accessory slot. While it is on and the
 * light is low, hostile things lose track of the holder, at a little sanity a second; light ends it, see
 * {@link Advanced#walk}.
 */
public final class NightwalkShroud extends Item {
    public NightwalkShroud(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (Platform.get().hasAccessories()) {
            return InteractionResult.PASS; // worn in its slot it is on, nothing to toggle
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResult.PASS;
        }
        boolean on = Advanced.toggleWalk(server);
        server.sendSystemMessage(
                Component.translatable(on ? "message.restless.walk_on" : "message.restless.walk_off"), true);
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.PLAYERS,
                0.8f,
                on ? 0.5f : 0.9f);
        return InteractionResult.SUCCESS_SERVER;
    }
}
