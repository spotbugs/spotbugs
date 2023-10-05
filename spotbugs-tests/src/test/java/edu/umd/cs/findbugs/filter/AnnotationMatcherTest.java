/*
 * SpotBugs - Find Bugs in Java programs
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.tools.ant.filters.StringInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

public class AnnotationMatcherTest {

    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    private String annotationName;

    @Before
    public void setUp() {
        annotationName = "org.immutables.value.Generated";
    }

    @After
    public void tearDown() {
        // Some other test cases fail in case the context is not correctly
        // cleaned up here.
        AnalysisContext.removeCurrentAnalysisContext();
    }

    @Test
    public void writeXML() throws Exception {
        AnnotationMatcher sm = new AnnotationMatcher(annotationName);

        String xmlOutput = writeXMLAndGetStringOutput(sm, false);
        assertEquals("<Annotation name=\"" + annotationName + "\"/>", xmlOutput);

        sm = new AnnotationMatcher(annotationName);
        xmlOutput = writeXMLAndGetStringOutput(sm, true);
        assertEquals("<Annotation name=\"" + annotationName + "\" disabled=\"true\"/>", xmlOutput);
    }

    @Test
    public void testMatchMissingPrimaryAnnotationIsFalse() throws Exception {
        Filter filter = readFilterFromXML();
        // no primary annotation; should not match
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 0);
        assertFalse(filter.match(bug));
    }

    @Test
    public void testMatchMissingJavaAnnotationIsFalse() throws Exception {
        Filter filter = readFilterFromXML();
        // added primary class annotation; should not match b.c. missing java annotation
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 0);
        ClassAnnotation buggyClass = new ClassAnnotation("BuggyClass", "BuggyClass.java");
        bug.add(buggyClass);
        assertFalse(filter.match(bug));
    }

    @Test
    public void testMatchOtherJavaAnnotationIsFalse() throws Exception {
        Filter filter = readFilterFromXML();
        // added primary class annotation; should not match b.c. other java annotation
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 0);
        ClassAnnotation buggyClass = new ClassAnnotation("BuggyClass", "BuggyClass.java");
        buggyClass.setJavaAnnotationNames(Arrays.asList("org.immutables.value.Other"));
        bug.add(buggyClass);
        assertFalse(filter.match(bug));
    }

    @Test
    public void testMatchJavaAnnotationIsTrue() throws Exception {
        Filter filter = readFilterFromXML();
        // added primary class annotation; should match b.c. has java annotation
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 0);
        ClassAnnotation buggyClass = new ClassAnnotation("AnnotatedBuggyClass", "AnnotatedBuggyClass.java");
        buggyClass.setJavaAnnotationNames(Arrays.asList(annotationName));
        bug.add(buggyClass);
        assertTrue(filter.match(bug));
    }

    private Filter readFilterFromXML() throws IOException {
        AnnotationMatcher sm = new AnnotationMatcher(annotationName);

        String matcherXml = writeXMLAndGetStringOutput(sm, false);
        String filterXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "\n<FindBugsFilter>"
                + "\n<Match>"
                + "\n"
                + matcherXml
                + "\n</Match>"
                + "\n</FindBugsFilter>\n";

        Filter filter = new Filter(new StringInputStream(filterXml));
        return filter;
    }

    @Test
    public void testPerformAnalysis() throws Exception {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/org/immutables/value/Generated.class"),
                Paths.get("../spotbugsTestCases/build/classes/java/main/org/immutables/value/Value.class"),
                Paths.get("../spotbugsTestCases/build/classes/java/main/org/immutables/value/Value$Immutable.class"),
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/issue543/FoobarValue.class"),
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/issue543/ImmutableFoobarValue.class"),
                Paths.get(
                        "../spotbugsTestCases/build/classes/java/main/ghIssues/issue543/ImmutableFoobarValue$1.class"),
                Paths.get(
                        "../spotbugsTestCases/build/classes/java/main/ghIssues/issue543/ImmutableFoobarValue$Builder.class"));

        AnnotationMatcher bugInstanceMatcher = new AnnotationMatcher(annotationName);
        long numberOfMatchedBugs = bugCollection.getCollection().stream()
                .filter(bugInstanceMatcher::match)
                .count();

        assertEquals(4, numberOfMatchedBugs);
    }

    private String writeXMLAndGetStringOutput(AnnotationMatcher matcher, boolean disabled) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(outputStream);

        matcher.writeXML(xmlOutput, disabled);
        xmlOutput.finish();

        return outputStream.toString(StandardCharsets.UTF_8.name()).trim();
    }

}
