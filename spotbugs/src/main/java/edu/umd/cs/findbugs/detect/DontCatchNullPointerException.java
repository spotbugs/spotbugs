package edu.umd.cs.findbugs.detect;


import org.apache.bcel.classfile.CodeException;

import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;

public class DontCatchNullPointerException
        extends BytecodeScanningDetector {

    private final BugReporter reporter;

    public DontCatchNullPointerException(BugReporter reporter) {
        this.reporter = reporter;
    }

    @DottedClassName
    private static final String NULLPOINTER_EXCEPTION_FQCN =
            "java.lang.NullPointerException";

    @Override
    public void visit(CodeException exc) {
        int type = exc.getCatchType();

        if (type == 0)
            return;

        String name = getConstantPool()
                .constantToString(getConstantPool()
                        .getConstant(type));

        if (name.equals(NULLPOINTER_EXCEPTION_FQCN)) {
            BugInstance bug = new BugInstance(this,
                    "DCN_NULLPOINTER_EXCEPTION",
                    NORMAL_PRIORITY);
            bug.addClassAndMethod(this);
            bug.addSourceLine(getClassContext(), this, exc.getHandlerPC());

            reporter.reportBug(bug);
        }
    }
}
