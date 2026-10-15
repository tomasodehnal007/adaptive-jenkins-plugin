package io.jenkins.plugins.adaptiveagent.action;

import static io.jenkins.plugins.adaptiveagent.TestSupport.countLines;
import static io.jenkins.plugins.adaptiveagent.TestSupport.post;
import static io.jenkins.plugins.adaptiveagent.TestSupport.projectOn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.DumbSlave;
import io.jenkins.plugins.adaptiveagent.NodePropertyImpl;
import io.jenkins.plugins.adaptiveagent.condition.ScriptCondition;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * The agent has two executors: one runs a build that lasts until the test releases it, the other a build after
 * which the agent reboots. Only the build that creates the file {@code trigger} reboots, otherwise both builds
 * would wait for each other. The reboot command is replaced by a harmless one.
 */
@WithJenkins
class GracefulRebootActionTest {

    @TempDir
    Path tempDir;

    @Test
    void waitsUntilTheOtherBuildsOnTheAgentHaveFinished(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = new DumbSlave(
                "two-executors",
                Files.createDirectories(tempDir.resolve("agent")).toString(),
                jenkins.createComputerLauncher(null));
        agent.setNumExecutors(2);
        agent.setNodeProperties(List.of(new NodePropertyImpl(List.of(
                post(new ScriptCondition("test -f trigger"), new GracefulRebootAction("echo REBOOT_COMMAND"))))));
        jenkins.jenkins.addNode(agent);
        jenkins.waitOnline(agent);
        Path release = tempDir.resolve("release");
        FreeStyleProject longJob = projectOn(jenkins, agent, "while [ ! -f '" + release + "' ]; do sleep 0.2; done");
        FreeStyleProject rebootingJob = projectOn(jenkins, agent, "touch trigger");

        QueueTaskFuture<FreeStyleBuild> longBuild = longJob.scheduleBuild2(0);
        longBuild.waitForStart();
        QueueTaskFuture<FreeStyleBuild> rebootingBuild = rebootingJob.scheduleBuild2(0);
        FreeStyleBuild rebooting = rebootingBuild.waitForStart();
        jenkins.waitForMessage("Waiting for the other builds", rebooting);
        Thread.sleep(2_000); // long enough for the command to have run if the action did not wait

        assertTrue(rebooting.isInProgress());
        assertEquals(0, countLines(JenkinsRule.getLog(rebooting), "REBOOT_COMMAND"));

        Files.createFile(release);
        jenkins.assertBuildStatusSuccess(longBuild);
        jenkins.assertBuildStatusSuccess(rebootingBuild);
        assertEquals(1, countLines(JenkinsRule.getLog(rebooting), "REBOOT_COMMAND"));
    }
}
