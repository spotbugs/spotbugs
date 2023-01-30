package edu.umd.cs.findbugs;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

public class BugInstanceTest {

    private BugInstance b;

    @Before
    public void setUp() {
        b = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        b.setProperty("A", "a");
        b.setProperty("B", "b");
        b.setProperty("C", "c");
    }

    private static final String EXPECTED_XML = ""
            + "<BugInstance type=\"UUF_UNUSED_FIELD\" priority=\"1\" rank=\"16\" abbrev=\"UuF\" category=\"PERFORMANCE\">\n"
            + "  <Class classname=\"ghIssues.issue543.ImmutableFoobarValue.class\" classAnnotationNames=\"org.immutables.value.Generated\">\n"
            + "    <SourceLine classname=\"ghIssues.issue543.ImmutableFoobarValue.class\"/>\n"
            + "  </Class>\n"
            + "  <Method classname=\"ghIssues.issue543.ImmutableFoobarValue.class\" name=\"foo\" signature=\"int\" isStatic=\"false\" classAnnotationNames=\"org.immutables.value.Generated\"/>\n"
            + "  <Field classname=\"ghIssues.issue543.ImmutableFoobarValue.class\" name=\"foo\" signature=\"int\" isStatic=\"false\" classAnnotationNames=\"org.immutables.value.Generated\">\n"
            + "    <SourceLine classname=\"ghIssues.issue543.ImmutableFoobarValue.class\"/>\n"
            + "  </Field>\n"
            + "</BugInstance>";

    @Test
    public void testWriteXML() throws Exception {
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 0);
        // test all PackageMemberAnnotations
        String className = "ghIssues.issue543.ImmutableFoobarValue.class";
        ClassAnnotation classAnnotation = new ClassAnnotation(className);
        classAnnotation.setJavaAnnotationNames(List.of("org.immutables.value.Generated"));
        bug.add(classAnnotation);

        MethodAnnotation methodAnnotation = new MethodAnnotation(className, "foo", "int", false);
        methodAnnotation.setJavaAnnotationNames(List.of("org.immutables.value.Generated"));
        bug.add(methodAnnotation);

        FieldAnnotation fieldAnnotation = new FieldAnnotation(className, "foo", "int", false);
        fieldAnnotation.setJavaAnnotationNames(List.of("org.immutables.value.Generated"));
        bug.add(fieldAnnotation);

        String xml = writeXMLAndGetStringOutput(bug);
        assertThat(xml, equalTo(EXPECTED_XML));
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

    private String writeXMLAndGetStringOutput(BugInstance bug) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(outputStream);

        bug.writeXML(xmlOutput);
        xmlOutput.finish();

        return outputStream.toString(StandardCharsets.UTF_8.name()).trim();
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
