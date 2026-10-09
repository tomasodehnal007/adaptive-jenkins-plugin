package io.jenkins.plugins.adaptiveagent.util;

import hudson.model.Run;
import hudson.util.RunList;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import java.util.ArrayList;
import java.util.List;

/** Looks at the builds that ran on the agent (of any job), for the conditions that depend on history. */
public final class BuildHistory {

    private BuildHistory() {}

    /**
     * Returns the most recent finished builds on the agent, newest first. The build the task runs in
     * and builds that are still running are not included.
     *
     * <p>It is the history of the <em>agent</em>, so builds of all jobs that ran on it are counted.
     *
     * @param context the build and the agent whose history is read
     * @param limit the maximum number of builds to return
     * @return at most {@code limit} finished builds, fewer if the agent has not run that many
     */
    public static List<Run<?, ?>> lastFinished(TaskContext context, int limit) {
        List<Run<?, ?>> result = new ArrayList<>();
        if (limit <= 0) {
            return result;
        }
        // getBuilds() is declared with a raw type, hence the typed variable.
        @SuppressWarnings("unchecked")
        RunList<? extends Run<?, ?>> builds = context.computer().getBuilds();
        for (Run<?, ?> run : builds) {
            if (result.size() == limit) {
                break;
            }
            if (run.getExternalizableId().equals(context.run().getExternalizableId()) || run.isBuilding()) {
                continue;
            }
            result.add(run);
        }
        return result;
    }
}
