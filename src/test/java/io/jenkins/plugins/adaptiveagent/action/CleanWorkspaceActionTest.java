package io.jenkins.plugins.adaptiveagent.action;

import static io.jenkins.plugins.adaptiveagent.TestSupport.localContext;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.Launcher;
import hudson.model.TaskListener;
import io.jenkins.plugins.adaptiveagent.Phase;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Deletes a workspace on this machine, without Jenkins. */
class CleanWorkspaceActionTest {

    @TempDir
    Path tempDir;

    @Test
    void deletesTheWorkspaceWithEverythingInIt() throws Exception {
        Path workspace = Files.createDirectories(tempDir.resolve("workspace/sub"));
        Files.writeString(workspace.resolve("file.txt"), "x");
        Path root = tempDir.resolve("workspace");

        new CleanWorkspaceAction().runAction(localContext(root, new ByteArrayOutputStream()));

        assertFalse(Files.exists(root));
    }

    @Test
    void doesNotTouchSiblingsOfTheWorkspace() throws Exception {
        Path workspace = Files.createDirectories(tempDir.resolve("workspace"));
        Path neighbour = Files.createDirectories(tempDir.resolve("other-job"));

        new CleanWorkspaceAction().runAction(localContext(workspace, new ByteArrayOutputStream()));

        assertFalse(Files.exists(workspace));
        assertTrue(Files.exists(neighbour));
    }

    @Test
    void aWorkspaceThatDoesNotExistIsNotAnError() {
        Path missing = tempDir.resolve("never-created");

        assertDoesNotThrow(
                () -> new CleanWorkspaceAction().runAction(localContext(missing, new ByteArrayOutputStream())));
    }

    @Test
    void nothingIsDeletedWhenTheWorkspaceIsUnknown() throws Exception {
        Path somewhere = Files.createDirectories(tempDir.resolve("somewhere"));
        TaskListener listener = TaskListener.NULL;
        TaskContext withoutWorkspace =
                new TaskContext(null, null, new Launcher.LocalLauncher(listener), listener, null);

        new CleanWorkspaceAction().runAction(withoutWorkspace);

        assertTrue(Files.exists(somewhere));
    }

    @Test
    void isOfferedBeforeAndAfterTheBuildButNotDuringIt() {
        CleanWorkspaceAction.DescriptorImpl descriptor = new CleanWorkspaceAction.DescriptorImpl();

        assertTrue(descriptor.isApplicable(Phase.PRE_BUILD));
        assertTrue(descriptor.isApplicable(Phase.POST_BUILD));
        assertFalse(descriptor.isApplicable(Phase.DURING_BUILD));
        assertEquals("Clean workspace", descriptor.getDisplayName());
    }
}
