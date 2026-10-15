package io.jenkins.plugins.adaptiveagent.util;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.util.LineEndingConversion;
import hudson.util.LineEndingConversion.EOLType;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import java.io.IOException;

/** Runs a shell script on the agent. */
public final class ScriptRunner {

    private ScriptRunner() {}

    /**
     * Runs the script in a shell on the agent and writes its output into the build log.
     *
     * <p>The script runs in the build's workspace. Before the build (PRE_BUILD) Jenkins has not
     * allocated the workspace yet, so the script then runs in the agent's root directory instead.
     *
     * <p>Like the "Execute shell" and "Execute Windows batch command" build steps, the script is written
     * into a temporary file in the agent's temporary directory and run by {@code sh -xe} (or {@code cmd} on
     * Windows) in the working directory above, so pipes, {@code ;} and multi-line scripts work. On Unix, as in
     * "Execute shell", every command is printed before it runs and the script stops at the first failing
     * command. The file is deleted afterwards.
     *
     * @param context the build and the agent to run the script on
     * @param script the text of the script
     * @return the exit code of the script
     * @throws AbortException if the agent has no directory to run the script in
     * @throws IOException if the script file cannot be written or the process cannot be started
     * @throws InterruptedException if the thread is interrupted while waiting for the script
     */
    public static int run(TaskContext context, String script) throws IOException, InterruptedException {
        FilePath workingDirectory = context.workspace();
        if (workingDirectory == null) {
            Node node = context.computer().getNode();
            workingDirectory = node != null ? node.getRootPath() : null;
        }
        if (workingDirectory == null) {
            throw new AbortException("The agent has no directory to run the script in");
        }
        // On a brand-new agent the root directory is only created with the first workspace.
        workingDirectory.mkdirs();

        Launcher launcher = context.launcher();
        boolean unix = launcher.isUnix();
        FilePath file = unix
                ? workingDirectory.createTextTempFile(
                        "adaptive-agent", ".sh", LineEndingConversion.convertEOL(script, EOLType.Unix), false)
                : workingDirectory.createTextTempFile(
                        "adaptive-agent",
                        ".bat",
                        LineEndingConversion.convertEOL(script + "\r\nexit %ERRORLEVEL%", EOLType.Windows),
                        false);
        try {
            String[] command = unix
                    ? new String[] {"sh", "-xe", file.getRemote()}
                    : new String[] {"cmd", "/c", "call", file.getRemote()};
            return launcher.launch()
                    .cmds(command)
                    .stdout(context.listener())
                    .pwd(workingDirectory)
                    .join();
        } finally {
            file.delete();
        }
    }
}
