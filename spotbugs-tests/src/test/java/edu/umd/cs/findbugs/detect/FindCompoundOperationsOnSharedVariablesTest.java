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

class FindCompoundOperationsOnSharedVariablesTest extends AbstractIntegrationTest {
    private static final String BUG_TYPE = "COSV_COMPOUND_OPERATIONS_ON_SHARED_VARIABLES";

    @Test
    void happyPath_compoundOperation_onSharedAtomicVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundOperationOnSharedAtomicVariable.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }

    @Test
    void happyPath_compoundOperationInsideSynchronizedBlock_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/SynchronizedBlockCompoundOperationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }

    @Test
    void happyPath_compoundOperationInsideSynchronizedMethod_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/SynchronizedMethodCompoundOperationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }

    // --num
    @Test
    void reportsBugFor_compoundPreDecrementation_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPreDecrementationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundPreDecrementationOnSharedVariable", "getNum", 29);
    }

    // num--
    @Test
    void reportsBugFor_compoundPostDecrementation_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPostDecrementationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundPostDecrementationOnSharedVariable", "getNum", 29);
    }

    // ++num
    @Test
    void reportsBugFor_compoundPreIncrementation_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPreIncrementationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundPreIncrementationOnSharedVariable", "getNum", 29);
    }

    // num++
    @Test
    void reportsBugFor_compoundPostIncrementation_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPostIncrementationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundPostIncrementationOnSharedVariable", "getNum", 29);
    }

    // &=
    @Test
    void reportsBugFor_compoundIAND_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundIANDOperationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundIANDOperationOnSharedVariable", "getNum", 29);
    }

    // |=
    @Test
    void reportsBugFor_compoundIOR_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundIOROperationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundIOROperationOnSharedVariable", "getNum", 29);
    }

    // >>>=
    @Test
    void reportsBugFor_compoundLogicalRightShifting_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundLogicalRightShiftingOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundLogicalRightShiftingOnSharedVariable", "getNum", 29);
    }

    // >>=
    @Test
    void reportsBugFor_compoundRightShifting_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundRightShiftingOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundRightShiftingOnSharedVariable", "getNum", 29);
    }

    // <<=
    @Test
    void reportsBugFor_compoundLeftShifting_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundLeftShiftingOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundLeftShiftingOnSharedVariable", "getNum", 29);
    }

    // %=
    @Test
    void reportsBugFor_compoundModulo_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundModuloOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundModuloOnSharedVariable", "getNum", 30);
    }

    // *=
    @Test
    void reportsBugFor_compoundMultiplication_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundMultiplicationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundMultiplicationOnSharedVariable", "getNum", 29);
    }

    // /=
    @Test
    void reportsBugFor_compoundDivision_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundDivisionOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundDivisionOnSharedVariable", "getNum", 29);
    }

    // -=
    @Test
    void reportsBugFor_compoundSubtraction_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundSubtractionOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundSubtractionOnSharedVariable", "getNum", 29);
    }


    // +=
    @Test
    void reportsBugFor_compoundAddition_onSharedVolatileVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundAdditionOnSharedVolatileVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundAdditionOnSharedVolatileVariable", "getNum", 29);
    }

    // ^=
    @Test
    void reportsBugFor_compoundXOROperation_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundXOROperationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundXOROperationOnSharedVariable", "getFlag", 29);
    }
}
