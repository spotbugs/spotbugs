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
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

class MethodMatcherTest {

    private BugInstance bug;

    @BeforeEach
    void setUp() {
        bug = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        // Add a primary method: class=com.example.MyClass, name=doWork, sig=(Ljava/lang/String;)V
        MethodAnnotation method = new MethodAnnotation("com.example.MyClass", "doWork", "(Ljava/lang/String;)V", false);
        bug.add(method);
    }

    @Test
    void exactMethodNameMatchSucceeds() {
        MethodMatcher matcher = new MethodMatcher("doWork");
        assertTrue(matcher.match(bug));
    }

    @Test
    void exactMethodNameMatchFails() {
        MethodMatcher matcher = new MethodMatcher("otherMethod");
        assertFalse(matcher.match(bug));
    }

    @Test
    void regexMethodNameMatchSucceeds() {
        MethodMatcher matcher = new MethodMatcher("~do.*");
        assertTrue(matcher.match(bug));
    }

    @Test
    void regexMethodNameMatchFails() {
        MethodMatcher matcher = new MethodMatcher("~get.*");
        assertFalse(matcher.match(bug));
    }

    @Test
    void matchWithParamsAndReturnsSucceeds() {
        // params = "java.lang.String", returns = "void"
        MethodMatcher matcher = new MethodMatcher("doWork", "java.lang.String", "void");
        assertTrue(matcher.match(bug));
    }

    @Test
    void matchWithWrongParamsFails() {
        MethodMatcher matcher = new MethodMatcher("doWork", "int", "void");
        assertFalse(matcher.match(bug));
    }

    @Test
    void matchWithWrongReturnTypeFails() {
        MethodMatcher matcher = new MethodMatcher("doWork", "java.lang.String", "int");
        assertFalse(matcher.match(bug));
    }

    @Test
    void matchWhenNoPrimaryMethodReturnsFalse() {
        BugInstance bugWithoutMethod = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        MethodMatcher matcher = new MethodMatcher("doWork");
        assertFalse(matcher.match(bugWithoutMethod));
    }

    @Test
    void matchByRoleUsesCorrectAnnotation() {
        MethodAnnotation roleMethod = new MethodAnnotation("com.example.MyClass", "specialMethod", "()V", false);
        roleMethod.setDescription("ROLE_SPECIAL");
        bug.add(roleMethod);

        MethodMatcher matcherRole = new MethodMatcher("specialMethod", "ROLE_SPECIAL");
        assertTrue(matcherRole.match(bug));

        MethodMatcher matcherDefaultRole = new MethodMatcher("specialMethod");
        // primary method is doWork, not specialMethod
        assertFalse(matcherDefaultRole.match(bug));
    }

    @Test
    void writeXMLNameOnly() throws IOException {
        MethodMatcher matcher = new MethodMatcher("doWork");
        String xml = writeXMLAndGetStringOutput(matcher, false);
        assertTrue(xml.contains("name=\"doWork\""));
        assertFalse(xml.contains("disabled"));
    }

    @Test
    void writeXMLWithParamsAndReturns() throws IOException {
        MethodMatcher matcher = new MethodMatcher("doWork", "java.lang.String", "void");
        String xml = writeXMLAndGetStringOutput(matcher, false);
        assertTrue(xml.contains("name=\"doWork\""));
        assertTrue(xml.contains("params="));
        assertTrue(xml.contains("returns="));
    }

    @Test
    void writeXMLWithDisabled() throws IOException {
        MethodMatcher matcher = new MethodMatcher("doWork");
        String xml = writeXMLAndGetStringOutput(matcher, true);
        assertTrue(xml.contains("disabled=\"true\""));
    }

    @Test
    void toStringContainsMethodName() {
        MethodMatcher matcher = new MethodMatcher("doWork");
        assertTrue(matcher.toString().contains("doWork"));
    }

    private String writeXMLAndGetStringOutput(MethodMatcher matcher, boolean disabled) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(outputStream);
        matcher.writeXML(xmlOutput, disabled);
        xmlOutput.finish();
        return outputStream.toString(StandardCharsets.UTF_8.name()).trim();
    }
}
