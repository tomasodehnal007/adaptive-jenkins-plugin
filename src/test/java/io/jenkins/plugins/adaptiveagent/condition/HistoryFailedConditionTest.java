package io.jenkins.plugins.adaptiveagent.condition;

import static hudson.model.Result.FAILURE;
import static hudson.model.Result.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The results are given newest first, as the history of the agent returns them. */
class HistoryFailedConditionTest {

    private final HistoryFailedCondition lastThreeFailed = new HistoryFailedCondition(3);

    @Test
    void holdsWhenTheLatestBuildsAllFailed() {
        assertTrue(lastThreeFailed.holdsFor(List.of(FAILURE, FAILURE, FAILURE, SUCCESS)));
    }

    @Test
    void doesNotHoldWhenOneOfTheLatestBuildsDidNotFail() {
        assertFalse(lastThreeFailed.holdsFor(List.of(FAILURE, SUCCESS, FAILURE)));
    }
}
