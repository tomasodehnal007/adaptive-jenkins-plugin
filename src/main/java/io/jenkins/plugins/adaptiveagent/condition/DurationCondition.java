package io.jenkins.plugins.adaptiveagent.condition;

import hudson.Extension;
import io.jenkins.plugins.adaptiveagent.Phase;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.TaskDescriptor;
import io.jenkins.plugins.adaptiveagent.util.DurationComparison;
import io.jenkins.plugins.adaptiveagent.util.IntervalUnit;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Condition on how long the build took (in a Pipeline: how long its {@code node} block on the agent took).
 * It only makes sense after the build, so it is offered only then.
 */
public class DurationCondition extends Condition {

    private final DurationComparison comparison;
    private final long time;
    private final IntervalUnit unit;

    /**
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

    public DurationComparison getComparison() {
        return comparison;
    }

    public long getTime() {
        return time;
    }

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
     * <p>The time is measured by {@link TaskContext#elapsedMillis()}: the duration of the build, or in a
     * Pipeline build the time of the {@code node} block on this agent.
     *
     * @return {@code true} if the duration satisfies the condition
     */
    @Override
    public boolean conditionPasses(TaskContext context) {
        return holdsFor(context.elapsedMillis());
    }

    @Extension(ordinal = 80)
    public static final class DescriptorImpl extends TaskDescriptor<Condition> {

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
