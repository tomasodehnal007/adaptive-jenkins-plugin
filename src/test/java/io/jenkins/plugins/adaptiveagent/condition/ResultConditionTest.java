package io.jenkins.plugins.adaptiveagent.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** Evaluates the condition on builds that ended with each possible result; no agent is needed. */
@WithJenkins
class ResultConditionTest {

    private static final List<Result> RESULTS =
            List.of(Result.SUCCESS, Result.FAILURE, Result.ABORTED, Result.UNSTABLE, Result.NOT_BUILT);

    /** A condition with only the box of the given result ticked. */
    private static ResultCondition onlyFor(Result result) {
        return new ResultCondition(
                result == Result.SUCCESS,
                result == Result.FAILURE,
                result == Result.ABORTED,
                result == Result.UNSTABLE,
                result == Result.NOT_BUILT);
    }

    private static TaskContext contextOf(FreeStyleBuild build) {
        return new TaskContext(build, null, null, null, null);
    }

    /** A finished build whose result is the given one. */
    private static FreeStyleBuild buildWithResult(JenkinsRule jenkins, Result result) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                build.setResult(result);
                return true;
            }
        });
        return jenkins.assertBuildStatus(result, project.scheduleBuild2(0));
    }

    @Test
    void passesOnlyForTheTickedResult(JenkinsRule jenkins) throws Exception {
        for (Result actual : RESULTS) {
            FreeStyleBuild build = buildWithResult(jenkins, actual);
            for (Result ticked : RESULTS) {
                assertEquals(
                        ticked == actual,
                        onlyFor(ticked).conditionPasses(contextOf(build)),
                        "box " + ticked + ", build " + actual);
            }
        }
    }

    @Test
    void passesForAnyOfSeveralTickedResults(JenkinsRule jenkins) throws Exception {
        ResultCondition failureOrUnstable = new ResultCondition(false, true, false, true, false);

        assertTrue(failureOrUnstable.conditionPasses(contextOf(buildWithResult(jenkins, Result.FAILURE))));
        assertTrue(failureOrUnstable.conditionPasses(contextOf(buildWithResult(jenkins, Result.UNSTABLE))));
        assertFalse(failureOrUnstable.conditionPasses(contextOf(buildWithResult(jenkins, Result.SUCCESS))));
    }

    @Test
    void neverPassesWhenNothingIsTicked(JenkinsRule jenkins) throws Exception {
        ResultCondition nothing = new ResultCondition(false, false, false, false, false);

        for (Result actual : RESULTS) {
            assertFalse(nothing.conditionPasses(contextOf(buildWithResult(jenkins, actual))), actual.toString());
        }
    }

    /** While a build is running its result is not set yet; nothing has failed so far, so it counts as success. */
    @Test
    void buildWithoutResultCountsAsSuccess(JenkinsRule jenkins) throws Exception {
        AtomicBoolean successBox = new AtomicBoolean();
        AtomicBoolean failureBox = new AtomicBoolean();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws IOException {
                TaskContext context = contextOf((FreeStyleBuild) build);
                successBox.set(onlyFor(Result.SUCCESS).conditionPasses(context));
                failureBox.set(onlyFor(Result.FAILURE).conditionPasses(context));
                return true;
            }
        });

        jenkins.buildAndAssertSuccess(project);

        assertTrue(successBox.get());
        assertFalse(failureBox.get());
    }
}
