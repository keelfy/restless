package dev.keelfy.restless.haunt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.keelfy.restless.Restless;
import dev.keelfy.restless.spirit.Traits;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Per-world state of spirits: personal ones keyed by player UUID, territorial ones tied to an anchor; each keeps its
 * own timer. Also what belongs to them for good: their graves and blighted land, and each player's sanity and journal.
 */
public final class HauntData extends SavedData {
    /**
     * One spirit. Saves from before identity existed carry only {@code spirit} and {@code nextEventAt}: they get a
     * fresh uuid and seed (and the traits that seed rolls), born at tick 0 with no anger, and keep them once the next
     * roll writes them back.
     *
     * @param seed rolls everything else about the spirit that stays fixed for its life
     * @param traits ids from the {@code traits} config, fixed at birth
     * @param bornAt overworld game time the spirit appeared; its age is how mature it is
     * @param anger 0..1
     * @param nextEventAt overworld game time of the next roll
     * @param evolved grown up after being left alone long enough; it never grows back down
     * @param notice events it still runs before the journal gets a line about an unknown spirit near the player; the
     *     event that makes the line is the last one counted, 0 when no line is due
     * @param waryUntil overworld game time until which it refuses an altar, after a wrong offering; 0 when it never was
     */
    public record Haunting(
            UUID uuid,
            String spirit,
            long seed,
            List<String> traits,
            long bornAt,
            double anger,
            long nextEventAt,
            boolean evolved,
            int notice,
            long waryUntil) {
        static final Codec<Haunting> CODEC = RecordCodecBuilder.create(i -> i.group(
                        UUIDUtil.CODEC.optionalFieldOf("uuid").forGetter(h -> Optional.of(h.uuid)),
                        Codec.STRING.fieldOf("spirit").forGetter(Haunting::spirit),
                        Codec.LONG.optionalFieldOf("seed").forGetter(h -> Optional.of(h.seed)),
                        Codec.STRING.listOf().optionalFieldOf("traits").forGetter(h -> Optional.of(h.traits)),
                        Codec.LONG.optionalFieldOf("born_at", 0L).forGetter(Haunting::bornAt),
                        Codec.DOUBLE.optionalFieldOf("anger", 0.0).forGetter(Haunting::anger),
                        Codec.LONG.fieldOf("next_event_at").forGetter(Haunting::nextEventAt),
                        Codec.BOOL.optionalFieldOf("evolved", false).forGetter(Haunting::evolved),
                        Codec.INT.optionalFieldOf("notice", 0).forGetter(Haunting::notice),
                        Codec.LONG.optionalFieldOf("wary_until", 0L).forGetter(Haunting::waryUntil))
                .apply(i, (uuid, spirit, seed, traits, bornAt, anger, next, evolved, notice, wary) -> {
                    long rolled =
                            seed.orElseGet(() -> ThreadLocalRandom.current().nextLong());
                    return new Haunting(
                            uuid.orElseGet(UUID::randomUUID),
                            spirit,
                            rolled,
                            traits.orElseGet(() -> Traits.roll(rolled)),
                            bornAt,
                            anger,
                            next,
                            evolved,
                            notice,
                            wary);
                }));

        public static Haunting born(String spirit, long now, RandomSource random) {
            long seed = random.nextLong();
            return new Haunting(UUID.randomUUID(), spirit, seed, Traits.roll(seed), now, 0.0, now, false, 0, 0L);
        }

        public Haunting rescheduled(long nextEventAt) {
            return new Haunting(uuid, spirit, seed, traits, bornAt, anger, nextEventAt, evolved, notice, waryUntil);
        }

        public Haunting angered(double by) {
            return new Haunting(
                    uuid,
                    spirit,
                    seed,
                    traits,
                    bornAt,
                    Math.max(0.0, Math.min(1.0, anger + by)),
                    nextEventAt,
                    evolved,
                    notice,
                    waryUntil);
        }

        /** Born {@code ticks} earlier than it was. */
        public Haunting aged(long ticks) {
            return new Haunting(
                    uuid, spirit, seed, traits, bornAt - ticks, anger, nextEventAt, evolved, notice, waryUntil);
        }

        public Haunting evolve() {
            return new Haunting(uuid, spirit, seed, traits, bornAt, anger, nextEventAt, true, notice, waryUntil);
        }

        public Haunting noticed(int events) {
            return new Haunting(uuid, spirit, seed, traits, bornAt, anger, nextEventAt, evolved, events, waryUntil);
        }

        /** Refusing every altar until this overworld game time. */
        public Haunting wary(long until) {
            return new Haunting(uuid, spirit, seed, traits, bornAt, anger, nextEventAt, evolved, notice, until);
        }
    }

