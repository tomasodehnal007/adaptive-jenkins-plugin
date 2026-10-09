package io.jenkins.plugins.adaptiveagent.condition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.util.SizeUnit;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** Reads the real free space of a node; the built-in node is used, so no agent has to be started. */
@WithJenkins
class DiskSpaceConditionOnAgentTest {

    private static TaskContext contextOn(JenkinsRule jenkins) {
        return new TaskContext(null, jenkins.jenkins.toComputer(), null, null, null);
    }

    @Test
    void holdsWhenTheThresholdIsFarAboveAnyRealDisk(JenkinsRule jenkins) throws Exception {
        // a million gibibytes (a pebibyte) is more than any disk the test can run on
        assertTrue(new DiskSpaceCondition(1_000_000, SizeUnit.GIB).conditionPasses(contextOn(jenkins)));
    }

    @Test
    void doesNotHoldWhenTheThresholdIsZero(JenkinsRule jenkins) throws Exception {
        assertFalse(new DiskSpaceCondition(0, SizeUnit.MIB).conditionPasses(contextOn(jenkins)));
    }
}
