/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
 *
 *  Author: Graham Allan
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

import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

public class NotMatcherTest extends TestCase {

    private final BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 0);

    public void testInvertsResultsFromWrappedMatcher_doesntMatchWhenWrappedDoesMatch() throws Exception {
        Matcher wrappedMatcher = new TestMatcher(true);
        NotMatcher notMatcher = new NotMatcher();
        notMatcher.addChild(wrappedMatcher);

        assertFalse(notMatcher.match(bug));
    }

    public void testInvertsResultsFromWrappedMatcher_doesMatchWhenWrappedDoesnt() throws Exception {
        Matcher wrappedMatcher = new TestMatcher(false);
        NotMatcher notMatcher = new NotMatcher();
        notMatcher.addChild(wrappedMatcher);

        assertTrue(notMatcher.match(bug));
    }

    public void testWriteXMLOutputAddsNotTagsAroundWrappedMatchersOutput() throws Exception {
        Matcher wrappedMatcher = new TestMatcher(true);
        NotMatcher notMatcher = new NotMatcher();
        notMatcher.addChild(wrappedMatcher);

        String xmlOutputCreated = writeXMLAndGetStringOutput(notMatcher);

        assertTrue(containsString("<Not>").matches(xmlOutputCreated));
        assertTrue(containsString("<TestMatch>").matches(xmlOutputCreated));
        assertTrue(containsString("</TestMatch>").matches(xmlOutputCreated));
        assertTrue(containsString("</Not>").matches(xmlOutputCreated));
    }

    private String writeXMLAndGetStringOutput(NotMatcher notMatcher) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(outputStream);

        notMatcher.writeXML(xmlOutput, false);
        xmlOutput.finish();

        String xmlOutputCreated = outputStream.toString();
        return xmlOutputCreated;
    }


    private static class TestMatcher implements Matcher {

        private final boolean alwaysMatches;

        public TestMatcher(boolean alwaysMatches) {
            this.alwaysMatches = alwaysMatches;
        }

        public boolean match(BugInstance bugInstance) {
            return alwaysMatches;
        }

        public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
            xmlOutput.openTag("TestMatch");
            xmlOutput.closeTag("TestMatch");
        }

    }
}
