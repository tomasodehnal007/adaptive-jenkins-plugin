package io.jenkins.plugins.adaptiveagent.entry;

import hudson.Extension;
import io.jenkins.plugins.adaptiveagent.Phase;
import io.jenkins.plugins.adaptiveagent.action.Action;
import io.jenkins.plugins.adaptiveagent.condition.Condition;
import org.kohsuke.stapler.DataBoundConstructor;

/** A task that runs after a build has finished on the agent. */
public final class PostBuildEntry extends BuildEntry {

    /**
     * @param condition decides whether the action runs
     * @param action what is done on the agent
     */
    @DataBoundConstructor
    public PostBuildEntry(Condition condition, Action action) {
        super(condition, action);
    }

    @Override
    public Phase getPhase() {
        return Phase.POST_BUILD;
    }

    /** The ordinal puts it in the "Add task" menu in the order of the build: before, during, after. */
    @Extension(ordinal = 1)
    public static class DescriptorImpl extends BuildEntryDescriptor {

        @Override
        public Phase getPhase() {
            return Phase.POST_BUILD;
        }

        @Override
        public String getDisplayName() {
            return "Action run after build";
        }
    }
}
