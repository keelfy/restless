package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Scan;
import dev.keelfy.restless.haunt.Wards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The domovoi's porridge: a bowl set down in a shut, roofed room. The house spirit eats it over a day, and while it
 * does, the crops and saplings around grow faster and the young animals grow up sooner and breed again sooner. Only one
 * bowl serves a room. The bowl is eaten in {@link #PORTIONS} bites, each a slice of the day the config sets.
 */
public final class DomovoiPorridge extends Block {
    public static final int PORTIONS = 8;
    /** How many bites are left, from {@link #PORTIONS} down to one. */
    public static final IntegerProperty LEFT = IntegerProperty.create("left", 1, PORTIONS);

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 5, 13);
    /** How often the bowl works on its surroundings. */
    private static final int INTERVAL = 40;

    public DomovoiPorridge(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEFT, PORTIONS));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEFT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (level instanceof ServerLevel server && !old.is(this)) {
            server.scheduleTick(pos, this, INTERVAL);
        }
    }

    /** The bowl in hand: set down in a shut, roofed room that has no other bowl, else it stays in hand. */
    public static final class Bowl extends net.minecraft.world.item.BlockItem {
        public Bowl(Block block, Item.Properties properties) {
            super(block, properties);
        }

        @Override
        public InteractionResult place(BlockPlaceContext context) {
            if (!(context.getLevel() instanceof ServerLevel level)) {
                return super.place(context);
            }
            BlockPos at = context.getClickedPos();
            if (!context.canPlace()) {
                return InteractionResult.FAIL;
            }
            Player player = context.getPlayer();
            if (!Wards.housed(level, at)) {
                refuse(player, "message.restless.porridge_open");
                return InteractionResult.FAIL;
            }
            if (served(level, at)) {
                refuse(player, "message.restless.porridge_served");
                return InteractionResult.FAIL;
            }
            return super.place(context);
        }

        private static void refuse(@Nullable Player player, String key) {
            if (player instanceof ServerPlayer server) {
                server.sendSystemMessage(Component.translatable(key), true);
            }
        }
    }

    /** Whether another bowl already serves the room around {@code at}. */
    private static boolean served(ServerLevel level, BlockPos at) {
        int radius = Config.section("consumables").integer("porridge_radius");
        boolean[] found = {false};
        Scan.each(level, at, radius, (pos, state) -> found[0] |= state.is(Content.DOMOVOIS_PORRIDGE.get()));
        return found[0];
    }

    /** A pass of the bowl: the surroundings grow, a bite is eaten when its slice of the day has gone by. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Config.Section config = Config.section("consumables");
        int radius = config.integer("porridge_radius");
        double extra = config.num("porridge_boost") - 1.0;
        double speed = level.getGameRules().get(GameRules.RANDOM_TICK_SPEED);
        double chance = extra * INTERVAL * speed / 4096.0;
        Scan.each(level, pos, radius, (at, plant) -> {
            if (grows(plant) && random.nextDouble() < chance) {
                plant.randomTick(level, at, random);
            }
        });
        int boost = (int) (extra * INTERVAL);
        for (AgeableMob young : level.getEntitiesOfClass(
                AgeableMob.class, new net.minecraft.world.phys.AABB(pos).inflate(radius), m -> m.getAge() != 0)) {
            young.setAge(
                    young.getAge() < 0 ? Math.min(0, young.getAge() + boost) : Math.max(0, young.getAge() - INTERVAL));
        }
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                2,
                0.2,
                0.1,
                0.2,
                0.0);
        long slice = Math.max(1, config.integer("porridge_ticks") / PORTIONS);
        BlockState next = state;
        if (level.getGameTime() % slice < INTERVAL) {
            if (state.getValue(LEFT) <= 1) {
                level.removeBlock(pos, false);
                level.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.BLOCKS, 0.6f, 0.6f);
                return;
            }
            next = state.setValue(LEFT, state.getValue(LEFT) - 1);
            level.setBlock(pos, next, 3);
            level.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.BLOCKS, 0.3f, 0.5f);
        }
        level.scheduleTick(pos, this, INTERVAL);
    }

    /** What gets the extra growth: crops, saplings and a few plants besides. */
    private static boolean grows(BlockState state) {
        return state.isRandomlyTicking()
                && (state.is(BlockTags.CROPS)
                        || state.is(BlockTags.SAPLINGS)
                        || state.is(Blocks.SWEET_BERRY_BUSH)
                        || state.is(Blocks.NETHER_WART)
                        || state.is(Blocks.COCOA));
    }

    /** What the anvil makes of a worn house key and a bowl: the key mended by the config's number of uses. */
    public static ItemStack mend(ItemStack key, ItemStack porridge) {
        if (!key.is(Content.HOUSE_KEY.get())
                || !porridge.is(Content.DOMOVOIS_PORRIDGE_ITEM.get())
                || !key.isDamaged()) {
            return ItemStack.EMPTY;
        }
        ItemStack mended = key.copy();
        mended.setDamageValue(
                Math.max(0, key.getDamageValue() - Config.section("consumables").integer("porridge_repair")));
        return mended;
    }
}
