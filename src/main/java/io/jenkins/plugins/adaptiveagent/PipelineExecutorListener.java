package io.jenkins.plugins.adaptiveagent;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.ExecutorListener;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.OneOffExecutor;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;

/**
 * Trigger for Pipeline builds.
 *
 * <p>A Pipeline build is not run by one executor on one agent: the script runs on the controller
 * and every {@code node {}} block is a separate task on an executor of whichever agent it got.
 * {@link ExecutorListener} is called by exactly that executor, so for each {@code node {}} block we
 * learn which agent it runs on and can run the tasks configured on that agent before and after the
 * block, and while it runs. A build with two {@code node {}} blocks therefore triggers the tasks
 * twice, once for each agent.
 *
 * <p>Freestyle builds are not handled here (see {@link RunListenerImpl}); they are recognised by
 * the executable being the build itself.
 *
 * <p>Like {@link RunListenerImpl} it is registered with a large negative {@code ordinal}, so that other executor
 * listeners are called before the tasks, which may be disruptive or slow.
 *
 * <p>The plugin therefore depends on the Pipeline API plugin ({@code workflow-api}), which is part of every
 * usual Jenkins installation.
 */
@Extension(ordinal = -1000)
public class PipelineExecutorListener implements ExecutorListener {

    private static final Logger LOGGER = Logger.getLogger(PipelineExecutorListener.class.getName());

    /** Timers of the during-build tasks of the {@code node {}} blocks running now, by executor. */
    private final Map<Executor, List<ScheduledFuture<?>>> timers = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     *
     * <p>A {@code node {}} block starts on the agent: runs the PRE_BUILD entries and starts the timers of
     * the DURING_BUILD entries. If a task that is set to abort the build fails, the whole Pipeline is
     * stopped as FAILURE.
     *
     * <p>The Pipeline is stopped through the executor of the build on the controller, not through the executor
     * of the block: the block runs as an asynchronous execution that is not yet registered at this point, so
     * an interrupt of its executor would be lost and the block would run anyway.
     */
    @Override
    public void taskStarted(Executor executor, Queue.Task task) {
        TaskContext context = contextOf(executor, false);
        if (context == null) {
            return;
        }
        if (new ActionRunner(context).run(Phase.PRE_BUILD)) {
            Executor buildExecutor = context.run().getExecutor();
            if (buildExecutor != null) {
                buildExecutor.interrupt(Result.FAILURE);
            }
            return;
        }
        List<ScheduledFuture<?>> futures = DuringBuildRunner.start(context, () -> contextOf(executor, true));
        if (!futures.isEmpty()) {
            timers.put(executor, futures);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The {@code node {}} block ended: stops its timers and runs the POST_BUILD entries.
     */
    @Override
    public void taskCompleted(Executor executor, Queue.Task task, long durationMS) {
        finished(executor);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The {@code node {}} block ended with an error. Unlike {@code taskCompleted}, the default
     * implementation of this method in Jenkins does nothing, so it has to be implemented here.
     */
    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long durationMS, Throwable problems) {
        finished(executor);
    }

    /** Stops the timers of the block that ended on the executor and runs its POST_BUILD entries. */
    private void finished(Executor executor) {
        List<ScheduledFuture<?>> futures = timers.remove(executor);
        if (futures != null) {
            futures.forEach(future -> future.cancel(true));
        }
        TaskContext context = contextOf(executor, true);
        if (context != null) {
            new ActionRunner(context).run(Phase.POST_BUILD);
        }
    }

    /**
     * Describes the {@code node {}} block the executor is running.
     *
     * @param executor the executor that runs the task
     * @param withWorkspace whether to look for the workspace of the block; it does not exist yet when the
     *     block starts
     * @return the context, or {@code null} if the executor is running anything else (a Freestyle build, the
     *     Pipeline script itself on the controller, a plain task)
     */
    @CheckForNull
    private static TaskContext contextOf(Executor executor, boolean withWorkspace) {
        if (executor instanceof OneOffExecutor) {
            return null; // the Pipeline script itself
        }
        Queue.Executable executable = executor.getCurrentExecutable();
        if (executable == null) {
            return null;
        }
        // For a node block the executable belongs to a build; for a Freestyle build it is the build itself.
        Queue.Executable parent = executable.getParentExecutable();
        if (!(parent instanceof Run<?, ?> run) || parent == executable) {
            return null;
        }
        Computer computer = executor.getOwner();
        Node node = computer.getNode();
        if (node == null) {
            return null;
        }
        TaskListener listener = listenerOf(run);
        FilePath workspace = withWorkspace ? existingWorkspace(node, run) : null;
        long startedAt = System.currentTimeMillis() - executor.getElapsedTime();
        return new TaskContext(run, computer, node.createLauncher(listener), listener, workspace, startedAt);
    }

    /** Returns the console log of the build, so that task output appears in it like for Freestyle builds. */
    private static TaskListener listenerOf(Run<?, ?> run) {
        try {
            if (run instanceof FlowExecutionOwner.Executable executable) {
                FlowExecutionOwner owner = executable.asFlowExecutionOwner();
                if (owner != null) {
                    return owner.getListener();
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Cannot open the log of " + run, e);
        }
        return TaskListener.NULL;
    }

    /** Returns the default workspace of the job on the node if it exists, otherwise {@code null}. */
    @CheckForNull
    private static FilePath existingWorkspace(Node node, Run<?, ?> run) {
        try {
            Job<?, ?> job = run.getParent();
            FilePath workspace = job instanceof TopLevelItem item ? node.getWorkspaceFor(item) : null;
            return workspace != null && workspace.exists() ? workspace : null;
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }
}
