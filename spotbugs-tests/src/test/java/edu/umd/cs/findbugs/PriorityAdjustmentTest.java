package edu.umd.cs.findbugs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriorityAdjustmentTest {

    @Test
    void nullValue_throwsIAE() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new PriorityAdjustment(null));
        assertTrue(ex.getMessage().contains("cannot be null or empty"));
    }

    @Test
    void emptyValue_throwsIAE() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new PriorityAdjustment(""));
        assertTrue(ex.getMessage().contains("cannot be null or empty"));
    }

    @Test
    void plusNumber_parsedAsPositiveDelta() {
        PriorityAdjustment pa = new PriorityAdjustment("+2");
        // delta mode => priority + 2
        assertEquals(7, pa.adjust(5));
    }

    @Test
    void minusNumber_parsedAsNegativeDelta() {
        PriorityAdjustment pa = new PriorityAdjustment("-3");
        assertEquals(2, pa.adjust(5));
    }

    @Test
    void plainNumber_parsedAsAbsolute() {
        PriorityAdjustment pa = new PriorityAdjustment("4");
        // absolute mode => result is 4 regardless of input
        assertEquals(4, pa.adjust(1));
        assertEquals(4, pa.adjust(99));
    }

    @Test
    void keyword_raise_meansMinusOneDelta() {
        PriorityAdjustment pa = new PriorityAdjustment("raise");
        assertEquals(4, pa.adjust(5)); // -1
    }

    @Test
    void keyword_lower_meansPlusOneDelta() {
        PriorityAdjustment pa = new PriorityAdjustment("lower");
        assertEquals(6, pa.adjust(5)); // +1
    }

    @Test
    void keyword_suppress_meansLargePositiveDelta() {
        PriorityAdjustment pa = new PriorityAdjustment("suppress");
        assertEquals(105, pa.adjust(5)); // +100
    }

    @Test
    void invalidFormats_throwIAE() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new PriorityAdjustment("+")),
                () -> assertThrows(IllegalArgumentException.class, () -> new PriorityAdjustment("-")),
                () -> assertThrows(IllegalArgumentException.class, () -> new PriorityAdjustment("+x")),
                () -> assertThrows(IllegalArgumentException.class, () -> new PriorityAdjustment("-x")),
                () -> assertThrows(IllegalArgumentException.class, () -> new PriorityAdjustment("x")),
                () -> assertThrows(IllegalArgumentException.class, () -> new PriorityAdjustment("Raise")), // case sensitive
                () -> assertThrows(IllegalArgumentException.class, () -> new PriorityAdjustment("LOWER")));
    }

}
