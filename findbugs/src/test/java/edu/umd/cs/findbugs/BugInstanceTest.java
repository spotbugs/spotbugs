package edu.umd.cs.findbugs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import junit.framework.Assert;
import junit.framework.TestCase;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

public class BugInstanceTest extends TestCase {

    BugInstance b;

    @Override
    protected void setUp() throws Exception {
        b = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        b.setProperty("A", "a");
        b.setProperty("B", "b");
        b.setProperty("C", "c");
    }

    public void testPropertyIterator() {
        checkPropertyIterator(b.propertyIterator(), new String[] { "A", "B", "C" }, new String[] { "a", "b", "c" });
    }

    public void testRemoveThroughIterator1() {
        removeThroughIterator(b.propertyIterator(), "A");
        checkPropertyIterator(b.propertyIterator(), new String[] { "B", "C" }, new String[] { "b", "c" });
    }

    public void testRemoveThroughIterator2() {
        removeThroughIterator(b.propertyIterator(), "B");
        checkPropertyIterator(b.propertyIterator(), new String[] { "A", "C" }, new String[] { "a", "c" });
    }

    public void testRemoveThroughIterator3() {
        removeThroughIterator(b.propertyIterator(), "C");
        checkPropertyIterator(b.propertyIterator(), new String[] { "A", "B" }, new String[] { "a", "b" });
    }

    public void testIterateTooFar() {
        Iterator<BugProperty> iter = b.propertyIterator();
        get(iter);
        get(iter);
        get(iter);
        noMore(iter);
    }

    public void testMultipleRemove() {
        Iterator<BugProperty> iter = b.propertyIterator();
        iter.next();
        iter.remove();
        try {
            iter.remove();
            fail();
        } catch (IllegalStateException e) {
            assert true;
        }
    }

    public void testRemoveBeforeNext() {
        Iterator<BugProperty> iter = b.propertyIterator();
        try {
            iter.remove();
            Assert.fail();
        } catch (IllegalStateException e) {
            assert true;
        }
    }

    public void testRemoveAndAdd() {
        removeThroughIterator(b.propertyIterator(), "C");
        b.setProperty("D", "d");
        checkPropertyIterator(b.propertyIterator(), new String[] { "A", "B", "D" }, new String[] { "a", "b", "d" });
        b.setProperty("E", "e");
        checkPropertyIterator(b.propertyIterator(), new String[] { "A", "B", "D", "E" }, new String[] { "a", "b", "d", "e" });
    }

    public void testRemoveAll1() {
        removeThroughIterator(b.propertyIterator(), "A");
        checkPropertyIterator(b.propertyIterator(), new String[] { "B", "C" }, new String[] { "b", "c" });
        removeThroughIterator(b.propertyIterator(), "B");
        checkPropertyIterator(b.propertyIterator(), new String[] { "C" }, new String[] { "c" });
        removeThroughIterator(b.propertyIterator(), "C");
        checkPropertyIterator(b.propertyIterator(), new String[0], new String[0]);
    }

    public void testWriteCloudPropertiesWithoutMessagesEnabled() throws Exception {
        BugInstance inst = new BugInstance("ABC", 2);
        inst.getXmlProps().setConsensus("NOT_A_BUG");
        inst.getXmlProps().setFirstSeen(BugInstance.firstSeenXMLFormat().parse("4/11/10 2:00 PM"));
        inst.getXmlProps().setReviewCount(3);

        SortedBugCollection bc = new SortedBugCollection();
        bc.setWithMessages(false);


        String output = writeXML(inst, bc);
        System.err.println(output);

        assertTrue("firstSeen", output.contains("firstSeen=\"4/11/10 2:00 PM\""));
        assertTrue("consensus", output.contains("consensus=\"NOT_A_BUG\""));
        assertTrue("reviews", output.contains("reviews=\"3\""));
        assertFalse("notAProblem", output.contains("notAProblem"));
        assertFalse("ageInDays", output.contains("ageInDays"));
    }

    public void testWriteCloudPropertiesWithMessagesEnabled() throws Exception {
        BugInstance inst = new BugInstance("ABC", 2);
        inst.addClass("my.class");
        inst.getXmlProps().setConsensus("NOT_A_BUG");
        inst.getXmlProps().setFirstSeen(BugInstance.firstSeenXMLFormat().parse("4/11/10 2:00 PM"));
        inst.getXmlProps().setReviewCount(3);

        SortedBugCollection bc = new SortedBugCollection();
        bc.setWithMessages(true);

        String output = writeXML(inst, bc);
        System.err.println(output);

        assertTrue("firstSeen", output.contains("firstSeen=\"4/11/10 2:00 PM\""));
        assertTrue("consensus", output.contains("consensus=\"NOT_A_BUG\""));
        assertTrue("reviews", output.contains("reviews=\"3\""));
        assertTrue("notAProblem", output.contains("notAProblem=\"true\""));
        assertTrue("ageInDays", output.contains("ageInDays="));
    }

    private String writeXML(BugInstance inst, BugCollection bc) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        XMLOutput out = new OutputStreamXMLOutput(bout);
        inst.writeXML(out, bc, bc.getWithMessages());
        out.finish();
        return new String(bout.toByteArray(), "UTF-8");
    }

    private void get(Iterator<BugProperty> iter) {
        try {
            iter.next();
            // Good
        } catch (NoSuchElementException e) {
            Assert.assertTrue(false);
        }
    }

    private void noMore(Iterator<BugProperty> iter) {
        try {
            iter.next();
            Assert.fail();
        } catch (NoSuchElementException e) {
            assert true;
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
