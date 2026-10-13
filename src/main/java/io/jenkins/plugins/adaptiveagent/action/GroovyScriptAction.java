package io.jenkins.plugins.adaptiveagent.action;

import hudson.AbortException;
import hudson.Extension;
import hudson.remoting.VirtualChannel;
import hudson.util.RemotingDiagnostics;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.TaskDescriptor;
import java.io.IOException;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Action with a parameter: a Groovy script that runs <em>inside the agent's JVM</em> (like the
 * script console of the agent). Everything the script prints, and its result, goes into the build log.
 *
 * <p>Only an administrator ({@link Jenkins#ADMINISTER}) can create or save it, because it is
 * as powerful as the script console.
 */
public class GroovyScriptAction extends Action {

    private final String scriptText;

    /**
     * Creates the action; Jenkins calls this when the agent's configuration form is saved, as the user
     * who saves it.
     *
     * @param scriptText the Groovy script, as typed into the form
     * @throws org.springframework.security.access.AccessDeniedException if the current user is not an administrator
     */
    @DataBoundConstructor
    public GroovyScriptAction(String scriptText) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        this.scriptText = scriptText;
    }

    /**
     * Returns the script to run; the form shows it in the "Script" field.
     *
     * @return the Groovy script
     */
    public String getScriptText() {
        return scriptText;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sends the script to the agent, runs it there and writes its output into the build log.
     *
     * @throws AbortException if there is no connection to the agent
     * @throws IOException if the script cannot be sent to the agent
     * @throws InterruptedException if the thread is interrupted while waiting for the script
     */
    @Override
    public void runAction(TaskContext context) throws IOException, InterruptedException {
        VirtualChannel channel = context.launcher().getChannel();
        if (channel == null) {
            throw new AbortException("There is no channel to the agent");
        }
        String output = RemotingDiagnostics.executeGroovy(scriptText, channel);
        context.listener().getLogger().print(output);
    }

    /** Describes this action type; its display name is the item offered in the "Action" dropdown. */
    @Extension
    public static final class DescriptorImpl extends TaskDescriptor<Action> {

        /** Creates the descriptor; Jenkins creates the single instance because of {@link Extension}. */
        public DescriptorImpl() {}

        @Override
        public String getDisplayName() {
            return "Run Groovy script on the agent";
        }
    }
}
