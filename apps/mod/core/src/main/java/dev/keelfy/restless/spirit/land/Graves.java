package dev.keelfy.restless.spirit.land;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.divine.Sense;
import dev.keelfy.restless.haunt.Location;
import dev.keelfy.restless.journal.Journal;
import dev.keelfy.restless.journal.Milestones;
import dev.keelfy.restless.journal.Notebook;
import dev.keelfy.restless.spirit.Story;
import dev.keelfy.restless.spirit.life.Echoes;
import dev.keelfy.restless.spirit.life.Evolution;
import dev.keelfy.restless.state.HauntData;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Unfinished business. Every spirit a player in the overworld can sense gets a grave somewhere around them, built when
 * its chunk is first loaded and rebuilt if it goes. The grave names who the spirit was and what it wants back; bringing
 * that lets the spirit go in peace, with a better reward than a ritual and no anger.
 */
public final class Graves {
    // Keeps the wanted item apart from the trait and story rolls, which use the same seed.
    private static final long SALT = 0x6A7E5L;

    private Graves() {}

    public static void tick(MinecraftServer server) {
        Config.Section config = Config.section("grave");
        if (server.getTickCount() % config.integer("check_ticks") != 0) {
            return;
        }
        ServerLevel overworld = server.overworld();
        HauntData data = HauntData.get(server);
        for (ServerPlayer player : overworld.players()) {
            for (Sense.Spirit spirit : Sense.of(player, data)) {
                // An echo has no grave: it was never laid to rest, only sent off.
                if (!data.graves().containsKey(spirit.haunting().uuid()) && !Echoes.is(data, spirit.haunting())) {
                    data.putGrave(
                            spirit.haunting().uuid(),
                            new HauntData.Grave(column(player.blockPosition(), config, player.getRandom()), false));
                }
            }
        }
        for (Map.Entry<UUID, HauntData.Grave> entry : List.copyOf(data.graves().entrySet())) {
            if (data.find(entry.getKey()).isEmpty()) {
                data.putGrave(entry.getKey(), null); // banished or removed: the stone stays, quiet
            } else {
                build(overworld, data, entry.getKey(), entry.getValue());
                Optional.ofNullable(data.graves().get(entry.getKey()))
                        .ifPresent(grave -> data.find(entry.getKey())
                                .ifPresent(spirit -> Remains.tick(overworld, data, spirit, grave)));
            }
        }
        Letter.tick(overworld, data);
    }

    private static BlockPos column(BlockPos from, Config.Section config, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = config.num("min_distance")
                + random.nextDouble() * (config.num("max_distance") - config.num("min_distance"));
        return BlockPos.containing(
                from.getX() + Math.cos(angle) * distance, 0, from.getZ() + Math.sin(angle) * distance);
    }

    private static void build(ServerLevel level, HauntData data, UUID spirit, HauntData.Grave grave) {
        BlockPos pos = grave.pos();
        if (level.getChunkSource()
                                .getChunkNow(
                                        SectionPos.blockToSectionCoord(pos.getX()),
                                        SectionPos.blockToSectionCoord(pos.getZ()))
                        == null
                || grave.built() && level.getBlockState(pos).is(Content.GRAVESTONE.get())) {
            return;
        }
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
        if (!level.getFluidState(top.below()).isEmpty()
                || !level.getBlockState(top.below()).isFaceSturdy(level, top.below(), Direction.UP)) {
            data.putGrave(spirit, null); // water or no floor here: the next check picks a new column
            return;
        }
        level.setBlock(top, Content.GRAVESTONE.get().defaultBlockState(), 3);
        data.putGrave(spirit, new HauntData.Grave(top, true, grave.stage(), grave.mark()));
    }

    /** What the spirit wants back, the same for its whole life. */
    @SuppressWarnings("java:S2245") // Seeded so the wish is reproducible; not security-sensitive.
    public static Item wanted(long seed) {
        List<String> wants = Config.section("grave").texts("wants");
        String id = wants.get(new Random(seed ^ SALT).nextInt(wants.size()));
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElse(Items.BONE);
    }

    /**
     * What this spirit wants back: for a spirit whose form comes from its death
     * ({@link dev.keelfy.restless.state.Marks#deathForm}) one thing of the family its cause names
     * ({@link Story#wants}), for an older one {@link #wanted(long)}.
     */
    @SuppressWarnings("java:S2245") // Seeded so the wish is reproducible; not security-sensitive.
    public static Item wanted(HauntData data, HauntData.Haunting spirit) {
        if (!data.marks(spirit.uuid()).deathForm()) {
            return wanted(spirit.seed());
        }
        List<String> wants = Story.of(spirit.seed()).wants();
        String id = wants.get(new Random(spirit.seed() ^ SALT).nextInt(wants.size()));
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElse(Items.BONE);
    }

    /** The spirit buried here, if it still haunts anyone. */
    public static Optional<HauntData.Haunting> at(HauntData data, BlockPos pos) {
        return data.graves().entrySet().stream()
                .filter(e -> e.getValue().pos().equals(pos))
                .findFirst()
                .flatMap(e -> data.find(e.getKey()));
    }

