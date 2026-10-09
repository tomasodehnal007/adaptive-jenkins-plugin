package io.jenkins.plugins.adaptiveagent.condition;

import static hudson.model.Result.FAILURE;
import static hudson.model.Result.SUCCESS;
import static io.jenkins.plugins.adaptiveagent.TestSupport.buildWithResult;
import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** Evaluates the condition inside a build that follows real builds on the built-in node. */
@WithJenkins
class HistoryFailedConditionOnBuildsTest {

    private static boolean evaluatedInANewBuild(JenkinsRule jenkins, HistoryFailedCondition condition)
            throws Exception {
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
    void holdsAfterThreeFailedBuildsOfDifferentJobs(JenkinsRule jenkins) throws Exception {
        buildWithResult(jenkins, FAILURE);
        buildWithResult(jenkins, FAILURE);
        buildWithResult(jenkins, FAILURE);

        assertEquals(true, evaluatedInANewBuild(jenkins, new HistoryFailedCondition(3)));
    }

    @Test
    void doesNotHoldWhenALatestBuildSucceeded(JenkinsRule jenkins) throws Exception {
        buildWithResult(jenkins, FAILURE);
        buildWithResult(jenkins, FAILURE);
        buildWithResult(jenkins, SUCCESS);

        assertEquals(false, evaluatedInANewBuild(jenkins, new HistoryFailedCondition(3)));
    }
}
