/*
 * Contributions to SpotBugs
 * Copyright (C) 2020, ritchan
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

class BadInheritanceTest extends AbstractIntegrationTest {
    @Test
    void testCircularDepsTest() {
        performAnalysis("CircularDepsTest.class", "Circ1.class", "Circ2.class", "NoCirc.class");

        assertBugTypeCount("CD_CIRCULAR_DEPENDENCY", 2);
        assertBugInClassCount("CD_CIRCULAR_DEPENDENCY", "CircularDepsTest", 2); // no more details provided for the bug
    }

    @Test
    void testCircularClassInitialization() {
        performAnalysis("CircularClassInitialization.class", "CircularClassInitialization$InnerClassSingleton.class");

        assertBugTypeCount("IC_SUPERCLASS_USES_SUBCLASS_DURING_INITIALIZATION", 1);
        assertBugInClass("IC_SUPERCLASS_USES_SUBCLASS_DURING_INITIALIZATION", "CircularClassInitialization");
    }

    @Test
    void testConfusify() {
        performAnalysis("Confusify.class", "Confusify$YarrBlarr.class", "Confusify$Helper.class");

        assertBugTypeCount("IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD", 1);
        assertBugInMethod("IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD", "Confusify$Helper", "yarrOrBlarr");
    }

    @Test
    void testConfusingParenting() {
        performAnalysis("ConfusingParenting.class");

        assertBugTypeCount("CI_CONFUSED_INHERITANCE", 2);
        assertBugAtField("CI_CONFUSED_INHERITANCE", "ConfusingParenting", "a");
        assertBugAtField("CI_CONFUSED_INHERITANCE", "ConfusingParenting", "b");
    }

    @Test
    void testProtectedMemberOfFinalClass() {
        performAnalysis("ProtectedMemberOfFinalClass.class");

        assertBugTypeCount("CI_CONFUSED_INHERITANCE", 1);
        assertBugAtField("CI_CONFUSED_INHERITANCE", "ProtectedMemberOfFinalClass", "foo");
    }

    @Test
    void testUselessSCMethods() {
        performAnalysis("UselessSCMethods.class", "Super.class");

        assertBugTypeCount("USM_USELESS_SUBCLASS_METHOD", 4);
        assertBugInMethod("USM_USELESS_SUBCLASS_METHOD", "UselessSCMethods", "test1");
        assertBugInMethod("USM_USELESS_SUBCLASS_METHOD", "UselessSCMethods", "test2");
        assertBugInMethod("USM_USELESS_SUBCLASS_METHOD", "UselessSCMethods", "test3");
        assertBugInMethod("USM_USELESS_SUBCLASS_METHOD", "UselessSCMethods", "test5");
    }
}
