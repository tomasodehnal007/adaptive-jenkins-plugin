package io.jenkins.plugins.adaptiveagent.condition;

import static io.jenkins.plugins.adaptiveagent.util.DurationComparison.LONGER_THAN;
import static io.jenkins.plugins.adaptiveagent.util.DurationComparison.SHORTER_THAN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.plugins.adaptiveagent.util.IntervalUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The durations are given newest first, in milliseconds, as the history of the agent returns them. */
class HistoryTimeConditionTest {

    private static final List<Long> AVERAGE_OF_THREE_SECONDS = List.of(2_000L, 3_000L, 4_000L);

    @Test
    void longerThanHoldsOnlyWhenTheAverageIsLonger() {
        assertTrue(
                new HistoryTimeCondition(3, LONGER_THAN, 2, IntervalUnit.SECONDS).holdsFor(AVERAGE_OF_THREE_SECONDS));
        assertFalse(
                new HistoryTimeCondition(3, LONGER_THAN, 4, IntervalUnit.SECONDS).holdsFor(AVERAGE_OF_THREE_SECONDS));
    }

    @Test
    void shorterThanHoldsOnlyWhenTheAverageIsShorter() {
        assertTrue(
                new HistoryTimeCondition(3, SHORTER_THAN, 4, IntervalUnit.SECONDS).holdsFor(AVERAGE_OF_THREE_SECONDS));
        assertFalse(
                new HistoryTimeCondition(3, SHORTER_THAN, 2, IntervalUnit.SECONDS).holdsFor(AVERAGE_OF_THREE_SECONDS));
    }
}
