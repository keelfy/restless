package dev.keelfy.restless.ward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.keelfy.restless.Restless;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * What the consumables leave in the world and must outlast a restart: a knot on a container, a patch of dusk salt, the
 * sighting marks a player has set. Each spell has an end, a game time (or {@link #FOREVER}); {@link #prune} drops the
 * ended ones.
 */
public final class Spells extends SavedData {
    /** What kind of spell a record is. */
    public enum Kind implements StringRepresentable {
        KNOT,
        DUSK;

        static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** The end of a spell that lasts until it is taken back. */
    public static final long FOREVER = Long.MAX_VALUE;

    public record Spell(Kind kind, ResourceKey<Level> dimension, BlockPos pos, long until) {
        static final Codec<Spell> CODEC = RecordCodecBuilder.create(i -> i.group(
                        Kind.CODEC.fieldOf("kind").forGetter(Spell::kind),
                        Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Spell::dimension),
                        BlockPos.CODEC.fieldOf("pos").forGetter(Spell::pos),
                        Codec.LONG.fieldOf("until").forGetter(Spell::until))
                .apply(i, Spell::new));
    }

    private static final Codec<Spells> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Spell.CODEC.listOf().optionalFieldOf("spells", List.of()).forGetter(s -> s.spells),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.CODEC.listOf())
                            .optionalFieldOf("marks", Map.of())
                            .forGetter(s -> s.marks))
            .apply(i, Spells::new));

    private static final SavedDataType<Spells> TYPE =
            new SavedDataType<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "spells"), Spells::new, CODEC, null);

    private final List<Spell> spells = new ArrayList<>();
    /** The sighting marks (entity ids) each player has set. */
    private final Map<UUID, List<UUID>> marks = new HashMap<>();

    private Spells() {}

    private Spells(List<Spell> spells, Map<UUID, List<UUID>> marks) {
        this.spells.addAll(spells);
        marks.forEach((owner, ids) -> this.marks.put(owner, new ArrayList<>(ids)));
    }

    public static Spells get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public void add(Spell spell) {
        spells.add(spell);
        setDirty();
    }

    public void remove(Spell spell) {
        if (spells.remove(spell)) {
            setDirty();
        }
    }

    /** The live spells of {@code kind} whose point lies within {@code radius} of {@code pos}. */
    public List<Spell> near(Kind kind, ResourceKey<Level> dimension, BlockPos pos, double radius, long now) {
        List<Spell> found = new ArrayList<>();
        for (Spell spell : spells) {
            if (spell.kind == kind
                    && spell.dimension.equals(dimension)
                    && spell.until > now
                    && spell.pos.distSqr(pos) <= radius * radius) {
                found.add(spell);
            }
        }
        return found;
    }

    public List<Spell> all(Kind kind) {
        return spells.stream().filter(s -> s.kind == kind).toList();
    }

    /** Drops the spells that have ended. */
    public void prune(long now) {
        if (spells.removeIf(s -> s.until <= now)) {
            setDirty();
        }
    }

    public int marks(UUID owner) {
        return marks.getOrDefault(owner, List.of()).size();
    }

    public void mark(UUID owner, UUID entity) {
        marks.computeIfAbsent(owner, k -> new ArrayList<>()).add(entity);
        setDirty();
    }

    public void unmark(UUID owner, UUID entity) {
        List<UUID> list = marks.get(owner);
        if (list != null && list.remove(entity)) {
            if (list.isEmpty()) {
                marks.remove(owner);
            }
            setDirty();
        }
    }
}
