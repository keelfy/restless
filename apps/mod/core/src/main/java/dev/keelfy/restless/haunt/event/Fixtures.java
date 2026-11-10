package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.event.kit.Nearby;
import dev.keelfy.restless.haunt.event.kit.SimpleEvent;
import dev.keelfy.restless.haunt.event.kit.Swaps;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.entity.vault.VaultSharedData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Blocks that hold something or stand in a pose: stands, beacons, statues, note blocks, campfires, spawners, vaults,
 * beds and carpets. Where the block has a block entity the lie is the entity's own data packet to this player alone,
 * and the real packet follows when the time is up.
 */
final class Fixtures {
    private static final int POTION_SLOTS = 3;
    private static final int STAND_SLOTS = 5;
    private static final int CAMPFIRE_SLOTS = 4;

    private Fixtures() {}

    static List<HauntEvent> all() {
        return List.of(
                // 208: one bottle in a stand becomes another potion; put back if untouched (stays under hardcore)
                SimpleEvent.light(
                        "brewing_swap",
                        EventTag.DOORS,
                        player -> !bottled(player).isEmpty(),
                        Fixtures::swapPotion),
                // 209: the sounds of a brew in a stand with nothing in it
                SimpleEvent.light(
                        "brewing_bubbles",
                        EventTag.DOORS,
                        player -> !idleStands(player).isEmpty(),
                        Fixtures::bubble),
                // 214: an opaque block, or black glass, over the beacon in this player's view only
                SimpleEvent.harmful(
                        "beacon_out", EventTag.BLOCKS, player -> !beams(player).isEmpty(), Fixtures::putOutBeam),
                // 222: out of view, the statue is in another pose when the player looks again
                SimpleEvent.light(
                        "statue_pose",
                        EventTag.DOORS,
                        player -> !unwatchedStatues(player).isEmpty(),
                        Fixtures::turnStatue),
                // 226: a fixed tune on the pitches of real note blocks near the player
                SimpleEvent.light(
                        "note_tune",
                        EventTag.SOUND,
                        player -> !noteBlocks(player).isEmpty(),
                        Fixtures::playTune),
                // 227: shown on a free slot of the campfire; the real campfire is empty
                SimpleEvent.light(
                        "campfire_cooks",
                        EventTag.BLOCKS,
                        player -> !freeFires(player).isEmpty(),
                        Fixtures::cook),
                // 229: the spawner's beast is another one in this player's view
                SimpleEvent.light(
                        "spawner_shows",
                        EventTag.BLOCKS,
                        player -> !spawners(player).isEmpty(),
                        Fixtures::showSpawnerMob),
                // 230
                SimpleEvent.light(
                        "trial_ominous",
                        EventTag.BLOCKS,
                        player -> Nearby.any(player, "trial_ominous", Fixtures::calmTrial),
                        Fixtures::turnOminous),
                // 231
                SimpleEvent.light(
                        "vault_head", EventTag.BLOCKS, player -> !vaults(player).isEmpty(), Fixtures::showHead),
                // 233: both halves; Swaps keeps the real bed (a block entity) and only shows the black one
                SimpleEvent.light(
                        "black_bed",
                        EventTag.BLOCKS,
                        player -> Nearby.any(player, "black_bed", Fixtures::paleBedHead),
                        Fixtures::blackenBed),
                // 234
                SimpleEvent.light(
                        "black_carpets",
                        EventTag.BLOCKS,
                        player ->
                                Household.indoors(player) && Nearby.any(player, "black_carpets", Fixtures::paleCarpet),
                        Fixtures::blackenCarpets),
                // 515: dark ash falls from the ceiling around the player
                SimpleEvent.light("black_ash", EventTag.PRESENCE, Household::indoors, Fixtures::fallAsh));
    }

