package io.jenkins.plugins.adaptiveagent.action;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.slaves.OfflineCause;
import io.jenkins.plugins.adaptiveagent.Phase;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.TaskDescriptor;
import java.io.IOException;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Like {@link RebootAction}, but after taking the agent offline (no new builds) it first waits until the
 * other builds running on the agent have finished, and only then reboots. Only offered after the build.
 *
 * <p>The wait is limited, so a stuck build cannot block this one forever. It has the same
 * "Reboot command" field as {@link RebootAction}, whose form it shares.
 */
public class GracefulRebootAction extends RebootAction {

    private static final long POLL_INTERVAL_MILLIS = 1_000;
    private static final long MAX_WAIT_MILLIS = 30 * 60 * 1000;

    /**
     * Creates the action; Jenkins calls this when the agent's configuration form is saved.
     *
     * @param command the command that reboots the machine, or {@code null} or blank for the default
     *     of the agent's operating system
     */
    @DataBoundConstructor
    public GracefulRebootAction(@CheckForNull String command) {
        super(command);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Waits until no other executor of the agent is busy. The executor running this build is not
     * counted, otherwise the action would wait for itself.
     *
     * @throws AbortException if other builds are still running after the time limit (30 minutes)
     */
    @Override
    protected void waitBeforeReboot(TaskContext context) throws IOException, InterruptedException {
        context.listener().getLogger().println("Waiting for the other builds on the agent to finish");
        long deadline = System.currentTimeMillis() + MAX_WAIT_MILLIS;
        while (otherBuildsRunning(context.computer())) {
            if (System.currentTimeMillis() > deadline) {
                context.computer()
                        .setTemporaryOfflineCause(new OfflineCause.ByCLI(
                                "Reboot not done: other builds were still running after 30 minutes"));
                throw new AbortException("Other builds are still running on the agent, giving up the reboot");
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
    }

    /** True if some executor of the agent other than the one running this code is busy. */
    private static boolean otherBuildsRunning(Computer computer) {
        Executor own = Executor.currentExecutor();
        for (Executor executor : computer.getExecutors()) {
            if (executor != own && executor.isBusy()) {
                return true;
            }
        }
        return false;
    }

    /** Describes this action type; its display name is the item offered in the "Action" dropdown. */
    @Extension
    public static final class DescriptorImpl extends TaskDescriptor<Action> {

        /** Creates the descriptor; Jenkins creates the single instance because of {@link Extension}. */
        public DescriptorImpl() {}

        @Override
        public String getDisplayName() {
            return "Reboot the agent's machine when other builds have finished";
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code true} only after the build
         */
        @Override
        public boolean isApplicable(Phase phase) {
            return phase == Phase.POST_BUILD;
        }
    }
}
