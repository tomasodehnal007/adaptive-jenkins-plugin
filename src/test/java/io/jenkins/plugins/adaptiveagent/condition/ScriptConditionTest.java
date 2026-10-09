package io.jenkins.plugins.adaptiveagent.condition;

import static io.jenkins.plugins.adaptiveagent.TestSupport.agentWith;
import static io.jenkins.plugins.adaptiveagent.TestSupport.countLines;
import static io.jenkins.plugins.adaptiveagent.TestSupport.echo;
import static io.jenkins.plugins.adaptiveagent.TestSupport.pre;
import static io.jenkins.plugins.adaptiveagent.TestSupport.projectOn;
import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.model.FreeStyleBuild;
import hudson.slaves.DumbSlave;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ScriptConditionTest {

    @Test
    void actionRunsWhenTheScriptExitsWithZero(JenkinsRule jenkins) throws Exception {
        DumbSlave agent =
                agentWith(jenkins, pre(new ScriptCondition("echo CONDITION_OUTPUT; exit 0"), echo("ACTION_RAN")));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(projectOn(jenkins, agent, "true"));

        String log = JenkinsRule.getLog(build);
        assertEquals(1, countLines(log, "ACTION_RAN"), log);
        assertEquals(1, countLines(log, "CONDITION_OUTPUT"), "the output of the script goes to the build log:\n" + log);
    }

    @Test
    void actionIsSkippedWhenTheScriptExitsWithNonZero(JenkinsRule jenkins) throws Exception {
        DumbSlave agent = agentWith(jenkins, pre(new ScriptCondition("exit 1"), echo("ACTION_RAN")));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(projectOn(jenkins, agent, "true"));

        assertEquals(0, countLines(JenkinsRule.getLog(build), "ACTION_RAN"));
    }
}
