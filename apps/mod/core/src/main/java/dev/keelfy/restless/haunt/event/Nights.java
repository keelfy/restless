package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.event.kit.Figure;
import dev.keelfy.restless.haunt.event.kit.Nearby;
import dev.keelfy.restless.haunt.event.kit.Place;
import dev.keelfy.restless.haunt.event.kit.SimpleEvent;
import dev.keelfy.restless.haunt.harm.Band;
import dev.keelfy.restless.haunt.recall.Kills;
import dev.keelfy.restless.haunt.recall.Said;
import dev.keelfy.restless.haunt.recall.Wakes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** What the mara does to a sleeper on waking, and what the revenant makes of the player's kills. */
final class Nights {
    private Nights() {}

    static List<HauntEvent> all() {
        return List.of(
                // mara: a dream written down, with the day in it
                SimpleEvent.light(
                        "mara_dream",
                        EventTag.AFFLICTION,
                        player -> woke(player, "mara_dream")
                                && player.getInventory().getFreeSlot() != -1
                                && Said.left(player, "mara_dream", usable(player)),
                        Nights::dream),
                // mara: the night's sleep does not count for the phantoms' clock
                SimpleEvent.harmful(
                        "mara_stolen_rest",
                        EventTag.AFFLICTION,
                        player -> woke(player, "mara_stolen_rest")
                                && player.level().getDifficulty() != Difficulty.PEACEFUL
                                && rest(player)
                                        < Config.event("mara_stolen_rest").integer("rest_ticks"),
                        player -> player.getStats()
                                .setValue(
                                        player,
                                        Stats.CUSTOM.get(Stats.TIME_SINCE_REST),
                                        Config.event("mara_stolen_rest").integer("rest_ticks"))),
                // mara: something heavy gets off the bed, and the player feels it
                SimpleEvent.harmful(
                        "mara_weight", EventTag.AFFLICTION, player -> woke(player, "mara_weight"), Nights::weigh),
                // revenant: the day's kills written down
                SimpleEvent.light(
                        "revenant_ledger", EventTag.ITEMS, player -> ledgerAt(player) != null, Nights::ledger),
                // revenant: a hostile mob wears the name of what the player killed
                SimpleEvent.harmful(
                        "revenant_tally",
                        EventTag.MOBS,
                        player -> player.level().getDifficulty() != Difficulty.PEACEFUL
                                && !player.level().isBrightOutside()
                                && !Kills.animals(player).isEmpty()
                                && Place.spawnSpot(
                                                player,
                                                Config.event("revenant_tally").integer("min_distance"),
                                                Config.event("revenant_tally").integer("radius"))
                                        != null,
                        Nights::tally),
                // revenant: a skeleton climbs out of the ground where the player last killed
                SimpleEvent.harmful(
                        "revenant_rises",
                        EventTag.MOBS,
                        player -> player.level().getDifficulty() != Difficulty.PEACEFUL && grave(player) != null,
                        Nights::rise));
    }

    private static boolean woke(ServerPlayer player, String event) {
        return Wakes.within(player, Config.event(event).integer("window_ticks"));
    }

    private static int rest(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
    }

    /** Fills {@code {name}} marks in the text with components. */
    static MutableComponent fill(String template, Map<String, Component> values) {
        MutableComponent out = Component.empty();
        int at = 0;
        while (at < template.length()) {
            int open = template.indexOf('{', at);
            int close = open < 0 ? -1 : template.indexOf('}', open);
            if (close < 0) {
                out.append(template.substring(at));
                break;
            }
            out.append(template.substring(at, open));
            Component value = values.get(template.substring(open + 1, close));
            out.append(value == null ? Component.literal(template.substring(open, close + 1)) : value);
            at = close + 1;
        }
        return out;
    }

