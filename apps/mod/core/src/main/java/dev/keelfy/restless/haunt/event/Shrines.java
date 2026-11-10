package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.divine.Sense;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.recall.Said;
import dev.keelfy.restless.spirit.Story;
import dev.keelfy.restless.state.HauntData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Things left behind by someone who is not there: a camp, a hut, a shrine, ash on the floor in a shape, and a house
 * with a light in it where no house stands. Built from {@link Templates} and put back by it.
 */
final class Shrines {
    private static final String CAMP = "abandoned_camp";
    private static final String HUT = "dirt_hut";
    private static final String SHRINE = "overnight_shrine";
    private static final String FAR_HOUSE = "far_house";
    private static final String ASH_SIGN = "restless:ash_sign";
    private static final int SIGN_LINES = 4;
    private static final int GLYPH_WIDTH = 3;
    private static final int GLYPH_HEIGHT = 5;

    /** A 3 by 5 letter, row after row, A to Z; a hash is ash. */
    private static final String[] GLYPHS = {
        ".#.#.#####.##.#", "##.#.###.#.###.", ".###..#..#...##", "##.#.##.##.###.",
        "####..##.#..###", "####..##.#..#..", ".###..#.##.#.##", "#.##.#####.##.#",
        "###.#..#..#.###", "..#..#..##.#.#.", "#.##.###.#.##.#", "#..#..#..#..###",
        "#.########.##.#", "####.##.##.##.#", "####.##.##.####", "####.#####..#..",
        "####.##.####..#", "####.###.#.##.#", ".###...#...###.", "###.#..#..#..#.",
        "#.##.##.##.####", "#.##.##.##.#.#.", "#.##.########.#", "#.##.#.#.#.##.#",
        "#.##.#.#..#..#.", "###..#.#.#..###"
    };

    private Shrines() {}

    static List<HauntEvent> all() {
        return List.of(
                // 375: a bed, a chest with a book in a spirit's voice and a campfire, nobody about; taken down later
                SimpleEvent.light(
                        CAMP,
                        EventTag.BLOCKS,
                        player -> Where.overworld(player)
                                && Said.left(player, CAMP, Config.event(CAMP).texts("pages"))
                                && camp(player) != null,
                        Shrines::buildCamp),
                // 381: a three-block dirt hut with a doorway, built since the player last looked
                SimpleEvent.light(
                        HUT,
                        EventTag.BLOCKS,
                        player -> Where.overworld(player) && hut(player) != null,
                        Shrines::buildHut),
                // 520: bone, skulls, candles and a sign
                SimpleEvent.light(
                        SHRINE,
                        EventTag.PRESENCE,
                        player -> Where.overworld(player)
                                && Said.left(player, SHRINE, signs(), Shrines::signKey)
                                && shrine(player) != null,
                        Shrines::buildShrine),
                // 521: at night, a house like the one the player is in stands far off with a lantern lit in it
                SimpleEvent.light(
                        FAR_HOUSE,
                        EventTag.LIGHTS,
                        player ->
                                Where.overworld(player) && player.level().isDarkOutside() && Household.indoors(player),
                        Shrines::copyHouse),
                // 526: ash laid on the floor ahead as a ring or as the first letter of the spirit's name
                SimpleEvent.light(
                        ASH_SIGN,
                        EventTag.PRESENCE,
                        player -> Household.indoors(player) && ash(player) != null,
                        Shrines::layAsh));
    }

    private static @Nullable String spiritName(ServerPlayer player) {
        List<Sense.Spirit> spirits =
                Sense.of(player, HauntData.get(player.level().getServer()));
        return spirits.isEmpty()
                ? null
                : Story.of(spirits.get(player.getRandom().nextInt(spirits.size()))
                                .haunting()
                                .seed())
                        .name();
    }

    // 375

    private static @Nullable List<Templates.Cell> camp(ServerPlayer player) {
        Config.Section config = Config.event(CAMP);
        return Templates.find(
                player, Templates.load(CAMP), config.integer("min_distance"), config.integer("max_distance"));
    }

    private static void buildCamp(ServerPlayer player) {
        Config.Section config = Config.event(CAMP);
        List<Templates.Cell> cells = camp(player);
        String page = Said.pick(player, CAMP, config.texts("pages"));
        if (cells == null || page == null) {
            return;
        }
        String spirit = spiritName(player);
        List<String> titles = config.texts("titles");
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(
                DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(
                        Filterable.passThrough(titles.get(player.getRandom().nextInt(titles.size()))),
                        spirit != null ? spirit : config.text("author"),
                        0,
                        List.of(Filterable.passThrough(Component.literal(page))),
                        true));
        Templates.put(
                player.level(),
                cells,
                Map.of("C", entity -> {
                    if (entity instanceof ChestBlockEntity chest) {
                        chest.setItem(0, book);
                    }
                }),
                config.integer("restore_ticks"));
    }

    // 381

    private static @Nullable List<Templates.Cell> hut(ServerPlayer player) {
        Config.Section config = Config.event(HUT);
        Templates.Template template = Templates.load(HUT);
        return Templates.find(player, template, config.integer("min_distance"), config.integer("max_distance"));
    }

    private static void buildHut(ServerPlayer player) {
        List<Templates.Cell> cells = hut(player);
        if (cells != null) {
            Templates.put(player.level(), cells, Map.of(), Config.event(HUT).integer("restore_ticks"));
        }
    }

    // 520

    private static List<List<String>> signs() {
        return Config.event(SHRINE).lines("signs");
    }

    private static String signKey(List<String> lines) {
        return String.join("/", lines);
    }

