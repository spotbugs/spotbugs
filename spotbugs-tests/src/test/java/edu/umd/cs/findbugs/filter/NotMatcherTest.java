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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

class NotMatcherTest {

    private final BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 0);

    @Test
    void invertsResultsFromWrappedMatcher_doesntMatchWhenWrappedDoesMatch() {
        Matcher wrappedMatcher = new TestMatcher(true);
        NotMatcher notMatcher = new NotMatcher();
        notMatcher.addChild(wrappedMatcher);

        assertFalse(notMatcher.match(bug));
    }

    @Test
    void invertsResultsFromWrappedMatcher_doesMatchWhenWrappedDoesnt() {
        Matcher wrappedMatcher = new TestMatcher(false);
        NotMatcher notMatcher = new NotMatcher();
        notMatcher.addChild(wrappedMatcher);

        assertTrue(notMatcher.match(bug));
    }

    @Test
    void writeXMLOutputAddsNotTagsAroundWrappedMatchersOutput() throws Exception {
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
    void canReturnChildMatcher() {
        Matcher wrappedMatcher = new TestMatcher(true);
        NotMatcher notMatcher = new NotMatcher();
        notMatcher.addChild(wrappedMatcher);

        assertSame(wrappedMatcher, notMatcher.originalMatcher(), "Should return child matcher.");
    }

    @Test
    void throwsExceptionWhenTryingToGetNonExistentChildMatcher() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            new NotMatcher().originalMatcher();
        });
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
