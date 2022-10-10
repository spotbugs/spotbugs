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
                    MutableClasses.looksLikeASetter(getNameConstantOperand(), retSig, classSig)) {
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
