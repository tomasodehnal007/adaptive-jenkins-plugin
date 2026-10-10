package io.jenkins.plugins.adaptiveagent;

import static io.jenkins.plugins.adaptiveagent.TestSupport.agentWith;
import static io.jenkins.plugins.adaptiveagent.TestSupport.post;
import static io.jenkins.plugins.adaptiveagent.TestSupport.pre;
import static io.jenkins.plugins.adaptiveagent.TestSupport.projectOn;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.FilePath;
import hudson.model.FreeStyleProject;
import hudson.slaves.DumbSlave;
import io.jenkins.plugins.adaptiveagent.action.CleanWorkspaceAction;
import io.jenkins.plugins.adaptiveagent.condition.NoCondition;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** Every action does its work inside a real build on a real agent. */
@WithJenkins
class ActionsFlowTest {

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
}
