package edu.umd.cs.findbugs;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BugInstanceTest {

    private BugInstance b;

    @Before
    public void setUp() {
        b = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        b.setProperty("A", "a");
        b.setProperty("B", "b");
        b.setProperty("C", "c");
    }

    @Test
    public void testPropertyIterator() {
        checkPropertyIterator(b.propertyIterator(), new String[] { "A", "B", "C" }, new String[] { "a", "b", "c" });
    }

    @Test
    public void testRemoveThroughIterator1() {
        removeThroughIterator(b.propertyIterator(), "A");
        checkPropertyIterator(b.propertyIterator(), new String[] { "B", "C" }, new String[] { "b", "c" });
    }

    @Test
    public void testRemoveThroughIterator2() {
        removeThroughIterator(b.propertyIterator(), "B");
        checkPropertyIterator(b.propertyIterator(), new String[] { "A", "C" }, new String[] { "a", "c" });
    }

    @Test
    public void testRemoveThroughIterator3() {
        removeThroughIterator(b.propertyIterator(), "C");
        checkPropertyIterator(b.propertyIterator(), new String[] { "A", "B" }, new String[] { "a", "b" });
    }

    @Test(expected = NoSuchElementException.class)
    public void testIterateTooFar() {
        Iterator<BugProperty> iter = b.propertyIterator();
        get(iter);
        get(iter);
        get(iter);
        iter.next();
    }

    @Test(expected = IllegalStateException.class)
    public void testMultipleRemove() {
        Iterator<BugProperty> iter = b.propertyIterator();
        iter.next();
        iter.remove();
        iter.remove();
    }

    @Test(expected = IllegalStateException.class)
    public void testRemoveBeforeNext() {
        Iterator<BugProperty> iter = b.propertyIterator();
        iter.remove();
    }

    @Test
    public void testRemoveAndAdd() {
        removeThroughIterator(b.propertyIterator(), "C");
        b.setProperty("D", "d");
        checkPropertyIterator(b.propertyIterator(), new String[] { "A", "B", "D" }, new String[] { "a", "b", "d" });
        b.setProperty("E", "e");
        checkPropertyIterator(b.propertyIterator(), new String[] { "A", "B", "D", "E" }, new String[] { "a", "b", "d", "e" });
    }

    @Test
    public void testRemoveAll1() {
        removeThroughIterator(b.propertyIterator(), "A");
        checkPropertyIterator(b.propertyIterator(), new String[] { "B", "C" }, new String[] { "b", "c" });
        removeThroughIterator(b.propertyIterator(), "B");
        checkPropertyIterator(b.propertyIterator(), new String[] { "C" }, new String[] { "c" });
        removeThroughIterator(b.propertyIterator(), "C");
        checkPropertyIterator(b.propertyIterator(), new String[0], new String[0]);
    }

    private void get(Iterator<BugProperty> iter) {
        try {
            iter.next();
            // Good
        } catch (NoSuchElementException e) {
            Assert.assertTrue(false);
        }
    }

    private void checkPropertyIterator(Iterator<BugProperty> iter, String[] names, String[] values) {
        if (names.length != values.length) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < names.length; ++i) {
            Assert.assertTrue(iter.hasNext());
            String name = names[i];
            String value = values[i];
            checkProperty(iter.next(), name, value);
        }
        Assert.assertFalse(iter.hasNext());
    }

    private void checkProperty(BugProperty property, String name, String value) {
        Assert.assertEquals(property.getName(), name);
        Assert.assertEquals(property.getValue(), value);
    }

    private void removeThroughIterator(Iterator<BugProperty> iter, String name) {
        while (iter.hasNext()) {
            BugProperty prop = iter.next();
            if (prop.getName().equals(name)) {
                iter.remove();
            }
        }
    }
}
