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
import org.apache.bcel.classfile.JavaClass;

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
     * Only interested in public classes
     */
    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass ctx = classContext.getJavaClass();
        // Break out of analyzing this class if not public
        if (!ctx.isPublic()) {
            return;
        }
        super.visitClassContext(classContext);
    }

    /**
     * Checks if the methods parameter is an initial arg.
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
        return seen == Const.INVOKESTATIC ||
                seen == Const.INVOKEVIRTUAL ||
                seen == Const.INVOKEINTERFACE ||
                seen == Const.INVOKESPECIAL;
    }

    /**
     * Returns the number of arguments that is popped from the stack for the given opcode
     */
    private int checkSeen(int seen) {
        int stackSize;
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
            stackSize = 0;
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
        XMethod method = getXMethod();
        if (!method.isPublic()) {
            return;
        }

        if (isMethodCall(seen)) {
            // Handle method call
            wasArg = isInitialArg();
        } else {
            // Handle comparison
            int stackSize = checkSeen(seen);
            if (stackSize > 0) {
                for (int i = 0; i < stackSize; i++) {
                    OpcodeStack.Item item = stack.getStackItem(i);
                    wasArg = item.isInitialParameter();
                    if (wasArg) {
                        break;
                    }
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
