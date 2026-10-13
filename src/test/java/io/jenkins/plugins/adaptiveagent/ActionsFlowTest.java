package io.jenkins.plugins.adaptiveagent;

import static io.jenkins.plugins.adaptiveagent.TestSupport.agentWith;
import static io.jenkins.plugins.adaptiveagent.TestSupport.post;
import static io.jenkins.plugins.adaptiveagent.TestSupport.pre;
import static io.jenkins.plugins.adaptiveagent.TestSupport.projectOn;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.DumbSlave;
import io.jenkins.plugins.adaptiveagent.action.CleanWorkspaceAction;
import io.jenkins.plugins.adaptiveagent.action.GracefulRebootAction;
import io.jenkins.plugins.adaptiveagent.action.GroovyScriptAction;
import io.jenkins.plugins.adaptiveagent.action.RebootAction;
import io.jenkins.plugins.adaptiveagent.action.SetOfflineAction;
import io.jenkins.plugins.adaptiveagent.action.StopBuildAction;
import io.jenkins.plugins.adaptiveagent.condition.NoCondition;
import io.jenkins.plugins.adaptiveagent.condition.ScriptCondition;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** Every action does its work inside a real build on a real agent. */
@WithJenkins
class ActionsFlowTest {

    @TempDir
    Path tempDir;

    @Test
    void cleanWorkspaceAfterTheBuild(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(jenkins, post(new NoCondition(), new CleanWorkspaceAction()));
        FreeStyleProject project = projectOn(jenkins, agent, "touch marker");

        jenkins.buildAndAssertSuccess(project);

        FilePath workspace = agent.getWorkspaceFor(project);
        assertNotNull(workspace);
        assertFalse(workspace.exists(), "the workspace should have been deleted");
    }

    /** Before the build Jenkins has not allocated the workspace yet; the old one must still be found and cleaned. */
    @Test
    void cleanWorkspaceBeforeTheBuild(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = jenkins.createOnlineSlave();
        FreeStyleProject project = projectOn(jenkins, agent, "test ! -e marker && touch marker");
        jenkins.buildAndAssertSuccess(project); // leaves "marker" in the workspace

        agent.setNodeProperties(
                List.of(new NodePropertyImpl(List.of(pre(new NoCondition(), new CleanWorkspaceAction())))));

        // without the cleanup this second build would find the marker and fail
        jenkins.buildAndAssertSuccess(project);
    }

    @Test
    void setOfflineAfterTheBuild(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(jenkins, post(new NoCondition(), new SetOfflineAction()));
        FreeStyleProject project = projectOn(jenkins, agent, "echo hello");

        jenkins.buildAndAssertSuccess(project);

        Computer computer = agent.toComputer();
        assertNotNull(computer);
        assertTrue(computer.isOffline(), "the agent should be offline");
        assertTrue(computer.isConnected(), "the agent should stay connected, only paused");
        assertTrue(computer.getOfflineCauseReason().contains(project.getFullDisplayName()));
    }

    /** Going offline does not abort the build that triggered it. */
    @Test
    void setOfflineBeforeTheBuildLetsTheBuildFinish(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(jenkins, pre(new NoCondition(), new SetOfflineAction()));
        FreeStyleProject project = projectOn(jenkins, agent, "echo hello");

        jenkins.buildAndAssertSuccess(project);

        assertTrue(agent.toComputer().isOffline());
    }

    /** Stopping before the build aborts it, so its steps never run. */
    @Test
    void stopBuildBeforeTheBuild(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(jenkins, pre(new NoCondition(), new StopBuildAction()));
        FreeStyleProject project = projectOn(jenkins, agent, "echo SHOULD_NOT_RUN");

        FreeStyleBuild build = jenkins.assertBuildStatus(Result.ABORTED, project.scheduleBuild2(0));

        jenkins.assertLogNotContains("SHOULD_NOT_RUN", build);
    }

    /** The script runs in the JVM of the agent and its output ends up in the build log. */
    @Test
    void groovyScriptRunsOnTheAgentAndLogsItsOutput(JenkinsRule jenkins) throws Exception {
        DumbSlave agent =
                agentWith(jenkins, post(new NoCondition(), new GroovyScriptAction("println 'GROOVY_' + (40 + 2)")));
        FreeStyleProject project = projectOn(jenkins, agent, "echo hello");

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        jenkins.assertLogContains("GROOVY_42", build);
    }

    /** The reboot command is replaced by a harmless one: the agent goes offline and the command runs. */
    @Test
    void rebootTakesTheAgentOfflineAndRunsTheCommand(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(jenkins, post(new NoCondition(), new RebootAction("echo REBOOT_COMMAND")));
        FreeStyleProject project = projectOn(jenkins, agent, "echo hello");

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        jenkins.assertLogContains("REBOOT_COMMAND", build);
        assertTrue(agent.toComputer().isOffline());
    }

    /**
     * The agent has two executors: one runs a build that lasts until the test releases it, the other the build
     * whose post-build task is the graceful reboot. The reboot command must not run before the first build ends.
     * (Only the build that creates the file {@code trigger} runs the task; otherwise both would wait for each other.)
     */
    @Test
    void gracefulRebootWaitsForTheOtherBuilds(JenkinsRule jenkins) throws Exception {
        Path root = Files.createDirectories(tempDir.resolve("agent"));
        DumbSlave agent = new DumbSlave("two-executors", root.toString(), jenkins.createComputerLauncher(null));
        agent.setNumExecutors(2);
        agent.setNodeProperties(List.of(new NodePropertyImpl(List.of(
                post(new ScriptCondition("test -f trigger"), new GracefulRebootAction("echo REBOOT_COMMAND"))))));
        jenkins.jenkins.addNode(agent);
        jenkins.waitOnline(agent);
        Path release = tempDir.resolve("release");
        FreeStyleProject longJob = projectOn(jenkins, agent, "while [ ! -f '" + release + "' ]; do sleep 0.2; done");
        FreeStyleProject triggerJob = projectOn(jenkins, agent, "touch trigger");

        QueueTaskFuture<FreeStyleBuild> longBuild = longJob.scheduleBuild2(0);
        longBuild.waitForStart();
        QueueTaskFuture<FreeStyleBuild> triggerBuild = triggerJob.scheduleBuild2(0);
        FreeStyleBuild trigger = triggerBuild.waitForStart();

        long deadline = System.currentTimeMillis() + 60_000;
        while (!trigger.getLog().contains("Waiting for the other builds") && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }
        Thread.sleep(3_000); // long enough to be sure the command would have run by now
        assertTrue(trigger.isInProgress(), "the build should still be waiting");
        jenkins.assertLogNotContains("REBOOT_COMMAND", trigger);

        Files.createFile(release);
        jenkins.assertBuildStatusSuccess(longBuild);
        jenkins.assertBuildStatusSuccess(triggerBuild);
        jenkins.assertLogContains("REBOOT_COMMAND", trigger);
        assertTrue(agent.toComputer().isOffline());
    }
}
