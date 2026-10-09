package io.jenkins.plugins.adaptiveagent.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DurationComparisonTest {

    @Test
    void longerThanHoldsOnlyAboveTheLimit() {
        assertTrue(DurationComparison.LONGER_THAN.holds(11, 10));
        assertFalse(DurationComparison.LONGER_THAN.holds(10, 10));
        assertFalse(DurationComparison.LONGER_THAN.holds(9, 10));
    }

    @Test
    void shorterThanHoldsOnlyBelowTheLimit() {
        assertTrue(DurationComparison.SHORTER_THAN.holds(9, 10));
        assertFalse(DurationComparison.SHORTER_THAN.holds(10, 10));
        assertFalse(DurationComparison.SHORTER_THAN.holds(11, 10));
    }
}
