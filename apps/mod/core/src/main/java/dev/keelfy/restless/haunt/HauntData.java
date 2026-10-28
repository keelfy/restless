package dev.keelfy.restless.haunt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.keelfy.restless.Restless;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/** Per-world state of player-bound spirits, keyed by player UUID. */
public final class HauntData extends SavedData {
    /** @param nextEventAt overworld game time of the next roll */
    public record Haunting(String spirit, long nextEventAt) {
        static final Codec<Haunting> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("spirit").forGetter(Haunting::spirit),
            Codec.LONG.fieldOf("next_event_at").forGetter(Haunting::nextEventAt)
        ).apply(i, Haunting::new));
    }

    private static final Codec<HauntData> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Haunting.CODEC)
        .xmap(HauntData::new, data -> data.hauntings);

    // No vanilla data fixer for mod data; NeoForge skips fixing when this is null.
    private static final SavedDataType<HauntData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Restless.MOD_ID, "hauntings"), HauntData::new, CODEC, null);

    private final Map<UUID, Haunting> hauntings;

    private HauntData() {
        this(Map.of());
    }

    private HauntData(Map<UUID, Haunting> hauntings) {
        this.hauntings = new HashMap<>(hauntings);
    }

    public static HauntData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public @Nullable Haunting get(UUID player) {
        return hauntings.get(player);
    }

    public void put(UUID player, Haunting haunting) {
        hauntings.put(player, haunting);
        setDirty();
    }
}
