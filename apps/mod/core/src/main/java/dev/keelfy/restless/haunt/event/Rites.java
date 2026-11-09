package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Altar;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.Divination;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.Faults;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntEvents;
import dev.keelfy.restless.haunt.Journal;
import dev.keelfy.restless.haunt.Location;
import dev.keelfy.restless.haunt.Notebook;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Sense;
import dev.keelfy.restless.spirit.Clue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The mod's own things misbehaving: the wards, the divination tools, the gravestone, the journal and the altar. Each
 * event acts on a block or tool the player has put there, and puts back what it moved.
 */
final class Rites {
    private static final Identifier DREAM = Identifier.parse("minecraft:dream");
    /** The block event id {@code DreamcatcherEntity} reads as a push; its data packs two components biased by 7. */
    private static final int TOUCH_EVENT = 1;

    private Rites() {}

    static List<HauntEvent> all() {
        return List.of(
                // 522: a scarecrow within reach takes one step toward the player; stepped back later
                SimpleEvent.light(
                        "restless:scarecrow_closer",
                        EventTag.BLOCKS,
                        player -> closer(player) != null,
                        Rites::scarecrowCloser),
                // 523: the planchette spells a short greeting with nobody asking
                SimpleEvent.light(
                        "restless:ouija_hi",
                        EventTag.PRESENCE,
                        player -> Nearby.any(player, "restless:ouija_hi", s -> s.is(Content.OUIJA_BOARD.get())),
                        Rites::ouijaHi),
                // 524: a word from a spirit's voice pool out of the spirit box, unasked
                SimpleEvent.light(
                        "restless:spirit_box_word",
                        EventTag.SOUND,
                        player -> Nearby.any(player, "restless:spirit_box_word", s -> s.is(Content.SPIRIT_BOX.get())),
                        Rites::spiritBoxWord),
                // 525: one cell of a salt line vanishes, the one nearest the spirit; it is back later
                SimpleEvent.harmful(
                        "restless:salt_gap", EventTag.BLOCKS, player -> gap(player) != null, Rites::saltGap),
                // 528: a gravestone nearby reads the player's own name
                SimpleEvent.light(
                        "restless:grave_name",
                        EventTag.PRESENCE,
                        player -> Nearby.any(player, "restless:grave_name", s -> s.is(Content.GRAVESTONE.get())),
                        Rites::graveName),
                // 530: one journal note is gone from the book until it comes back
                SimpleEvent.light("restless:journal_page_torn", EventTag.ITEMS, Rites::tearable, Rites::tearPage),
                // 531: a dreamcatcher swings hard, then the dream comes anyway
                SimpleEvent.light(
                        "restless:dreamcatcher_fails",
                        EventTag.AFFLICTION,
                        player ->
                                Nearby.any(player, "restless:dreamcatcher_fails", s -> s.is(Content.DREAMCATCHER.get()))
                                        && dream(player) != null,
                        Rites::dreamcatcherFails),
                // 532: a hallowed lantern burns with soul fire while a spirit is about
                SimpleEvent.light(
                        "restless:lantern_soul_fire",
                        EventTag.LIGHTS,
                        player -> Nearby.any(
                                        player, "restless:lantern_soul_fire", s -> s.is(Content.HALLOWED_LANTERN.get()))
                                && !Sense.of(
                                                player,
                                                HauntData.get(player.level().getServer()))
                                        .isEmpty(),
                        Rites::soulFire),
                // 533: a door charm drops off its hook as an item
                SimpleEvent.harmful(
                        "restless:charm_falls",
                        EventTag.BLOCKS,
                        player -> Nearby.any(player, "restless:charm_falls", s -> s.is(Content.DOOR_CHARM.get())),
                        Rites::charmFalls),
                // 536: at night the candles around an altar light one after another
                SimpleEvent.light(
                        "restless:altar_midnight",
                        EventTag.LIGHTS,
                        player -> Where.overworld(player)
                                && player.level().isDarkOutside()
                                && !unlitAround(player).isEmpty(),
                        Rites::altarMidnight));
    }

    // 522

    private record Step(BlockPos from, BlockPos to) {}

    /** A scarecrow far enough away to come closer, with a free cell on a floor between it and the player. */
    private static @Nullable Step closer(ServerPlayer player) {
        ServerLevel level = player.level();
        Config.Section config = Config.event("restless:scarecrow_closer");
        List<BlockPos> stands = Nearby.all(player, "restless:scarecrow_closer", s -> s.is(Content.SCARECROW.get()));
        for (BlockPos from : stands) {
            Vec3 toPlayer = player.position().subtract(Vec3.atCenterOf(from)).multiply(1, 0, 1);
            if (toPlayer.length() < config.num("min_gap")) {
                continue;
            }
            BlockPos to = from.relative(Direction.getApproximateNearest(toPlayer));
            BlockPos floor = to.below();
            if (level.isEmptyBlock(to)
                    && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                    && level.getEntitiesOfClass(LivingEntity.class, new AABB(to))
                            .isEmpty()) {
                return new Step(from, to);
            }
        }
        return null;
    }

