package dev.keelfy.restless.content;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

/**
 * The poltergeist's amulet with the hoarder's greed: in the off hand it draws near items in on its own, and used while
 * sneaking on a container it links to it, so what the holder cannot carry goes there. The link breaks with the
 * container, see {@link Advanced#hook}.
 */
public final class MagpiesHook extends TelekinesisAmulet {
    public MagpiesHook(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)
                || context.getPlayer() == null
                || !context.getPlayer().isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        return Advanced.link(level, context.getItemInHand(), context.getClickedPos())
                ? InteractionResult.SUCCESS_SERVER
                : InteractionResult.PASS;
    }
}
