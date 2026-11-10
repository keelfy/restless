package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.block.Altar;
import dev.keelfy.restless.content.item.Essences;
import dev.keelfy.restless.content.item.Reliquary;
import dev.keelfy.restless.content.item.RequiemBell;
import dev.keelfy.restless.haunt.requiem.Movement;
import dev.keelfy.restless.haunt.requiem.Movements;
import dev.keelfy.restless.haunt.requiem.Rite;
import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.Spirits;
import dev.keelfy.restless.spirit.Story;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * The finale of the essence arc: a player who has sung the Requiem is <em>released</em>. No spirit is born on them or
 * acts on them (territorial ones still act on everyone else), and their sanity stays full. It is a saved state of the
 * player, not an item, so dying does not lose it; the Requiem Bell gives it back ({@link #takeBack}). The rite itself,
 * and the call to {@link #release} that ends it well, belong to {@link #begin}.
 */
public final class Requiem {
    private static final String ADVANCEMENT_CRITERION = "impossible";
    /** The rite each singing player has under way. Memory only, like every rite: a stopping server ends them. */
    private static final Map<UUID, Rite> RITES = Session.SERVER.keep(new ConcurrentHashMap<>());

    private Requiem() {}

    /** Whether the Requiem has released this player. */
    public static boolean released(ServerPlayer player) {
        return HauntData.get(player.level().getServer()).released(player.getUUID());
    }

    /**
     * Where the rite starts at an altar, for the player's {@code reliquary} (not taken from them): it must be full, the
     * altar ringed with salt, {@code requiem.candles} candles lit around it and a bell near. It runs as
     * {@code requiem.movement_ticks} per movement and calls {@link #release} when the player passes.
     *
     * @return whether a rite began; if not, the player was told why
     */
    public static boolean begin(ServerPlayer player, ServerLevel level, BlockPos altar, ItemStack reliquary) {
        return begin(player, level, altar, reliquary, Config.section("requiem").integer("movement_ticks"));
    }

    /** As {@link #begin(ServerPlayer, ServerLevel, BlockPos, ItemStack)}, with each movement {@code ticks} long. */
    public static boolean begin(
            ServerPlayer player, ServerLevel level, BlockPos altar, ItemStack reliquary, int ticks) {
        Config.Section config = Config.section("requiem");
        HauntData data = HauntData.get(level.getServer());
        Essences held = Reliquary.of(reliquary);
        if (!held.full()) {
            return refuse(player, "requiem_not_full");
        }
        if (data.released(player.getUUID())) {
            return refuse(player, "requiem_released");
        }
        if (data.requiemWary(player.getUUID()) > level.getServer().overworld().getGameTime()) {
            return refuse(player, "requiem_wary");
        }
        if (RITES.containsKey(player.getUUID()) || running(altar)) {
            return refuse(player, "requiem_busy");
        }
        List<BlockPos> candles = new ArrayList<>();
        Scan.each(level, altar, config.integer("candle_radius"), (pos, state) -> {
            if (AbstractCandleBlock.isLit(state)) {
                candles.add(pos.immutable());
            }
        });
        candles.sort(Comparator.comparingDouble(pos -> pos.distSqr(altar)));
        if (!Altar.isComplete(level, altar)
                || !Wards.ringed(level, altar)
                || candles.size() < config.integer("candles")
                || !belled(level, altar, config.integer("bell_radius"))) {
            return refuse(player, "requiem_incomplete", config.integer("candles"), config.integer("bell_radius"));
        }
        Rite rite = new Rite(player, level, altar, ticks, held.held(), candles.subList(0, config.integer("candles")));
        RITES.put(player.getUUID(), rite);
        level.playSound(null, altar, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 0.5f);
        player.sendSystemMessage(Component.translatable("message.restless.requiem_begins"));
        return true;
    }

    private static boolean refuse(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable("message.restless." + key, args), true);
        return false;
    }

    private static boolean belled(ServerLevel level, BlockPos altar, int radius) {
        boolean[] found = {false};
        Scan.each(level, altar, radius, (pos, state) -> found[0] |= state.is(Blocks.BELL));
        return found[0];
    }

    /** The rite this player is singing, if any. */
    public static @Nullable Rite rite(ServerPlayer player) {
        return RITES.get(player.getUUID());
    }

    /** Whether a rite stands at this altar, or one of its decoys is this block. */
    public static boolean running(BlockPos pos) {
        return RITES.values().stream().anyMatch(rite -> rite.altar.equals(pos) || rite.decoys.contains(pos));
    }

    /** Whether this block is a false altar a movement set up; it cannot be broken. */
    public static boolean decoy(BlockPos pos) {
        return RITES.values().stream().anyMatch(rite -> rite.decoys.contains(pos));
    }

    /**
     * The player used {@code stack} on an altar a rite stands at, or on one of its decoys. Only holy water, in the
     * Mimic's movement, means anything.
     */
    public static InteractionResult used(ServerPlayer player, BlockPos pos, ItemStack stack) {
        Rite rite = RITES.get(player.getUUID());
        if (rite == null
                || !rite.type.equals("mimic")
                || !stack.is(Content.HOLY_WATER.get())
                || !(pos.equals(rite.altar) || rite.decoys.contains(pos))) {
            player.sendSystemMessage(Component.translatable("message.restless.requiem_busy"), true);
            return InteractionResult.FAIL;
        }
        stack.consume(1, player);
        rite.blessed = pos;
        rite.level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.8f);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** A bell at {@code pos} was rung: it counts for every rite with that bell near its altar. */
    public static void rung(ServerLevel level, BlockPos pos) {
        if (RITES.isEmpty()) {
            return;
        }
        int reach = Config.section("requiem").integer("bell_radius");
        for (Rite rite : RITES.values()) {
            if (rite.level == level
                    && Math.abs(pos.getX() - rite.altar.getX()) <= reach
                    && Math.abs(pos.getZ() - rite.altar.getZ()) <= reach) {
                rite.rings.add(rite.tick);
            }
        }
    }

    /** The player answered the demon's bargain: taking it skips the movement for {@code requiem.demon_cost} candles. */
    public static void answer(ServerPlayer player, boolean accept) {
        Rite rite = RITES.get(player.getUUID());
        if (rite == null || !rite.type.equals("demon") || rite.bargain != null) {
            player.sendSystemMessage(Component.translatable("message.restless.requiem_no_bargain"), true);
            return;
        }
        rite.bargain = accept;
        if (accept) {
            snuff(rite, Config.section("requiem").integer("demon_cost"));
        }
        player.sendSystemMessage(
                Component.translatable(accept ? "message.restless.requiem_taken" : "message.restless.requiem_refused"),
                true);
    }

    /** Call once per server tick: every rite advances, or ends if its player has gone. */
    public static void tick(MinecraftServer server) {
        if (RITES.isEmpty()) {
            return;
        }
        for (Rite rite : List.copyOf(RITES.values())) {
            ServerPlayer player = rite.player;
            if (player.isRemoved()
                    || !player.isAlive()
                    || player.level() != rite.level
                    || player.level().getServer() != server) {
                abort(rite);
                continue;
            }
            try {
                step(rite);
            } catch (RuntimeException e) {
                Restless.LOGGER.error("The Requiem failed and was dropped", e);
                abort(rite);
            }
        }
    }

    /** Ends every rite without a loss; call when the server is stopping. */
    public static void abortAll() {
        List.copyOf(RITES.values()).forEach(Requiem::abort);
    }

    /** Ends this player's rite, if any, as a stopping server does. */
    public static void abort(ServerPlayer player) {
        Rite rite = RITES.get(player.getUUID());
        if (rite != null) {
            abort(rite);
        }
    }

    /** A rite cut short: death, leaving, a stopping server. What it changed is put back; nothing is lost or locked. */
    private static void abort(Rite rite) {
        RITES.remove(rite.player.getUUID(), rite);
        if (rite.current != null) {
            rite.current.end(rite);
            rite.current = null;
        }
        if (rite.player.isAlive()
                && !rite.player.isRemoved()
                && rite.player.level().getServer() != null) {
            rite.player.sendSystemMessage(Component.translatable("message.restless.requiem_aborted"), true);
        }
    }

    private static void step(Rite rite) {
        if (rite.current == null) {
            rite.type = rite.order.get(rite.index);
            rite.current = Movements.of(rite.type);
            rite.tick = 0;
            rite.rings.clear();
            rite.blessed = null;
            rite.bargain = null;
            rite.skip = false;
            rite.player.sendSystemMessage(Component.translatable("requiem.restless.cue." + rite.type));
            rite.current.start(rite);
        }
        rite.current.tick(rite, rite.tick);
        if (rite.tick % 10 == 0) {
            for (BlockPos pos : rite.candles.out()) {
                if (!rite.free.contains(pos)) {
                    rite.put(pos);
                }
            }
        }
        if (rite.tick % 20 == 0) {
            rite.player.sendSystemMessage(
                    Component.translatable(
                            "message.restless.requiem_progress",
                            Component.translatable("requiem.restless.movement." + rite.type),
                            rite.index + 1,
                            rite.order.size(),
                            rite.burning(),
                            rite.candles.lit().size() + rite.candles.out().size()),
                    true);
        }
        if (++rite.tick >= rite.length || rite.skip) {
            finish(rite);
        }
    }

    /** A movement's time is out: a failed one costs a candle; the last one decides the Requiem. */
    private static void finish(Rite rite) {
        Movement movement = rite.current;
        boolean passed = rite.skip || movement.passed(rite);
        movement.end(rite);
        rite.current = null;
        if (!passed) {
            snuff(rite, 1);
            rite.player.sendSystemMessage(Component.translatable(
                    "message.restless.requiem_missed",
                    Component.translatable("requiem.restless.movement." + rite.type)));
        }
        if (++rite.index < rite.order.size()) {
            return;
        }
        RITES.remove(rite.player.getUUID(), rite);
        if (rite.candles.standing()
                && rite.burning() >= Config.section("requiem").integer("needed")) {
            rite.level.playSound(null, rite.altar, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 0.6f);
            release(rite.player);
        } else {
            fail(rite);
        }
    }

    private static void snuff(Rite rite, int count) {
        for (BlockPos pos : rite.candles.snuff(count, rite.player.getRandom()::nextInt)) {
            rite.put(pos);
        }
        rite.level.playSound(
                null, rite.altar, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.0f, 0.5f);
    }

    /**
     * The Requiem failed: the reliquary keeps its essences but one chosen at random, every personal spirit grows
     * angrier, and the altar refuses another for {@code requiem.wary_ticks}.
     */
    private static void fail(Rite rite) {
        ServerPlayer player = rite.player;
        Config.Section config = Config.section("requiem");
        HauntData data = HauntData.get(rite.level.getServer());
        long now = rite.level.getServer().overworld().getGameTime();
        Component lost = Component.empty();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            Essences held = Reliquary.of(stack);
            if (stack.is(Content.RELIQUARY.get()) && !held.held().isEmpty()) {
                List<String> kept = new ArrayList<>(held.held());
                lost = Component.translatable(
                        "spirit.restless." + kept.remove(player.getRandom().nextInt(kept.size())));
                stack.set(Content.ESSENCES.get(), new Essences(List.copyOf(kept), held.known()));
                break;
            }
        }
        for (HauntData.Haunting spirit : List.copyOf(data.get(player.getUUID()))) {
            data.change(spirit.uuid(), h -> h.angered(config.num("fail_anger")));
        }
        data.putRequiemWary(player.getUUID(), now + config.integer("wary_ticks"));
        rite.level.playSound(null, rite.altar, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6f, 1.4f);
        player.sendSystemMessage(Component.translatable("message.restless.requiem_failed", lost));
    }

    /**
     * The Requiem went well: every personal spirit is laid to rest, each under a stone that names it and with a last
     * line in its dossier; the player is released, the reliquary becomes the bell, and the closing text plays.
     */
    public static void release(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        HauntData data = HauntData.get(server);
        long now = server.overworld().getGameTime();
        List<HauntData.Haunting> own = List.copyOf(Director.hauntings(player, data, now));
        for (int i = 0; i < own.size(); i++) {
            layToRest(player, data, own.get(i), i, own.size());
        }
        for (int i = own.size() - 1; i >= 0; i--) {
            Director.remove(player, data, i, now);
        }
        data.putReleased(player.getUUID(), true);
        Sanity.set(player, 1.0);
        RequiemBell.replaceReliquary(player);
        Journal.unlock(player, Notebook.Chapter.REQUIEM);
        AdvancementHolder advancement =
                server.getAdvancements().get(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "requiem"));
        if (advancement != null) {
            player.getAdvancements().award(advancement, ADVANCEMENT_CRITERION);
        }
        player.connection.send(new ClientboundCustomPayloadPacket(new RequiemPayload()));
    }

    /**
     * The bell was rung: the release is given back, and {@code requiem.bell_spirits} fresh personal spirits are born at
     * the start anger of any birth. Nothing happens to a player who is not released.
     */
    public static void takeBack(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        HauntData data = HauntData.get(server);
        if (!data.released(player.getUUID())) {
            return;
        }
        data.putReleased(player.getUUID(), false);
        long now = server.overworld().getGameTime();
        Config.Section births = Config.section("births");
        List<SpiritType> pool = new ArrayList<>(Spirits.personal());
        for (int i = Config.section("requiem").integer("bell_spirits"); i > 0 && !pool.isEmpty(); i--) {
            SpiritType type = pool.remove(player.getRandom().nextInt(pool.size()));
            HauntData.Haunting born = Director.add(player, data, type, now);
            if (born != null) {
                data.change(
                        born.uuid(),
                        h -> h.angered(births.num("start_anger")).rescheduled(now + births.integer("grace_ticks")));
            }
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0f, 0.5f);
        player.sendSystemMessage(Component.translatable("message.restless.bell_rung"), true);
    }

    /** A stone and a sign near the player name the spirit; its dossier gets its last line. */
    private static void layToRest(
            ServerPlayer player, HauntData data, HauntData.Haunting spirit, int index, int count) {
        ServerLevel level = player.level();
        Story story = Story.of(spirit.seed());
        Component type = Evolution.name(spirit);
        double angle = Math.PI * 2 * index / count + 0.5;
        BlockPos stone = surface(
                level,
                BlockPos.containing(
                        player.getX() + Math.cos(angle) * 3, player.getY(), player.getZ() + Math.sin(angle) * 3));
        level.setBlock(stone, Content.GRAVESTONE.get().defaultBlockState(), 3);
        Direction toward = Direction.getApproximateNearest(
                player.getX() - stone.getX() - 0.5, 0, player.getZ() - stone.getZ() - 0.5);
        BlockPos signPos = surface(level, stone.relative(toward));
        level.setBlock(
                signPos,
                Blocks.OAK_SIGN
                        .defaultBlockState()
                        .setValue(StandingSignBlock.ROTATION, RotationSegment.convertToSegment(toward)),
                3);
        if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            List<Component> lines = List.of(
                    Component.translatable("sign.restless.requiem.rests"),
                    Component.literal(story.name()),
                    type,
                    Component.translatable("sign.restless.requiem.letgo"));
            sign.setText(new SignText(lines, lines, DyeColor.BLACK, false), SignTextSlot.FRONT);
            sign.setWaxed(true);
        }
        data.putGrave(spirit.uuid(), null);
        Journal.note(player, Notebook.Kind.LAID, spirit, story.name(), type);
        player.sendSystemMessage(Component.translatable("message.restless.requiem_laid", story.name(), type));
    }

    private static BlockPos surface(ServerLevel level, BlockPos near) {
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near);
    }
}
