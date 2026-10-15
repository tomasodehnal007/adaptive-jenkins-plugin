package io.jenkins.plugins.adaptiveagent;

import static io.jenkins.plugins.adaptiveagent.TestSupport.agentWith;
import static io.jenkins.plugins.adaptiveagent.TestSupport.assertInOrder;
import static io.jenkins.plugins.adaptiveagent.TestSupport.countLines;
import static io.jenkins.plugins.adaptiveagent.TestSupport.echo;
import static io.jenkins.plugins.adaptiveagent.TestSupport.post;
import static io.jenkins.plugins.adaptiveagent.TestSupport.pre;
import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.model.Result;
import io.jenkins.plugins.adaptiveagent.action.ShellScriptAction;
import io.jenkins.plugins.adaptiveagent.condition.NoCondition;
import io.jenkins.plugins.adaptiveagent.entry.PreBuildEntry;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class PipelineTest {

    @Test
    void eachAgentRunsItsTasksAroundItsNodeBlock(JenkinsRule jenkins) throws Exception {
        agentWith(jenkins, "agent-a", pre(new NoCondition(), echo("PRE_A")), post(new NoCondition(), echo("POST_A")));
        agentWith(jenkins, "agent-b", pre(new NoCondition(), echo("PRE_B")), post(new NoCondition(), echo("POST_B")));

        WorkflowRun build = run(jenkins, "node('agent-a') { echo 'A' }\nnode('agent-b') { echo 'B' }", Result.SUCCESS);

        assertInOrder(JenkinsRule.getLog(build), "PRE_A", "A", "POST_A", "PRE_B", "B", "POST_B");
    }

    @Test
    void failingTaskBeforeTheNodeBlockAbortsThePipelineWhenSetTo(JenkinsRule jenkins) throws Exception {
        PreBuildEntry failing = pre(new NoCondition(), new ShellScriptAction("exit 5"));
        failing.setAbortBuildOnFailure(true);
        agentWith(jenkins, "agent-a", failing);

        // repeated, because an abort that arrived too early used to get lost only now and then
        for (int i = 0; i < 3; i++) {
            WorkflowRun build = run(jenkins, "node('agent-a') { echo 'SHOULD_NOT_RUN' }", Result.FAILURE);

            assertEquals(0, countLines(JenkinsRule.getLog(build), "SHOULD_NOT_RUN"));
        }
    }

    private static WorkflowRun run(JenkinsRule jenkins, String script, Result expected) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition(script, true));
        return jenkins.assertBuildStatus(expected, job.scheduleBuild2(0));
    }
}
