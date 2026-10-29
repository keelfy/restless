package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Ritual;
import dev.keelfy.restless.spirit.Focus;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The center of a ritual. It holds one thing: a focus, which starts a ritual on the spirit weak to it (see {@link Ritual}),
 * or a bottle of water, which turns holy after a night on it. Using it with an empty hand takes the thing back.
 */
public final class Altar extends Block implements EntityBlock {
    /** Offsets from the bowl; only the center owns an offering or a block entity. */
    public enum Part implements StringRepresentable {
        CENTER(0, 0), NORTH(0, -1), EAST(1, 0), SOUTH(0, 1), WEST(-1, 0),
        NORTH_WEST(-1, -1), NORTH_EAST(1, -1), SOUTH_EAST(1, 1), SOUTH_WEST(-1, 1);

        final int x;
        final int z;

        Part(int x, int z) {
            this.x = x;
            this.z = z;
        }

        BlockPos position(BlockPos center) {
            return center.offset(x, 0, z);
        }

        BlockPos center(BlockPos pos) {
            return pos.offset(-x, 0, -z);
        }

        boolean corner() {
            return x != 0 && z != 0;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

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
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    private static final VoxelShape BASE = Block.box(0, 0, 0, 16, 3, 16);
    private static final VoxelShape BOWL = Shapes.or(BASE,
        Block.box(3, 3, 3, 13, 5, 13), Block.box(5, 5, 5, 11, 9, 11),
        Block.box(2, 9, 2, 14, 11, 14),
        Block.box(1, 11, 1, 15, 14, 3), Block.box(1, 11, 13, 15, 14, 15),
        Block.box(1, 11, 3, 3, 14, 13), Block.box(13, 11, 3, 15, 14, 13));
    private static final VoxelShape PILLAR = Shapes.or(BASE,
        Block.box(3, 3, 3, 13, 5, 13), Block.box(5, 5, 5, 11, 11, 11),
        Block.box(3, 11, 3, 13, 13, 13));

    public Altar(BlockBehaviour.Properties properties) {
        super(properties.pushReaction(PushReaction.IMMOVEABLE));
        registerDefaultState(stateDefinition.any().setValue(HELD, Held.EMPTY).setValue(PART, Part.CENTER));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == Part.CENTER ? new AltarEntity(pos, state) : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HELD, PART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Part part = state.getValue(PART);
        return part == Part.CENTER ? BOWL : part.corner() ? PILLAR : BASE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        for (Part part : Part.values()) {
            BlockPos at = part.position(context.getClickedPos());
            BlockState state = defaultBlockState().setValue(PART, part);
            if (!level.getWorldBorder().isWithinBounds(at) || !level.hasChunkAt(at)
                || !level.getBlockState(at).canBeReplaced(context) || !level.getFluidState(at).isEmpty()
                || !state.canSurvive(level, at)
                || !level.isUnobstructed(state, at, CollisionContext.placementContext(context.getPlayer()))
                || (context.getPlayer() != null && !context.getPlayer().mayUseItemAt(at, Direction.UP, context.getItemInHand()))) {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    player.sendSystemMessage(Component.translatable("message.restless.altar_space"), true);
                }
                return null;
            }
        }
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        for (Part part : Part.values()) {
            if (part != Part.CENTER) {
                level.setBlock(part.position(pos), defaultBlockState().setValue(PART, part), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    /** Removal starts at the center, so recursive neighbour callbacks cannot drop or dismantle it twice. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Part removed = state.getValue(PART);
        BlockPos center = removed.center(pos);
        if (removed != Part.CENTER) {
            if (isPart(level.getBlockState(center), Part.CENTER)) {
                level.destroyBlock(center, true);
            }
            return;
        }
        popResource(level, pos, state.getValue(HELD).stack());
        for (Part part : Part.values()) {
            BlockPos at = part.position(center);
            if (part != Part.CENTER && isPart(level.getBlockState(at), part)) {
                level.removeBlock(at, false);
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops() && state.getValue(PART) != Part.CENTER) {
            BlockPos center = state.getValue(PART).center(pos);
            if (isPart(level.getBlockState(center), Part.CENTER)) {
                level.destroyBlock(center, false);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private boolean isPart(BlockState state, Part part) {
        return state.is(this) && state.getValue(PART) == part;
    }

    public static boolean isComplete(BlockGetter level, BlockPos center) {
        for (Part part : Part.values()) {
            BlockState state = level.getBlockState(part.position(center));
            if (!(state.getBlock() instanceof Altar) || state.getValue(PART) != part) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        pos = state.getValue(PART).center(pos);
        state = level.getBlockState(pos);
        if (!isPart(state, Part.CENTER)) {
            return InteractionResult.FAIL;
        }
        Held held = Held.from(stack);
        if (held == null || state.getValue(HELD) != Held.EMPTY) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer caster)) {
            return InteractionResult.SUCCESS;
        }
        if (!isComplete(level, pos)) {
            caster.sendSystemMessage(Component.translatable("message.restless.altar_rebuild"), true);
            return InteractionResult.FAIL;
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
        pos = state.getValue(PART).center(pos);
        state = level.getBlockState(pos);
        if (!isPart(state, Part.CENTER)) {
            return InteractionResult.PASS;
        }
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
        Part part = state.getValue(PART);
        if (part != Part.CENTER && !part.corner()) {
            return;
        }
        BlockState center = level.getBlockState(part.center(pos));
        if (!isPart(center, Part.CENTER) || center.getValue(HELD) == Held.EMPTY || random.nextInt(5) != 0) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = part == Part.CENTER ? 0.3 : 0.1;
        double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
        double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
        boolean blessing = center.getValue(HELD) == Held.WATER || center.getValue(HELD) == Held.HOLY_WATER;
        level.addParticle(blessing ? ParticleTypes.END_ROD : ParticleTypes.SOUL_FIRE_FLAME,
            x, pos.getY() + 0.9, z, 0, 0.015, 0);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) == Part.CENTER && state.getValue(HELD) == Held.WATER) {
            level.setBlock(pos, state.setValue(HELD, Held.HOLY_WATER), 3);
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.2, 0.3, 0.2, 0.02);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.8f);
        }
    }
}
