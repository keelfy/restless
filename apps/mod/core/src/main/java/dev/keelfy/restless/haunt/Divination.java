package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.spirit.Clue;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/**
 * Asking the spirits: the camera, the spirit box and the ouija board each read one clue off the spirits around the
 * player.
 */
public final class Divination {
    /**
     * What a clue showed: the spirit it came from and the trait it named, which for a liar may not be the spirit's own.
     */
    public record Reading(Sense.Spirit spirit, Trait trait, Random random) {}

    private Divination() {}

    public static Optional<Reading> read(ServerPlayer player, Clue clue) {
        List<Sense.Spirit> spirits =
                new ArrayList<>(Sense.of(player, HauntData.get(player.level().getServer())));
        Random random = new Random(player.getRandom().nextLong());
        Collections.shuffle(spirits, random);
        List<Trait> catalog = Traits.all();
        for (Sense.Spirit spirit : spirits) {
            Optional<Trait> trait = Clues.reveal(Traits.of(spirit.haunting().traits()), catalog, clue, random);
            if (trait.isPresent()) {
                return Optional.of(new Reading(spirit, trait.get(), random));
            }
        }
        return Optional.empty();
    }

    /**
     * The camera's photo: the spirit's type and one thing about it; empty when no spirit near the player shows in a
     * picture.
     */
    public static Optional<ItemStack> photograph(ServerPlayer player) {
        ServerLevel level = player.level();
        level.sendParticles(
                ParticleTypes.END_ROD, player.getX(), player.getEyeY(), player.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        level.playSound(
                null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.2f);
        Optional<Reading> reading = read(player, Clue.SILHOUETTE);
        if (reading.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.restless.camera_empty"), true);
            return Optional.empty();
        }
        HauntData.Haunting spirit = reading.get().spirit().haunting();
        Component type = Evolution.name(spirit);
        String hint = reading.get().trait().hint();
        Component weakness = Component.translatable(
                "focus.restless." + reading.get().trait().weakness().name().toLowerCase(java.util.Locale.ROOT));
        Journal.note(
                player,
                Notebook.Kind.PHOTO,
                spirit,
                spirit.spirit() + (spirit.evolved() ? "_evolved" : ""),
                type,
                hint,
                weakness);
        ItemStack photo = new ItemStack(Content.PHOTO.get());
        photo.set(
                DataComponents.LORE,
                new ItemLore(List.of(
                        Component.translatable("item.restless.photo.silhouette", type),
                        Component.translatable("item.restless.photo.hint", hint),
                        Component.translatable("item.restless.photo.weakness", weakness))));
        return Optional.of(photo);
    }

    /**
     * Called when a player speaks: a spirit box close by answers a moment later, or hisses when no spirit has a voice.
     */
    public static void ask(ServerPlayer player) {
        Config.Section config = Config.section("spirit_box");
        boolean[] near = {false};
        Scan.each(
                player.level(),
                player.blockPosition(),
                config.integer("radius"),
                (pos, state) -> near[0] |= state.is(Content.SPIRIT_BOX.get()));
        if (!near[0]) {
            return;
        }
        Optional<Reading> reading = read(player, Clue.VOICE);
        Optional<String> word = reading.map(
                r -> r.trait().words().get(r.random().nextInt(r.trait().words().size())));
        Component answer = word.<Component>map(w -> Component.translatable("message.restless.spirit_box", w))
                .orElse(Component.translatable("message.restless.spirit_box_static"));
        Scheduler.later(player, config.integer("delay_ticks"), p -> {
            p.sendSystemMessage(answer);
            word.ifPresent(w ->
                    Journal.note(p, Notebook.Kind.VOICE, reading.get().spirit().haunting(), w));
        });
        // Only the shaken hear where the spirit lies; rounded, so the grave still has to be looked for.
        Optional<BlockPos> grave = reading.filter(r -> Sanity.shaken(player))
                .flatMap(r -> Graves.of(
                        HauntData.get(player.level().getServer()),
                        r.spirit().haunting().uuid()));
        grave.ifPresent(pos -> Scheduler.later(player, config.integer("delay_ticks") * 2, p -> {
            int x = Math.round(pos.getX() / 10f) * 10;
            int z = Math.round(pos.getZ() / 10f) * 10;
            p.sendSystemMessage(Component.translatable("message.restless.spirit_box_grave", x, z));
            Journal.note(p, Notebook.Kind.GRAVE_WHISPER, reading.get().spirit().haunting(), x, z);
        }));
    }

    /** The planchette spells out a trait, one letter at a time on the action bar. */
    public static void spell(ServerPlayer player) {
        Config.Section config = Config.section("ouija");
        Optional<Reading> reading = read(player, Clue.PLANCHETTE);
        if (reading.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.restless.ouija_still"), true);
            return;
        }
        String text = reading.get().trait().hint().toUpperCase(java.util.Locale.ROOT);
        Scheduler.repeat(player, config.integer("delay_ticks"), config.integer("letter_ticks"), (p, n) -> {
            p.sendSystemMessage(Component.literal(text.substring(0, n + 1)), true);
            p.level()
                    .playSound(
                            null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.4f, 1.5f);
            if (n + 1 >= text.length()) {
                Journal.note(
                        p,
                        Notebook.Kind.PLANCHETTE,
                        reading.get().spirit().haunting(),
                        reading.get().trait().hint());
                return false;
            }
            return true;
        });
    }
}
