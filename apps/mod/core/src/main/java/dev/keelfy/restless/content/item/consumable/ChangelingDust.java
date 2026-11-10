package dev.keelfy.restless.content.item.consumable;

import dev.keelfy.restless.config.Config;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The trickster's dust: on a block it turns the whole joined body of that block, up to {@code consumables.dust_blocks},
 * into another of its family (wool to wool, log to log, stone to stone); on a novice villager, traded with or not, it
 * deals their trades anew. Ores, spawners and anything with a block entity are outside every family.
 */
public final class ChangelingDust extends Item {
    private static final List<TagKey<Block>> FAMILIES = List.of(
            BlockTags.WOOL,
            BlockTags.WOOL_CARPETS,
            BlockTags.OVERWORLD_NATURAL_LOGS,
            BlockTags.PLANKS,
            BlockTags.TERRACOTTA,
            BlockTags.GLAZED_TERRACOTTA,
            family("stone"),
            family("concrete"),
            family("glass"));

    public ChangelingDust(Item.Properties properties) {
        super(properties);
    }

    private static TagKey<Block> family(String name) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("restless", "changeling/" + name));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Block other = otherOf(level, state);
        if (other == null) {
            return InteractionResult.PASS;
        }
        for (BlockPos at :
                body(level, pos, state.getBlock(), Config.section("consumables").integer("dust_blocks"))) {
            level.setBlock(at, other.withPropertiesOf(level.getBlockState(at)), 3);
            level.sendParticles(
                    ParticleTypes.WITCH, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 3, 0.4, 0.4, 0.4, 0.05);
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, 1.4f);
        if (context.getPlayer() != null) {
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** The blocks of {@code block} joined to {@code start} face to face, nearest first, at most {@code limit}. */
    private static List<BlockPos> body(ServerLevel level, BlockPos start, Block block, int limit) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>(Set.of(start));
        found.add(start);
        for (int i = 0; i < found.size() && found.size() < limit; i++) {
            for (Direction side : Direction.values()) {
                BlockPos next = found.get(i).relative(side);
                if (found.size() < limit
                        && seen.add(next)
                        && level.getBlockState(next).is(block)) {
                    found.add(next);
                }
            }
        }
        return found;
    }

    /**
     * A random other block of the family {@code state} belongs to; null if it belongs to none or has a block entity.
     */
    static Block otherOf(ServerLevel level, BlockState state) {
        if (state.hasBlockEntity()) {
            return null;
        }
        for (TagKey<Block> tag : FAMILIES) {
            if (!state.is(tag)) {
                continue;
            }
            List<Block> others = new ArrayList<>();
            for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
                if (holder.value() != state.getBlock()
                        && !holder.value().defaultBlockState().hasBlockEntity()) {
                    others.add(holder.value());
                }
            }
            return others.isEmpty() ? null : others.get(level.getRandom().nextInt(others.size()));
        }
        return null;
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Villager villager) || !(player.level() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }
        if (villager.isBaby()
                || villager.getVillagerData()
                        .profession()
                        .is(net.minecraft.world.entity.npc.villager.VillagerProfession.NONE)) {
            return InteractionResult.PASS;
        }
        if (villager.getVillagerData().level() > 1) {
            if (player instanceof ServerPlayer server) {
                server.sendSystemMessage(Component.translatable("message.restless.dust_traded"), true);
            }
            return InteractionResult.FAIL;
        }
        villager.setOffers(null);
        villager.getOffers();
        level.sendParticles(
                ParticleTypes.WITCH, villager.getX(), villager.getY() + 1.0, villager.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0f, 1.3f);
        stack.consume(1, player);
        return InteractionResult.SUCCESS_SERVER;
    }
}
