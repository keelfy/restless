package dev.keelfy.restless.content.item.charm;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Presence;
import dev.keelfy.restless.content.block.MimicBlock;
import dev.keelfy.restless.content.block.MimicBlockEntity;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.Scan;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Sense;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The shade's artifact: its holder sees in the dark, every living thing around shows through walls for a while, and the
 * spirits in reach are marked where they are.
 */
public class DarkEye extends Item {
    public DarkEye(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Config.Section config = Config.section("artifacts");
        int ticks = config.integer("eye_ticks");
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ticks));
        for (LivingEntity seen : level.getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(config.num("eye_radius")), e -> e != player)) {
            seen.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks));
        }
        if (player instanceof ServerPlayer server && level instanceof ServerLevel world) {
            mark(server, world, config, ticks);
            reveal(world, player.blockPosition(), config.integer("eye_radius"), world.getGameTime() + ticks);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.8f, 0.6f);
        player.getCooldowns().addCooldown(player.getItemInHand(hand), config.integer("eye_cooldown_ticks"));
        return InteractionResult.SUCCESS_SERVER;
    }

    /** The mimics in reach show their true face for as long as the eye sees. */
    private static void reveal(ServerLevel level, BlockPos centre, int radius, long until) {
        Scan.each(level, centre, radius, (pos, state) -> {
            if (state.getBlock() instanceof MimicBlock && level.getBlockEntity(pos) instanceof MimicBlockEntity mimic) {
                mimic.reveal(until);
            }
        });
    }

    /** Where the spirits are that the eye of {@code player} sees within {@code reach}. */
    public static List<Vec3> spots(ServerPlayer player, HauntData data, double reach) {
        List<Vec3> spots = new ArrayList<>();
        for (Sense.Spirit spirit : Sense.of(player, data)) {
            if (spirit.at() != null && spirit.at().distanceToSqr(player.position()) <= reach * reach) {
                spots.add(spirit.at());
            }
        }
        return spots;
    }

    /** Each spirit in reach shows as its faint haze where it stands, for as long as the eye sees. */
    private static void mark(ServerPlayer player, ServerLevel level, Config.Section config, int ticks) {
        List<Vec3> spots = spots(player, HauntData.get(level.getServer()), config.num("eye_radius"));
        if (spots.isEmpty()) {
            return;
        }
        int every = 10;
        Scheduler.repeat(player, 0, every, (p, n) -> {
            for (Vec3 at : spots) {
                Presence.emit(level, at);
            }
            return (n + 1) * every < ticks;
        });
    }
}
