package dev.keelfy.restless.haunt.event.kit;

import com.mojang.authlib.GameProfile;
import dev.keelfy.restless.haunt.scenario.ScenarioContext;
import dev.keelfy.restless.spirit.Story;
import java.util.List;
import java.util.Random;
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
public final class Figure {
    private Figure() {}

    /**
     * A mannequin only one player is sent. The server tracks it like any entity, but every other player never gets it,
     * so nobody else sees it or can push it. It is still a plain mannequin to the client.
     */
    private static final class Private extends Mannequin {
        private final UUID owner;

        Private(ServerLevel level, UUID owner) {
            super(EntityTypes.MANNEQUIN, level);
            this.owner = owner;
        }

        @Override
        public boolean broadcastToPlayer(ServerPlayer viewer) {
            return viewer.getUUID().equals(owner) && super.broadcastToPlayer(viewer);
        }
    }

    /**
     * A silent, unhurtable mannequin wearing {@code profile}, standing on {@code spot} and facing the player. Only that
     * player sees it.
     */
    public static @Nullable Mannequin spawn(ServerPlayer player, BlockPos spot, ResolvableProfile profile) {
        ServerLevel level = player.level();
        Mannequin figure = new Private(level, player.getUUID());
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
    private static final List<String> SKINS =
            List.of("alex", "ari", "efe", "kai", "makena", "noor", "steve", "sunny", "zuri");

    /**
     * A skin of no one: a fresh id with no textures, dressed in a random default skin, Steve, Alex and the rest, wide
     * or slim.
     */
    public static ResolvableProfile stranger(ServerPlayer player) {
        ResolvableProfile profile = ResolvableProfile.createResolved(nobody(player, "Steve"));
        String model = player.getRandom().nextBoolean() ? "wide" : "slim";
        String skin = SKINS.get(player.getRandom().nextInt(SKINS.size()));
        return ResolvableProfile.CODEC
                .encodeStart(NbtOps.INSTANCE, profile)
                .result()
                .map(tag -> {
                    CompoundTag data = (CompoundTag) tag;
                    data.putString("texture", "entity/player/" + model + "/" + skin);
                    data.putString("model", model);
                    return ResolvableProfile.CODEC
                            .parse(NbtOps.INSTANCE, data)
                            .result()
                            .orElse(profile);
                })
                .orElse(profile);
    }

    /** The longest name a player can have; the client is sent nothing longer. */
    private static final int NAME_MAX = 16;

    /** A made-up player nickname, as spirits have, short enough to be a real player's. */
    public static String nickname(ServerPlayer player) {
        return ScenarioContext.nickname(() -> freshNickname(player));
    }

    private static String freshNickname(ServerPlayer player) {
        Random random = new Random(player.getRandom().nextLong());
        String name = Story.nickname(random);
        for (int i = 0; i < 8 && name.length() > NAME_MAX; i++) {
            name = Story.nickname(random);
        }
        return name.length() > NAME_MAX ? name.substring(0, NAME_MAX) : name;
    }

    /** A profile of no one, under {@code name}, with a fresh id and no textures. */
    public static GameProfile nobody(ServerPlayer player, String name) {
        return new GameProfile(
                new UUID(player.getRandom().nextLong(), player.getRandom().nextLong()), name);
    }

    public static void face(Mannequin figure, ServerPlayer player) {
        figure.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
    }

    /** Head and body toward {@code yaw}, level, as a walker looks where it goes. */
    public static void turn(Mannequin figure, float yaw) {
        figure.setYRot(yaw);
        figure.setYHeadRot(yaw);
        figure.setYBodyRot(yaw);
        figure.setXRot(0.0F);
    }

    /**
     * A name with nobody under it: a text display that looks like a player's nameplate, seen through walls as one is.
     */
    public static Display.@Nullable TextDisplay nameplate(ServerLevel level, Vec3 at, String name) {
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

    /**
     * The profile (name and skin) has no setter, so it goes in through the entity's save format; the caption is hidden.
     */
    private static void dress(Mannequin figure, ResolvableProfile profile, ServerLevel level) {
        rewrite(figure, level, data -> {
            ResolvableProfile.CODEC
                    .encodeStart(NbtOps.INSTANCE, profile)
                    .result()
                    .ifPresent(tag -> data.put("profile", tag));
            data.putBoolean("hide_description", true);
        });
    }

    /**
     * Sets what has no setter: the entity is saved, {@code edit} changes the saved data, and the entity loads it back.
     */
    private static void rewrite(Entity entity, ServerLevel level, Consumer<CompoundTag> edit) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        entity.saveWithoutId(out);
        CompoundTag data = out.buildResult();
        edit.accept(data);
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
    }
}
