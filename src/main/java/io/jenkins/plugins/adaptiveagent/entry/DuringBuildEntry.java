package io.jenkins.plugins.adaptiveagent.entry;

import hudson.Extension;
import io.jenkins.plugins.adaptiveagent.Phase;
import io.jenkins.plugins.adaptiveagent.action.Action;
import io.jenkins.plugins.adaptiveagent.condition.Condition;
import org.kohsuke.stapler.DataBoundConstructor;

/** A task that runs while a build is running on the agent. */
public class DuringBuildEntry extends BuildEntry {

    /**
     * Creates the entry; Jenkins calls this when the agent's configuration form is saved.
     *
     * @param condition decides whether the action runs
     * @param action what is done on the agent
     */
    @DataBoundConstructor
    public DuringBuildEntry(Condition condition, Action action) {
        super(condition, action);
    }

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
    public static class DescriptorImpl extends BuildEntryDescriptor {

        /** Creates the descriptor; Jenkins creates the single instance because of {@link Extension}. */
        public DescriptorImpl() {}

        /**
         * {@inheritDoc}
         *
         * @return {@link Phase#DURING_BUILD}
         */
        @Override
        public Phase getPhase() {
            return Phase.DURING_BUILD;
        }

        @Override
        public String getDisplayName() {
            return "Action run during build";
        }
    }
}
