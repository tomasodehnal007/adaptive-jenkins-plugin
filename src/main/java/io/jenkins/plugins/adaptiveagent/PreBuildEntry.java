package io.jenkins.plugins.adaptiveagent;

import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/** A task that runs before a build starts on the agent. */
public class PreBuildEntry extends BuildEntry {

    /** Creates the entry; Jenkins calls this when the agent's configuration form is saved. */
    @DataBoundConstructor
    public PreBuildEntry() {}

    /**
     * {@inheritDoc}
     *
     * @return {@link Phase#PRE_BUILD}
     */
    @Override
    public Phase getPhase() {
        return Phase.PRE_BUILD;
    }

    /** Describes this entry type; its display name is the item offered in the "Add task" menu. */
    @Extension
    public static class DescriptorImpl extends Descriptor<BuildEntry> {

        /** Creates the descriptor; Jenkins creates the single instance because of {@link Extension}. */
        public DescriptorImpl() {}

        @Override
        public String getDisplayName() {
            return "Action run before build";
        }
    }
}
