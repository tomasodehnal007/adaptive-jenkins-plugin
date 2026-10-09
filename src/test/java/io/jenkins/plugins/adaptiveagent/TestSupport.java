package io.jenkins.plugins.adaptiveagent;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.slaves.DumbSlave;
import hudson.tasks.Shell;
import hudson.util.StreamTaskListener;
import io.jenkins.plugins.adaptiveagent.action.Action;
import io.jenkins.plugins.adaptiveagent.action.ShellScriptAction;
import io.jenkins.plugins.adaptiveagent.condition.Condition;
import io.jenkins.plugins.adaptiveagent.entry.BuildEntry;
import io.jenkins.plugins.adaptiveagent.entry.PostBuildEntry;
import io.jenkins.plugins.adaptiveagent.entry.PreBuildEntry;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.jvnet.hudson.test.JenkinsRule;

/** Small helpers shared by the tests. */
public final class TestSupport {

    private TestSupport() {}

    /** An online agent that has the given entries configured. */
    public static DumbSlave agentWith(JenkinsRule jenkins, BuildEntry... entries) throws Exception {
        DumbSlave agent = jenkins.createOnlineSlave();
        agent.setNodeProperties(List.of(new NodePropertyImpl(List.of(entries))));
        return agent;
    }

    /** A Freestyle job pinned to the agent whose only build step is the given shell script. */
    public static FreeStyleProject projectOn(JenkinsRule jenkins, DumbSlave agent, String buildScript)
            throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setAssignedNode(agent);
        project.getBuildersList().add(new Shell(buildScript));
        return project;
    }

    /** An entry that runs before the build. */
    public static PreBuildEntry pre(Condition condition, Action action) {
        return new PreBuildEntry(condition, action);
    }

    /** An entry that runs after the build. */
    public static PostBuildEntry post(Condition condition, Action action) {
        return new PostBuildEntry(condition, action);
    }

    /** An action that prints the text, so the test can see in the log that it ran. */
    public static ShellScriptAction echo(String text) {
        return new ShellScriptAction("echo " + text);
    }

    /** Number of lines of the log that are exactly {@code line} (the output of an echo, not the command line). */
    public static long countLines(String log, String line) {
        return log.lines().filter(line::equals).count();
    }

    /**
     * A context for running scripts on this machine without Jenkins: the output goes to {@code log}
     * and the scripts run in {@code workspace}. There is no build and no agent.
     */
    public static TaskContext localContext(Path workspace, OutputStream log) {
        TaskListener listener = new StreamTaskListener(log, StandardCharsets.UTF_8);
        return new TaskContext(
                null, null, new Launcher.LocalLauncher(listener), listener, new FilePath(workspace.toFile()));
    }
}