    private static <T extends BlockEntity> List<T> entities(
            ServerPlayer player, String event, Block block, Class<T> type) {
        return Nearby.all(player, event, s -> s.is(block)).stream()
                .map(pos -> player.level().getBlockEntity(pos))
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    private static BlockPos pick(ServerPlayer player, List<BlockPos> spots) {
        return spots.get(player.getRandom().nextInt(spots.size()));
    }

    /**
     * This player's client is given {@code fake} in place of the entity's own update data, and the real data again
     * after {@code ticks}.
     */
    private static void lie(
            ServerPlayer player, BlockEntity entity, Function<RegistryAccess, CompoundTag> fake, int ticks) {
        ServerLevel level = player.level();
        BlockPos pos = entity.getBlockPos();
        player.connection.send(ClientboundBlockEntityDataPacket.create(entity, (be, access) -> fake.apply(access)));
        Scheduler.later(player, ticks, p -> {
            BlockEntity now = p.level() == level ? level.getBlockEntity(pos) : null;
            Packet<ClientGamePacketListener> real = now == null ? null : now.getUpdatePacket();
            if (real != null) {
                p.connection.send(real);
            }
        });
    }

    // 208

    private static boolean bottle(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.potion().isPresent();
    }

    private static List<BrewingStandBlockEntity> bottled(ServerPlayer player) {
        return entities(player, "brewing_swap", Blocks.BREWING_STAND, BrewingStandBlockEntity.class).stream()
                .filter(stand -> {
                    for (int slot = 0; slot < POTION_SLOTS; slot++) {
                        if (bottle(stand.getItem(slot))) {
                            return true;
                        }
                    }
                    return false;
                })
                .toList();
    }

    private static void swapPotion(ServerPlayer player) {
        List<BrewingStandBlockEntity> stands = bottled(player);
        if (stands.isEmpty()) {
            return;
        }
        BrewingStandBlockEntity stand = stands.get(player.getRandom().nextInt(stands.size()));
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < POTION_SLOTS; slot++) {
            if (bottle(stand.getItem(slot))) {
                slots.add(slot);
            }
        }
        int slot = slots.get(player.getRandom().nextInt(slots.size()));
        ItemStack before = stand.getItem(slot).copy();
        PotionContents was = before.get(DataComponents.POTION_CONTENTS);
        Config.Section config = Config.event("brewing_swap");
        List<Holder.Reference<Potion>> others = config.texts("potions").stream()
                .map(id -> BuiltInRegistries.POTION.get(Identifier.parse(id)))
                .flatMap(Optional::stream)
                .filter(potion -> !was.is(potion))
                .toList();
        if (others.isEmpty()) {
            return;
        }
        ItemStack after = before.copy();
        after.set(
                DataComponents.POTION_CONTENTS,
                new PotionContents(others.get(player.getRandom().nextInt(others.size()))));
        stand.setItem(slot, after);
        stand.setChanged();
        ServerLevel level = player.level();
        BlockPos pos = stand.getBlockPos();
        Restore.revert(level, config.integer("restore_ticks"), force -> {
            if (!force && !level.isLoaded(pos)) {
                return false;
            }
            if (level.getBlockEntity(pos) instanceof BrewingStandBlockEntity now
                    && ItemStack.matches(now.getItem(slot), after)) {
                now.setItem(slot, before);
                now.setChanged();
            }
            return true;
        });
    }

    // 209

    private static List<BlockPos> idleStands(ServerPlayer player) {
        return entities(player, "brewing_bubbles", Blocks.BREWING_STAND, BrewingStandBlockEntity.class).stream()
                .filter(stand -> {
                    for (int slot = 0; slot < STAND_SLOTS; slot++) {
                        if (!stand.getItem(slot).isEmpty()) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(BlockEntity::getBlockPos)
                .toList();
    }

    private static void bubble(ServerPlayer player) {
        List<BlockPos> stands = idleStands(player);
        if (stands.isEmpty()) {
            return;
        }
        BlockPos pos = pick(player, stands);
        Config.Section config = Config.event("brewing_bubbles");
        Config.Tone tone = config.tone("sound");
        Config.Burst bubbles = config.burst("particles");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            ServerLevel level = p.level();
            if (!level.getBlockState(pos).is(Blocks.BREWING_STAND)) {
                return false;
            }
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, tone.volume(), tone.pitch());
            level.sendParticles(
                    ParticleTypes.BUBBLE_POP,
                    pos.getX() + 0.5,
                    pos.getY() + bubbles.offsetY(),
                    pos.getZ() + 0.5,
                    bubbles.count(),
                    bubbles.dx(),
                    bubbles.dy(),
                    bubbles.dz(),
                    bubbles.speed());
            return n + 1 < config.integer("bubbles");
        });
    }

    // 214

    private static List<BlockPos> beams(ServerPlayer player) {
        ServerLevel level = player.level();
        return Nearby.all(player, "beacon_out", s -> s.is(Blocks.BEACON)).stream()
                .filter(pos -> level.getBlockEntity(pos) instanceof BeaconBlockEntity beacon
                        && !beacon.getBeamSections().isEmpty()
                        && level.getBlockState(pos.above()).isAir())
                .toList();
    }

