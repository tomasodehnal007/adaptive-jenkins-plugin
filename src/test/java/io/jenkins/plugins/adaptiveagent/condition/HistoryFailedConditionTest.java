package io.jenkins.plugins.adaptiveagent.condition;

import static hudson.model.Result.ABORTED;
import static hudson.model.Result.FAILURE;
import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The decision of the condition for given results of the latest builds, without Jenkins. */
class HistoryFailedConditionTest {

    @Test
    void holdsWhenTheLatestBuildsAllFailed() {
        assertTrue(new HistoryFailedCondition(3).holdsFor(List.of(FAILURE, FAILURE, FAILURE)));
    }

    @Test
    void doesNotHoldWhenOneOfTheLatestBuildsDidNotFail() {
        assertFalse(new HistoryFailedCondition(3).holdsFor(List.of(FAILURE, SUCCESS, FAILURE)));
    }

    @Test
    void unstableAndAbortedBuildsDoNotCountAsFailed() {
        assertFalse(new HistoryFailedCondition(2).holdsFor(List.of(FAILURE, UNSTABLE)));
        assertFalse(new HistoryFailedCondition(2).holdsFor(List.of(ABORTED, FAILURE)));
    }

    @Test
    void doesNotHoldWhenThereAreFewerBuildsThanRequired() {
        assertFalse(new HistoryFailedCondition(3).holdsFor(List.of(FAILURE, FAILURE)));
        assertFalse(new HistoryFailedCondition(1).holdsFor(List.of()));
    }

    @Test
    void onlyTheLatestBuildsAreJudged() {
        // newest first: the two latest failed, an older success does not matter
        assertTrue(new HistoryFailedCondition(2).holdsFor(List.of(FAILURE, FAILURE, SUCCESS)));
    }

    @Test
    void nonPositiveQuantityNeverHolds() {
        assertFalse(new HistoryFailedCondition(0).holdsFor(List.of(FAILURE)));
    }
}
