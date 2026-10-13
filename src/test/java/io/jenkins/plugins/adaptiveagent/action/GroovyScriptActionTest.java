package io.jenkins.plugins.adaptiveagent.action;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hudson.model.Computer;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.springframework.security.access.AccessDeniedException;

/** Only administrators can save a Groovy action. */
@WithJenkins
class GroovyScriptActionTest {

    @Test
    void userWhoIsNotAdministratorCannotCreateIt(JenkinsRule jenkins) {
        jenkins.jenkins.setSecurityRealm(jenkins.createDummySecurityRealm());
        jenkins.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ, Computer.CONFIGURE)
                .everywhere()
                .to("configurer")
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("admin"));

        try (ACLContext ignored = ACL.as2(User.getById("configurer", true).impersonate2())) {
            assertThrows(AccessDeniedException.class, () -> new GroovyScriptAction("println 1"));
        }
        try (ACLContext ignored = ACL.as2(User.getById("admin", true).impersonate2())) {
            assertDoesNotThrow(() -> new GroovyScriptAction("println 1"));
        }
    }
}
