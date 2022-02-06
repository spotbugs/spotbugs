package edu.umd.cs.findbugs.detect;

import org.apache.bcel.Const;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import java.util.stream.Stream;

public class DontUseFloatsAsLoopCounters extends OpcodeStackDetector implements StatelessDetector {

    BugReporter bugReporter;

    public DontUseFloatsAsLoopCounters(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        if ((seen == Const.GOTO || seen == Const.GOTO_W) &&
                Stream.of(Const.FLOAD, Const.FLOAD_0, Const.FLOAD_1, Const.FLOAD_2, Const.FLOAD_3, Const.DLOAD, Const.DLOAD_0, Const.DLOAD_1,
                        Const.DLOAD_2, Const.DLOAD_3).anyMatch(x -> x == getCodeByte(getBranchTarget())) && (getBranchTarget() < getPC())) {
            bugReporter.reportBug(new BugInstance(this, "FL_FLOATS_AS_LOOP_COUNTERS", NORMAL_PRIORITY)
                    .addClassAndMethod(this).addSourceLine(this, getBranchTarget()));
        }
    }
}
