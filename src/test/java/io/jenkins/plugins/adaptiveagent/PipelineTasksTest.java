package io.jenkins.plugins.adaptiveagent;

import static io.jenkins.plugins.adaptiveagent.TestSupport.countLines;
import static io.jenkins.plugins.adaptiveagent.TestSupport.echo;
import static io.jenkins.plugins.adaptiveagent.TestSupport.post;
import static io.jenkins.plugins.adaptiveagent.TestSupport.pre;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.Label;
import hudson.model.Result;
import hudson.slaves.DumbSlave;
import io.jenkins.plugins.adaptiveagent.action.CleanWorkspaceAction;
import io.jenkins.plugins.adaptiveagent.action.ShellScriptAction;
import io.jenkins.plugins.adaptiveagent.action.StopBuildAction;
import io.jenkins.plugins.adaptiveagent.condition.DurationCondition;
import io.jenkins.plugins.adaptiveagent.condition.NoCondition;
import io.jenkins.plugins.adaptiveagent.entry.BuildEntry;
import io.jenkins.plugins.adaptiveagent.entry.DuringBuildEntry;
import io.jenkins.plugins.adaptiveagent.entry.PreBuildEntry;
import io.jenkins.plugins.adaptiveagent.util.DurationComparison;
import io.jenkins.plugins.adaptiveagent.util.IntervalUnit;
import java.util.List;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** The tasks configured on an agent also run around the {@code node {}} blocks of Pipeline builds. */
@WithJenkins
class PipelineTasksTest {

    private static DumbSlave agent(JenkinsRule jenkins, String label, BuildEntry... entries) throws Exception {
        DumbSlave agent = jenkins.createOnlineSlave(Label.get(label));
        agent.setNodeProperties(List.of(new NodePropertyImpl(List.of(entries))));
        return agent;
    }

