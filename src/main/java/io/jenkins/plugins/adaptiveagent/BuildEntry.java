package io.jenkins.plugins.adaptiveagent;

import hudson.model.Describable;

/**
 * One row of the agent's task list. For now it only says <em>when</em> the task runs: the
 * {@link Phase} is given by the subclass ({@link PreBuildEntry}, {@link DuringBuildEntry},
 * {@link PostBuildEntry}), which is what the user picks when pressing "Add task".
 */
public abstract class BuildEntry implements Describable<BuildEntry> {

    /** Creates an entry; only the subclasses, one per {@link Phase}, are instantiated. */
    protected BuildEntry() {}

    /**
     * Returns the phase of a build in which this entry's task runs.
     *
     * @return the phase given by the entry type, never {@code null}
     */
    public abstract Phase getPhase();
}
