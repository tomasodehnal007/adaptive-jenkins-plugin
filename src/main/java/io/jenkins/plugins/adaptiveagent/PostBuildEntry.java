package io.jenkins.plugins.adaptiveagent;

import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/** A task that runs after a build has finished on the agent. */
public class PostBuildEntry extends BuildEntry {

    /**
     * Creates the entry; Jenkins calls this when the agent's configuration form is saved.
     *
     * @param condition decides whether the action runs
     * @param action what is done on the agent
     */
    @DataBoundConstructor
    public PostBuildEntry(Condition condition, Action action) {
        super(condition, action);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Phase#POST_BUILD}
     */
    @Override
    public Phase getPhase() {
        return Phase.POST_BUILD;
    }

    /** Describes this entry type; its display name is the item offered in the "Add task" menu. */
    @Extension
    public static class DescriptorImpl extends Descriptor<BuildEntry> {

        /** Creates the descriptor; Jenkins creates the single instance because of {@link Extension}. */
        public DescriptorImpl() {}

        @Override
        public String getDisplayName() {
            return "Action run after build";
        }
    }
}
