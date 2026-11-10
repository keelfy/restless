package dev.keelfy.restless.content.item.consumable;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.Spells;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * The shade's salt, scattered on a block: no hostile mob spawns of itself within reach of that spot until the salt is
 * swept up again, which sneaking with salt in hand near the spot does. What a spawner makes, or a haunt event, is not
 * stopped. It is a {@link Spells} record, so a restart does not undo it.
 */
public final class DuskSalt extends Item {
    public DuskSalt(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Spells spells = Spells.get(level.getServer());
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            List<Spells.Spell> patches =
                    spells.near(Spells.Kind.DUSK, level.dimension(), pos, SWEEP, level.getGameTime());
            if (!patches.isEmpty()) {
                return sweep(level, spells, patches.getFirst(), context.getPlayer());
            }
        }
        spells.add(new Spells.Spell(Spells.Kind.DUSK, level.dimension(), pos, Spells.FOREVER));
        level.sendParticles(
                ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 20, 0.5, 0.1, 0.5, 0.02);
        level.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 1.0f, 0.5f);
        if (context.getPlayer() != null) {
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** How far from a patch a sneaking use still sweeps it up, in blocks. */
    private static final double SWEEP = 2.0;

    /** Takes a patch up, and gives its salt back. */
    private static InteractionResult sweep(ServerLevel level, Spells spells, Spells.Spell patch, Player player) {
        spells.remove(patch);
        level.sendParticles(
                ParticleTypes.SQUID_INK,
                patch.pos().getX() + 0.5,
                patch.pos().getY() + 0.2,
                patch.pos().getZ() + 0.5,
                10,
                0.4,
                0.1,
                0.4,
                0.01);
        level.playSound(null, patch.pos(), SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1.0f, 0.5f);
        player.addItem(new ItemStack(Content.DUSK_SALT.get()));
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Call before a mob is let into the world: whether dusk salt keeps it out. */
    public static boolean keeps(ServerLevel level, Mob mob, EntitySpawnReason reason) {
        if (reason != EntitySpawnReason.NATURAL || !(mob instanceof Enemy)) {
            return false;
        }
        double radius = Config.section("consumables").num("dusk_radius");
        return !Spells.get(level.getServer())
                .near(Spells.Kind.DUSK, level.dimension(), mob.blockPosition(), radius, level.getGameTime())
                .isEmpty();
    }

    /** Call every second or so: ended patches are forgotten, live ones breathe a little dark. */
    public static void tick(MinecraftServer server) {
        Spells spells = Spells.get(server);
        spells.prune(server.overworld().getGameTime());
        for (Spells.Spell patch : spells.all(Spells.Kind.DUSK)) {
            ServerLevel level = server.getLevel(patch.dimension());
            if (level != null && level.isLoaded(patch.pos())) {
                level.sendParticles(
                        ParticleTypes.SQUID_INK,
                        patch.pos().getX() + 0.5,
                        patch.pos().getY() + 0.3,
                        patch.pos().getZ() + 0.5,
                        1,
                        0.5,
                        0.1,
                        0.5,
                        0.0);
            }
        }
    }
}
