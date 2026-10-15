package io.jenkins.plugins.adaptiveagent.action;

import static io.jenkins.plugins.adaptiveagent.TestSupport.countLines;
import static io.jenkins.plugins.adaptiveagent.TestSupport.localContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hudson.AbortException;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@DisabledOnOs(value = OS.WINDOWS, disabledReason = "the scripts use sh")
class ShellScriptActionTest {

    @TempDir
    Path workspace;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private TaskContext context;

    @BeforeEach
    void setUp() {
        context = localContext(workspace, out);
    }

    @Test
    void outputOfTheScriptGoesToTheBuildLog() throws Exception {
        new ShellScriptAction("echo HELLO").runAction(context);

        assertEquals(1, countLines(log(), "HELLO"), log());
    }

    @Test
    void scriptThatExitsWithANonZeroCodeFails() {
        AbortException e = assertThrows(AbortException.class, () -> new ShellScriptAction("exit 3").runAction(context));

        assertEquals("Script exited with code 3", e.getMessage());
    }

    private String log() {
        context.listener().getLogger().flush();
        return out.toString(StandardCharsets.UTF_8);
    }
}
