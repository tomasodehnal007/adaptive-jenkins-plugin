package io.jenkins.plugins.adaptiveagent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.slaves.DumbSlave;
import hudson.tasks.Shell;
import io.jenkins.plugins.adaptiveagent.action.ShellScriptAction;
import io.jenkins.plugins.adaptiveagent.condition.NoCondition;
import io.jenkins.plugins.adaptiveagent.condition.ResultCondition;
import io.jenkins.plugins.adaptiveagent.entry.BuildEntry;
import io.jenkins.plugins.adaptiveagent.entry.PostBuildEntry;
import io.jenkins.plugins.adaptiveagent.entry.PreBuildEntry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** Tasks configured on an agent are run around Freestyle builds executed on that agent. */
@WithJenkins
class FreestyleTasksTest {

    private static DumbSlave agentWith(JenkinsRule jenkins, BuildEntry... entries) throws Exception {
        DumbSlave agent = jenkins.createOnlineSlave();
        agent.setNodeProperties(List.of(new NodePropertyImpl(List.of(entries))));
        return agent;
    }

    private static FreeStyleProject projectOn(JenkinsRule jenkins, DumbSlave agent, String buildScript)
            throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setAssignedNode(agent);
        project.getBuildersList().add(new Shell(buildScript));
        return project;
    }

    private static PreBuildEntry preBuild(String script) {
        return new PreBuildEntry(new NoCondition(), new ShellScriptAction(script));
    }

    private static PostBuildEntry postBuild(ResultCondition condition, String script) {
        return new PostBuildEntry(condition, new ShellScriptAction(script));
    }

    private static ResultCondition onSuccess() {
        return new ResultCondition(true, false, false, false, false);
    }

    private static ResultCondition onFailure() {
        return new ResultCondition(false, true, false, false, false);
    }

    private static ResultCondition onAborted() {
        return new ResultCondition(false, false, true, false, false);
    }

    @Test
    void preTaskRunsBeforeAndPostTaskAfterTheBuild(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(
                jenkins,
                preBuild("echo PRE_TASK"),
                postBuild(onSuccess(), "echo POST_ON_SUCCESS"),
                postBuild(onFailure(), "echo POST_ON_FAILURE"));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(projectOn(jenkins, agent, "echo BUILD_STEP"));

        String log = JenkinsRule.getLog(build);
        int pre = log.indexOf("PRE_TASK");
        int step = log.indexOf("BUILD_STEP");
        int post = log.indexOf("POST_ON_SUCCESS");
        assertTrue(pre >= 0 && step >= 0 && post >= 0, log);
        assertTrue(pre < step && step < post, "expected pre < build step < post, but log was:\n" + log);
        assertFalse(log.contains("POST_ON_FAILURE"), log);
    }

    /** The core idea of the plugin: tasks follow the agent, whatever job runs on it. */
    @Test
    void tasksBelongToTheAgentNotToTheJob(JenkinsRule jenkins) throws Exception {
        DumbSlave configured = agentWith(jenkins, preBuild("echo PRE_TASK"));
        DumbSlave plain = jenkins.createOnlineSlave();

        FreeStyleBuild firstJob = jenkins.buildAndAssertSuccess(projectOn(jenkins, configured, "true"));
        FreeStyleBuild otherJob = jenkins.buildAndAssertSuccess(projectOn(jenkins, configured, "true"));
        FreeStyleBuild onOtherAgent = jenkins.buildAndAssertSuccess(projectOn(jenkins, plain, "true"));

        assertTrue(JenkinsRule.getLog(firstJob).contains("PRE_TASK"));
        assertTrue(JenkinsRule.getLog(otherJob).contains("PRE_TASK"));
        assertFalse(JenkinsRule.getLog(onOtherAgent).contains("PRE_TASK"));
    }

    @Test
    void failingTaskIsReportedButDoesNotFailTheBuild(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(jenkins, preBuild("exit 3"), preBuild("echo SECOND_TASK"));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(projectOn(jenkins, agent, "true"));

        String log = JenkinsRule.getLog(build);
        assertTrue(log.contains("exited with code 3"), log);
        assertTrue(log.contains("SECOND_TASK"), "a failing task must not stop the following ones:\n" + log);
    }

    /** Before the build Jenkins has not allocated the workspace yet, so the script runs in the agent's root. */
    @Test
    void preBuildScriptRunsInAShellInTheAgentRootDirectory(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(jenkins, preBuild("echo FIRST; echo \"IN_DIR=$(pwd)\" | cat"));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(projectOn(jenkins, agent, "true"));

        String log = JenkinsRule.getLog(build);
        assertTrue(log.contains("FIRST"), log); // ';' and '|' only work in a real shell
        assertTrue(log.contains("IN_DIR=" + agent.getRemoteFS() + "\n"), "expected the agent root, log was:\n" + log);
    }

    /** A task meant for aborted builds (e.g. a cleanup) must still run after the user stops the build. */
    @Test
    void postTaskRunsAfterTheBuildWasAborted(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(jenkins, postBuild(onAborted(), "echo POST_ON_ABORTED"));
        FreeStyleProject project = projectOn(jenkins, agent, "echo BUILD_STARTED; sleep 30");

        FreeStyleBuild build = project.scheduleBuild2(0).waitForStart();
        jenkins.waitForMessage("BUILD_STARTED", build);
        build.getExecutor().interrupt();
        jenkins.waitForCompletion(build);

        jenkins.assertBuildStatus(Result.ABORTED, build);
        String log = JenkinsRule.getLog(build);
        assertTrue(log.contains("POST_ON_ABORTED"), log);
    }
}
