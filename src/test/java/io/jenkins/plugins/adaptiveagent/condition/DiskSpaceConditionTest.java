package io.jenkins.plugins.adaptiveagent.condition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.plugins.adaptiveagent.util.SizeUnit;
import org.junit.jupiter.api.Test;

/** The decision of the condition for exact amounts of free space, without Jenkins. */
class DiskSpaceConditionTest {

    private static final long GIB = 1024L * 1024L * 1024L;

    @Test
    void holdsWhenThereIsLessFreeSpaceThanTheThreshold() {
        DiskSpaceCondition condition = new DiskSpaceCondition(5, SizeUnit.GIB);

        assertTrue(condition.holdsFor(5 * GIB - 1));
        assertTrue(condition.holdsFor(0));
    }

    @Test
    void doesNotHoldWhenThereIsEnoughFreeSpace() {
        DiskSpaceCondition condition = new DiskSpaceCondition(5, SizeUnit.GIB);

        assertFalse(condition.holdsFor(5 * GIB + 1));
        assertFalse(condition.holdsFor(100 * GIB));
    }

    @Test
    void exactlyTheThresholdIsNotBelowIt() {
        assertFalse(new DiskSpaceCondition(5, SizeUnit.GIB).holdsFor(5 * GIB));
    }

    @Test
    void thresholdInMegabytesIsConvertedToBytes() {
        DiskSpaceCondition condition = new DiskSpaceCondition(100, SizeUnit.MIB);
        long hundredMb = 100L * 1024L * 1024L;

        assertTrue(condition.holdsFor(hundredMb - 1));
        assertFalse(condition.holdsFor(hundredMb));
    }

    @Test
    void zeroThresholdNeverHolds() {
        assertFalse(new DiskSpaceCondition(0, SizeUnit.GIB).holdsFor(0));
    }
}
