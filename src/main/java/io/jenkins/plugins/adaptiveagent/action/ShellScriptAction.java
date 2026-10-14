package io.jenkins.plugins.adaptiveagent.action;

import hudson.AbortException;
import hudson.Extension;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.TaskDescriptor;
import io.jenkins.plugins.adaptiveagent.util.ScriptRunner;
import java.io.IOException;
import org.kohsuke.stapler.DataBoundConstructor;

/** Action with a parameter: a shell script to run on the agent. */
public class ShellScriptAction extends Action {

    private final String scriptText;

    /**
     * @param scriptText the text of the script, as typed into the form
     */
    @DataBoundConstructor
    public ShellScriptAction(String scriptText) {
        this.scriptText = scriptText;
    }

    public String getScriptText() {
        return scriptText;
    }

    /**
     * Runs the script (see {@link ScriptRunner}).
     *
     * @param context the build and the agent to run the script on
     * @throws IOException if the process cannot be started
     * @throws InterruptedException if the thread is interrupted while waiting for the script
     * @throws AbortException if the script exits with a non-zero code
     */
    @Override
    public void runAction(TaskContext context) throws IOException, InterruptedException {
        int exitCode = ScriptRunner.run(context, scriptText);
        if (exitCode != 0) {
            throw new AbortException("Script exited with code " + exitCode);
        }
    }

    @Extension(ordinal = 100)
    public static final class DescriptorImpl extends TaskDescriptor<Action> {

        @Override
        public String getDisplayName() {
            return "Run custom shell script";
        }
    }
}
