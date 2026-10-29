package dev.keelfy.restless.haunt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.keelfy.restless.Restless;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/** Per-world state of player-bound spirits, keyed by player UUID; each spirit keeps its own timer. */
public final class HauntData extends SavedData {
    /**
     * One spirit. Saves from before identity existed carry only {@code spirit} and {@code nextEventAt}: they get a fresh uuid and
     * seed, born at tick 0 with no anger, and keep them once the next roll writes them back.
     *
     * @param seed rolls everything else about the spirit that stays fixed for its life
     * @param bornAt overworld game time the spirit appeared; its age is how mature it is
     * @param anger 0..1
     * @param nextEventAt overworld game time of the next roll
     */
    public record Haunting(UUID uuid, String spirit, long seed, long bornAt, double anger, long nextEventAt) {
        static final Codec<Haunting> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.optionalFieldOf("uuid").forGetter(h -> Optional.of(h.uuid)),
            Codec.STRING.fieldOf("spirit").forGetter(Haunting::spirit),
            Codec.LONG.optionalFieldOf("seed").forGetter(h -> Optional.of(h.seed)),
            Codec.LONG.optionalFieldOf("born_at", 0L).forGetter(Haunting::bornAt),
            Codec.DOUBLE.optionalFieldOf("anger", 0.0).forGetter(Haunting::anger),
            Codec.LONG.fieldOf("next_event_at").forGetter(Haunting::nextEventAt)
        ).apply(i, (uuid, spirit, seed, bornAt, anger, next) -> new Haunting(
            uuid.orElseGet(UUID::randomUUID), spirit, seed.orElseGet(() -> ThreadLocalRandom.current().nextLong()), bornAt, anger, next)));

        public static Haunting born(String spirit, long now, RandomSource random) {
            return new Haunting(UUID.randomUUID(), spirit, random.nextLong(), now, 0.0, now);
        }

        public Haunting rescheduled(long nextEventAt) {
            return new Haunting(uuid, spirit, seed, bornAt, anger, nextEventAt);
        }
    }

    private static final Codec<HauntData> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Haunting.CODEC.listOf())
        .xmap(HauntData::new, data -> data.hauntings);

    // No vanilla data fixer for mod data; NeoForge skips fixing when this is null.
    private static final SavedDataType<HauntData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Restless.MOD_ID, "hauntings"), HauntData::new, CODEC, null);

    private final Map<UUID, List<Haunting>> hauntings;

    private HauntData() {
        this(Map.of());
    }

    private HauntData(Map<UUID, List<Haunting>> hauntings) {
        this.hauntings = new HashMap<>(hauntings);
    }

    public static HauntData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    /** @return null for a player never seen before; an empty list means every spirit was removed */
    public @Nullable List<Haunting> get(UUID player) {
        return hauntings.get(player);
    }

    public void put(UUID player, List<Haunting> spirits) {
        hauntings.put(player, List.copyOf(spirits));
        setDirty();
    }
}
