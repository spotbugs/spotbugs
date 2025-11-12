package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;

/**
 * Detector for identifying usage of {@code sun.misc.Unsafe} or {@code jdk.internal.misc.Unsafe}
 * methods in Java bytecode. This class extends {@link OpcodeStackDetector} and reports a bug
 * whenever an {@code INVOKEVIRTUAL} instruction targets one of the Unsafe classes.
 *
 * <p>Unsafe operations can compromise memory safety and are discouraged in most Java applications.
 * This detector helps flag such usages for further review.
 *
 * <p>Detected issues are reported to the provided {@link BugReporter} with high priority.
 */
public class UnsafeDetector extends OpcodeStackDetector {

    /**
     * The {@code BugReporter} instance used to report detected bugs. This field is initialized via
     * constructor injection and is used throughout the detector to log or report any unsafe code
     * patterns found during analysis.
     */
    private final BugReporter bugReporter;

    /**
     * Constructs a new {@code UnsafeDetector} with the specified {@link BugReporter}.
     *
     * @param reporter the {@link BugReporter} instance used to report detected bugs
     */
    public UnsafeDetector(final BugReporter reporter) {
        this.bugReporter = reporter;
    }

    /**
     * Inspects bytecode instructions as they are encountered. Specifically, this method checks if the
     * current opcode is an invocation of a virtual method ({@code INVOKEVIRTUAL}) on the {@code
     * sun/misc/Unsafe} or {@code jdk/internal/misc/Unsafe} classes. If such a call is detected, it
     * reports a high-priority bug instance indicating the use of potentially unsafe operations.
     *
     * @param seen the opcode of the currently visited instruction
     */
    @Override
    public void sawOpcode(final int seen) {
        if (seen != Const.INVOKEVIRTUAL) {
            return;
        }
        if (getClassConstantOperand().equals("sun/misc/Unsafe")
                || getClassConstantOperand().equals("jdk/internal/misc/Unsafe")) {
            BugInstance bug =
                    new BugInstance(this, "UNS_UNSAFE_CALL", HIGH_PRIORITY)
                            .addCalledMethod(this)
                            .addClassAndMethod(this)
                            .addSourceLine(this, getPC());
            bugReporter.reportBug(bug);
        }
    }
}
