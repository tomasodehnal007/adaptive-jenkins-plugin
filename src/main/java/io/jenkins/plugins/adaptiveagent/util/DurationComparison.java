package io.jenkins.plugins.adaptiveagent.util;

/** Whether a measured time must be longer or shorter than a limit; the choice offered in the forms. */
public enum DurationComparison {
    /** The measured time is greater than the limit. */
    LONGER_THAN("longer than"),
    /** The measured time is less than the limit. */
    SHORTER_THAN("shorter than");

    private final String displayName;

    DurationComparison(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the text shown in the form.
     *
     * @return the name of the comparison
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Compares a measured time with a limit.
     *
     * @param actual the measured time
     * @param limit the limit, in the same unit as {@code actual}
     * @return {@code true} if {@code actual} is strictly longer (or shorter) than {@code limit}; equal is neither
     */
    public boolean holds(long actual, long limit) {
        return this == LONGER_THAN ? actual > limit : actual < limit;
    }
}
