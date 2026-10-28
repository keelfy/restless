package dev.keelfy.restless.haunt.event;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;

/** A human-shaped figure for events that only need something to stand, watch or walk: the vanilla mannequin entity. */
final class Figure {
    private Figure() {}

    /** A silent, unhurtable mannequin wearing {@code profile}, standing on {@code spot} and facing the player. */
    static @Nullable Mannequin spawn(ServerPlayer player, BlockPos spot, ResolvableProfile profile) {
        ServerLevel level = player.level();
        Mannequin figure = EntityTypes.MANNEQUIN.create(level, EntitySpawnReason.EVENT);
        if (figure == null) {
            return null;
        }
        figure.setPos(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        dress(figure, profile, level);
        figure.setNoGravity(true);
        figure.setSilent(true);
        figure.setPermanentlyInvulnerable(true);
        face(figure, player);
        level.addFreshEntity(figure);
        return figure;
    }

    static void face(Mannequin figure, ServerPlayer player) {
        figure.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
    }

    /** The profile (name and skin) has no setter, so it goes in through the entity's save format; the caption is hidden. */
    private static void dress(Mannequin figure, ResolvableProfile profile, ServerLevel level) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        figure.saveWithoutId(out);
        CompoundTag data = out.buildResult();
        ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, profile).result().ifPresent(tag -> data.put("profile", tag));
        data.putBoolean("hide_description", true);
        figure.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
    }
}
