package io.jenkins.plugins.adaptiveagent;

import static io.jenkins.plugins.adaptiveagent.TestSupport.agentWith;
import static io.jenkins.plugins.adaptiveagent.TestSupport.countLines;
import static io.jenkins.plugins.adaptiveagent.TestSupport.echo;
import static io.jenkins.plugins.adaptiveagent.TestSupport.projectOn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.slaves.DumbSlave;
import io.jenkins.plugins.adaptiveagent.action.Action;
import io.jenkins.plugins.adaptiveagent.action.StopBuildAction;
import io.jenkins.plugins.adaptiveagent.condition.NoCondition;
import io.jenkins.plugins.adaptiveagent.entry.DuringBuildEntry;
import io.jenkins.plugins.adaptiveagent.util.IntervalUnit;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** Tasks checked periodically while a real build runs on a real agent. */
@WithJenkins
class DuringBuildFlowTest {

    private static DuringBuildEntry during(boolean repeat, Action action) {
        return new DuringBuildEntry(300, IntervalUnit.MILLISECONDS, repeat, new NoCondition(), action);
    }

    @Test
    void repeatedTaskRunsAgainAndAgain(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(jenkins, during(true, echo("TICK")));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(projectOn(jenkins, agent, "sleep 3"));

        long ticks = countLines(JenkinsRule.getLog(build), "TICK");
        assertTrue(ticks >= 3, "expected several ticks during a 3 second build, got " + ticks);
    }

    /** "Already performed" is remembered per build, so every build gets the task, not just the first one ever. */
    @Test
    void runOnceTaskRunsOncePerBuild(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(jenkins, during(false, echo("TICK")));
        FreeStyleProject project = projectOn(jenkins, agent, "sleep 2");

        String first = JenkinsRule.getLog(jenkins.buildAndAssertSuccess(project));
        String second = JenkinsRule.getLog(jenkins.buildAndAssertSuccess(project));

        assertEquals(1, countLines(first, "TICK"), first);
        assertEquals(1, countLines(second, "TICK"), second);
    }

    /**
     * The interval is long enough for the build to be running its step: an interrupt that arrives while the step
     * is still being launched makes Jenkins end the build as FAILURE, not as ABORTED.
     */
    @Test
    void duringTaskCanStopTheBuild(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(
                jenkins, new DuringBuildEntry(2, IntervalUnit.SECONDS, true, new NoCondition(), new StopBuildAction()));

        FreeStyleBuild build = jenkins.buildAndAssertStatus(
                Result.ABORTED, projectOn(jenkins, agent, "sleep 30; echo SHOULD_NOT_RUN"));

        assertFalse(JenkinsRule.getLog(build).contains("SHOULD_NOT_RUN"));
    }

    /** The first check comes after one interval, so a build shorter than that is never checked. */
    @Test
    void buildShorterThanTheIntervalIsNotChecked(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(
                jenkins, new DuringBuildEntry(1, IntervalUnit.MINUTES, true, new NoCondition(), echo("TICK")));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(projectOn(jenkins, agent, "echo hello"));

        assertEquals(0, countLines(JenkinsRule.getLog(build), "TICK"));
    }
}
