package dev.keelfy.restless.haunt.requiem;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Altar;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.UnlitLights;
import dev.keelfy.restless.content.WardBlock;
import dev.keelfy.restless.haunt.Sanity;
import dev.keelfy.restless.haunt.event.Figure;
import dev.keelfy.restless.haunt.event.Swarm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The fifteen movements of the Requiem, one per spirit type, each a small class behind {@link Movement}. A movement
 * tests the counter the player learned against its type; the rite reads {@link Movement#passed} when its time is out
 * and snuffs a candle for each one failed. What a movement spawns or changes is put back in {@link Movement#end}.
 */
public final class Movements {
    private static final Map<String, Supplier<Movement>> ALL = Map.ofEntries(
            Map.entry("shade", Shade::new),
            Map.entry("poltergeist", Poltergeist::new),
            Map.entry("nix", Nix::new),
            Map.entry("phantom", Phantom::new),
            Map.entry("mimic", Mimic::new),
            Map.entry("wraith", Wraith::new),
            Map.entry("mara", Mara::new),
            Map.entry("banshee", Banshee::new),
            Map.entry("revenant", Revenant::new),
            Map.entry("lost_one", LostOne::new),
            Map.entry("domovoi", Domovoi::new),
            Map.entry("hoarder", Hoarder::new),
            Map.entry("demon", Demon::new),
            Map.entry("trickster", Trickster::new),
            Map.entry("boggart", Boggart::new));

    /** A loot table that holds nothing: a container with it counts as unopened without any loot in it. */
    private static final ResourceKey<LootTable> NOTHING =
            ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("empty"));

    private Movements() {}

    /** A fresh movement for a spirit type. */
    public static Movement of(String type) {
        return ALL.get(type).get();
    }

    private static Config.Section config() {
        return Config.section("requiem");
    }

    /**
     * The first open spot above the surface, {@code radius} blocks from {@code from} toward {@code angle}; where that
     * is far above or below {@code from} (a void, a cliff), the spot at the height of {@code from}.
     */
    private static BlockPos spot(ServerLevel level, BlockPos from, double angle, double radius) {
        BlockPos column = BlockPos.containing(
                from.getX() + Math.cos(angle) * radius, from.getY(), from.getZ() + Math.sin(angle) * radius);
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        return Math.abs(top.getY() - from.getY()) > 8 ? column : top;
    }

    /** Whether {@code at} is within {@code degrees} of where the player looks, with nothing solid between. */
    private static boolean watched(ServerPlayer player, Vec3 at, double degrees) {
        Vec3 eye = player.getEyePosition();
        return player.getLookAngle().dot(at.subtract(eye).normalize()) >= Math.cos(Math.toRadians(degrees))
                && player.level()
                                .clip(new ClipContext(
                                        eye, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                                .getType()
                        == HitResult.Type.MISS;
    }

    private static void sound(Rite rite, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        rite.level.playSound(null, rite.altar, sound, SoundSource.HOSTILE, volume, pitch);
    }

    // ---------------------------------------------------------------------------------------------------------------

    /** The light dies around the altar: lamps, torches and candles other than the ring's go out, one by one. */
    static final class Shade implements Movement {
        /** Light sources near the altar, apart from the ring's candles, that still burn. */
        static int lights(Rite rite, boolean snuffable, List<BlockPos> found) {
            int radius = config().integer("light_radius");
            Set<BlockPos> ring = new HashSet<>(rite.candles.lit());
            ring.addAll(rite.candles.out());
            int[] count = {0};
            for (BlockPos pos : BlockPos.betweenClosed(
                    rite.altar.offset(-radius, -radius, -radius), rite.altar.offset(radius, radius, radius))) {
                if (!rite.level.isLoaded(pos) || ring.contains(pos)) {
                    continue;
                }
                BlockState state = rite.level.getBlockState(pos);
                boolean hallowed = state.getBlock() instanceof WardBlock ward && ward.kind() == WardBlock.Kind.LANTERN;
                boolean fire = AbstractCandleBlock.isLit(state)
                        || UnlitLights.isTorch(state)
                        || UnlitLights.isLantern(state)
                        || (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT));
                if (fire || (hallowed && !snuffable)) {
                    count[0]++;
                    found.add(pos.immutable());
                }
            }
            return count[0];
        }

        @Override
        public void start(Rite rite) {}

        @Override
        public void tick(Rite rite, int n) {
            int every = Math.max(1, rite.length / 12);
            if (n % every != every - 1) {
                return;
            }
            List<BlockPos> found = new ArrayList<>();
            lights(rite, true, found);
            if (found.isEmpty()) {
                return;
            }
            BlockPos pos = found.get(rite.player.getRandom().nextInt(found.size()));
            BlockState state = rite.level.getBlockState(pos);
            if (AbstractCandleBlock.isLit(state)) {
                AbstractCandleBlock.extinguish(null, state, rite.level, pos);
            } else if (state.getBlock() instanceof CampfireBlock) {
                rite.level.setBlock(pos, state.setValue(CampfireBlock.LIT, false), Block.UPDATE_ALL);
            } else {
                UnlitLights.extinguish(rite.level, pos);
            }
            rite.level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.8f);
        }

        @Override
        public boolean passed(Rite rite) {
            return lights(rite, false, new ArrayList<>()) >= config().integer("shade_lights");
        }
    }

    /** Items fly from the player's pockets; the player picks them all back up. */
    static final class Poltergeist implements Movement {
        private final List<ItemEntity> flying = new ArrayList<>();

        @Override
        public void start(Rite rite) {
            Inventory inventory = rite.player.getInventory();
            List<Integer> slots = new ArrayList<>();
            for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty() && !stack.is(Content.RELIQUARY.get()) && !stack.is(Content.REQUIEM_BELL.get())) {
                    slots.add(slot);
                }
            }
            Collections.shuffle(
                    slots, new java.util.Random(rite.player.getRandom().nextLong()));
            for (int slot : slots.subList(0, Math.min(slots.size(), config().integer("poltergeist_stacks")))) {
                ItemEntity item = new ItemEntity(
                        rite.level,
                        rite.player.getX(),
                        rite.player.getY() + 1.0,
                        rite.player.getZ(),
                        inventory.removeItemNoUpdate(slot));
                item.setDeltaMovement(
                        (rite.player.getRandom().nextDouble() - 0.5) * 0.7,
                        0.35 + rite.player.getRandom().nextDouble() * 0.3,
                        (rite.player.getRandom().nextDouble() - 0.5) * 0.7);
                item.setPickUpDelay(20);
                rite.level.addFreshEntity(item);
                flying.add(item);
            }
        }

        @Override
        public boolean passed(Rite rite) {
            return flying.stream()
                    .allMatch(item -> item.isRemoved() && item.getItem().isEmpty());
        }

        /**
         * Whatever was not picked up goes back into the pockets, unless the player is gone: then it stays to be found.
         */
        @Override
        public void end(Rite rite) {
            ServerPlayer player = rite.player;
            if (player.isAlive() && !player.isRemoved()) {
                for (ItemEntity item : flying) {
                    if (!item.getItem().isEmpty()) {
                        ItemStack back = item.getItem().copy();
                        if (!player.getInventory().add(back)) {
                            rite.level.addFreshEntity(
                                    new ItemEntity(rite.level, player.getX(), player.getY(), player.getZ(), back));
                        }
                        item.getItem().setCount(0);
                    }
                    item.discard();
                }
            }
            flying.clear();
        }
    }

    /** Water rises in the circle, two blocks deep; the player must not drown. */
    static final class Nix implements Movement {
        private final List<BlockPos> water = new ArrayList<>();
        private boolean drowned;

        @Override
        public void start(Rite rite) {
            flood(rite, 0);
        }

        private void flood(Rite rite, int layer) {
            int radius = config().integer("nix_radius");
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = rite.altar.offset(dx, layer, dz);
                    if (Math.max(Math.abs(dx), Math.abs(dz)) >= 2
                            && rite.level.getBlockState(pos).isAir()) {
                        // No fluid tick is scheduled, so the water stays where it is put and cannot reach a candle.
                        rite.level.setBlock(
                                pos,
                                Blocks.WATER.defaultBlockState(),
                                Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ON_PLACE);
                        water.add(pos);
                    }
                }
            }
            rite.level.playSound(
                    null, rite.altar, SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.HOSTILE, 1.0f, 0.6f);
        }

        @Override
        public void tick(Rite rite, int n) {
            if (n == rite.length / 4) {
                flood(rite, 1);
            }
            if (rite.level
                            .getFluidState(BlockPos.containing(rite.player.getEyePosition()))
                            .is(FluidTags.WATER)
                    && rite.player.getAirSupply() <= 0) {
                drowned = true;
            }
        }

        @Override
        public boolean passed(Rite rite) {
            return !drowned && rite.level.getFluidState(rite.altar.above()).isEmpty();
        }

        @Override
        public void end(Rite rite) {
            for (BlockPos pos : water) {
                if (rite.level.getBlockState(pos).is(Blocks.WATER)) {
                    rite.level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            water.clear();
        }
    }

    /** Strangers stand outside the ring; the player must not look at any of them. */
    static final class Phantom implements Movement {
        private final List<Mannequin> figures = new ArrayList<>();
        private boolean seen;

        @Override
        public void start(Rite rite) {
            int count = config().integer("phantom_figures");
            double turn = rite.player.getRandom().nextDouble() * Math.PI * 2;
            for (int i = 0; i < count; i++) {
                BlockPos at = spot(
                        rite.level, rite.altar, turn + Math.PI * 2 * i / count, config().integer("phantom_radius"));
                Mannequin figure = Figure.spawn(rite.player, at, Figure.stranger(rite.player));
                if (figure != null) {
                    figures.add(figure);
                }
            }
        }

        @Override
        public void tick(Rite rite, int n) {
            if (n % 2 == 0 && !seen) {
                double angle = config().num("look_angle");
                seen = figures.stream()
                        .anyMatch(f -> watched(rite.player, f.position().add(0, 1.0, 0), angle));
            }
        }

        @Override
        public boolean passed(Rite rite) {
            return !seen;
        }

        @Override
        public void end(Rite rite) {
            figures.forEach(Mannequin::discard);
            figures.clear();
        }
    }

    /** Altars stand in for the real one; holy water must go on the real one. */
    static final class Mimic implements Movement {
        @Override
        public void start(Rite rite) {
            int want = config().integer("mimic_decoys");
            double turn = rite.player.getRandom().nextDouble() * Math.PI * 2;
            for (int i = 0; i < 8 && rite.decoys.size() < want; i++) {
                BlockPos at = spot(rite.level, rite.altar, turn + Math.PI * 2 * i / 8, 4.5)
                        .atY(rite.altar.getY());
                if (rite.level.getBlockState(at).isAir()
                        && rite.level.getBlockState(at.below()).isFaceSturdy(rite.level, at.below(), Direction.UP)
                        && !rite.decoys.contains(at)) {
                    rite.level.setBlock(at, Content.ALTAR.get().defaultBlockState(), Block.UPDATE_ALL);
                    rite.decoys.add(at);
                }
            }
        }

        @Override
        public boolean passed(Rite rite) {
            return rite.altar.equals(rite.blessed);
        }

        @Override
        public void end(Rite rite) {
            for (BlockPos pos : rite.decoys) {
                if (rite.level.getBlockState(pos).getBlock() instanceof Altar) {
                    rite.level.removeBlock(pos, false);
                }
            }
            rite.decoys.clear();
        }
    }

    /** Darkness and blows from nowhere; the player must come out above three hearts. */
    static final class Wraith implements Movement {
        @Override
        public void start(Rite rite) {
            rite.player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, rite.length + 20, 0, true, false));
        }

        @Override
        public void tick(Rite rite, int n) {
            int every = Math.max(1, rite.length / 10);
            if (n % every != every - 1) {
                return;
            }
            ServerPlayer player = rite.player;
            Config.Section config = config();
            boolean dark = rite.level.getMaxLocalRawBrightness(player.blockPosition()) < config.integer("wraith_light");
            float damage = (float) (config.num("wraith_damage") * (dark ? 2 : 1));
            // A blow never kills: it leaves the player a half heart to carry on with.
            damage = Math.min(damage, player.getHealth() - 1.0f);
            if (damage > 0) {
                player.hurtServer(rite.level, rite.level.damageSources().magic(), damage);
            }
            rite.level.playSound(
                    null, player.blockPosition(), SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 1.0f, 0.5f);
        }

        @Override
        public boolean passed(Rite rite) {
            return rite.player.getHealth() > config().num("wraith_health");
        }

        @Override
        public void end(Rite rite) {
            rite.player.removeEffect(MobEffects.DARKNESS);
        }
    }

    /** The player falls asleep standing, unless their mind was steady when it began. */
    static final class Mara implements Movement {
        private boolean awake;

        @Override
        public void start(Rite rite) {
            awake = Sanity.of(rite.player) > config().num("mara_sanity");
            if (!awake) {
                rite.player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, rite.length + 20, 6, true, false));
                rite.player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, rite.length + 20, 0, true, false));
            }
        }

        @Override
        public boolean passed(Rite rite) {
            return awake;
        }

        @Override
        public void end(Rite rite) {
            if (!awake) {
                rite.player.removeEffect(MobEffects.SLOWNESS);
                rite.player.removeEffect(MobEffects.BLINDNESS);
            }
        }
    }

    /** A wail, then silence; the bell must ring in the silence. */
    static final class Banshee implements Movement {
        /** The silence lasts from the end of the wail until four fifths of the movement. */
        static int silenceFrom(Rite rite) {
            return rite.length * 2 / 5;
        }

        static int silenceTo(Rite rite) {
            return rite.length * 4 / 5;
        }

        @Override
        public void start(Rite rite) {}

        @Override
        public void tick(Rite rite, int n) {
            if (n < silenceFrom(rite) && n % 20 == 0) {
                sound(rite, SoundEvents.GHAST_SCREAM, 2.0f, 0.5f);
            } else if (n == silenceFrom(rite)) {
                // The honest warning: a clear note says the silence has begun.
                sound(rite, SoundEvents.AMETHYST_BLOCK_CHIME, 2.0f, 0.5f);
            } else if (n == silenceTo(rite)) {
                sound(rite, SoundEvents.GHAST_SCREAM, 2.0f, 0.7f);
            }
        }

        @Override
        public boolean passed(Rite rite) {
            return rite.rings.stream().anyMatch(at -> at >= silenceFrom(rite) && at < silenceTo(rite));
        }
    }

    /** Mobs of the kinds the player has killed rise and come for them; all must fall. */
    static final class Revenant implements Movement {
        private final List<Mob> risen = new ArrayList<>();

        @Override
        public void start(Rite rite) {
            ServerPlayer player = rite.player;
            List<EntityType<?>> past = BuiltInRegistries.ENTITY_TYPE.stream()
                    .filter(type -> type.getCategory() == MobCategory.MONSTER
                            && type.canSummon()
                            && type != EntityTypes.WITHER
                            && type != EntityTypes.WARDEN
                            && player.getStats().getValue(Stats.ENTITY_KILLED.get(type)) > 0)
                    .toList();
            int count = config().integer("revenant_mobs");
            double turn = player.getRandom().nextDouble() * Math.PI * 2;
            for (int i = 0; i < count; i++) {
                EntityType<?> type = past.isEmpty()
                        ? EntityTypes.ZOMBIE
                        : past.get(player.getRandom().nextInt(past.size()));
                BlockPos at = spot(rite.level, player.blockPosition(), turn + Math.PI * 2 * i / count, 5);
                if (type.spawn(rite.level, at, EntitySpawnReason.EVENT) instanceof Mob mob) {
                    mob.setPersistenceRequired();
                    mob.setTarget(player);
                    mob.setGlowingTag(true);
                    risen.add(mob);
                }
            }
        }

        @Override
        public boolean passed(Rite rite) {
            return risen.stream().noneMatch(Mob::isAlive);
        }

        @Override
        public void end(Rite rite) {
            risen.forEach(mob -> {
                if (mob.isAlive()) {
                    mob.discard();
                }
            });
            risen.clear();
        }
    }

    /** The player is moved far from the altar and must walk back into the ring in time. */
    static final class LostOne implements Movement {
        private boolean home;

        /** The ring: close enough to the altar to stand among its candles. */
        static boolean inRing(Rite rite) {
            return rite.near(config().integer("candle_radius") + 1);
        }

        @Override
        public void start(Rite rite) {
            ServerPlayer player = rite.player;
            double distance = config().num("lost_distance");
            double turn = player.getRandom().nextDouble() * Math.PI * 2;
            BlockPos best = null;
            for (int i = 0; i < 8; i++) {
                BlockPos at = spot(rite.level, rite.altar, turn + Math.PI * i / 4, distance);
                BlockState floor = rite.level.getBlockState(at.below());
                best = best == null ? at : best;
                if (floor.getFluidState().isEmpty() && floor.isSolid()) {
                    best = at;
                    break;
                }
            }
            player.teleportTo(
                    rite.level,
                    best.getX() + 0.5,
                    best.getY(),
                    best.getZ() + 0.5,
                    Set.of(),
                    player.getYRot(),
                    player.getXRot(),
                    true);
            rite.level.playSound(null, best, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 0.6f);
        }

        @Override
        public void tick(Rite rite, int n) {
            // Back in the ring within two fifths of the movement counts; the rest is spare time.
            if (n <= rite.length * 2 / 5 && inRing(rite)) {
                home = true;
            }
        }

        @Override
        public boolean passed(Rite rite) {
            return home;
        }

        /** A player still far away is brought back, so the next movement finds them at the altar. */
        @Override
        public void end(Rite rite) {
            if (!inRing(rite)
                    && rite.player.isAlive()
                    && !rite.player.isRemoved()
                    && rite.player.level() == rite.level) {
                BlockPos at = rite.altar.above();
                rite.player.teleportTo(
                        rite.level,
                        at.getX() + 0.5,
                        at.getY(),
                        at.getZ() + 2.5,
                        Set.of(),
                        rite.player.getYRot(),
                        rite.player.getXRot(),
                        true);
            }
        }
    }

    /** The doors near the altar slam open; the player must close every one. */
    static final class Domovoi implements Movement {
        private final List<BlockPos> doors = new ArrayList<>();

        private List<BlockPos> near(Rite rite) {
            int radius = config().integer("door_radius");
            List<BlockPos> found = new ArrayList<>();
            for (BlockPos pos : BlockPos.betweenClosed(
                    rite.altar.offset(-radius, -2, -radius), rite.altar.offset(radius, 3, radius))) {
                BlockState state = rite.level.getBlockState(pos);
                if (state.getBlock() instanceof DoorBlock
                        && DoorBlock.isWoodenDoor(state)
                        && pos.distSqr(rite.altar) <= radius * radius
                        && state.getValue(DoorBlock.HALF)
                                == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
                    found.add(pos.immutable());
                }
            }
            return found;
        }

        @Override
        public void start(Rite rite) {
            slam(rite);
        }

        private void slam(Rite rite) {
            for (BlockPos pos : near(rite)) {
                BlockState state = rite.level.getBlockState(pos);
                ((DoorBlock) state.getBlock()).setOpen(null, rite.level, state, pos, true);
                if (!doors.contains(pos)) {
                    doors.add(pos);
                }
            }
        }

        @Override
        public void tick(Rite rite, int n) {
            if (n == rite.length / 2) {
                slam(rite);
            }
        }

        @Override
        public boolean passed(Rite rite) {
            return near(rite).stream()
                    .noneMatch(pos -> rite.level.getBlockState(pos).getValue(DoorBlock.OPEN));
        }

        @Override
        public void end(Rite rite) {
            for (BlockPos pos : doors) {
                BlockState state = rite.level.getBlockState(pos);
                if (state.getBlock() instanceof DoorBlock door && state.getValue(DoorBlock.OPEN)) {
                    door.setOpen(null, rite.level, state, pos, false);
                }
            }
            doors.clear();
        }
    }

    /** The offering is stolen into one of three chests; the player must find it. */
    static final class Hoarder implements Movement {
        private final List<BlockPos> chests = new ArrayList<>();

        /** Whether the stack is the offering. */
        static boolean isOffering(ItemStack stack) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            return data != null && data.copyTag().contains("requiem_offering");
        }

        static ItemStack offering() {
            ItemStack stack = new ItemStack(Items.AMETHYST_SHARD);
            stack.set(DataComponents.ITEM_NAME, Component.translatable("item.restless.requiem_offering"));
            stack.set(
                    DataComponents.CUSTOM_DATA,
                    CustomData.EMPTY.update(tag -> tag.putBoolean("requiem_offering", true)));
            return stack;
        }

        @Override
        public void start(Rite rite) {
            int want = config().integer("hoarder_chests");
            double turn = rite.player.getRandom().nextDouble() * Math.PI * 2;
            for (int i = 0; i < 12 && chests.size() < want; i++) {
                BlockPos at = spot(rite.level, rite.altar, turn + Math.PI * 2 * i / 12, 4 + i % 3)
                        .atY(rite.altar.getY());
                if (rite.level.getBlockState(at).isAir()
                        && rite.level.getBlockState(at.below()).isFaceSturdy(rite.level, at.below(), Direction.UP)
                        && !chests.contains(at)) {
                    rite.level.setBlock(at, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
                    chests.add(at);
                }
            }
            if (chests.isEmpty()) {
                return;
            }
            BlockPos hidden = chests.get(rite.player.getRandom().nextInt(chests.size()));
            for (BlockPos pos : chests) {
                if (rite.level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                    if (pos.equals(hidden)) {
                        chest.setItem(13, offering());
                    }
                    // A loot table not yet rolled is what a magpie feather points at; this one holds nothing.
                    chest.setLootTable(NOTHING);
                }
            }
        }

        @Override
        public boolean passed(Rite rite) {
            if (chests.isEmpty()) {
                return true;
            }
            Inventory inventory = rite.player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                if (isOffering(inventory.getItem(slot))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void end(Rite rite) {
            Inventory inventory = rite.player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                if (isOffering(inventory.getItem(slot))) {
                    inventory.setItem(slot, ItemStack.EMPTY);
                }
            }
            for (BlockPos pos : chests) {
                if (rite.level.getBlockEntity(pos) instanceof Container chest) {
                    for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                        if (isOffering(chest.getItem(slot))) {
                            chest.setItem(slot, ItemStack.EMPTY);
                        }
                    }
                    rite.level.removeBlock(pos, false);
                }
            }
            chests.clear();
        }
    }

    /** A bargain: the demon offers to skip the movement for two candles. Refusing, or saying nothing, passes. */
    static final class Demon implements Movement {
        @Override
        public void start(Rite rite) {
            rite.player.sendSystemMessage(Component.translatable(
                    "message.restless.requiem_bargain",
                    Component.translatable("message.restless.requiem_accept")
                            .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.RED)
                                    .withClickEvent(
                                            new net.minecraft.network.chat.ClickEvent.RunCommand("/requiem accept"))),
                    Component.translatable("message.restless.requiem_refuse")
                            .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GREEN)
                                    .withClickEvent(
                                            new net.minecraft.network.chat.ClickEvent.RunCommand("/requiem refuse"))),
                    config().integer("demon_cost")));
        }

        @Override
        public void tick(Rite rite, int n) {
            if (Boolean.TRUE.equals(rite.bargain)) {
                rite.skip = true;
            }
        }

        @Override
        public boolean passed(Rite rite) {
            return true;
        }
    }

    /** A swarm of tiny spiders, seen only by the player, runs at them; they must hold their ground by the altar. */
    static final class Boggart implements Movement {
        private boolean fled;

        @Override
        public void start(Rite rite) {
            Swarm.spiders(rite.player, rite.length);
        }

        @Override
        public void tick(Rite rite, int n) {
            if (rite.player.position().distanceToSqr(Vec3.atCenterOf(rite.altar))
                    > Math.pow(config().num("boggart_stay"), 2)) {
                fled = true;
            }
        }

        @Override
        public boolean passed(Rite rite) {
            return !fled;
        }
    }

    /** Three candles are snuffed, the rest change colors and decoys are set; the player relights the right ones. */
    static final class Trickster implements Movement {
        private final List<BlockPos> snuffed = new ArrayList<>();
        private final List<BlockPos> decoys = new ArrayList<>();
        private final Map<BlockPos, BlockState> colors = new java.util.HashMap<>();

        private static List<Block> candleBlocks() {
            return BuiltInRegistries.BLOCK.stream()
                    .filter(block -> block instanceof CandleBlock)
                    .toList();
        }

        @Override
        public void start(Rite rite) {
            var random = rite.player.getRandom();
            List<Block> candles = candleBlocks();
            List<BlockPos> ring = new ArrayList<>(rite.candles.lit());
            ring.addAll(rite.candles.out());
            for (BlockPos pos : ring) {
                BlockState state = rite.level.getBlockState(pos);
                if (state.getBlock() instanceof CandleBlock) {
                    colors.put(pos, state);
                    recolor(
                            rite.level,
                            pos,
                            candles.get(random.nextInt(candles.size())).withPropertiesOf(state));
                }
            }
            List<BlockPos> lit = new ArrayList<>(
                    rite.candles.lit().stream().filter(rite::burns).toList());
            Collections.shuffle(lit, new java.util.Random(random.nextLong()));
            for (BlockPos pos : lit.subList(0, Math.min(lit.size(), config().integer("trickster_snuffed")))) {
                rite.put(pos);
                snuffed.add(pos);
                rite.free.add(pos);
            }
            int radius = config().integer("candle_radius");
            for (int i = 0; i < 40 && decoys.size() < config().integer("trickster_decoys"); i++) {
                BlockPos at = rite.altar.offset(
                        random.nextInt(radius * 2 + 1) - radius, 0, random.nextInt(radius * 2 + 1) - radius);
                if (rite.level.getBlockState(at).isAir()
                        && rite.level.getBlockState(at.below()).isFaceSturdy(rite.level, at.below(), Direction.UP)
                        && !decoys.contains(at)
                        && !ring.contains(at)) {
                    rite.level.setBlock(
                            at,
                            candles.get(random.nextInt(candles.size()))
                                    .defaultBlockState()
                                    .setValue(AbstractCandleBlock.LIT, false),
                            Block.UPDATE_ALL);
                    decoys.add(at);
                }
            }
        }

        @Override
        public boolean passed(Rite rite) {
            return snuffed.stream().allMatch(rite::burns)
                    && decoys.stream()
                            .noneMatch(
                                    pos -> rite.burns(pos) || AbstractCandleBlock.isLit(rite.level.getBlockState(pos)));
        }

        @Override
        public void end(Rite rite) {
            for (BlockPos pos : decoys) {
                if (rite.level.getBlockState(pos).getBlock() instanceof CandleBlock) {
                    rite.level.removeBlock(pos, false);
                }
            }
            decoys.clear();
            colors.forEach((pos, original) -> {
                BlockState now = rite.level.getBlockState(pos);
                if (now.getBlock() instanceof CandleBlock) {
                    recolor(rite.level, pos, original.getBlock().withPropertiesOf(now));
                }
            });
            colors.clear();
            // Candles the player left out are lit again: the rite alone decides which are lost.
            for (BlockPos pos : snuffed) {
                BlockState now = rite.level.getBlockState(pos);
                if (now.getBlock() instanceof CandleBlock && !AbstractCandleBlock.isLit(now)) {
                    rite.level.setBlock(pos, now.setValue(AbstractCandleBlock.LIT, true), Block.UPDATE_ALL);
                }
            }
            rite.free.removeAll(snuffed);
            snuffed.clear();
        }
    }

    /** Changes a block without neighbour updates, so nothing attached to it pops off. */
    private static void recolor(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }
}
