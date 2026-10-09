package io.jenkins.plugins.adaptiveagent.action;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.condition.Condition;

/**
 * Something done on the agent (run a script, clean the workspace, ...).
 *
 * <p>Not to be confused with {@code hudson.model.Action}, which is an unrelated Jenkins type.
 * Like {@link Condition}, this is an extension point: new actions are added by subclassing and
 * declaring a nested {@code @Extension} descriptor.
 */
public abstract class Action implements Describable<Action>, ExtensionPoint {

    /**
     * Performs the action.
     *
     * @param context the build and the agent the action is performed for
     * @throws Exception if the action fails; the runner writes the failure into the build log
     */
    public abstract void runAction(TaskContext context) throws Exception;
}
