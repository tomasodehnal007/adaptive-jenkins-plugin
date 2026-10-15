package io.jenkins.plugins.adaptiveagent.condition;

import static io.jenkins.plugins.adaptiveagent.TestSupport.buildWithResult;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.Result;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ResultConditionTest {

    @Test
    void holdsOnlyForTheTickedResult(JenkinsRule jenkins) throws Exception {
        ResultCondition onFailure = new ResultCondition(false, true, false, false, false);
        TaskContext failedBuild = new TaskContext(buildWithResult(jenkins, Result.FAILURE), null, null, null, null);
        TaskContext successfulBuild = new TaskContext(buildWithResult(jenkins, Result.SUCCESS), null, null, null, null);

        assertTrue(onFailure.conditionPasses(failedBuild));
        assertFalse(onFailure.conditionPasses(successfulBuild));
    }
}
