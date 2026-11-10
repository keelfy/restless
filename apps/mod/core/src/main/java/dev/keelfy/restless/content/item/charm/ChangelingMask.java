package dev.keelfy.restless.content.item.charm;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The mimic's face with the trickster's trick: the first blow struck while it is on does not break it. The struck mob
 * is framed instead, and the nearest hostile turns on it, see {@link Advanced#framed}. The second blow breaks the face.
 */
public final class ChangelingMask extends BorrowedFace {
    public ChangelingMask(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        InteractionResult result = super.use(level, player, hand);
        if (player instanceof ServerPlayer server) {
            Advanced.unframe(server);
        }
        return result;
    }
}