    private static @Nullable List<Templates.Cell> shrine(ServerPlayer player) {
        Config.Section config = Config.event(SHRINE);
        Templates.Template template = Templates.load(SHRINE);
        return Templates.find(player, template, config.integer("min_distance"), config.integer("max_distance"));
    }

    /**
     * The shrine as only this player's client is told of it: the server keeps open ground, so it can be walked through
     * and the first click on a block shows the real air. The real ground comes back after {@code restore_ticks}.
     */
    private static void buildShrine(ServerPlayer player) {
        Config.Section config = Config.event(SHRINE);
        List<Templates.Cell> cells = shrine(player);
        List<String> lines = Said.pick(player, SHRINE, signs(), Shrines::signKey);
        if (cells == null || lines == null) {
            return;
        }
        ServerLevel level = player.level();
        Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
        cells.forEach(cell -> shown.put(cell.pos().immutable(), cell.state()));
        Swaps.show(player, shown, config.integer("restore_ticks"));
        cells.stream().filter(cell -> cell.slot().equals("s")).forEach(cell -> {
            SignBlockEntity sign = new SignBlockEntity(cell.pos(), cell.state());
            sign.setLevel(level);
            SignText.Mutable text = sign.getText(SignTextSlot.FRONT).asMutable();
            int first = (SIGN_LINES - lines.size()) / 2;
            for (int i = 0; i < SIGN_LINES; i++) {
                text.setLine(i, Component.literal(i >= first && i - first < lines.size() ? lines.get(i - first) : ""));
            }
            SignText.CODEC
                    .encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), text.asImmutable())
                    .result()
                    .ifPresent(front ->
                            player.connection.send(ClientboundBlockEntityDataPacket.create(sign, (be, access) -> {
                                CompoundTag tag = be.getUpdateTag(access);
                                tag.put("front_text", front);
                                return tag;
                            })));
        });
    }
    // 521

    /**
     * The room round the player, as this player's client is told of it, stands again on open ground far off with a
     * lantern lit on its floor. The server never changes; the real blocks are sent again when the time is up.
     */
    private static void copyHouse(ServerPlayer player) {
        Config.Section config = Config.event(FAR_HOUSE);
        ServerLevel level = player.level();
        BlockPos base = player.blockPosition();
        BlockPos stand = null;
        for (int i = 0; i < config.integer("tries") && stand == null; i++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2;
            double distance = config.integer("min_distance")
                    + player.getRandom().nextDouble()
                            * (config.integer("max_distance") - config.integer("min_distance"));
            stand = Place.standing(
                    level,
                    BlockPos.containing(
                            base.getX() + Math.cos(angle) * distance,
                            base.getY(),
                            base.getZ() + Math.sin(angle) * distance));
        }
        if (stand == null) {
            return;
        }
        BlockPos shift = stand.subtract(base);
        int radius = config.integer("radius");
        Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
        for (BlockPos src : BlockPos.betweenClosed(
                base.offset(-radius, -1, -radius), base.offset(radius, config.integer("height"), radius))) {
            shown.put(src.offset(shift).immutable(), level.getBlockState(src));
        }
        shown.put(stand, Blocks.LANTERN.defaultBlockState());
        shown.forEach((pos, state) -> player.connection.send(new ClientboundBlockUpdatePacket(pos, state)));
        List<BlockPos> where = new ArrayList<>(shown.keySet());
        Scheduler.later(player, config.integer("ticks"), p -> {
            if (p.level() == level) {
                where.forEach(pos -> p.connection.send(new ClientboundBlockUpdatePacket(level, pos)));
            }
        });
    }

    // 526

    /** The cells of a ring or a letter on the floor ahead of the player that are bare floor; null when too few are. */
    private static @Nullable List<Templates.Cell> ash(ServerPlayer player) {
        Config.Section config = Config.event(ASH_SIGN);
        ServerLevel level = player.level();
        BlockPos centre = Place.standingAhead(player, config.num("distance"), 0.0, config.num("spread_degrees"));
        if (centre == null) {
            return null;
        }
        Direction forward = player.getDirection();
        Direction right = forward.getClockWise();
        String name = spiritName(player);
        char initial = name == null ? ' ' : Character.toUpperCase(name.charAt(0));
        List<BlockPos> shape = new ArrayList<>();
        if (initial >= 'A' && initial <= 'Z' && player.getRandom().nextDouble() < config.num("letter_chance")) {
            String glyph = GLYPHS[initial - 'A'];
            for (int i = 0; i < glyph.length(); i++) {
                if (glyph.charAt(i) == '#') {
                    shape.add(centre.relative(right, i % GLYPH_WIDTH - 1)
                            .relative(forward, GLYPH_HEIGHT / 2 - i / GLYPH_WIDTH));
                }
            }
        } else {
            int radius = config.integer("radius");
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(Math.hypot(dx, dz) - radius) <= 0.5) {
                        shape.add(centre.offset(dx, 0, dz));
                    }
                }
            }
        }
        BlockState ash = Content.ASH_LINE.get().defaultBlockState();
        List<Templates.Cell> cells = shape.stream()
                .filter(pos -> Place.floored(level, pos))
                .map(pos -> new Templates.Cell(pos, ash, "", false))
                .toList();
        return cells.size() >= shape.size() * config.num("min_share") ? cells : null;
    }

    private static void layAsh(ServerPlayer player) {
        List<Templates.Cell> cells = ash(player);
        if (cells != null) {
            Templates.put(
                    player.level(), cells, Map.of(), Config.event(ASH_SIGN).integer("restore_ticks"));
        }
    }
}
