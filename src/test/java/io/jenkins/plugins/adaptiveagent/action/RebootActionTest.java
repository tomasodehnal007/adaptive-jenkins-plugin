package io.jenkins.plugins.adaptiveagent.action;

import static io.jenkins.plugins.adaptiveagent.TestSupport.agentWith;
import static io.jenkins.plugins.adaptiveagent.TestSupport.countLines;
import static io.jenkins.plugins.adaptiveagent.TestSupport.post;
import static io.jenkins.plugins.adaptiveagent.TestSupport.projectOn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleBuild;
import hudson.slaves.DumbSlave;
import io.jenkins.plugins.adaptiveagent.condition.NoCondition;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** The reboot command is always replaced by a harmless one. */
@WithJenkins
class RebootActionTest {

    @Test
    void takesTheAgentOfflineAndRunsTheRebootCommand(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(jenkins, post(new NoCondition(), new RebootAction("echo REBOOT_COMMAND")));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(projectOn(jenkins, agent, "echo hello"));

        assertEquals(1, countLines(JenkinsRule.getLog(build), "REBOOT_COMMAND"));
        assertTrue(agent.toComputer().isOffline());
    }
}
