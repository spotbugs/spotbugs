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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.containsString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

public class NotMatcherTest {

    private final BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 0);

    @Test
    public void invertsResultsFromWrappedMatcher_doesntMatchWhenWrappedDoesMatch() throws Exception {
        Matcher wrappedMatcher = new TestMatcher(true);
        NotMatcher notMatcher = new NotMatcher();
        notMatcher.addChild(wrappedMatcher);

        assertFalse(notMatcher.match(bug));
    }

    @Test
    public void invertsResultsFromWrappedMatcher_doesMatchWhenWrappedDoesnt() throws Exception {
        Matcher wrappedMatcher = new TestMatcher(false);
        NotMatcher notMatcher = new NotMatcher();
        notMatcher.addChild(wrappedMatcher);

        assertTrue(notMatcher.match(bug));
    }

    @Test
    public void writeXMLOutputAddsNotTagsAroundWrappedMatchersOutput() throws Exception {
        Matcher wrappedMatcher = new TestMatcher(true);
        NotMatcher notMatcher = new NotMatcher();
        notMatcher.addChild(wrappedMatcher);

        String xmlOutputCreated = writeXMLAndGetStringOutput(notMatcher);

        assertTrue(containsString("<Not>").matches(xmlOutputCreated));
        assertTrue(containsString("<TestMatch>").matches(xmlOutputCreated));
        assertTrue(containsString("</TestMatch>").matches(xmlOutputCreated));
        assertTrue(containsString("</Not>").matches(xmlOutputCreated));
    }

    @Test
    public void canReturnChildMatcher() {
        Matcher wrappedMatcher = new TestMatcher(true);
        NotMatcher notMatcher = new NotMatcher();
        notMatcher.addChild(wrappedMatcher);

        assertSame("Should return child matcher.", wrappedMatcher, notMatcher.originalMatcher());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionWhenTryingToGetNonExistentChildMatcher() {
        new NotMatcher().originalMatcher();
    }

    private String writeXMLAndGetStringOutput(NotMatcher notMatcher) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(outputStream);

        notMatcher.writeXML(xmlOutput, false);
        xmlOutput.finish();

        return outputStream.toString(StandardCharsets.UTF_8.name());
    }

    private static class TestMatcher implements Matcher {

        private final boolean alwaysMatches;

        public TestMatcher(boolean alwaysMatches) {
            this.alwaysMatches = alwaysMatches;
        }

        @Override
        public boolean match(BugInstance bugInstance) {
            return alwaysMatches;
        }

        @Override
        public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
            xmlOutput.openTag("TestMatch");
            xmlOutput.closeTag("TestMatch");
        }
    }
}
