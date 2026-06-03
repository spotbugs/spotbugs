/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

class LocalMatcherTest {

    private BugInstance bug;

    @BeforeEach
    void setUp() {
        bug = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        // register=1, pc=0
        LocalVariableAnnotation local = new LocalVariableAnnotation("myVariable", 1, 0);
        bug.add(local);
    }

    @Test
    void exactNameMatchSucceeds() {
        LocalMatcher matcher = new LocalMatcher("myVariable");
        assertTrue(matcher.match(bug));
    }

    @Test
    void exactNameMatchFails() {
        LocalMatcher matcher = new LocalMatcher("otherVariable");
        assertFalse(matcher.match(bug));
    }

    @Test
    void regexNameMatchSucceeds() {
        LocalMatcher matcher = new LocalMatcher("~my.*");
        assertTrue(matcher.match(bug));
    }

    @Test
    void regexNameMatchFails() {
        LocalMatcher matcher = new LocalMatcher("~other.*");
        assertFalse(matcher.match(bug));
    }

    @Test
    void matchWhenNoLocalVariableReturnsFalse() {
        BugInstance bugWithoutLocal = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        LocalMatcher matcher = new LocalMatcher("myVariable");
        assertFalse(matcher.match(bugWithoutLocal));
    }

    @Test
    void writeXMLNameOnly() throws IOException {
        LocalMatcher matcher = new LocalMatcher("myVariable");
        String xml = writeXMLAndGetStringOutput(matcher, false);
        assertTrue(xml.contains("name=\"myVariable\""));
        assertFalse(xml.contains("disabled"));
    }

    @Test
    void writeXMLWithDisabled() throws IOException {
        LocalMatcher matcher = new LocalMatcher("myVariable");
        String xml = writeXMLAndGetStringOutput(matcher, true);
        assertTrue(xml.contains("disabled=\"true\""));
    }

    @Test
    void toStringContainsName() {
        LocalMatcher matcher = new LocalMatcher("myVariable");
        assertTrue(matcher.toString().contains("myVariable"));
    }

    private String writeXMLAndGetStringOutput(LocalMatcher matcher, boolean disabled) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(outputStream);
        matcher.writeXML(xmlOutput, disabled);
        xmlOutput.finish();
        return outputStream.toString(StandardCharsets.UTF_8.name()).trim();
    }
}
