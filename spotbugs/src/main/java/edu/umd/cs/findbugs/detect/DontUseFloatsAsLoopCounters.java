package edu.umd.cs.findbugs.detect;

import org.apache.bcel.Const;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DontUseFloatsAsLoopCounters extends OpcodeStackDetector implements StatelessDetector {

    private final BugReporter bugReporter;

    private static final Map<Integer, Integer> FLOAT_LOADERS;
    static {
        Map<Integer, Integer> tmp = new HashMap<>();
        tmp.put((int) Const.FLOAD, 2);
        tmp.put((int) Const.FLOAD_0, 1);
        tmp.put((int) Const.FLOAD_1, 1);
        tmp.put((int) Const.FLOAD_2, 1);
        tmp.put((int) Const.FLOAD_3, 1);
        tmp.put((int) Const.DLOAD, 2);
        tmp.put((int) Const.DLOAD_0, 1);
        tmp.put((int) Const.DLOAD_1, 1);
        tmp.put((int) Const.DLOAD_2, 1);
        tmp.put((int) Const.DLOAD_3, 1);
        FLOAT_LOADERS = Collections.unmodifiableMap(tmp);
    }

    private static final Map<Integer, Integer> FLOAT_CONSTANT_PUSHERS;
    static {
        Map<Integer, Integer> tmp = new HashMap<>();
        tmp.put((int) Const.FCONST_1, 1);
        tmp.put((int) Const.FCONST_2, 1);
        tmp.put((int) Const.DCONST_1, 1);
        tmp.put((int) Const.LDC, 2);
        tmp.put((int) Const.LDC_W, 3);
        tmp.put((int) Const.LDC2_W, 3);
        FLOAT_CONSTANT_PUSHERS = Collections.unmodifiableMap(tmp);
    }

    private static final Set<Integer> FLOAT_COMPARERS = new HashSet<>(Arrays.asList(
            (int) Const.FCMPG,
            (int) Const.FCMPL,
            (int) Const.DCMPG,
            (int) Const.DCMPL));

    private static final Set<Integer> FLOAT_STORERS = new HashSet<>(Arrays.asList(
            (int) Const.FSTORE,
            (int) Const.FSTORE_0,
            (int) Const.FSTORE_1,
            (int) Const.FSTORE_2,
            (int) Const.FSTORE_3,
            (int) Const.DSTORE,
            (int) Const.DSTORE_0,
            (int) Const.DSTORE_1,
            (int) Const.DSTORE_2,
            (int) Const.DSTORE_3));

    private static final Set<Integer> FLOAT_ADDITIVE_OPS = new HashSet<>(Arrays.asList(
            (int) Const.FADD,
            (int) Const.FSUB,
            (int) Const.DADD,
            (int) Const.DSUB));

    public DontUseFloatsAsLoopCounters(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        if ((seen == Const.GOTO || seen == Const.GOTO_W) &&
                getBranchTarget() < getPC() &&
                checkLoopEnd() && checkLoopStart(getBranchTarget())) {
            bugReporter.reportBug(new BugInstance(this, "FL_FLOATS_AS_LOOP_COUNTERS", NORMAL_PRIORITY)
                    .addClassAndMethod(this).addSourceLine(this, getBranchTarget()));
        }
    }

    private boolean checkLoopEnd() {
        return FLOAT_STORERS.contains(getPrevOpcode(1)) &&
                FLOAT_ADDITIVE_OPS.contains(getPrevOpcode(2));
    }

    private boolean checkLoopStart(int startPC) {
        if (!FLOAT_LOADERS.containsKey(getCodeByte(startPC))) {
            return false;
        }

        int nextPC = startPC + FLOAT_LOADERS.get(getCodeByte(startPC));

        if (!FLOAT_CONSTANT_PUSHERS.containsKey(getCodeByte(nextPC))) {
            return false;
        }

        nextPC += FLOAT_CONSTANT_PUSHERS.get(getCodeByte(nextPC));

        if (!FLOAT_COMPARERS.contains(getCodeByte(nextPC++))) {
            return false;
        }

        return isBranch(getCodeByte(nextPC));
    }
}
