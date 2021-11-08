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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.BootstrapMethods;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
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
    private static final Map<XMethod, CallerInfo> callerConstructors = new HashMap<>();
    private static final Map<XMethod, CallerInfo> callerClones = new HashMap<>();
    private static final Map<XMethod, XMethod> callsToOverridable = new HashMap<>();
    private static final MultiMap<XMethod, XMethod> callerToCalleeMap = new MultiMap<>(ArrayList.class);
    private static final MultiMap<XMethod, XMethod> calleeToCallerMap = new MultiMap<>(ArrayList.class);

    // For methods called using method references
    private static final Map<Integer, CallerInfo> refCallerConstructors = new HashMap<>();
    private static final Map<Integer, CallerInfo> refCallerClones = new HashMap<>();
    private static final MultiMap<Integer, XMethod> refCalleeToCallerMap = new MultiMap<>(ArrayList.class);


    private final BugAccumulator bugAccumulator;

    public FindOverridableMethodCall(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(JavaClass obj) {
        super.visit(obj);
        callerConstructors.clear();
        callerClones.clear();
        callsToOverridable.clear();
        callerToCalleeMap.clear();
        calleeToCallerMap.clear();
        refCallerConstructors.clear();
        refCallerClones.clear();
        refCalleeToCallerMap.clear();
    }

    @Override
    public void visitBootstrapMethods​(BootstrapMethods obj) {
        for (int i = 0; i < obj.getBootstrapMethods().length; ++i) {
            BootstrapMethod bm = obj.getBootstrapMethods()[i];
            CallerInfo ctor = refCallerConstructors.get(i);
            CallerInfo clone = refCallerClones.get(i);
            Collection<XMethod> callers = refCalleeToCallerMap.get(i);
            if (ctor == null && clone == null && (callers == null || callers.isEmpty())) {
                continue;
            }
            for (int arg : bm.getBootstrapArguments()) {
                Constant c = obj.getConstantPool().getConstant(arg);
                if (!(c instanceof ConstantMethodHandle)) {
                    continue;
                }
                ConstantMethodHandle cmh = (ConstantMethodHandle) c;
                c = getConstantPool().getConstant(cmh.getReferenceIndex());
                if (!(c instanceof ConstantMethodref) && !(c instanceof ConstantInterfaceMethodref)) {
                    continue;
                }
                ConstantCP cp = (ConstantCP) c;
                if (cp.getClassIndex() != getThisClass().getClassNameIndex()) {
                    return;
                }
                ConstantNameAndType cnat = (ConstantNameAndType) getConstantPool()
                        .getConstant(cp.getNameAndTypeIndex());
                Optional<Method> metOpt = Arrays.stream(getThisClass().getMethods())
                        .filter(m -> m.getNameIndex() == cnat.getNameIndex()
                                && m.getSignatureIndex() == cnat.getSignatureIndex())
                        .findAny();
                if (!metOpt.isPresent()) {
                    continue;
                }
                XMethod method = getXClass().findMethod(metOpt.get().getName(), metOpt.get().getSignature(),
                        metOpt.get().isStatic());
                if (ctor != null && checkDirectCase(ctor.method, method, "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR",
                        LOW_PRIORITY, ctor.sourceLine)) {
                    checkAndRecordCallFromConstructor(ctor.method, method, ctor.sourceLine);
                }
                if (clone != null && checkDirectCase(clone.method, method, "MC_OVERRIDABLE_METHOD_CALL_IN_CLONE",
                        NORMAL_PRIORITY, clone.sourceLine)) {
                    checkAndRecordCallFromClone(clone.method, method, clone.sourceLine);
                }
                if (callers != null) {
                    for (XMethod caller : callers) {
                        if (method.isPrivate() || method.isFinal()) {
                            checkAndRecordCallBetweenNonOverridableMethods(caller, method);
                        } else {
                            checkAndRecordCallToOverridable(caller, method);
                        }
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
        if (seen == Const.INVOKEDYNAMIC) {
            ConstantInvokeDynamic constDyn = (ConstantInvokeDynamic) getConstantRefOperand();
            if (stack.getStackDepth() == 0) {
                return;
            }
            OpcodeStack.Item item = stack.getStackItem(0);

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

        if (constructor != null || clone != null) {
            return false;
        }

        callsToOverridable.put(caller, overridable);
        return true;
    }

    private boolean checkAndRecordCallBetweenNonOverridableMethods(XMethod caller, XMethod callee) {
        CallerInfo constructor = getIndirectCallerConstructor(caller);
        CallerInfo clone = getIndirectCallerClone(caller);

        if (constructor != null || clone != null) {
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

                return false;
            }
        }

        callerToCalleeMap.add(caller, callee);
        calleeToCallerMap.add(callee, caller);
        return true;
    }

    private XMethod getIndirectlyCalledOverridable(XMethod caller) {
        return getIndirectlyCalledOverridable(caller, new HashSet<XMethod>());
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

    private CallerInfo getIndirectCallerSpecial(XMethod callee, Map<XMethod, CallerInfo> map) {
        return getIndirectCallerSpecial(callee, map, new HashSet<XMethod>());
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
