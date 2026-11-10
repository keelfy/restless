package dev.keelfy.restless.content.item.consumable;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.Spells;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The poltergeist's knot: tied on a container, it takes in whatever is dropped within reach, as a hopper with no pipe,
 * until it is taken back: used sneaking on the container it comes off, and a container that is gone gives it up. One
 * knot to a container, and a full container lets items lie. It is a {@link Spells} record, so a restart does not undo
 * it.
 */
public final class RestlessKnot extends Item {
    public RestlessKnot(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        if (HopperBlockEntity.getContainerAt(level, pos) == null) {
            return InteractionResult.PASS;
        }
        Spells spells = Spells.get(level.getServer());
        long now = level.getGameTime();
        ServerPlayer player = context.getPlayer() instanceof ServerPlayer p ? p : null;
        List<Spells.Spell> tied = spells.near(Spells.Kind.KNOT, level.dimension(), pos, 0.5, now);
        if (!tied.isEmpty()) {
            if (player == null || !player.isShiftKeyDown()) {
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("message.restless.knot_tied"), true);
                }
                return InteractionResult.FAIL;
            }
            spells.remove(tied.getFirst());
            level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1.0f, 0.8f);
            player.addItem(new ItemStack(Content.RESTLESS_KNOT.get()));
            return InteractionResult.SUCCESS_SERVER;
        }
        spells.add(new Spells.Spell(Spells.Kind.KNOT, level.dimension(), pos, Spells.FOREVER));
        level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
        if (context.getPlayer() != null) {
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Call every few ticks: each live knot takes in what lies within its reach, if the container has room. */
    public static void tick(MinecraftServer server) {
        Spells spells = Spells.get(server);
        double radius = Config.section("consumables").num("knot_radius");
        long now = server.overworld().getGameTime();
        for (Spells.Spell knot : spells.all(Spells.Kind.KNOT)) {
            ServerLevel level = server.getLevel(knot.dimension());
            if (level == null || !level.isLoaded(knot.pos())) {
                continue;
            }
            Container container = HopperBlockEntity.getContainerAt(level, knot.pos());
            if (container == null) {
                spells.remove(knot);
                Block.popResource(level, knot.pos(), new ItemStack(Content.RESTLESS_KNOT.get()));
                continue;
            }
            Vec3 centre = Vec3.atCenterOf(knot.pos());
            for (ItemEntity item : level.getEntitiesOfClass(
                    ItemEntity.class,
                    new AABB(knot.pos()).inflate(radius),
                    i -> !i.hasPickUpDelay() && i.position().distanceToSqr(centre) <= radius * radius)) {
                ItemStack rest = HopperBlockEntity.addItem(
                        null, container, item.getItem().copy(), null);
                if (rest.getCount() == item.getItem().getCount()) {
                    continue;
                }
                level.sendParticles(
                        ParticleTypes.PORTAL, item.getX(), item.getY() + 0.2, item.getZ(), 6, 0.1, 0.1, 0.1, 0.4);
                if (rest.isEmpty()) {
                    item.discard();
                } else {
                    item.setItem(rest);
                }
            }
            if (server.getTickCount() % 40 == 0) {
                level.sendParticles(ParticleTypes.CRIT, centre.x, centre.y + 0.6, centre.z, 2, 0.3, 0.1, 0.3, 0.0);
            }
        }
    }
}