    /** Where the grave of this spirit is, if it has one yet. */
    public static Optional<BlockPos> of(HauntData data, UUID spirit) {
        return Optional.ofNullable(data.graves().get(spirit)).map(HauntData.Grave::pos);
    }

    /**
     * A player at a grave holding {@code held}: once its unfinished business ({@link Quest}) is done the spirit is laid
     * to rest, otherwise the stone is read and the business begins. @return whether it took the item
     */
    public static boolean visit(ServerPlayer player, BlockPos pos, ItemStack held) {
        HauntData data = HauntData.get(player.level().getServer());
        Optional<HauntData.Haunting> buried = at(data, pos);
        if (buried.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.restless.grave_quiet"), true);
            return false;
        }
        HauntData.Haunting spirit = buried.get();
        HauntData.Grave grave = data.graves().get(spirit.uuid());
        Story story = Story.of(spirit.seed());
        Quest quest = Quest.of(spirit.seed());
        boolean took = switch (quest) {
            case ITEM -> held.is(wanted(data, spirit));
            case REMAINS -> Remains.is(held, spirit.uuid());
            case LETTER, KILLER, RIVAL -> false;
        };
        if (took || done(data, quest, grave)) {
            rest(player, pos, spirit, story);
            return took;
        }
        Component want = switch (quest) {
            case ITEM -> wanted(data, spirit).getDefaultInstance().getHoverName();
            case RIVAL -> Rival.read(player, data, spirit, grave);
            case REMAINS -> Remains.read(player, data, spirit, grave);
            case LETTER -> Letter.read(player, data, spirit, grave);
            case KILLER -> Killer.read(player, data, spirit, grave);
        };
        player.sendSystemMessage(Component.translatable(
                quest == Quest.ITEM ? "message.restless.epitaph" : "message.restless.epitaph." + quest.id(),
                story.name(),
                story.daysDead(),
                Component.translatable(story.causeKey()),
                want));
        Journal.tagged(
                player,
                Notebook.Kind.GRAVE,
                spirit,
                quest == Quest.ITEM ? null : quest.id(),
                story.name(),
                pos.getX(),
                pos.getZ(),
                want);
        return false;
    }

    /** A named killer died, see {@link Killer}. */
    public static void killed(@Nullable Entity killer, LivingEntity dead) {
        if (dead.level() instanceof ServerLevel level) {
            Killer.killed(HauntData.get(level.getServer()), killer, dead);
        }
    }

    /** How fast the spirit acts for this player: faster while they carry its bones. */
    public static double pace(ServerPlayer player, HauntData.Haunting spirit) {
        return Remains.pace(player, spirit.uuid());
    }

    /** Marks a stack with the spirit it belongs to. */
    static ItemStack tag(ItemStack stack, String key, UUID spirit) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY.update(tag -> tag.putString(key, spirit.toString())));
        return stack;
    }

    static boolean tagged(ItemStack stack, String key, UUID spirit) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getStringOr(key, "")
                .equals(spirit.toString());
    }

    /** Whether the business is done and the next visit lays the spirit to rest. */
    private static boolean done(HauntData data, Quest quest, HauntData.Grave grave) {
        return switch (quest) {
            case ITEM, REMAINS -> false; // brought to the grave in hand
            case LETTER, KILLER -> grave.stage() >= 2;
            case RIVAL -> Rival.gone(data, grave);
        };
    }

    private static void rest(ServerPlayer player, BlockPos pos, HauntData.Haunting spirit, Story story) {
        ServerLevel level = player.level();
        Config.Section config = Config.section("grave");
        HauntData data = HauntData.get(level.getServer());
        Optional<UUID> owner = data.owner(spirit.uuid()).filter(id -> !id.equals(player.getUUID()));
        data.change(spirit.uuid(), h -> null);
        data.putGrave(spirit.uuid(), null);
        Location.forget(spirit.uuid());
        Vec3 above = Vec3.atCenterOf(pos).add(0, 0.8, 0);
        Supplier<Item> essence = Content.ESSENCE.get(spirit.spirit());
        if (essence != null) {
            level.addFreshEntity(new ItemEntity(
                    level, above.x, above.y, above.z, new ItemStack(essence.get(), config.integer("essence"))));
        }
        ExperienceOrb.award(level, above, config.integer("experience"));
        level.sendParticles(ParticleTypes.END_ROD, above.x, above.y, above.z, 30, 0.3, 0.8, 0.3, 0.03);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, 0.7f);
        Component type = Evolution.name(spirit);
        player.sendSystemMessage(Component.translatable("message.restless.rested", story.name()), true);
        Journal.note(player, Notebook.Kind.RESTED, spirit, story.name(), type);
        Milestones.award(player, "rested");
        // Somebody else laid it to rest: its owner is told too, even if they are away.
        owner.ifPresent(id -> Journal.noteFor(level.getServer(), id, Notebook.Kind.RESTED, spirit, story.name(), type));
    }
}
