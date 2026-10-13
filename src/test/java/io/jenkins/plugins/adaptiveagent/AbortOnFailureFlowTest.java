package io.jenkins.plugins.adaptiveagent;

import static io.jenkins.plugins.adaptiveagent.TestSupport.agentWith;
import static io.jenkins.plugins.adaptiveagent.TestSupport.countLines;
import static io.jenkins.plugins.adaptiveagent.TestSupport.echo;
import static io.jenkins.plugins.adaptiveagent.TestSupport.pre;
import static io.jenkins.plugins.adaptiveagent.TestSupport.projectOn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.slaves.DumbSlave;
import io.jenkins.plugins.adaptiveagent.action.ShellScriptAction;
import io.jenkins.plugins.adaptiveagent.condition.NoCondition;
import io.jenkins.plugins.adaptiveagent.condition.ScriptCondition;
import io.jenkins.plugins.adaptiveagent.entry.PreBuildEntry;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** A task before the build that fails either aborts the build or is only logged, as configured. */
@WithJenkins
class AbortOnFailureFlowTest {

    @Test
    void failingTaskAbortsTheBuildWhenItIsSetTo(JenkinsRule jenkins) throws Exception {
        PreBuildEntry entry = pre(new NoCondition(), new ShellScriptAction("exit 5"));
        entry.setAbortBuildOnFailure(true);
        DumbSlave agent = agentWith(jenkins, entry);

        FreeStyleBuild build =
                jenkins.buildAndAssertStatus(Result.FAILURE, projectOn(jenkins, agent, "echo SHOULD_NOT_RUN"));

        String log = JenkinsRule.getLog(build);
        assertTrue(log.contains("aborting the build"), log);
        assertEquals(0, countLines(log, "SHOULD_NOT_RUN"), log);
    }

    @Test
    void failingTaskDoesNotAbortTheBuildByDefault(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(jenkins, pre(new NoCondition(), new ShellScriptAction("exit 5")));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(projectOn(jenkins, agent, "echo STILL_RUNS"));

        assertEquals(1, countLines(JenkinsRule.getLog(build), "STILL_RUNS"));
    }

    /** A task that only did not apply (its condition does not hold) is not a failure. */
    @Test
    void skippedTaskDoesNotAbortTheBuild(JenkinsRule jenkins) throws Exception {
        PreBuildEntry entry = pre(new ScriptCondition("exit 1"), new ShellScriptAction("exit 5"));
        entry.setAbortBuildOnFailure(true);
        DumbSlave agent = agentWith(jenkins, entry, pre(new NoCondition(), echo("OTHER")));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(projectOn(jenkins, agent, "echo STILL_RUNS"));

        assertEquals(1, countLines(JenkinsRule.getLog(build), "STILL_RUNS"));
    }

    /** After the abort the remaining tasks of the phase are not run. */
    @Test
    void remainingTasksAreNotRunAfterTheAbort(JenkinsRule jenkins) throws Exception {
        PreBuildEntry failing = pre(new NoCondition(), new ShellScriptAction("exit 5"));
        failing.setAbortBuildOnFailure(true);
        DumbSlave agent = agentWith(jenkins, failing, pre(new NoCondition(), echo("SECOND_TASK")));

        FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.FAILURE, projectOn(jenkins, agent, "echo body"));

        assertEquals(0, countLines(JenkinsRule.getLog(build), "SECOND_TASK"));
    }
}
