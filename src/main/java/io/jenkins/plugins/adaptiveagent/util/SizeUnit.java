package io.jenkins.plugins.adaptiveagent.util;

/** Unit of a disk size entered in the form. */
public enum SizeUnit {
    /** Mebibyte, 1024 * 1024 bytes. */
    MIB("MiB", 1024L * 1024L),
    /** Gibibyte, 1024 * 1024 * 1024 bytes. */
    GIB("GiB", 1024L * 1024L * 1024L);

    private final String displayName;
    private final long bytes;

    SizeUnit(String displayName, long bytes) {
        this.displayName = displayName;
        this.bytes = bytes;
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
     * Converts an amount in this unit to bytes.
     *
     * @param amount the size in this unit
     * @return the size in bytes; {@link Long#MAX_VALUE} if it does not fit into a {@code long}
     */
    public long toBytes(long amount) {
        try {
            return Math.multiplyExact(amount, bytes);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }
}
