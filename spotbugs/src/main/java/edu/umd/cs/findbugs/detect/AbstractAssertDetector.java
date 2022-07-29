package edu.umd.cs.findbugs.detect;

import org.apache.bcel.Const;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

/**
 * Abstract base class for finding assertions
 */
public abstract class AbstractAssertDetector extends OpcodeStackDetector {

    private final BugReporter bugReporter;

    protected boolean inAssert = false;

    public AbstractAssertDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    /**
     * Implement this method in a concrete detector
     */
    abstract protected void detect(int seen);

    /**
     * Searches for assertion opening, and closing points.
     * When in assert, will call the detect method.
     */
    @Override
    public void sawOpcode(int seen) {
        if (inAssert) {
            detect(seen);
        }
        if (seen == Const.GETSTATIC && "$assertionsDisabled".equals(getNameConstantOperand())) {
            inAssert = true;
        }
        if (seen == Const.NEW && getClassConstantOperand().equals("java/lang/AssertionError")) {
            resetState();
        }
    }

    protected void resetState() {
        inAssert = false;
    }

    protected void reportBug(BugInstance bug) {
        bugReporter.reportBug(bug);
    }
}
