package io.jenkins.plugins.adaptiveagent.action;

import static io.jenkins.plugins.adaptiveagent.TestSupport.agentWith;
import static io.jenkins.plugins.adaptiveagent.TestSupport.countLines;
import static io.jenkins.plugins.adaptiveagent.TestSupport.pre;
import static io.jenkins.plugins.adaptiveagent.TestSupport.projectOn;
import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.slaves.DumbSlave;
import io.jenkins.plugins.adaptiveagent.condition.NoCondition;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class StopBuildActionTest {

    @Test
    void stopsTheBuildBeforeItsStepsRun(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(jenkins, pre(new NoCondition(), new StopBuildAction()));

        FreeStyleBuild build =
                jenkins.buildAndAssertStatus(Result.ABORTED, projectOn(jenkins, agent, "echo SHOULD_NOT_RUN"));

        assertEquals(0, countLines(JenkinsRule.getLog(build), "SHOULD_NOT_RUN"));
    }
}
