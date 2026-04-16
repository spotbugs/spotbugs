/*
 * FindBugs - Find Bugs in Java programs
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.TypeAnnotation;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

class TypeMatcherTest {

    private BugInstance bug;

    @BeforeEach
    void setUp() {
        bug = new BugInstance("BC_IMPOSSIBLE_CAST", Priorities.NORMAL_PRIORITY);
        TypeAnnotation typeAnnotation = new TypeAnnotation("Ljava/lang/String;");
        bug.add(typeAnnotation);
    }

    @Test
    void exactDescriptorMatchSucceeds() {
        TypeMatcher matcher = new TypeMatcher("Ljava/lang/String;", null, null);
        assertTrue(matcher.match(bug));
    }

    @Test
    void exactDescriptorMatchFails() {
        TypeMatcher matcher = new TypeMatcher("Ljava/lang/Integer;", null, null);
        assertFalse(matcher.match(bug));
    }

    @Test
    void regexDescriptorMatchSucceeds() {
        TypeMatcher matcher = new TypeMatcher("~Ljava/lang/.*", null, null);
        assertTrue(matcher.match(bug));
    }

    @Test
    void regexDescriptorMatchFails() {
        TypeMatcher matcher = new TypeMatcher("~Ljava/util/.*", null, null);
        assertFalse(matcher.match(bug));
    }

    @Test
    void matchWhenNoPrimaryTypeReturnsFalse() {
        BugInstance bugWithoutType = new BugInstance("BC_IMPOSSIBLE_CAST", Priorities.NORMAL_PRIORITY);
        TypeMatcher matcher = new TypeMatcher("Ljava/lang/String;", null, null);
        assertFalse(matcher.match(bugWithoutType));
    }

    @Test
    void matchByRoleUsesCorrectAnnotation() {
        TypeAnnotation roleType = new TypeAnnotation("Ljava/lang/Integer;", "TYPE_FOUND");
        bug.add(roleType);

        TypeMatcher matcherRole = new TypeMatcher("Ljava/lang/Integer;", "TYPE_FOUND", null);
        assertTrue(matcherRole.match(bug));

        // primary type is Ljava/lang/String; - shouldn't match Integer with default role
        TypeMatcher matcherDefault = new TypeMatcher("Ljava/lang/Integer;", null, null);
        assertFalse(matcherDefault.match(bug));
    }

    @Test
    void matchWithTypeParametersSucceeds() {
        TypeAnnotation typeWithParams = new TypeAnnotation("Ljava/util/List;");
        typeWithParams.setTypeParameters("<Ljava/lang/String;>");
        BugInstance bugWithParams = new BugInstance("BC_IMPOSSIBLE_CAST", Priorities.NORMAL_PRIORITY);
        bugWithParams.add(typeWithParams);

        TypeMatcher matcher = new TypeMatcher("Ljava/util/List;", null, "<Ljava/lang/String;>");
        assertTrue(matcher.match(bugWithParams));
    }

    @Test
    void matchWithWrongTypeParametersFails() {
        TypeAnnotation typeWithParams = new TypeAnnotation("Ljava/util/List;");
        typeWithParams.setTypeParameters("<Ljava/lang/String;>");
        BugInstance bugWithParams = new BugInstance("BC_IMPOSSIBLE_CAST", Priorities.NORMAL_PRIORITY);
        bugWithParams.add(typeWithParams);

        TypeMatcher matcher = new TypeMatcher("Ljava/util/List;", null, "<Ljava/lang/Integer;>");
        assertFalse(matcher.match(bugWithParams));
    }

    @Test
    void writeXMLWithDescriptor() throws IOException {
        TypeMatcher matcher = new TypeMatcher("Ljava/lang/String;", null, null);
        String xml = writeXMLAndGetStringOutput(matcher, false);
        assertTrue(xml.contains("descriptor=\"Ljava/lang/String;\""));
        assertFalse(xml.contains("disabled"));
    }

    @Test
    void writeXMLWithDisabled() throws IOException {
        TypeMatcher matcher = new TypeMatcher("Ljava/lang/String;", null, null);
        String xml = writeXMLAndGetStringOutput(matcher, true);
        assertTrue(xml.contains("disabled=\"true\""));
    }

    @Test
    void writeXMLWithTypeParameters() throws IOException {
        TypeMatcher matcher = new TypeMatcher("Ljava/util/List;", null, "<Ljava/lang/String;>");
        String xml = writeXMLAndGetStringOutput(matcher, false);
        assertTrue(xml.contains("typeParameters="));
    }

    @Test
    void toStringContainsDescriptor() {
        TypeMatcher matcher = new TypeMatcher("Ljava/lang/String;", null, null);
        assertTrue(matcher.toString().contains("Ljava/lang/String;"));
    }

    private String writeXMLAndGetStringOutput(TypeMatcher matcher, boolean disabled) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(outputStream);
        matcher.writeXML(xmlOutput, disabled);
        xmlOutput.finish();
        return outputStream.toString(StandardCharsets.UTF_8.name()).trim();
    }
}
