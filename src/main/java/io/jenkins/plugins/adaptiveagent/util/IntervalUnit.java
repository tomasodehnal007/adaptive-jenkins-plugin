package io.jenkins.plugins.adaptiveagent.util;

import java.util.concurrent.TimeUnit;

/** Unit of a time value entered in the form. */
public enum IntervalUnit {
    /** Thousandths of a second. */
    MILLISECONDS("milliseconds", TimeUnit.MILLISECONDS),
    /** Seconds. */
    SECONDS("seconds", TimeUnit.SECONDS),
    /** Minutes. */
    MINUTES("minutes", TimeUnit.MINUTES);

    private final String displayName;
    private final TimeUnit timeUnit;

    IntervalUnit(String displayName, TimeUnit timeUnit) {
        this.displayName = displayName;
        this.timeUnit = timeUnit;
    }

    /**
     * Returns the text shown in the form.
     *
     * @return the name of the unit
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converts an amount in this unit to milliseconds.
     *
     * @param amount the time in this unit
     * @return the time in milliseconds; {@link Long#MAX_VALUE} if it does not fit into a {@code long}
     */
    public long toMillis(long amount) {
        return timeUnit.toMillis(amount);
    }
}
