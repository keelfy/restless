package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Each player's grip, 0..1. It slips in the dark, inside a territorial spirit's reach and with every event; light,
 * sleep, a full stomach and a salt-warded home bring it back. Low sanity shows what is otherwise hidden; the client
 * sees it as darkness closing in from the edges ({@link SanityOverlay}).
 */
public final class Sanity {
    /** What each client last heard; a new player object (join, respawn) is told again. */
    private static final Map<ServerPlayer, Float> SENT = new WeakHashMap<>();

    private Sanity() {}

    public static double of(ServerPlayer player) {
        return HauntData.get(player.level().getServer()).sanity(player.getUUID());
    }

    /** Whether the player's sanity is low enough to see what only the shaken see. */
    public static boolean shaken(ServerPlayer player) {
        return of(player) < Config.section("sanity").num("reveal_below");
    }

    public static void tick(MinecraftServer server) {
        Config.Section config = Config.section("sanity");
        int interval = config.integer("check_ticks");
        if (server.getTickCount() % interval != 0) {
            return;
        }
        HauntData data = HauntData.get(server);
        double seconds = interval / (double) Ticks.SECOND;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isSpectator() && !player.isCreative()) {
                set(player, of(player) + drift(player, data, config) * seconds);
            } else {
                sync(player, of(player));
            }
        }
    }

    /** Change per second right now. */
    private static double drift(ServerPlayer player, HauntData data, Config.Section config) {
        int light = player.level().getMaxLocalRawBrightness(player.blockPosition());
        double drift = 0;
        if (light < config.integer("dark_light")) {
            boolean heldLight = emitsLight(player.getMainHandItem()) || emitsLight(player.getOffhandItem());
            drift -= config.num("dark_loss") * (heldLight ? config.num("held_light_loss_multiplier") : 1.0);
        } else if (light >= config.integer("lit_light")) {
            drift += config.num("lit_gain");
        }
        if (data.territorial().stream().anyMatch(t -> t.reaches(player))) {
            drift -= config.num("spirit_loss");
        }
        if (player.isSleeping()) {
            drift += config.num("sleep_gain");
        }
        if (player.getFoodData().getFoodLevel() >= config.integer("fed_food")) {
            drift += config.num("fed_gain");
        }
        if (Wards.shelters(player)) {
            drift += config.num("ward_gain");
        }
        return drift;
    }

    /** Server-side light sources work even when held lighting is rendered only by a shader. */
    private static boolean emitsLight(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(Items.LAVA_BUCKET)
                        || stack.getItem() instanceof BlockItem block
                                && block.getBlock().defaultBlockState().getLightEmission() > 0);
    }

    /** An event just ran on the player. */
    public static void shock(ServerPlayer player) {
        if (!player.isSpectator() && !player.isCreative()) {
            set(player, of(player) - Config.section("sanity").num("event_loss"));
        }
    }

    public static void set(ServerPlayer player, double value) {
        double clamped = Math.clamp(value, 0.0, 1.0);
        if (clamped != of(player)) {
            HauntData.get(player.level().getServer()).putSanity(player.getUUID(), clamped);
        }
        if (clamped < Config.section("sanity").num("overlay_below")) {
            Journal.unlock(player, Notebook.Chapter.SANITY);
        }
        sync(player, clamped);
    }

    private static void sync(ServerPlayer player, double value) {
        Float sent = SENT.get(player);
        if (sent == null || Math.abs(sent - value) >= 0.01 || sent != (float) value && (value == 0.0 || value == 1.0)) {
            SENT.put(player, (float) value);
            player.connection.send(new ClientboundCustomPayloadPacket(new SanityPayload((float) value)));
        }
    }
}