    private static void scarecrowCloser(ServerPlayer player) {
        Step step = closer(player);
        if (step == null) {
            return;
        }
        ServerLevel level = player.level();
        Config.Section config = Config.event("restless:scarecrow_closer");
        BlockState scarecrow = level.getBlockState(step.from());
        Restore.swap(level, step.to(), scarecrow, config.integer("restore_ticks"));
        Restore.swap(level, step.from(), Blocks.AIR.defaultBlockState(), config.integer("restore_ticks"));
        Config.Tone tone = config.tone("sound");
        level.playSound(
                null,
                step.to(),
                scarecrow.getSoundType().getPlaceSound(),
                SoundSource.BLOCKS,
                tone.volume(),
                tone.pitch());
    }

    // 523

    private static void ouijaHi(ServerPlayer player) {
        Config.Section ouija = Config.section("ouija");
        List<String> words = Config.event("restless:ouija_hi").texts("words");
        String text = words.get(player.getRandom().nextInt(words.size()));
        Scheduler.repeat(player, ouija.integer("delay_ticks"), ouija.integer("letter_ticks"), (p, n) -> {
            p.sendSystemMessage(Component.literal(text.substring(0, n + 1)), true);
            p.level()
                    .playSound(
                            null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.4f, 1.5f);
            return n + 1 < text.length();
        });
    }

    // 524

    private static void spiritBoxWord(ServerPlayer player) {
        Optional<Divination.Reading> reading = Divination.read(player, Clue.VOICE);
        Optional<Component> word = reading.map(r -> Divination.word(
                r.trait().words().get(r.random().nextInt(r.trait().words().size()))));
        Component answer = word.<Component>map(w -> Component.translatable("message.restless.spirit_box", w))
                .orElse(Component.translatable("message.restless.spirit_box_static"));
        Scheduler.later(player, Config.section("spirit_box").integer("delay_ticks"), p -> {
            p.sendSystemMessage(answer);
            word.ifPresent(w ->
                    Journal.note(p, Notebook.Kind.VOICE, reading.get().spirit().haunting(), w));
        });
    }

    // 525

