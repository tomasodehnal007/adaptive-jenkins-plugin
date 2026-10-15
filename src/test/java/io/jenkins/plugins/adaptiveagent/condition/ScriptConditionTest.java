package io.jenkins.plugins.adaptiveagent.condition;

import static io.jenkins.plugins.adaptiveagent.TestSupport.localContext;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.plugins.adaptiveagent.TaskContext;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@DisabledOnOs(value = OS.WINDOWS, disabledReason = "the scripts use sh")
class ScriptConditionTest {

    @TempDir
    Path workspace;

    private TaskContext context;

    @BeforeEach
    void setUp() {
        context = localContext(workspace, new ByteArrayOutputStream());
    }

    @Test
    void holdsWhenTheScriptExitsWithZero() throws Exception {
        assertTrue(new ScriptCondition("exit 0").conditionPasses(context));
    }

    @Test
    void doesNotHoldWhenTheScriptExitsWithANonZeroCode() throws Exception {
        assertFalse(new ScriptCondition("exit 1").conditionPasses(context));
    }
}
