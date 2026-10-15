package io.jenkins.plugins.adaptiveagent.action;

import static io.jenkins.plugins.adaptiveagent.TestSupport.localContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CleanWorkspaceActionTest {

    @TempDir
    Path workspace;

    @Test
    void deletesEverythingInTheWorkspaceButKeepsTheDirectory() throws Exception {
        Files.writeString(workspace.resolve("file.txt"), "x");
        Files.createDirectories(workspace.resolve("sub/dir"));

        new CleanWorkspaceAction().runAction(localContext(workspace, new ByteArrayOutputStream()));

        assertTrue(Files.isDirectory(workspace));
        try (var content = Files.list(workspace)) {
            assertEquals(0, content.count());
        }
    }
}
