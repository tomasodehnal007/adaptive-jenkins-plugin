package io.jenkins.plugins.adaptiveagent.condition;

import static io.jenkins.plugins.adaptiveagent.condition.DurationCondition.Comparison.LONGER_THAN;
import static io.jenkins.plugins.adaptiveagent.condition.DurationCondition.Comparison.SHORTER_THAN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Shell;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.util.IntervalUnit;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** Reads the duration of a real finished build; it runs on the built-in node, so no agent is started. */
@WithJenkins
class DurationConditionOnBuildTest {

    /** A finished build that took about a second. */
    private static TaskContext contextOfSlowBuild(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new Shell("sleep 1"));
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        return new TaskContext(build, null, null, null, null);
    }

    @Test
    void buildOfAboutASecondIsLongerThanTenMillisecondsAndShorterThanAnHour(JenkinsRule jenkins) throws Exception {
        TaskContext context = contextOfSlowBuild(jenkins);

        assertTrue(new DurationCondition(LONGER_THAN, 10, IntervalUnit.MILLISECONDS).conditionPasses(context));
        assertTrue(new DurationCondition(SHORTER_THAN, 60, IntervalUnit.MINUTES).conditionPasses(context));
    }

    @Test
    void buildOfAboutASecondIsNotLongerThanAnHourNorShorterThanTenMilliseconds(JenkinsRule jenkins) throws Exception {
        TaskContext context = contextOfSlowBuild(jenkins);

        assertFalse(new DurationCondition(LONGER_THAN, 60, IntervalUnit.MINUTES).conditionPasses(context));
        assertFalse(new DurationCondition(SHORTER_THAN, 10, IntervalUnit.MILLISECONDS).conditionPasses(context));
    }
}
