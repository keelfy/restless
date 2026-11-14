package dev.keelfy.restless.haunt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TallyTest {
    @Test
    void eachKindCountsOnItsOwn() {
        Tally tally = new Tally();
        tally.add("a", Tally.Kind.ROLLED);
        tally.add("a", Tally.Kind.ROLLED);
        tally.add("a", Tally.Kind.RAN);
        tally.add("a", Tally.Kind.WARDED);
        tally.add("a", Tally.Kind.FAILED);
        assertEquals(new Tally.Counts(2, 1, 1, 1), tally.of("a"));
        assertEquals(Tally.Counts.NONE, tally.of("b"));
    }

    @Test
    void ranksByRunsAndListsTheOnesThatNeverRan() {
        Tally tally = new Tally();
        tally.add("b", Tally.Kind.RAN);
        tally.add("c", Tally.Kind.RAN);
        tally.add("c", Tally.Kind.RAN);
        tally.add("d", Tally.Kind.ROLLED);
        List<String> ids = List.of("d", "c", "b", "a");
        assertEquals(List.of("c", "b", "a", "d"), tally.ranked(ids));
        assertEquals(List.of("a", "d"), tally.never(ids));
    }

    @Test
    void csvListsEveryIdMostRunFirst() {
        Tally tally = new Tally();
        tally.add("a", Tally.Kind.RAN);
        tally.add("a", Tally.Kind.FAILED);
        assertEquals("event,rolled,ran,warded,failed\na,0,1,0,1\nb,0,0,0,0\n", tally.csv(List.of("b", "a")));
    }
}
