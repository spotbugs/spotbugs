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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.BootstrapMethods;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.util.BootstrapMethodsUtil;

public class FindPotentialSecurityCheckBasedOnUntrustedSource extends OpcodeStackDetector {

    private static final Pattern NESTED_CLASS_VARIABLE_NAME_PATTERN = Pattern.compile("val\\$.*");

    private static class CalleeInfo {
        private final ClassDescriptor calledClass;
        private final XMethod calledMethod;
        private final String calledOnName;
        private final SourceLineAnnotation srcLine;

        CalleeInfo(ClassDescriptor cls, XMethod called, String name, SourceLineAnnotation line) {
            calledClass = cls;
            calledMethod = called;
            calledOnName = name;
            srcLine = line;
        }
    }

    private static class CallerInfo {
        private final OpcodeStack.Item calledOn;
        private final JavaClass callerClass;
        private final SourceLineAnnotation srcLine;

        CallerInfo(OpcodeStack.Item obj, JavaClass cls, SourceLineAnnotation line) {
            calledOn = obj;
            callerClass = cls;
            srcLine = line;
        }
    }

    private static class LambdaInfo {
        private final Method lambdaMethod;
        private final String[] argumentNames;

        LambdaInfo(Method method, String[] argNames) {
            lambdaMethod = method;
            argumentNames = argNames;
        }
    }

    private static class LambdaCallInfo {
        private final JavaClass callerClass;
        private final XMethod callerMethod;
        private final SourceLineAnnotation srcLine;
        private final String[] argumentNames;

        LambdaCallInfo(JavaClass cls, XMethod met, SourceLineAnnotation line, String[] argNames) {
            callerClass = cls;
            callerMethod = met;
            srcLine = line;
            argumentNames = argNames;
        }
    }

    private static class CallPair {
        private final CalleeInfo outside;
        private final CallerInfo inside;

        CallPair(CalleeInfo out, CallerInfo in) {
            outside = out;
            inside = in;
        }
    }

    private Map<XMethod, Set<CalleeInfo>> nonFinalMethodsCalledOnParam = new HashMap<>();
    private Map<XMethod, Set<CallerInfo>> methodsCalledInsidePrivilegedAction = new HashMap<>();
    private Map<OpcodeStack.Item, LambdaInfo> lambdaFunctions = new HashMap<>();
    private Map<Method, LambdaCallInfo> lambdaCalledInDoPrivileged = new HashMap<>();

    private Stack<String> parameterNameStack = new Stack<>();

    private LambdaInfo currentLambda = null;

    private boolean isDoPrivileged = false;
    private boolean isDoPrivilegedRun = false;
    private boolean isLambdaCalledInDoPrivileged = false;

    private final BugAccumulator bugAccumulator;

