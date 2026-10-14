package io.jenkins.plugins.adaptiveagent.entry;

import hudson.model.Describable;
import hudson.model.Descriptor;
import io.jenkins.plugins.adaptiveagent.Phase;
import io.jenkins.plugins.adaptiveagent.TaskDescriptor;
import io.jenkins.plugins.adaptiveagent.action.Action;
import io.jenkins.plugins.adaptiveagent.condition.Condition;
import java.util.List;

/**
 * One row of the agent's task list: "if this {@link Condition} holds, perform this {@link Action}".
 * The {@link Phase} in which it happens is given by the subclass ({@link PreBuildEntry},
 * {@link DuringBuildEntry}, {@link PostBuildEntry}), which is what the user picks when pressing "Add task".
 */
public abstract sealed class BuildEntry implements Describable<BuildEntry>
        permits PreBuildEntry, DuringBuildEntry, PostBuildEntry {

    private final Condition condition;
    private final Action action;

    /**
     * Creates an entry; only the subclasses, one per {@link Phase}, are instantiated.
     *
     * @param condition decides whether the action runs
     * @param action what is done on the agent
     */
    protected BuildEntry(Condition condition, Action action) {
        this.condition = condition;
        this.action = action;
    }

    /**
     * Returns the condition that decides whether the action runs.
     *
     * @return the condition
     */
    public Condition getCondition() {
        return condition;
    }

    /**
     * Returns what is done on the agent when the condition holds.
     *
     * @return the action
     */
    public Action getAction() {
        return action;
    }

    /**
     * Returns the phase of a build in which this entry's task runs.
     *
     * @return the phase given by the entry type, never {@code null}
     */
    public abstract Phase getPhase();

    /**
     * Tells whether the build must be aborted if the action of this entry fails. Only entries that run
     * before the build can do it; for the others there is nothing left to abort or the result is not to be
     * changed by the task.
     *
     * @return {@code false}; {@link PreBuildEntry} overrides it
     */
    public boolean isAbortBuildOnFailure() {
        return false;
    }

    /**
     * Descriptor of an entry type. Besides the name shown in the "Add task" menu it tells the form
     * which conditions and actions to offer, i.e. only those applicable in this entry's phase.
     * The form ({@code BuildEntry/config.jelly}) reads them through {@link #getConditionDescriptors()}
     * and {@link #getActionDescriptors()}.
     */
    public abstract static class BuildEntryDescriptor extends Descriptor<BuildEntry> {

        /**
         * Returns the phase of the entries of this type.
         *
         * @return the phase
         */
        public abstract Phase getPhase();

        /**
         * Returns the conditions the form offers for entries of this type.
         *
         * @return descriptors of the conditions applicable in {@link #getPhase()}
         */
        public List<Descriptor<Condition>> getConditionDescriptors() {
            return TaskDescriptor.applicableTo(Condition.class, getPhase());
        }

        /**
         * Returns the actions the form offers for entries of this type.
         *
         * @return descriptors of the actions applicable in {@link #getPhase()}
         */
        public List<Descriptor<Action>> getActionDescriptors() {
            return TaskDescriptor.applicableTo(Action.class, getPhase());
        }
    }
}
