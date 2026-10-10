package io.jenkins.plugins.adaptiveagent;

import static io.jenkins.plugins.adaptiveagent.TestSupport.localContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.plugins.adaptiveagent.action.Action;
import io.jenkins.plugins.adaptiveagent.condition.Condition;
import io.jenkins.plugins.adaptiveagent.entry.PreBuildEntry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Runs single entries with hand-made conditions and actions, without Jenkins. */
class ActionRunnerTest {

    /** Code that may fail with any exception. */
    private interface Body<T> {
        T run() throws Exception;
    }

    private static Condition condition(Body<Boolean> body) {
        return new Condition() {
            @Override
            public boolean conditionPasses(TaskContext context) throws Exception {
                return body.run();
            }
        };
    }

    /** An action that counts how many times it ran, then runs {@code body}. */
    private static class CountingAction extends Action {
        final AtomicInteger runs = new AtomicInteger();
        private final Body<Void> body;

        CountingAction(Body<Void> body) {
            this.body = body;
        }

        @Override
        public void runAction(TaskContext context) throws Exception {
            runs.incrementAndGet();
            body.run();
        }
    }

    @TempDir
    Path workspace;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private TaskContext context;
    private ActionRunner runner;

    @BeforeEach
    void setUp() {
        context = localContext(workspace, out);
        runner = new ActionRunner(context);
    }

    /** A test that interrupts the thread must not leave the flag set for the next test. */
    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    private String log() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private void run(Condition condition, Action action) {
        runner.runEntry(new PreBuildEntry(condition, action));
        context.listener().getLogger().flush();
    }

    @Test
    void actionRunsWhenTheConditionPasses() {
        CountingAction action = new CountingAction(() -> null);

        run(condition(() -> true), action);

        assertEquals(1, action.runs.get());
        assertTrue(log().contains("running"), log());
        assertTrue(log().contains("finished"), log());
    }

    @Test
    void actionIsSkippedWhenTheConditionDoesNotPass() {
        CountingAction action = new CountingAction(() -> null);

        run(condition(() -> false), action);

        assertEquals(0, action.runs.get());
        assertTrue(log().contains("not met"), log());
    }

    @Test
    void actionIsSkippedAndTheReasonLoggedWhenTheConditionFails() {
        CountingAction action = new CountingAction(() -> null);

        run(
                condition(() -> {
                    throw new IOException("cannot read the disk");
                }),
                action);

        assertEquals(0, action.runs.get());
        assertTrue(log().contains("could not be evaluated: cannot read the disk"), log());
    }

    @Test
    void failingActionIsLoggedWithItsMessage() {
        CountingAction action = new CountingAction(() -> {
            throw new IOException("boom");
        });

        run(condition(() -> true), action);

        assertEquals(1, action.runs.get());
        assertTrue(log().contains("failed: boom"), log());
        assertFalse(log().contains("finished"), log());
    }

    @Test
    void failureWithoutAMessageIsLoggedByItsType() {
        CountingAction action = new CountingAction(() -> {
            throw new IllegalStateException();
        });

        run(condition(() -> true), action);

        assertTrue(log().contains("java.lang.IllegalStateException"), log());
    }

    @Test
    void entryWithoutConditionOrActionRunsNothing() {
        CountingAction action = new CountingAction(() -> null);

        run(null, action);
        run(condition(() -> true), null);

        assertEquals(0, action.runs.get());
        assertEquals(2, log().split("incomplete entry", -1).length - 1, log());
    }

    @Test
    void interruptWhileEvaluatingTheConditionKeepsTheFlagAndSkipsTheAction() {
        CountingAction action = new CountingAction(() -> null);

        run(
                condition(() -> {
                    throw new InterruptedException();
                }),
                action);

        assertTrue(Thread.currentThread().isInterrupted(), "the interrupt must not be swallowed");
        assertEquals(0, action.runs.get());
        assertTrue(log().contains("interrupted while evaluating"), log());
    }

    @Test
    void interruptWhileRunningTheActionKeepsTheFlag() {
        CountingAction action = new CountingAction(() -> {
            throw new InterruptedException();
        });

        run(condition(() -> true), action);

        assertTrue(Thread.currentThread().isInterrupted(), "the interrupt must not be swallowed");
        assertTrue(log().contains("interrupted while running"), log());
    }
}
