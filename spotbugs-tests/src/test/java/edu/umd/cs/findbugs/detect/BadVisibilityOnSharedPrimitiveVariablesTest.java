/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Before;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class BadVisibilityOnSharedPrimitiveVariablesTest extends AbstractIntegrationTest {

    @Before
    public void setup() {
        SystemProperties.setProperty("ful.debug", "true");
    }

    @Test
    public void failurePath_fieldWithBadVisibility_whenOtherMethodHasSynchronizedBlock() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlockAndBadVisibilityOnField.class");
        assertSPVNumOfBugs(1);
        assertSPVBug("SynchronizedBlockAndBadVisibilityOnField", "shutdown", 36);
    }

    @Test
    public void failurePath_fieldWithBadVisibility_whenOtherMethodIsSynchronized() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedMethodAndBadVisibilityOnField.class");
        assertSPVNumOfBugs(1);
        assertSPVBug("SynchronizedMethodAndBadVisibilityOnField", "shutdown", 36);
    }

    @Test
    public void failurePath_fieldWithBadVisibility_whenClassExtendsThread() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityThread.class");
        assertSPVNumOfBugs(1);
        assertSPVBug("FieldWithBadVisibilityThread", "shutdown", 37);
    }

    @Test
    public void failurePath_fieldWithBadVisibility_whenClassImplementsRunnable() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityRunnable.class");
        assertSPVNumOfBugs(1);
        assertSPVBug("FieldWithBadVisibilityRunnable", "shutdown", 37);
    }

    @Test
    public void happyPath_atomicField() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/AtomicField.class");
        assertSPVNumOfBugs(0);
    }

    @Test
    public void happyPath_volatileField() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/VolatileField.class");
        assertSPVNumOfBugs(0);
    }

    @Test
    public void happyPath_synchronizedBlock() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlock.class");
        assertSPVNumOfBugs(0);
    }

    @Test
    public void happyPath_synchronizedMethod() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedMethod.class");
        assertSPVNumOfBugs(0);
    }

    private void assertSPVNumOfBugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("SPV_BAD_VISIBILITY_ON_SHARED_PRIMITIVE_VARIABLES")
                .build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertSPVBug(String className, String methodName, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("SPV_BAD_VISIBILITY_ON_SHARED_PRIMITIVE_VARIABLES")
                .inClass(className)
                .inMethod(methodName)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

}
