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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethods;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.BootstrapMethodsUtil;
import edu.umd.cs.findbugs.util.MultiMap;

public class FindOverridableMethodCall extends OpcodeStackDetector {

    private static class CallerInfo {
        XMethod method;
        SourceLineAnnotation sourceLine;

        CallerInfo(XMethod m, SourceLineAnnotation sl) {
            method = m;
            sourceLine = sl;
        }
    }

    // For methods called using the standard way
    private final Map<XMethod, CallerInfo> callerConstructors = new HashMap<>();
    private final Map<XMethod, CallerInfo> callerClones = new HashMap<>();
    private final Map<XMethod, CallerInfo> callerReadObjects = new HashMap<>();
    private final Map<XMethod, XMethod> callsToOverridable = new HashMap<>();
    private final MultiMap<XMethod, XMethod> callerToCalleeMap = new MultiMap<>(ArrayList.class);
    private final MultiMap<XMethod, XMethod> calleeToCallerMap = new MultiMap<>(ArrayList.class);

    // For methods called using method references
    private final Map<Integer, CallerInfo> refCallerConstructors = new HashMap<>();
    private final Map<Integer, CallerInfo> refCallerClones = new HashMap<>();
    private final Map<Integer, CallerInfo> refCallerReadObjects = new HashMap<>();
    private final MultiMap<Integer, XMethod> refCalleeToCallerMap = new MultiMap<>(ArrayList.class);


    private final BugAccumulator bugAccumulator;

