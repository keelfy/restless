package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.BirthRules;
import dev.keelfy.restless.haunt.Births;
import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.Spirits;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A scarecrow stuffed with one essence and lit with fire. It burns for {@code effigy.burn_ticks} and then calls a
 * spirit of the essence's type, through {@link Births} so its cooldown, cap and spacing hold. A personal spirit is born
 * on the nearest player within {@code effigy.radius}; a territorial one is anchored where the effigy stood. The burn is
 * a scheduled block tick, so it goes on across a restart. Fire is refused when the call could not be answered.
 */
public final class Effigy extends ShapedBlock {
    /** What the effigy holds; one value per spirit type that leaves an essence. */
    public enum Stuffing implements StringRepresentable {
        EMPTY,
        POLTERGEIST,
        SHADE,
        NIX,
        PHANTOM,
        MIMIC,
        WRAITH,
        MARA,
        BANSHEE,
        REVENANT,
        LOST_ONE,
        DOMOVOI,
        HOARDER,
        DEMON,
        TRICKSTER,
        BOGGART;

        /** The spirit type's id; empty for {@link #EMPTY}. */
        public String spirit() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String getSerializedName() {
            return spirit();
        }

        static Optional<Stuffing> of(String spirit) {
            return Arrays.stream(values())
                    .filter(s -> s != EMPTY && s.spirit().equals(spirit))
                    .findFirst();
        }
    }

    public static final EnumProperty<Stuffing> STUFFING = EnumProperty.create("stuffing", Stuffing.class);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public Effigy(BlockBehaviour.Properties properties) {
        super(properties.lightLevel(state -> state.getValue(LIT) ? 10 : 0), Block.box(1, 0, 6, 15, 16, 10));
        registerDefaultState(
                stateDefinition.any().setValue(STUFFING, Stuffing.EMPTY).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STUFFING, LIT);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (state.getValue(LIT)) {
            return InteractionResult.PASS;
        }
        if (state.getValue(STUFFING) == Stuffing.EMPTY) {
            Optional<Stuffing> held = essence(stack);
            if (held.isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(STUFFING, held.get()), 3);
                level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        boolean flint = stack.is(Items.FLINT_AND_STEEL);
        if (!flint && !stack.is(Items.FIRE_CHARGE)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server
                && player instanceof ServerPlayer lighter
                && light(server, pos, state, lighter)) {
            if (flint) {
                stack.hurtAndBreak(1, player, hand);
            } else {
                stack.consume(1, player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && state.getValue(STUFFING) == Stuffing.EMPTY) {
            server.sendSystemMessage(Component.translatable("message.restless.effigy_hungry"), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** @return whether it caught: it does not when the spirit it calls could not come, so no essence is wasted */
    private boolean light(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer lighter) {
        SpiritType spirit = Spirits.byId(state.getValue(STUFFING).spirit()).orElseThrow();
        BirthRules.Verdict outlook = spirit.kind() == SpiritType.Kind.TERRITORIAL
                ? Births.room(level, pos)
                : Births.outlook(lighter, spirit);
        if (outlook == BirthRules.Verdict.COOLDOWN
                || outlook == BirthRules.Verdict.FULL
                || outlook == BirthRules.Verdict.CROWDED
                || outlook == BirthRules.Verdict.NOTHING) {
            lighter.sendSystemMessage(Component.translatable("message.restless.effigy_refused"), true);
            return false;
        }
        level.setBlock(pos, state.setValue(LIT, true), 3);
        level.scheduleTick(pos, this, Config.section("effigy").integer("burn_ticks"));
        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        SpiritType spirit = Spirits.byId(state.getValue(STUFFING).spirit()).orElseThrow();
        level.removeBlock(pos, false);
        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                pos.getX() + 0.5,
                pos.getY() + 0.8,
                pos.getZ() + 0.5,
                30,
                0.3,
                0.6,
                0.3,
                0.05);
        level.playSound(null, pos, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 1.0f, 0.6f);
        BirthRules.Verdict verdict = BirthRules.Verdict.NOTHING;
        ServerPlayer near = null;
        if (spirit.kind() == SpiritType.Kind.TERRITORIAL) {
            verdict = Births.place(level, spirit, pos);
        } else {
            Player closest = level.getNearestPlayer(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    Config.section("effigy").num("radius"),
                    true);
            near = closest instanceof ServerPlayer player ? player : null;
            if (near != null) {
                verdict = Births.lure(near, spirit);
            }
        }
        if (verdict != BirthRules.Verdict.BORN && verdict != BirthRules.Verdict.FED && near != null) {
            near.sendSystemMessage(Component.translatable("message.restless.effigy_cold"), true);
        }
    }

    /** Breaking it before it is lit gives the essence back. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        Stuffing held = state.getValue(STUFFING);
        if (!level.isClientSide() && held != Stuffing.EMPTY && !state.getValue(LIT) && !player.isCreative()) {
            popResource(
                    level, pos, new ItemStack(Content.ESSENCE.get(held.spirit()).get()));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && random.nextInt(3) == 0) {
            level.addParticle(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4,
                    pos.getY() + 1.0,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4,
                    0,
                    0.03,
                    0);
        }
    }

    private static Optional<Stuffing> essence(ItemStack stack) {
        for (Map.Entry<String, java.util.function.Supplier<net.minecraft.world.item.Item>> e :
                Content.ESSENCE.entrySet()) {
            if (stack.is(e.getValue().get())) {
                return Stuffing.of(e.getKey());
            }
        }
        return Optional.empty();
    }
}
