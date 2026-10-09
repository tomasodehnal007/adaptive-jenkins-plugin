package io.jenkins.plugins.adaptiveagent.condition;

import static io.jenkins.plugins.adaptiveagent.util.DurationComparison.LONGER_THAN;
import static io.jenkins.plugins.adaptiveagent.util.DurationComparison.SHORTER_THAN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.plugins.adaptiveagent.util.IntervalUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The decision of the condition for given durations of the latest builds, without Jenkins. */
class HistoryTimeConditionTest {

    @Test
    void shorterThanHoldsWhenTheAverageIsBelowTheLimit() {
        HistoryTimeCondition condition = new HistoryTimeCondition(3, SHORTER_THAN, 2, IntervalUnit.SECONDS);

        assertTrue(condition.holdsFor(List.of(1_000L, 1_500L, 2_000L))); // average 1500 ms
        assertFalse(condition.holdsFor(List.of(1_000L, 2_000L, 6_000L))); // average 3000 ms
    }

    @Test
    void longerThanHoldsWhenTheAverageIsAboveTheLimit() {
        HistoryTimeCondition condition = new HistoryTimeCondition(3, LONGER_THAN, 2, IntervalUnit.SECONDS);

        assertTrue(condition.holdsFor(List.of(1_000L, 2_000L, 6_000L))); // average 3000 ms
        assertFalse(condition.holdsFor(List.of(1_000L, 1_500L, 2_000L))); // average 1500 ms
    }

    @Test
    void averageExactlyAtTheLimitIsNeitherLongerNorShorter() {
        List<Long> averageTwoSeconds = List.of(1_000L, 3_000L);

        assertFalse(new HistoryTimeCondition(2, SHORTER_THAN, 2, IntervalUnit.SECONDS).holdsFor(averageTwoSeconds));
        assertFalse(new HistoryTimeCondition(2, LONGER_THAN, 2, IntervalUnit.SECONDS).holdsFor(averageTwoSeconds));
    }

    @Test
    void averageIsNotRoundedBeforeItIsCompared() {
        // the average of 2000 ms and 2001 ms is 2000.5 ms: longer than 2 s, although rounded down it would be 2000
        assertTrue(new HistoryTimeCondition(2, LONGER_THAN, 2, IntervalUnit.SECONDS).holdsFor(List.of(2_000L, 2_001L)));
    }

    @Test
    void limitIsConvertedFromItsUnit() {
        HistoryTimeCondition condition = new HistoryTimeCondition(1, SHORTER_THAN, 1, IntervalUnit.MINUTES);

        assertTrue(condition.holdsFor(List.of(59_999L)));
        assertFalse(condition.holdsFor(List.of(60_000L)));
    }

    @Test
    void onlyTheLatestBuildsAreAveraged() {
        HistoryTimeCondition condition = new HistoryTimeCondition(2, SHORTER_THAN, 2, IntervalUnit.SECONDS);

        // newest first: the older 100 s build is not part of the average
        assertTrue(condition.holdsFor(List.of(1_000L, 1_000L, 100_000L)));
    }

    @Test
    void doesNotHoldWhenThereAreFewerBuildsThanRequired() {
        assertFalse(new HistoryTimeCondition(3, SHORTER_THAN, 1, IntervalUnit.MINUTES).holdsFor(List.of(1L, 1L)));
        assertFalse(new HistoryTimeCondition(3, LONGER_THAN, 1, IntervalUnit.MINUTES).holdsFor(List.of(9L, 9L)));
    }

    @Test
    void nonPositiveQuantityNeverHolds() {
        assertFalse(new HistoryTimeCondition(0, SHORTER_THAN, 1, IntervalUnit.MINUTES).holdsFor(List.of(1L)));
    }
}
