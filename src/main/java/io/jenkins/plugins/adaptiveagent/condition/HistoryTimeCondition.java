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

/** Condition on the average duration of the last N finished builds on the agent: longer or shorter than a limit. */
public class HistoryTimeCondition extends Condition {

    private final int quantity;
    private final DurationComparison comparison;
    private final long time;
    private final IntervalUnit unit;

    /**
     * Creates the condition; Jenkins calls this when the agent's configuration form is saved.
     *
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

    /**
     * Returns how many of the latest builds are averaged.
     *
     * @return the number of builds
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Returns whether the average must be longer or shorter than the limit.
     *
     * @return the comparison
     */
    public DurationComparison getComparison() {
        return comparison;
    }

    /**
     * Returns the limit of the average; the form shows it in the "Duration" field.
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
            return Math.multiplyExact(unit.toMillis(time), (long) quantity);
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

    /** Describes this condition type; its display name is the item offered in the "Condition" dropdown. */
    @Extension
    public static final class DescriptorImpl extends TaskDescriptor<Condition> {

        /** Creates the descriptor; Jenkins creates the single instance because of {@link Extension}. */
        public DescriptorImpl() {}

        @Override
        public String getDisplayName() {
            return "Average duration of the last builds on this agent";
        }
    }
}
