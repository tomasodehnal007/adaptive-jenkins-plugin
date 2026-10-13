package io.jenkins.plugins.adaptiveagent;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.FilePath;
import java.io.File;
import org.junit.jupiter.api.Test;

class TaskContextTest {

    @Test
    void workspaceOrDefaultIsTheWorkspaceWhenThereIsOne() {
        FilePath workspace = new FilePath(new File("/some/workspace"));

        TaskContext context = new TaskContext(null, null, null, null, workspace);

        assertSame(workspace, context.workspaceOrDefault());
    }

    @Test
    void workspaceOrDefaultIsNullWhenNothingIsKnown() {
        assertNull(new TaskContext(null, null, null, null, null).workspaceOrDefault());
    }

    /** The time since the task started, measured from the given start (a Pipeline node block). */
    @Test
    void elapsedTimeCountsFromTheStartOfTheTask() {
        long fiveSecondsAgo = System.currentTimeMillis() - 5_000;
        TaskContext context = new TaskContext(null, null, null, null, null, fiveSecondsAgo);

        long elapsed = context.elapsedMillis();

        assertTrue(elapsed >= 5_000 && elapsed < 60_000, "elapsed: " + elapsed);
    }
}
