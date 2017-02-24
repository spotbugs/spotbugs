/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase;
import edu.umd.cs.findbugs.ba.NullnessAnnotation;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

/**
 * Base class for simple type checking detectors which tests if the method
 * returns null references for specific types.
 *
 * @author alison
 * @author Andrey Loskutov
 */
public abstract class TypeReturnNull extends OpcodeStackDetector {

    protected final BugAccumulator bugAccumulator;

    public TypeReturnNull(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(Code code) {
        SignatureParser sp = new SignatureParser(getMethodSig());
        // Check to see if the method has expected return type
        String returnSignature = sp.getReturnTypeSignature();
        if (!matchesReturnSignature(returnSignature)){
            return;
        }

        if (isExplicitlyNullable()){
            return;
        }

        super.visit(code); // make callbacks to sawOpcode for all opcodes
        bugAccumulator.reportAccumulatedBugs();
    }

    private boolean isExplicitlyNullable() {
        AnalysisContext analysisContext = AnalysisContext.currentAnalysisContext();
        INullnessAnnotationDatabase nullnessAnnotationDatabase = analysisContext.getNullnessAnnotationDatabase();
        XMethod xMethod = getXMethod();
        NullnessAnnotation na = nullnessAnnotationDatabase.getResolvedAnnotation(xMethod, true);
        return na != null && na != NullnessAnnotation.NONNULL;
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == ARETURN && getPrevOpcode(1) == ACONST_NULL){
            accumulateBug();
        }
    }

    /**
     * @return true if the given return signature matches expected type
     */
    protected abstract boolean matchesReturnSignature(String returnSignature);

    /**
     * creates individual bug instance on match
     */
    protected abstract void accumulateBug();

}
