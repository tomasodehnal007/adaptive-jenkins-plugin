package io.jenkins.plugins.adaptiveagent;

import static hudson.model.Result.FAILURE;
import static io.jenkins.plugins.adaptiveagent.TestSupport.agentWith;
import static io.jenkins.plugins.adaptiveagent.TestSupport.countLines;
import static io.jenkins.plugins.adaptiveagent.TestSupport.echo;
import static io.jenkins.plugins.adaptiveagent.TestSupport.post;
import static io.jenkins.plugins.adaptiveagent.TestSupport.pre;
import static io.jenkins.plugins.adaptiveagent.TestSupport.projectOn;
import static io.jenkins.plugins.adaptiveagent.util.DurationComparison.LONGER_THAN;
import static io.jenkins.plugins.adaptiveagent.util.DurationComparison.SHORTER_THAN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.model.FreeStyleBuild;
import hudson.slaves.DumbSlave;
import io.jenkins.plugins.adaptiveagent.condition.DiskSpaceCondition;
import io.jenkins.plugins.adaptiveagent.condition.DurationCondition;
import io.jenkins.plugins.adaptiveagent.condition.HistoryFailedCondition;
import io.jenkins.plugins.adaptiveagent.condition.HistoryTimeCondition;
import io.jenkins.plugins.adaptiveagent.condition.ScriptCondition;
import io.jenkins.plugins.adaptiveagent.util.IntervalUnit;
import io.jenkins.plugins.adaptiveagent.util.SizeUnit;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Every condition decides, inside a real build on a real agent, whether its action runs. Each entry
 * prints its own marker, so the log shows which entries ran.
 */
@WithJenkins
class ConditionsFlowTest {

    private static void assertRan(String log, String marker) {
        assertEquals(1, countLines(log, marker), marker + " should have been printed, log was:\n" + log);
    }

    private static void assertNotRan(String log, String marker) {
        assertEquals(0, countLines(log, marker), marker + " should not have been printed, log was:\n" + log);
    }

    @Test
    void scriptDiskSpaceAndDurationConditionsDecideInOneBuild(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(
                jenkins,
                pre(new ScriptCondition("exit 0"), echo("SCRIPT_PASSES")),
                pre(new ScriptCondition("exit 1"), echo("SCRIPT_FAILS")),
                pre(new DiskSpaceCondition(1_000_000, SizeUnit.GIB), echo("DISK_LOW")),
                pre(new DiskSpaceCondition(0, SizeUnit.MIB), echo("DISK_NEVER_LOW")),
                post(new DurationCondition(LONGER_THAN, 10, IntervalUnit.MILLISECONDS), echo("LONGER_THAN_10_MS")),
                post(new DurationCondition(LONGER_THAN, 60, IntervalUnit.MINUTES), echo("LONGER_THAN_1_HOUR")),
                post(new DurationCondition(SHORTER_THAN, 60, IntervalUnit.MINUTES), echo("SHORTER_THAN_1_HOUR")));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(projectOn(jenkins, agent, "sleep 1"));

        String log = JenkinsRule.getLog(build);
        assertRan(log, "SCRIPT_PASSES");
        assertNotRan(log, "SCRIPT_FAILS");
        assertRan(log, "DISK_LOW");
        assertNotRan(log, "DISK_NEVER_LOW");
        assertRan(log, "LONGER_THAN_10_MS");
        assertNotRan(log, "LONGER_THAN_1_HOUR");
        assertRan(log, "SHORTER_THAN_1_HOUR");
    }

    /** The history is that of the agent: builds of different jobs count together. */
    @Test
    void historyConditionsLookAtEarlierBuildsOnTheAgent(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(
                jenkins,
                pre(new HistoryFailedCondition(2), echo("TWO_FAILED")),
                pre(new HistoryTimeCondition(2, SHORTER_THAN, 60, IntervalUnit.MINUTES), echo("QUICK")),
                pre(new HistoryTimeCondition(2, LONGER_THAN, 60, IntervalUnit.MINUTES), echo("SLOW")));

        FreeStyleBuild first = jenkins.buildAndAssertStatus(FAILURE, projectOn(jenkins, agent, "exit 1"));
        FreeStyleBuild second = jenkins.buildAndAssertStatus(FAILURE, projectOn(jenkins, agent, "exit 1"));
        FreeStyleBuild third = jenkins.buildAndAssertSuccess(projectOn(jenkins, agent, "true"));
        FreeStyleBuild fourth = jenkins.buildAndAssertSuccess(projectOn(jenkins, agent, "true"));

        // no history yet, and one earlier build is not enough for a count of two
        assertNotRan(JenkinsRule.getLog(first), "TWO_FAILED");
        assertNotRan(JenkinsRule.getLog(first), "QUICK");
        assertNotRan(JenkinsRule.getLog(second), "TWO_FAILED");
        assertNotRan(JenkinsRule.getLog(second), "QUICK");
        // the two earlier builds failed and were quick
        assertRan(JenkinsRule.getLog(third), "TWO_FAILED");
        assertRan(JenkinsRule.getLog(third), "QUICK");
        assertNotRan(JenkinsRule.getLog(third), "SLOW");
        // the latest earlier build succeeded
        assertNotRan(JenkinsRule.getLog(fourth), "TWO_FAILED");
        assertRan(JenkinsRule.getLog(fourth), "QUICK");
        assertNotRan(JenkinsRule.getLog(fourth), "SLOW");
    }
}
