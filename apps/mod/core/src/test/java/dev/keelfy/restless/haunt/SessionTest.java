package dev.keelfy.restless.haunt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SessionTest {
    @Test
    void endingASideEmptiesWhatItKeptAndLeavesTheOtherSideAlone() {
        List<String> server = Session.SERVER.keep(new ArrayList<>());
        Map<String, Integer> client = Session.CLIENT.keep(new HashMap<>());
        server.add("player");
        client.put("door", 1);

        Session.SERVER.end();

        assertTrue(server.isEmpty());
        assertEquals(1, client.size());
        Session.CLIENT.end();
        assertTrue(client.isEmpty());
    }

    @Test
    void aKeptCollectionStaysUsableInTheNextWorld() {
        List<String> kept = Session.SERVER.keep(new ArrayList<>());
        kept.add("old");
        Session.SERVER.end();
        kept.add("new");
        assertEquals(List.of("new"), kept);
        Session.SERVER.end();
    }
}
