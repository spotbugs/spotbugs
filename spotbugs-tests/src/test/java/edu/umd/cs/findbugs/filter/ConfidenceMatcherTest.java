/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

class ConfidenceMatcherTest {

    @Test
    void matchesWhenConfidenceEquals() {
        BugInstance bug = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        ConfidenceMatcher matcher = new ConfidenceMatcher(String.valueOf(Priorities.NORMAL_PRIORITY));
        assertTrue(matcher.match(bug));
    }

    @Test
    void doesNotMatchWhenConfidenceDiffers() {
        BugInstance bug = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        ConfidenceMatcher matcher = new ConfidenceMatcher(String.valueOf(Priorities.HIGH_PRIORITY));
        assertFalse(matcher.match(bug));
    }

    @Test
    void equalityHoldsForSameConfidence() {
        ConfidenceMatcher m1 = new ConfidenceMatcher("2");
        ConfidenceMatcher m2 = new ConfidenceMatcher("2");
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void equalityFailsForDifferentConfidence() {
        ConfidenceMatcher m1 = new ConfidenceMatcher("1");
        ConfidenceMatcher m2 = new ConfidenceMatcher("2");
        assertNotEquals(m1, m2);
    }

    @Test
    void writeXMLWithoutDisabled() throws IOException {
        ConfidenceMatcher matcher = new ConfidenceMatcher("2");
        String xml = writeXMLAndGetStringOutput(matcher, false);
        assertTrue(xml.contains("value=\"2\""));
        assertFalse(xml.contains("disabled"));
    }

    @Test
    void writeXMLWithDisabled() throws IOException {
        ConfidenceMatcher matcher = new ConfidenceMatcher("2");
        String xml = writeXMLAndGetStringOutput(matcher, true);
        assertTrue(xml.contains("disabled=\"true\""));
    }

    @Test
    void toStringContainsConfidence() {
        ConfidenceMatcher matcher = new ConfidenceMatcher("3");
        assertTrue(matcher.toString().contains("3"));
    }

    private String writeXMLAndGetStringOutput(ConfidenceMatcher matcher, boolean disabled) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(outputStream);
        matcher.writeXML(xmlOutput, disabled);
        xmlOutput.finish();
        return outputStream.toString(StandardCharsets.UTF_8.name()).trim();
    }
}
