package io.jenkins.plugins.adaptiveagent;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Environment;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.jspecify.annotations.NonNull;

/**
 * Trigger for Freestyle builds: Jenkins calls this listener for every build of every job, and from
 * here we run the tasks configured on the agent the build runs on.
 *
 * <p>The only state is the timers of the during-build tasks, kept per build so that builds running
 * at the same time cannot touch each other's timers. Pipeline builds are not handled here, so they
 * are skipped.
 */
@Extension
public class RunListenerImpl extends RunListener<Run<?, ?>> {

    /** Timers of the during-build tasks of the builds that are running now, by build. */
    private final Map<Run<?, ?>, List<ScheduledFuture<?>>> timers = new ConcurrentHashMap<>();

    /**
     * Called when the build starts: starts the timers of the DURING_BUILD entries of the agent.
     *
     * <p>The agent is taken from the executor, because the build does not know yet on which node it runs.
     *
     * @param run the build that has started
     * @param listener writes into the console log of the build
     */
    @Override
    public void onStarted(Run<?, ?> run, TaskListener listener) {
        if (!(run instanceof AbstractBuild<?, ?> build)) {
            return; // not a Freestyle build
        }
        Executor executor = build.getExecutor();
        Computer computer = executor == null ? null : executor.getOwner();
        Node node = computer == null ? null : computer.getNode();
        if (node == null) {
            return;
        }
        TaskContext first = new TaskContext(build, computer, node.createLauncher(listener), listener, null);
        List<ScheduledFuture<?>> futures = DuringBuildRunner.start(
                first,
                () -> new TaskContext(build, computer, node.createLauncher(listener), listener, build.getWorkspace()));
        if (!futures.isEmpty()) {
            timers.put(build, futures);
        }
    }

    /**
     * Called just before the build steps run: runs the PRE_BUILD entries of the agent.
     *
     * @param build the build that is about to run its steps
     * @param launcher starts processes on the agent
     * @param listener writes into the console log of the build
     * @return an environment that changes nothing
     */
    @Override
    public Environment setUpEnvironment(AbstractBuild build, Launcher launcher, BuildListener listener) {
        runEntries(Phase.PRE_BUILD, build, launcher, listener);
        return new Environment() {};
    }

    /**
     * Called when the build is finished and its result is known: stops the timers of the build, then runs
     * the POST_BUILD entries of the agent.
     *
     * @param run the finished build
     * @param listener writes into the console log of the build
     */
    @Override
    public void onCompleted(Run<?, ?> run, @NonNull TaskListener listener) {
        List<ScheduledFuture<?>> futures = timers.remove(run);
        if (futures != null) {
            futures.forEach(future -> future.cancel(true));
        }
        if (!(run instanceof AbstractBuild<?, ?> build)) {
            return; // not a Freestyle build
        }
        Node node = build.getBuiltOn();
        if (node == null) {
            return;
        }
        runEntries(Phase.POST_BUILD, build, node.createLauncher(listener), listener);
    }

    /** Runs the entries of the given phase configured on the agent the build runs on. */
    private void runEntries(Phase phase, AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) {
        Node node = build.getBuiltOn();
        Computer computer = node == null ? null : node.toComputer();
        if (computer == null) {
            return;
        }
        TaskContext context = new TaskContext(build, computer, launcher, listener, build.getWorkspace());
        new ActionRunner(context).run(phase);
    }
}
