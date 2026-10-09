package io.jenkins.plugins.adaptiveagent;

import hudson.Extension;
import hudson.model.Result;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Condition on the result of the finished build. It only makes sense after the build, so it is
 * offered only in {@link Phase#POST_BUILD}.
 */
public class ResultCondition extends Condition {

    private final boolean success;
    private final boolean failure;
    private final boolean aborted;
    private final boolean unstable;
    private final boolean notBuilt;

    /**
     * Creates the condition; Jenkins calls this when the agent's configuration form is saved.
     * The parameter names must match the field names in {@code config.jelly}.
     *
     * @param success passes when the build succeeded
     * @param failure passes when the build failed
     * @param aborted passes when the build was aborted
     * @param unstable passes when the build is unstable
     * @param notBuilt passes when the build was not built
     */
    @DataBoundConstructor
    public ResultCondition(boolean success, boolean failure, boolean aborted, boolean unstable, boolean notBuilt) {
        this.success = success;
        this.failure = failure;
        this.aborted = aborted;
        this.unstable = unstable;
        this.notBuilt = notBuilt;
    }

    /**
     * Returns whether the condition passes for a successful build.
     *
     * @return {@code true} if ticked in the form
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns whether the condition passes for a failed build.
     *
     * @return {@code true} if ticked in the form
     */
    public boolean isFailure() {
        return failure;
    }

    /**
     * Returns whether the condition passes for an aborted build.
     *
     * @return {@code true} if ticked in the form
     */
    public boolean isAborted() {
        return aborted;
    }

    /**
     * Returns whether the condition passes for an unstable build.
     *
     * @return {@code true} if ticked in the form
     */
    public boolean isUnstable() {
        return unstable;
    }

    /**
     * Returns whether the condition passes for a build that was not built.
     *
     * @return {@code true} if ticked in the form
     */
    public boolean isNotBuilt() {
        return notBuilt;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A build whose result is not set yet has had no failure so far and counts as successful.
     *
     * @return {@code true} if the build result is one of the results ticked in the form
     */
    @Override
    public boolean conditionPasses(TaskContext context) {
        Result result = context.run().getResult();
        if (result == null) {
            result = Result.SUCCESS;
        }
        return (success && result == Result.SUCCESS)
                || (failure && result == Result.FAILURE)
                || (aborted && result == Result.ABORTED)
                || (unstable && result == Result.UNSTABLE)
                || (notBuilt && result == Result.NOT_BUILT);
    }

    /** Describes this condition type; it is offered in the "Condition" dropdown only after the build. */
    @Extension
    public static final class DescriptorImpl extends TaskDescriptor<Condition> {

        /** Creates the descriptor; Jenkins creates the single instance because of {@link Extension}. */
        public DescriptorImpl() {}

        @Override
        public String getDisplayName() {
            return "Result of the build";
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code true} only for {@link Phase#POST_BUILD}, when the result is known
         */
        @Override
        public boolean isApplicable(Phase phase) {
            return phase == Phase.POST_BUILD;
        }
    }
}