    private static ItemStack book(String title, String author, List<Component> pages) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(
                DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(
                        Filterable.passThrough(title),
                        author,
                        0,
                        pages.stream().map(Filterable::passThrough).toList(),
                        true));
        return book;
    }

    // ---- mara

    private static Component biome(ServerPlayer player) {
        return player.level()
                .getBiome(player.blockPosition())
                .unwrapKey()
                .<Component>map(key ->
                        Component.translatable("biome." + key.identifier().getNamespace() + "."
                                + key.identifier().getPath()))
                .orElse(Component.literal("?"));
    }

    /** Dreams that can be told: one with a {@code {mob}} needs a last kill. */
    private static List<String> usable(ServerPlayer player) {
        boolean killed = Kills.last(player) != null;
        return Config.event("mara_dream").texts("dreams").stream()
                .filter(dream -> killed || !dream.contains("{mob}"))
                .toList();
    }

    private static void dream(ServerPlayer player) {
        String dream = Said.pick(player, "mara_dream", usable(player));
        if (dream == null || player.getInventory().getFreeSlot() == -1) {
            return;
        }
        Kills.Last last = Kills.last(player);
        Map<String, Component> values = last == null
                ? Map.of("biome", biome(player))
                : Map.of("biome", biome(player), "mob", last.type().getDescription());
        player.getInventory()
                .add(book(
                        Config.event("mara_dream").text("title"),
                        Figure.nickname(player),
                        List.of(fill(dream, values))));
    }

    private static void weigh(ServerPlayer player) {
        Config.Section config = Config.event("mara_weight");
        int ticks = config.integer("seconds") * 20;
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, ticks, 0));
        Config.Tone tone = config.tone("sound");
        ServerLevel level = player.level();
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.RAVAGER_STEP,
                SoundSource.HOSTILE,
                tone.volume(),
                tone.pitch());
        level.playSound(
                null, player.blockPosition(), SoundEvents.WOOL_STEP, SoundSource.HOSTILE, tone.volume(), tone.pitch());
    }

    // ---- revenant

    private static int killed(ServerPlayer player) {
        return Kills.animals(player).values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    private static @Nullable BlockPos ledgerAt(ServerPlayer player) {
        return killed(player) >= Config.event("revenant_ledger").integer("min_kills")
                ? Nearby.random(player, "revenant_ledger", s -> s.is(Blocks.LECTERN))
                : null;
    }

    private static void ledger(ServerPlayer player) {
        BlockPos pos = ledgerAt(player);
        ServerLevel level = player.level();
        if (pos == null || !(level.getBlockEntity(pos) instanceof LecternBlockEntity lectern)) {
            return;
        }
        Config.Section config = Config.event("revenant_ledger");
        List<Component> lines = new ArrayList<>();
        Kills.animals(player)
                .forEach((type, count) -> lines.add(
                        Component.empty().append(type.getDescription()).append(": " + count + "\n")));
        List<Component> pages = new ArrayList<>();
        int per = config.integer("lines_per_page");
        for (int i = 0; i < lines.size(); i += per) {
            MutableComponent page = Component.empty();
            lines.subList(i, Math.min(lines.size(), i + per)).forEach(page::append);
            pages.add(page);
        }
        ItemStack ledger = book(config.text("title"), Figure.nickname(player), pages);
        BlockState state = level.getBlockState(pos);
        boolean had = state.getValue(LecternBlock.HAS_BOOK);
        ItemStack original = lectern.getBook().copy();
        level.setBlock(pos, state.setValue(LecternBlock.HAS_BOOK, true), 3);
        lectern.setBook(ledger);
        level.playSound(null, pos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0F, 1.0F);
        Restore.revert(level, config.integer("restore_ticks"), force -> {
            if (!force && !level.isLoaded(pos)) {
                return false;
            }
            if (level.getBlockEntity(pos) instanceof LecternBlockEntity now
                    && (now.getBook().isEmpty() || ItemStack.matches(now.getBook(), ledger))) {
                now.setBook(original);
                BlockState current = level.getBlockState(pos);
                if (current.is(Blocks.LECTERN)) {
                    level.setBlock(pos, current.setValue(LecternBlock.HAS_BOOK, had), 3);
                }
            }
            return true;
        });
    }

    private static void tally(ServerPlayer player) {
        Map<EntityType<?>, Integer> kills = Kills.animals(player);
        Config.Section config = Config.event("revenant_tally");
        BlockPos spot = Place.spawnSpot(player, config.integer("min_distance"), config.integer("radius"));
        var most = kills.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        if (spot == null || most == null) {
            return;
        }
        Mob mob = EntityTypes.ZOMBIE.spawn(player.level(), spot, EntitySpawnReason.EVENT);
        if (mob == null) {
            return;
        }
        Band.THREAT.mark(mob);
        mob.setCustomName(
                Component.empty().append(most.getKey().getDescription()).append(" " + most.getValue()));
        mob.setCustomNameVisible(true);
        mob.setTarget(player);
    }

    /** Where the player last killed, if it was near and recent and there is ground to climb out of. */
    private static @Nullable BlockPos grave(ServerPlayer player) {
        Kills.Last last = Kills.last(player);
        Config.Section config = Config.event("revenant_rises");
        ServerLevel level = player.level();
        if (last == null
                || last.dimension() != level.dimension()
                || level.getGameTime() - last.at() > config.integer("max_age_ticks")
                || Vec3.atCenterOf(last.pos()).distanceTo(player.position()) > config.num("radius")
                || !level.isLoaded(last.pos())) {
            return null;
        }
        return Place.standing(level, last.pos());
    }

    private static void rise(ServerPlayer player) {
        BlockPos at = grave(player);
        Kills.Last last = Kills.last(player);
        if (at == null || last == null) {
            return;
        }
        ServerLevel level = player.level();
        boolean horse = last.type() == EntityTypes.HORSE
                || last.type() == EntityTypes.DONKEY
                || last.type() == EntityTypes.MULE;
        Mob mob = (horse ? EntityTypes.SKELETON_HORSE : EntityTypes.SKELETON).spawn(level, at, EntitySpawnReason.EVENT);
        if (mob == null) {
            return;
        }
        Band.THREAT.mark(mob);
        mob.setTarget(player);
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                at.getX() + 0.5,
                at.getY() + 0.2,
                at.getZ() + 0.5,
                30,
                0.4,
                0.2,
                0.4,
                0.1);
        level.playSound(null, at, SoundEvents.GRAVEL_BREAK, SoundSource.HOSTILE, 1.0F, 0.6F);
    }
}