    public FindPotentialSecurityCheckBasedOnUntrustedSource(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(JavaClass obj) {
        nonFinalMethodsCalledOnParam.clear();
        isDoPrivileged = Subtypes2.instanceOf(getDottedClassName(), "java.security.PrivilegedAction")
                || Subtypes2.instanceOf(getDottedClassName(), "java.security.PrivilegedExceptionAction");
    }

    @Override
    public void visit(Method obj) {
        isDoPrivilegedRun = isDoPrivileged && "run".equals(getMethodName())
                && getMethodSig().startsWith("()");
        isLambdaCalledInDoPrivileged = lambdaCalledInDoPrivileged.containsKey(obj);
    }

    @Override
    public void visit(Code obj) {
        if (!isDoPrivilegedRun && !isLambdaCalledInDoPrivileged &&
                (!getThisClass().isPublic() || !getMethod().isPublic())) {
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
        currentLambda = null;

        if (seen == Const.INVOKEDYNAMIC) {
            ConstantInvokeDynamic constDyn = (ConstantInvokeDynamic) getConstantRefOperand();
            for (Attribute attr : getThisClass().getAttributes()) {
                if (attr instanceof BootstrapMethods) {
                    Optional<Method> method = BootstrapMethodsUtil.getMethodFromBootstrap((BootstrapMethods) attr,
                            constDyn.getBootstrapMethodAttrIndex(), getConstantPool(), getThisClass());
                    if (method.isPresent()) {
                        String[] paramNames = getParamNames();
                        currentLambda = new LambdaInfo(method.get(), paramNames);
                        break;
                    }
                }
            }
        } else if (seen == Const.INVOKEVIRTUAL && getXClassOperand() != null && getXMethodOperand() != null) {
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

            if (isLambdaCalledInDoPrivileged) {
                LambdaCallInfo lambdaCall = lambdaCalledInDoPrivileged.get(getMethod());
                if (!getXClassOperand().isFinal() && !getXMethodOperand().isFinal() && object.isInitialParameter()
                        && isLambdaNestingMethodLocalVariable(object, lambdaCall)) {
                    CalleeInfo callBefore = lookForCalledOutside(lambdaCall.callerClass, lambdaCall.callerMethod,
                            getXClassOperand(), getXMethodOperand(),
                            lambdaCall.argumentNames[object.getRegisterNumber()]);
                    if (callBefore != null) {
                        reportBug(lambdaCall.callerClass, lambdaCall.callerMethod, lambdaCall.srcLine, callBefore,
                                SourceLineAnnotation.fromVisitedInstruction(this));
                    }
                }
                return;
            }

            if (getXClass().isPublic() && getXMethod().isPublic() && !getXClassOperand().isFinal()
                    && !getXMethodOperand().isFinal() && object.isInitialParameter() && object.getXField() == null
                    && !parameterNameStack.empty() && parameterNameStack.peek() != null) {
                addToNonFinalMethodsCalledOnParam(getClassDescriptorOperand(), getXMethodOperand(), object);
            }
        } else if (seen == Const.INVOKESTATIC && getXMethodOperand() != null
                && "doPrivileged".equals(getXMethodOperand().getName())) {
            OpcodeStack.Item action = stack.getStackItem(0);
            CallPair callPair = lookForCalledOutsideAndInside(action);
            if (callPair != null) {
                reportBug(callPair);
                return;
            }

            LambdaInfo lambda = lambdaFunctions.get(action);
            if (lambda != null) {
                lambdaCalledInDoPrivileged.put(lambda.lambdaMethod, new LambdaCallInfo(getThisClass(), getXMethod(),
                        SourceLineAnnotation.fromVisitedInstruction(this), lambda.argumentNames));
            }
        }
    }

    private String[] getParamNames() {
        String[] names = new String[stack.getStackDepth()];
        for (int i = 0; i < stack.getStackDepth() && i < parameterNameStack.size(); ++i) {
            OpcodeStack.Item param = stack.getStackItem(i);
            if (param.isInitialParameter() && param.getXField() == null) {
                names[i] = parameterNameStack.pop();
            }
        }
        return names;
    }

    private boolean isNestingMethodLocalVariable(OpcodeStack.Item object) {
        XField field = object.getXField();
        if (field == null) {
            return false;
        }

        return NESTED_CLASS_VARIABLE_NAME_PATTERN.matcher(field.getName()).matches();
    }

    private boolean isLambdaNestingMethodLocalVariable(OpcodeStack.Item object, LambdaCallInfo lambdaCall) {
        if (object.getRegisterNumber() < 0) {
            return false;
        }

        return object.getRegisterNumber() <= lambdaCall.argumentNames.length
                && lambdaCall.argumentNames[object.getRegisterNumber()] != null;
    }

    private void addToMethodsCalledInsidePrivilegedAction(XMethod calledMethod, OpcodeStack.Item object) {
        Set<CallerInfo> objects = methodsCalledInsidePrivilegedAction.computeIfAbsent(calledMethod,
                k -> new HashSet<>());
        objects.add(new CallerInfo(object, getThisClass(),
                SourceLineAnnotation.fromVisitedInstruction(this)));
    }

    private void addToNonFinalMethodsCalledOnParam(ClassDescriptor calledClass, XMethod calledMethod,
            OpcodeStack.Item object) {
        Set<CalleeInfo> objects = nonFinalMethodsCalledOnParam.computeIfAbsent(getXMethod(), k -> new HashSet<>());
        objects.add(new CalleeInfo(calledClass, calledMethod, parameterNameStack.peek(),
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

    private CalleeInfo lookForCalledOutside(JavaClass callerClass, XMethod callerMethod, XClass calledClass,
            XMethod calledMethod, String argumentName) {
        Set<CalleeInfo> callees = nonFinalMethodsCalledOnParam.get(callerMethod);
        if (callees == null) {
            return null;
        }

        for (CalleeInfo calleeInfo : callees) {
            if (calleeInfo.calledMethod == calledMethod && calleeInfo.calledOnName.equals(argumentName)) {
                return calleeInfo;
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
        bugAccumulator.accumulateBug(new BugInstance(this, "USC_POTENTIAL_SECURITY_CHECK_BASED_ON_UNTRUSTED_SOURCE",
                NORMAL_PRIORITY)
                .addClassAndMethod(this)
                .addSourceLine(this)
                .addClass(callPair.outside.calledClass.getClassName())
                .addCalledMethod(callPair.outside.calledClass.getClassName(),
                        callPair.outside.calledMethod.getName(), callPair.outside.calledMethod.getSignature(),
                        callPair.outside.calledMethod.isStatic())
                .addSourceLine(callPair.outside.srcLine)
                .addSourceLine(callPair.inside.srcLine), this);
    }

    private void reportBug(JavaClass cls, XMethod method, SourceLineAnnotation srcLine,
            CalleeInfo calleInfo, SourceLineAnnotation insideSrcLine) {
        bugAccumulator.accumulateBug(new BugInstance(this, "USC_POTENTIAL_SECURITY_CHECK_BASED_ON_UNTRUSTED_SOURCE",
                NORMAL_PRIORITY)
                .addClass(cls)
                .addMethod(method)
                .addSourceLine(srcLine)
                .addClass(calleInfo.calledClass.getClassName())
                .addCalledMethod(calleInfo.calledClass.getClassName(),
                        calleInfo.calledMethod.getName(), calleInfo.calledMethod.getSignature(),
                        calleInfo.calledMethod.isStatic())
                .addSourceLine(calleInfo.srcLine)
                .addSourceLine(insideSrcLine), this);
    }

    @Override
    public void afterOpcode(int seen) {
        super.afterOpcode(seen);

        if (currentLambda != null && stack.getStackDepth() > 0) {
            lambdaFunctions.put(stack.getStackItem(0), currentLambda);
            return;
        }

        if (seen != Const.ALOAD && seen != Const.ALOAD_0 && seen != Const.ALOAD_1 && seen != Const.ALOAD_2
                && seen != Const.ALOAD_3) {
            parameterNameStack.clear();
        }

        LocalVariable localVar = null;
        if (getMethod().getLocalVariableTable() != null) {
            if (seen == Const.ALOAD_1 && getXMethod().getNumParams() >= 1) {
                localVar = getMethod().getLocalVariableTable().getLocalVariable(1, getPC());
            } else if (seen == Const.ALOAD_2 && getXMethod().getNumParams() >= 2) {
                localVar = getMethod().getLocalVariableTable().getLocalVariable(2, getPC());
            } else if (seen == Const.ALOAD_3 && getXMethod().getNumParams() >= 3) {
                localVar = getMethod().getLocalVariableTable().getLocalVariable(3, getPC());
            } else if (seen == Const.ALOAD && getXMethod().getNumParams() >= getRegisterOperand()) {
                localVar = getMethod().getLocalVariableTable().getLocalVariable(getRegisterOperand(), getPC());
            }
        }

        if (localVar == null) {
            parameterNameStack.push(null);
            return;
        }

        parameterNameStack.push(localVar.getName());
    }
}