    public FindOverridableMethodCall(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(JavaClass obj) {
        super.visit(obj);
        callerConstructors.clear();
        callerClones.clear();
        callerReadObjects.clear();
        callsToOverridable.clear();
        callerToCalleeMap.clear();
        calleeToCallerMap.clear();
        refCallerConstructors.clear();
        refCallerClones.clear();
        refCallerReadObjects.clear();
        refCalleeToCallerMap.clear();
    }

    @Override
    public void visitBootstrapMethods(BootstrapMethods obj) {
        if (getXClass().isFinal()) {
            return;
        }
        for (int i = 0; i < obj.getBootstrapMethods().length; ++i) {
            CallerInfo ctor = refCallerConstructors.get(i);
            CallerInfo clone = refCallerClones.get(i);
            CallerInfo readObject = refCallerReadObjects.get(i);
            Collection<XMethod> callers = refCalleeToCallerMap.get(i);
            if (ctor == null && clone == null && readObject == null && (callers == null || callers.isEmpty())) {
                continue;
            }
            Optional<Method> method = BootstrapMethodsUtil.getMethodFromBootstrap(obj, i, getConstantPool(),
                    getThisClass());
            if (!method.isPresent()) {
                continue;
            }
            XMethod xMethod = getXClass().findMethod(method.get().getName(), method.get().getSignature(),
                    method.get().isStatic());
            if (ctor != null && checkDirectCase(ctor.method, xMethod, "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR",
                    LOW_PRIORITY, ctor.sourceLine)) {
                checkAndRecordCallFromConstructor(ctor.method, xMethod, ctor.sourceLine);
            }
            if (clone != null && checkDirectCase(clone.method, xMethod, "MC_OVERRIDABLE_METHOD_CALL_IN_CLONE",
                    NORMAL_PRIORITY, clone.sourceLine)) {
                checkAndRecordCallFromClone(clone.method, xMethod, clone.sourceLine);
            }
            if (readObject != null && reportIfOverridableCallInReadObject(readObject.method, xMethod, readObject.sourceLine)) {
                checkAndRecordCallFromReadObject(readObject.method, xMethod, readObject.sourceLine);
            }
            if (callers != null) {
                for (XMethod caller : callers) {
                    if (xMethod.isPrivate() || xMethod.isFinal()) {
                        checkAndRecordCallBetweenNonOverridableMethods(caller, xMethod);
                    } else {
                        checkAndRecordCallToOverridable(caller, xMethod);
                    }
                }
            }
        }
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
        if (getXClass().isFinal()) {
            return;
        }
        if (seen == Const.INVOKEDYNAMIC) {
            ConstantInvokeDynamic constDyn = (ConstantInvokeDynamic) getConstantRefOperand();
            if (stack.getStackDepth() == 0) {
                return;
            }
            OpcodeStack.Item item = stack.getStackItem(0);

            if (getNextOpcode() == Const.PUTFIELD) {
                // INVOKEDYNAMIC followed by PUTFIELD means that we assigned the method reference to a field
                // But we might be missing calls to that field afterwards, this would be a false negative
                return;
            }

            if (item.getRegisterNumber() == 0 && Const.CONSTRUCTOR_NAME.equals(getMethodName())) {
                refCallerConstructors.put(constDyn.getBootstrapMethodAttrIndex(),
                        new CallerInfo(getXMethod(), SourceLineAnnotation.fromVisitedInstruction(this)));
            } else if ("clone".equals(getMethodName())
                    && (("()" + getClassDescriptor().getSignature()).equals(getMethodSig())
                            || "()Ljava/lang/Object;".equals(getMethodSig()))
                    && item.getReturnValueOf() != null
                    && item.getReturnValueOf().equals(superClone(getXClass()))) {
                refCallerClones.put(constDyn.getBootstrapMethodAttrIndex(),
                        new CallerInfo(getXMethod(), SourceLineAnnotation.fromVisitedInstruction(this)));
            } else if (isCurrentMethodReadObject()) {
                refCallerReadObjects.put(constDyn.getBootstrapMethodAttrIndex(),
                        new CallerInfo(getXMethod(), SourceLineAnnotation.fromVisitedInstruction(this)));
            } else {
                refCalleeToCallerMap.add(constDyn.getBootstrapMethodAttrIndex(), getXMethod());
            }
        }
        if (seen == Const.INVOKEINTERFACE || seen == Const.INVOKEVIRTUAL) {
            XMethod method = getXMethodOperand();
            if (method == null) {
                return;
            }
            OpcodeStack.Item item = stack.getStackItem(0);
            if (item.getRegisterNumber() == 0 && Const.CONSTRUCTOR_NAME.equals(getMethodName())) {
                if (checkDirectCase(getXMethod(), method, "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", LOW_PRIORITY,
                        SourceLineAnnotation.fromVisitedInstruction(this))) {
                    checkAndRecordCallFromConstructor(getXMethod(), method,
                            SourceLineAnnotation.fromVisitedInstruction(this));
                }

            } else if ("clone".equals(getMethodName())
                    && (("()" + getClassDescriptor().getSignature()).equals(getMethodSig())
                            || "()Ljava/lang/Object;".equals(getMethodSig()))
                    && item.getReturnValueOf() != null
                    && item.getReturnValueOf().equals(superClone(getXClass()))) {
                if (checkDirectCase(getXMethod(), method, "MC_OVERRIDABLE_METHOD_CALL_IN_CLONE", NORMAL_PRIORITY,
                        SourceLineAnnotation.fromVisitedInstruction(this))) {
                    checkAndRecordCallFromClone(getXMethod(), method,
                            SourceLineAnnotation.fromVisitedInstruction(this));
                }
            } else if (isCurrentMethodReadObject()) {
                if (reportIfOverridableCallInReadObject(getXMethod(), method,
                        SourceLineAnnotation.fromVisitedInstruction(this))) {
                    checkAndRecordCallFromReadObject(getXMethod(), method,
                            SourceLineAnnotation.fromVisitedInstruction(this));
                }
            } else if (item.getRegisterNumber() == 0
                    && (getXMethod().isPrivate() || getXMethod().isFinal())) {
                if (method.isPrivate() || method.isFinal()) {
                    checkAndRecordCallBetweenNonOverridableMethods(getXMethod(), method);
                } else {
                    checkAndRecordCallToOverridable(getXMethod(), method);
                }
            }
        }
    }

    private boolean isCurrentMethodReadObject() {
        return "readObject".equals(getMethodName()) && "(Ljava/io/ObjectInputStream;)V".equals(getMethodSig());
    }

    private XMethod superClone(XClass clazz) {
        ClassDescriptor superD = clazz.getSuperclassDescriptor();
        XClass xSuper;
        try {
            xSuper = superD.getXClass();
            XMethod cloneMethod = xSuper.findMethod("clone", "()" + superD.getSignature(), false);
            if (cloneMethod == null) {
                cloneMethod = xSuper.findMethod("clone", "()Ljava/lang/Object;", false);
            }
            return cloneMethod;
        } catch (CheckedAnalysisException e) {
            AnalysisContext.logError("Could not find XClass object for " + superD + ".");
            return null;
        }
    }

    boolean checkDirectCase(XMethod caller, XMethod method, String message, int priority,
            SourceLineAnnotation sourceLine) {
        if (!method.isPrivate() && !method.isFinal()) {
            bugAccumulator.accumulateBug(new BugInstance(this, message, priority)
                    .addClass(this).addMethod(caller).addString(method.getName()), sourceLine);
            return false;
        }
        return true;
    }

    private boolean shouldIgnoreCallInReadObject(XMethod method) {
        // We're only interested in method calls on the object itself
        // Calling ObjectInputStream.readInt() is not considered risky here because we assume that the object stream is not under the control of the attacker.
        // Checking for vulnerabilities when the object stream IS under control of the attacker is beyond the scope of this detector.
        @DottedClassName
        String className = getClassContext().getClassDescriptor().getDottedClassName();
        @DottedClassName
        String methodClassName = method.getClassName();

        try {
            return !className.equals(methodClassName) && !Hierarchy.isSubtype(className, methodClassName);
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);

            return true;
        }
    }

