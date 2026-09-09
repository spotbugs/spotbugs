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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

class BugMatcherTest {

    private BugInstance bug;

    @BeforeEach
    void setUp() {
        // NP_NULL_ON_SOME_PATH: abbrev=NP, category=CORRECTNESS
        bug = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
    }

    @Test
    void matchByPatternSucceeds() {
        BugMatcher matcher = new BugMatcher("", "NP_NULL_ON_SOME_PATH", "");
        assertTrue(matcher.match(bug));
    }

    @Test
    void matchByPatternFailsForDifferentPattern() {
        BugMatcher matcher = new BugMatcher("", "UUF_UNUSED_FIELD", "");
        assertFalse(matcher.match(bug));
    }

    @Test
    void matchByCodeSucceeds() {
        // NP_NULL_ON_SOME_PATH has abbrev "NP"
        BugMatcher matcher = new BugMatcher("NP", "", "");
        assertTrue(matcher.match(bug));
    }

    @Test
    void matchByCodeFailsForWrongCode() {
        BugMatcher matcher = new BugMatcher("UUF", "", "");
        assertFalse(matcher.match(bug));
    }

    @Test
    void matchByCategorySucceeds() {
        // NP_NULL_ON_SOME_PATH belongs to CORRECTNESS category
        BugMatcher matcher = new BugMatcher("", "", "CORRECTNESS");
        assertTrue(matcher.match(bug));
    }

    @Test
    void matchByCategoryFailsForWrongCategory() {
        BugMatcher matcher = new BugMatcher("", "", "PERFORMANCE");
        assertFalse(matcher.match(bug));
    }

    @Test
    void matchByMultipleCodesSucceedsOnAny() {
        BugMatcher matcher = new BugMatcher("UUF,NP", "", "");
        assertTrue(matcher.match(bug));
    }

    @Test
    void matchByMultiplePatternsSucceedsOnAny() {
        BugMatcher matcher = new BugMatcher("", "UUF_UNUSED_FIELD,NP_NULL_ON_SOME_PATH", "");
        assertTrue(matcher.match(bug));
    }

    @Test
    void emptyMatcherDoesNotMatch() {
        BugMatcher matcher = new BugMatcher("", "", "");
        assertFalse(matcher.match(bug));
    }

    @Test
    void writeXMLWithPattern() throws IOException {
        BugMatcher matcher = new BugMatcher("", "NP_NULL_ON_SOME_PATH", "");
        String xml = writeXMLAndGetStringOutput(matcher, false);
        assertTrue(xml.contains("pattern=\"NP_NULL_ON_SOME_PATH\""));
        assertFalse(xml.contains("disabled"));
    }

    @Test
    void writeXMLWithDisabled() throws IOException {
        BugMatcher matcher = new BugMatcher("NP", "", "");
        String xml = writeXMLAndGetStringOutput(matcher, true);
        assertTrue(xml.contains("disabled=\"true\""));
        assertTrue(xml.contains("code=\"NP\""));
    }

    @Test
    void writeXMLWithCategory() throws IOException {
        BugMatcher matcher = new BugMatcher("", "", "CORRECTNESS");
        String xml = writeXMLAndGetStringOutput(matcher, false);
        assertTrue(xml.contains("category=\"CORRECTNESS\""));
    }

    @Test
    void equalityHoldsForSameMatcher() {
        BugMatcher m1 = new BugMatcher("NP", "NP_NULL_ON_SOME_PATH", "CORRECTNESS");
        BugMatcher m2 = new BugMatcher("NP", "NP_NULL_ON_SOME_PATH", "CORRECTNESS");
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void equalityFailsForDifferentMatcher() {
        BugMatcher m1 = new BugMatcher("NP", "", "");
        BugMatcher m2 = new BugMatcher("UUF", "", "");
        assertNotEquals(m1, m2);
    }

    @Test
    void toStringContainsMatchingInfo() {
        BugMatcher matcher = new BugMatcher("NP", "NP_NULL_ON_SOME_PATH", "CORRECTNESS");
        String str = matcher.toString();
        assertTrue(str.contains("NP"));
    }

    private String writeXMLAndGetStringOutput(BugMatcher matcher, boolean disabled) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(outputStream);
        matcher.writeXML(xmlOutput, disabled);
        xmlOutput.finish();
        return outputStream.toString(StandardCharsets.UTF_8.name()).trim();
    }
}
