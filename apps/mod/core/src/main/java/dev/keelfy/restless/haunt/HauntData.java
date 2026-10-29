package dev.keelfy.restless.haunt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.keelfy.restless.Restless;
import dev.keelfy.restless.spirit.Traits;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Per-world state of spirits: personal ones keyed by player UUID, territorial ones tied to an anchor; each keeps its own timer.
 * Also what belongs to them for good: their graves and blighted land, and each player's sanity and journal.
 */
public final class HauntData extends SavedData {
    /**
     * One spirit. Saves from before identity existed carry only {@code spirit} and {@code nextEventAt}: they get a fresh uuid and
     * seed (and the traits that seed rolls), born at tick 0 with no anger, and keep them once the next roll writes them back.
     *
     * @param seed rolls everything else about the spirit that stays fixed for its life
     * @param traits ids from the {@code traits} config, fixed at birth
     * @param bornAt overworld game time the spirit appeared; its age is how mature it is
     * @param anger 0..1
     * @param nextEventAt overworld game time of the next roll
     * @param evolved grown up after being left alone long enough; it never grows back down
     */
    public record Haunting(UUID uuid, String spirit, long seed, List<String> traits, long bornAt, double anger, long nextEventAt, boolean evolved) {
        static final Codec<Haunting> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.optionalFieldOf("uuid").forGetter(h -> Optional.of(h.uuid)),
            Codec.STRING.fieldOf("spirit").forGetter(Haunting::spirit),
            Codec.LONG.optionalFieldOf("seed").forGetter(h -> Optional.of(h.seed)),
            Codec.STRING.listOf().optionalFieldOf("traits").forGetter(h -> Optional.of(h.traits)),
            Codec.LONG.optionalFieldOf("born_at", 0L).forGetter(Haunting::bornAt),
            Codec.DOUBLE.optionalFieldOf("anger", 0.0).forGetter(Haunting::anger),
            Codec.LONG.fieldOf("next_event_at").forGetter(Haunting::nextEventAt),
            Codec.BOOL.optionalFieldOf("evolved", false).forGetter(Haunting::evolved)
        ).apply(i, (uuid, spirit, seed, traits, bornAt, anger, next, evolved) -> {
            long rolled = seed.orElseGet(() -> ThreadLocalRandom.current().nextLong());
            return new Haunting(uuid.orElseGet(UUID::randomUUID), spirit, rolled, traits.orElseGet(() -> Traits.roll(rolled)), bornAt, anger, next, evolved);
        }));

        public static Haunting born(String spirit, long now, RandomSource random) {
            long seed = random.nextLong();
            return new Haunting(UUID.randomUUID(), spirit, seed, Traits.roll(seed), now, 0.0, now, false);
        }

        public Haunting rescheduled(long nextEventAt) {
            return new Haunting(uuid, spirit, seed, traits, bornAt, anger, nextEventAt, evolved);
        }

        public Haunting angered(double by) {
            return new Haunting(uuid, spirit, seed, traits, bornAt, Math.min(1.0, anger + by), nextEventAt, evolved);
        }

        /** Born {@code ticks} earlier than it was. */
        public Haunting aged(long ticks) {
            return new Haunting(uuid, spirit, seed, traits, bornAt - ticks, anger, nextEventAt, evolved);
        }

        public Haunting evolve() {
            return new Haunting(uuid, spirit, seed, traits, bornAt, anger, nextEventAt, true);
        }
    }

    /**
     * A spirit bound to a place: it acts on players within {@code radius} blocks of {@code anchor} in {@code dimension}.
     *
     * @param radius blocks
     */
    public record Territorial(Haunting haunting, ResourceKey<Level> dimension, BlockPos anchor, int radius) {
        static final Codec<Territorial> CODEC = RecordCodecBuilder.create(i -> i.group(
            Haunting.CODEC.fieldOf("haunting").forGetter(Territorial::haunting),
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Territorial::dimension),
            BlockPos.CODEC.fieldOf("anchor").forGetter(Territorial::anchor),
            Codec.INT.fieldOf("radius").forGetter(Territorial::radius)
        ).apply(i, Territorial::new));

        public Territorial rescheduled(long nextEventAt) {
            return new Territorial(haunting.rescheduled(nextEventAt), dimension, anchor, radius);
        }

        public Territorial angered(double by) {
            return new Territorial(haunting.angered(by), dimension, anchor, radius);
        }

        /** Whether {@code pos} is within {@code reach} blocks of the anchor, but never beyond the spirit's own radius. */
        public boolean covers(BlockPos pos, int reach) {
            long limit = Math.min(reach, radius);
            return anchor.distSqr(pos) <= limit * limit;
        }

        public boolean reaches(Entity entity) {
            return entity.level().dimension().equals(dimension) && entity.distanceToSqr(Vec3.atCenterOf(anchor)) <= (double) radius * radius;
        }
    }

    /**
     * Where a spirit's grave stands, always in the overworld; {@code pos} is only a column until the grave is first built.
     *
     * @param built whether the block was ever placed; a built grave that has gone is put back where the ground now is
     */
    public record Grave(BlockPos pos, boolean built) {
        static final Codec<Grave> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(Grave::pos),
            Codec.BOOL.fieldOf("built").forGetter(Grave::built)
        ).apply(i, Grave::new));
    }

    /**
     * The land a territorial spirit has fouled: each spot keeps what stood there before and each biome column the biomes it
     * had, so both can be put back once the spirit is gone, even after the spirit's own record is.
     */
    public record Blight(ResourceKey<Level> dimension, List<Spot> spots, List<Column> columns) {
        static final Codec<Blight> CODEC = RecordCodecBuilder.create(i -> i.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Blight::dimension),
            Spot.CODEC.listOf().fieldOf("spots").forGetter(Blight::spots),
            Column.CODEC.listOf().optionalFieldOf("columns", List.of()).forGetter(Blight::columns)
        ).apply(i, Blight::new));

        boolean empty() {
            return spots.isEmpty() && columns.isEmpty();
        }

        public record Spot(BlockPos pos, BlockState was) {
            static final Codec<Spot> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Spot::pos),
                BlockState.CODEC.fieldOf("was").forGetter(Spot::was)
            ).apply(i, Spot::new));
        }

        /** A stack of biome cells in quart coordinates, from {@code y} upward, and the biome each held before. */
        public record Column(int x, int y, int z, List<ResourceKey<Biome>> was) {
            static final Codec<Column> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Column::x),
                Codec.INT.fieldOf("y").forGetter(Column::y),
                Codec.INT.fieldOf("z").forGetter(Column::z),
                ResourceKey.codec(Registries.BIOME).listOf().fieldOf("was").forGetter(Column::was)
            ).apply(i, Column::new));
        }
    }

    private static final Codec<Map<UUID, List<Haunting>>> PERSONAL = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Haunting.CODEC.listOf());

    // Saves from before territorial spirits are the bare player map; "players" is required so that map cannot parse as the new shape.
    private static final Codec<HauntData> CODEC = Codec.withAlternative(
        RecordCodecBuilder.create(i -> i.group(
            PERSONAL.fieldOf("players").forGetter(data -> data.hauntings),
            Territorial.CODEC.listOf().fieldOf("territorial").forGetter(data -> data.territorial),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Grave.CODEC).optionalFieldOf("graves", Map.of()).forGetter(data -> data.graves),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.DOUBLE).optionalFieldOf("sanity", Map.of()).forGetter(data -> data.sanity),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, ComponentSerialization.CODEC.listOf()).optionalFieldOf("journal", Map.of()).forGetter(data -> data.journal),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Blight.CODEC).optionalFieldOf("blight", Map.of()).forGetter(data -> data.blight)
        ).apply(i, HauntData::new)),
        PERSONAL.xmap(players -> new HauntData(players, List.of(), Map.of(), Map.of(), Map.of(), Map.of()), data -> data.hauntings));

    // No vanilla data fixer for mod data; NeoForge skips fixing when this is null.
    private static final SavedDataType<HauntData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Restless.MOD_ID, "hauntings"), HauntData::new, CODEC, null);

    private final Map<UUID, List<Haunting>> hauntings;
    private List<Territorial> territorial;
    /** Keyed by spirit. */
    private final Map<UUID, Grave> graves;
    /** Keyed by player, 0..1; a player not in it is fully sane. */
    private final Map<UUID, Double> sanity;
    /** Keyed by player, oldest entry first. */
    private final Map<UUID, List<Component>> journal;
    /** Keyed by spirit; outlives the spirit until the land has healed. */
    private final Map<UUID, Blight> blight;

    private HauntData() {
        this(Map.of(), List.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    private HauntData(Map<UUID, List<Haunting>> hauntings, List<Territorial> territorial, Map<UUID, Grave> graves,
                      Map<UUID, Double> sanity, Map<UUID, List<Component>> journal, Map<UUID, Blight> blight) {
        this.hauntings = new HashMap<>(hauntings);
        this.territorial = List.copyOf(territorial);
        this.graves = new HashMap<>(graves);
        this.sanity = new HashMap<>(sanity);
        this.journal = new HashMap<>(journal);
        this.blight = new HashMap<>(blight);
    }

    public static HauntData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    /** @return null for a player never seen before; an empty list means every spirit was removed */
    public @Nullable List<Haunting> get(UUID player) {
        return hauntings.get(player);
    }

    public List<Territorial> territorial() {
        return territorial;
    }

    public void putTerritorial(List<Territorial> spirits) {
        territorial = List.copyOf(spirits);
        setDirty();
    }

    public void put(UUID player, List<Haunting> spirits) {
        hauntings.put(player, List.copyOf(spirits));
        setDirty();
    }

    /** Changes the spirit with this uuid, wherever it is saved; a null result removes it. */
    public void change(UUID spirit, Function<Haunting, @Nullable Haunting> how) {
        for (Map.Entry<UUID, List<Haunting>> entry : hauntings.entrySet()) {
            if (entry.getValue().stream().anyMatch(h -> h.uuid().equals(spirit))) {
                List<Haunting> changed = new ArrayList<>();
                for (Haunting h : entry.getValue()) {
                    Haunting kept = h.uuid().equals(spirit) ? how.apply(h) : h;
                    if (kept != null) {
                        changed.add(kept);
                    }
                }
                entry.setValue(List.copyOf(changed));
            }
        }
        List<Territorial> changed = new ArrayList<>();
        for (Territorial t : territorial) {
            Haunting kept = t.haunting().uuid().equals(spirit) ? how.apply(t.haunting()) : t.haunting();
            if (kept != null) {
                changed.add(new Territorial(kept, t.dimension(), t.anchor(), t.radius()));
            }
        }
        territorial = List.copyOf(changed);
        setDirty();
    }

    /** The spirit with this uuid, personal or territorial. */
    public Optional<Haunting> find(UUID spirit) {
        return hauntings.values().stream().flatMap(List::stream).filter(h -> h.uuid().equals(spirit)).findFirst()
            .or(() -> territorial.stream().map(Territorial::haunting).filter(h -> h.uuid().equals(spirit)).findFirst());
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
            blight.put(spirit, new Blight(spread.dimension(), List.copyOf(spread.spots()), List.copyOf(spread.columns())));
        }
        setDirty();
    }

    public double sanity(UUID player) {
        return sanity.getOrDefault(player, 1.0);
    }

    public void putSanity(UUID player, double value) {
        sanity.put(player, value);
        setDirty();
    }

    public List<Component> journal(UUID player) {
        return journal.getOrDefault(player, List.of());
    }

    /** @return false when the player already has this exact entry */
    public boolean write(UUID player, Component entry) {
        List<Component> entries = journal(player);
        if (entries.contains(entry)) {
            return false;
        }
        List<Component> grown = new ArrayList<>(entries);
        grown.add(entry);
        journal.put(player, List.copyOf(grown));
        setDirty();
        return true;
    }
}
