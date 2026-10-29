package dev.keelfy.restless.haunt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.keelfy.restless.Restless;
import dev.keelfy.restless.spirit.Traits;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Per-world state of spirits: personal ones keyed by player UUID, territorial ones tied to an anchor; each keeps its own timer. */
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
     */
    public record Haunting(UUID uuid, String spirit, long seed, List<String> traits, long bornAt, double anger, long nextEventAt) {
        static final Codec<Haunting> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.optionalFieldOf("uuid").forGetter(h -> Optional.of(h.uuid)),
            Codec.STRING.fieldOf("spirit").forGetter(Haunting::spirit),
            Codec.LONG.optionalFieldOf("seed").forGetter(h -> Optional.of(h.seed)),
            Codec.STRING.listOf().optionalFieldOf("traits").forGetter(h -> Optional.of(h.traits)),
            Codec.LONG.optionalFieldOf("born_at", 0L).forGetter(Haunting::bornAt),
            Codec.DOUBLE.optionalFieldOf("anger", 0.0).forGetter(Haunting::anger),
            Codec.LONG.fieldOf("next_event_at").forGetter(Haunting::nextEventAt)
        ).apply(i, (uuid, spirit, seed, traits, bornAt, anger, next) -> {
            long rolled = seed.orElseGet(() -> ThreadLocalRandom.current().nextLong());
            return new Haunting(uuid.orElseGet(UUID::randomUUID), spirit, rolled, traits.orElseGet(() -> Traits.roll(rolled)), bornAt, anger, next);
        }));

        public static Haunting born(String spirit, long now, RandomSource random) {
            long seed = random.nextLong();
            return new Haunting(UUID.randomUUID(), spirit, seed, Traits.roll(seed), now, 0.0, now);
        }

        public Haunting rescheduled(long nextEventAt) {
            return new Haunting(uuid, spirit, seed, traits, bornAt, anger, nextEventAt);
        }

        public Haunting angered(double by) {
            return new Haunting(uuid, spirit, seed, traits, bornAt, Math.min(1.0, anger + by), nextEventAt);
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

    private static final Codec<Map<UUID, List<Haunting>>> PERSONAL = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Haunting.CODEC.listOf());

    // Saves from before territorial spirits are the bare player map; "players" is required so that map cannot parse as the new shape.
    private static final Codec<HauntData> CODEC = Codec.withAlternative(
        RecordCodecBuilder.create(i -> i.group(
            PERSONAL.fieldOf("players").forGetter(data -> data.hauntings),
            Territorial.CODEC.listOf().fieldOf("territorial").forGetter(data -> data.territorial)
        ).apply(i, HauntData::new)),
        PERSONAL.xmap(players -> new HauntData(players, List.of()), data -> data.hauntings));

    // No vanilla data fixer for mod data; NeoForge skips fixing when this is null.
    private static final SavedDataType<HauntData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Restless.MOD_ID, "hauntings"), HauntData::new, CODEC, null);

    private final Map<UUID, List<Haunting>> hauntings;
    private List<Territorial> territorial;

    private HauntData() {
        this(Map.of(), List.of());
    }

    private HauntData(Map<UUID, List<Haunting>> hauntings, List<Territorial> territorial) {
        this.hauntings = new HashMap<>(hauntings);
        this.territorial = List.copyOf(territorial);
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
}
