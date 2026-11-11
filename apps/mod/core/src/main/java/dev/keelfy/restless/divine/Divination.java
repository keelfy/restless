package dev.keelfy.restless.divine;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.item.charm.Advanced;
import dev.keelfy.restless.haunt.Scan;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.journal.Journal;
import dev.keelfy.restless.journal.Notebook;
import dev.keelfy.restless.photo.DevelopPayload;
import dev.keelfy.restless.photo.ShutterPayload;
import dev.keelfy.restless.sanity.Sanity;
import dev.keelfy.restless.spirit.Clue;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import dev.keelfy.restless.spirit.land.Graves;
import dev.keelfy.restless.spirit.life.Evolution;
import dev.keelfy.restless.state.HauntData;
import dev.keelfy.restless.state.Session;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/**
 * Asking the spirits: the camera, the spirit box and the ouija board each read one clue off the spirits around the
 * player.
 */
public final class Divination {
    /**
     * Who each undeveloped photo shows, by film, so its picture can be pasted into the journal; the oldest are dropped.
     */
    private static final Map<Integer, UUID> SUBJECTS = Session.SERVER.keep(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, UUID> eldest) {
            return size() > 64;
        }
    });

    /** When each player's ouija board answers again, in game time. */
    private static final Map<UUID, Long> ASKED = Session.SERVER.keep(new java.util.HashMap<>());

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
            List<Trait> owned = Traits.of(spirit.haunting().traits());
            boolean honest = Advanced.truthful(player, clue) && owned.stream().anyMatch(Trait::lies);
            Optional<Trait> trait = Clues.reveal(owned, catalog, clue, random, honest);
            if (trait.isPresent()) {
                if (honest) {
                    Advanced.truthSpent(player);
                }
                return Optional.of(new Reading(spirit, trait.get(), random));
            }
        }
        return Optional.empty();
    }

    /**
     * The camera's photo: only the spirit's type. Its traits and what it fears are for the other tools and the altar to
     * tell; empty when no spirit near the player shows in a picture.
     */
    public static Optional<ItemStack> photograph(ServerPlayer player) {
        ServerLevel level = player.level();
        // Not to the photographer: the sparks hang at the lens and would be in the frame.
        for (ServerPlayer viewer : level.players()) {
            if (viewer != player) {
                level.sendParticles(
                        viewer,
                        ParticleTypes.END_ROD,
                        false,
                        true,
                        player.getX(),
                        player.getEyeY(),
                        player.getZ(),
                        6,
                        0.3,
                        0.3,
                        0.3,
                        0.02);
            }
        }
        level.playSound(
                null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.2f);
        Optional<Reading> reading = read(player, Clue.SILHOUETTE);
        if (reading.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.restless.camera_empty"), true);
            return Optional.empty();
        }
        HauntData.Haunting spirit = reading.get().spirit().haunting();
        Component type = Evolution.name(spirit);
        Journal.tagged(
                player, Notebook.Kind.PHOTO, spirit, spirit.spirit() + (spirit.evolved() ? "_evolved" : ""), type);
        ItemStack photo = new ItemStack(Content.PHOTO.get());
        photo.set(
                DataComponents.LORE,
                new ItemLore(List.of(Component.translatable("item.restless.photo.silhouette", type))));
        int film = reading.get().random().nextInt();
        photo.set(Content.FILM.get(), film);
        synchronized (SUBJECTS) {
            SUBJECTS.put(film, spirit.uuid());
        }
        player.connection.send(new ClientboundCustomPayloadPacket(new ShutterPayload(
                film,
                spirit.spirit(),
                spirit.seed(),
                Optional.ofNullable(reading.get().spirit().at()))));
        return Optional.of(photo);
    }

    /**
     * Puts the picture a client took into its undeveloped photo. Only the photographer's own inventory is searched, and
     * each film develops once.
     */
    public static void develop(ServerPlayer player, DevelopPayload payload) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (develop(player, inventory.getItem(slot), payload)) {
                return;
            }
        }
        develop(player, player.containerMenu.getCarried(), payload);
    }

    private static boolean develop(ServerPlayer player, ItemStack stack, DevelopPayload payload) {
        Integer film = stack.get(Content.FILM.get());
        if (!stack.is(Content.PHOTO.get()) || film == null || film != payload.film()) {
            return false;
        }
        stack.remove(Content.FILM.get());
        stack.set(Content.PICTURE.get(), payload.picture());
        UUID subject;
        synchronized (SUBJECTS) {
            subject = SUBJECTS.remove(film);
        }
        if (subject != null) {
            Journal.paste(player, subject, payload.picture());
        }
        return true;
    }

    /** Whether a spirit box is close enough to the player to hear them. */
    public static boolean listening(ServerPlayer player) {
        return box(player).isPresent();
    }

    /** The spirit box nearest the player that hears them. */
    private static Optional<BlockPos> box(ServerPlayer player) {
        BlockPos[] nearest = {null};
        Scan.each(
                player.level(),
                player.blockPosition(),
                Config.section("spirit_box").integer("radius"),
                (pos, state) -> {
                    if (state.is(Content.SPIRIT_BOX.get())
                            && (nearest[0] == null
                                    || pos.distSqr(player.blockPosition())
                                            < nearest[0].distSqr(player.blockPosition()))) {
                        nearest[0] = pos.immutable();
                    }
                });
        return Optional.ofNullable(nearest[0]);
    }

    /**
     * Called when a player says something: a spirit box close by answers a moment later, aloud, or hisses when no
     * spirit has a voice. The answer bears one word of a trait; what was asked only changes the words around it, see
     * {@link Reply}. A line that could not be made out is empty.
     */
    public static void ask(ServerPlayer player, Heard heard) {
        Config.Section config = Config.section("spirit_box");
        Optional<BlockPos> box = box(player);
        if (box.isEmpty()) {
            return;
        }
        Optional<Reading> reading = read(player, Clue.VOICE);
        Optional<String> word = reading.map(
                r -> r.trait().words().get(r.random().nextInt(r.trait().words().size())));
        Optional<Reply> reply = word.map(w -> Reply.of(heard, w, reading.get().random()));
        Component answer = reply.<Component>map(r -> Component.translatable("message.restless.spirit_box", r.text()))
                .orElse(Component.translatable("message.restless.spirit_box_static"));
        SpiritBoxPayload voice = reply.map(r -> r.voice(box.get())).orElse(Reply.hiss(box.get()));
        Scheduler.later(player, config.integer("delay_ticks"), p -> {
            p.sendSystemMessage(answer);
            p.connection.send(new ClientboundCustomPayloadPacket(voice));
            word.ifPresent(w ->
                    Journal.note(p, Notebook.Kind.VOICE, reading.get().spirit().haunting(), word(w)));
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

    /** A spirit box word in the listener's language; a word added in config without a translation stays as written. */
    public static Component word(String word) {
        return Component.translatableWithFallback("word.restless." + word, word);
    }

    /** What the trait is, in a few words, for the ouija board and dreams. */
    public static Component hint(Trait trait) {
        return Component.translatable("trait.restless." + trait.id() + ".hint");
    }

    /** What the planchette spells for a trait: one of its phrases, a word or a sentence, never the trait named. */
    public static Component phrase(Reading reading) {
        return Component.translatable(phraseKey(reading));
    }

    private static String phraseKey(Reading reading) {
        List<String> phrases = reading.trait().planchette();
        return "planchette.restless." + phrases.get(reading.random().nextInt(phrases.size()));
    }

    /**
     * The planchette spells a phrase of a trait, one letter at a time on the action bar. A spirit is not always
     * listening: the board is still some of the time and for a while after every try, so asking again and again gets
     * nowhere, and a trait takes several answers to tell.
     */
    public static void spell(ServerPlayer player) {
        Config.Section config = Config.section("ouija");
        long now = player.level().getGameTime();
        Long free = ASKED.get(player.getUUID());
        if (free != null && now < free) {
            player.sendSystemMessage(Component.translatable("message.restless.ouija_wait"), true);
            return;
        }
        ASKED.put(player.getUUID(), now + config.integer("cooldown_ticks"));
        Optional<Reading> reading = player.getRandom().nextDouble() < config.num("silence_chance")
                ? Optional.empty()
                : read(player, Clue.PLANCHETTE);
        if (reading.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.restless.ouija_still"), true);
            return;
        }
        String key = phraseKey(reading.get());
        String text = Language.getInstance().getOrDefault(key).toUpperCase(java.util.Locale.ROOT);
        Scheduler.repeat(player, config.integer("delay_ticks"), config.integer("letter_ticks"), (p, n) -> {
            p.sendSystemMessage(Component.literal(text.substring(0, n + 1)), true);
            p.level()
                    .playSound(
                            null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.4f, 1.5f);
            if (n + 1 >= text.length()) {
                Journal.note(
                        p, Notebook.Kind.PLANCHETTE, reading.get().spirit().haunting(), Component.translatable(key));
                return false;
            }
            return true;
        });
    }
}
