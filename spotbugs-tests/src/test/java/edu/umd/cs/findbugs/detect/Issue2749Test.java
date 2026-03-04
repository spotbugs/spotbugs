/*
 * Contributions to SpotBugs
 * Copyright (C) 2026 PANTHEON.tech, s.r.o.
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
package edu.umd.cs.findbugs.detect;

import static org.hamcrest.MatcherAssert.assertThat;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.stream.Streams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

class Issue2749Test extends AbstractIntegrationTest {
    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testIssue() {
        performAnalysis(
                "../java11/ghIssues/Issue2749$WithMethodHandlesPrimitive.class",
                "../java11/ghIssues/Issue2749$WithMethodHandlesObject.class",
                "../java11/ghIssues/Issue2749$WithMethodHandlesObject$Value.class",
                "../java11/ghIssues/Issue2749$WithVarHandle$Value.class",
                "../java11/ghIssues/Issue2749$WithVarHandle.class",
                "../java11/ghIssues/Issue2749.class");
        BugCollection bugCollection = getBugCollection();
        assertBugNotPresent(bugCollection, "UUF_UNUSED_FIELD");
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testSetterNotInvoked() {
        performAnalysis(
                "../java11/ghIssues/Issue2749$WithMethodHandlesObjectSetterNotInvoked.class",
                "../java11/ghIssues/Issue2749$WithMethodHandlesObjectSetterNotInvoked$DummyValue.class",
                "../java11/ghIssues/Issue2749.class");
        BugCollection bugCollection = getBugCollection();
        assertBugNotPresent(bugCollection, "UUF_UNUSED_FIELD");
        assertBugPresent(bugCollection, "UWF_UNWRITTEN_FIELD");
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testGetterNotInvoked() {
        performAnalysis(
                "../java11/ghIssues/Issue2749$WithMethodHandlesObjectGetterNotInvoked.class",
                "../java11/ghIssues/Issue2749$WithMethodHandlesObjectGetterNotInvoked$DummyValue.class",
                "../java11/ghIssues/Issue2749.class");
        BugCollection bugCollection = getBugCollection();
        assertBugNotPresent(bugCollection, "UUF_UNUSED_FIELD");
        assertBugPresent(bugCollection, "URF_UNREAD_FIELD");
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testMethodHandlesNeverInvoked() {
        performAnalysis(
                "../java11/ghIssues/Issue2749$WithMethodHandlesNeverInvoked.class",
                "../java11/ghIssues/Issue2749$WithMethodHandlesNeverInvoked$DummyValue.class",
                "../java11/ghIssues/Issue2749.class");
        BugCollection bugCollection = getBugCollection();
        assertBugPresent(bugCollection, "UUF_UNUSED_FIELD");
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testVarHandleNeverInvoked() {
        performAnalysis(
                "../java11/ghIssues/Issue2749$WithVarHandleNeverInvoked.class",
                "../java11/ghIssues/Issue2749$WithVarHandleNeverInvoked$Value.class",
                "../java11/ghIssues/Issue2749.class");
        BugCollection bugCollection = getBugCollection();
        assertBugPresent(bugCollection, "UUF_UNUSED_FIELD");
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testVarHandleNoWriting() {
        performAnalysis(
                "../java11/ghIssues/Issue2749$WithVarHandleNoWriting.class",
                "../java11/ghIssues/Issue2749$WithVarHandleNoWriting$Value.class",
                "../java11/ghIssues/Issue2749.class");
        BugCollection bugCollection = getBugCollection();
        assertBugNotPresent(bugCollection, "UUF_UNUSED_FIELD");
        assertBugPresent(bugCollection, "UWF_UNWRITTEN_FIELD");
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testVarHandleNoReading() {
        performAnalysis(
                "../java11/ghIssues/Issue2749$WithVarHandleNoReading.class",
                "../java11/ghIssues/Issue2749$WithVarHandleNoReading$Value.class",
                "../java11/ghIssues/Issue2749.class");
        BugCollection bugCollection = getBugCollection();
        assertBugNotPresent(bugCollection, "UUF_UNUSED_FIELD");
        assertBugPresent(bugCollection, "URF_UNREAD_FIELD");
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testVarHandleReadingAndWritingInSingleInvocation() {
        performAnalysis(
                "../java11/ghIssues/Issue2749$WithVarHandleReadingAndWritingInOneInvocation.class",
                "../java11/ghIssues/Issue2749$WithVarHandleReadingAndWritingInOneInvocation$Value.class",
                "../java11/ghIssues/Issue2749.class");
        BugCollection bugCollection = getBugCollection();
        assertBugNotPresent(bugCollection, "UUF_UNUSED_FIELD");
        assertBugNotPresent(bugCollection, "UWF_UNWRITTEN_FIELD");
        assertBugNotPresent(bugCollection, "URF_UNREAD_FIELD");
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testAtomicUpdaterNoWriting() {
        performAnalysis(
                "../java11/ghIssues/Issue2749$WithAtomicUpdaterSetterNotInvoked.class",
                "../java11/ghIssues/Issue2749$WithAtomicUpdaterSetterNotInvoked$DummyValue.class",
                "../java11/ghIssues/Issue2749.class");
        BugCollection bugCollection = getBugCollection();
        assertBugNotPresent(bugCollection, "UUF_UNUSED_FIELD");
        assertBugPresent(bugCollection, "UWF_UNWRITTEN_FIELD");
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testAtomicUpdaterNoReading() {
        performAnalysis(
                "../java11/ghIssues/Issue2749$WithAtomicUpdaterGetterNotInvoked.class",
                "../java11/ghIssues/Issue2749$WithAtomicUpdaterGetterNotInvoked$DummyValue.class",
                "../java11/ghIssues/Issue2749.class");
        BugCollection bugCollection = getBugCollection();
        assertBugNotPresent(bugCollection, "UUF_UNUSED_FIELD");
        assertBugPresent(bugCollection, "URF_UNREAD_FIELD");
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testAtomicUpdaterReadingAndWritingInSingleInvocation() {
        performAnalysis(
                "../java11/ghIssues/Issue2749$WithAtomicUpdaterReadingAndWritingInOneInvocation.class",
                "../java11/ghIssues/Issue2749$WithAtomicUpdaterReadingAndWritingInOneInvocation$DummyValue.class",
                "../java11/ghIssues/Issue2749.class");
        BugCollection bugCollection = getBugCollection();
        assertBugNotPresent(bugCollection, "UUF_UNUSED_FIELD");
        assertBugNotPresent(bugCollection, "URF_UNREAD_FIELD");
        assertBugNotPresent(bugCollection, "UWF_UNWRITTEN_FIELD");
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testAtomicUpdaters() {
        performAnalysis(
                "../java11/ghIssues/Issue2749$WithAtomicUpdaters.class",
                "../java11/ghIssues/Issue2749$WithAtomicUpdaters$DummyValue.class",
                "../java11/ghIssues/Issue2749.class");
        BugCollection bugCollection = getBugCollection();
        assertBugNotPresent(bugCollection, "UUF_UNUSED_FIELD");
        assertBugNotPresent(bugCollection, "URF_UNREAD_FIELD");
        assertBugNotPresent(bugCollection, "UWF_UNWRITTEN_FIELD");
    }

    private void assertBugPresent(final BugCollection bugCollection, final String bugName) {
        final Set<BugInstance> unusedFields = Streams.of(bugCollection)
                .filter(bug -> bug.getType().equals(bugName))
                .collect(Collectors.toSet());
        assertThat("There should be more " + bugName, !unusedFields.isEmpty());
    }

    private void assertBugNotPresent(final BugCollection bugCollection, final String bugName) {
        final Set<BugInstance> unusedFields = Streams.of(bugCollection)
                .filter(bug -> bug.getType().equals(bugName))
                .collect(Collectors.toSet());
        assertThat("There should be no more " + bugName, unusedFields.isEmpty());
    }
}
