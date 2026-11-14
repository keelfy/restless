package dev.keelfy.restless.banish;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlanTest {
    private static final Plan PLAN = new Plan(2, 2, 6, 2);

    @Test
    void waves_fall_into_phases_in_order() {
        assertEquals(12, PLAN.total());
        assertEquals(Phase.CALL, PLAN.at(0));
        assertEquals(Phase.CALL, PLAN.at(1));
        assertEquals(Phase.BARGAIN, PLAN.at(2));
        assertEquals(Phase.BARGAIN, PLAN.at(3));
        assertEquals(Phase.FURY, PLAN.at(4));
        assertEquals(Phase.FURY, PLAN.at(9));
        assertEquals(Phase.LEAVING, PLAN.at(10));
        assertEquals(Phase.LEAVING, PLAN.at(11));
        assertEquals(Phase.LEAVING, PLAN.at(40), "past the end is still the leaving");
    }

    @Test
    void an_empty_phase_is_skipped() {
        Plan noBargain = new Plan(1, 0, 2, 1);
        assertEquals(Phase.CALL, noBargain.at(0));
        assertEquals(Phase.FURY, noBargain.at(1));
        assertEquals(Phase.LEAVING, noBargain.at(3));
    }

    @Test
    void only_the_fury_changes_length() {
        assertEquals(new Plan(2, 2, 10, 2), PLAN.longerFury(4));
        assertEquals(new Plan(2, 2, 0, 2), PLAN.longerFury(-9), "never below none");
    }
}
