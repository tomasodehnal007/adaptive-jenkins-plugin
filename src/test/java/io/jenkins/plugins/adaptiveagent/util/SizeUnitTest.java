package io.jenkins.plugins.adaptiveagent.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SizeUnitTest {

    @Test
    void mebibyteIsMultipliedBy1024Squared() {
        assertEquals(1_048_576L, SizeUnit.MIB.toBytes(1));
        assertEquals(5L * 1_048_576L, SizeUnit.MIB.toBytes(5));
    }

    @Test
    void gibibyteIsMultipliedBy1024Cubed() {
        assertEquals(1_073_741_824L, SizeUnit.GIB.toBytes(1));
        assertEquals(2L * 1_073_741_824L, SizeUnit.GIB.toBytes(2));
    }

    @Test
    void zeroStaysZero() {
        assertEquals(0L, SizeUnit.GIB.toBytes(0));
    }

    @Test
    void tooLargeAmountDoesNotOverflowIntoANegativeNumber() {
        assertEquals(Long.MAX_VALUE, SizeUnit.GIB.toBytes(Long.MAX_VALUE));
    }
}
