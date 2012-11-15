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
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase;
import edu.umd.cs.findbugs.ba.NullnessAnnotation;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

/**
 * @author alison
 */
public class BooleanReturnNull extends OpcodeStackDetector {

    BugAccumulator bugAccumulator;

    public BooleanReturnNull(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(Code code) {
        String s = getMethodSig();
        SignatureParser sp = new SignatureParser(s);
        // Check to see if the method has Boolean return type
        if (!"Ljava/lang/Boolean;".equals(sp.getReturnTypeSignature()))
            return;

        if (isExplicitlyNullable())
            return;

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
    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.bcel.OpcodeStackDetector#sawOpcode(int)
     */
    @Override
    public void sawOpcode(int seen) {
        if (seen == ARETURN && getPrevOpcode(1) == ACONST_NULL)
            bugAccumulator.accumulateBug(new BugInstance(this, "NP_BOOLEAN_RETURN_NULL",
                    getMethodName().startsWith("is") ? HIGH_PRIORITY : NORMAL_PRIORITY).addClassAndMethod(this), this);

    }

}
