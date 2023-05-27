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
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Before;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class FindCompoundOperationsOnSharedVariablesTest extends AbstractIntegrationTest {

    @Before
    public void setup() {
        SystemProperties.setProperty("ful.debug", "true");
    }

    @Test
    public void happyPath_compoundOperation_onSharedAtomicVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundOperationOnSharedAtomicVariable.class");
        assertCOSVNumOfBugs(0);
    }

    @Test
    public void happyPath_compoundOperationInsideSynchronizedBlock_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/SynchronizedBlockCompoundOperationOnSharedVariable.class");
        assertCOSVNumOfBugs(0);
    }

    @Test
    public void happyPath_compoundOperationInsideSynchronizedMethod_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/SynchronizedMethodCompoundOperationOnSharedVariable.class");
        assertCOSVNumOfBugs(0);
    }

    // --num
    @Test
    public void reportsBugFor_compoundPreDecrementation_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPreDecrementationOnSharedVariable.class");
        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundPreDecrementationOnSharedVariable", "getNum", 29);
    }

    // num--
    @Test
    public void reportsBugFor_compoundPostDecrementation_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPostDecrementationOnSharedVariable.class");
        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundPostDecrementationOnSharedVariable", "getNum", 29);
    }

    // ++num
    @Test
    public void reportsBugFor_compoundPreIncrementation_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPreIncrementationOnSharedVariable.class");
        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundPreIncrementationOnSharedVariable", "getNum", 29);
    }

    // num++
    @Test
    public void reportsBugFor_compoundPostIncrementation_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPostIncrementationOnSharedVariable.class");
        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundPostIncrementationOnSharedVariable", "getNum", 29);
    }

    // &=
    @Test
    public void reportsBugFor_compoundIAND_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundIANDOperationOnSharedVariable.class");
        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundIANDOperationOnSharedVariable", "getNum", 29);
    }

    // |=
    @Test
    public void reportsBugFor_compoundIOR_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundIOROperationOnSharedVariable.class");
        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundIOROperationOnSharedVariable", "getNum", 29);
    }

    // >>>=
    @Test
    public void reportsBugFor_compoundLogicalRightShifting_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundLogicalRightShiftingOnSharedVariable.class");
        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundLogicalRightShiftingOnSharedVariable", "getNum", 29);
    }

    // >>=
    @Test
    public void reportsBugFor_compoundRightShifting_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundRightShiftingOnSharedVariable.class");
        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundRightShiftingOnSharedVariable", "getNum", 29);
    }

    // <<=
    @Test
    public void reportsBugFor_compoundLeftShifting_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundLeftShiftingOnSharedVariable.class");
        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundLeftShiftingOnSharedVariable", "getNum", 29);
    }

    // %=
    @Test
    public void reportsBugFor_compoundModulo_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundModuloOnSharedVariable.class");
        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundModuloOnSharedVariable", "getNum", 30);
    }

    // *=
    @Test
    public void reportsBugFor_compoundMultiplication_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundMultiplicationOnSharedVariable.class");
        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundMultiplicationOnSharedVariable", "getNum", 29);
    }

    // /=
    @Test
    public void reportsBugFor_compoundDivision_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundDivisionOnSharedVariable.class");
        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundDivisionOnSharedVariable", "getNum", 29);
    }

    // -=
    @Test
    public void reportsBugFor_compoundSubtraction_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundSubtractionOnSharedVariable.class");
        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundSubtractionOnSharedVariable", "getNum", 29);
    }


    // +=
    @Test
    public void reportsBugFor_compoundAddition_onSharedVolatileVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundAdditionOnSharedVolatileVariable.class");
        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundAdditionOnSharedVolatileVariable", "getNum", 29);
    }

    // ^=
    @Test
    public void reportsBugFor_compoundXOROperation_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundXOROperationOnSharedVariable.class");
        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundXOROperationOnSharedVariable", "getFlag", 29);
    }

    private void assertCOSVNumOfBugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("COSV_COMPOUND_OPERATIONS_ON_SHARED_VARIABLES")
                .build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertCOSVBug(String className, String methodName, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("COSV_COMPOUND_OPERATIONS_ON_SHARED_VARIABLES")
                .inClass(className)
                .inMethod(methodName)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
