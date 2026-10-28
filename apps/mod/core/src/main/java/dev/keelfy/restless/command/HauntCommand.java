package dev.keelfy.restless.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntEvents;
import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.Spirits;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Debug/admin command: /haunt info|force|spirit [player]. */
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
                .then(Commands.argument("event", StringArgumentType.word())
                    .suggests((c, b) -> SharedSuggestionProvider.suggest(HauntEvents.all().stream().map(HauntEvent::id), b))
                    .executes(c -> force(c, c.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> force(c, EntityArgument.getPlayer(c, "player"))))))
            .then(Commands.literal("spirit")
                .then(Commands.argument("spirit", StringArgumentType.word())
                    .suggests((c, b) -> SharedSuggestionProvider.suggest(Spirits.all().stream().map(SpiritType::id), b))
                    .executes(c -> spirit(c, c.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> spirit(c, EntityArgument.getPlayer(c, "player")))))));
    }

    private static int info(CommandSourceStack source, ServerPlayer player) {
        long now = source.getServer().overworld().getGameTime();
        HauntData.Haunting haunting = Director.haunting(player, HauntData.get(source.getServer()), now);
        long days = source.getServer().overworld().getOverworldClockTime() / 24000L;
        String text = String.format(Locale.ROOT, "%s: spirit=%s danger=%.2f days=%d advancements=%d next=%ds",
            player.getName().getString(), haunting.spirit(), Director.danger(player), days,
            Director.advancements(player), Math.max(0, (haunting.nextEventAt() - now) / 20));
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int force(CommandContext<CommandSourceStack> c, ServerPlayer player) throws CommandSyntaxException {
        String id = StringArgumentType.getString(c, "event");
        HauntEvent event = HauntEvents.byId(id).orElseThrow(() -> UNKNOWN_EVENT.create(id));
        if (!event.canRun(player)) {
            c.getSource().sendFailure(Component.literal(id + ": nothing to act on near " + player.getName().getString()));
            return 0;
        }
        Director.fire(player, event);
        c.getSource().sendSuccess(() -> Component.literal("Forced " + id + " on " + player.getName().getString()), true);
        return 1;
    }

    private static int spirit(CommandContext<CommandSourceStack> c, ServerPlayer player) throws CommandSyntaxException {
        String id = StringArgumentType.getString(c, "spirit");
        SpiritType spirit = Spirits.byId(id).orElseThrow(() -> UNKNOWN_SPIRIT.create(id));
        long now = c.getSource().getServer().overworld().getGameTime();
        Director.schedule(player, HauntData.get(c.getSource().getServer()), spirit, now);
        c.getSource().sendSuccess(() -> Component.literal(player.getName().getString() + " is now haunted by " + id), true);
        return 1;
    }
}
