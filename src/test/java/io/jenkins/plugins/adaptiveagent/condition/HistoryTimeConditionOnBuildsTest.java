package io.jenkins.plugins.adaptiveagent.condition;

import static hudson.model.Result.SUCCESS;
import static io.jenkins.plugins.adaptiveagent.TestSupport.buildWithResult;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import io.jenkins.plugins.adaptiveagent.util.DurationComparison;
import io.jenkins.plugins.adaptiveagent.util.IntervalUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** Evaluates the condition inside a build that follows real builds on the built-in node. */
@WithJenkins
class HistoryTimeConditionOnBuildsTest {

    private static boolean evaluatedInANewBuild(JenkinsRule jenkins, HistoryTimeCondition condition) throws Exception {
        AtomicBoolean result = new AtomicBoolean();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                result.set(condition.conditionPasses(
                        new TaskContext(build, jenkins.jenkins.toComputer(), null, null, null)));
                return true;
            }
        });
        jenkins.buildAndAssertSuccess(project);
        return result.get();
    }

    @Test
    void quickBuildsAreBelowAnHourButNotBelowZero(JenkinsRule jenkins) throws Exception {
        buildWithResult(jenkins, SUCCESS);
        buildWithResult(jenkins, SUCCESS);

        assertTrue(evaluatedInANewBuild(
                jenkins, new HistoryTimeCondition(2, DurationComparison.SHORTER_THAN, 60, IntervalUnit.MINUTES)));
        assertFalse(evaluatedInANewBuild(
                jenkins, new HistoryTimeCondition(2, DurationComparison.SHORTER_THAN, 0, IntervalUnit.MILLISECONDS)));
    }

    @Test
    void doesNotHoldWithoutEnoughHistory(JenkinsRule jenkins) throws Exception {
        buildWithResult(jenkins, SUCCESS);

        assertFalse(evaluatedInANewBuild(
                jenkins, new HistoryTimeCondition(3, DurationComparison.SHORTER_THAN, 60, IntervalUnit.MINUTES)));
    }
}
