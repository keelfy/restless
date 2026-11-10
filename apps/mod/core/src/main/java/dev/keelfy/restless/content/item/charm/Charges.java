package dev.keelfy.restless.content.item.charm;

import com.mojang.serialization.Codec;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

/** Hits a weapon coated in wraith oil still has; the tooltip says how many. */
public record Charges(int left) implements TooltipProvider {
    public static final Codec<Charges> CODEC = Codec.INT.xmap(Charges::new, Charges::left);
    public static final StreamCodec<RegistryFriendlyByteBuf, Charges> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(Charges::new, Charges::left).cast();

    @Override
    public void addToTooltip(
            Item.TooltipContext context,
            Consumer<Component> consumer,
            TooltipFlag flag,
            DataComponentGetter components) {
        consumer.accept(Component.translatable("item.restless.wraith_oil.coated", left)
                .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
    }
}
