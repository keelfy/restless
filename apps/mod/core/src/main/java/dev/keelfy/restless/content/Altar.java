package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Ritual;
import dev.keelfy.restless.spirit.Focus;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import org.jspecify.annotations.Nullable;

/**
 * The center of a ritual. It holds one thing: a focus, which starts a ritual on the spirit weak to it (see {@link Ritual}),
 * or a bottle of water, which turns holy after a night on it. Using it with an empty hand takes the thing back.
 */
public final class Altar extends ShapedBlock implements EntityBlock {
    /** What lies on the altar. */
    public enum Held implements StringRepresentable {
        EMPTY(null), WATER(null), IRON(Focus.IRON), GLOWSTONE(Focus.GLOWSTONE), HOLY_WATER(Focus.HOLY_WATER), MIRROR(Focus.MIRROR);

        private final @Nullable Focus focus;

        Held(@Nullable Focus focus) {
            this.focus = focus;
        }

        public static Held of(Focus focus) {
            return valueOf(focus.name());
        }

        /** What the player gets back when taking it off. */
        ItemStack stack() {
            return switch (this) {
                case EMPTY -> ItemStack.EMPTY;
                case WATER -> PotionContents.createItemStack(Items.POTION, Potions.WATER);
                case IRON -> new ItemStack(Items.IRON_INGOT);
                case GLOWSTONE -> new ItemStack(Items.GLOWSTONE_DUST);
                case HOLY_WATER -> new ItemStack(Content.HOLY_WATER.get());
                case MIRROR -> new ItemStack(Content.MIRROR.get());
            };
        }

        static @Nullable Held from(ItemStack stack) {
            PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
            if (stack.is(Items.POTION) && potion != null && potion.is(Potions.WATER)) {
                return WATER;
            }
            for (Held held : values()) {
                if (held.focus != null && stack.is(held.stack().getItem())) {
                    return held;
                }
            }
            return null;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Held> HELD = EnumProperty.create("held", Held.class);

    public Altar(BlockBehaviour.Properties properties) {
        super(properties, Shapes.or(
            Block.box(1, 0, 1, 15, 3, 15),
            Block.box(2, 3, 2, 14, 9, 14),
            Block.box(0, 9, 0, 16, 12, 16),
            Block.box(0, 12, 0, 3, 13, 3),
            Block.box(13, 12, 0, 16, 13, 3),
            Block.box(0, 12, 13, 3, 13, 16),
            Block.box(13, 12, 13, 16, 13, 16)));
        registerDefaultState(stateDefinition.any().setValue(HELD, Held.EMPTY));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AltarEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HELD);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        Held held = Held.from(stack);
        if (held == null || state.getValue(HELD) != Held.EMPTY) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer caster)) {
            return InteractionResult.SUCCESS;
        }
        if (held == Held.WATER) {
            server.setBlock(pos, state.setValue(HELD, Held.WATER), 3);
            server.scheduleTick(pos, this, Config.section("ritual").integer("bless_ticks"));
        } else if (!Ritual.begin(caster, server, pos, held.focus)) {
            return InteractionResult.FAIL;
        }
        stack.consume(1, player);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Held held = state.getValue(HELD);
        if (held == Held.EMPTY) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(HELD, Held.EMPTY), 3);
            ItemStack back = held.stack();
            if (!player.addItem(back)) {
                level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), back));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(HELD) == Held.EMPTY || random.nextInt(3) != 0) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2;
        double x = pos.getX() + 0.5 + Math.cos(angle) * 0.35;
        double z = pos.getZ() + 0.5 + Math.sin(angle) * 0.35;
        boolean blessing = state.getValue(HELD) == Held.WATER || state.getValue(HELD) == Held.HOLY_WATER;
        level.addParticle(blessing ? ParticleTypes.END_ROD : ParticleTypes.SOUL_FIRE_FLAME,
            x, pos.getY() + 0.83, z, 0, 0.015, 0);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(HELD) == Held.WATER) {
            level.setBlock(pos, state.setValue(HELD, Held.HOLY_WATER), 3);
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.2, 0.3, 0.2, 0.02);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.8f);
        }
    }
}
