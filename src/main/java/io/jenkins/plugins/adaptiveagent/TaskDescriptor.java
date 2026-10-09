package io.jenkins.plugins.adaptiveagent;

import hudson.model.Describable;
import hudson.model.Descriptor;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;

/**
 * Base descriptor of {@link Condition}s and {@link Action}s. Besides the name shown in the dropdown it
 * says in which {@link Phase} the condition or action makes sense.
 *
 * <p>There is one class per condition or action. The dropdown on the agent's page lists only those
 * whose descriptor returns {@code true} from {@link #isApplicable(Phase)}.
 *
 * @param <T> {@link Condition} or {@link Action}
 */
public abstract class TaskDescriptor<T extends Describable<T>> extends Descriptor<T> {

    /**
     * Tells whether the condition or action is offered in the given phase. Override to restrict it.
     *
     * @param phase the phase of the entry being edited
     * @return {@code true} if it is offered in {@code phase}; by default in every phase
     */
    public boolean isApplicable(Phase phase) {
        return true;
    }

    /**
     * Lists the registered descriptors of the given type that are offered in the given phase.
     *
     * @param <T> {@link Condition} or {@link Action}
     * @param type {@code Condition.class} or {@code Action.class}
     * @param phase the phase of the entry being edited
     * @return the descriptors to show in the dropdown; descriptors of other plugins that are not
     *     {@link TaskDescriptor}s are offered in every phase
     */
    public static <T extends Describable<T>> List<Descriptor<T>> applicableTo(Class<T> type, Phase phase) {
        List<Descriptor<T>> result = new ArrayList<>();
        for (Descriptor<T> descriptor : Jenkins.get().getDescriptorList(type)) {
            boolean applicable =
                    !(descriptor instanceof TaskDescriptor) || ((TaskDescriptor<?>) descriptor).isApplicable(phase);
            if (applicable) {
                result.add(descriptor);
            }
        }
        return result;
    }
}
