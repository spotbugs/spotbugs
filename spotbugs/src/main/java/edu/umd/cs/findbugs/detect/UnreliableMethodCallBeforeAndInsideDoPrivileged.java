package edu.umd.cs.findbugs.detect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

public class UnreliableMethodCallBeforeAndInsideDoPrivileged extends OpcodeStackDetector {
    private static class CalleeInfo {
        ClassDescriptor calledClass;
        XMethod calledMethod;
        OpcodeStack.Item calledOn;
        String calledOnName;
        SourceLineAnnotation srcLine;

        CalleeInfo(ClassDescriptor cls, XMethod called, OpcodeStack.Item obj, String name, SourceLineAnnotation line) {
            calledClass = cls;
            calledMethod = called;
            calledOn = obj;
            calledOnName = name;
            srcLine = line;
        }
    }

    private static class CallerInfo {
        OpcodeStack.Item calledOn;
        JavaClass callerClass;
        XMethod callerMethod;
        SourceLineAnnotation srcLine;

        CallerInfo(OpcodeStack.Item obj, JavaClass cls, XMethod caller, SourceLineAnnotation line) {
            calledOn = obj;
            callerClass = cls;
            callerMethod = caller;
            srcLine = line;
        }
    }

    private static class CallPair {
        CalleeInfo outside;
        CallerInfo inside;

        CallPair(CalleeInfo out, CallerInfo in) {
            outside = out;
            inside = in;
        }
    }

    private Map<XMethod, Set<CalleeInfo>> nonFinalMethodsCalledOnParam = new HashMap<>();
    private Map<XMethod, Set<CallerInfo>> methodsCalledInsidePrivilegedAction = new HashMap<>();

    private String topParameterName = null;

    private boolean isDoPrivileged = false;
    private boolean isDoPrivilegedRun = false;

    BugAccumulator bugAccumulator;

    public UnreliableMethodCallBeforeAndInsideDoPrivileged(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(JavaClass obj) {
        isDoPrivileged = Subtypes2.instanceOf(getDottedClassName(), "java.security.PrivilegedAction")
                || Subtypes2.instanceOf(getDottedClassName(), "java.security.PrivilegedExceptionAction");
    }

    @Override
    public void visit(Method obj) {
        nonFinalMethodsCalledOnParam.clear();
        isDoPrivilegedRun = isDoPrivileged && "run".equals(getMethodName());
    }

    @Override
    public void visit(Code obj) {
        if (!isDoPrivilegedRun && (!getThisClass().isPublic() || !getMethod().isPublic())) {
            return;
        }
        super.visit(obj);
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.INVOKEVIRTUAL && getXClassOperand() != null && getXMethodOperand() != null) {
            if (getXMethodOperand().getSignature().endsWith("V")) {
                return;
            }
            OpcodeStack.Item object = stack.getStackItem(0);

            if (isDoPrivilegedRun) {
                if (!getXClassOperand().isFinal() && !getXMethodOperand().isFinal() && object.isInitialParameter()
                        && isNestingMethodLocalVariable(object)) {
                    addToMethodsCalledInsidePrivilegedAction(getXMethodOperand(), object);
                }
                return;
            }

            if (getXClass().isPublic() && getXMethod().isPublic() && !getXClassOperand().isFinal()
                    && !getXMethodOperand().isFinal() && object.isInitialParameter() && object.getXField() == null
                    && topParameterName != null) {
                addTononFinalMethodsCalledOnParam(getClassDescriptorOperand(), getXMethodOperand(), object);
            }
        } else if (seen == Const.INVOKESTATIC && getXMethodOperand() != null
                && "doPrivileged".equals(getXMethodOperand().getName())) {
            OpcodeStack.Item action = stack.getStackItem(0);
            CallPair callPair = lookForCalledOutsideAndInside(action);
            if (callPair != null) {
                reportBug(callPair);
            }
        }
    }

    private boolean isNestingMethodLocalVariable(OpcodeStack.Item object) {
        XField field = object.getXField();
        if (field == null) {
            return false;
        }

        return field.getName().matches("val\\$.*");
    }

