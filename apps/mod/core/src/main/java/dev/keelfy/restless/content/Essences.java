package dev.keelfy.restless.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
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

/**
 * What a {@link Reliquary} holds: the spirit types it has an essence of, in the order they went in, and the types its
 * holder's journal has named (the tooltip is built on the client, which has no journal).
 */
public record Essences(List<String> held, List<String> known) implements TooltipProvider {
    public static final Essences EMPTY = new Essences(List.of(), List.of());
    public static final Codec<Essences> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.STRING.listOf().fieldOf("held").forGetter(Essences::held),
                    Codec.STRING.listOf().optionalFieldOf("known", List.of()).forGetter(Essences::known))
            .apply(i, Essences::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Essences> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            Essences::held,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            Essences::known,
            Essences::new);

    public boolean full() {
        return held.size() >= Content.ESSENCE.size();
    }

    public Essences with(String type) {
        List<String> grown = new ArrayList<>(held);
        grown.add(type);
        return new Essences(List.copyOf(grown), known);
    }

    @Override
    public void addToTooltip(
            Item.TooltipContext context,
            Consumer<Component> consumer,
            TooltipFlag flag,
            DataComponentGetter components) {
        consumer.accept(Component.translatable("item.restless.reliquary.held", held.size(), Content.ESSENCE.size())
                .withStyle(ChatFormatting.GRAY));
        for (String type : Content.ESSENCE.keySet()) {
            Component name = Component.translatable("spirit.restless." + type);
            if (held.contains(type)) {
                consumer.accept(Component.empty().append("+ ").append(name).withStyle(ChatFormatting.AQUA));
            } else if (known.contains(type)) {
                consumer.accept(Component.empty().append("- ").append(name).withStyle(ChatFormatting.DARK_GRAY));
                consumer.accept(Component.translatable("journal.restless.met." + type)
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            } else {
                consumer.accept(Component.literal("- ???").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
