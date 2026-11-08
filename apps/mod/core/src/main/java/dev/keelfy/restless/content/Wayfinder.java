package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Session;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The lost one's ash: used, it lays footprints ahead of the player toward where they last died, or, sneaking, toward
 * their respawn point, for as long as it burns. Only one trail leads a player at a time.
 */
public final class Wayfinder extends Item {
    /** The game time each player's trail ends at. */
    private static final Map<UUID, Long> LEADING = Session.SERVER.keep(new HashMap<>());

    public Wayfinder(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel world)) {
            return InteractionResult.SUCCESS;
        }
        long now = world.getGameTime();
        if (LEADING.getOrDefault(server.getUUID(), 0L) > now) {
            server.sendSystemMessage(Component.translatable("message.restless.wayfinder_leading"), true);
            return InteractionResult.FAIL;
        }
        boolean home = server.isShiftKeyDown();
        GlobalPos target =
                home ? respawn(server) : server.getLastDeathLocation().orElse(null);
        if (target == null || !target.dimension().equals(world.dimension())) {
            server.sendSystemMessage(Component.translatable("message.restless.wayfinder_nowhere"), true);
            return InteractionResult.FAIL;
        }
        ItemStack ash = player.getItemInHand(hand);
        ash.consume(1, player);
        server.sendSystemMessage(
                Component.translatable(home ? "message.restless.wayfinder_home" : "message.restless.wayfinder_death"),
                true);
        world.playSound(null, server.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0f, 0.8f);
        lead(server, world, target.pos());
        return InteractionResult.SUCCESS_SERVER;
    }

    private static GlobalPos respawn(ServerPlayer player) {
        ServerPlayer.RespawnConfig config = player.getRespawnConfig();
        return config != null
                ? config.respawnData().globalPos()
                : player.level().getRespawnData().globalPos();
    }

    private static void lead(ServerPlayer player, ServerLevel level, BlockPos target) {
        Config.Section config = Config.section("consumables");
        int ticks = config.integer("ash_ticks");
        UUID id = player.getUUID();
        LEADING.put(id, level.getGameTime() + ticks);
        // Where a print stands and until when, so a step never lays on one that is still there.
        Map<Long, Long> laid = new HashMap<>();
        Scheduler.repeat(player, 0, 20, (p, n) -> {
            long now = level.getGameTime();
            if (p.level() != level || LEADING.getOrDefault(id, 0L) <= now) {
                LEADING.remove(id);
                return false;
            }
            double dx = target.getX() + 0.5 - p.getX();
            double dz = target.getZ() + 0.5 - p.getZ();
            double distance = Math.hypot(dx, dz);
            if (distance < config.num("ash_arrive")) {
                p.sendSystemMessage(Component.translatable("message.restless.wayfinder_arrived"), true);
                LEADING.remove(id);
                return false;
            }
            lay(level, p, Math.atan2(dz, dx), Math.min(config.num("ash_reach"), distance), laid, now, config);
            laid.values().removeIf(until -> until <= now);
            return true;
        });
    }

    /** Footprints from the player toward the angle, one stride apart, alternating feet, where there is ground. */
    private static void lay(
            ServerLevel level,
            ServerPlayer player,
            double angle,
            double reach,
            Map<Long, Long> laid,
            long now,
            Config.Section config) {
        double stride = config.num("ash_step");
        int life = config.integer("ash_print_ticks");
        Vec3 forward = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        int k = 0;
        for (double d = stride * 1.5; d <= reach; d += stride, k++) {
            boolean left = (k & 1) == 0;
            double x = player.getX() + forward.x * d + side.x * (left ? -0.15 : 0.15);
            double z = player.getZ() + forward.z * d + side.z * (left ? -0.15 : 0.15);
            BlockPos ground = ground(level, BlockPos.containing(x, player.getY(), z));
            if (ground == null || laid.containsKey(ground.asLong())) {
                continue;
            }
            if (AshFootprint.lay(level, new Vec3(x, ground.getY() + 0.01, z), angle, 1, left, life)) {
                laid.put(ground.asLong(), now + life);
            }
        }
    }

    /** The air above the first solid floor within a short climb or fall of {@code near}, if any. */
    private static BlockPos ground(ServerLevel level, BlockPos near) {
        if (!level.hasChunkAt(near)) {
            return null;
        }
        for (int dy = 2; dy >= -8; dy--) {
            BlockPos at = near.above(dy);
            BlockPos below = at.below();
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                    && level.getBlockState(at).getCollisionShape(level, at).isEmpty()) {
                return at;
            }
        }
        return null;
    }
}
