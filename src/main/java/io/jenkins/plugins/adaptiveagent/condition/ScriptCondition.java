package io.jenkins.plugins.adaptiveagent.condition;

import hudson.Extension;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.TaskDescriptor;
import io.jenkins.plugins.adaptiveagent.util.ScriptRunner;
import java.io.IOException;
import org.kohsuke.stapler.DataBoundConstructor;

/** Condition decided by a shell script run on the agent: exit code 0 means the condition holds. */
public class ScriptCondition extends Condition {

    private final String scriptText;

    /**
     * Creates the condition; Jenkins calls this when the agent's configuration form is saved.
     *
     * @param scriptText the text of the script, as typed into the form
     */
    @DataBoundConstructor
    public ScriptCondition(String scriptText) {
        this.scriptText = scriptText;
    }

    /**
     * Returns the script that decides the condition; the form shows it in the "Script" field.
     *
     * @return the text of the script
     */
    public String getScriptText() {
        return scriptText;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The script runs on the agent (see {@link ScriptRunner}) and its output goes into the build log.
     *
     * @return {@code true} if the script exits with code 0
     * @throws IOException if the process cannot be started
     * @throws InterruptedException if the thread is interrupted while waiting for the script
     */
    @Override
    public boolean conditionPasses(TaskContext context) throws IOException, InterruptedException {
        return ScriptRunner.run(context, scriptText) == 0;
    }

    /** Describes this condition type; its display name is the item offered in the "Condition" dropdown. */
    @Extension
    public static final class DescriptorImpl extends TaskDescriptor<Condition> {

        /** Creates the descriptor; Jenkins creates the single instance because of {@link Extension}. */
        public DescriptorImpl() {}

        @Override
        public String getDisplayName() {
            return "Script exits with code 0";
        }
    }
}
