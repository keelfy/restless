package dev.keelfy.restless.gametest;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.BirthRules.Verdict;
import dev.keelfy.restless.haunt.Births;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.History;
import dev.keelfy.restless.haunt.Notebook;
import dev.keelfy.restless.haunt.SanityPayload;
import dev.keelfy.restless.spirit.Spirits;
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
                    Births.generated(level, start(helper, "shipwreck", new BlockPos(4040, 70, 4000)), false)
                                    == Verdict.CROWDED
                            && Director.territorial(data, level).size() == 1,
                    "A structure near another haunted one must stay empty");
            HauntEventGameTests.check(
                    helper,
                    Births.generated(level, start(helper, "shipwreck", new BlockPos(4300, 70, 4000)), false)
                                    == Verdict.BORN
                            && Director.territorial(data, level).size() == 2,
                    "A far structure must get its own spirit");
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
            var entry = History.last(player.getName().getString(), 1).get(0);
            HauntEventGameTests.check(
                    helper,
                    entry.event().equals(event.get().id().toString())
                            && entry.spirit().startsWith("revenant "),
                    "The history must name the event and the spirit that acted");
            Director.fire(player, event.get());
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
