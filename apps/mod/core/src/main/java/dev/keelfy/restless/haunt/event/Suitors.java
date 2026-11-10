package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.HauntSounds;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.event.kit.Figure;
import dev.keelfy.restless.haunt.event.kit.Nearby;
import dev.keelfy.restless.haunt.event.kit.Place;
import dev.keelfy.restless.haunt.event.kit.SimpleEvent;
import dev.keelfy.restless.haunt.recall.Said;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The banshee, who has taken a liking to the player: letters and gifts on the bed, a song at the window, jealousy. */
final class Suitors {
    private Suitors() {}

    static List<HauntEvent> all() {
        return List.of(
                // banshee: a letter on the bed
                SimpleEvent.light(
                        "banshee_letter",
                        EventTag.ITEMS,
                        player -> night(player)
                                && bed(player, "banshee_letter") != null
                                && Said.left(
                                        player,
                                        "banshee_letter",
                                        Config.event("banshee_letter").texts("pages")),
                        Suitors::letter),
                // banshee: a small gift on the bed
                SimpleEvent.light(
                        "banshee_gift",
                        EventTag.ITEMS,
                        player -> bed(player, "banshee_gift") != null
                                && Said.left(player, "banshee_gift", gifts(), String::valueOf),
                        Suitors::gift),
                // banshee: a soft song from outside the window
                SimpleEvent.light(
                        "banshee_serenade",
                        EventTag.SOUND,
                        player -> night(player) && window(player) != null,
                        Suitors::serenade),
                // banshee: whoever else is near the player shivers
                SimpleEvent.light("banshee_jealous", EventTag.MOBS, player -> rival(player) != null, Suitors::shiver));
    }

    private static boolean night(ServerPlayer player) {
        return !player.level().isBrightOutside();
    }

    private static List<List<String>> gifts() {
        return Config.event("banshee_gift").lines("gifts");
    }

    // ---- the bed

    /** The head of a free bed with room above it for something to lie on. */
    private static @Nullable BlockPos bed(ServerPlayer player, String event) {
        ServerLevel level = player.level();
        List<BlockPos> beds = Nearby.all(
                        player,
                        event,
                        state -> state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD)
                .stream()
                .filter(pos -> level.isEmptyBlock(pos.above()))
                .toList();
        return beds.isEmpty() ? null : beds.get(player.getRandom().nextInt(beds.size()));
    }

    /** Lays the stack on the bed, still, as something left there and not thrown. */
    private static void lay(ServerPlayer player, BlockPos bed, ItemStack stack) {
        ItemEntity item = new ItemEntity(
                player.level(), bed.getX() + 0.5, bed.getY() + 0.7, bed.getZ() + 0.5, stack, 0.0, 0.0, 0.0);
        player.level().addFreshEntity(item);
    }

    private static void letter(ServerPlayer player) {
        BlockPos bed = bed(player, "banshee_letter");
        Config.Section config = Config.event("banshee_letter");
        String page = Said.pick(player, "banshee_letter", config.texts("pages"));
        if (bed == null || page == null) {
            return;
        }
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(
                DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(
                        Filterable.passThrough(config.text("title")
                                .formatted(player.getGameProfile().name())),
                        Figure.nickname(player),
                        0,
                        List.of(Filterable.passThrough(Component.literal(page))),
                        true));
        lay(player, bed, book);
    }

    private static void gift(ServerPlayer player) {
        BlockPos bed = bed(player, "banshee_gift");
        List<String> chosen = bed == null ? null : Said.pick(player, "banshee_gift", gifts(), String::valueOf);
        if (chosen == null) {
            return;
        }
        Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.parse(chosen.get(0)));
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(chosen.get(1)));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(chosen.get(2)))));
        lay(player, bed, stack);
    }

    // ---- the window

    private static boolean glass(BlockState state) {
        return Place.glass(state);
    }

    private static @Nullable BlockPos window(ServerPlayer player) {
        List<BlockPos> panes = Nearby.all(player, "banshee_serenade", Suitors::glass);
        return panes.isEmpty() ? null : panes.get(player.getRandom().nextInt(panes.size()));
    }

    /** Her song comes from the far side of the glass, two blocks out from the player's side of it. */
    private static void serenade(ServerPlayer player) {
        BlockPos pane = window(player);
        if (pane == null) {
            return;
        }
        Vec3 glass = Vec3.atCenterOf(pane);
        Vec3 out = glass.subtract(player.position()).multiply(1, 0, 1);
        out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
        Vec3 at = glass.add(out.scale(Config.event("banshee_serenade").num("outside")));
        Config.Tone tone = Config.event("banshee_serenade").tone("sound");
        player.level()
                .playSound(
                        null,
                        at.x,
                        at.y,
                        at.z,
                        HauntSounds.MOAN,
                        net.minecraft.sounds.SoundSource.HOSTILE,
                        tone.volume(),
                        tone.pitch());
    }

    // ---- the rival

    private static boolean rival(LivingEntity mob) {
        return mob instanceof Villager || mob instanceof Wolf || mob instanceof Parrot || isCat(mob);
    }

    private static boolean isCat(LivingEntity mob) {
        return mob instanceof net.minecraft.world.entity.animal.feline.Cat;
    }

    private static @Nullable LivingEntity rival(ServerPlayer player) {
        List<LivingEntity> near = player.level()
                .getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox()
                                .inflate(Config.event("banshee_jealous").num("radius")),
                        mob -> rival(mob) && !(mob instanceof Animal animal && animal.isBaby()));
        return near.isEmpty() ? null : near.get(player.getRandom().nextInt(near.size()));
    }

    private static SoundEvent reaction(LivingEntity mob) {
        if (mob instanceof Villager) {
            return SoundEvents.VILLAGER_NO;
        }
        if (mob instanceof Wolf) {
            return SoundEvents.WOLF_SOUNDS
                    .get(net.minecraft.world.entity.animal.wolf.WolfSoundVariants.SoundSet.CLASSIC)
                    .adultSounds()
                    .growlSound()
                    .value();
        }
        if (mob instanceof Parrot) {
            return SoundEvents.PARROT_IMITATE_GHAST;
        }
        return SoundEvents.CAT_SOUNDS
                .get(net.minecraft.world.entity.animal.feline.CatSoundVariants.SoundSet.CLASSIC)
                .adultSounds()
                .hissSound()
                .value();
    }

    /**
     * The mob shakes with frost as in powder snow for a few seconds, and a villager shakes its head. The frost can hurt
     * a little, so its health is put back after.
     */
    private static void shiver(ServerPlayer player) {
        LivingEntity mob = rival(player);
        if (mob == null) {
            return;
        }
        Config.Section config = Config.event("banshee_jealous");
        float health = mob.getHealth();
        player.level()
                .playSound(
                        null,
                        mob.getX(),
                        mob.getY(),
                        mob.getZ(),
                        reaction(mob),
                        net.minecraft.sounds.SoundSource.NEUTRAL,
                        1.0F,
                        1.0F);
        if (mob instanceof Villager villager) {
            villager.setUnhappyCounter(config.integer("head_shake_ticks"));
        }
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (!mob.isAlive()) {
                return false;
            }
            if (n * interval >= config.integer("seconds") * 20) {
                mob.setTicksFrozen(0);
                mob.setHealth(Math.max(mob.getHealth(), health));
                return false;
            }
            mob.setTicksFrozen(mob.getTicksRequiredToFreeze() + 1);
            return true;
        });
    }
}
