package io.jenkins.plugins.adaptiveagent.entry;

import hudson.Extension;
import io.jenkins.plugins.adaptiveagent.Phase;
import io.jenkins.plugins.adaptiveagent.action.Action;
import io.jenkins.plugins.adaptiveagent.condition.Condition;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/** A task that runs before a build starts on the agent. */
public class PreBuildEntry extends BuildEntry {

    private boolean abortBuildOnFailure;

    /**
     * Creates the entry; Jenkins calls this when the agent's configuration form is saved.
     *
     * @param condition decides whether the action runs
     * @param action what is done on the agent
     */
    @DataBoundConstructor
    public PreBuildEntry(Condition condition, Action action) {
        super(condition, action);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Phase#PRE_BUILD}
     */
    @Override
    public Phase getPhase() {
        return Phase.PRE_BUILD;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Off by default: a failing task is then only written into the build log.
     *
     * @return {@code true} if a failure of the action aborts the build, which then ends as FAILURE
     */
    @Override
    public boolean isAbortBuildOnFailure() {
        return abortBuildOnFailure;
    }

    /**
     * Sets whether a failure of the action aborts the build. It is an optional setting, hence a setter
     * and not a constructor parameter.
     *
     * @param abortBuildOnFailure {@code true} to abort the build when the action fails
     */
    @DataBoundSetter
    public void setAbortBuildOnFailure(boolean abortBuildOnFailure) {
        this.abortBuildOnFailure = abortBuildOnFailure;
    }

    /**
     * Describes this entry type; its display name is the item offered in the "Add task" menu. The ordinal puts
     * it in the menu in the order of the build: before, during, after.
     */
    @Extension(ordinal = 3)
    public static class DescriptorImpl extends BuildEntryDescriptor {

        /** Creates the descriptor; Jenkins creates the single instance because of {@link Extension}. */
        public DescriptorImpl() {}

        /**
         * {@inheritDoc}
         *
         * @return {@link Phase#PRE_BUILD}
         */
        @Override
        public Phase getPhase() {
            return Phase.PRE_BUILD;
        }

        @Override
        public String getDisplayName() {
            return "Action run before build";
        }
    }
}
