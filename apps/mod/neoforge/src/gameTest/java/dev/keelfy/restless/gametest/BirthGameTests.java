package dev.keelfy.restless.gametest;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.History;
import dev.keelfy.restless.haunt.Ticks;
import dev.keelfy.restless.journal.Notebook;
import dev.keelfy.restless.sanity.SanityPayload;
import dev.keelfy.restless.spirit.Focus;
import dev.keelfy.restless.spirit.Spirits;
import dev.keelfy.restless.spirit.Traits;
import dev.keelfy.restless.spirit.life.BirthRules.Verdict;
import dev.keelfy.restless.spirit.life.Births;
import dev.keelfy.restless.state.HauntData;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.BuriedTreasurePieces;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

final class BirthGameTests {
    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("birth_rules", helper -> {
            var player = HauntEventGameTests.connected(helper);
            var server = helper.getLevel().getServer();
            var data = HauntData.get(server);
            long now = server.overworld().getGameTime();
            data.put(player.getUUID(), List.of());

            HauntEventGameTests.check(
                    helper,
                    Births.deed(player, Births.Deed.SLAUGHTER, false) == Verdict.BORN,
                    "A deed must bind a spirit");
            var own = data.get(player.getUUID());
            HauntEventGameTests.check(
                    helper,
                    own.size() == 1 && own.get(0).spirit().equals("revenant"),
                    "Slaughter must bring a revenant");
            double start = Config.section("births").num("start_anger");
            HauntEventGameTests.check(
                    helper,
                    Math.abs(own.get(0).anger() - start) < 1e-9 && Director.power(own.get(0), now) > 0.1,
                    "A born spirit must start angry, not harmless");
            HauntEventGameTests.check(
                    helper,
                    own.get(0).nextEventAt() >= now + Config.section("births").integer("grace_ticks"),
                    "A born spirit must wait before its first event");

            int within = Config.section("births").integer("notice_within");
            HauntEventGameTests.check(
                    helper,
                    own.get(0).notice() >= 1 && own.get(0).notice() <= within,
                    "A born spirit must owe the journal a line within its first events");
            var spirit = own.get(0);
            for (int i = 0; i < within; i++) {
                spirit = Births.noticing(player, spirit);
            }
            long lines = data.notebook(player.getUUID()).notes().stream()
                    .filter(n -> n.kind() == Notebook.Kind.DREAM)
                    .count();
            HauntEventGameTests.check(
                    helper, spirit.notice() == 0 && lines == 1, "The journal must get exactly one line, then none");
            Director.add(player, data, Spirits.byId("revenant").orElseThrow(), now);
            HauntEventGameTests.check(
                    helper, data.get(player.getUUID()).size() == 1, "A spirit type must not be bound twice");

            HauntEventGameTests.check(
                    helper,
                    Births.deed(player, Births.Deed.FAR_DEATH, false) == Verdict.COOLDOWN
                            && data.get(player.getUUID()).size() == 1,
                    "A second birth inside the cooldown must do nothing");
            HauntEventGameTests.check(
                    helper,
                    Births.deed(player, Births.Deed.PET_SLAIN, false) == Verdict.FED,
                    "The same type must be fed");
            HauntEventGameTests.check(
                    helper,
                    data.get(player.getUUID()).size() == 1
                            && data.get(player.getUUID()).get(0).anger()
                                    > start + Config.section("births").num("repeat_anger") - 1e-9,
                    "Feeding must raise the anger of the spirit that is there");
            player.discard();

            var creative = (net.minecraft.server.level.ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
            HauntEventGameTests.check(
                    helper,
                    Births.deed(creative, Births.Deed.SLAUGHTER, false) == Verdict.NOTHING,
                    "Creative players must not be haunted");
            creative.discard();
            helper.succeed();
        });
        tests.put("birth_old_spirit_keeps_nature", helper -> {
            // a deceit poltergeist saved before types leaned to a nature: poltergeists now lean to objects
            var json = new com.google.gson.JsonObject();
            json.addProperty("spirit", "poltergeist");
            json.addProperty("seed", 5L);
            json.addProperty("next_event_at", 0L);
            var traits = new com.google.gson.JsonArray();
            List.of("whisperer", "liar", "door_slammer", "night_stalker").forEach(traits::add);
            json.add("traits", traits);
            var old = HauntData.Haunting.CODEC
                    .parse(com.mojang.serialization.JsonOps.INSTANCE, json)
                    .getOrThrow();
            var again = HauntData.Haunting.CODEC
                    .parse(
                            com.mojang.serialization.JsonOps.INSTANCE,
                            HauntData.Haunting.CODEC
                                    .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, old)
                                    .getOrThrow())
                    .getOrThrow();
            HauntEventGameTests.check(
                    helper,
                    again.traits().equals(old.traits()) && Traits.weakness(Traits.of(again.traits())) == Focus.MIRROR,
                    "A saved spirit must keep its traits and its nature");
            // a save from before traits rolls them by the old even rule, not the type's lean
            json.remove("traits");
            HauntEventGameTests.check(
                    helper,
                    HauntData.Haunting.CODEC
                            .parse(com.mojang.serialization.JsonOps.INSTANCE, json)
                            .getOrThrow()
                            .traits()
                            .equals(Traits.roll(5L)),
                    "A save without traits must roll them by the even rule");
            var born =
                    HauntData.Haunting.born("poltergeist", 0L, helper.getLevel().getRandom());
            HauntEventGameTests.check(
                    helper,
                    born.traits().equals(Traits.roll(born.seed(), "poltergeist")),
                    "A spirit born now must roll by its type's lean");
            helper.succeed();
        });
        tests.put("birth_successor", helper -> {
            var player = HauntEventGameTests.connected(helper);
            var server = helper.getLevel().getServer();
            var data = HauntData.get(server);
            long now = server.overworld().getGameTime();
            var config = Config.section("births");
            long min = (long) (config.num("successor_min_days") * Ticks.DAY);
            long max = (long) (config.num("successor_max_days") * Ticks.DAY);
            data.put(player.getUUID(), List.of());
            Director.add(player, data, Spirits.byId("revenant").orElseThrow(), now);
            HauntEventGameTests.check(
                    helper, data.successor(player.getUUID()) == null, "A spirit must cancel a successor");
            Director.add(player, data, Spirits.byId("revenant").orElseThrow(), now);
            data.put(player.getUUID(), List.of());
            var waiting = data.successor(player.getUUID());
            HauntEventGameTests.check(
                    helper,
                    waiting != null && waiting.gone().equals(List.of("revenant")),
                    "The last spirit leaving must start a successor that remembers its type");

            Births.successor(player, data, now);
            long due = data.successor(player.getUUID()).dueAt();
            HauntEventGameTests.check(
                    helper, due >= now + min && due <= now + max, "A successor must come 2 to 4 days after");
            Births.successor(player, data, due - 1);
            HauntEventGameTests.check(
                    helper, data.get(player.getUUID()).isEmpty(), "No successor must come before it is due");
            Births.successor(player, data, due);
            var own = data.get(player.getUUID());
            HauntEventGameTests.check(
                    helper,
                    own.size() == 1
                            && !own.get(0).spirit().equals("revenant")
                            && data.successor(player.getUUID()) == null,
                    "A successor must come when due, of another type");
            HauntEventGameTests.check(
                    helper,
                    Math.abs(own.get(0).anger() - config.num("start_anger")) < 1e-9
                            && Director.power(own.get(0), due) > 0.1,
                    "A successor must not be harmless");

            data.putSuccessor(player.getUUID(), new HauntData.Successor(0, List.of()));
            Births.successor(player, data, due + 10 * Ticks.DAY);
            HauntEventGameTests.check(
                    helper,
                    data.get(player.getUUID()).size() == 1 && data.successor(player.getUUID()) == null,
                    "A player who has a spirit must get no successor");
            var saved = new HauntData.Successor(48000L, List.of("revenant"));
            var json = HauntData.Successor.CODEC
                    .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, saved)
                    .getOrThrow();
            HauntEventGameTests.check(
                    helper,
                    HauntData.Successor.CODEC
                                    .parse(com.mojang.serialization.JsonOps.INSTANCE, json)
                                    .getOrThrow()
                                    .equals(saved)
                            && HauntData.Successor.CODEC
                                            .parse(
                                                    com.mojang.serialization.JsonOps.INSTANCE,
                                                    new com.google.gson.JsonObject())
                                            .getOrThrow()
                                            .dueAt()
                                    == -1L,
                    "A successor must survive a save, and an entry without fields must load");
            player.discard();
            helper.succeed();
        });
        tests.put("birth_generated", helper -> {
            var level = helper.getLevel();
            var data = HauntData.get(level.getServer());
            data.putTerritorial(List.of());
            var shipwreck = start(helper, "shipwreck", new BlockPos(4000, 70, 4000));
            HauntEventGameTests.check(
                    helper,
                    Births.generated(level, shipwreck, false) == Verdict.BORN
                            && Director.territorial(data, level).size() == 1,
                    "A configured structure must get one territorial spirit");
            HauntEventGameTests.check(
                    helper,
                    Director.territorial(data, level)
                            .get(0)
                            .anchor()
                            .equals(shipwreck.getBoundingBox().getCenter()),
                    "The spirit must sit at the centre of the structure");
            HauntEventGameTests.check(
                    helper,
                    Births.generated(level, start(helper, "shipwreck", new BlockPos(4300, 70, 4000)), false)
                                    == Verdict.BORN
                            && Director.territorial(data, level).size() == 2,
                    "Another structure must get its own spirit");
            HauntEventGameTests.check(
                    helper,
                    Births.generated(level, start(helper, "buried_treasure", new BlockPos(-4000, 70, 4000)), false)
                                    == Verdict.NOTHING
                            && Director.territorial(data, level).size() == 2,
                    "A structure not in the config must stay empty");
            data.putTerritorial(List.of());
            helper.succeed();
        });
        tests.put("spirit_act_history", helper -> {
            var player = HauntEventGameTests.connected(helper);
            ChannelAttributes.getOrCreateCommonChannels(player.connection.getConnection(), ConnectionProtocol.PLAY)
                    .add(SanityPayload.TYPE.id());
            var server = helper.getLevel().getServer();
            var data = HauntData.get(server);
            data.put(player.getUUID(), List.of());
            var spirit = Director.add(
                    player,
                    data,
                    Spirits.byId("revenant").orElseThrow(),
                    server.overworld().getGameTime());
            var event = Director.act(player, spirit, null, null);
            HauntEventGameTests.check(helper, event.isPresent(), "A revenant must have an event it can act with");
            // A waiting event is listed when its scare comes, not when it starts.
            if (event.get().waits()) {
                Director.landed(player, event.get().id().toString(), spirit.uuid());
            }
            var entry = History.last(player.getName().getString(), 1).get(0);
            HauntEventGameTests.check(
                    helper,
                    entry.event().equals(event.get().id().toString())
                            && entry.spirit().startsWith("revenant "),
                    "The history must name the event and the spirit that acted");
            Director.fire(player, event.get());
            if (event.get().waits()) {
                Director.landed(player, event.get().id().toString(), null);
            }
            HauntEventGameTests.check(
                    helper,
                    History.last(player.getName().getString(), 1).get(0).spirit() == null,
                    "An event with no spirit behind it must be listed without one");
            player.discard();
            helper.succeed();
        });
    }

    /** A structure start of the named vanilla kind with one piece at {@code pos}. */
    static StructureStart start(GameTestHelper helper, String id, BlockPos pos) {
        var structure = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.STRUCTURE)
                .getOrThrow(net.minecraft.resources.ResourceKey.create(
                        Registries.STRUCTURE, Identifier.withDefaultNamespace(id)))
                .value();
        return new StructureStart(
                structure,
                ChunkPos.containing(pos),
                0,
                new PiecesContainer(List.of(new BuriedTreasurePieces.BuriedTreasurePiece(pos))));
    }
}