    private boolean reportIfOverridableCallInReadObject(XMethod caller, XMethod method, SourceLineAnnotation sourceLine) {
        if (!shouldIgnoreCallInReadObject(method) && !method.isPrivate() && !method.isFinal()) {
            bugAccumulator.accumulateBug(
                    new BugInstance(this, "MC_OVERRIDABLE_METHOD_CALL_IN_READ_OBJECT", NORMAL_PRIORITY)
                            .addClass(this)
                            .addMethod(caller)
                            .addString(method.getName()), sourceLine);
            return false;
        }

        return true;
    }

    private boolean checkAndRecordCallFromConstructor(XMethod constructor, XMethod callee,
            SourceLineAnnotation sourceLine) {
        XMethod overridable = getIndirectlyCalledOverridable(callee);
        if (overridable != null) {
            bugAccumulator.accumulateBug(new BugInstance(this,
                    "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", LOW_PRIORITY)
                    .addClass(this).addMethod(constructor).addString(overridable.getName()), sourceLine);
            return false;
        }
        callerConstructors.put(callee, new CallerInfo(constructor, sourceLine));
        return true;
    }

    private boolean checkAndRecordCallFromClone(XMethod clone, XMethod callee,
            SourceLineAnnotation sourceLine) {
        XMethod overridable = getIndirectlyCalledOverridable(callee);
        if (overridable != null) {
            bugAccumulator.accumulateBug(new BugInstance(this,
                    "MC_OVERRIDABLE_METHOD_CALL_IN_CLONE", NORMAL_PRIORITY)
                    .addClass(this).addMethod(clone).addString(overridable.getName()), sourceLine);
            return false;
        }
        callerClones.put(callee, new CallerInfo(clone, sourceLine));
        return true;
    }

    private void checkAndRecordCallFromReadObject(XMethod readObject, XMethod callee,
            SourceLineAnnotation sourceLine) {
        XMethod overridable = getIndirectlyCalledOverridable(callee);
        if (overridable != null && !shouldIgnoreCallInReadObject(overridable)) {
            bugAccumulator.accumulateBug(new BugInstance(this,
                    "MC_OVERRIDABLE_METHOD_CALL_IN_READ_OBJECT", NORMAL_PRIORITY)
                    .addClass(this).addMethod(readObject).addString(overridable.getName()), sourceLine);
            return;
        }
        callerReadObjects.put(callee, new CallerInfo(readObject, sourceLine));
    }

