package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ExceptionTable;
import org.apache.bcel.classfile.Method;


public class ThrowingExceptions extends OpcodeStackDetector {
    private final BugReporter bugReporter;

    public ThrowingExceptions(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(Method obj) {
        if (obj.isSynthetic()) {
            return;
        }

        ExceptionTable exceptionTable = obj.getExceptionTable();
        if (exceptionTable == null) {
            return;
        }

        String[] exceptionNames = exceptionTable.getExceptionNames();
        for (String exception : exceptionNames) {
            if ("java.lang.Exception".equals(exception)) {
                reportBug("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", getXMethod());
            } else if ("java.lang.Throwable".equals(exception)) {
                reportBug("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", getXMethod());
            }
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.ATHROW) {
            OpcodeStack.Item item = stack.getStackItem(0);
            if (item != null) {
                if ("Ljava/lang/RuntimeException;".equals(item.getSignature())) {
                    reportBug("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", getXMethod());
                }
            }
        }
    }

    private void reportBug(String bugName, XMethod method) {
        bugReporter.reportBug(new BugInstance(this, bugName, NORMAL_PRIORITY).addClass(this).addMethod(method));
    }
}
