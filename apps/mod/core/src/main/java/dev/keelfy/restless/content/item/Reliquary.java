package dev.keelfy.restless.content.item;

import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.Notebook;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The Requiem's vessel: one slot for each type of spirit, filled by using an essence of it with the reliquary in the
 * other hand. It refuses a second essence of a type, and one of a type the journal has not named. Full, it glows.
 */
public final class Reliquary extends Item {
    /** What putting an essence in came to. */
    public enum Result {
        ADDED,
        /** The journal has not named this type: it was never photographed. */
        UNNAMED,
        DUPLICATE,
        NOT_ESSENCE
    }

    /** How often a carried reliquary looks in its holder's journal for newly named types. */
    private static final int REFRESH_TICKS = 100;

    public Reliquary(Item.Properties properties) {
        super(properties);
    }

    public static Essences of(ItemStack stack) {
        return stack.getOrDefault(Content.ESSENCES.get(), Essences.EMPTY);
    }

    /** The spirit type an essence stack is of. */
    public static Optional<String> typeOf(ItemStack stack) {
        for (Map.Entry<String, Supplier<Item>> e : Content.ESSENCE.entrySet()) {
            if (stack.is(e.getValue().get())) {
                return Optional.of(e.getKey());
            }
        }
        return Optional.empty();
    }

    /** Puts one essence of the stack into the reliquary, if the reliquary will have it; the essence is spent then. */
    public static Result insert(ServerPlayer player, ItemStack reliquary, ItemStack essence) {
        Optional<String> type = typeOf(essence);
        if (type.isEmpty()) {
            return Result.NOT_ESSENCE;
        }
        refresh(player, reliquary);
        Essences held = of(reliquary);
        if (held.held().contains(type.get())) {
            return Result.DUPLICATE;
        }
        if (!held.known().contains(type.get())) {
            return Result.UNNAMED;
        }
        reliquary.set(Content.ESSENCES.get(), held.with(type.get()));
        essence.consume(1, player);
        return Result.ADDED;
    }

    /** Writes the types the player's journal has named into the stack, so its tooltip can show where the rest are. */
    public static void refresh(ServerPlayer player, ItemStack reliquary) {
        Notebook notebook = HauntData.get(player.level().getServer()).notebook(player.getUUID());
        List<String> known =
                Content.ESSENCE.keySet().stream().filter(notebook::identified).toList();
        Essences held = of(reliquary);
        if (!known.equals(held.known())) {
            reliquary.set(Content.ESSENCES.get(), new Essences(held.held(), known));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack other = player.getItemInHand(
                hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (typeOf(other).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResult.SUCCESS;
        }
        String type = typeOf(other).orElseThrow();
        Result result = insert(server, player.getItemInHand(hand), other);
        switch (result) {
            case ADDED -> {
                level.playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.PLAYERS,
                        1.0f,
                        0.8f);
                server.sendSystemMessage(
                        Component.translatable(
                                "message.restless.reliquary_added", Component.translatable("spirit.restless." + type)),
                        true);
            }
            case UNNAMED ->
                server.sendSystemMessage(Component.translatable("message.restless.reliquary_unnamed"), true);
            case DUPLICATE ->
                server.sendSystemMessage(Component.translatable("message.restless.reliquary_duplicate"), true);
            default -> {}
        }
        return result == Result.ADDED ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (owner instanceof ServerPlayer player && level.getGameTime() % REFRESH_TICKS == 0) {
            refresh(player, stack);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return of(stack).full();
    }
}
