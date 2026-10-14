package io.jenkins.plugins.adaptiveagent.condition;

import hudson.Extension;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.TaskDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/** Condition without parameters: the action always runs. */
public class NoCondition extends Condition {

    @DataBoundConstructor
    public NoCondition() {}

    /**
     * {@inheritDoc}
     *
     * @return always {@code true}
     */
    @Override
    public boolean conditionPasses(TaskContext context) {
        return true;
    }

    @Extension(ordinal = 100)
    public static final class DescriptorImpl extends TaskDescriptor<Condition> {

        @Override
        public String getDisplayName() {
            return "No condition";
        }
    }
}
