package io.jenkins.plugins.adaptiveagent.condition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.plugins.adaptiveagent.util.SizeUnit;
import org.junit.jupiter.api.Test;

class DiskSpaceConditionTest {

    private static final long GIB = 1024L * 1024L * 1024L;

    private final DiskSpaceCondition lessThanFiveGib = new DiskSpaceCondition(5, SizeUnit.GIB);

    @Test
    void holdsWhenThereIsLessFreeSpaceThanTheThreshold() {
        assertTrue(lessThanFiveGib.holdsFor(4 * GIB));
    }

    @Test
    void doesNotHoldWhenThereIsEnoughFreeSpace() {
        assertFalse(lessThanFiveGib.holdsFor(5 * GIB));
        assertFalse(lessThanFiveGib.holdsFor(6 * GIB));
    }
}
