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

class BadVisibilityOnSharedPrimitiveVariablesTest extends AbstractIntegrationTest {
    private static final String BUG_TYPE = "SPV_BAD_VISIBILITY_ON_SHARED_PRIMITIVE_VARIABLES";

    @Test
    void failurePath_fieldWithBadVisibility_whenOtherMethodHasSynchronizedBlock() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlockAndBadVisibilityOnField.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "SynchronizedBlockAndBadVisibilityOnField", "shutdown", 35);
    }

    @Test
    void failurePath_fieldWithBadVisibility_whenOtherMethodIsSynchronized() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedMethodAndBadVisibilityOnField.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "SynchronizedMethodAndBadVisibilityOnField", "shutdown", 35);
    }

    @Test
    void failurePath_fieldWithBadVisibility_whenClassExtendsThread() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityThread.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "FieldWithBadVisibilityThread", "shutdown", 36);
    }

    @Test
    void failurePath_fieldWithBadVisibility_whenClassImplementsRunnable() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityRunnable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "FieldWithBadVisibilityRunnable", "shutdown", 36);
    }

    @Test
    void happyPath_atomicField() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/AtomicField.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }

    @Test
    void happyPath_volatileField() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/VolatileField.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }

    @Test
    void happyPath_synchronizedBlock() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlock.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }

    @Test
    void happyPath_synchronizedMethod() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedMethod.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }
}
