package io.jenkins.plugins.adaptiveagent;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * Describes the build a task runs in: the build itself, the agent it runs on, a launcher for starting
 * processes on that agent, the build log and the workspace.
 *
 * <p>It is passed to every {@code Condition} and {@code Action}, so they do not depend on the type of the
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
        @CheckForNull FilePath workspace) {}
