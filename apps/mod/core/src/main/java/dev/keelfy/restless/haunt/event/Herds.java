package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Location;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.squid.GlowSquid;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Animals and villagers gathering, leading or holding still where there is no reason: at windows, in corners, at a
 * wall; a trader who should not be there; herds in the Nether; a pearl that lands elsewhere. A mob that is held loses
 * its AI and gets it back through {@link Restore}, so none is left frozen.
 */
final class Herds {
    private Herds() {}

    static List<HauntEvent> all() {
        return List.of(
                // 257: villagers wake at night and stand at their windows, looking at the player
                SimpleEvent.light(
                        "villagers_at_windows",
                        EventTag.MOBS,
                        player -> Where.overworld(player)
                                && player.level().isDarkOutside()
                                && !villagerPosts(player).isEmpty(),
                        Herds::windowWatch),
                // 260: a wandering trader at night, with trades nobody sells
                SimpleEvent.light(
                        "strange_trader",
                        EventTag.MOBS,
                        player ->
                                Where.overworld(player) && player.level().isDarkOutside() && traderSpot(player) != null,
                        Herds::trade),
                // 261: an iron golem walks into the house and stands in a corner, facing it
                SimpleEvent.light(
                        "golem_in_corner",
                        EventTag.MOBS,
                        player -> golem(player) != null && corner(player) != null,
                        Herds::standInCorner),
                // 267: dolphins swim ahead of the player toward the anchor, and wait when left behind
                SimpleEvent.light(
                        "dolphins_lead",
                        EventTag.WATER,
                        player -> player.isInWater() && !dolphins(player).isEmpty(),
                        Herds::lead),
                // 272: spiders gather outside, on the wall under a window, and hold there
                SimpleEvent.light(
                        "spiders_at_window",
                        EventTag.MOBS,
                        player -> spiderPosts(player).size()
                                >= Config.event("spiders_at_window").integer("min_spiders"),
                        Herds::gatherSpiders),
                // 278: a glow squid stops glowing for a while
                SimpleEvent.light(
                        "squid_goes_dark",
                        EventTag.WATER,
                        player -> !squids(player).isEmpty(),
                        Herds::darken),
                // 420: a thrown ender pearl lands somewhere else
                SimpleEvent.harmful(
                        "pearl_strays",
                        EventTag.ITEMS,
                        player -> player.getInventory().contains(stack -> stack.is(Items.ENDER_PEARL)),
                        Herds::arm),
                // 448: a hoglin herd charges the player and stops short, then lets go
                SimpleEvent.harmful(
                        "hoglins_charge",
                        EventTag.MOBS,
                        player -> Where.nether(player)
                                && herd(player).size()
                                        >= Config.event("hoglins_charge").integer("min_hoglins"),
                        Herds::charge));
    }

    private static <T extends Entity> List<T> near(ServerPlayer player, Class<T> type, String event) {
        return player.level().getEntitiesOfClass(type, new AABB(player.blockPosition()).inflate(Nearby.radius(event)));
    }

    /** A mob, the cell it should stand in, and what it faces there ({@code null}: the player). */
    private record Post(Mob mob, BlockPos spot, @Nullable Vec3 gaze) {}

