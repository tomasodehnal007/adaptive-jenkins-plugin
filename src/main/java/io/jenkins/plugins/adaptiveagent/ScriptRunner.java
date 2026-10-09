package io.jenkins.plugins.adaptiveagent;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import java.io.IOException;

/** Runs a shell script on the agent. */
final class ScriptRunner {

    private ScriptRunner() {}

    /**
     * Runs the script in a shell on the agent and writes its output into the build log.
     *
     * <p>The script runs in the build's workspace. Before the build (PRE_BUILD) Jenkins has not
     * allocated the workspace yet, so the script then runs in the agent's root directory instead.
     *
     * <p>The script is handed to a real shell ({@code sh -c}, or {@code cmd /c} on Windows), so
     * pipes, {@code ;} and multi-line scripts work. (On Windows only the first line is executed.)
     *
     * @param context the build and the agent to run the script on
     * @param script the text of the script
     * @return the exit code of the script
     * @throws IOException if the process cannot be started
     * @throws InterruptedException if the thread is interrupted while waiting for the script
     */
    static int run(TaskContext context, String script) throws IOException, InterruptedException {
        Launcher launcher = context.launcher();
        String[] command = launcher.isUnix() ? new String[] {"sh", "-c", script} : new String[] {"cmd", "/c", script};

        Launcher.ProcStarter starter = launcher.launch().cmds(command).stdout(context.listener());
        FilePath workingDirectory = context.workspace();
        if (workingDirectory == null) {
            Node node = context.computer().getNode();
            workingDirectory = node != null ? node.getRootPath() : null;
        }
        if (workingDirectory != null) {
            // On a brand-new agent the root directory is only created with the first workspace.
            workingDirectory.mkdirs();
            starter.pwd(workingDirectory);
        }
        return starter.join();
    }
}
