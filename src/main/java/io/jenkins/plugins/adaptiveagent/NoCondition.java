package io.jenkins.plugins.adaptiveagent;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

/** Condition without parameters: the action always runs. */
public class NoCondition extends Condition {

    /** Creates the condition; Jenkins calls this when the agent's configuration form is saved. */
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

    /** Describes this condition type; its display name is the item offered in the "Condition" dropdown. */
    @Extension
    public static final class DescriptorImpl extends TaskDescriptor<Condition> {

        /** Creates the descriptor; Jenkins creates the single instance because of {@link Extension}. */
        public DescriptorImpl() {}

        @Override
        public String getDisplayName() {
            return "No condition";
        }
    }
}