    /**
     * Each mob walks to its spot; one that gets there loses its AI and faces its gaze until the hold is over. One that
     * does not arrive in time is left alone.
     */
    private static void gather(ServerPlayer player, List<Post> posts, Config.Section config) {
        ServerLevel level = player.level();
        int interval = config.integer("interval_ticks");
        int walk = config.integer("walk_ticks");
        int total = walk + config.integer("hold_ticks");
        double arrive = config.num("arrive_distance");
        Set<Mob> held = new HashSet<>();
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            int elapsed = n * interval;
            for (Post post : posts) {
                Mob mob = post.mob();
                if (!mob.isAlive()) {
                    continue;
                }
                if (!held.contains(mob)) {
                    Vec3 to = Vec3.atBottomCenterOf(post.spot());
                    if (mob.position().distanceToSqr(to) > arrive * arrive) {
                        if (elapsed < walk) {
                            mob.getNavigation().moveTo(to.x, to.y, to.z, config.num("speed"));
                        }
                        continue;
                    }
                    mob.getNavigation().stop();
                    mob.setNoAi(true);
                    held.add(mob);
                }
                mob.lookAt(EntityAnchorArgument.Anchor.EYES, post.gaze() == null ? p.getEyePosition() : post.gaze());
                mob.setYHeadRot(mob.getYRot());
                mob.setYBodyRot(mob.getYRot());
            }
            return elapsed < total;
        });
        Restore.after(level, total + interval * 2, force -> {
            held.forEach(mob -> mob.setNoAi(false));
            return true;
        });
    }

    // Windows: a glass block or pane with open air on both sides.

    private record Window(BlockPos glass, BlockPos inside, BlockPos outside) {}

    private static boolean isGlass(BlockState state) {
        return !state.isAir()
                && (state.is(BlockTags.IMPERMEABLE)
                        || BuiltInRegistries.BLOCK
                                .getKey(state.getBlock())
                                .getPath()
                                .endsWith("glass_pane"));
    }

    private static List<Window> windows(ServerPlayer player, String event) {
        ServerLevel level = player.level();
        List<Window> found = new ArrayList<>();
        for (BlockPos glass : Nearby.all(
                level, player.blockPosition(), Config.event(event).integer("window_radius"), Herds::isGlass)) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos inside = glass.relative(side);
                BlockPos outside = glass.relative(side.getOpposite());
                if (Place.open(level, inside) && Place.open(level, outside)) {
                    found.add(new Window(glass, inside, outside));
                }
            }
        }
        return found;
    }

    private static <T> List<T> shuffled(ServerPlayer player, List<T> list) {
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy, new Random(player.getRandom().nextLong()));
        return copy;
    }

    // 257

    /** Each villager (up to the limit) with the roofed floor cell at the foot of the nearest unclaimed window. */
    private static List<Post> villagerPosts(ServerPlayer player) {
        Config.Section config = Config.event("villagers_at_windows");
        ServerLevel level = player.level();
        List<Villager> villagers = shuffled(
                player,
                near(player, Villager.class, "villagers_at_windows").stream()
                        .filter(v -> v.isAlive() && !v.isNoAi() && !v.isBaby())
                        .toList());
        List<BlockPos> stands = new ArrayList<>();
        for (Window window : windows(player, "villagers_at_windows")) {
            BlockPos stand = Place.standing(level, window.inside());
            if (stand != null
                    && stand.getY() <= window.glass().getY()
                    && stand.getY() >= window.glass().getY() - 1
                    && !level.canSeeSky(stand)
                    && !stands.contains(stand)) {
                stands.add(stand);
            }
        }
        List<Post> posts = new ArrayList<>();
        for (Villager villager : villagers.subList(0, Math.min(villagers.size(), config.integer("max_villagers")))) {
            BlockPos best = stands.stream()
                    .min(Comparator.comparingDouble(s -> s.distSqr(villager.blockPosition())))
                    .orElse(null);
            if (best != null) {
                stands.remove(best);
                posts.add(new Post(villager, best, null));
            }
        }
        return posts;
    }

    private static void windowWatch(ServerPlayer player) {
        List<Post> posts = villagerPosts(player);
        for (Post post : posts) {
            if (post.mob() instanceof Villager villager && villager.isSleeping()) {
                villager.stopSleeping();
            }
        }
        gather(player, posts, Config.event("villagers_at_windows"));
    }

    // 260

    private static @Nullable BlockPos traderSpot(ServerPlayer player) {
        Config.Section config = Config.event("strange_trader");
        return Place.standingAhead(player, config.num("distance"), config.num("min_angle"), config.num("max_angle"));
    }

    private static @Nullable Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElse(null);
    }

    /** The trades from {@code offers}: buy item, buy count, sold item, sold count. A head is the player's own. */
    private static MerchantOffers offers(ServerPlayer player, Config.Section config) {
        MerchantOffers offers = new MerchantOffers();
        for (List<String> row : config.lines("offers")) {
            Item buy = item(row.get(0));
            Item sell = item(row.get(2));
            if (buy == null || sell == null) {
                continue;
            }
            ItemStack result = new ItemStack(sell, Integer.parseInt(row.get(3)));
            if (result.is(Items.PLAYER_HEAD)) {
                result.set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.getGameProfile()));
            }
            offers.add(new MerchantOffer(
                    new ItemCost(buy, Integer.parseInt(row.get(1))), result, config.integer("max_uses"), 0, 0.0F));
        }
        return offers;
    }

    private static boolean watched(ServerPlayer player, Mob mob, double lookDot) {
        Vec3 to = mob.getEyePosition().subtract(player.getEyePosition()).normalize();
        return player.getViewVector(1.0F).dot(to) > lookDot && player.hasLineOfSight(mob);
    }

    /** The trader stands facing the player; once seen and then looked away from, or after its lifetime, it is gone. */
    private static void trade(ServerPlayer player) {
        BlockPos spot = traderSpot(player);
        if (spot == null) {
            return;
        }
        Config.Section config = Config.event("strange_trader");
        ServerLevel level = player.level();
        WanderingTrader trader = EntityTypes.WANDERING_TRADER.spawn(level, spot, EntitySpawnReason.EVENT);
        if (trader == null) {
            return;
        }
        MerchantOffers offers = offers(player, config);
        if (offers.isEmpty()) {
            trader.discard();
            return;
        }
        trader.overrideOffers(offers);
        trader.setNoAi(true);
        int interval = config.integer("interval_ticks");
        boolean[] seen = {false};
        Scheduler.repeat(player, interval, interval, (p, n) -> {
            if (!trader.isAlive()) {
                return false;
            }
            trader.lookAt(EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
            trader.setYHeadRot(trader.getYRot());
            boolean looked = watched(p, trader, config.num("look_dot"));
            seen[0] |= looked;
            boolean over = p.level() != level
                    || (n + 1) * interval >= config.integer("lifetime_ticks")
                    || (seen[0] && !looked && trader.getTradingPlayer() == null);
            if (over) {
                level.sendParticles(
                        ParticleTypes.POOF, trader.getX(), trader.getY() + 1.0, trader.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
                trader.discard();
            }
            return !over;
        });
        Restore.after(level, config.integer("lifetime_ticks") + interval * 2, force -> {
            trader.discard();
            return true;
        });
    }

    // 261

    private static @Nullable IronGolem golem(ServerPlayer player) {
        return near(player, IronGolem.class, "golem_in_corner").stream()
                .filter(g -> g.isAlive() && !g.isNoAi() && g.getTarget() == null)
                .min(Comparator.comparingDouble(g -> g.distanceToSqr(player)))
                .orElse(null);
    }

    /** A floor cell in the player's roofed room with walls on two sides, and a point in the corner to face. */
    private static @Nullable Post corner(ServerPlayer player) {
        Config.Section config = Config.event("golem_in_corner");
        ServerLevel level = player.level();
        IronGolem golem = golem(player);
        if (golem == null) {
            return null;
        }
        int radius = config.integer("corner_radius");
        double min = config.num("min_distance");
        List<Post> corners = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                player.blockPosition().offset(-radius, -1, -radius),
                player.blockPosition().offset(radius, 1, radius))) {
            if (pos.distSqr(player.blockPosition()) < min * min
                    || !Place.floored(level, pos)
                    || !Place.open(level, pos.above())
                    || !Place.open(level, pos.above(2))
                    || level.canSeeSky(pos)) {
                continue;
            }
            for (Direction a : Direction.Plane.HORIZONTAL) {
                Direction b = a.getClockWise();
                if (Place.solid(level, pos.relative(a))
                        && Place.solid(level, pos.relative(a).above())
                        && Place.solid(level, pos.relative(b))
                        && Place.solid(level, pos.relative(b).above())) {
                    Vec3 gaze = Vec3.atBottomCenterOf(pos)
                            .add(0.0, 1.5, 0.0)
                            .add((a.getStepX() + b.getStepX()) * 3.0, 0.0, (a.getStepZ() + b.getStepZ()) * 3.0);
                    corners.add(new Post(golem, pos.immutable(), gaze));
                    break;
                }
            }
        }
        return corners.isEmpty() ? null : corners.get(player.getRandom().nextInt(corners.size()));
    }

    private static void standInCorner(ServerPlayer player) {
        Post post = corner(player);
        if (post != null) {
            gather(player, List.of(post), Config.event("golem_in_corner"));
        }
    }

    // 267

    private static List<Dolphin> dolphins(ServerPlayer player) {
        return near(player, Dolphin.class, "dolphins_lead").stream()
                .filter(d -> d.isAlive() && !d.isNoAi() && d.isInWater())
                .toList();
    }

    /** Every pulse each dolphin swims a stretch toward the anchor, or waits and faces the player if left behind. */
    private static void lead(ServerPlayer player) {
        Config.Section config = Config.event("dolphins_lead");
        ServerLevel level = player.level();
        List<Dolphin> dolphins = shuffled(player, dolphins(player));
        dolphins = dolphins.subList(0, Math.min(dolphins.size(), config.integer("max_dolphins")));
        List<Dolphin> leaders = dolphins;
        Vec3 anchor = Location.source(Nearby.randomAround(player, config.num("fallback_distance")));
        double wait = config.num("wait_distance");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            for (Dolphin dolphin : leaders) {
                if (!dolphin.isAlive() || !dolphin.isInWater()) {
                    continue;
                }
                if (dolphin.distanceToSqr(p) > wait * wait) {
                    dolphin.getNavigation().stop();
                    dolphin.lookAt(EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
                    continue;
                }
                Vec3 way = anchor.subtract(dolphin.position()).multiply(1.0, 0.0, 1.0);
                if (way.lengthSqr() < 1.0) {
                    continue;
                }
                for (double step = config.num("step"); step >= 2.0; step /= 2.0) {
                    Vec3 to = dolphin.position().add(way.normalize().scale(step));
                    if (level.getFluidState(BlockPos.containing(to)).is(FluidTags.WATER)) {
                        dolphin.getNavigation().moveTo(to.x, to.y, to.z, config.num("speed"));
                        break;
                    }
                }
                if (n % config.integer("call_every") == 0) {
                    Config.Tone tone = config.tone("sound");
                    level.playSound(
                            null,
                            dolphin,
                            SoundEvents.DOLPHIN_AMBIENT_WATER,
                            SoundSource.NEUTRAL,
                            tone.volume(),
                            tone.pitch());
                }
            }
            return n + 1 < config.integer("pulses");
        });
    }

    // 272

    /** Spiders that are not after anyone, each with a cell on the ground under a window, outside; empty if too few. */
    private static List<Post> spiderPosts(ServerPlayer player) {
        Config.Section config = Config.event("spiders_at_window");
        ServerLevel level = player.level();
        List<Spider> spiders = shuffled(
                player,
                near(player, Spider.class, "spiders_at_window").stream()
                        .filter(s -> s.isAlive() && !s.isNoAi() && s.getTarget() == null)
                        .toList());
        if (spiders.size() < config.integer("min_spiders")) {
            return List.of();
        }
        spiders = spiders.subList(0, Math.min(spiders.size(), config.integer("max_spiders")));
        List<Post> posts = new ArrayList<>();
        for (Window window : shuffled(player, windows(player, "spiders_at_window"))) {
            BlockPos ground = Place.standing(level, window.outside());
            if (ground == null
                    || !level.canSeeSky(ground)
                    || ground.getY() > window.glass().getY()
                    || window.glass().getY() - ground.getY() > config.integer("max_below")) {
                continue;
            }
            List<BlockPos> spots = new ArrayList<>(List.of(ground));
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos next = ground.relative(side);
                if (Place.floored(level, next) && Place.open(level, next.above())) {
                    spots.add(next);
                }
            }
            Vec3 gaze = Vec3.atCenterOf(window.glass());
            for (int i = 0; i < spiders.size(); i++) {
                posts.add(new Post(spiders.get(i), spots.get(i % spots.size()), gaze));
            }
            return posts;
        }
        return List.of();
    }

    private static void gatherSpiders(ServerPlayer player) {
        gather(player, spiderPosts(player), Config.event("spiders_at_window"));
    }

    // 278

    private static List<GlowSquid> squids(ServerPlayer player) {
        return near(player, GlowSquid.class, "squid_goes_dark").stream()
                .filter(s -> s.isAlive() && s.getDarkTicksRemaining() == 0)
                .toList();
    }

    private static void darken(ServerPlayer player) {
        ServerLevel level = player.level();
        int ticks = Config.event("squid_goes_dark").integer("dark_ticks");
        for (GlowSquid squid : squids(player)) {
            rewrite(squid, level, data -> data.putInt("DarkTicksRemaining", ticks));
        }
    }

    /**
     * Sets what has no setter: the entity is saved, {@code edit} changes the saved data, and the entity loads it back.
     */
    private static void rewrite(Entity entity, ServerLevel level, Consumer<CompoundTag> edit) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        entity.saveWithoutId(out);
        CompoundTag data = out.buildResult();
        edit.accept(data);
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
    }

    // 420

    /** For a while, the first pearl the player throws is moved to standing room elsewhere before it lands. */
    private static void arm(ServerPlayer player) {
        Config.Section config = Config.event("pearl_strays");
        ServerLevel level = player.level();
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (p.level() != level) {
                return false;
            }
            for (ThrownEnderpearl pearl : level.getEntitiesOfClass(
                    ThrownEnderpearl.class,
                    p.getBoundingBox().inflate(config.num("radius")),
                    e -> e.getOwner() != null && e.getOwner().getUUID().equals(p.getUUID()))) {
                BlockPos spot = Place.spawnSpot(p, config.integer("min_distance"), config.integer("max_distance"));
                if (spot != null) {
                    pearl.setPos(spot.getX() + 0.5, spot.getY() + config.num("drop"), spot.getZ() + 0.5);
                    pearl.setDeltaMovement(0.0, -config.num("fall_speed"), 0.0);
                    return false;
                }
            }
            return n * interval < config.integer("armed_ticks");
        });
    }

    // 448

    private static List<Hoglin> herd(ServerPlayer player) {
        double stop = Config.event("hoglins_charge").num("stop_distance");
        return near(player, Hoglin.class, "hoglins_charge").stream()
                .filter(h -> h.isAlive() && !h.isBaby() && !h.isNoAi() && h.distanceToSqr(player) > stop * stop)
                .toList();
    }

    /**
     * They are set on the player until each is within the stop distance; there it freezes, faces the player, and later
     * backs off.
     */
    private static void charge(ServerPlayer player) {
        Config.Section config = Config.event("hoglins_charge");
        ServerLevel level = player.level();
        List<Hoglin> herd = herd(player);
        int interval = config.integer("interval_ticks");
        int total = config.integer("charge_ticks") + config.integer("hold_ticks");
        double stop = config.num("stop_distance");
        Set<Hoglin> halted = new HashSet<>();
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            for (Hoglin hoglin : herd) {
                if (!hoglin.isAlive()) {
                    continue;
                }
                if (!halted.contains(hoglin) && hoglin.distanceToSqr(p) <= stop * stop) {
                    hoglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                    hoglin.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                    hoglin.getNavigation().stop();
                    hoglin.setNoAi(true);
                    halted.add(hoglin);
                }
                if (halted.contains(hoglin)) {
                    hoglin.lookAt(EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
                    hoglin.setYHeadRot(hoglin.getYRot());
                } else if (n * interval < config.integer("charge_ticks")) {
                    hoglin.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, p, interval * 3L);
                }
            }
            return n * interval < total;
        });
        Restore.after(level, total + interval * 2, force -> {
            for (Hoglin hoglin : halted) {
                hoglin.setNoAi(false);
                if (!force && player.isAlive()) {
                    hoglin.getBrain()
                            .setMemoryWithExpiry(
                                    MemoryModuleType.AVOID_TARGET, player, config.integer("retreat_ticks"));
                }
            }
            return true;
        });
    }
}
