package edu.umd.cs.findbugs.detect;


import org.apache.bcel.classfile.CodeException;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class DontCatchNullPointerException
        extends PreorderVisitor implements Detector {

    private final BugReporter reporter;
    ClassContext classContext;

    public DontCatchNullPointerException(BugReporter reporter) {
        this.reporter = reporter;
    }

    private static final String NullPtrExceptionID =
            "java.lang.NullPointerException";

    @Override
    public void visit(CodeException exc) {
        int type = exc.getCatchType();

        if (type == 0)
            return;

        String name = getConstantPool()
                .constantToString(getConstantPool()
                        .getConstant(type));

        if (name.equals(NullPtrExceptionID)) {
            BugInstance bug = new BugInstance(this,
                    "DCN_NULLPOINTER_EXCEPTION",
                    NORMAL_PRIORITY);
            bug.addClassAndMethod(this);
            bug.addSourceLine(this.classContext, this, exc.getHandlerPC());

            reporter.reportBug(bug);
        }
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        this.classContext = classContext;
        this.classContext.getJavaClass().accept(this);
    }

    @Override
    public void report() {
    }
}
