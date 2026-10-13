package io.jenkins.plugins.adaptiveagent;

import hudson.model.Describable;
import hudson.model.Node;
import io.jenkins.plugins.adaptiveagent.action.Action;
import io.jenkins.plugins.adaptiveagent.condition.Condition;
import io.jenkins.plugins.adaptiveagent.entry.BuildEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs the entries configured on an agent: for each entry it evaluates the condition and, if it
 * passes, performs the action.
 *
 * <p>The runner keeps no state between calls (one instance is created per build), so builds running
 * at the same time on the same agent cannot interfere with each other. A failing condition or
 * action is reported in the build log and never stops the remaining entries.
 */
public final class ActionRunner {

    private static final Logger LOGGER = Logger.getLogger(ActionRunner.class.getName());

    /** What happened when an entry was run. */
    enum EntryResult {
        /** The condition did not hold, could not be evaluated, or the entry was incomplete or interrupted. */
        SKIPPED,
        /** The condition held and the action finished without an error. */
        SUCCEEDED,
        /** The condition held, but the action failed or was interrupted. */
        FAILED
    }

    private final TaskContext context;

    /**
     * Creates a runner for one build on one agent.
     *
     * @param context the build and the agent whose entries are run
     */
    public ActionRunner(TaskContext context) {
        this.context = context;
    }

    /**
     * Runs all entries of the given phase in the order they are configured. If the thread is
     * interrupted (e.g. the build is aborted), the remaining entries are not run.
     *
     * @param phase the phase of the build whose entries are run
     * @return {@code true} if an entry that is set to abort the build failed; the remaining entries are
     *     then not run and the caller should abort the build
     */
    public boolean run(Phase phase) {
        for (BuildEntry entry : entriesOf(phase)) {
            if (Thread.currentThread().isInterrupted()) {
                log(phase + ": interrupted, the remaining tasks are not run");
                return false;
            }
            if (runEntry(entry) == EntryResult.FAILED && entry.isAbortBuildOnFailure()) {
                log(phase + ": aborting the build because the task is set to abort it on failure");
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the entries configured on the agent for the given phase.
     *
     * @param phase the phase of the build
     * @return the entries of that phase; empty if the agent has no configuration
     */
    List<BuildEntry> entriesOf(Phase phase) {
        List<BuildEntry> result = new ArrayList<>();
        Node node = context.computer().getNode();
        if (node == null) {
            return result;
        }
        NodePropertyImpl property = node.getNodeProperties().get(NodePropertyImpl.class);
        if (property == null) {
            return result;
        }
        for (BuildEntry entry : property.getEntries()) {
            if (entry.getPhase() == phase) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * Evaluates the condition of the entry and, if it passes, performs the action. Failures are
     * written into the build log.
     *
     * @param entry the entry to run
     * @return what happened; the action ran only for {@link EntryResult#SUCCEEDED} and
     *     {@link EntryResult#FAILED}
     */
    EntryResult runEntry(BuildEntry entry) {
        Condition condition = entry.getCondition();
        Action action = entry.getAction();
        if (condition == null || action == null) {
            log("Skipping an incomplete entry (" + entry.getPhase() + "): condition or action is missing");
            return EntryResult.SKIPPED;
        }
        String conditionName = nameOf(condition);
        String actionName = nameOf(action);

        try {
            if (!condition.conditionPasses(context)) {
                log(entry.getPhase() + ": condition '" + conditionName + "' not met, skipping '" + actionName + "'");
                return EntryResult.SKIPPED;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log(entry.getPhase() + ": interrupted while evaluating condition '" + conditionName + "'");
            return EntryResult.SKIPPED;
        } catch (Exception e) {
            fail(entry.getPhase() + ": condition '" + conditionName + "' could not be evaluated", e);
            return EntryResult.SKIPPED;
        }

        log(entry.getPhase() + ": running '" + actionName + "'");
        try {
            action.runAction(context);
            log(entry.getPhase() + ": '" + actionName + "' finished");
            return EntryResult.SUCCEEDED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log(entry.getPhase() + ": interrupted while running '" + actionName + "'");
            return EntryResult.FAILED;
        } catch (Exception e) {
            fail(entry.getPhase() + ": '" + actionName + "' failed", e);
            return EntryResult.FAILED;
        }
    }

    /** Returns the name shown in the form, or the class name if the descriptor cannot be found. */
    private static <T extends Describable<T>> String nameOf(T describable) {
        try {
            return describable.getDescriptor().getDisplayName();
        } catch (RuntimeException | AssertionError e) {
            return describable.getClass().getSimpleName();
        }
    }

    /** Writes into the console log of the build. */
    private void log(String message) {
        context.listener().getLogger().println("[adaptive-agent] " + message);
    }

    /** Writes the failure into the console log of the build and into the Jenkins log. */
    private void fail(String message, Exception e) {
        log(message + ": " + (e.getMessage() != null ? e.getMessage() : e.toString()));
        LOGGER.log(Level.WARNING, message, e);
    }
}
