package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Altar;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.spirit.Focus;
import dev.keelfy.restless.spirit.Story;
import dev.keelfy.restless.spirit.Traits;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Banishing. An altar inside a closed salt loop with lit candles around it takes a focus: the spirit near the player
 * that is weak to it is called in and fights for the length of the ritual, acting on the player and snuffing candles.
 * Every candle lit and the focus in place at the end banishes it, and it leaves its essence. A focus no spirit is weak
 * to enrages one of them instead. Rituals live in memory only, like every {@link Scheduler} task.
 */
public final class Ritual {
    private Ritual() {}

    /** @return whether the altar took the focus; false leaves everything as it was */
    public static boolean begin(ServerPlayer player, ServerLevel level, BlockPos altar, Focus focus) {
        Config.Section config = Config.section("ritual");
        List<BlockPos> candles = new ArrayList<>();
        Scan.each(level, altar, config.integer("candle_radius"), (pos, state) -> {
            if (AbstractCandleBlock.isLit(state)) {
                candles.add(pos.immutable());
            }
        });
        if (!Altar.isComplete(level, altar) || candles.size() < config.integer("candles") || !Wards.ringed(level, altar)) {
            player.sendSystemMessage(Component.translatable("message.restless.altar_incomplete", config.integer("candles")), true);
            return false;
        }
        List<Sense.Spirit> spirits = Sense.of(player, HauntData.get(level.getServer()));
        if (spirits.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.restless.ritual_silent"), true);
            return false;
        }
        level.setBlock(altar, level.getBlockState(altar).setValue(Altar.HELD, Altar.Held.of(focus)), 3);
        Optional<Sense.Spirit> weak = spirits.stream().filter(s -> Traits.weakness(Traits.of(s.haunting().traits())) == focus).findFirst();
        if (weak.isEmpty()) {
            wrong(player, level, altar, spirits.get(player.getRandom().nextInt(spirits.size())));
            return true;
        }
        Sense.Spirit spirit = weak.get();
        player.sendSystemMessage(Component.translatable("message.restless.ritual_begins"), true);
        level.playSound(null, altar, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 0.6f);
        int waveTicks = Math.max(1, config.integer("wave_ticks"));
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            int elapsed = n + 1;
            if (elapsed % waveTicks == 0 && !wave(p, level, altar, focus, candles, spirit, elapsed / waveTicks - 1)) {
                return false;
            }
            if (elapsed % 2 == 0) {
                smoke(level, altar, focus);
            }
            return true;
        });
        return true;
    }

    /** Dense vanilla soot follows the offering's bob; its task ends with the ritual, including player departure. */
    private static void smoke(ServerLevel level, BlockPos altar, Focus focus) {
        BlockState state = level.getBlockState(altar);
        if (!state.is(Content.ALTAR.get()) || state.getValue(Altar.HELD) != Altar.Held.of(focus)
            || !Altar.isComplete(level, altar)) {
            return;
        }
        double height = 1.12 + Math.sin((level.getGameTime() % 24000) * 0.06) * 0.035;
        level.sendParticles(ParticleTypes.LARGE_SMOKE, altar.getX() + 0.5, altar.getY() + height, altar.getZ() + 0.5,
            4, 0.22, 0.10, 0.22, 0.008);
    }

    /** One wave: the ritual holds or breaks, ends after the last, and otherwise the spirit fights back. @return whether it goes on */
    private static boolean wave(ServerPlayer player, ServerLevel level, BlockPos altar, Focus focus, List<BlockPos> candles, Sense.Spirit spirit, int n) {
        Config.Section config = Config.section("ritual");
        BlockState state = level.getBlockState(altar);
        boolean held = state.is(Content.ALTAR.get()) && state.getValue(Altar.HELD) == Altar.Held.of(focus)
            && Altar.isComplete(level, altar);
        boolean lit = candles.stream().allMatch(pos -> AbstractCandleBlock.isLit(level.getBlockState(pos)));
        boolean near = player.level() == level && player.blockPosition().closerThan(altar, config.integer("radius"));
        if (!held || !lit || !near) {
            fail(player, level, altar, spirit);
            return false;
        }
        if (n + 1 >= config.integer("waves") + (spirit.haunting().evolved() ? Config.section("evolution").integer("extra_waves") : 0)) {
            banish(player, level, altar, spirit);
            return false;
        }
        level.sendParticles(ParticleTypes.SOUL, altar.getX() + 0.5, altar.getY() + 1.2, altar.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.02);
        if (player.getRandom().nextDouble() < config.num("snuff_chance")) {
            BlockPos candle = candles.get(player.getRandom().nextInt(candles.size()));
            AbstractCandleBlock.extinguish(null, level.getBlockState(candle), level, candle); // all lit: checked above
            player.sendSystemMessage(Component.translatable("message.restless.candle_snuffed"), true);
        }
        if (player.getRandom().nextDouble() < config.num("fury_chance")) {
            Director.act(player, spirit.haunting(), spirit.at(), null);
        }
        return true;
    }

    private static void banish(ServerPlayer player, ServerLevel level, BlockPos altar, Sense.Spirit spirit) {
        clear(level, altar);
        HauntData.get(level.getServer()).change(spirit.haunting().uuid(), h -> null);
        Location.forget(spirit.haunting().uuid());
        Identifier essence = Identifier.fromNamespaceAndPath(Restless.MOD_ID, spirit.haunting().spirit() + "_essence");
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(essence);
        item.ifPresent(i -> level.addFreshEntity(new ItemEntity(level, altar.getX() + 0.5, altar.getY() + 1.0, altar.getZ() + 0.5, new ItemStack(i))));
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, altar.getX() + 0.5, altar.getY() + 1.0, altar.getZ() + 0.5, 40, 0.5, 1.0, 0.5, 0.05);
        level.playSound(null, altar, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 0.6f);
        Component type = Evolution.name(spirit.haunting());
        player.sendSystemMessage(Component.translatable("message.restless.banished", type), true);
        Journal.note(player, Notebook.Kind.BANISHED, spirit.haunting(), type, Story.of(spirit.haunting().seed()).name());
    }

    private static void fail(ServerPlayer player, ServerLevel level, BlockPos altar, Sense.Spirit spirit) {
        clear(level, altar);
        HauntData.get(level.getServer()).change(spirit.haunting().uuid(), h -> h.angered(Config.section("ritual").num("fail_anger")));
        level.playSound(null, altar, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.0f, 0.5f);
        player.sendSystemMessage(Component.translatable("message.restless.ritual_failed"), true);
    }

    /** The wrong focus: the spirit eats it, its anger peaks and it strikes at once. */
    private static void wrong(ServerPlayer player, ServerLevel level, BlockPos altar, Sense.Spirit spirit) {
        clear(level, altar);
        HauntData.Haunting angered = spirit.haunting().angered(1.0);
        HauntData.get(level.getServer()).change(angered.uuid(), h -> h.angered(1.0));
        level.playSound(null, altar, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6f, 1.4f);
        player.sendSystemMessage(Component.translatable("message.restless.wrong_focus"), true);
        Director.act(player, angered, spirit.at(), HauntTier.HARMFUL);
    }

    private static void clear(ServerLevel level, BlockPos altar) {
        BlockState state = level.getBlockState(altar);
        if (state.is(Content.ALTAR.get())) {
            level.setBlock(altar, state.setValue(Altar.HELD, Altar.Held.EMPTY), 3);
        }
    }
}
