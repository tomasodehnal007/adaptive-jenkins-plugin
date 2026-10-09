package io.jenkins.plugins.adaptiveagent;

import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/** A task that runs while a build is running on the agent. */
public class DuringBuildEntry extends BuildEntry {

    /** Creates the entry; Jenkins calls this when the agent's configuration form is saved. */
    @DataBoundConstructor
    public DuringBuildEntry() {}

    /**
     * {@inheritDoc}
     *
     * @return {@link Phase#DURING_BUILD}
     */
    @Override
    public Phase getPhase() {
        return Phase.DURING_BUILD;
    }

    /** Describes this entry type; its display name is the item offered in the "Add task" menu. */
    @Extension
    public static class DescriptorImpl extends Descriptor<BuildEntry> {

        /** Creates the descriptor; Jenkins creates the single instance because of {@link Extension}. */
        public DescriptorImpl() {}

        @Override
        public String getDisplayName() {
            return "Action run during build";
        }
    }
}
