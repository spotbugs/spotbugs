package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;

import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Method;

/**
 * A detector that checks for lines in JUnit tests that look like `assertTrue(object instanceof Class)` and discourages them.
 *
 * It may be more useful to observe error messages from bad casts, as this may reveal more information regarding the
 * circumstances of the exception than a "false is not true" assert message.
 *
 * This detector reports bugs of the type `JUA_DONT_ASSERT_INSTANCEOF_IN_TESTS`.
 */
public class DontAssertInstanceofInTests extends OpcodeStackDetector {
    private boolean isTest;

    private BugInstance currBug = null;

    BugReporter bugReporter;

    public DontAssertInstanceofInTests(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitMethod(Method obj) {
        isTest = false;
        for (AnnotationEntry entry : obj.getAnnotationEntries()) {
            String entryType = entry.getAnnotationType();
            // Visit the method only if it's a JUnit4 test.
            isTest |= "Lorg/junit/Test;".equals(entryType);
            // Visit the method only if it's a JUnit5 test.
            isTest |= "Lorg/junit/jupiter/api/Test;".equals(entryType);
        }
    }

    @Override
    public boolean shouldVisitCode(Code obj) {
        return isTest;
    }

    @Override
    public void sawOpcode(int seen) {

        switch (seen) {
        case Const.INSTANCEOF: {
            // Initialise the bug instance here.
            currBug = new BugInstance(this, "JUA_DONT_ASSERT_INSTANCEOF_IN_TESTS", NORMAL_PRIORITY);
            String operand = getDottedClassConstantOperand();
            currBug.addTypeOfNamedClass(operand); // {0}

            break;
        }
        case Const.INVOKESTATIC: {
            if (getPrevOpcode(1) == Const.INSTANCEOF) {
                String ciOp = getClassConstantOperand();
                String ncOp = getNameConstantOperand();

                if (("org/junit/Assert".equals(ciOp) || "org/junit/jupiter/api/Assertion".equals(ciOp)) &&
                        "assertTrue".equals(ncOp)) {
                    // This condition only triggers if the previous opcode was instanceof,
                    // so currBug is guaranteed to be a new BugInstance.
                    currBug.addClassAndMethod(this) // {2}
                            .addSourceLine(this); // {3}
                    bugReporter.reportBug(currBug);
                }
            }
        }
        }

    }
}