    private static void putOutBeam(ServerPlayer player) {
        List<BlockPos> beacons = beams(player);
        if (beacons.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("beacon_out");
        List<String> blocks = config.texts("blocks");
        Block cover = BuiltInRegistries.BLOCK.getValue(
                Identifier.parse(blocks.get(player.getRandom().nextInt(blocks.size()))));
        Swaps.show(player, pick(player, beacons).above(), cover.defaultBlockState(), config.integer("ticks"));
    }

    // 222

    private static List<BlockPos> unwatchedStatues(ServerPlayer player) {
        double cone = Math.cos(Math.toRadians(Config.event("statue_pose").num("watch_degrees")));
        return Nearby.all(player, "statue_pose", s -> s.getBlock() instanceof CopperGolemStatueBlock).stream()
                .filter(pos -> player.getLookAngle()
                                .dot(Vec3.atCenterOf(pos)
                                        .subtract(player.getEyePosition())
                                        .normalize())
                        < cone)
                .toList();
    }

    private static void turnStatue(ServerPlayer player) {
        List<BlockPos> statues = unwatchedStatues(player);
        if (statues.isEmpty()) {
            return;
        }
        ServerLevel level = player.level();
        BlockPos pos = pick(player, statues);
        BlockState state = level.getBlockState(pos);
        CopperGolemStatueBlock.Pose now = state.getValue(CopperGolemStatueBlock.POSE);
        List<CopperGolemStatueBlock.Pose> others = new ArrayList<>(List.of(CopperGolemStatueBlock.Pose.values()));
        others.remove(now);
        Restore.swap(
                level,
                pos,
                state.setValue(
                        CopperGolemStatueBlock.POSE,
                        others.get(player.getRandom().nextInt(others.size()))),
                Config.event("statue_pose").integer("restore_ticks"));
    }

    // 226

    private static List<BlockPos> noteBlocks(ServerPlayer player) {
        ServerLevel level = player.level();
        return Nearby.all(player, "note_tune", s -> s.getBlock() instanceof NoteBlock).stream()
                .filter(pos -> playable(level.getBlockState(pos), level, pos))
                .sorted(Comparator.comparingDouble((BlockPos pos) -> pos.distToCenterSqr(player.position())))
                .toList();
    }

    /** As the block itself decides: a tunable instrument, with air above it unless it sounds from above. */
    private static boolean playable(BlockState state, ServerLevel level, BlockPos pos) {
        return state.getValue(NoteBlock.INSTRUMENT).isTunable()
                && (state.getValue(NoteBlock.INSTRUMENT).worksAboveNoteBlock()
                        || level.getBlockState(pos.above()).isAir());
    }

    private static void playTune(ServerPlayer player) {
        List<BlockPos> blocks = noteBlocks(player);
        if (blocks.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("note_tune");
        List<String> tunes = config.texts("tunes");
        String[] notes =
                tunes.get(player.getRandom().nextInt(tunes.size())).trim().split("\\s+");
        Config.Tone tone = config.tone("sound");
        Scheduler.repeat(player, 0, config.integer("step_ticks"), (p, n) -> {
            ServerLevel level = p.level();
            BlockPos pos = blocks.get(n % blocks.size());
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof NoteBlock && playable(state, level, pos)) {
                int note = Integer.parseInt(notes[n]);
                level.playSound(
                        null,
                        pos,
                        state.getValue(NoteBlock.INSTRUMENT).getSoundEvent().value(),
                        SoundSource.RECORDS,
                        tone.volume(),
                        NoteBlock.getPitchFromNote(note));
                level.sendParticles(
                        ParticleTypes.NOTE,
                        pos.getX() + 0.5,
                        pos.getY() + 1.2,
                        pos.getZ() + 0.5,
                        0,
                        note / 24.0,
                        0,
                        0,
                        1);
            }
            return n + 1 < notes.length;
        });
    }

    // 227

    private static List<CampfireBlockEntity> freeFires(ServerPlayer player) {
        return Nearby.all(player, "campfire_cooks", s -> s.is(BlockTags.CAMPFIRES)).stream()
                .map(pos -> player.level().getBlockEntity(pos))
                .filter(entity -> entity instanceof CampfireBlockEntity fire
                        && fire.getItems().stream().anyMatch(ItemStack::isEmpty))
                .map(entity -> (CampfireBlockEntity) entity)
                .toList();
    }

    private static void cook(ServerPlayer player) {
        List<CampfireBlockEntity> fires = freeFires(player);
        if (fires.isEmpty()) {
            return;
        }
        CampfireBlockEntity fire = fires.get(player.getRandom().nextInt(fires.size()));
        Config.Section config = Config.event("campfire_cooks");
        List<String> items = config.texts("items");
        ItemStack food = new ItemStack(BuiltInRegistries.ITEM.getValue(
                Identifier.parse(items.get(player.getRandom().nextInt(items.size())))));
        lie(
                player,
                fire,
                access -> {
                    NonNullList<ItemStack> shown = NonNullList.withSize(CAMPFIRE_SLOTS, ItemStack.EMPTY);
                    for (int slot = 0; slot < CAMPFIRE_SLOTS; slot++) {
                        shown.set(slot, fire.getItems().get(slot).copy());
                    }
                    for (int slot = 0; slot < CAMPFIRE_SLOTS; slot++) {
                        if (shown.get(slot).isEmpty()) {
                            shown.set(slot, food);
                            break;
                        }
                    }
                    TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, access);
                    ContainerHelper.saveAllItems(out, shown, true);
                    return out.buildResult();
                },
                config.integer("ticks"));
    }

