package dev.keelfy.restless.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntEvents;
import dev.keelfy.restless.haunt.Ticks;
import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.Spirits;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Debug/admin command: /haunt info|force|spirit add|spirit remove [player]. */
public final class HauntCommand {
    private static final DynamicCommandExceptionType UNKNOWN_EVENT =
        new DynamicCommandExceptionType(id -> Component.literal("Unknown haunt event: " + id));
    private static final DynamicCommandExceptionType UNKNOWN_SPIRIT =
        new DynamicCommandExceptionType(id -> Component.literal("Unknown spirit: " + id));

    private HauntCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("haunt")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("info")
                .executes(c -> info(c.getSource(), c.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(c -> info(c.getSource(), EntityArgument.getPlayer(c, "player")))))
            .then(Commands.literal("force")
                .then(Commands.argument("event", IdentifierArgument.id())
                    .suggests((c, b) -> SharedSuggestionProvider.suggestResource(HauntEvents.all().stream().map(HauntEvent::id), b))
                    .executes(c -> force(c, c.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> force(c, EntityArgument.getPlayer(c, "player"))))))
            .then(Commands.literal("spirit")
                .then(Commands.literal("add")
                    .then(Commands.argument("spirit", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Spirits.all().stream().map(SpiritType::id), b))
                        .executes(c -> add(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(c -> add(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .executes(c -> remove(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(c -> remove(c, EntityArgument.getPlayer(c, "player"))))))));
    }

    private static int info(CommandSourceStack source, ServerPlayer player) {
        long now = source.getServer().overworld().getGameTime();
        List<HauntData.Haunting> spirits = Director.hauntings(player, HauntData.get(source.getServer()), now);
        long days = source.getServer().overworld().getOverworldClockTime() / Ticks.DAY;
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < spirits.size(); i++) {
            HauntData.Haunting h = spirits.get(i);
            list.append(String.format(Locale.ROOT, "%n  %d. %s next=%ds", i + 1, h.spirit(), Math.max(0, (h.nextEventAt() - now) / Ticks.SECOND)));
        }
        String text = String.format(Locale.ROOT, "%s: danger=%.2f days=%d advancements=%d spirits=%d%s",
            player.getName().getString(), Director.danger(player), days, Director.advancements(player), spirits.size(), list);
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int force(CommandContext<CommandSourceStack> c, ServerPlayer player) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(c, "event");
        HauntEvent event = HauntEvents.byId(id).orElseThrow(() -> UNKNOWN_EVENT.create(id));
        if (!event.canRun(player)) {
            c.getSource().sendFailure(Component.literal(id + ": nothing to act on near " + player.getName().getString()));
            return 0;
        }
        Director.fire(player, event);
        c.getSource().sendSuccess(() -> Component.literal("Forced " + id + " on " + player.getName().getString()), true);
        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> c, ServerPlayer player) throws CommandSyntaxException {
        String id = StringArgumentType.getString(c, "spirit");
        SpiritType spirit = Spirits.byId(id).orElseThrow(() -> UNKNOWN_SPIRIT.create(id));
        long now = c.getSource().getServer().overworld().getGameTime();
        Director.add(player, HauntData.get(c.getSource().getServer()), spirit, now);
        c.getSource().sendSuccess(() -> Component.literal(id + " now haunts " + player.getName().getString()), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        int index = IntegerArgumentType.getInteger(c, "index");
        long now = c.getSource().getServer().overworld().getGameTime();
        if (!Director.remove(player, HauntData.get(c.getSource().getServer()), index - 1, now)) {
            c.getSource().sendFailure(Component.literal(player.getName().getString() + " has no spirit #" + index));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal("Removed spirit #" + index + " from " + player.getName().getString()), true);
        return 1;
    }
}
