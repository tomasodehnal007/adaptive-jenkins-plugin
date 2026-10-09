package io.jenkins.plugins.adaptiveagent;

import hudson.ExtensionPoint;
import hudson.model.Describable;

/**
 * A condition deciding whether an {@link Action} should run.
 *
 * <p>This is an extension point of this plugin: another plugin can add its own condition by
 * subclassing this class and declaring a nested {@code @Extension} descriptor. Jenkins then offers
 * it in the "Condition" dropdown automatically.
 */
public abstract class Condition implements Describable<Condition>, ExtensionPoint {

    /**
     * Decides whether the action of the entry should run.
     *
     * @param context the build and the agent the condition is evaluated for
     * @return {@code true} if the action should run
     * @throws Exception if the condition cannot be evaluated; the runner then treats it as not met
     */
    public abstract boolean conditionPasses(TaskContext context) throws Exception;
}
