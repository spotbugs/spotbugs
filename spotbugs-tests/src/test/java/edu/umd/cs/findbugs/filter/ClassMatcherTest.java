/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

class ClassMatcherTest {

    private BugInstance bug;

    @BeforeEach
    void setUp() {
        bug = new BugInstance("NP_NULL_ON_SOME_PATH", 0);
        bug.addClass("com.example.MyClass");
    }

    @Test
    void exactMatchSucceeds() {
        ClassMatcher matcher = new ClassMatcher("com.example.MyClass");
        assertTrue(matcher.match(bug));
    }

    @Test
    void exactMatchFailsForDifferentClass() {
        ClassMatcher matcher = new ClassMatcher("com.example.OtherClass");
        assertFalse(matcher.match(bug));
    }

    @Test
    void regexMatchSucceeds() {
        ClassMatcher matcher = new ClassMatcher("~com\\.example\\..*");
        assertTrue(matcher.match(bug));
    }

    @Test
    void regexMatchFailsForNonMatchingPattern() {
        ClassMatcher matcher = new ClassMatcher("~com\\.other\\..*");
        assertFalse(matcher.match(bug));
    }

    @Test
    void writeXMLWithoutDisabled() throws IOException {
        ClassMatcher matcher = new ClassMatcher("com.example.MyClass");
        String xml = writeXMLAndGetStringOutput(matcher, false);
        assertEquals("<Class name=\"com.example.MyClass\"/>", xml);
    }

    @Test
    void writeXMLWithDisabled() throws IOException {
        ClassMatcher matcher = new ClassMatcher("com.example.MyClass");
        String xml = writeXMLAndGetStringOutput(matcher, true);
        assertEquals("<Class name=\"com.example.MyClass\" disabled=\"true\"/>", xml);
    }

    @Test
    void writeXMLWithRole() throws IOException {
        ClassMatcher matcher = new ClassMatcher("com.example.MyClass", "ROLE_TEST");
        String xml = writeXMLAndGetStringOutput(matcher, false);
        assertEquals("<Class name=\"com.example.MyClass\" role=\"ROLE_TEST\"/>", xml);
    }

    @Test
    void matchByRoleUsesCorrectAnnotation() {
        // bug has default primary class com.example.MyClass
        // add a second class annotation with a specific role
        ClassAnnotation otherClass = new ClassAnnotation("com.example.OtherClass");
        otherClass.setDescription("ROLE_SPECIAL");
        bug.add(otherClass);

        // match against the role annotation
        ClassMatcher matcherRole = new ClassMatcher("com.example.OtherClass", "ROLE_SPECIAL");
        assertTrue(matcherRole.match(bug));

        // matching a different name against the role should fail
        ClassMatcher matcherRoleOther = new ClassMatcher("com.example.MyClass", "ROLE_SPECIAL");
        assertFalse(matcherRoleOther.match(bug));
    }

    @Test
    void toStringContainsClassName() {
        ClassMatcher matcher = new ClassMatcher("com.example.MyClass");
        assertTrue(matcher.toString().contains("com.example.MyClass"));
    }

    private String writeXMLAndGetStringOutput(ClassMatcher matcher, boolean disabled) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(outputStream);
        matcher.writeXML(xmlOutput, disabled);
        xmlOutput.finish();
        return outputStream.toString(StandardCharsets.UTF_8.name()).trim();
    }
}
