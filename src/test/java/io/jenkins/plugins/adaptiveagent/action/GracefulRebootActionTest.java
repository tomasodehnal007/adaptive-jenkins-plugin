package io.jenkins.plugins.adaptiveagent.action;

import static io.jenkins.plugins.adaptiveagent.TestSupport.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import hudson.slaves.DumbSlave;
import io.jenkins.plugins.adaptiveagent.NodePropertyImpl;
import io.jenkins.plugins.adaptiveagent.condition.NoCondition;
import io.jenkins.plugins.adaptiveagent.entry.BuildEntry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** The graceful variant has no form of its own: it must still save and reload the command of its parent. */
@WithJenkins
class GracefulRebootActionTest {

    @Test
    void theFormOfTheParentSavesTheCommand(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = jenkins.createSlave();
        agent.setNodeProperties(List.of(new NodePropertyImpl(
                List.of(post(new NoCondition(), new GracefulRebootAction("sudo -n shutdown -r now"))))));

        jenkins.configRoundtrip(agent);

        NodePropertyImpl property = agent.getNodeProperties().get(NodePropertyImpl.class);
        BuildEntry entry = property.getEntries().get(0);
        GracefulRebootAction action = assertInstanceOf(GracefulRebootAction.class, entry.getAction());
        assertEquals("sudo -n shutdown -r now", action.getCommand());
    }
}
