package io.jenkins.plugins.adaptiveagent.action;

import hudson.AbortException;
import hudson.Extension;
import hudson.remoting.VirtualChannel;
import hudson.util.RemotingDiagnostics;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.TaskDescriptor;
import java.io.IOException;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Action with a parameter: a Groovy script that runs <em>inside the agent's JVM</em> (like the
 * script console of the agent). Everything the script prints, and its result, goes into the build log.
 */
public class GroovyScriptAction extends Action {

    private final String scriptText;

    /**
     * @param scriptText the Groovy script, as typed into the form
     */
    @DataBoundConstructor
    public GroovyScriptAction(String scriptText) {
        this.scriptText = scriptText;
    }

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

    @Extension(ordinal = 90)
    public static final class DescriptorImpl extends TaskDescriptor<Action> {

        @Override
        public String getDisplayName() {
            return "Run Groovy script on the agent";
        }
    }
}
