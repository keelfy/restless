package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.HauntSounds;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Ticks;
import dev.keelfy.restless.haunt.event.kit.Nearby;
import dev.keelfy.restless.haunt.event.kit.PacketEntity;
import dev.keelfy.restless.haunt.event.kit.Place;
import dev.keelfy.restless.haunt.event.kit.RestlessTags;
import dev.keelfy.restless.haunt.event.kit.SimpleEvent;
import dev.keelfy.restless.haunt.recall.Said;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The poltergeist's tantrums and games, and the trickster's jokes. */
final class Pranks {
    private Pranks() {}

    static List<HauntEvent> all() {
        return List.of(
                // poltergeist: a chest opens and tosses its things at the player
                SimpleEvent.light("poltergeist_toss", EventTag.ITEMS, player -> loaded(player) != null, Pranks::toss),
                // poltergeist: every note block and bell nearby is hit in turn
                SimpleEvent.light(
                        "poltergeist_play",
                        EventTag.DOORS,
                        player -> noisy(player).size()
                                >= Config.event("poltergeist_play").integer("min_count"),
                        Pranks::play),
                // poltergeist: every door slams, every flower pot pops
                SimpleEvent.harmful(
                        "poltergeist_tantrum",
                        EventTag.DOORS,
                        player -> slammable(player).size()
                                >= Config.event("poltergeist_tantrum").integer("min_count"),
                        Pranks::tantrum),
                // trickster: a chest of treasure that is dirt once it is taken
                SimpleEvent.light(
                        "trickster_gift",
                        EventTag.ITEMS,
                        player -> spot(player, "trickster_gift") != null,
                        Pranks::gift),
                // trickster: suspicious sand with a joke inside
                SimpleEvent.light(
                        "trickster_dig",
                        EventTag.BLOCKS,
                        player -> diggable(player) != null
                                && Said.left(player, "trickster_dig", jokes(), String::valueOf),
                        Pranks::dig),
                // trickster: a lit TNT, only the player sees it, that pops into fireworks
                SimpleEvent.light(
                        "trickster_dud", EventTag.MOBS, player -> spot(player, "trickster_dud") != null, Pranks::dud));
    }

    private static @Nullable BlockPos spot(ServerPlayer player, String event) {
        Config.Section config = Config.event(event);
        return Place.spawnSpot(player, config.integer("min_distance"), config.integer("radius"));
    }

    // ---- poltergeist

    private static @Nullable ChestBlockEntity loaded(ServerPlayer player) {
        List<ChestBlockEntity> chests = Nearby.all(player, "poltergeist_toss", s -> s.is(Blocks.CHEST)).stream()
                .map(pos -> player.level().getBlockEntity(pos))
                .filter(ChestBlockEntity.class::isInstance)
                .map(ChestBlockEntity.class::cast)
                .filter(chest -> !chest.isEmpty())
                .toList();
        return chests.isEmpty() ? null : chests.get(player.getRandom().nextInt(chests.size()));
    }

    private static void lid(ServerLevel level, BlockPos pos, boolean open) {
        level.blockEvent(pos, level.getBlockState(pos).getBlock(), 1, open ? 1 : 0);
    }

