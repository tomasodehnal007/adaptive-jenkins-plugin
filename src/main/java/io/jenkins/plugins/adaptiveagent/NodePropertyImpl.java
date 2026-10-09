package io.jenkins.plugins.adaptiveagent;

import hudson.Extension;
import hudson.model.Node;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import io.jenkins.plugins.adaptiveagent.entry.BuildEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * The agent-centric part of the plugin: the task list is stored on the <em>agent</em> (as a node
 * property shown in the "Node Properties" section of its Configure page), not on any job.
 *
 * <p>Jenkins saves the fields of this object into the agent's configuration automatically, so the
 * list survives a restart without any extra code.
 */
public final class NodePropertyImpl extends NodeProperty<Node> {

    private final List<BuildEntry> entries;

    /**
     * Creates the property from the agent's configuration form.
     *
     * @param entries the configured tasks in the order shown in the form; {@code null} means no tasks
     */
    @DataBoundConstructor
    public NodePropertyImpl(List<BuildEntry> entries) {
        // The form sends null when the user removed every row.
        this.entries = entries != null ? new ArrayList<>(entries) : new ArrayList<>();
    }

    /**
     * Returns the tasks configured on this agent.
     *
     * @return a read-only view of the entries, never {@code null}
     */
    public List<BuildEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /** Describes this property; its display name is the section title on the agent's Configure page. */
    @Extension
    public static final class DescriptorImpl extends NodePropertyDescriptor {

        /** Creates the descriptor; Jenkins creates the single instance because of {@link Extension}. */
        public DescriptorImpl() {}

        @Override
        public String getDisplayName() {
            return "Adaptive agent tasks";
        }
    }
}
