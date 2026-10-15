package io.jenkins.plugins.adaptiveagent.condition;

import static io.jenkins.plugins.adaptiveagent.util.DurationComparison.LONGER_THAN;
import static io.jenkins.plugins.adaptiveagent.util.DurationComparison.SHORTER_THAN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.plugins.adaptiveagent.util.IntervalUnit;
import org.junit.jupiter.api.Test;

class DurationConditionTest {

    @Test
    void longerThanHoldsOnlyForALongerBuild() {
        DurationCondition longerThanFiveSeconds = new DurationCondition(LONGER_THAN, 5, IntervalUnit.SECONDS);

        assertTrue(longerThanFiveSeconds.holdsFor(6_000));
        assertFalse(longerThanFiveSeconds.holdsFor(4_000));
    }

    @Test
    void shorterThanHoldsOnlyForAShorterBuild() {
        DurationCondition shorterThanFiveSeconds = new DurationCondition(SHORTER_THAN, 5, IntervalUnit.SECONDS);

        assertTrue(shorterThanFiveSeconds.holdsFor(4_000));
        assertFalse(shorterThanFiveSeconds.holdsFor(6_000));
    }
}
