package io.jenkins.plugins.adaptiveagent;

import hudson.model.Describable;

/**
 * One row of the agent's task list: "if this {@link Condition} holds, perform this {@link Action}".
 * The {@link Phase} in which it happens is given by the subclass ({@link PreBuildEntry},
 * {@link DuringBuildEntry}, {@link PostBuildEntry}), which is what the user picks when pressing "Add task".
 */
public abstract class BuildEntry implements Describable<BuildEntry> {

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
}
