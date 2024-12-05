/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
 *
 *  Author: Andrey Loskutov
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.tools.ant.filters.StringInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

class SourceMatcherTest {

    private BugInstance bug;
    private String fileName;

    @BeforeEach
    void setUp() {
        bug = new BugInstance("UUF_UNUSED_FIELD", 0);
        fileName = "bla.groovy";
    }

    @AfterEach
    void tearDown() {
        // Some other test cases fail in case the context is not correctly
        // cleaned up here.
        AnalysisContext.removeCurrentAnalysisContext();
    }

    @Test
    void writeXML() throws Exception {
        SourceMatcher sm = new SourceMatcher(fileName);

        String xmlOutput = writeXMLAndGetStringOutput(sm, false);
        assertEquals("<Source name=\"" + fileName + "\"/>", xmlOutput);

        sm = new SourceMatcher(fileName);
        xmlOutput = writeXMLAndGetStringOutput(sm, true);
        assertEquals("<Source name=\"" + fileName + "\" disabled=\"true\"/>", xmlOutput);
    }

    @Test
    void readXML() throws Exception {
        SourceMatcher sm = new SourceMatcher(fileName);

        String xml = writeXMLAndGetStringOutput(sm, false);
        xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "\n<FindBugsFilter>"
                + "\n<Match>"
                + "\n" + xml
                + "\n</Match>"
                + "\n</FindBugsFilter>\n";

        Filter filter = new Filter(new StringInputStream(xml));

        assertFalse(filter.match(bug));

        bug.addClass("bla", fileName);
        assertTrue(filter.match(bug));
    }


    @Test
    void match() {
        SourceMatcher sm = new SourceMatcher(fileName);

        // no source set: test incomplete data
        assertFalse(sm.match(bug));

        bug.addClass("bla", null);
        assertFalse(sm.match(bug));

        ClassAnnotation primaryClass = bug.getPrimaryClass();
        primaryClass.setSourceLines(SourceLineAnnotation.createUnknown("bla", ""));
        assertFalse(sm.match(bug));

        // set right source file
        primaryClass.setSourceLines(SourceLineAnnotation.createUnknown("bla", fileName));

        // exact match
        assertTrue(sm.match(bug));

        // regexp first part
        sm = new SourceMatcher("~bla.*");
        assertTrue(sm.match(bug));

        sm = new SourceMatcher("~blup.*");
        assertFalse(sm.match(bug));

        // regexp second part
        sm = new SourceMatcher("~.*\\.groovy");
        assertTrue(sm.match(bug));

        sm = new SourceMatcher("~.*\\.java");
        assertFalse(sm.match(bug));
    }

    @Test
    void testRealPathMatchWithRegexpAndProject() {
        // add this test class as the bug target
        bug.addClass("SourceMatcherTest", null);
        ClassAnnotation primaryClass = bug.getPrimaryClass();

        // set source file
        primaryClass.setSourceLines(SourceLineAnnotation.createUnknown("SourceMatcherTest", "SourceMatcherTest.java"));

        // setup a testing project with source directory, as of right now the source directory should really exist!!
        Project testProject = new Project();
        String sourceDir = "src/test/java/edu/umd/cs/findbugs/filter";
        testProject.addSourceDirs(Collections.singletonList(sourceDir));

        // add test project to SourceLineAnnotation
        SourceLineAnnotation.generateRelativeSource(new File(sourceDir), testProject);

        // regexp match source folder with project
        SourceMatcher sm = new SourceMatcher("~.*findbugs.*.java");
        assertTrue(sm.match(bug), "The regex matches the source directory of the given java file");
        sm = new SourceMatcher("~.*notfound.*.java");
        assertFalse(sm.match(bug), "The regex does not match the source directory of the given java file");
    }

    @Test
    void testRealPathMatchWithRegexpAndAnalysisContext() {
        // add this test class as the bug target
        bug.addClass("SourceMatcherTest", null);
        ClassAnnotation primaryClass = bug.getPrimaryClass();

        // set source file
        primaryClass.setSourceLines(SourceLineAnnotation.createUnknown("SourceMatcherTest", "SourceMatcherTest.java"));

        // setup a testing project with source directory, as of right now the source directory should really exist!!
        Project testProject = new Project();
        String sourceDir = "src/test/java/edu/umd/cs/findbugs/filter";
        testProject.addSourceDirs(Collections.singletonList(sourceDir));

        // setup test analysis context
        AnalysisContext.setCurrentAnalysisContext(new AnalysisContext(testProject));

        // regexp match source folder with analysis context
        SourceMatcher sm = new SourceMatcher("~.*findbugs.*.java");
        assertTrue(sm.match(bug), "The regex matches the source directory of the given java file");
        sm = new SourceMatcher("~.*notfound.*.java");
        assertFalse(sm.match(bug), "The regex does not match the source directory of the given java file");
    }

    private String writeXMLAndGetStringOutput(SourceMatcher matcher, boolean disabled) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(outputStream);

        matcher.writeXML(xmlOutput, disabled);
        xmlOutput.finish();

        return outputStream.toString(StandardCharsets.UTF_8.name()).trim();
    }

}