    /**
     * A spirit bound to a place: it acts on players within {@code radius} blocks of {@code anchor} in
     * {@code dimension}.
     *
     * @param radius blocks
     */
    public record Territorial(Haunting haunting, ResourceKey<Level> dimension, BlockPos anchor, int radius) {
        static final Codec<Territorial> CODEC = RecordCodecBuilder.create(i -> i.group(
                        Haunting.CODEC.fieldOf("haunting").forGetter(Territorial::haunting),
                        Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Territorial::dimension),
                        BlockPos.CODEC.fieldOf("anchor").forGetter(Territorial::anchor),
                        Codec.INT.fieldOf("radius").forGetter(Territorial::radius))
                .apply(i, Territorial::new));

        public Territorial rescheduled(long nextEventAt) {
            return new Territorial(haunting.rescheduled(nextEventAt), dimension, anchor, radius);
        }

        public Territorial angered(double by) {
            return new Territorial(haunting.angered(by), dimension, anchor, radius);
        }

        /**
         * Whether {@code pos} is within {@code reach} blocks of the anchor, but never beyond the spirit's own radius.
         */
        public boolean covers(BlockPos pos, int reach) {
            long limit = Math.min(reach, radius);
            return anchor.distSqr(pos) <= limit * limit;
        }

        public boolean reaches(Entity entity) {
            return entity.level().dimension().equals(dimension)
                    && entity.distanceToSqr(Vec3.atCenterOf(anchor)) <= (double) radius * radius;
        }
    }

    /**
     * Where a spirit's grave stands, always in the overworld; {@code pos} is only a column until the grave is first
     * built.
     *
     * @param built whether the block was ever placed; a built grave that has gone is put back where the ground now is
     */
    public record Grave(BlockPos pos, boolean built) {
        static final Codec<Grave> CODEC = RecordCodecBuilder.create(i -> i.group(
                        BlockPos.CODEC.fieldOf("pos").forGetter(Grave::pos),
                        Codec.BOOL.fieldOf("built").forGetter(Grave::built))
                .apply(i, Grave::new));
    }

    /**
     * The land a territorial spirit has fouled: each spot keeps what stood there before and each biome column the
     * biomes it had, so both can be put back once the spirit is gone, even after the spirit's own record is.
     */
    public record Blight(ResourceKey<Level> dimension, List<Spot> spots, List<Column> columns) {
        static final Codec<Blight> CODEC = RecordCodecBuilder.create(i -> i.group(
                        Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Blight::dimension),
                        Spot.CODEC.listOf().fieldOf("spots").forGetter(Blight::spots),
                        Column.CODEC
                                .listOf()
                                .optionalFieldOf("columns", List.of())
                                .forGetter(Blight::columns))
                .apply(i, Blight::new));

        boolean empty() {
            return spots.isEmpty() && columns.isEmpty();
        }

        public record Spot(BlockPos pos, BlockState was) {
            static final Codec<Spot> CODEC = RecordCodecBuilder.create(i -> i.group(
                            BlockPos.CODEC.fieldOf("pos").forGetter(Spot::pos),
                            BlockState.CODEC.fieldOf("was").forGetter(Spot::was))
                    .apply(i, Spot::new));
        }

        /** A stack of biome cells in quart coordinates, from {@code y} upward, and the biome each held before. */
        public record Column(int x, int y, int z, List<ResourceKey<Biome>> was) {
            static final Codec<Column> CODEC = RecordCodecBuilder.create(i -> i.group(
                            Codec.INT.fieldOf("x").forGetter(Column::x),
                            Codec.INT.fieldOf("y").forGetter(Column::y),
                            Codec.INT.fieldOf("z").forGetter(Column::z),
                            ResourceKey.codec(Registries.BIOME)
                                    .listOf()
                                    .fieldOf("was")
                                    .forGetter(Column::was))
                    .apply(i, Column::new));
        }
    }

    /**
     * What one speaker (a spirit, or the player when no spirit is behind the event) has said through one text event:
     * the variants used since it last rested, and the overworld game time it rests until. See {@link Said}.
     */
    public record Spoken(List<String> used, long restUntil) {
        static final Spoken NONE = new Spoken(List.of(), 0L);
        static final Codec<Spoken> CODEC = RecordCodecBuilder.create(i -> i.group(
                        Codec.STRING.listOf().fieldOf("used").forGetter(Spoken::used),
                        Codec.LONG.optionalFieldOf("rest_until", 0L).forGetter(Spoken::restUntil))
                .apply(i, Spoken::new));
    }

    private static final Codec<Menace> MENACE = RecordCodecBuilder.create(i -> i.group(
                    Codec.LONG.optionalFieldOf("last_lethal_at", -1L).forGetter(Menace::lastLethalAt),
                    Codec.BOOL.optionalFieldOf("warned", false).forGetter(Menace::warned),
                    Codec.LONG.optionalFieldOf("mercy_until", 0L).forGetter(Menace::mercyUntil))
            .apply(i, Menace::new));

    private static final Codec<Map<UUID, List<Haunting>>> PERSONAL =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Haunting.CODEC.listOf());

    // Saves from before territorial spirits are the bare player map; "players" is required so that map cannot parse as
    // the new shape.
    private static final Codec<HauntData> CODEC = Codec.withAlternative(
            RecordCodecBuilder.create(i -> i.group(
                            PERSONAL.fieldOf("players").forGetter(data -> data.hauntings),
                            Territorial.CODEC.listOf().fieldOf("territorial").forGetter(HauntData::territorial),
                            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Grave.CODEC)
                                    .optionalFieldOf("graves", Map.of())
                                    .forGetter(data -> data.graves),
                            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.DOUBLE)
                                    .optionalFieldOf("sanity", Map.of())
                                    .forGetter(data -> data.sanity),
                            // Entries from before the journal kept notes; read once into OLD notes and never written
                            // again.
                            Codec.unboundedMap(UUIDUtil.STRING_CODEC, ComponentSerialization.CODEC.listOf())
                                    .optionalFieldOf("journal", Map.of())
                                    .forGetter(data -> Map.of()),
                            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Notebook.CODEC)
                                    .optionalFieldOf("notebooks", Map.of())
                                    .forGetter(data -> data.notebooks),
                            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Blight.CODEC)
                                    .optionalFieldOf("blight", Map.of())
                                    .forGetter(data -> data.blight),
                            Codec.BOOL
                                    .optionalFieldOf("hardcore")
                                    .forGetter(data -> Optional.ofNullable(data.hardcore)),
                            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.unboundedMap(Codec.STRING, Spoken.CODEC))
                                    .optionalFieldOf("said", Map.of())
                                    .forGetter(data -> data.said),
                            Codec.unboundedMap(UUIDUtil.STRING_CODEC, MENACE)
                                    .optionalFieldOf("menace", Map.of())
                                    .forGetter(data -> data.menace),
                            Codec.unboundedMap(UUIDUtil.STRING_CODEC, ItemStack.CODEC)
                                    .optionalFieldOf("kept", Map.of())
                                    .forGetter(data -> data.kept))
                    .apply(
                            i,
                            (players,
                                    territorial,
                                    graves,
                                    sanity,
                                    journal,
                                    notebooks,
                                    blight,
                                    hardcore,
                                    said,
                                    menace,
                                    kept) -> {
                                HauntData data = new HauntData(players, territorial, graves, sanity, notebooks, blight);
                                journal.forEach((player, entries) -> {
                                    List<Notebook.Note> notes = new ArrayList<>();
                                    entries.forEach(e -> notes.add(new Notebook.Note(
                                            Notebook.Kind.OLD, Optional.empty(), Optional.empty(), List.of(e))));
                                    notes.addAll(data.notebook(player).notes());
                                    data.notebooks.put(
                                            player,
                                            new Notebook(
                                                    List.copyOf(notes),
                                                    data.notebook(player).chapters(),
                                                    data.notebook(player).pictures(),
                                                    data.notebook(player).filed()));
                                });
                                data.hardcore = hardcore.orElse(null);
                                said.forEach((speaker, events) -> data.said.put(speaker, new HashMap<>(events)));
                                data.menace.putAll(menace);
                                data.kept.putAll(kept);
                                return data;
                            })),
            PERSONAL.xmap(
                    players -> new HauntData(players, List.of(), Map.of(), Map.of(), Map.of(), Map.of()),
                    data -> data.hauntings));

    // No vanilla data fixer for mod data; NeoForge skips fixing when this is null.
    private static final SavedDataType<HauntData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Restless.MOD_ID, "hauntings"), HauntData::new, CODEC, null);

    private static final int CELL_SHIFT = 6;

    private final Map<UUID, List<Haunting>> hauntings;
    /** Which player each personal spirit belongs to. */
    private final Map<UUID, UUID> owners = new HashMap<>();
    /** Keyed by spirit, in saved order (the order /haunt info lists them). */
    private final Map<UUID, Territorial> territorial = new LinkedHashMap<>();
    /** The territorial spirits of {@link #territorial} as one list, rebuilt when it is asked for after a change. */
    private @Nullable List<Territorial> territorialList;
    /** Spirits by dimension and 64-block square of their anchor; an anchor never moves once placed. */
    private final Map<ResourceKey<Level>, Map<Long, List<UUID>>> cells = new HashMap<>();
    /** The largest radius any territorial spirit has had; it bounds how far from an anchor a spirit can reach. */
    private int widest;
    /** Keyed by spirit. */
    private final Map<UUID, Grave> graves;
    /** Keyed by player, 0..1; a player not in it is fully sane. */
    private final Map<UUID, Double> sanity;
    /** Keyed by player. */
    private final Map<UUID, Notebook> notebooks;
    /** Keyed by spirit; outlives the spirit until the land has healed. */
    private final Map<UUID, Blight> blight;
    /** Keyed by speaker, then by event id. */
    private final Map<UUID, Map<String, Spoken>> said = new HashMap<>();
    /** Keyed by player. */
    private final Map<UUID, Menace> menace = new HashMap<>();
    /** What a charm saved from a player's death, until the player is back. Keyed by player. */
    private final Map<UUID, ItemStack> kept = new HashMap<>();
    /**
     * Decided the first time anyone asks and never again, so changing the config later does not flip a world; null
     * until then.
     */
    private @Nullable Boolean hardcore;

    private HauntData() {
        this(Map.of(), List.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    private HauntData(
            Map<UUID, List<Haunting>> hauntings,
            List<Territorial> territorial,
            Map<UUID, Grave> graves,
            Map<UUID, Double> sanity,
            Map<UUID, Notebook> notebooks,
            Map<UUID, Blight> blight) {
        this.hauntings = new HashMap<>(hauntings);
        hauntings.forEach((player, spirits) -> spirits.forEach(h -> owners.put(h.uuid(), player)));
        territorial.forEach(this::index);
        this.graves = new HashMap<>(graves);
        this.sanity = new HashMap<>(sanity);
        this.notebooks = new HashMap<>(notebooks);
        this.blight = new HashMap<>(blight);
    }

    public static HauntData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    /** @return null for a player never seen before; an empty list means every spirit was removed */
    public @Nullable List<Haunting> get(UUID player) {
        return hauntings.get(player);
    }

    public Menace menace(UUID player) {
        return menace.getOrDefault(player, Menace.NONE);
    }

    public void putMenace(UUID player, Menace state) {
        menace.put(player, state);
        setDirty();
    }

    /** Saves a stack for a player who has just died; it is kept through a restart until {@link #takeKept}. */
    public void keep(UUID player, ItemStack stack) {
        kept.put(player, stack.copy());
        setDirty();
    }

    /** The stack kept for a player, which is no longer kept; empty if there is none. */
    public ItemStack takeKept(UUID player) {
        ItemStack stack = kept.remove(player);
        if (stack == null) {
            return ItemStack.EMPTY;
        }
        setDirty();
        return stack;
    }

    /** Every territorial spirit in saved order; a snapshot, so it can be walked while spirits change. */
    public List<Territorial> territorial() {
        if (territorialList == null) {
            territorialList = List.copyOf(territorial.values());
        }
        return territorialList;
    }

    /** Replaces every territorial spirit. */
    public void putTerritorial(List<Territorial> spirits) {
        Set<UUID> gone = new HashSet<>(territorial.keySet());
        territorial.clear();
        cells.clear();
        widest = 0;
        for (Territorial t : spirits) {
            gone.remove(t.haunting().uuid());
            index(t);
        }
        gone.forEach(said::remove);
        territorialList = null;
        setDirty();
    }

    /** Adds the spirit, or replaces the one with its uuid keeping its place in the saved order. */
    public void putTerritorial(Territorial spirit) {
        Territorial old = territorial.get(spirit.haunting().uuid());
        if (old != null
                && (!old.dimension().equals(spirit.dimension()) || !old.anchor().equals(spirit.anchor()))) {
            unindex(old);
            old = null;
        }
        if (old == null) {
            index(spirit);
        } else {
            territorial.put(spirit.haunting().uuid(), spirit);
        }
        territorialList = null;
        setDirty();
    }

    public void removeTerritorial(UUID spirit) {
        Territorial old = territorial.get(spirit);
        if (old != null) {
            unindex(old);
            said.remove(spirit);
            territorialList = null;
            setDirty();
        }
    }

    /** The territorial spirits whose anchor is within {@code distance} blocks of {@code pos} in {@code dimension}. */
    public List<Territorial> territorialWithin(ResourceKey<Level> dimension, BlockPos pos, int distance) {
        Map<Long, List<UUID>> grid = cells.get(dimension);
        if (grid == null) {
            return List.of();
        }
        List<Territorial> found = new ArrayList<>();
        long limit = (long) distance * distance;
        for (int x = (pos.getX() - distance) >> CELL_SHIFT; x <= (pos.getX() + distance) >> CELL_SHIFT; x++) {
            for (int z = (pos.getZ() - distance) >> CELL_SHIFT; z <= (pos.getZ() + distance) >> CELL_SHIFT; z++) {
                for (UUID id : grid.getOrDefault(pack(x, z), List.of())) {
                    Territorial t = territorial.get(id);
                    if (t.anchor().distSqr(pos) <= limit) {
                        found.add(t);
                    }
                }
            }
        }
        return found;
    }

    /** The territorial spirits whose radius holds the entity. */
    public List<Territorial> territorialReaching(Entity entity) {
        return territorialWithin(entity.level().dimension(), entity.blockPosition(), widest + 1).stream()
                .filter(t -> t.reaches(entity))
                .toList();
    }

    /** The territorial spirits that {@link Territorial#covers} {@code pos} at this reach. */
    public List<Territorial> territorialCovering(ResourceKey<Level> dimension, BlockPos pos, int reach) {
        return territorialWithin(dimension, pos, Math.min(reach, widest)).stream()
                .filter(t -> t.covers(pos, reach))
                .toList();
    }

    public void put(UUID player, List<Haunting> spirits) {
        List<Haunting> old = hauntings.put(player, List.copyOf(spirits));
        if (old != null) {
            old.forEach(h -> owners.remove(h.uuid()));
        }
        spirits.forEach(h -> owners.put(h.uuid(), player));
        if (old != null) {
            old.stream()
                    .map(Haunting::uuid)
                    .filter(id -> !owners.containsKey(id))
                    .forEach(said::remove);
        }
        setDirty();
    }

    /** Changes the spirit with this uuid, wherever it is saved; a null result removes it. */
    public void change(UUID spirit, Function<Haunting, @Nullable Haunting> how) {
        UUID player = owners.get(spirit);
        if (player != null) {
            List<Haunting> changed = new ArrayList<>();
            for (Haunting h : hauntings.get(player)) {
                Haunting kept = h.uuid().equals(spirit) ? how.apply(h) : h;
                if (kept != null) {
                    changed.add(kept);
                }
            }
            put(player, changed);
            return;
        }
        Territorial t = territorial.get(spirit);
        if (t != null) {
            Haunting kept = how.apply(t.haunting());
            if (kept == null) {
                removeTerritorial(spirit);
            } else {
                putTerritorial(new Territorial(kept, t.dimension(), t.anchor(), t.radius()));
            }
        }
    }

    /** The spirit with this uuid, personal or territorial. */
    public Optional<Haunting> find(UUID spirit) {
        UUID player = owners.get(spirit);
        if (player != null) {
            return hauntings.get(player).stream()
                    .filter(h -> h.uuid().equals(spirit))
                    .findFirst();
        }
        return Optional.ofNullable(territorial.get(spirit)).map(Territorial::haunting);
    }

    private void index(Territorial spirit) {
        territorial.put(spirit.haunting().uuid(), spirit);
        cells.computeIfAbsent(spirit.dimension(), k -> new HashMap<>())
                .computeIfAbsent(cell(spirit.anchor()), k -> new ArrayList<>())
                .add(spirit.haunting().uuid());
        widest = Math.max(widest, spirit.radius());
    }

    private void unindex(Territorial spirit) {
        territorial.remove(spirit.haunting().uuid());
        Map<Long, List<UUID>> grid = cells.get(spirit.dimension());
        if (grid != null) {
            grid.getOrDefault(cell(spirit.anchor()), new ArrayList<>())
                    .remove(spirit.haunting().uuid());
        }
    }

    private static long pack(int x, int z) {
        return (long) x << 32 | z & 0xFFFFFFFFL;
    }

    private static long cell(BlockPos pos) {
        return pack(pos.getX() >> CELL_SHIFT, pos.getZ() >> CELL_SHIFT);
    }

    public Map<UUID, Grave> graves() {
        return Collections.unmodifiableMap(graves);
    }

    /** A null grave removes it. */
    public void putGrave(UUID spirit, @Nullable Grave grave) {
        if (grave == null) {
            graves.remove(spirit);
        } else {
            graves.put(spirit, grave);
        }
        setDirty();
    }

    public Map<UUID, Blight> blight() {
        return Collections.unmodifiableMap(blight);
    }

    /** A null or empty blight removes it. */
    public void putBlight(UUID spirit, @Nullable Blight spread) {
        if (spread == null || spread.empty()) {
            blight.remove(spirit);
        } else {
            blight.put(
                    spirit, new Blight(spread.dimension(), List.copyOf(spread.spots()), List.copyOf(spread.columns())));
        }
        setDirty();
    }

    /**
     * Whether this world runs hardcore haunting; {@code initial} is asked once, the first time, and the answer is
     * saved.
     */
    public boolean hardcore(Supplier<Boolean> initial) {
        if (hardcore == null) {
            hardcore = initial.get();
            setDirty();
        }
        return hardcore;
    }

    public void putHardcore(boolean on) {
        hardcore = on;
        setDirty();
    }

    public Spoken spoken(UUID speaker, String event) {
        return said.getOrDefault(speaker, Map.of()).getOrDefault(event, Spoken.NONE);
    }

    public void putSpoken(UUID speaker, String event, Spoken spoken) {
        said.computeIfAbsent(speaker, k -> new HashMap<>())
                .put(event, new Spoken(List.copyOf(spoken.used()), spoken.restUntil()));
        setDirty();
    }

    public double sanity(UUID player) {
        return sanity.getOrDefault(player, 1.0);
    }

    public void putSanity(UUID player, double value) {
        sanity.put(player, value);
        setDirty();
    }

    public Notebook notebook(UUID player) {
        return notebooks.getOrDefault(player, Notebook.EMPTY);
    }

    public void putNotebook(UUID player, Notebook notebook) {
        notebooks.put(player, notebook);
        setDirty();
    }
}