    // 229

    private static List<SpawnerBlockEntity> spawners(ServerPlayer player) {
        return entities(player, "spawner_shows", Blocks.SPAWNER, SpawnerBlockEntity.class);
    }

    private static void showSpawnerMob(ServerPlayer player) {
        List<SpawnerBlockEntity> found = spawners(player);
        if (found.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("spawner_shows");
        List<String> mobs = config.texts("mobs");
        String mob = mobs.get(player.getRandom().nextInt(mobs.size()));
        SpawnerBlockEntity spawner = found.get(player.getRandom().nextInt(found.size()));
        lie(
                player,
                spawner,
                access -> {
                    CompoundTag entity = new CompoundTag();
                    entity.putString("id", mob);
                    CompoundTag data = new CompoundTag();
                    data.put("entity", entity);
                    CompoundTag tag = spawner.getUpdateTag(access);
                    tag.put("SpawnData", data);
                    return tag;
                },
                config.integer("ticks"));
    }

    // 230

    private static boolean calmTrial(BlockState state) {
        return state.getBlock() instanceof TrialSpawnerBlock && !state.getValue(TrialSpawnerBlock.OMINOUS);
    }

    private static void turnOminous(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "trial_ominous", Fixtures::calmTrial);
        if (pos != null) {
            Swaps.show(
                    player,
                    pos,
                    player.level().getBlockState(pos).setValue(TrialSpawnerBlock.OMINOUS, true),
                    Config.event("trial_ominous").integer("ticks"));
        }
    }

    // 231

    private static List<VaultBlockEntity> vaults(ServerPlayer player) {
        return entities(player, "vault_head", Blocks.VAULT, VaultBlockEntity.class);
    }

    private static void showHead(ServerPlayer player) {
        List<VaultBlockEntity> found = vaults(player);
        if (found.isEmpty()) {
            return;
        }
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.getGameProfile()));
        lie(
                player,
                found.get(player.getRandom().nextInt(found.size())),
                access -> {
                    CompoundTag tag = new CompoundTag();
                    tag.store(
                            VaultSharedData.TAG_NAME,
                            VaultSharedData.CODEC,
                            access.createSerializationContext(NbtOps.INSTANCE),
                            new VaultSharedData(head, Set.of(), VaultConfig.DEFAULT.deactivationRange()));
                    return tag;
                },
                Config.event("vault_head").integer("ticks"));
    }

    // 233

    private static boolean paleBedHead(BlockState state) {
        return state.getBlock() instanceof BedBlock
                && state.getValue(BedBlock.PART) == BedPart.HEAD
                && !state.is(Blocks.BED.black());
    }

    private static void blackenBed(ServerPlayer player) {
        BlockPos head = Nearby.random(player, "black_bed", Fixtures::paleBedHead);
        if (head == null) {
            return;
        }
        ServerLevel level = player.level();
        BlockState state = level.getBlockState(head);
        BlockPos foot = head.relative(state.getValue(BedBlock.FACING).getOpposite());
        Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
        for (BlockPos half : List.of(head, foot)) {
            BlockState now = level.getBlockState(half);
            if (now.getBlock() instanceof BedBlock) {
                shown.put(half, Blocks.BED.black().defaultBlockState().withPropertiesOf(now));
            }
        }
        Swaps.show(player, shown, Config.event("black_bed").integer("ticks"));
    }

    // 234

    private static boolean paleCarpet(BlockState state) {
        return state.is(BlockTags.WOOL_CARPETS) && !state.is(Blocks.CARPET.black());
    }

    private static void blackenCarpets(ServerPlayer player) {
        Config.Section config = Config.event("black_carpets");
        ServerLevel level = player.level();
        Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
        Nearby.all(player, "black_carpets", Fixtures::paleCarpet).stream()
                .sorted(Comparator.comparingDouble((BlockPos pos) -> pos.distToCenterSqr(player.position())))
                .limit(config.integer("max_blocks"))
                .forEach(pos -> shown.put(
                        pos, Blocks.CARPET.black().defaultBlockState().withPropertiesOf(level.getBlockState(pos))));
        Swaps.show(player, shown, config.integer("ticks"));
    }

    // 515

    private static void fallAsh(ServerPlayer player) {
        Config.Section config = Config.event("black_ash");
        Config.Burst ash = config.burst("particles");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            p.level()
                    .sendParticles(
                            ParticleTypes.ASH,
                            p.getX(),
                            p.getY() + ash.offsetY(),
                            p.getZ(),
                            ash.count(),
                            ash.dx(),
                            ash.dy(),
                            ash.dz(),
                            ash.speed());
            return n + 1 < config.integer("bursts");
        });
    }
}
