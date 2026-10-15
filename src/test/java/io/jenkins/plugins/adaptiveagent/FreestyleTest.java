package io.jenkins.plugins.adaptiveagent;

import static io.jenkins.plugins.adaptiveagent.TestSupport.agentWith;
import static io.jenkins.plugins.adaptiveagent.TestSupport.assertInOrder;
import static io.jenkins.plugins.adaptiveagent.TestSupport.countLines;
import static io.jenkins.plugins.adaptiveagent.TestSupport.echo;
import static io.jenkins.plugins.adaptiveagent.TestSupport.post;
import static io.jenkins.plugins.adaptiveagent.TestSupport.pre;
import static io.jenkins.plugins.adaptiveagent.TestSupport.projectOn;
import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.slaves.DumbSlave;
import io.jenkins.plugins.adaptiveagent.action.ShellScriptAction;
import io.jenkins.plugins.adaptiveagent.condition.NoCondition;
import io.jenkins.plugins.adaptiveagent.entry.DuringBuildEntry;
import io.jenkins.plugins.adaptiveagent.entry.PreBuildEntry;
import io.jenkins.plugins.adaptiveagent.util.IntervalUnit;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class FreestyleTest {

    @Test
    void tasksRunBeforeDuringAndAfterTheBuild(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(
                jenkins,
                pre(new NoCondition(), echo("PRE")),
                new DuringBuildEntry(1, IntervalUnit.SECONDS, true, new NoCondition(), echo("TICK")),
                post(new NoCondition(), echo("POST")));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(projectOn(jenkins, agent, "echo BODY; sleep 3"));

        String log = JenkinsRule.getLog(build);
        assertInOrder(log, "PRE", "BODY", "POST");
        assertInOrder(log, "PRE", "TICK", "POST");
    }

    @Test
    void tasksBelongToTheAgentNotToTheJob(JenkinsRule jenkins) throws Exception {
        DumbSlave configured = agentWith(jenkins, pre(new NoCondition(), echo("PRE")));
        DumbSlave plain = jenkins.createOnlineSlave();

        FreeStyleBuild firstJob = jenkins.buildAndAssertSuccess(projectOn(jenkins, configured, "true"));
        FreeStyleBuild secondJob = jenkins.buildAndAssertSuccess(projectOn(jenkins, configured, "true"));
        FreeStyleBuild onOtherAgent = jenkins.buildAndAssertSuccess(projectOn(jenkins, plain, "true"));

        assertEquals(1, countLines(JenkinsRule.getLog(firstJob), "PRE"));
        assertEquals(1, countLines(JenkinsRule.getLog(secondJob), "PRE"));
        assertEquals(0, countLines(JenkinsRule.getLog(onOtherAgent), "PRE"));
    }

    @Test
    void failingTaskBeforeTheBuildAbortsItWhenSetTo(JenkinsRule jenkins) throws Exception {
        PreBuildEntry failing = pre(new NoCondition(), new ShellScriptAction("exit 5"));
        failing.setAbortBuildOnFailure(true);
        DumbSlave agent = agentWith(jenkins, failing);

        FreeStyleBuild build =
                jenkins.buildAndAssertStatus(Result.FAILURE, projectOn(jenkins, agent, "echo SHOULD_NOT_RUN"));

        assertEquals(0, countLines(JenkinsRule.getLog(build), "SHOULD_NOT_RUN"));
    }
}
