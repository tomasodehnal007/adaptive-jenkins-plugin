package io.jenkins.plugins.adaptiveagent;

import hudson.AbortException;
import hudson.Extension;
import java.io.IOException;
import org.kohsuke.stapler.DataBoundConstructor;

/** Action with a parameter: a shell script to run on the agent. */
public class ShellScriptAction extends Action {

    private final String scriptText;

    /**
     * Creates the action; Jenkins calls this when the agent's configuration form is saved.
     *
     * @param scriptText the text of the script, as typed into the form
     */
    @DataBoundConstructor
    public ShellScriptAction(String scriptText) {
        this.scriptText = scriptText;
    }

    /**
     * Returns the script to run; the form shows it in the "Script" field.
     *
     * @return the text of the script
     */
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

    /** Describes this action type; its display name is the item offered in the "Action" dropdown. */
    @Extension
    public static final class DescriptorImpl extends TaskDescriptor<Action> {

        /** Creates the descriptor; Jenkins creates the single instance because of {@link Extension}. */
        public DescriptorImpl() {}

        @Override
        public String getDisplayName() {
            return "Run custom shell script";
        }
    }
}
