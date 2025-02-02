package edu.umd.cs.findbugs.detect;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.util.ClassName;

public class IDivResultCastToDouble extends BytecodeScanningDetector {
    private static final boolean DEBUG = SystemProperties.getBoolean("idcd.debug");

    //    private final BugReporter bugReporter;

    private final BugAccumulator bugAccumulator;

    private int prevOpCode;

    public IDivResultCastToDouble(BugReporter bugReporter) {
        //        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(Method obj) {
        if (DEBUG) {
            System.out.println("Visiting " + obj);
        }
    }

    @Override
    public void visit(Code obj) {
        super.visit(obj);
        bugAccumulator.reportAccumulatedBugs();
    }

    SourceLineAnnotation pendingIdivCastToDivBugLocation = null;

    @Override
    public void sawOpcode(int seen) {

        if (DEBUG) {
            System.out.println("Saw opcode " + Const.getOpcodeName(seen) + " " + pendingIdivCastToDivBugLocation);
        }

        if ((prevOpCode == Const.I2D || prevOpCode == Const.L2D) && seen == Const.INVOKESTATIC && ClassName.isMathClass(getClassConstantOperand())
                && "ceil".equals(getNameConstantOperand())) {
            bugAccumulator
                    .accumulateBug(new BugInstance(this, "ICAST_INT_CAST_TO_DOUBLE_PASSED_TO_CEIL", HIGH_PRIORITY)
                            .addClassAndMethod(this), this);
            pendingIdivCastToDivBugLocation = null;
        } else if ((prevOpCode == Const.I2F || prevOpCode == Const.L2F) && seen == Const.INVOKESTATIC
                && ClassName.isMathClass(getClassConstantOperand()) && "round".equals(getNameConstantOperand())) {
            bugAccumulator.accumulateBug(
                    new BugInstance(this, "ICAST_INT_CAST_TO_FLOAT_PASSED_TO_ROUND", NORMAL_PRIORITY).addClassAndMethod(this),
                    this);
            pendingIdivCastToDivBugLocation = null;
        } else if (pendingIdivCastToDivBugLocation != null) {
            bugAccumulator.accumulateBug(
                    new BugInstance(this, "ICAST_IDIV_CAST_TO_DOUBLE", NORMAL_PRIORITY).addClassAndMethod(this),
                    pendingIdivCastToDivBugLocation);
            pendingIdivCastToDivBugLocation = null;
        }

        if (prevOpCode == Const.IDIV && (seen == Const.I2D || seen == Const.I2F) || prevOpCode == Const.LDIV && (seen == Const.L2D
                || seen == Const.L2F)) {
            pendingIdivCastToDivBugLocation = SourceLineAnnotation.fromVisitedInstruction(this);
        }
        prevOpCode = seen;
    }
}
