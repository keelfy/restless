package dev.keelfy.restless.haunt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.keelfy.restless.Restless;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/** Per-world state of player-bound spirits, keyed by player UUID; each spirit keeps its own timer. */
public final class HauntData extends SavedData {
    /** @param nextEventAt overworld game time of the next roll */
    public record Haunting(String spirit, long nextEventAt) {
        static final Codec<Haunting> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("spirit").forGetter(Haunting::spirit),
            Codec.LONG.fieldOf("next_event_at").forGetter(Haunting::nextEventAt)
        ).apply(i, Haunting::new));
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
