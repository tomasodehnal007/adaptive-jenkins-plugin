package io.jenkins.plugins.adaptiveagent.condition;

import static io.jenkins.plugins.adaptiveagent.TestSupport.countLines;
import static io.jenkins.plugins.adaptiveagent.TestSupport.localContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.Functions;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Evaluates the condition directly on this machine, without Jenkins. */
class ScriptConditionTest {

    @TempDir
    Path workspace;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private TaskContext context;

    @BeforeEach
    void setUp() {
        Assumptions.assumeFalse(Functions.isWindows(), "the scripts use sh");
        context = localContext(workspace, out);
    }

    @Test
    void passesWhenTheScriptExitsWithZero() throws Exception {
        assertTrue(new ScriptCondition("exit 0").conditionPasses(context));
    }

    @Test
    void doesNotPassWhenTheScriptExitsWithNonZero() throws Exception {
        assertFalse(new ScriptCondition("exit 1").conditionPasses(context));
    }

    @Test
    void outputOfTheScriptGoesToTheLog() throws Exception {
        new ScriptCondition("echo CONDITION_OUTPUT").conditionPasses(context);

        context.listener().getLogger().flush();
        assertEquals(1, countLines(out.toString(StandardCharsets.UTF_8), "CONDITION_OUTPUT"));
    }
}
