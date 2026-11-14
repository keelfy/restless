package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.effect.DimPayload;
import dev.keelfy.restless.haunt.event.kit.Nearby;
import dev.keelfy.restless.haunt.event.kit.Place;
import dev.keelfy.restless.haunt.event.kit.SimpleEvent;
import dev.keelfy.restless.haunt.event.kit.Swaps;
import dev.keelfy.restless.haunt.recall.Said;
import dev.keelfy.restless.journal.Journal;
import dev.keelfy.restless.journal.Notebook;
import dev.keelfy.restless.state.HauntData;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Tells: one small sign per habit that only a spirit with that habit gives ({@code habit} in the event's config, see
 * {@link HauntEvent#habit}). Each is a light event of low impact, and each notes what was seen in the journal as a
 * {@link Notebook.Kind#TELL}, so watching becomes a way to learn a spirit's habits.
 */
final class Tells {
    private Tells() {}

    static List<HauntEvent> all() {
        return List.of(
                tell("restless:tell_cold_touch", EventTag.AFFLICTION, SimpleEvent.ALWAYS, player -> {
                    // Breath that frosts in front of the face, whatever the weather.
                    Vec3 mouth =
                            player.getEyePosition().add(player.getLookAngle().scale(0.4));
                    particles(player, ParticleTypes.SNOWFLAKE, mouth, 0.15, 12);
                    particles(player, ParticleTypes.CLOUD, mouth, 0.1, 4);
                    sound(player, SoundEvents.POWDER_SNOW_STEP, "restless:tell_cold_touch");
                }),
                restlessHands(),
                doorSlammer(),
                meddler(),
                thrower(),
                stacker(),
                knocker(),
                lightEater(),
                shadowCaster(),
                blinder(),
                hearthKiller(),
                cornerLurker());
    }

    private static final String LIGHT_EATER = "restless:tell_light_eater";
    private static final String HEARTH_KILLER = "restless:tell_hearth_killer";

    /** An item taken up from the floor beside the player, with nothing there. */
    private static HauntEvent restlessHands() {
        String id = "restless:tell_restless_hands";
        return tell(id, EventTag.ITEMS, SimpleEvent.ALWAYS, player -> {
            Vec3 at = Nearby.around(player, 1.2, 90.0);
            particles(player, ParticleTypes.POOF, at, 0.1, 3);
            soundAt(player, SoundEvents.ITEM_PICKUP, at, id);
        });
    }

    /** A door slammed shut somewhere out of sight. */
    private static HauntEvent doorSlammer() {
        String id = "restless:tell_door_slammer";
        return tell(id, EventTag.DOORS, SimpleEvent.ALWAYS, player -> {
            soundAt(player, SoundEvents.WOODEN_DOOR_CLOSE, Nearby.around(player, 7.0, 150.0), id);
        });
    }

    /** The ground beside the player settles with the sound and dust of a block set down. */
    private static HauntEvent meddler() {
        String id = "restless:tell_meddler";
        return tell(id, EventTag.BLOCKS, SimpleEvent.ALWAYS, player -> {
            BlockPos floor = BlockPos.containing(
                            Nearby.around(player, 3.0, player.getRandom().nextInt(360)))
                    .atY(player.blockPosition().getY() - 1);
            BlockState state = player.level().getBlockState(floor);
            if (state.isAir()) {
                floor = player.blockPosition().below();
                state = player.level().getBlockState(floor);
            }
            Vec3 top = Vec3.atCenterOf(floor).add(0.0, 0.6, 0.0);
            particles(player, new BlockParticleOption(ParticleTypes.BLOCK, state), top, 0.3, 10);
            soundAt(player, state.getSoundType().getPlaceSound(), top, id);
        });
    }

    /** A small stone sails past with no hand behind it. */
    private static HauntEvent thrower() {
        String id = "restless:tell_thrower";
        return tell(id, EventTag.ITEMS, SimpleEvent.ALWAYS, player -> {
            Vec3 at = Nearby.around(player, 1.5, 180.0).add(0.0, 0.8, 0.0);
            particles(player, new ItemParticleOption(ParticleTypes.ITEM, Items.COBBLESTONE), at, 0.15, 8);
            soundAt(player, SoundEvents.SNOWBALL_THROW, at, id);
        });
    }

    /** Two blocks stand one on the other where there were none, then are gone. */
    private static HauntEvent stacker() {
        String id = "restless:tell_stacker";
        return tell(id, EventTag.BLOCKS, SimpleEvent.ALWAYS, player -> {
            BlockPos at = Place.standingAhead(player, 4.0, 0.0, 40.0);
            if (at == null) {
                // No standing room found: the pair goes at the player's own level, if that is free.
                at = BlockPos.containing(
                        Nearby.around(player, 3.0, 0.0).x, player.getY(), Nearby.around(player, 3.0, 0.0).z);
                if (!player.level().isEmptyBlock(at) || !player.level().isEmptyBlock(at.above())) {
                    at = null;
                }
            }
            if (at != null) {
                Swaps.show(
                        player,
                        List.of(at, at.above()),
                        Blocks.STONE_BRICKS.defaultBlockState(),
                        Config.event(id).integer("ticks"));
                soundAt(player, SoundEvents.STONE_PLACE, Vec3.atCenterOf(at), id);
            }
        });
    }

    /** Three knocks from inside a wall nearby. */
    private static HauntEvent knocker() {
        String id = "restless:tell_knocker";
        return tell(id, EventTag.DOORS, SimpleEvent.ALWAYS, player -> {
            Vec3 at = Nearby.around(player, 3.0, player.getRandom().nextInt(360));
            int gap = Config.event(id).integer("gap_ticks");
            soundAt(player, SoundEvents.WOOD_HIT, at, id);
            Scheduler.later(player, gap, p -> soundAt(p, SoundEvents.WOOD_HIT, at, id));
            Scheduler.later(player, gap * 2, p -> soundAt(p, SoundEvents.WOOD_HIT, at, id));
        });
    }

    private static boolean torch(BlockState state) {
        return state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH)
                || state.is(Blocks.SOUL_WALL_TORCH);
    }

    /** A torch flame near the player goes out in a curl of smoke for a moment. */
    private static HauntEvent lightEater() {
        return tell(LIGHT_EATER, EventTag.LIGHTS, player -> Nearby.any(player, LIGHT_EATER, Tells::torch), player -> {
            BlockPos pos = Nearby.random(player, LIGHT_EATER, Tells::torch);
            if (pos != null) {
                Swaps.show(
                        player,
                        pos,
                        Blocks.AIR.defaultBlockState(),
                        Config.event(LIGHT_EATER).integer("ticks"));
                Vec3 flame = Vec3.atCenterOf(pos).add(0.0, 0.2, 0.0);
                particles(player, ParticleTypes.SMOKE, flame, 0.1, 8);
                soundAt(player, SoundEvents.FIRE_EXTINGUISH, flame, LIGHT_EATER);
            }
        });
    }

    /** A figure of smoke, tall and thin, stands a few blocks off and thins away. */
    private static HauntEvent shadowCaster() {
        String id = "restless:tell_shadow_caster";
        return tell(id, EventTag.PRESENCE, SimpleEvent.ALWAYS, player -> {
            Vec3 foot = Nearby.around(player, 5.0, 150.0 + player.getRandom().nextInt(60));
            Vec3 base = new Vec3(foot.x, player.getY(), foot.z);
            for (int i = 0; i < 7; i++) {
                particles(player, ParticleTypes.LARGE_SMOKE, base.add(0.0, i * 0.3, 0.0), 0.08, 2);
            }
            soundAt(player, SoundEvents.SOUL_ESCAPE.value(), base, id);
        });
    }

    /** The view goes dark for a moment. */
    private static HauntEvent blinder() {
        String id = "restless:tell_blinder";
        return tell(id, EventTag.AFFLICTION, SimpleEvent.ALWAYS, player -> {
            Config.Section config = Config.event(id);
            player.connection.send(new ClientboundCustomPayloadPacket(new DimPayload(
                    config.integer("fall_ticks"), config.integer("hold_ticks"), config.integer("rise_ticks"), 1, (float)
                            config.num("peak"))));
        });
    }

    private static boolean hearth(BlockState state) {
        return (state.getBlock() instanceof CampfireBlock || state.getBlock() instanceof AbstractFurnaceBlock)
                && state.hasProperty(BlockStateProperties.LIT)
                && state.getValue(BlockStateProperties.LIT);
    }

    /** A lit campfire or furnace nearby gutters out for a moment. */
    private static HauntEvent hearthKiller() {
        return tell(
                HEARTH_KILLER, EventTag.LIGHTS, player -> Nearby.any(player, HEARTH_KILLER, Tells::hearth), player -> {
                    BlockPos pos = Nearby.random(player, HEARTH_KILLER, Tells::hearth);
                    if (pos != null) {
                        BlockState out = player.level().getBlockState(pos).setValue(BlockStateProperties.LIT, false);
                        Swaps.show(player, pos, out, Config.event(HEARTH_KILLER).integer("ticks"));
                        Vec3 top = Vec3.atCenterOf(pos).add(0.0, 0.6, 0.0);
                        particles(player, ParticleTypes.CAMPFIRE_COSY_SMOKE, top, 0.15, 6);
                        soundAt(player, SoundEvents.FIRE_EXTINGUISH, top, HEARTH_KILLER);
                    }
                });
    }

    /** Two dark points, a hand apart, hang at head height far off and are gone. */
    private static HauntEvent cornerLurker() {
        String id = "restless:tell_corner_lurker";
        return tell(id, EventTag.PRESENCE, SimpleEvent.ALWAYS, player -> {
            Vec3 at = Nearby.around(player, 6.0, 120.0 + player.getRandom().nextInt(120));
            Vec3 eyes = new Vec3(at.x, player.getEyeY() - 0.2, at.z);
            Vec3 across = new Vec3(
                            -Math.cos(Math.toRadians(player.getYRot())),
                            0.0,
                            -Math.sin(Math.toRadians(player.getYRot())))
                    .scale(0.15);
            particles(player, ParticleTypes.SQUID_INK, eyes.add(across), 0.01, 2);
            particles(player, ParticleTypes.SQUID_INK, eyes.subtract(across), 0.01, 2);
            particles(player, ParticleTypes.SMOKE, eyes, 0.3, 5);
        });
    }

    /**
     * A tell: after {@code action} the spirit's player notes {@code journal.restless.tell.<habit>}, the habit being
     * {@code events.<id>.habit}.
     */
    static HauntEvent tell(String id, EventTag tag, Predicate<ServerPlayer> condition, Consumer<ServerPlayer> action) {
        return SimpleEvent.light(id, tag, condition, player -> {
            action.accept(player);
            note(player, Config.event(id).text("habit"));
        });
    }

    /** Notes the tell of {@code habit} in the journal, about the spirit running it if one is. */
    static void note(ServerPlayer player, String habit) {
        UUID spirit = Said.current();
        HauntData.Haunting haunting = spirit == null
                ? null
                : HauntData.get(player.level().getServer()).find(spirit).orElse(null);
        Journal.note(player, Notebook.Kind.TELL, haunting, Component.translatable("journal.restless.tell." + habit));
    }

    /** Particles only this player sees. */
    static void particles(ServerPlayer player, ParticleOptions type, Vec3 at, double spread, int count) {
        float s = (float) spread;
        player.connection.send(
                new ClientboundLevelParticlesPacket(type, false, false, at.x, at.y, at.z, s, s, s, 0.01f, count));
    }

    /** A quiet sound at {@code at}, pitched by {@code events.<id>.sound}. */
    static void soundAt(ServerPlayer player, SoundEvent sound, Vec3 at, String id) {
        Config.Section tone = Config.event(id).section("sound");
        player.level().playSound(null, at.x, at.y, at.z, sound, SoundSource.AMBIENT, (float) tone.num("volume"), (float)
                tone.num("pitch"));
    }

    /** A quiet sound at the player, pitched by {@code events.<id>.sound}. */
    static void sound(ServerPlayer player, SoundEvent sound, String id) {
        Config.Section tone = Config.event(id).section("sound");
        player.level()
                .playSound(null, player.blockPosition(), sound, SoundSource.AMBIENT, (float) tone.num("volume"), (float)
                        tone.num("pitch"));
    }
}
