package io.jenkins.plugins.adaptiveagent.condition;

import hudson.Extension;
import hudson.model.Run;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.TaskDescriptor;
import io.jenkins.plugins.adaptiveagent.util.BuildHistory;
import io.jenkins.plugins.adaptiveagent.util.DurationComparison;
import io.jenkins.plugins.adaptiveagent.util.IntervalUnit;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Condition on the average duration of the last N finished builds on the agent: longer or shorter than a limit.
 * Only Freestyle builds are counted, Pipeline builds are not seen (see {@link BuildHistory}).
 */
public class HistoryTimeCondition extends Condition {

    private final int quantity;
    private final DurationComparison comparison;
    private final long time;
    private final IntervalUnit unit;

    /**
     * @param quantity how many of the latest builds on the agent are averaged
     * @param comparison whether the average must be longer or shorter than {@code time}
     * @param time the limit of the average, in {@code unit}
     * @param unit the unit of {@code time}
     */
    @DataBoundConstructor
    public HistoryTimeCondition(int quantity, DurationComparison comparison, long time, IntervalUnit unit) {
        this.quantity = quantity;
        this.comparison = comparison;
        this.time = time;
        this.unit = unit;
    }

    public int getQuantity() {
        return quantity;
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
     * Decides from the durations of the latest builds whether the condition holds.
     *
     * @param lastDurationsMillis the durations of the most recent finished builds, newest first, in milliseconds
     * @return {@code true} if there are at least {@code quantity} durations and the average of the latest
     *     {@code quantity} of them is strictly longer (or shorter, see {@link #getComparison()}) than the limit
     */
    public boolean holdsFor(List<Long> lastDurationsMillis) {
        if (quantity <= 0 || lastDurationsMillis.size() < quantity) {
            return false;
        }
        long total = lastDurationsMillis.stream()
                .limit(quantity)
                .mapToLong(Long::longValue)
                .sum();
        // total / quantity compared with the limit, written without the division so that nothing is rounded
        return comparison.holds(total, limitTimesQuantity());
    }

    private long limitTimesQuantity() {
        try {
            return Math.multiplyExact(unit.toMillis(time), quantity);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads the history of the agent the build runs on (see {@link BuildHistory}).
     *
     * @return {@code true} if the average duration of the latest builds on the agent satisfies the condition
     */
    @Override
    public boolean conditionPasses(TaskContext context) {
        List<Long> durations = BuildHistory.lastFinished(context, quantity).stream()
                .map(Run::getDuration)
                .toList();
        return holdsFor(durations);
    }

    @Extension(ordinal = 40)
    public static final class DescriptorImpl extends TaskDescriptor<Condition> {

        @Override
        public String getDisplayName() {
            return "Average duration of the last builds on this agent";
        }
    }
}