    private boolean checkAndRecordCallToOverridable(XMethod caller, XMethod overridable) {
        CallerInfo constructor = getIndirectCallerConstructor(caller);
        if (constructor != null) {
            bugAccumulator.accumulateBug(new BugInstance(this,
                    "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", LOW_PRIORITY)
                    .addClassAndMethod(constructor.method).addString(overridable.getName()),
                    constructor.sourceLine);
        }

        CallerInfo clone = getIndirectCallerClone(caller);
        if (clone != null) {
            bugAccumulator.accumulateBug(new BugInstance(this,
                    "MC_OVERRIDABLE_METHOD_CALL_IN_CLONE", NORMAL_PRIORITY)
                    .addClassAndMethod(clone.method).addString(overridable.getName()), clone.sourceLine);
        }

        CallerInfo readObject = getIndirectCallerReadObject(caller);
        if (readObject != null) {
            bugAccumulator.accumulateBug(new BugInstance(this,
                    "MC_OVERRIDABLE_METHOD_CALL_IN_READ_OBJECT", NORMAL_PRIORITY)
                    .addClassAndMethod(readObject.method).addString(overridable.getName()), readObject.sourceLine);
        }

        if (constructor != null || clone != null || readObject != null) {
            return false;
        }

        callsToOverridable.put(caller, overridable);
        return true;
    }

    private boolean checkAndRecordCallBetweenNonOverridableMethods(XMethod caller, XMethod callee) {
        CallerInfo constructor = getIndirectCallerConstructor(caller);
        CallerInfo clone = getIndirectCallerClone(caller);
        CallerInfo readObject = getIndirectCallerReadObject(caller);

        if (constructor != null || clone != null || readObject != null) {
            XMethod overridable = getIndirectlyCalledOverridable(callee);
            if (overridable != null) {
                if (constructor != null) {
                    bugAccumulator.accumulateBug(new BugInstance(this,
                            "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", LOW_PRIORITY)
                            .addClassAndMethod(constructor.method).addString(overridable.getName()),
                            constructor.sourceLine);

                }

                if (clone != null) {
                    bugAccumulator.accumulateBug(new BugInstance(this,
                            "MC_OVERRIDABLE_METHOD_CALL_IN_CLONE", NORMAL_PRIORITY)
                            .addClassAndMethod(clone.method).addString(overridable.getName()),
                            clone.sourceLine);
                }

                if (readObject != null) {
                    bugAccumulator.accumulateBug(new BugInstance(this,
                            "MC_OVERRIDABLE_METHOD_CALL_IN_READ_OBJECT", NORMAL_PRIORITY)
                            .addClassAndMethod(readObject.method).addString(overridable.getName()),
                            readObject.sourceLine);
                }

                return false;
            }
        }

        callerToCalleeMap.add(caller, callee);
        calleeToCallerMap.add(callee, caller);
        return true;
    }

    private XMethod getIndirectlyCalledOverridable(XMethod caller) {
        return getIndirectlyCalledOverridable(caller, new HashSet<>());
    }

    private XMethod getIndirectlyCalledOverridable(XMethod caller, Set<XMethod> visited) {
        XMethod overridable = callsToOverridable.get(caller);
        if (overridable != null) {
            return overridable;
        }

        for (XMethod callee : callerToCalleeMap.get(caller)) {
            if (!visited.contains(callee)) {
                visited.add(callee);
                overridable = getIndirectlyCalledOverridable(callee, visited);
                if (overridable != null) {
                    return overridable;
                }
            }
        }

        return null;
    }

    private CallerInfo getIndirectCallerConstructor(XMethod callee) {
        return getIndirectCallerSpecial(callee, callerConstructors);
    }

    private CallerInfo getIndirectCallerClone(XMethod callee) {
        return getIndirectCallerSpecial(callee, callerClones);
    }

    private CallerInfo getIndirectCallerReadObject(XMethod callee) {
        return getIndirectCallerSpecial(callee, callerReadObjects);
    }

    private CallerInfo getIndirectCallerSpecial(XMethod callee, Map<XMethod, CallerInfo> map) {
        return getIndirectCallerSpecial(callee, map, new HashSet<>());
    }

    private CallerInfo getIndirectCallerSpecial(XMethod callee, Map<XMethod, CallerInfo> map, Set<XMethod> visited) {
        CallerInfo special = map.get(callee);
        if (special != null) {
            return special;
        }

        for (XMethod caller : calleeToCallerMap.get(callee)) {
            if (!visited.contains(caller)) {
                visited.add(caller);
                special = getIndirectCallerSpecial(caller, map, visited);
                if (special != null) {
                    return special;
                }
            }
        }

        return null;
    }
}
