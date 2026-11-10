package dev.keelfy.restless.content.item.consumable;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.entity.SightingMark;
import dev.keelfy.restless.haunt.Spells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** The phantom's mark, set on a block: a {@link SightingMark} waypoint stands there. A renamed mark names it. */
public final class SightingMarkItem extends Item {
    public SightingMarkItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)
                || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        Spells spells = Spells.get(level.getServer());
        if (spells.marks(player.getUUID()) >= Config.section("consumables").integer("mark_max")) {
            player.sendSystemMessage(Component.translatable("message.restless.mark_many"), true);
            return InteractionResult.FAIL;
        }
        BlockPos at = context.getClickedPos().relative(context.getClickedFace());
        @SuppressWarnings("unchecked") // registered as this class
        SightingMark mark = new SightingMark((EntityType<? extends SightingMark>) Content.MARK_ENTITY.get(), level);
        mark.setPos(at.getX() + 0.5, at.getY() + 0.3, at.getZ() + 0.5);
        mark.setUp(player.getUUID());
        ItemStack stack = context.getItemInHand();
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            mark.setCustomName(stack.getHoverName());
        }
        if (!level.addFreshEntity(mark)) {
            return InteractionResult.FAIL;
        }
        spells.mark(player.getUUID(), mark.getUUID());
        level.playSound(null, at, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.6f);
        stack.consume(1, player);
        return InteractionResult.SUCCESS_SERVER;
    }
}
