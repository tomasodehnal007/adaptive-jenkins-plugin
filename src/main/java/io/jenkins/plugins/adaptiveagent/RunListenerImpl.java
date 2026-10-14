package io.jenkins.plugins.adaptiveagent;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Environment;
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
 * <p>It is registered with a large negative {@code ordinal}, so that Jenkins calls it after all other run
 * listeners: the tasks run here can be disruptive or slow (cleaning the workspace, taking the agent offline,
 * waiting for a reboot), and other listeners may still need the agent or the workspace.
 *
 * <p>The only state is the timers of the during-build tasks, kept per build so that builds running
 * at the same time cannot touch each other's timers. Pipeline builds are not handled here, so they
 * are skipped.
 */
@Extension(ordinal = -1000)
public class RunListenerImpl extends RunListener<Run<?, ?>> {

    /** Timers of the during-build tasks of the builds that are running now, by build. */
    private final Map<Run<?, ?>, List<ScheduledFuture<?>>> timers = new ConcurrentHashMap<>();

    /**
     * Called just before the build steps run: runs the PRE_BUILD entries of the agent, then starts the
     * timers of its DURING_BUILD entries.
     *
     * @param build the build that is about to run its steps
     * @param launcher starts processes on the agent
     * @param listener writes into the console log of the build
     * @return an environment that changes nothing
     * @throws Run.RunnerAbortedException (unchecked) if a task that is set to abort the build failed; Jenkins
     *     then stops the build and marks it as failed, and no during-build timers are started
     */
    @Override
    public Environment setUpEnvironment(AbstractBuild build, Launcher launcher, BuildListener listener) {
        Node node = build.getBuiltOn();
        Computer computer = node == null ? null : node.toComputer();
        if (computer == null) {
            return new Environment() {};
        }
        TaskContext context = new TaskContext(build, computer, launcher, listener, build.getWorkspace());
        if (new ActionRunner(context).run(Phase.PRE_BUILD)) {
            throw new Run.RunnerAbortedException();
        }
        List<ScheduledFuture<?>> futures = DuringBuildRunner.start(
                context, () -> new TaskContext(build, computer, launcher, listener, build.getWorkspace()));
        if (!futures.isEmpty()) {
            timers.put(build, futures);
        }
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
        Computer computer = node == null ? null : node.toComputer();
        if (computer == null) {
            return;
        }
        TaskContext context =
                new TaskContext(build, computer, node.createLauncher(listener), listener, build.getWorkspace());
        new ActionRunner(context).run(Phase.POST_BUILD);
    }
}
