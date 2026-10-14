package io.jenkins.plugins.adaptiveagent.entry;

import hudson.Extension;
import io.jenkins.plugins.adaptiveagent.Phase;
import io.jenkins.plugins.adaptiveagent.action.Action;
import io.jenkins.plugins.adaptiveagent.condition.Condition;
import io.jenkins.plugins.adaptiveagent.util.IntervalUnit;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A task that is checked periodically while a build runs on the agent: every {@code interval} the
 * condition is evaluated and, if it holds, the action is performed.
 *
 * <p>This class holds only the configuration. Whether the task has already run in the current build is
 * remembered by the timer of that build, not here, because this object is shared by all builds.
 */
public final class DuringBuildEntry extends BuildEntry {

    private final long interval;
    private final IntervalUnit unit;
    private final boolean repeat;

    /**
     * The parameter names must match the field names in {@code config.jelly}.
     *
     * @param interval how often the condition is checked, in {@code unit}
     * @param unit the unit of {@code interval}
     * @param repeat {@code true} to keep checking until the build ends, {@code false} to stop after the
     *     action has run once
     * @param condition decides whether the action runs
     * @param action what is done on the agent
     */
    @DataBoundConstructor
    public DuringBuildEntry(long interval, IntervalUnit unit, boolean repeat, Condition condition, Action action) {
        super(condition, action);
        this.interval = interval;
        this.unit = unit;
        this.repeat = repeat;
    }

    public long getInterval() {
        return interval;
    }

    public IntervalUnit getUnit() {
        return unit;
    }

    /**
     * Tells whether the task is checked again after it has run.
     *
     * @return {@code true} to keep checking until the build ends, {@code false} to stop after the action
     *     has run once
     */
    public boolean isRepeat() {
        return repeat;
    }

    @Override
    public Phase getPhase() {
        return Phase.DURING_BUILD;
    }

    /** The ordinal puts it in the "Add task" menu in the order of the build: before, during, after. */
    @Extension(ordinal = 2)
    public static class DescriptorImpl extends BuildEntryDescriptor {

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
