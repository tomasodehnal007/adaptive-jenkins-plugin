package io.jenkins.plugins.adaptiveagent;

import hudson.slaves.DumbSlave;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class NodePropertyImplTest {

    /** Same as the user opening the agent's Configure page and pressing Save without changing anything. */
    @Test
    void configRoundtrip(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = jenkins.createSlave();
        NodePropertyImpl property = new NodePropertyImpl(List.of(
                new PreBuildEntry(new NoCondition(), new ShellScriptAction("echo before")),
                new DuringBuildEntry(new NoCondition(), new ShellScriptAction("echo during")),
                new PostBuildEntry(new NoCondition(), new ShellScriptAction("echo after"))));
        agent.setNodeProperties(List.of(property));

        DumbSlave reloaded = jenkins.configRoundtrip(agent);

        NodePropertyImpl actual = reloaded.getNodeProperties().get(NodePropertyImpl.class);
        jenkins.assertEqualDataBoundBeans(property.getEntries(), actual.getEntries());
    }
}
