package io.jenkins.plugins.adaptiveagent.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SizeUnitTest {

    @Test
    void megabyteIsMultipliedBy1024Squared() {
        assertEquals(1_048_576L, SizeUnit.MB.toBytes(1));
        assertEquals(5L * 1_048_576L, SizeUnit.MB.toBytes(5));
    }

    @Test
    void gigabyteIsMultipliedBy1024Cubed() {
        assertEquals(1_073_741_824L, SizeUnit.GB.toBytes(1));
        assertEquals(2L * 1_073_741_824L, SizeUnit.GB.toBytes(2));
    }

    @Test
    void zeroStaysZero() {
        assertEquals(0L, SizeUnit.GB.toBytes(0));
    }

    @Test
    void tooLargeAmountDoesNotOverflowIntoANegativeNumber() {
        assertEquals(Long.MAX_VALUE, SizeUnit.GB.toBytes(Long.MAX_VALUE));
    }
}
