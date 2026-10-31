package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.spirit.Story;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
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
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

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
                if (!data.graves().containsKey(spirit.haunting().uuid())) {
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
            }
        }
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
        level.setBlock(top, Content.GRAVESTONE.get().defaultBlockState(), 3);
        data.putGrave(spirit, new HauntData.Grave(top, true));
    }

    /** What the spirit wants back, the same for its whole life. */
    public static Item wanted(long seed) {
        List<String> wants = Config.section("grave").texts("wants");
        String id = wants.get(new Random(seed ^ SALT).nextInt(wants.size()));
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
     * A player at a grave holding {@code held}: the wanted thing lays the spirit to rest, anything else reads the
     * stone. @return whether it took the item
     */
    public static boolean visit(ServerPlayer player, BlockPos pos, ItemStack held) {
        HauntData data = HauntData.get(player.level().getServer());
        Optional<HauntData.Haunting> buried = at(data, pos);
        if (buried.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.restless.grave_quiet"), true);
            return false;
        }
        HauntData.Haunting spirit = buried.get();
        Story story = Story.of(spirit.seed());
        Item wanted = wanted(spirit.seed());
        if (held.is(wanted)) {
            rest(player, pos, spirit, story);
            return true;
        }
        Component thing = wanted.getDefaultInstance().getHoverName();
        player.sendSystemMessage(Component.translatable(
                "message.restless.epitaph", story.name(), story.daysDead(), story.cause(), thing));
        Journal.note(player, Notebook.Kind.GRAVE, spirit, story.name(), pos.getX(), pos.getZ(), thing);
        return false;
    }

    private static void rest(ServerPlayer player, BlockPos pos, HauntData.Haunting spirit, Story story) {
        ServerLevel level = player.level();
        Config.Section config = Config.section("grave");
        HauntData data = HauntData.get(level.getServer());
        data.change(spirit.uuid(), h -> null);
        data.putGrave(spirit.uuid(), null);
        Location.forget(spirit.uuid());
        Vec3 above = Vec3.atCenterOf(pos).add(0, 0.8, 0);
        BuiltInRegistries.ITEM
                .getOptional(Identifier.fromNamespaceAndPath(Restless.MOD_ID, spirit.spirit() + "_essence"))
                .ifPresent(essence -> level.addFreshEntity(new ItemEntity(
                        level, above.x, above.y, above.z, new ItemStack(essence, config.integer("essence")))));
        ExperienceOrb.award(level, above, config.integer("experience"));
        level.sendParticles(ParticleTypes.END_ROD, above.x, above.y, above.z, 30, 0.3, 0.8, 0.3, 0.03);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, 0.7f);
        Component type = Evolution.name(spirit);
        player.sendSystemMessage(Component.translatable("message.restless.rested", story.name()), true);
        Journal.note(player, Notebook.Kind.RESTED, spirit, story.name(), type);
    }
}