    private static void toss(ServerPlayer player) {
        ChestBlockEntity chest = loaded(player);
        if (chest == null) {
            return;
        }
        Config.Section config = Config.event("poltergeist_toss");
        ServerLevel level = player.level();
        BlockPos pos = chest.getBlockPos();
        lid(level, pos, true);
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            List<Integer> filled = new ArrayList<>();
            for (int i = 0; i < chest.getContainerSize(); i++) {
                if (!chest.getItem(i).isEmpty()) {
                    filled.add(i);
                }
            }
            if (filled.isEmpty() || n >= config.integer("stacks") || chest.isRemoved()) {
                lid(level, pos, false);
                level.playSound(null, pos, HauntSounds.LAUGH, SoundSource.HOSTILE, 0.5F, 1.4F);
                return false;
            }
            int slot = filled.get(p.getRandom().nextInt(filled.size()));
            ItemStack thrown = chest.removeItemNoUpdate(slot);
            chest.setChanged();
            Vec3 from = Vec3.atCenterOf(pos).add(0, 0.6, 0);
            Vec3 toward = p.position().subtract(from).multiply(1, 0, 1);
            toward =
                    toward.lengthSqr() < 1.0E-4 ? Vec3.ZERO : toward.normalize().scale(config.num("speed"));
            level.addFreshEntity(
                    new ItemEntity(level, from.x, from.y, from.z, thrown, toward.x, config.num("lift"), toward.z));
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.7F, 0.6F);
            return true;
        });
    }

    private static List<BlockPos> noisy(ServerPlayer player) {
        return Nearby.all(player, "poltergeist_play", s -> s.is(Blocks.NOTE_BLOCK) || s.is(Blocks.BELL));
    }

    private static void play(ServerPlayer player) {
        Config.Section config = Config.event("poltergeist_play");
        List<BlockPos> order = new ArrayList<>(noisy(player));
        order.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(player.position())));
        List<BlockPos> run = order.subList(0, Math.min(order.size(), config.integer("max_hits")));
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            ServerLevel level = p.level();
            BlockPos pos = run.get(n);
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.NOTE_BLOCK)) {
                level.blockEvent(pos, state.getBlock(), 0, 0);
            } else if (state.getBlock() instanceof BellBlock bell) {
                bell.attemptToRing(level, pos, null);
            }
            return n + 1 < run.size();
        });
    }

    static boolean slams(BlockState state) {
        return state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.WOODEN_TRAPDOORS)
                || state.is(BlockTags.FENCE_GATES);
    }

    private static List<BlockPos> slammable(ServerPlayer player) {
        return Nearby.all(player, "poltergeist_tantrum", Pranks::slams);
    }

    static boolean isOpen(BlockState state) {
        return state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.OPEN)
                || state.getBlock() instanceof TrapDoorBlock && state.getValue(TrapDoorBlock.OPEN)
                || state.getBlock() instanceof FenceGateBlock && state.getValue(FenceGateBlock.OPEN);
    }

    static void shut(ServerLevel level, BlockPos pos, boolean open) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock door) {
            door.setOpen(null, level, state, pos, open);
        } else if (state.getBlock() instanceof TrapDoorBlock) {
            level.setBlock(pos, state.setValue(TrapDoorBlock.OPEN, open), Block.UPDATE_ALL);
            level.playSound(
                    null,
                    pos,
                    open ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
        } else if (state.getBlock() instanceof FenceGateBlock) {
            level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, open), Block.UPDATE_ALL);
            level.playSound(
                    null,
                    pos,
                    open ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
        }
    }

    /** Every door flies open in the same tick, then slams; then the flower pots pop off their shelves. */
    private static void tantrum(ServerPlayer player) {
        Config.Section config = Config.event("poltergeist_tantrum");
        List<BlockPos> doors = slammable(player);
        ServerLevel level = player.level();
        doors.forEach(pos -> shut(level, pos, true));
        List<BlockPos> pots = Nearby.all(player, "poltergeist_tantrum", s -> s.getBlock() instanceof FlowerPotBlock);
        Scheduler.later(player, config.integer("slam_ticks"), p -> {
            doors.forEach(pos -> {
                if (slams(level.getBlockState(pos))) {
                    shut(level, pos, false);
                }
            });
            pots.stream().limit(config.integer("max_pots")).forEach(pos -> pop(level, pos));
        });
    }

    private static void pop(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof FlowerPotBlock pot) {
            Block plant = pot.getPotted();
            level.removeBlock(pos, false);
            Block.popResource(level, pos, new ItemStack(Items.FLOWER_POT));
            if (plant != Blocks.AIR) {
                Block.popResource(level, pos, new ItemStack(plant));
            }
            level.sendParticles(
                    ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.02);
            level.playSound(null, pos, SoundEvents.DECORATED_POT_SHATTER, SoundSource.BLOCKS, 0.8F, 1.2F);
        }
    }

    // ---- trickster

    /** The tag a gift's stacks carry until they are taken out of the chest. */
    private static final String GIFT = "restless_gift";

    private static void gift(ServerPlayer player) {
        BlockPos at = spot(player, "trickster_gift");
        if (at == null) {
            return;
        }
        Config.Section config = Config.event("trickster_gift");
        ServerLevel level = player.level();
        Direction face =
                Direction.getApproximateNearest(player.getX() - at.getX() - 0.5, 0, player.getZ() - at.getZ() - 0.5);
        level.setBlockAndUpdate(at, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, face));
        if (!(level.getBlockEntity(at) instanceof ChestBlockEntity chest)) {
            return;
        }
        List<List<String>> loot = config.lines("loot");
        for (int i = 0; i < loot.size() && i < chest.getContainerSize(); i++) {
            chest.setItem(i + player.getRandom().nextInt(2), disguised(loot.get(i)));
        }
        chest.setChanged();
        int ticks = config.integer("restore_ticks");
        Scheduler.repeat(player, 10, 10, (p, n) -> {
            reveal(p);
            return n * 10 < ticks + Ticks.SECOND * 10;
        });
        Restore.revert(level, ticks, force -> {
            if (!force && !level.isLoaded(at)) {
                return false;
            }
            if (level.getBlockState(at).is(Blocks.CHEST)) {
                level.removeBlock(at, false);
            }
            return true;
        });
    }

    /** Dirt wearing the look and name of {@code loot}: {@code [item, count]}. */
    private static ItemStack disguised(List<String> loot) {
        ItemStack real = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(loot.get(0))));
        ItemStack dirt = new ItemStack(Items.DIRT, Integer.parseInt(loot.get(1)));
        dirt.set(DataComponents.ITEM_MODEL, BuiltInRegistries.ITEM.getKey(real.getItem()));
        dirt.set(DataComponents.ITEM_NAME, real.getItemName());
        CompoundTag mark = new CompoundTag();
        mark.putBoolean(GIFT, true);
        dirt.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(mark));
        return dirt;
    }

    private static boolean disguised(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(GIFT);
    }

    /** What was taken out of the chest turns into the dirt it is, with a laugh. */
    private static void reveal(ServerPlayer player) {
        boolean found = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (disguised(stack)) {
                player.getInventory().setItem(i, new ItemStack(Items.DIRT, stack.getCount()));
                found = true;
            }
        }
        if (found) {
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.broadcastChanges();
            player.level().playSound(null, player.blockPosition(), HauntSounds.LAUGH, SoundSource.HOSTILE, 0.8F, 1.3F);
        }
    }

    private static List<List<String>> jokes() {
        return Config.event("trickster_dig").lines("jokes");
    }

    private static @Nullable BlockPos diggable(ServerPlayer player) {
        ServerLevel level = player.level();
        List<BlockPos> blocks = Nearby.all(player, "trickster_dig", s -> s.is(RestlessTags.DIGGABLE)).stream()
                .filter(pos -> level.isEmptyBlock(pos.above()))
                .toList();
        return blocks.isEmpty() ? null : blocks.get(player.getRandom().nextInt(blocks.size()));
    }

    private static void dig(ServerPlayer player) {
        BlockPos pos = diggable(player);
        List<String> joke = pos == null ? null : Said.pick(player, "trickster_dig", jokes(), String::valueOf);
        if (joke == null) {
            return;
        }
        ServerLevel level = player.level();
        BlockState was = level.getBlockState(pos);
        level.setBlockAndUpdate(
                pos,
                was.is(Blocks.GRAVEL)
                        ? Blocks.SUSPICIOUS_GRAVEL.defaultBlockState()
                        : Blocks.SUSPICIOUS_SAND.defaultBlockState());
        if (level.getBlockEntity(pos) instanceof BrushableBlockEntity brushable) {
            ItemStack prize = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(joke.get(0))));
            if (joke.size() > 1) {
                prize.set(DataComponents.CUSTOM_NAME, Component.literal(joke.get(1)));
            }
            CompoundTag data = brushable.saveCustomOnly(level.registryAccess());
            data.put(
                    "item",
                    ItemStack.CODEC
                            .encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), prize)
                            .getOrThrow());
            brushable.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
        }
        Restore.revert(level, Config.event("trickster_dig").integer("restore_ticks"), force -> {
            if (!force && !level.isLoaded(pos)) {
                return false;
            }
            BlockState now = level.getBlockState(pos);
            if (now.is(Blocks.SUSPICIOUS_SAND) || now.is(Blocks.SUSPICIOUS_GRAVEL)) {
                level.setBlockAndUpdate(pos, was);
            }
            return true;
        });
    }

    private static void dud(ServerPlayer player) {
        BlockPos at = spot(player, "trickster_dud");
        if (at == null) {
            return;
        }
        Config.Section config = Config.event("trickster_dud");
        Vec3 where = Vec3.atBottomCenterOf(at);
        PacketEntity tnt = PacketEntity.show(player, EntityTypes.TNT, where, 0.0F);
        player.level().playSound(null, at, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        Scheduler.later(player, config.integer("fuse_ticks"), p -> {
            tnt.remove(p);
            p.level().sendParticles(ParticleTypes.FIREWORK, where.x, where.y + 0.5, where.z, 40, 0.4, 0.5, 0.4, 0.1);
            p.level().playSound(null, at, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 1.0F, 1.0F);
            p.level().playSound(null, at, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.BLOCKS, 1.0F, 1.0F);
            p.level().playSound(null, at, HauntSounds.LAUGH, SoundSource.HOSTILE, 0.7F, 1.4F);
        });
    }
}
