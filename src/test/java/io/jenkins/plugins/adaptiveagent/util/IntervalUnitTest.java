package io.jenkins.plugins.adaptiveagent.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IntervalUnitTest {

    @Test
    void millisecondsStayTheSame() {
        assertEquals(250L, IntervalUnit.MILLISECONDS.toMillis(250));
    }

    @Test
    void secondsAreMultipliedBy1000() {
        assertEquals(3_000L, IntervalUnit.SECONDS.toMillis(3));
    }

    @Test
    void minutesAreMultipliedBy60000() {
        assertEquals(120_000L, IntervalUnit.MINUTES.toMillis(2));
    }

    @Test
    void zeroStaysZero() {
        assertEquals(0L, IntervalUnit.MINUTES.toMillis(0));
    }
}
