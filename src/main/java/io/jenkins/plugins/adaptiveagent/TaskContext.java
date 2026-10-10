package io.jenkins.plugins.adaptiveagent;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import io.jenkins.plugins.adaptiveagent.action.Action;
import io.jenkins.plugins.adaptiveagent.condition.Condition;

/**
 * Describes the build a task runs in: the build itself, the agent it runs on, a launcher for starting
 * processes on that agent, the build log and the workspace.
 *
 * <p>It is passed to every {@link Condition} and {@link Action}, so they do not depend on the type of the
 * job (Freestyle, Pipeline) that started the build.
 *
 * @param run the build being run
 * @param computer the agent the build runs on
 * @param launcher starts processes on that agent (not on the controller)
 * @param listener writes into the console log of the build
 * @param workspace workspace of the build on the agent, or {@code null} if there is none (yet), e.g. before the
 *     build starts
 */
public record TaskContext(
        Run<?, ?> run,
        Computer computer,
        Launcher launcher,
        TaskListener listener,
        @CheckForNull FilePath workspace) {

    /**
     * Returns the workspace of the build, or, before Jenkins has allocated it, the directory where the
     * agent will put it. The directory may not exist yet.
     *
     * <p>The default is the standard location for the job on the agent. A job with a custom workspace,
     * or a workspace chosen inside a Pipeline, can use another one, which is known only once it is allocated.
     *
     * @return the workspace, or {@code null} if neither the build nor its agent and job are known
     */
    @CheckForNull
    public FilePath workspaceOrDefault() {
        if (workspace != null) {
            return workspace;
        }
        Node node = computer == null ? null : computer.getNode();
        Job<?, ?> job = run == null ? null : run.getParent();
        if (node != null && job instanceof TopLevelItem item) {
            return node.getWorkspaceFor(item);
        }
        return null;
    }
}