    private static WorkflowRun run(JenkinsRule jenkins, String script, Result expected) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition(script, true));
        return jenkins.assertBuildStatus(expected, job.scheduleBuild2(0));
    }

    /** Asserts that the lines (each on a line of its own) appear in the log in this order. */
    private static void assertInOrder(String log, String... lines) {
        int from = 0;
        for (String line : lines) {
            int at = log.indexOf("\n" + line + "\n", from);
            assertTrue(at >= 0, "expected '" + line + "' after position " + from + " in:\n" + log);
            from = at + 1;
        }
    }

    @Test
    void tasksRunBeforeAndAfterTheNodeBlock(JenkinsRule jenkins) throws Exception {
        agent(jenkins, "agent-a", pre(new NoCondition(), echo("PRE_A")), post(new NoCondition(), echo("POST_A")));

        WorkflowRun build = run(jenkins, "node('agent-a') { echo 'BODY' }", Result.SUCCESS);

        assertInOrder(JenkinsRule.getLog(build), "PRE_A", "BODY", "POST_A");
    }

    /** A build with two node blocks on two agents: each agent runs its own tasks around its own block. */
    @Test
    void eachAgentRunsItsOwnTasks(JenkinsRule jenkins) throws Exception {
        agent(jenkins, "agent-a", pre(new NoCondition(), echo("PRE_A")), post(new NoCondition(), echo("POST_A")));
        agent(jenkins, "agent-b", pre(new NoCondition(), echo("PRE_B")), post(new NoCondition(), echo("POST_B")));

        WorkflowRun build = run(jenkins, "node('agent-a') { echo 'A' }\nnode('agent-b') { echo 'B' }", Result.SUCCESS);

        String log = JenkinsRule.getLog(build);
        assertInOrder(log, "PRE_A", "A", "POST_A", "PRE_B", "B", "POST_B");
        assertEquals(1, countLines(log, "PRE_A"));
        assertEquals(1, countLines(log, "PRE_B"));
    }

    @Test
    void nothingRunsOnAnAgentWithoutTasksOrOnTheController(JenkinsRule jenkins) throws Exception {
        agent(jenkins, "configured", pre(new NoCondition(), echo("PRE_CONFIGURED")));
        jenkins.createOnlineSlave(Label.get("plain"));

        WorkflowRun onPlain = run(jenkins, "node('plain') { echo 'BODY' }", Result.SUCCESS);
        WorkflowRun noNode = run(jenkins, "echo 'ON_CONTROLLER'", Result.SUCCESS);

        assertEquals(0, countLines(JenkinsRule.getLog(onPlain), "PRE_CONFIGURED"));
        assertEquals(0, countLines(JenkinsRule.getLog(noNode), "PRE_CONFIGURED"));
    }

    @Test
    void duringTaskTicksWhileTheNodeBlockRuns(JenkinsRule jenkins) throws Exception {
        agent(
                jenkins,
                "agent-a",
                new DuringBuildEntry(300, IntervalUnit.MILLISECONDS, true, new NoCondition(), echo("TICK")));

        WorkflowRun build = run(jenkins, "node('agent-a') { sh 'sleep 3' }", Result.SUCCESS);

        long ticks = countLines(JenkinsRule.getLog(build), "TICK");
        assertTrue(ticks >= 3, "expected several ticks during a 3 second block, got " + ticks);
    }

    @Test
    void failingTaskAbortsThePipelineWhenItIsSetTo(JenkinsRule jenkins) throws Exception {
        PreBuildEntry entry = pre(new NoCondition(), new ShellScriptAction("exit 5"));
        entry.setAbortBuildOnFailure(true);
        agent(jenkins, "agent-a", entry);

        // repeated, because an abort that arrives too early used to be lost only now and then
        for (int i = 0; i < 3; i++) {
            WorkflowRun build = run(jenkins, "node('agent-a') { echo 'SHOULD_NOT_RUN' }", Result.FAILURE);

            assertFalse(JenkinsRule.getLog(build).contains("\nSHOULD_NOT_RUN\n"), JenkinsRule.getLog(build));
        }
    }

    @Test
    void stopBuildTaskAbortsThePipeline(JenkinsRule jenkins) throws Exception {
        agent(jenkins, "agent-a", pre(new NoCondition(), new StopBuildAction()));

        WorkflowRun build = run(jenkins, "node('agent-a') { sh 'sleep 30'; echo 'SHOULD_NOT_RUN' }", Result.ABORTED);

        assertFalse(JenkinsRule.getLog(build).contains("\nSHOULD_NOT_RUN\n"));
    }

    /** The duration is that of the node block on the agent, not of the whole build, which already took longer. */
    @Test
    void durationIsMeasuredForTheNodeBlockOnly(JenkinsRule jenkins) throws Exception {
        DurationCondition longerThanTwoSeconds =
                new DurationCondition(DurationComparison.LONGER_THAN, 2, IntervalUnit.SECONDS);
        agent(jenkins, "agent-a", post(longerThanTwoSeconds, echo("LONG_BLOCK")));

        WorkflowRun shortBlock = run(jenkins, "sleep 3\nnode('agent-a') { echo 'BODY' }", Result.SUCCESS);
        WorkflowRun longBlock = run(jenkins, "node('agent-a') { sh 'sleep 3' }", Result.SUCCESS);

        assertEquals(0, countLines(JenkinsRule.getLog(shortBlock), "LONG_BLOCK"));
        assertEquals(1, countLines(JenkinsRule.getLog(longBlock), "LONG_BLOCK"));
    }

    /** The workspace of a node block is not known, so nothing is cleaned and no other workspace is touched. */
    @Test
    void cleanWorkspaceDoesNothingInPipeline(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agent(jenkins, "agent-a", post(new NoCondition(), new CleanWorkspaceAction()));

        WorkflowRun build = run(jenkins, "node('agent-a') { sh 'touch marker' }", Result.SUCCESS);

        assertTrue(agent.getWorkspaceFor(build.getParent()).child("marker").exists());
        assertTrue(JenkinsRule.getLog(build).contains("The workspace is not known"));
    }
}
