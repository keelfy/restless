package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.EyesType;
import dev.keelfy.restless.content.item.Photo;
import dev.keelfy.restless.haunt.effect.ScreenPayload;
import dev.keelfy.restless.haunt.event.kit.Place;
import dev.keelfy.restless.haunt.event.kit.SimpleEvent;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Things laid over the player's own view and over a photograph: a handprint, frost, a face, eyes in the dark, a figure
 * on film. Nothing in the world changes. Whatever needs the mod on the client is skipped for a player without it.
 */
final class Screens {
    private Screens() {}

    static List<HauntEvent> all() {
        return List.of(
                // 512: a handprint on the screen, as on glass, fading away
                SimpleEvent.light(
                        "screen_handprint",
                        EventTag.PRESENCE,
                        SimpleEvent.ALWAYS,
                        player -> overlay(player, "screen_handprint", ScreenPayload.HANDPRINT)),
                // 513: frost creeps in along the screen's edges and melts
                SimpleEvent.light(
                        "screen_frost",
                        EventTag.PRESENCE,
                        SimpleEvent.ALWAYS,
                        player -> overlay(player, "screen_frost", ScreenPayload.FROST)),
                // 514: a face for two ticks; rare, off by default (weight 0)
                SimpleEvent.light(
                        "screen_face",
                        EventTag.PRESENCE,
                        SimpleEvent.ALWAYS,
                        player -> overlay(player, "screen_face", ScreenPayload.FACE)),
                // 511: a pair of eyes in a dark spot ahead; they blink and go out
                SimpleEvent.light("eyes_in_dark", EventTag.PRESENCE, player -> darkSpot(player) != null, Screens::eyes),
                // 529: a developed photo in the inventory now shows a figure that was not there
                SimpleEvent.light(
                        "photo_figure", EventTag.ITEMS, player -> developed(player) != null, Screens::photoFigure));
    }

    private static void overlay(ServerPlayer player, String id, int kind) {
        Config.Section config = Config.event(id);
        player.connection.send(new ClientboundCustomPayloadPacket(new ScreenPayload(
                kind,
                config.integer("ticks"),
                (float) config.num("strength"),
                player.getRandom().nextInt())));
    }

    /** Standing room ahead of the player that is dark enough for eyes to be seen in. */
    private static @Nullable BlockPos darkSpot(ServerPlayer player) {
        Config.Section config = Config.event("eyes_in_dark");
        double distance = config.num("min_distance")
                + player.getRandom().nextDouble() * (config.num("max_distance") - config.num("min_distance"));
        return Place.standingAhead(
                player,
                distance,
                0,
                config.num("max_angle"),
                pos -> player.level().getMaxLocalRawBrightness(pos) <= config.integer("dark_max"));
    }

    private static void eyes(ServerPlayer player) {
        BlockPos spot = darkSpot(player);
        if (spot == null) {
            return;
        }
        Vec3 at =
                Vec3.atBottomCenterOf(spot).add(0, Config.event("eyes_in_dark").num("height"), 0);
        player.connection.send(new ClientboundLevelParticlesPacket(
                EyesType.type(), false, false, at.x, at.y, at.z, 0f, 0f, 0f, 0f, 1));
    }

    /** A developed photo (one with a picture) in the main inventory, picked at random. */
    private static @Nullable ItemStack developed(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        List<ItemStack> photos = java.util.stream.IntStream.range(0, inventory.getContainerSize())
                .mapToObj(inventory::getItem)
                .filter(stack -> stack.is(Content.PHOTO.get()) && stack.has(Content.PICTURE.get()))
                .toList();
        return photos.isEmpty() ? null : photos.get(player.getRandom().nextInt(photos.size()));
    }

    private static void photoFigure(ServerPlayer player) {
        ItemStack photo = developed(player);
        if (photo == null) {
            return;
        }
        Config.Section config = Config.event("photo_figure");
        Random random = new Random(player.getRandom().nextLong());
        int span = config.integer("tall_max") - config.integer("tall_min") + 1;
        int tall = config.integer("tall_min") + random.nextInt(span);
        photo.set(Content.PICTURE.get(), Photo.withFigure(photo.get(Content.PICTURE.get()), random, tall));
    }
}
