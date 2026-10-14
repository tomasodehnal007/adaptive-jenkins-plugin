package io.jenkins.plugins.adaptiveagent.condition;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Node;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.TaskDescriptor;
import io.jenkins.plugins.adaptiveagent.util.SizeUnit;
import java.io.IOException;
import org.kohsuke.stapler.DataBoundConstructor;

/** Condition that holds when the agent has <em>less</em> free disk space than a threshold. */
public class DiskSpaceCondition extends Condition {

    private final long space;
    private final SizeUnit unit;

    /**
     * @param space the threshold, in {@code unit}
     * @param unit the unit of {@code space}
     */
    @DataBoundConstructor
    public DiskSpaceCondition(long space, SizeUnit unit) {
        this.space = space;
        this.unit = unit;
    }

    public long getSpace() {
        return space;
    }

    public SizeUnit getUnit() {
        return unit;
    }

    /**
     * Decides whether the amount of free space is below the threshold.
     *
     * @param usableBytes the free space, in bytes
     * @return {@code true} if {@code usableBytes} is less than the threshold; equal is not below
     */
    public boolean holdsFor(long usableBytes) {
        return usableBytes < unit.toBytes(space);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Measures the free space of the volume holding the agent's root directory.
     *
     * @return {@code true} if the free space is below the threshold
     * @throws IOException if the free space cannot be read
     * @throws InterruptedException if the thread is interrupted while waiting for the agent
     */
    @Override
    public boolean conditionPasses(TaskContext context) throws IOException, InterruptedException {
        Node node = context.computer().getNode();
        FilePath root = node != null ? node.getRootPath() : null;
        if (root == null) {
            throw new AbortException("The agent has no root directory");
        }
        root.mkdirs(); // on a brand-new agent the directory does not exist yet
        return holdsFor(root.getUsableDiskSpace());
    }

    @Extension(ordinal = 60)
    public static final class DescriptorImpl extends TaskDescriptor<Condition> {

        @Override
        public String getDisplayName() {
            return "Free disk space is low";
        }
    }
}
