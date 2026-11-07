package dev.keelfy.restless.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Sanity;
import java.util.Set;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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
 * The lost one's charm: the first use marks the spot, the next, within the mark's time and in the same dimension, walks
 * the holder back to it at a cost in sanity.
 */
public final class WayBack extends Item {
    /** @param until game time after which the mark is forgotten */
    public record Mark(GlobalPos at, long until) {
        public static final Codec<Mark> CODEC = RecordCodecBuilder.create(i -> i.group(
                        GlobalPos.CODEC.fieldOf("at").forGetter(Mark::at),
                        Codec.LONG.fieldOf("until").forGetter(Mark::until))
                .apply(i, Mark::new));
        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Mark> STREAM_CODEC =
                StreamCodec.composite(GlobalPos.STREAM_CODEC, Mark::at, ByteBufCodecs.VAR_LONG, Mark::until, Mark::new);
    }

    public WayBack(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        Config.Section config = Config.section("charms");
        long now = level.getGameTime();
        Mark mark = stack.get(Content.MARK.get());
        boolean back = mark != null
                && mark.until() > now
                && mark.at().dimension().equals(level.dimension())
                && player instanceof ServerPlayer
                && level instanceof ServerLevel;
        if (!back) {
            stack.set(
                    Content.MARK.get(),
                    new Mark(
                            GlobalPos.of(level.dimension(), player.blockPosition()),
                            now + config.integer("way_ticks")));
            level.playSound(
                    null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.6f);
            if (player instanceof ServerPlayer server) {
                server.sendSystemMessage(Component.translatable("message.restless.way_marked"), true);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        ServerPlayer server = (ServerPlayer) player;
        Vec3 to = Vec3.atBottomCenterOf(mark.at().pos());
        server.teleportTo((ServerLevel) level, to.x, to.y, to.z, Set.of(), player.getYRot(), player.getXRot(), true);
        stack.remove(Content.MARK.get());
        Sanity.set(server, Sanity.of(server) - config.num("way_sanity_cost"));
        level.playSound(null, server.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 0.5f);
        player.getCooldowns().addCooldown(stack, config.integer("way_cooldown_ticks"));
        return InteractionResult.SUCCESS_SERVER;
    }
}
