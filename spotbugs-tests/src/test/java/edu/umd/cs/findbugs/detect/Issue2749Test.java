/*
 * SpotBugs - Find bugs in Java programs
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

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

class Issue2749Test extends AbstractIntegrationTest {
    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testAccessViaGettersAndSetters() {
        performAnalysis(
                "../java11/ghIssues/issue2749/WithMethodHandlesPrimitive.class",
                "../java11/ghIssues/issue2749/WithMethodHandlesObject.class",
                "../java11/ghIssues/issue2749/WithMethodHandlesObject$Value.class",
                "../java11/ghIssues/issue2749/WithVarHandle$Value.class",
                "../java11/ghIssues/issue2749/WithVarHandle.class");

        assertNoBugType("UUF_UNUSED_FIELD");
        assertNoBugType("UWF_UNWRITTEN_FIELD");
        assertNoBugType("URF_UNREAD_FIELD");
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testSetterNotInvoked() {
        performAnalysis(
                "../java11/ghIssues/issue2749/WithMethodHandlesObjectSetterNotInvoked.class",
                "../java11/ghIssues/issue2749/WithMethodHandlesObjectSetterNotInvoked$Value.class");

        assertNoBugType("UUF_UNUSED_FIELD");
        assertNoBugType("URF_UNREAD_FIELD");

        assertBugTypeCount("UWF_UNWRITTEN_FIELD", 1);
        assertBugAtFieldAtLine("UWF_UNWRITTEN_FIELD", "WithMethodHandlesObjectSetterNotInvoked", "reflectiveField", 31);
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testGetterNotInvoked() {
        performAnalysis(
                "../java11/ghIssues/issue2749/WithMethodHandlesObjectGetterNotInvoked.class",
                "../java11/ghIssues/issue2749/WithMethodHandlesObjectGetterNotInvoked$Value.class");

        assertNoBugType("UUF_UNUSED_FIELD");
        assertNoBugType("UWF_UNWRITTEN_FIELD");

        assertBugTypeCount("URF_UNREAD_FIELD", 1);
        assertBugAtFieldAtLine("URF_UNREAD_FIELD", "WithMethodHandlesObjectGetterNotInvoked", "reflectiveField", 31);
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testMethodHandlesNeverInvoked() {
        performAnalysis(
                "../java11/ghIssues/issue2749/WithMethodHandlesNeverInvoked.class",
                "../java11/ghIssues/issue2749/WithMethodHandlesNeverInvoked$Value.class");

        assertNoBugType("URF_UNREAD_FIELD");
        assertNoBugType("UWF_UNWRITTEN_FIELD");
        assertBugTypeCount("UUF_UNUSED_FIELD", 1);
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testVarHandleNeverInvoked() {
        performAnalysis(
                "../java11/ghIssues/issue2749/WithVarHandleNeverInvoked.class",
                "../java11/ghIssues/issue2749/WithVarHandleNeverInvoked$Value.class");

        assertNoBugType("URF_UNREAD_FIELD");
        assertNoBugType("UWF_UNWRITTEN_FIELD");
        assertBugTypeCount("UUF_UNUSED_FIELD", 1);
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testVarHandleNoWriting() {
        performAnalysis(
                "../java11/ghIssues/issue2749/WithVarHandleNoWriting.class",
                "../java11/ghIssues/issue2749/WithVarHandleNoWriting$Value.class");

        assertNoBugType("UUF_UNUSED_FIELD");
        assertNoBugType("URF_UNREAD_FIELD");

        assertBugTypeCount("UWF_UNWRITTEN_FIELD", 1);
        assertBugAtFieldAtLine("UWF_UNWRITTEN_FIELD", "WithVarHandleNoWriting", "value", 27);
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testVarHandleNoReading() {
        performAnalysis(
                "../java11/ghIssues/issue2749/WithVarHandleNoReading.class",
                "../java11/ghIssues/issue2749/WithVarHandleNoReading$Value.class");

        assertNoBugType("UUF_UNUSED_FIELD");
        assertNoBugType("UWF_UNWRITTEN_FIELD");

        assertBugTypeCount("URF_UNREAD_FIELD", 1);
        assertBugAtFieldAtLine("URF_UNREAD_FIELD", "WithVarHandleNoReading", "value", 28);
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testVarHandleReadingAndWritingInSingleInvocation() {
        performAnalysis(
                "../java11/ghIssues/issue2749/WithVarHandleReadingAndWritingInOneInvocation.class",
                "../java11/ghIssues/issue2749/WithVarHandleReadingAndWritingInOneInvocation$Value.class");

        assertNoBugType("UUF_UNUSED_FIELD");
        assertNoBugType("UWF_UNWRITTEN_FIELD");
        assertNoBugType("URF_UNREAD_FIELD");
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testAtomicUpdaterNoWriting() {
        performAnalysis(
                "../java11/ghIssues/issue2749/WithAtomicUpdaterSetterNotInvoked.class",
                "../java11/ghIssues/issue2749/WithAtomicUpdaterSetterNotInvoked$Value.class");

        assertNoBugType("UUF_UNUSED_FIELD");
        assertNoBugType("URF_UNREAD_FIELD");

        assertBugTypeCount("UWF_UNWRITTEN_FIELD", 1);
        assertBugAtFieldAtLine("UWF_UNWRITTEN_FIELD", "WithAtomicUpdaterSetterNotInvoked", "reflectiveField", 21);
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testAtomicUpdaterNoReading() {
        performAnalysis(
                "../java11/ghIssues/issue2749/WithAtomicUpdaterGetterNotInvoked.class",
                "../java11/ghIssues/issue2749/WithAtomicUpdaterGetterNotInvoked$Value.class");

        assertNoBugType("UUF_UNUSED_FIELD");
        assertNoBugType("UWF_UNWRITTEN_FIELD");

        assertBugTypeCount("URF_UNREAD_FIELD", 1);
        assertBugAtFieldAtLine("URF_UNREAD_FIELD", "WithAtomicUpdaterGetterNotInvoked", "reflectiveField", 21);
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testAtomicUpdaterReadingAndWritingInSingleInvocation() {
        performAnalysis(
                "../java11/ghIssues/issue2749/WithAtomicUpdaterReadingAndWritingInOneInvocation.class",
                "../java11/ghIssues/issue2749/WithAtomicUpdaterReadingAndWritingInOneInvocation$Value.class");

        assertNoBugType("UUF_UNUSED_FIELD");
        assertNoBugType("URF_UNREAD_FIELD");
        assertNoBugType("UWF_UNWRITTEN_FIELD");
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testAtomicUpdaters() {
        performAnalysis(
                "../java11/ghIssues/issue2749/WithAtomicUpdaters.class",
                "../java11/ghIssues/issue2749/WithAtomicUpdaters$Value.class");

        assertNoBugType("UUF_UNUSED_FIELD");
        assertNoBugType("URF_UNREAD_FIELD");
        assertNoBugType("UWF_UNWRITTEN_FIELD");
    }
}
