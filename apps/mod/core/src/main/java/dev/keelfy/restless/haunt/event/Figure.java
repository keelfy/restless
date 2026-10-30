package dev.keelfy.restless.haunt.event;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A human-shaped figure for events that only need something to stand, watch or walk: the vanilla mannequin entity. Also
 * its nameplate alone.
 */
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

    /** The vanilla default player skins, each in both arm widths. */
    private static final List<String> SKINS = List.of("alex", "ari", "efe", "kai", "makena", "noor", "steve", "sunny", "zuri");

    /** A skin of no one: a fresh id with no textures, dressed in a random default skin, Steve, Alex and the rest, wide or slim. */
    static ResolvableProfile stranger(ServerPlayer player) {
        ResolvableProfile profile = ResolvableProfile.createResolved(nobody(player, "Steve"));
        String model = player.getRandom().nextBoolean() ? "wide" : "slim";
        String skin = SKINS.get(player.getRandom().nextInt(SKINS.size()));
        return ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, profile).result()
            .map(tag -> {
                CompoundTag data = (CompoundTag) tag;
                data.putString("texture", "entity/player/" + model + "/" + skin);
                data.putString("model", model);
                return ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, data).result().orElse(profile);
            })
            .orElse(profile);
    }

    /** A profile of no one, under {@code name}, with a fresh id and no textures. */
    static GameProfile nobody(ServerPlayer player, String name) {
        return new GameProfile(new UUID(player.getRandom().nextLong(), player.getRandom().nextLong()), name);
    }

    static void face(Mannequin figure, ServerPlayer player) {
        figure.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
    }

    /** Head and body toward {@code yaw}, level, as a walker looks where it goes. */
    static void turn(Mannequin figure, float yaw) {
        figure.setYRot(yaw);
        figure.setYHeadRot(yaw);
        figure.setYBodyRot(yaw);
        figure.setXRot(0.0F);
    }

    /** A name with nobody under it: a text display that looks like a player's nameplate, seen through walls as one is. */
    static Display.@Nullable TextDisplay nameplate(ServerLevel level, Vec3 at, String name) {
        Display.TextDisplay plate = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.EVENT);
        if (plate == null) {
            return null;
        }
        plate.setPos(at);
        rewrite(plate, level, data -> {
            data.putString("text", name);
            data.putString("billboard", "center");
            data.putBoolean("see_through", true);
            data.putBoolean("default_background", true);
        });
        level.addFreshEntity(plate);
        return plate;
    }

    /** The profile (name and skin) has no setter, so it goes in through the entity's save format; the caption is hidden. */
    private static void dress(Mannequin figure, ResolvableProfile profile, ServerLevel level) {
        rewrite(figure, level, data -> {
            ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, profile).result().ifPresent(tag -> data.put("profile", tag));
            data.putBoolean("hide_description", true);
        });
    }

    /** Sets what has no setter: the entity is saved, {@code edit} changes the saved data, and the entity loads it back. */
    private static void rewrite(Entity entity, ServerLevel level, Consumer<CompoundTag> edit) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        entity.saveWithoutId(out);
        CompoundTag data = out.buildResult();
        edit.accept(data);
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
    }
}
