package io.jenkins.plugins.adaptiveagent;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import java.util.logging.Logger;

@Extension
public class AgentBuildListener extends RunListener<Run<?, ?>> {

    private static final Logger LOGGER = Logger.getLogger(AgentBuildListener.class.getName());

    @Override
    public void onStarted(Run<?, ?> run, TaskListener listener) {
        String node = nodeNameOf(run);
        String message = "[adaptive-agent] Build '" + run.getFullDisplayName() + "' STARTED on agent: " + node;

        listener.getLogger().println(message);
        LOGGER.info(message);
    }

    @Override
    public void onCompleted(Run<?, ?> run, TaskListener listener) {
        String node = nodeNameOf(run);
        String message = "[adaptive-agent] Build '" + run.getFullDisplayName() + "' COMPLETED on agent: " + node
                + " with result: " + run.getResult();

        listener.getLogger().println(message);
        LOGGER.info(message);
    }

    private static String nodeNameOf(Run<?, ?> run) {
        Executor executor = run.getExecutor();
        if (executor == null) {
            return "(unknown - no executor assigned)";
        }
        Computer computer = executor.getOwner();
        String name = computer.getName();
        return name.isEmpty() ? "(built-in / controller)" : name;
    }
}
