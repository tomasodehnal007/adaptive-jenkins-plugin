package io.jenkins.plugins.adaptiveagent.condition;

import hudson.Extension;
import hudson.model.Run;
import io.jenkins.plugins.adaptiveagent.Phase;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.TaskDescriptor;
import io.jenkins.plugins.adaptiveagent.util.DurationComparison;
import io.jenkins.plugins.adaptiveagent.util.IntervalUnit;
import org.kohsuke.stapler.DataBoundConstructor;

/** Condition on how long the build took. It only makes sense after the build, so it is offered only then. */
public class DurationCondition extends Condition {

    private final DurationComparison comparison;
    private final long time;
    private final IntervalUnit unit;

    /**
     * Creates the condition; Jenkins calls this when the agent's configuration form is saved.
     *
     * @param comparison whether the build must be longer or shorter than {@code time}
     * @param time the limit, in {@code unit}
     * @param unit the unit of {@code time}
     */
    @DataBoundConstructor
    public DurationCondition(DurationComparison comparison, long time, IntervalUnit unit) {
        this.comparison = comparison;
        this.time = time;
        this.unit = unit;
    }

    /**
     * Returns whether the build must be longer or shorter than the limit.
     *
     * @return the comparison
     */
    public DurationComparison getComparison() {
        return comparison;
    }

    /**
     * Returns the limit; the form shows it in the "Duration" field.
     *
     * @return the limit, in {@link #getUnit()}
     */
    public long getTime() {
        return time;
    }

    /**
     * Returns the unit of the limit.
     *
     * @return the unit
     */
    public IntervalUnit getUnit() {
        return unit;
    }

    /**
     * Decides whether a build of the given length satisfies the condition.
     *
     * @param durationMillis how long the build took, in milliseconds
     * @return {@code true} if it took strictly longer (or shorter, see {@link #getComparison()}) than the limit
     */
    public boolean holdsFor(long durationMillis) {
        return comparison.holds(durationMillis, unit.toMillis(time));
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the build has no duration stored yet (it is still running, as a Pipeline build is after a
     * {@code node} block), the time since it started is used.
     *
     * @return {@code true} if the duration of the build satisfies the condition
     */
    @Override
    public boolean conditionPasses(TaskContext context) {
        Run<?, ?> run = context.run();
        long duration =
                run.getDuration() > 0 ? run.getDuration() : System.currentTimeMillis() - run.getStartTimeInMillis();
        return holdsFor(duration);
    }

    /** Describes this condition type; it is offered in the "Condition" dropdown only after the build. */
    @Extension
    public static final class DescriptorImpl extends TaskDescriptor<Condition> {

        /** Creates the descriptor; Jenkins creates the single instance because of {@link Extension}. */
        public DescriptorImpl() {}

        @Override
        public String getDisplayName() {
            return "Duration of the build";
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code true} only for {@link Phase#POST_BUILD}, when the duration is known
         */
        @Override
        public boolean isApplicable(Phase phase) {
            return phase == Phase.POST_BUILD;
        }
    }
}
