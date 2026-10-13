package io.jenkins.plugins.adaptiveagent;

import io.jenkins.plugins.adaptiveagent.ActionRunner.EntryResult;
import io.jenkins.plugins.adaptiveagent.entry.BuildEntry;
import io.jenkins.plugins.adaptiveagent.entry.DuringBuildEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.util.Timer;

/**
 * Schedules the {@link Phase#DURING_BUILD} entries: each one is checked periodically on a timer thread
 * until the caller cancels the returned futures (when the build ends).
 *
 * <p>All state ("already performed") lives in the objects created by one call of {@link #start}, so it is
 * separate for every build. Nothing is kept in the configuration or in a shared field.
 */
final class DuringBuildRunner {

    private static final Logger LOGGER = Logger.getLogger(DuringBuildRunner.class.getName());

    private DuringBuildRunner() {}

    /**
     * Starts a timer for every during-build entry configured on the agent. The first check of an entry
     * comes after one interval; the start of the build is covered by the before-build entries.
     *
     * @param first a context used only to find the entries of the agent
     * @param contexts creates a fresh context for every check, so that e.g. the workspace is found once
     *     Jenkins has allocated it
     * @return the scheduled checks; the caller must cancel them when the build has ended
     */
    static List<ScheduledFuture<?>> start(TaskContext first, Supplier<TaskContext> contexts) {
        List<ScheduledFuture<?>> futures = new ArrayList<>();
        for (BuildEntry entry : new ActionRunner(first).entriesOf(Phase.DURING_BUILD)) {
            DuringBuildEntry duringEntry = (DuringBuildEntry) entry;
            if (duringEntry.getUnit() == null) {
                continue; // incomplete configuration
            }
            long period = duringEntry.getUnit().toMillis(duringEntry.getInterval());
            if (period <= 0) {
                continue; // nonsensical interval, nothing sensible to schedule
            }
            Runnable check = checkOf(duringEntry, contexts);
            futures.add(Timer.get().scheduleAtFixedRate(check, period, period, TimeUnit.MILLISECONDS));
        }
        return futures;
    }

    /** Creates one check; the flag "the action has already succeeded" belongs to it alone. */
    private static Runnable checkOf(DuringBuildEntry entry, Supplier<TaskContext> contexts) {
        AtomicBoolean performed = new AtomicBoolean(false);
        return () -> {
            if (!entry.isRepeat() && performed.get()) {
                return;
            }
            try {
                EntryResult result = new ActionRunner(contexts.get()).runEntry(entry);
                if (result == EntryResult.SUCCEEDED) {
                    performed.set(true);
                }
            } catch (RuntimeException e) {
                // An exception escaping a scheduled task would silently stop all later checks.
                LOGGER.log(Level.WARNING, "Check of a during-build task failed", e);
            }
        };
    }
}
