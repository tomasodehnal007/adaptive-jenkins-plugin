package io.jenkins.plugins.adaptiveagent.condition;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.TaskDescriptor;
import io.jenkins.plugins.adaptiveagent.util.BuildHistory;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Condition that holds when the last N finished builds on the agent (of any job) all failed.
 * Only Freestyle builds are counted, Pipeline builds are not seen (see {@link BuildHistory}).
 */
public class HistoryFailedCondition extends Condition {

    private final int quantity;

    /**
     * @param quantity how many of the latest builds on the agent must have failed
     */
    @DataBoundConstructor
    public HistoryFailedCondition(int quantity) {
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }

    /**
     * Decides from the results of the latest builds whether the condition holds.
     *
     * @param lastResults the results of the most recent finished builds, newest first
     * @return {@code true} if there are at least {@code quantity} results and the latest
     *     {@code quantity} of them are all {@link Result#FAILURE}; unstable or aborted builds do not count as failed
     */
    public boolean holdsFor(List<Result> lastResults) {
        if (quantity <= 0 || lastResults.size() < quantity) {
            return false;
        }
        return lastResults.stream().limit(quantity).allMatch(result -> result == Result.FAILURE);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads the history of the agent the build runs on (see {@link BuildHistory}).
     *
     * @return {@code true} if the latest builds on the agent all failed
     */
    @Override
    public boolean conditionPasses(TaskContext context) {
        List<Result> results = BuildHistory.lastFinished(context, quantity).stream()
                .map(Run::getResult)
                .toList();
        return holdsFor(results);
    }

    @Extension(ordinal = 50)
    public static final class DescriptorImpl extends TaskDescriptor<Condition> {

        @Override
        public String getDisplayName() {
            return "The last builds on this agent all failed";
        }
    }
}
