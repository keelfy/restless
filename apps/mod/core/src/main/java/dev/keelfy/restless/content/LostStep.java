package dev.keelfy.restless.content;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Sanity;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The lost one's charm: the holder slips a few steps ahead, through whatever is in the way, and comes out where there
 * is room. It costs a little sanity and rests before the next.
 */
public final class LostStep extends Item {
    /** Blocks the step does not pass; builders use this to answer it. */
    public static final TagKey<Block> STEP_PROOF =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Restless.MOD_ID, "step_proof"));

    public LostStep(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel world)) {
            return InteractionResult.PASS;
        }
        Config.Section config = Config.section("charms");
        Vec3 to = landing(server, config.num("step_distance"));
        if (to == null) {
            return InteractionResult.FAIL;
        }
        Vec3 from = server.position();
        world.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1.0, from.z, 24, 0.3, 0.6, 0.3, 0.2);
        server.teleportTo(world, to.x, to.y, to.z, Set.of(), server.getYRot(), server.getXRot(), true);
        world.sendParticles(ParticleTypes.PORTAL, to.x, to.y + 1.0, to.z, 24, 0.3, 0.6, 0.3, 0.2);
        world.playSound(null, server.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.6f);
        Sanity.set(server, Sanity.of(server) - config.num("step_sanity_cost"));
        server.getCooldowns().addCooldown(server.getItemInHand(hand), config.integer("step_cooldown_ticks"));
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * The farthest spot along the look, within {@code reach}, where the player fits, never past a wall they could not
     * pass (a wall of one block or two is passed; solid rock beyond that stops it). Null if there is none.
     */
    private static Vec3 landing(ServerPlayer player, double reach) {
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        // A wall is slipped through, but not a mountain: the step ends at the first free room after at most 2 blocks of
        // it.
        HitResult wall = player.level()
                .clip(new ClipContext(
                        eye, eye.add(look.scale(reach)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double open = wall.getType() == HitResult.Type.MISS ? reach : eye.distanceTo(wall.getLocation());
        double limit = Math.min(reach, open + 2.0);
        // Some blocks are not slipped through at all: the step ends short of the first of them.
        for (double d = 0.5; d <= limit; d += 0.25) {
            Vec3 at = eye.add(look.scale(d));
            if (player.level().getBlockState(BlockPos.containing(at)).is(STEP_PROOF)
                    || player.level()
                            .getBlockState(BlockPos.containing(at.subtract(0, 1.0, 0)))
                            .is(STEP_PROOF)) {
                limit = Math.max(0.0, d - 0.75);
                break;
            }
        }
        for (double d = limit; d >= 1.0; d -= 0.25) {
            Vec3 move = look.scale(d);
            if (player.level().noCollision(player, player.getBoundingBox().move(move))) {
                return player.position().add(move);
            }
        }
        return null;
    }
}