    /** The salt cell nearest the spirit, when there is enough salt around to call it a line. */
    private static @Nullable BlockPos gap(ServerPlayer player) {
        List<BlockPos> salt = Nearby.all(player, "restless:salt_gap", s -> s.is(Content.SALT_LINE.get()));
        if (salt.size() < Config.event("restless:salt_gap").integer("min_cells")) {
            return null;
        }
        Vec3 spirit = Location.source(player.position());
        return salt.stream()
                .min(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(spirit)))
                .orElse(null);
    }

    private static void saltGap(ServerPlayer player) {
        BlockPos pos = gap(player);
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        Config.Section config = Config.event("restless:salt_gap");
        Restore.swap(level, pos, Blocks.AIR.defaultBlockState(), config.integer("restore_ticks"));
        level.sendParticles(
                ParticleTypes.WHITE_ASH, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 12, 0.2, 0.1, 0.2, 0.02);
        Config.Tone tone = config.tone("sound");
        level.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, tone.volume(), tone.pitch());
    }

    // 528

    private static void graveName(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "restless:grave_name", s -> s.is(Content.GRAVESTONE.get()));
        if (pos == null) {
            return;
        }
        List<String> lines = Config.event("restless:grave_name").texts("lines");
        player.sendSystemMessage(Component.literal(lines.get(player.getRandom().nextInt(lines.size()))
                .formatted(player.getName().getString())));
        ServerLevel level = player.level();
        level.sendParticles(
                ParticleTypes.WHITE_ASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 10, 0.3, 0.4, 0.3, 0.01);
        Config.Tone tone = Config.event("restless:grave_name").tone("sound");
        level.playSound(
                null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, tone.volume(), tone.pitch());
    }

    // 530

    private static boolean tearable(ServerPlayer player) {
        return holdsJournal(player)
                && HauntData.get(player.level().getServer())
                                .notebook(player.getUUID())
                                .notes()
                                .size()
                        >= Config.event("restless:journal_page_torn").integer("min_notes");
    }

    private static boolean holdsJournal(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(Content.JOURNAL.get())) {
                return true;
            }
        }
        return false;
    }

    /** The note is taken out of the notebook and put back where it was, whatever the player wrote in between. */
    private static void tearPage(ServerPlayer player) {
        ServerLevel level = player.level();
        HauntData data = HauntData.get(level.getServer());
        UUID owner = player.getUUID();
        Notebook book = data.notebook(owner);
        if (book.notes().isEmpty()) {
            return;
        }
        int index = player.getRandom().nextInt(book.notes().size());
        Notebook.Note torn = book.notes().get(index);
        List<Notebook.Note> rest = new ArrayList<>(book.notes());
        rest.remove(index);
        data.putNotebook(owner, book.withNotes(rest));
        // Not Restore.revert: the page comes back under hardcore too, it is the player's own progress.
        Restore.after(level, Config.event("restless:journal_page_torn").integer("restore_ticks"), force -> {
            Notebook now = data.notebook(owner);
            if (!now.notes().contains(torn)) {
                List<Notebook.Note> back = new ArrayList<>(now.notes());
                back.add(Math.min(index, back.size()), torn);
                data.putNotebook(owner, now.withNotes(back));
            }
            return true;
        });
        Config.Tone tone = Config.event("restless:journal_page_torn").tone("sound");
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.BOOK_PAGE_TURN,
                SoundSource.PLAYERS,
                tone.volume(),
                tone.pitch());
    }

    // 531

    private static @Nullable HauntEvent dream(ServerPlayer player) {
        return HauntEvents.byId(DREAM).filter(e -> Faults.canRun(e, player)).orElse(null);
    }

    private static void dreamcatcherFails(ServerPlayer player) {
        ServerLevel level = player.level();
        Config.Section config = Config.event("restless:dreamcatcher_fails");
        for (BlockPos pos : Nearby.all(player, "restless:dreamcatcher_fails", s -> s.is(Content.DREAMCATCHER.get()))) {
            BlockState state = level.getBlockState(pos);
            int push = 7 + (player.getRandom().nextBoolean() ? 7 : -7);
            int other = 7 + player.getRandom().nextInt(15) - 7;
            level.blockEvent(pos, state.getBlock(), TOUCH_EVENT, push | (other << 4));
            Config.Tone tone = config.tone("sound");
            level.playSound(
                    null, pos, state.getSoundType().getHitSound(), SoundSource.BLOCKS, tone.volume(), tone.pitch());
        }
        // The dream is the real one; it is checked again at its turn, because the player may have moved on.
        Scheduler.later(player, config.integer("delay_ticks"), p -> {
            HauntEvent dream = dream(p);
            if (dream != null) {
                dream.run(p);
            }
        });
    }

    // 532

    private static void soulFire(ServerPlayer player) {
        ServerLevel level = player.level();
        Config.Section config = Config.event("restless:lantern_soul_fire");
        Config.Tone tone = config.tone("sound");
        for (BlockPos pos :
                Nearby.all(player, "restless:lantern_soul_fire", s -> s.is(Content.HALLOWED_LANTERN.get()))) {
            BlockPos at = pos.immutable();
            BlockState from = level.getBlockState(at);
            BlockState to = Blocks.SOUL_LANTERN.withPropertiesOf(from);
            Restore.quiet(level, at, to);
            // Always put back, hardcore or not: the lantern is a ward, not scenery.
            Restore.after(level, config.integer("restore_ticks"), force -> {
                if (!force && !level.isLoaded(at)) {
                    return false;
                }
                if (level.getBlockState(at) == to) {
                    Restore.quiet(level, at, from);
                }
                return true;
            });
            level.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    at.getX() + 0.5,
                    at.getY() + 0.5,
                    at.getZ() + 0.5,
                    8,
                    0.15,
                    0.2,
                    0.15,
                    0.01);
            level.playSound(null, at, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, tone.volume(), tone.pitch());
        }
    }

    // 533

    private static void charmFalls(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "restless:charm_falls", s -> s.is(Content.DOOR_CHARM.get()));
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        level.destroyBlock(pos, false);
        level.addFreshEntity(new ItemEntity(
                level,
                pos.getX() + 0.5,
                pos.getY() + 0.3,
                pos.getZ() + 0.5,
                new ItemStack(Content.DOOR_CHARM_ITEM.get())));
    }

    // 536

    /** Unlit candles within the ritual's reach of any altar near the player. */
    private static List<BlockPos> unlitAround(ServerPlayer player) {
        ServerLevel level = player.level();
        int reach = Config.section("ritual").integer("candle_radius");
        List<BlockPos> candles = new ArrayList<>();
        for (BlockPos altar : Nearby.all(
                player,
                "restless:altar_midnight",
                s -> s.is(Content.ALTAR.get()) && s.getValue(Altar.PART) == Altar.Part.CENTER)) {
            candles.addAll(Nearby.all(level, altar, reach, CandleBlock::canLight));
        }
        return candles.stream().distinct().toList();
    }

    private static void altarMidnight(ServerPlayer player) {
        ServerLevel level = player.level();
        Config.Section config = Config.event("restless:altar_midnight");
        List<BlockPos> candles = new ArrayList<>(unlitAround(player));
        Config.Tone tone = config.tone("sound");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            if (candles.isEmpty()) {
                return false;
            }
            BlockPos pos = candles.remove(0);
            BlockState state = level.getBlockState(pos);
            if (CandleBlock.canLight(state)) {
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), Block.UPDATE_ALL_IMMEDIATE);
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, tone.volume(), tone.pitch());
            }
            return !candles.isEmpty();
        });
    }
}