    private void addToMethodsCalledInsidePrivilegedAction(XMethod calledMethod, OpcodeStack.Item object) {
        Set<CallerInfo> objects = methodsCalledInsidePrivilegedAction.get(calledMethod);
        if (objects == null) {
            objects = new HashSet<>();
            methodsCalledInsidePrivilegedAction.put(calledMethod, objects);
        }
        objects.add(new CallerInfo(object, getThisClass(), getXMethod(),
                SourceLineAnnotation.fromVisitedInstruction(this)));
    }

    private void addTononFinalMethodsCalledOnParam(ClassDescriptor calledClass, XMethod calledMethod,
            OpcodeStack.Item object) {
        Set<CalleeInfo> objects = nonFinalMethodsCalledOnParam.get(getXMethod());
        if (objects == null) {
            objects = new HashSet<>();
            nonFinalMethodsCalledOnParam.put(getXMethod(), objects);
        }
        objects.add(new CalleeInfo(calledClass, calledMethod, object, topParameterName,
                SourceLineAnnotation.fromVisitedInstruction(this)));
    }

    private CallPair lookForCalledOutsideAndInside(OpcodeStack.Item action) {
        Set<CalleeInfo> callees = nonFinalMethodsCalledOnParam.get(getXMethod());
        if (callees == null) {
            return null;
        }

        for (CalleeInfo calleeInfo : callees) {
            CallerInfo inside = getCalledInside(action, calleeInfo);
            if (inside != null) {
                return new CallPair(calleeInfo, inside);
            }
        }
        return null;
    }

    private CallerInfo getCalledInside(OpcodeStack.Item action, CalleeInfo calleeInfo) {
        Set<CallerInfo> callers = methodsCalledInsidePrivilegedAction.get(calleeInfo.calledMethod);
        if (callers == null) {
            return null;
        }

        for (CallerInfo callerInfo : callers) {
            if (isTheSame(callerInfo, calleeInfo, action)) {
                return callerInfo;
            }
        }
        return null;
    }

    private boolean isTheSame(CallerInfo inside, CalleeInfo outside, OpcodeStack.Item action) {
        XField field = inside.calledOn.getXField();
        if (field == null) {
            return false;
        }

        try {
            return action.getJavaClass().equals(inside.callerClass)
                    && field.getName().equals("val$" + outside.calledOnName);
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
            return false;
        }
    }

    private void reportBug(CallPair callPair) {
        bugAccumulator.accumulateBug(new BugInstance(this, "DP_UNRELIABLE_METHOD_CALL_BEFORE_AND_INSIDE_DO_PRIVILEGED", NORMAL_PRIORITY)
                .addClassAndMethod(this)
                .addSourceLine(this)
                .addClass(callPair.outside.calledClass.getClassName())
                .addCalledMethod(callPair.outside.calledClass.getClassName(), callPair.outside.calledMethod.getName(),
                        callPair.outside.calledMethod.getSignature(), callPair.outside.calledMethod.isStatic())
                .addSourceLine(callPair.outside.srcLine)
                .addSourceLine(callPair.inside.srcLine), this);
    }

    @Override
    public void afterOpcode(int seen) {
        super.afterOpcode(seen);

        LocalVariable localVar = null;
        if (seen == Const.ALOAD_1 && getXMethod().getNumParams() >= 1) {
            localVar = getMethod().getLocalVariableTable().getLocalVariable(1);
        } else if (seen == Const.ALOAD_2 && getXMethod().getNumParams() >= 2) {
            localVar = getMethod().getLocalVariableTable().getLocalVariable(2);
        } else if (seen == Const.ALOAD_3 && getXMethod().getNumParams() >= 3) {
            localVar = getMethod().getLocalVariableTable().getLocalVariable(3);
        } else if (seen == Const.ALOAD && getXMethod().getNumParams() >= getRegisterOperand()) {
            localVar = getMethod().getLocalVariableTable().getLocalVariable(getRegisterOperand());
        }

        if (localVar == null) {
            return;
        }

        topParameterName = localVar.getName();
    }
}
