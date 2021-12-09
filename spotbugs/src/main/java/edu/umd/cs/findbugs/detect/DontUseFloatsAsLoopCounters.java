package edu.umd.cs.findbugs.detect;

import org.apache.bcel.Const;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;


public class DontUseFloatsAsLoopCounters extends OpcodeStackDetector implements StatelessDetector {

    BugReporter bugReporter;

    public DontUseFloatsAsLoopCounters(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        if ((seen == Const.GOTO || seen == Const.GOTO_W) && (isFloatOrDouble(getCodeByte(getBranchTarget())) && (getBranchTarget() < getPC())
                && (isInductionVariable(getCodeByte(getPC() - 2)) || isInductionVariable(getCodeByte(getPC() - 3))))) {
            bugReporter.reportBug(new BugInstance(this, "FL_FLOATS_AS_LOOP_COUNTERS", NORMAL_PRIORITY)
                    .addClassAndMethod(this).addSourceLine(this, getBranchTarget()));
        }
    }

    private boolean isFloatOrDouble(int seen) {
        if ((seen == Const.FLOAD || seen == Const.FLOAD_0 || seen == Const.FLOAD_1 || seen == Const.FLOAD_2 || seen == Const.FLOAD_3)
                || (seen == Const.DLOAD || seen == Const.DLOAD_0 || seen == Const.DLOAD_1 || seen == Const.DLOAD_2 || seen == Const.DLOAD_3)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isInductionVariable(int seen) {
        if (seen == Const.FADD || seen == Const.FSUB || seen == Const.DADD || seen == Const.DSUB) {
            return true;
        } else {
            return false;
        }
    }
}
