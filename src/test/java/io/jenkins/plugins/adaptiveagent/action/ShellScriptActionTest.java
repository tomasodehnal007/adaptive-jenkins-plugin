package io.jenkins.plugins.adaptiveagent.action;

import static io.jenkins.plugins.adaptiveagent.TestSupport.countLines;
import static io.jenkins.plugins.adaptiveagent.TestSupport.localContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hudson.AbortException;
import hudson.Functions;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Runs the action directly on this machine, without Jenkins. */
class ShellScriptActionTest {

    @TempDir
    Path workspace;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private TaskContext context;

    @BeforeEach
    void setUp() {
        Assumptions.assumeFalse(Functions.isWindows(), "the scripts use sh");
        context = localContext(workspace, out);
    }

    private String log() {
        context.listener().getLogger().flush();
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void outputOfTheScriptGoesToTheLog() throws Exception {
        new ShellScriptAction("echo HELLO").runAction(context);

        assertEquals(1, countLines(log(), "HELLO"), log());
    }

    @Test
    void nonZeroExitCodeIsReportedAsFailure() {
        AbortException e = assertThrows(AbortException.class, () -> new ShellScriptAction("exit 3").runAction(context));

        assertEquals("Script exited with code 3", e.getMessage());
    }

    @Test
    void scriptRunsInAShellSoPipesAndSeparatorsWork() throws Exception {
        new ShellScriptAction("echo FIRST; echo SECOND | cat").runAction(context);

        assertEquals(1, countLines(log(), "FIRST"), log());
        assertEquals(1, countLines(log(), "SECOND"), log());
    }

    @Test
    void multiLineScriptRunsEveryLine() throws Exception {
        new ShellScriptAction("echo ONE\necho TWO\n").runAction(context);

        assertEquals(1, countLines(log(), "ONE"), log());
        assertEquals(1, countLines(log(), "TWO"), log());
    }

    @Test
    void scriptRunsInTheWorkspace() throws Exception {
        new ShellScriptAction("echo \"DIR=$(pwd)\"").runAction(context);

        assertEquals(1, countLines(log(), "DIR=" + workspace.toRealPath()), log());
    }
}
