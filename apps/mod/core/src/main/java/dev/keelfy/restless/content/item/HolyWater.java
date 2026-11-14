package dev.keelfy.restless.content.item;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.ward.Weakness;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Holy water. Used while looking down it is poured at the player's feet: see {@link Weakness#pour}. */
public final class HolyWater extends Item {
    public HolyWater(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer server)
                || player.getXRot() < Config.section("weakness").integer("pour_pitch")) {
            return InteractionResult.PASS;
        }
        server.level()
                .sendParticles(
                        ParticleTypes.SPLASH,
                        server.getX(),
                        server.getY() + 0.1,
                        server.getZ(),
                        16,
                        0.3,
                        0.05,
                        0.3,
                        0.1);
        Weakness.pour(server);
        player.getItemInHand(hand).consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
