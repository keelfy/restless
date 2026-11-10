package dev.keelfy.restless.content.item.consumable;

import com.mojang.serialization.Codec;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** The block a lump of mimic clay has been pressed against, and so what it shows as when it is set down. */
public record Look(BlockState state) implements TooltipProvider {
    public static final Codec<Look> CODEC = BlockState.CODEC.xmap(Look::new, Look::state);
    public static final StreamCodec<RegistryFriendlyByteBuf, Look> STREAM_CODEC = ByteBufCodecs.idMapper(
                    Block.BLOCK_STATE_REGISTRY)
            .map(Look::new, Look::state)
            .cast();

    @Override
    public void addToTooltip(
            Item.TooltipContext context,
            Consumer<Component> consumer,
            TooltipFlag flag,
            DataComponentGetter components) {
        consumer.accept(Component.translatable(
                        "item.restless.mimic_clay.shows", state.getBlock().getName())
                .withStyle(ChatFormatting.GRAY));
    }
}
