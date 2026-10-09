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
import org.jspecify.annotations.NonNull;

/**
 * Trigger for Freestyle builds: Jenkins calls this listener for every build of every job, and from
 * here we run the tasks configured on the agent the build runs on.
 *
 * <p>Pipeline builds are not handled here, so they are skipped.
 */
@Extension
public class RunListenerImpl extends RunListener<Run<?, ?>> {

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
     * Called when the build is finished and its result is known: runs the POST_BUILD entries of the agent.
     *
     * @param run the finished build
     * @param listener writes into the console log of the build
     */
    @Override
    public void onCompleted(Run<?, ?> run, @NonNull TaskListener listener) {
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
