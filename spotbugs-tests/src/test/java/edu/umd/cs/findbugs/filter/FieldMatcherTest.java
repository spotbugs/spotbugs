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
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

class FieldMatcherTest {

    private BugInstance bug;

    @BeforeEach
    void setUp() {
        bug = new BugInstance("UUF_UNUSED_FIELD", Priorities.NORMAL_PRIORITY);
        // Add a primary field: class=com.example.MyClass, name=myField, sig=Ljava/lang/String;
        FieldAnnotation field = new FieldAnnotation("com.example.MyClass", "myField", "Ljava/lang/String;", false);
        bug.add(field);
    }

    @Test
    void exactFieldNameMatchSucceeds() {
        FieldMatcher matcher = new FieldMatcher("myField");
        assertTrue(matcher.match(bug));
    }

    @Test
    void exactFieldNameMatchFails() {
        FieldMatcher matcher = new FieldMatcher("otherField");
        assertFalse(matcher.match(bug));
    }

    @Test
    void regexFieldNameMatchSucceeds() {
        FieldMatcher matcher = new FieldMatcher("~my.*");
        assertTrue(matcher.match(bug));
    }

    @Test
    void regexFieldNameMatchFails() {
        FieldMatcher matcher = new FieldMatcher("~other.*");
        assertFalse(matcher.match(bug));
    }

    @Test
    void matchWhenNoPrimaryFieldReturnsFalse() {
        BugInstance bugWithoutField = new BugInstance("UUF_UNUSED_FIELD", Priorities.NORMAL_PRIORITY);
        FieldMatcher matcher = new FieldMatcher("myField");
        assertFalse(matcher.match(bugWithoutField));
    }

    @Test
    void matchByRoleUsesCorrectAnnotation() {
        FieldAnnotation roleField = new FieldAnnotation("com.example.MyClass", "specialField", "I", false);
        roleField.setDescription("ROLE_SPECIAL");
        bug.add(roleField);

        FieldMatcher matcherRole = new FieldMatcher("specialField", null, "ROLE_SPECIAL");
        assertTrue(matcherRole.match(bug));

        // match default role - primary field is myField
        FieldMatcher matcherDefault = new FieldMatcher("specialField");
        assertFalse(matcherDefault.match(bug));
    }

    @Test
    void writeXMLNameOnly() throws IOException {
        FieldMatcher matcher = new FieldMatcher("myField");
        String xml = writeXMLAndGetStringOutput(matcher, false);
        assertTrue(xml.contains("name=\"myField\""));
        assertFalse(xml.contains("disabled"));
    }

    @Test
    void writeXMLWithDisabled() throws IOException {
        FieldMatcher matcher = new FieldMatcher("myField");
        String xml = writeXMLAndGetStringOutput(matcher, true);
        assertTrue(xml.contains("disabled=\"true\""));
    }

    private String writeXMLAndGetStringOutput(FieldMatcher matcher, boolean disabled) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(outputStream);
        matcher.writeXML(xmlOutput, disabled);
        xmlOutput.finish();
        return outputStream.toString(StandardCharsets.UTF_8.name()).trim();
    }
}
