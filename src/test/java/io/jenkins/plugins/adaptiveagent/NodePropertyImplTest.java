package io.jenkins.plugins.adaptiveagent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.model.Descriptor;
import hudson.slaves.DumbSlave;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
                new PostBuildEntry(
                        new ResultCondition(false, true, false, true, false), new ShellScriptAction("echo after"))));
        agent.setNodeProperties(List.of(property));

        DumbSlave reloaded = jenkins.configRoundtrip(agent);

        NodePropertyImpl actual = reloaded.getNodeProperties().get(NodePropertyImpl.class);
        jenkins.assertEqualDataBoundBeans(property.getEntries(), actual.getEntries());
    }

    /** The result of a build is known only after it, so that condition is offered only for "after build". */
    @Test
    void resultConditionIsOfferedOnlyAfterTheBuild(JenkinsRule jenkins) {
        BuildEntry.BuildEntryDescriptor pre = jenkins.jenkins.getDescriptorByType(PreBuildEntry.DescriptorImpl.class);
        BuildEntry.BuildEntryDescriptor during =
                jenkins.jenkins.getDescriptorByType(DuringBuildEntry.DescriptorImpl.class);
        BuildEntry.BuildEntryDescriptor post = jenkins.jenkins.getDescriptorByType(PostBuildEntry.DescriptorImpl.class);

        assertEquals(Set.of(NoCondition.class), classesOf(pre.getConditionDescriptors()));
        assertEquals(Set.of(NoCondition.class), classesOf(during.getConditionDescriptors()));
        assertEquals(Set.of(NoCondition.class, ResultCondition.class), classesOf(post.getConditionDescriptors()));
    }

    private static Set<Class<?>> classesOf(List<? extends Descriptor<?>> descriptors) {
        return descriptors.stream().map(descriptor -> descriptor.clazz).collect(Collectors.toSet());
    }
}
