package io.jenkins.plugins.adaptiveagent.action;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.slaves.OfflineCause;
import io.jenkins.plugins.adaptiveagent.Phase;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.TaskDescriptor;
import io.jenkins.plugins.adaptiveagent.util.ScriptRunner;
import java.io.IOException;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Action with an optional parameter: takes the agent offline and reboots the whole machine the agent runs
 * on at once, not just the agent process. Everything else running on that machine is restarted too.
 *
 * <p>Only offered after the build, so the build's own result is already final. The operating
 * system user running the agent must be allowed to run the reboot command.
 *
 * <p>The steps are always the same (take the agent offline, {@link #waitBeforeReboot}, run the command);
 * {@link GracefulRebootAction} only changes the waiting.
 */
public class RebootAction extends Action {

    private static final String UNIX_COMMAND = "shutdown -r now";
    private static final String WINDOWS_COMMAND = "shutdown -r -f -t 0";

    @CheckForNull
    private final String command;

    /**
     * Creates the action; Jenkins calls this when the agent's configuration form is saved.
     *
     * @param command the command that reboots the machine, or {@code null} or blank for the default
     *     of the agent's operating system
     */
    @DataBoundConstructor
    public RebootAction(@CheckForNull String command) {
        this.command = command == null || command.isBlank() ? null : command;
    }

    /**
     * Returns the command set in the form.
     *
     * @return the command, or {@code null} if the default is used
     */
    @CheckForNull
    public String getCommand() {
        return command;
    }

    /**
     * Returns the command that will actually run.
     *
     * @param unix {@code true} if the agent runs on a Unix-like system
     * @return the configured command, otherwise the default of the operating system
     */
    String effectiveCommand(boolean unix) {
        if (command != null) {
            return command;
        }
        return unix ? UNIX_COMMAND : WINDOWS_COMMAND;
    }

    /**
     * {@inheritDoc}
     *
     * @throws AbortException if the reboot command exits with a non-zero code (the agent then stays offline
     *     with a reason saying so), or if {@link #waitBeforeReboot} gives up
     * @throws IOException if the command cannot be started
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    @Override
    public void runAction(TaskContext context) throws IOException, InterruptedException {
        SetOfflineAction.takeOffline(context);
        waitBeforeReboot(context);
        int exitCode =
                ScriptRunner.run(context, effectiveCommand(context.launcher().isUnix()));
        if (exitCode != 0) {
            context.computer()
                    .setTemporaryOfflineCause(
                            new OfflineCause.ByCLI("Reboot not done: the reboot command exited with code " + exitCode));
            throw new AbortException("The reboot command exited with code " + exitCode);
        }
    }

    /**
     * Step between taking the agent offline and running the reboot command. Does nothing here, so the
     * machine restarts at once; subclasses override it to wait for something.
     *
     * @param context the build and the agent to reboot
     * @throws AbortException if the reboot should not go ahead
     * @throws IOException if waiting fails
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    protected void waitBeforeReboot(TaskContext context) throws IOException, InterruptedException {}

    /** Describes this action type; its display name is the item offered in the "Action" dropdown. */
    @Extension(ordinal = 50)
    public static final class DescriptorImpl extends TaskDescriptor<Action> {

        /** Creates the descriptor; Jenkins creates the single instance because of {@link Extension}. */
        public DescriptorImpl() {}

        @Override
        public String getDisplayName() {
            return "Reboot the agent's machine";
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
