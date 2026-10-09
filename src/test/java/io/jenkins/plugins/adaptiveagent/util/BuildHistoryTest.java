package io.jenkins.plugins.adaptiveagent.util;

import static hudson.model.Result.FAILURE;
import static hudson.model.Result.SUCCESS;
import static io.jenkins.plugins.adaptiveagent.TestSupport.buildWithResult;
import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import io.jenkins.plugins.adaptiveagent.TaskContext;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** The history is read from builds on the built-in node, so no agent is started. */
@WithJenkins
class BuildHistoryTest {

    /** Runs one more build and returns what {@code lastFinished(limit)} sees from inside it. */
    private static List<String> idsSeenFromANewBuild(JenkinsRule jenkins, int limit) throws Exception {
        AtomicReference<List<String>> seen = new AtomicReference<>();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                TaskContext context = new TaskContext(build, jenkins.jenkins.toComputer(), null, null, null);
                seen.set(BuildHistory.lastFinished(context, limit).stream()
                        .map(Run::getExternalizableId)
                        .toList());
                return true;
            }
        });
        jenkins.buildAndAssertSuccess(project);
        return seen.get();
    }

    @Test
    void listsBuildsOfAllJobsNewestFirstWithoutTheCurrentBuild(JenkinsRule jenkins) throws Exception {
        FreeStyleBuild older = buildWithResult(jenkins, SUCCESS);
        FreeStyleBuild newer = buildWithResult(jenkins, FAILURE); // another job

        List<String> seen = idsSeenFromANewBuild(jenkins, 10);

        assertEquals(List.of(newer.getExternalizableId(), older.getExternalizableId()), seen);
    }

    @Test
    void returnsAtMostTheRequestedNumberOfBuilds(JenkinsRule jenkins) throws Exception {
        buildWithResult(jenkins, SUCCESS);
        FreeStyleBuild newest = buildWithResult(jenkins, FAILURE);

        List<String> seen = idsSeenFromANewBuild(jenkins, 1);

        assertEquals(List.of(newest.getExternalizableId()), seen);
    }

    @Test
    void nonPositiveLimitReturnsNothing(JenkinsRule jenkins) throws Exception {
        buildWithResult(jenkins, SUCCESS);

        assertEquals(List.of(), idsSeenFromANewBuild(jenkins, 0));
        assertEquals(List.of(), idsSeenFromANewBuild(jenkins, -1));
    }

    @Test
    void isEmptyWhenTheAgentHasRunNothingElse(JenkinsRule jenkins) throws Exception {
        assertEquals(List.of(), idsSeenFromANewBuild(jenkins, 5));
    }
}
