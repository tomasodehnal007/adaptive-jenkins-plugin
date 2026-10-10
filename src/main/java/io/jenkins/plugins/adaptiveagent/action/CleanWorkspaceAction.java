package io.jenkins.plugins.adaptiveagent.action;

import hudson.Extension;
import hudson.FilePath;
import io.jenkins.plugins.adaptiveagent.Phase;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.TaskDescriptor;
import java.io.IOException;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Action without parameters: deletes the workspace of the build on the agent. It is not offered during
 * the build, where it would pull the files out from under the running build steps.
 */
public class CleanWorkspaceAction extends Action {

    /** Creates the action; Jenkins calls this when the agent's configuration form is saved. */
    @DataBoundConstructor
    public CleanWorkspaceAction() {}

    /**
     * {@inheritDoc}
     *
     * <p>Works before the build too, when the workspace is not allocated yet: then the default location
     * of the workspace of the job is cleaned (see {@link TaskContext#workspaceOrDefault()}). A workspace
     * that does not exist is not an error.
     *
     * @throws IOException if a file cannot be deleted
     * @throws InterruptedException if the thread is interrupted while deleting
     */
    @Override
    public void runAction(TaskContext context) throws IOException, InterruptedException {
        FilePath workspace = context.workspaceOrDefault();
        if (workspace != null) {
            workspace.deleteRecursive();
        }
    }

    /** Describes this action type; its display name is the item offered in the "Action" dropdown. */
    @Extension
    public static final class DescriptorImpl extends TaskDescriptor<Action> {

        /** Creates the descriptor; Jenkins creates the single instance because of {@link Extension}. */
        public DescriptorImpl() {}

        @Override
        public String getDisplayName() {
            return "Clean workspace";
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code false} during the build, {@code true} before and after it
         */
        @Override
        public boolean isApplicable(Phase phase) {
            return phase != Phase.DURING_BUILD;
        }
    }
}
