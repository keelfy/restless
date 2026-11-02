package dev.keelfy.restless.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.keelfy.restless.debug.DebugMenu;
import dev.keelfy.restless.haunt.BirthRules;
import dev.keelfy.restless.haunt.Births;
import dev.keelfy.restless.haunt.Corruption;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.Graves;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntEvents;
import dev.keelfy.restless.haunt.History;
import dev.keelfy.restless.haunt.Location;
import dev.keelfy.restless.haunt.Sanity;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Ticks;
import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.Spirits;
import dev.keelfy.restless.spirit.Story;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Debug/admin command: /haunt opens the debug screen; info|force (optionally after a delay in
 * seconds)|act|history|sanity|birth|spirit add|age|remove [player] work from chat.
 */
public final class HauntCommand {
    private static final DynamicCommandExceptionType UNKNOWN_EVENT =
            new DynamicCommandExceptionType(id -> Component.literal("Unknown haunt event: " + id));
    private static final DynamicCommandExceptionType UNKNOWN_SPIRIT =
            new DynamicCommandExceptionType(id -> Component.literal("Unknown spirit: " + id));

    private static final DynamicCommandExceptionType UNKNOWN_DEED =
            new DynamicCommandExceptionType(id -> Component.literal("Unknown deed: " + id));

