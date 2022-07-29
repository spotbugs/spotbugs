package edu.umd.cs.findbugs.detect;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XMethod;

/**
 * This detector can find Assertions that try to validate method
 * arguments.
 */
public class FindArgumentAssertions extends AbstractAssertDetector {

    public FindArgumentAssertions(BugReporter bugReporter) {
        super(bugReporter);
    }

    /**
     * Only interested in public methods
     */
    @Override
    public void visit(Method obj) {
        if (!obj.isPublic())
            return;
    }

    /**
     * Only interested in public classes
     */
    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass ctx = classContext.getJavaClass();
        // Break out of analyzing this class if not public
        if (!ctx.isPublic())
            return;
        super.visitClassContext(classContext);
    }

    /**
     * Checks if the methods paramaeter is an initial arg.
     */
    private boolean isInitialArg() {
        XMethod m = getXMethodOperand();
        int numPar = m.getNumParams();
        // Get values from the stack
        for (int i = 0; i < numPar; i++) {
            Item item = stack.getStackItem(i);
            if (item.isInitialParameter())
                return true;
        }
        return false;
    }

    /**
     * Returns true if the opcode is a method invocation false otherwise
     */
    private boolean isMethodCall(int seen) {
        boolean methodCall = false;
        if (seen == Const.INVOKESTATIC ||
                seen == Const.INVOKEVIRTUAL ||
                seen == Const.INVOKEINTERFACE ||
                seen == Const.INVOKESPECIAL) {
            methodCall = true;
        }
        return methodCall;
    }

    /**
     * Returns the numbor of arguments that is popped from the stack for the given opcode
     */
    private int checkSeen(int seen) {
        int stackSize = 0;
        switch (seen) {
        // Handle nullchecks
        case Const.IFNONNULL:
        case Const.IFNULL:
        case Const.IFEQ:
        case Const.IFNE:
        case Const.IFLT:
        case Const.IFLE:
        case Const.IFGT:
        case Const.IFGE:
            stackSize = 1;
            break;
        // Handle integer comparison
        case Const.IF_ICMPEQ:
        case Const.IF_ICMPNE:
        case Const.IF_ICMPLT:
        case Const.IF_ICMPLE:
        case Const.IF_ICMPGT:
        case Const.IF_ICMPGE:
            // Handle long
        case Const.LCMP:
            // Handle float comparison
        case Const.FCMPG:
        case Const.FCMPL:
            // Handle double comparison
        case Const.DCMPG:
        case Const.DCMPL:
            // Reference equality
        case Const.IF_ACMPEQ:
        case Const.IF_ACMPNE:
            stackSize = 2;
            break;
        default:
            break;
        }
        return stackSize;
    }

    /**
     * Finds MET01 rule violating assertions. 
     */
    @Override
    protected void detect(int seen) {
        boolean wasArg = false;
        // Handle method call
        if (isMethodCall(seen)) {
            // If wasArg have not been found - Nested methods
            if (!wasArg)
                wasArg = isInitialArg();
            // Handle comparison
        } else {
            int stackSize = checkSeen(seen);
            if (stackSize > 0) {
                for (int i = 0; i < stackSize; i++) {
                    OpcodeStack.Item item = stack.getStackItem(i);
                    wasArg = item.isInitialParameter();
                }
            }
        }
        if (wasArg) {
            BugInstance bug = new BugInstance(this, "AA_ASSERTION_OF_ARGUMENTS", LOW_PRIORITY)
                    .addClassAndMethod(this)
                    .addSourceLine(this, getPC());
            reportBug(bug);
        }
    }
}
