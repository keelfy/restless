package dev.keelfy.restless.content;

import dev.keelfy.restless.haunt.Journal;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Opens the holder's {@link Journal}. */
public final class JournalBook extends Item {
    public JournalBook(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer reader) {
            Journal.open(reader, player.getItemInHand(hand), hand);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.SUCCESS;
    }
}
