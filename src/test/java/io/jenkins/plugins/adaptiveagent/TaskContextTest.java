package io.jenkins.plugins.adaptiveagent;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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
}
