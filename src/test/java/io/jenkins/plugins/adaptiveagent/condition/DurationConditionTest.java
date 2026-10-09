package io.jenkins.plugins.adaptiveagent.condition;

import static io.jenkins.plugins.adaptiveagent.util.DurationComparison.LONGER_THAN;
import static io.jenkins.plugins.adaptiveagent.util.DurationComparison.SHORTER_THAN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.plugins.adaptiveagent.util.IntervalUnit;
import org.junit.jupiter.api.Test;

/** The decision of the condition for exact durations, without Jenkins. */
class DurationConditionTest {

    @Test
    void longerThanHoldsOnlyAboveTheLimit() {
        DurationCondition condition = new DurationCondition(LONGER_THAN, 5, IntervalUnit.SECONDS);

        assertTrue(condition.holdsFor(5_001));
        assertFalse(condition.holdsFor(5_000));
        assertFalse(condition.holdsFor(4_999));
    }

    @Test
    void shorterThanHoldsOnlyBelowTheLimit() {
        DurationCondition condition = new DurationCondition(SHORTER_THAN, 5, IntervalUnit.SECONDS);

        assertTrue(condition.holdsFor(4_999));
        assertFalse(condition.holdsFor(5_000));
        assertFalse(condition.holdsFor(5_001));
    }

    @Test
    void limitIsConvertedFromItsUnit() {
        DurationCondition condition = new DurationCondition(LONGER_THAN, 2, IntervalUnit.MINUTES);

        assertTrue(condition.holdsFor(120_001));
        assertFalse(condition.holdsFor(120_000));
    }
}
