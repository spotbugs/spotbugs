package edu.umd.cs.findbugs.ba.npe;

import junit.framework.Assert;
import junit.framework.TestCase;
import edu.umd.cs.findbugs.ba.interproc.ParameterProperty;

public class NonNullParamPropertyTest extends TestCase {

    ParameterProperty empty;

    ParameterProperty nonEmpty;

    ParameterProperty extremes;

    @Override
    protected void setUp() throws Exception {
        empty = new ParameterProperty();

        nonEmpty = new ParameterProperty();
        nonEmpty.setParamWithProperty(11, true);
        nonEmpty.setParamWithProperty(25, true);

        extremes = new ParameterProperty();
        extremes.setParamWithProperty(0, true);
        extremes.setParamWithProperty(31, true);
    }

    public void testEmpty() {
        for (int i = 0; i < 32; ++i) {
            Assert.assertFalse(empty.hasProperty(i));
        }
    }

    public void testIsEmpty() {
        Assert.assertTrue(empty.isEmpty());
        Assert.assertFalse(nonEmpty.isEmpty());
        Assert.assertFalse(extremes.isEmpty());
    }

    public void testNonEmpty() {
        Assert.assertTrue(nonEmpty.hasProperty(11));
        Assert.assertTrue(nonEmpty.hasProperty(25));
        Assert.assertFalse(nonEmpty.hasProperty(5));
    }

    public void testExtremes() {
        Assert.assertTrue(extremes.hasProperty(0));
        Assert.assertTrue(extremes.hasProperty(31));
        Assert.assertFalse(extremes.hasProperty(10));
    }

    public void testOutOfBounds() {
        Assert.assertFalse(nonEmpty.hasProperty(-1));
        Assert.assertFalse(nonEmpty.hasProperty(32));
    }
}
