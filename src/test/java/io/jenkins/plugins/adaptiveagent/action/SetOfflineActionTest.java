package io.jenkins.plugins.adaptiveagent.action;

import static io.jenkins.plugins.adaptiveagent.TestSupport.agentWith;
import static io.jenkins.plugins.adaptiveagent.TestSupport.post;
import static io.jenkins.plugins.adaptiveagent.TestSupport.projectOn;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.slaves.DumbSlave;
import io.jenkins.plugins.adaptiveagent.condition.NoCondition;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class SetOfflineActionTest {

    @Test
    void takesTheAgentOfflineAndNamesTheBuildAsTheReason(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(jenkins, post(new NoCondition(), new SetOfflineAction()));
        FreeStyleProject project = projectOn(jenkins, agent, "echo hello");

        jenkins.buildAndAssertSuccess(project);

        Computer computer = agent.toComputer();
        assertTrue(computer.isOffline());
        assertTrue(computer.getOfflineCauseReason().contains(project.getFullDisplayName()));
    }
}