    private HauntCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("haunt")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> {
                    DebugMenu.open(c.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.literal("info")
                        .executes(c -> info(c.getSource(), c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> info(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("force")
                        .then(Commands.argument("event", IdentifierArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(
                                        HauntEvents.all().stream().map(HauntEvent::id), b))
                                .executes(c -> force(c, c.getSource().getPlayerOrException(), 0))
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                        .executes(c -> force(
                                                c,
                                                c.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(c, "seconds")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(c -> force(
                                                        c,
                                                        EntityArgument.getPlayer(c, "player"),
                                                        IntegerArgumentType.getInteger(c, "seconds")))))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> force(c, EntityArgument.getPlayer(c, "player"), 0)))))
                .then(Commands.literal("act")
                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(c -> act(c, c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> act(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("history")
                        .executes(c -> history(c.getSource(), null))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> history(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("sanity")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 1))
                                .executes(c -> sanity(c, c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> sanity(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("birth")
                        .then(Commands.argument("deed", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Births.Deed.values()).map(Births.Deed::key), b))
                                .executes(c -> birth(c, c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> birth(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("spirit")
                        .then(Commands.literal("add")
                                .then(Commands.argument("spirit", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                Spirits.all().stream().map(SpiritType::id), b))
                                        .executes(c -> add(c, c.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(c -> add(c, EntityArgument.getPlayer(c, "player"))))))
                        .then(Commands.literal("age")
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("days", DoubleArgumentType.doubleArg(0))
                                                .executes(c ->
                                                        age(c, c.getSource().getPlayerOrException()))
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .executes(
                                                                c -> age(c, EntityArgument.getPlayer(c, "player")))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .executes(c -> remove(c, c.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(c -> remove(c, EntityArgument.getPlayer(c, "player"))))))));
    }

    private static int info(CommandSourceStack source, ServerPlayer player) {
        long now = source.getServer().overworld().getGameTime();
        HauntData data = HauntData.get(source.getServer());
        List<HauntData.Haunting> spirits = Director.hauntings(player, data, now);
        List<HauntData.Territorial> places = Director.territorial(data, player.level());
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < spirits.size(); i++) {
            HauntData.Haunting h = spirits.get(i);
            list.append(describe(source, i + 1, h, "personal", now, ""));
        }
        for (int i = 0; i < places.size(); i++) {
            HauntData.Territorial t = places.get(i);
            list.append(describe(
                    source,
                    spirits.size() + i + 1,
                    t.haunting(),
                    "territorial",
                    now,
                    String.format(
                            Locale.ROOT,
                            " anchor=%d,%d,%d radius=%d%s",
                            t.anchor().getX(),
                            t.anchor().getY(),
                            t.anchor().getZ(),
                            t.radius(),
                            t.reaches(player) ? " (in range)" : "")));
        }
        String text = String.format(
                Locale.ROOT,
                "%s: sanity=%.2f spirits=%d%s",
                player.getName().getString(),
                Sanity.of(player),
                spirits.size() + places.size(),
                list);
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static String describe(
            CommandSourceStack source, int number, HauntData.Haunting h, String kind, long now, String extra) {
        Vec3 roaming = Location.roaming(h.uuid());
        String at = roaming == null
                ? ""
                : String.format(Locale.ROOT, " at=%.0f,%.0f,%.0f", roaming.x, roaming.y, roaming.z);
        HauntData.Grave grave = HauntData.get(source.getServer()).graves().get(h.uuid());
        String buried = grave == null
                ? ""
                : String.format(
                        Locale.ROOT,
                        " grave=%d,%d,%d%s wants=%s",
                        grave.pos().getX(),
                        grave.pos().getY(),
                        grave.pos().getZ(),
                        grave.built() ? "" : "(unbuilt)",
                        BuiltInRegistries.ITEM.getKey(Graves.wanted(h.seed())));
        String blight = Corruption.size(HauntData.get(source.getServer()), h.uuid());
        extra += (h.evolved() ? " evolved" : "") + (blight.isEmpty() ? "" : " blight=" + blight);
        return String.format(
                Locale.ROOT,
                "%n  %d. %s %s \"%s\" seed=%x traits=%s power=%.2f anger=%.2f next=%ds%s%s",
                number,
                h.spirit(),
                kind,
                Story.of(h.seed()),
                h.seed(),
                String.join(",", h.traits()),
                Director.power(h, now),
                h.anger(),
                Math.max(0, (h.nextEventAt() - now) / Ticks.SECOND),
                at + buried,
                extra);
    }

    private static int sanity(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        double value = DoubleArgumentType.getDouble(c, "value");
        Sanity.set(player, value);
        c.getSource()
                .sendSuccess(
                        () -> Component.literal(String.format(
                                Locale.ROOT,
                                "Sanity of %s is now %.2f",
                                player.getName().getString(),
                                value)),
                        true);
        return 1;
    }

    /** Fires the event now, or after {@code seconds}, when the conditions are checked, so a state can be got into. */
    private static int force(CommandContext<CommandSourceStack> c, ServerPlayer player, int seconds)
            throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(c, "event");
        HauntEvent event = HauntEvents.byId(id).orElseThrow(() -> UNKNOWN_EVENT.create(id));
        CommandSourceStack source = c.getSource();
        String name = player.getName().getString();
        if (seconds > 0) {
            Scheduler.later(player, seconds * Ticks.SECOND, p -> {
                if (event.canRun(p)) {
                    Director.fire(p, event);
                    source.sendSuccess(() -> Component.literal("Forced " + id + " on " + name), true);
                } else {
                    source.sendFailure(Component.literal(id + ": nothing to act on near " + name));
                }
            });
            source.sendSuccess(
                    () -> Component.literal("Will force " + id + " on " + name + " in " + seconds + "s"), true);
            return 1;
        }
        if (!event.canRun(player)) {
            source.sendFailure(Component.literal(id + ": nothing to act on near " + name));
            return 0;
        }
        Director.fire(player, event);
        source.sendSuccess(() -> Component.literal("Forced " + id + " on " + name), true);
        return 1;
    }

    /**
     * The spirit at this /haunt info index acts on the player now: it rolls the event it would roll on its own, as
     * itself, outside its timer and past wards.
     */
    private static int act(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        int index = IntegerArgumentType.getInteger(c, "index");
        long now = c.getSource().getServer().overworld().getGameTime();
        HauntData data = HauntData.get(c.getSource().getServer());
        List<HauntData.Haunting> personal = Director.hauntings(player, data, now);
        List<HauntData.Territorial> places = Director.territorial(data, player.level());
        String name = player.getName().getString();
        if (index > personal.size() + places.size()) {
            c.getSource().sendFailure(Component.literal(name + " has no spirit #" + index));
            return 0;
        }
        HauntData.Haunting spirit;
        Vec3 at;
        if (index <= personal.size()) {
            spirit = personal.get(index - 1);
            at = Location.roaming(spirit.uuid());
        } else {
            HauntData.Territorial place = places.get(index - personal.size() - 1);
            spirit = place.haunting();
            at = Vec3.atCenterOf(place.anchor());
        }
        Optional<HauntEvent> acted = Director.act(player, spirit, at, null);
        if (acted.isEmpty()) {
            c.getSource().sendFailure(Component.literal(Director.label(spirit) + " has nothing to do to " + name));
            return 0;
        }
        c.getSource()
                .sendSuccess(
                        () -> Component.literal(Director.label(spirit) + " forced "
                                + acted.get().id() + " on " + name),
                        true);
        return 1;
    }

    /** The newest events and the spirit behind each, of one player or of everyone. */
    private static int history(CommandSourceStack source, @Nullable ServerPlayer player) {
        long now = source.getServer().overworld().getGameTime();
        List<History.Entry> entries =
                History.last(player == null ? null : player.getName().getString(), 15);
        StringBuilder text = new StringBuilder(entries.isEmpty() ? "No events yet" : "Latest events:");
        for (History.Entry e : entries) {
            text.append(String.format(
                    Locale.ROOT,
                    "%n  %ds ago %s %s by %s",
                    Math.max(0, (now - e.at()) / Ticks.SECOND),
                    e.player(),
                    e.event(),
                    e.spirit() == null ? "no spirit" : e.spirit()));
        }
        source.sendSuccess(() -> Component.literal(text.toString()), false);
        return entries.size();
    }

    private static int birth(CommandContext<CommandSourceStack> c, ServerPlayer player) throws CommandSyntaxException {
        String key = StringArgumentType.getString(c, "deed");
        Births.Deed deed = Births.Deed.byKey(key).orElseThrow(() -> UNKNOWN_DEED.create(key));
        BirthRules.Verdict verdict = Births.deed(player, deed, false);
        c.getSource()
                .sendSuccess(
                        () -> Component.literal(key + " on " + player.getName().getString() + ": " + verdict), true);
        return verdict == BirthRules.Verdict.BORN || verdict == BirthRules.Verdict.FED ? 1 : 0;
    }

    private static int add(CommandContext<CommandSourceStack> c, ServerPlayer player) throws CommandSyntaxException {
        String id = StringArgumentType.getString(c, "spirit");
        SpiritType spirit = Spirits.byId(id).orElseThrow(() -> UNKNOWN_SPIRIT.create(id));
        long now = c.getSource().getServer().overworld().getGameTime();
        HauntData data = HauntData.get(c.getSource().getServer());
        if (spirit.kind() == SpiritType.Kind.TERRITORIAL) {
            Director.anchor(data, spirit, player.level(), player.blockPosition(), now);
            c.getSource()
                    .sendSuccess(
                            () -> Component.literal(id + " now haunts the place where "
                                    + player.getName().getString() + " stands"),
                            true);
            return 1;
        }
        Director.add(player, data, spirit, now);
        c.getSource()
                .sendSuccess(
                        () -> Component.literal(
                                id + " now haunts " + player.getName().getString()),
                        true);
        return 1;
    }

    /**
     * Makes the spirit at this /haunt info index older, so maturity, blight reach and evolution can be tried at once.
     */
    private static int age(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        int index = IntegerArgumentType.getInteger(c, "index");
        double days = DoubleArgumentType.getDouble(c, "days");
        long now = c.getSource().getServer().overworld().getGameTime();
        HauntData data = HauntData.get(c.getSource().getServer());
        List<HauntData.Haunting> listed = new ArrayList<>(Director.hauntings(player, data, now));
        Director.territorial(data, player.level()).forEach(t -> listed.add(t.haunting()));
        if (index > listed.size()) {
            c.getSource().sendFailure(Component.literal(player.getName().getString() + " has no spirit #" + index));
            return 0;
        }
        data.change(listed.get(index - 1).uuid(), h -> h.aged((long) (days * Ticks.DAY)));
        c.getSource()
                .sendSuccess(
                        () -> Component.literal(String.format(
                                Locale.ROOT,
                                "Spirit #%d of %s is %.1f days older",
                                index,
                                player.getName().getString(),
                                days)),
                        true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        int index = IntegerArgumentType.getInteger(c, "index");
        long now = c.getSource().getServer().overworld().getGameTime();
        if (!Director.remove(player, HauntData.get(c.getSource().getServer()), index - 1, now)) {
            c.getSource().sendFailure(Component.literal(player.getName().getString() + " has no spirit #" + index));
            return 0;
        }
        c.getSource()
                .sendSuccess(
                        () -> Component.literal("Removed spirit #" + index + " from "
                                + player.getName().getString()),
                        true);
        return 1;
    }
}
