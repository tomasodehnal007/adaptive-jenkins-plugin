package io.jenkins.plugins.adaptiveagent.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** Only chooses the command; the reboot itself is never run here. */
class RebootActionTest {

    @Test
    void usesTheDefaultOfTheOperatingSystem() {
        RebootAction action = new RebootAction(null);

        assertEquals("shutdown -r now", action.effectiveCommand(true));
        assertEquals("shutdown -r -f -t 0", action.effectiveCommand(false));
    }

    @Test
    void blankCommandMeansTheDefault() {
        RebootAction action = new RebootAction("   ");

        assertNull(action.getCommand());
        assertEquals("shutdown -r now", action.effectiveCommand(true));
    }

    @Test
    void aConfiguredCommandWinsOnEverySystem() {
        RebootAction action = new RebootAction("sudo -n shutdown -r now");

        assertEquals("sudo -n shutdown -r now", action.effectiveCommand(true));
        assertEquals("sudo -n shutdown -r now", action.effectiveCommand(false));
    }
}
