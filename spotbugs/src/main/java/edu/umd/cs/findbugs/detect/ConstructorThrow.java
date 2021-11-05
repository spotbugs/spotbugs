package edu.umd.cs.findbugs.detect;

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.Const;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.ExceptionTable;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Code;

/**
 * This detector can find constructors that throw exception.
 */
public class ConstructorThrow extends OpcodeStackDetector {

    private final BugReporter bugReporter;
    private final BugAccumulator bugAccumulator;
    private final Set<String> calledFromCtor = new HashSet<String>();

    private boolean isFinalClass = false;
    private boolean isFinalFinalizer = false;
    private boolean isFirstPass = true;

    public ConstructorThrow(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    /**
     * Visit a class to find the constructor, then collect all the methods that gets called in it.
     * Also we are checking for final declaration on the class, or a final finalizer, as if present
     * no finalizer attack can happen.
     */
    @Override
    public void visit(JavaClass obj) {
        resetState();
        if (obj.isFinal()) {
            isFinalClass = true;
            return;
        }
        for (Method m : obj.getMethods()) {
            // First visit the constructor, it might not be at the start of the class.
            if ("<init>".equals(m.getName())) {
                // This will visit all constructors.
                doVisitMethod(m);
            } else if ("finalize".equals(m.getName())) {
                // Check for finalizer.
                if (m.isFinal()) {
                    isFinalFinalizer = true;
                }
            }
        }
        isFirstPass = false;
    }

    @Override
    public void visit(Method obj) {
        if (isFinalClass || isFinalFinalizer)
            return;
        if (isConstructor() || methodCalledFromCtor()) {
            // Check if there is a throws keyword for checked exceptions.
            ExceptionTable tbl = obj.getExceptionTable();
            boolean throwsExceptions = tbl != null && tbl.getNumberOfExceptions() > 0;
            if (throwsExceptions) {
                accumulateBug();
            }
        }
    }

    @Override
    public void visit(Code obj) {
        super.visit(obj);
        bugAccumulator.reportAccumulatedBugs();
    }

    /**
     * 1. Check for any throw expression in the constructor.
     * 2. Check for any unchecked exception throw inside constructor,
     *    or any of the called methods.
     * If the class is final, we are fine, no finalizer attack can happen.
     */
    @Override
    public void sawOpcode(int seen) {
        if (isFinalClass || isFinalFinalizer)
            return;
        if (isFirstPass) {
            tryCollectMethod(seen);
        } else {
            if (isConstructor() || methodCalledFromCtor()) {
                if (seen == Const.ATHROW)
                    accumulateBug();
            }
        }
    }

    /** Collects all the methods that are called from the constructor */
    private void tryCollectMethod(int seen) {
        if (!isMethodCall(seen))
            return;
        String classConstantOperand = "";
        try {
            classConstantOperand = getClassConstantOperand();
        } catch (IllegalStateException e) {
            bugReporter.logError("Seen OPcode and failed to get ClassConstantOperand: " + seen, e);
        }
        String method = classConstantOperand + "." + getNameConstantOperand() + " : " + getSigConstantOperand();
        // Not interested in object superctor
        if (!"java/lang/Object.<init> : ()V".equals(method)) {
            calledFromCtor.add(method);
        }
    }

    private void resetState() {
        isFinalClass = false;
        isFinalFinalizer = false;
        isFirstPass = true;
        calledFromCtor.clear();
    }

    private void accumulateBug() {
        BugInstance bug = new BugInstance(this, "CT_CONSTRUCTOR_THROW", NORMAL_PRIORITY)
                .addClassAndMethod(this)
                .addSourceLine(this, getPC());
        bugAccumulator.accumulateBug(bug, this);
    }

    private boolean isMethodCall(int seen) {
        return seen == Const.INVOKESTATIC || seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE || seen == Const.INVOKESPECIAL;
    }

    private boolean methodCalledFromCtor() {
        return calledFromCtor.contains(getFullyQualifiedMethodName());
    }

    private boolean isConstructor() {
        return Const.CONSTRUCTOR_NAME.equals(getMethodName());
    }
}
