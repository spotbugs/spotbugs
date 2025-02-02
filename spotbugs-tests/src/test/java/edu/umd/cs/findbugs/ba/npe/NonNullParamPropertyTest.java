package edu.umd.cs.findbugs.ba.npe;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.ba.interproc.ParameterProperty;

class NonNullParamPropertyTest {

    ParameterProperty empty;

    ParameterProperty nonEmpty;

    ParameterProperty extremes;

    @BeforeEach
    void setUp() {
        empty = new ParameterProperty();

        nonEmpty = new ParameterProperty();
        nonEmpty.setParamWithProperty(11, true);
        nonEmpty.setParamWithProperty(25, true);

        extremes = new ParameterProperty();
        extremes.setParamWithProperty(0, true);
        extremes.setParamWithProperty(31, true);
    }

    @Test
    void testEmpty() {
        for (int i = 0; i < 32; ++i) {
            Assertions.assertFalse(empty.hasProperty(i));
        }
    }

    @Test
    void testIsEmpty() {
        Assertions.assertTrue(empty.isEmpty());
        Assertions.assertFalse(nonEmpty.isEmpty());
        Assertions.assertFalse(extremes.isEmpty());
    }

    @Test
    void testNonEmpty() {
        Assertions.assertTrue(nonEmpty.hasProperty(11));
        Assertions.assertTrue(nonEmpty.hasProperty(25));
        Assertions.assertFalse(nonEmpty.hasProperty(5));
    }

    @Test
    void testExtremes() {
        Assertions.assertTrue(extremes.hasProperty(0));
        Assertions.assertTrue(extremes.hasProperty(31));
        Assertions.assertFalse(extremes.hasProperty(10));
    }

    @Test
    void testOutOfBounds() {
        Assertions.assertFalse(nonEmpty.hasProperty(-1));
        Assertions.assertFalse(nonEmpty.hasProperty(32));
    }
}
