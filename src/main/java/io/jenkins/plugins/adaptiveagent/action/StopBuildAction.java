package io.jenkins.plugins.adaptiveagent.action;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.Executor;
import hudson.model.Result;
import io.jenkins.plugins.adaptiveagent.Phase;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.TaskDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Action without parameters: stops the build. After the build there is nothing left to stop, so
 * it is not offered in {@link Phase#POST_BUILD}.
 *
 * <p>Used during the build it is best set to repeat: an interrupt that arrives while the build is only
 * being prepared can get lost, and the next check then stops the build.
 */
public class StopBuildAction extends Action {

    @DataBoundConstructor
    public StopBuildAction() {}

    /**
     * {@inheritDoc}
     *
     * <p>Interrupts the executor running the build; the build ends as ABORTED.
     *
     * @throws AbortException if the build has no executor, so there is nothing to interrupt
     */
    @Override
    public void runAction(TaskContext context) throws AbortException {
        Executor executor = context.run().getExecutor();
        if (executor == null) {
            throw new AbortException("The build has no executor to stop");
        }
        executor.interrupt(Result.ABORTED);
    }

    @Extension(ordinal = 70)
    public static final class DescriptorImpl extends TaskDescriptor<Action> {

        @Override
        public String getDisplayName() {
            return "Stop build";
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code false} after the build, {@code true} before and during it
         */
        @Override
        public boolean isApplicable(Phase phase) {
            return phase != Phase.POST_BUILD;
        }
    }
}
