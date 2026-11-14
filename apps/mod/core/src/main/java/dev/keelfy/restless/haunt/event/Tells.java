package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.event.kit.SimpleEvent;
import dev.keelfy.restless.haunt.recall.Said;
import dev.keelfy.restless.journal.Journal;
import dev.keelfy.restless.journal.Notebook;
import dev.keelfy.restless.state.HauntData;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Tells: one small sign per habit that only a spirit with that habit gives ({@code habit} in the event's config, see
 * {@link HauntEvent#habit}). Each is a light event of low impact, and each notes what was seen in the journal as a
 * {@link Notebook.Kind#TELL}, so watching becomes a way to learn a spirit's habits.
 */
final class Tells {
    private Tells() {}

    static List<HauntEvent> all() {
        return List.of(tell("restless:tell_cold_touch", EventTag.AFFLICTION, SimpleEvent.ALWAYS, player -> {
            // Breath that frosts in front of the face, whatever the weather.
            Vec3 mouth = player.getEyePosition().add(player.getLookAngle().scale(0.4));
            particles(player, ParticleTypes.SNOWFLAKE, mouth, 0.15, 12);
            particles(player, ParticleTypes.CLOUD, mouth, 0.1, 4);
            sound(player, SoundEvents.POWDER_SNOW_STEP, "restless:tell_cold_touch");
        }));
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

    /** A quiet sound at the player, pitched by {@code events.<id>.sound}. */
    static void sound(ServerPlayer player, SoundEvent sound, String id) {
        Config.Section tone = Config.event(id).section("sound");
        player.level()
                .playSound(null, player.blockPosition(), sound, SoundSource.AMBIENT, (float) tone.num("volume"), (float)
                        tone.num("pitch"));
    }
}
