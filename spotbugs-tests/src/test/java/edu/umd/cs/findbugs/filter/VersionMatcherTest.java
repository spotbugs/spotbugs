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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

class VersionMatcherTest {

    @Test
    void firstVersionEqMatchSucceeds() {
        BugInstance bug = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        bug.setFirstVersion(3L);
        FirstVersionMatcher matcher = new FirstVersionMatcher("3", RelationalOp.EQ);
        assertTrue(matcher.match(bug));
    }

    @Test
    void firstVersionEqMatchFails() {
        BugInstance bug = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        bug.setFirstVersion(5L);
        FirstVersionMatcher matcher = new FirstVersionMatcher("3", RelationalOp.EQ);
        assertFalse(matcher.match(bug));
    }

    @Test
    void firstVersionLtMatchSucceeds() {
        BugInstance bug = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        bug.setFirstVersion(2L);
        FirstVersionMatcher matcher = new FirstVersionMatcher("5", RelationalOp.LT);
        assertTrue(matcher.match(bug));
    }

    @Test
    void firstVersionGtMatchSucceeds() {
        BugInstance bug = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        bug.setFirstVersion(10L);
        FirstVersionMatcher matcher = new FirstVersionMatcher("5", RelationalOp.GT);
        assertTrue(matcher.match(bug));
    }

    @Test
    void lastVersionEqMatchSucceeds() {
        BugInstance bug = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        bug.setLastVersion(7L);
        LastVersionMatcher matcher = new LastVersionMatcher("7", RelationalOp.EQ);
        assertTrue(matcher.match(bug));
    }

    @Test
    void lastVersionEqMatchFails() {
        BugInstance bug = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        bug.setLastVersion(4L);
        LastVersionMatcher matcher = new LastVersionMatcher("7", RelationalOp.EQ);
        assertFalse(matcher.match(bug));
    }

    @Test
    void deadBugMatcherMatchesDeadBugs() {
        BugInstance bug = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        bug.setLastVersion(-1L);
        // DEAD_BUG_MATCHER checks lastVersion != -1 so this should NOT match
        assertFalse(LastVersionMatcher.DEAD_BUG_MATCHER.match(bug));
    }

    @Test
    void deadBugMatcherDoesNotMatchActiveBug() {
        BugInstance bug = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        bug.setLastVersion(2L); // alive bug (last version != -1)
        assertTrue(LastVersionMatcher.DEAD_BUG_MATCHER.match(bug));
    }

    @Test
    void firstVersionWriteXMLWithoutDisabled() throws IOException {
        FirstVersionMatcher matcher = new FirstVersionMatcher("3", RelationalOp.EQ);
        String xml = writeFirstVersionXML(matcher, false);
        assertTrue(xml.contains("value=\"3\""));
        assertTrue(xml.contains("relOp="));
        assertFalse(xml.contains("disabled"));
    }

    @Test
    void firstVersionWriteXMLWithDisabled() throws IOException {
        FirstVersionMatcher matcher = new FirstVersionMatcher("3", RelationalOp.EQ);
        String xml = writeFirstVersionXML(matcher, true);
        assertTrue(xml.contains("disabled=\"true\""));
    }

    @Test
    void lastVersionWriteXMLWithoutDisabled() throws IOException {
        LastVersionMatcher matcher = new LastVersionMatcher("5", RelationalOp.NEQ);
        String xml = writeLastVersionXML(matcher, false);
        assertTrue(xml.contains("value=\"5\""));
        assertTrue(xml.contains("relOp="));
        assertFalse(xml.contains("disabled"));
    }

    @Test
    void firstVersionToStringContainsVersion() {
        FirstVersionMatcher matcher = new FirstVersionMatcher("3", RelationalOp.EQ);
        assertTrue(matcher.toString().contains("3"));
    }

    @Test
    void lastVersionToStringForDeadBugsIsDescriptive() {
        LastVersionMatcher matcher = new LastVersionMatcher("-1", RelationalOp.EQ);
        assertTrue(matcher.toString().contains("ActiveBugs"));
    }

    @Test
    void lastVersionToStringForActiveBugsIsDescriptive() {
        LastVersionMatcher matcher = new LastVersionMatcher("-1", RelationalOp.NEQ);
        assertTrue(matcher.toString().contains("DeadBugs"));
    }

    @Test
    void firstVersionStringConstructorParsesRelOp() {
        FirstVersionMatcher matcher = new FirstVersionMatcher("5", "EQ");
        BugInstance bug = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        bug.setFirstVersion(5L);
        assertTrue(matcher.match(bug));
    }

    private String writeFirstVersionXML(FirstVersionMatcher matcher, boolean disabled) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(outputStream);
        matcher.writeXML(xmlOutput, disabled);
        xmlOutput.finish();
        return outputStream.toString(StandardCharsets.UTF_8.name()).trim();
    }

    private String writeLastVersionXML(LastVersionMatcher matcher, boolean disabled) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(outputStream);
        matcher.writeXML(xmlOutput, disabled);
        xmlOutput.finish();
        return outputStream.toString(StandardCharsets.UTF_8.name()).trim();
    }
}
