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

import org.apache.bcel.Const;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.util.MutableClasses;

/**
 * This detector can find assertions that violate the EXP06 rule.
 */
public class FindAssertionsWithSideEffects extends AbstractAssertDetector {

    public FindAssertionsWithSideEffects(BugReporter bugReporter) {
        super(bugReporter);
    }

    /**
     * Returns true if the opcode is a method invocation false otherwise
     */
    private boolean isMethodCall(int seen) {
        return seen == Const.INVOKESTATIC ||
                seen == Const.INVOKEVIRTUAL ||
                seen == Const.INVOKEINTERFACE ||
                seen == Const.INVOKESPECIAL ||
                seen == Const.INVOKEDYNAMIC;
    }

    /**
     * Returns true if the opcode is a side effect producing instruction
     */
    private boolean checkSeen(int seen) {
        return seen == Const.IINC ||
                seen == Const.ISTORE ||
                seen == Const.ISTORE_0 ||
                seen == Const.ISTORE_1 ||
                seen == Const.ISTORE_2 ||
                seen == Const.ISTORE_3;
    }

    /**
     * Finds assertion with possible side effect
     */
    @Override
    protected void detect(int seen) {
        if (isMethodCall(seen)) {
            String retSig = new SignatureParser(getXMethodOperand().getSignature()).getReturnTypeSignature();
            String classSig = getXClassOperand().getSourceSignature();
            if (MutableClasses.mutableSignature("L" + getClassConstantOperand() + ";") &&
                    MutableClasses.looksLikeASetter(getNameConstantOperand(), classSig, retSig)) {
                BugInstance bug = new BugInstance(this, "ASE_ASSERTION_WITH_SIDE_EFFECT_METHOD", LOW_PRIORITY)
                        .addClassAndMethod(this)
                        .addSourceLine(this, getPC());
                reportBug(bug);
            }
        } else if (checkSeen(seen)) {
            BugInstance bug = new BugInstance(this, "ASE_ASSERTION_WITH_SIDE_EFFECT", LOW_PRIORITY)
                    .addClassAndMethod(this)
                    .addSourceLine(this, getPC());
            reportBug(bug);
        }
    }
}
