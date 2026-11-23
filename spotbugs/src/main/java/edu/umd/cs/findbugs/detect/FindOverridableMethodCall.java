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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.BootstrapMethods;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantNameAndType;
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
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
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

    private static final class ConstructorReference {
        final int bootstrapIndex;
        final SourceLineAnnotation sourceLine;
        final boolean directStorage;
        boolean stored;
        boolean possiblyInvoked;

        ConstructorReference(int bootstrapIndex, SourceLineAnnotation sourceLine, boolean directStorage) {
            this.bootstrapIndex = bootstrapIndex;
            this.sourceLine = sourceLine;
            this.directStorage = directStorage;
        }
    }

    private static final class DeferredValue {
        final String interfaceSignature;
        final Set<ConstructorReference> references;

        DeferredValue(String interfaceSignature, Set<ConstructorReference> references) {
            this.interfaceSignature = interfaceSignature;
            this.references = references;
        }
    }

    // OpcodeStack items retain the producing bytecode offset through casts and local variables.
    private final Map<Integer, ConstructorReference> constructorReferences = new LinkedHashMap<>();
    private final Map<Integer, DeferredValue> deferredValues = new HashMap<>();
    private final Map<FieldDescriptor, Set<ConstructorReference>> storedReferences = new HashMap<>();
    private boolean uncertainFlow;

    private static final String FUNCTION_PACKAGE = "java/util/function/";
    private static final String PREDICATE = FUNCTION_PACKAGE + "Predicate";
    private static final String FUNCTION = FUNCTION_PACKAGE + "Function";

    private static final String CONSTRUCTOR_BUG = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR";
    private static final String CLONE_BUG = "MC_OVERRIDABLE_METHOD_CALL_IN_CLONE";
    private static final String READ_OBJECT_BUG = "MC_OVERRIDABLE_METHOD_CALL_IN_READ_OBJECT";

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
            Optional<Method> method = BootstrapMethodsUtil.getMethodFromBootstrap(obj, i, getConstantPool(), getThisClass());
            if (method.isEmpty()) {
                continue;
            }
            XMethod xMethod = getXClass().findMethod(method.get().getName(), method.get().getSignature(), method.get().isStatic());
            if (ctor != null
                    && checkAndRecordDirectCase(ctor.method, xMethod, CONSTRUCTOR_BUG, LOW_PRIORITY, ctor.sourceLine)) {
                checkAndRecordCallFromConstructor(ctor.method, xMethod, ctor.sourceLine);
            }
            if (clone != null
                    && checkAndRecordDirectCase(clone.method, xMethod, CLONE_BUG, NORMAL_PRIORITY, clone.sourceLine)) {
                checkAndRecordCallFromClone(clone.method, xMethod, clone.sourceLine);
            }
            if (readObject != null
                    && checkAndRecordDirectCase(readObject.method, xMethod, READ_OBJECT_BUG, NORMAL_PRIORITY, readObject.sourceLine)) {
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
    public void visit(Method method) {
        super.visit(method);
        uncertainFlow = method.getCode() != null && method.getCode().getExceptionTable().length > 0;
    }

    @Override
    public void visitAfter(Method method) {
        for (ConstructorReference ref : constructorReferences.values()) {
            // At joins we may lose provenance. Only preserve the immediate field-store exemption in that case.
            if (!ref.stored || ref.possiblyInvoked || (uncertainFlow && !ref.directStorage)) {
                refCallerConstructors.put(ref.bootstrapIndex, new CallerInfo(getXMethod(), ref.sourceLine));
            }
        }
        constructorReferences.clear();
        deferredValues.clear();
        storedReferences.clear();
    }

    @Override
    public void sawOpcode(int seen) {
        if (getXClass().isFinal()) {
            return;
        }
        XMethod caller = getXMethod();
        boolean inConstructor = Const.CONSTRUCTOR_NAME.equals(getMethodName());
        if (inConstructor) {
            trackConstructorReferences(seen);
        }

        if (seen == Const.INVOKEDYNAMIC && !constructorReferences.containsKey(getPC()) && stack.getStackDepth() != 0
                && getNextOpcode() != Const.PUTFIELD) {
            ConstantInvokeDynamic constDyn = (ConstantInvokeDynamic) getConstantRefOperand();
            OpcodeStack.Item item = stack.getStackItem(0);
            if ("clone".equals(getMethodName())
                    && (("()" + getClassDescriptor().getSignature()).equals(getMethodSig())
                            || "()Ljava/lang/Object;".equals(getMethodSig()))
                    && item.getReturnValueOf() != null
                    && item.getReturnValueOf().equals(superClone(getXClass()))) {
                refCallerClones.put(constDyn.getBootstrapMethodAttrIndex(),
                        new CallerInfo(caller, SourceLineAnnotation.fromVisitedInstruction(this)));
            } else if (isCurrentMethodReadObject()) {
                refCallerReadObjects.put(constDyn.getBootstrapMethodAttrIndex(),
                        new CallerInfo(caller, SourceLineAnnotation.fromVisitedInstruction(this)));
            } else {
                refCalleeToCallerMap.add(constDyn.getBootstrapMethodAttrIndex(), caller);
            }
        }
        if (seen == Const.INVOKEINTERFACE || seen == Const.INVOKEVIRTUAL) {
            XMethod method = getXMethodOperand();
            if (method == null) {
                return;
            }

            int argumentCount = getNumberArguments(getSigConstantOperand());
            OpcodeStack.Item item = stack.getStackItem(argumentCount);

            if (item.getRegisterNumber() == 0 && Const.CONSTRUCTOR_NAME.equals(getMethodName())) {
                if (checkAndRecordDirectCase(caller, method, CONSTRUCTOR_BUG, LOW_PRIORITY, SourceLineAnnotation.fromVisitedInstruction(
                        this))) {
                    checkAndRecordCallFromConstructor(caller, method, SourceLineAnnotation.fromVisitedInstruction(this));
                }

            } else if ("clone".equals(getMethodName())
                    && (("()" + getClassDescriptor().getSignature()).equals(getMethodSig())
                            || "()Ljava/lang/Object;".equals(getMethodSig()))
                    && item.getReturnValueOf() != null
                    && item.getReturnValueOf().equals(superClone(getXClass()))) {
                if (checkAndRecordDirectCase(caller, method, CLONE_BUG, NORMAL_PRIORITY, SourceLineAnnotation.fromVisitedInstruction(this))) {
                    checkAndRecordCallFromClone(caller, method, SourceLineAnnotation.fromVisitedInstruction(this));
                }
            } else if (isCurrentMethodReadObject()) {
                if (checkAndRecordDirectCase(caller, method, READ_OBJECT_BUG, NORMAL_PRIORITY, SourceLineAnnotation.fromVisitedInstruction(this))) {
                    checkAndRecordCallFromReadObject(caller, method, SourceLineAnnotation.fromVisitedInstruction(this));
                }
            } else if (item.getRegisterNumber() == 0
                    && (caller.isPrivate() || caller.isFinal())) {
                if (method.isPrivate() || method.isFinal()) {
                    checkAndRecordCallBetweenNonOverridableMethods(caller, method);
                } else {
                    checkAndRecordCallToOverridable(caller, method);
                }
            }
        }
    }

    private void trackConstructorReferences(int opcode) {
        uncertainFlow |= isBranch(opcode) || isSwitch(opcode);
        switch (opcode) {
        case Const.INVOKEDYNAMIC:
            trackDynamicReference();
            break;
        case Const.INVOKESTATIC:
        case Const.INVOKEINTERFACE:
        case Const.INVOKEVIRTUAL:
        case Const.INVOKESPECIAL:
            trackReferenceInvocation(opcode == Const.INVOKESTATIC);
            break;
        case Const.PUTFIELD:
            DeferredValue value = deferredValues.get(stack.getStackItem(0).getPC());
            if (value != null && stack.getStackItem(1).getRegisterNumber() == 0) {
                value.references.forEach(ref -> ref.stored = true);
                storedReferences.computeIfAbsent(getFieldDescriptorOperand(), field -> new HashSet<>()).addAll(value.references);
            }
            break;
        case Const.GETFIELD:
            markPossiblyInvoked(storedReferences.getOrDefault(getFieldDescriptorOperand(), Set.of()));
            break;
        case Const.AASTORE:
        case Const.PUTSTATIC:
            markPossiblyInvoked(referencesAtStackOffset(0));
            break;
        default:
            break;
        }
    }

    private void trackDynamicReference() {
        String signature = getSigConstantOperand();
        int argumentCount = getNumberArguments(signature);
        if (stack.getStackDepth() < argumentCount) {
            uncertainFlow = true;
            return;
        }
        for (int i = 0; i < argumentCount; i++) {
            markPossiblyInvoked(referencesAtStackOffset(i));
        }
        int index = ((ConstantInvokeDynamic) getConstantRefOperand()).getBootstrapMethodAttrIndex();
        Set<ConstructorReference> references = Set.of();
        if (argumentCount > 0 && stack.getStackItem(0).getRegisterNumber() == 0) {
            ConstructorReference reference = new ConstructorReference(index, SourceLineAnnotation.fromVisitedInstruction(this),
                    getNextOpcode() == Const.PUTFIELD);
            constructorReferences.put(getPC(), reference);
            references = Set.of(reference);
        }
        String result = signature.substring(signature.indexOf(')') + 1);
        if (result.startsWith("L")) {
            boolean exactType = isLambdaMetafactory(index);
            if (exactType || !references.isEmpty()) {
                deferredValues.put(getPC(), new DeferredValue(exactType ? result : "", references));
            }
        }
    }

    private boolean isLambdaMetafactory(int index) {
        for (Attribute attribute : getThisClass().getAttributes()) {
            if (attribute instanceof BootstrapMethods) {
                int methodIndex = ((BootstrapMethods) attribute).getBootstrapMethods()[index].getBootstrapMethodRef();
                Constant handle = getConstantPool().getConstant(methodIndex);
                if (!(handle instanceof ConstantMethodHandle)) {
                    return false;
                }
                Constant reference = getConstantPool().getConstant(((ConstantMethodHandle) handle).getReferenceIndex());
                if (!(reference instanceof ConstantCP)) {
                    return false;
                }
                ConstantCP method = (ConstantCP) reference;
                ConstantNameAndType nameAndType = getConstantPool().getConstant(method.getNameAndTypeIndex());
                // The alternate bootstrap can add interfaces beyond the result type.
                return "java.lang.invoke.LambdaMetafactory".equals(method.getClass(getConstantPool()))
                        && "metafactory".equals(nameAndType.getName(getConstantPool()));
            }
        }
        return false;
    }

    private void trackReferenceInvocation(boolean isStatic) {
        String signature = getSigConstantOperand();
        int argumentCount = getNumberArguments(signature);
        int consumed = argumentCount + (isStatic ? 0 : 1);
        if (stack.getStackDepth() < consumed) {
            uncertainFlow = true;
            return;
        }
        Set<ConstructorReference> references = new HashSet<>();
        for (int i = 0; i < consumed; i++) {
            references.addAll(referencesAtStackOffset(i));
        }
        String result = signature.substring(signature.indexOf(')') + 1);
        if (isDeferredCombinator(isStatic, argumentCount, result)) {
            deferredValues.put(getPC(), new DeferredValue(result, references));
            return;
        }
        markPossiblyInvoked(references);
        if (!storedReferences.isEmpty()) {
            for (int i = 0; i < consumed; i++) {
                if (stack.getStackItem(i).getRegisterNumber() == 0) {
                    // A call on or with the under-construction object may use its stored references.
                    storedReferences.values().forEach(FindOverridableMethodCall::markPossiblyInvoked);
                    break;
                }
            }
        }
    }

    private boolean isDeferredCombinator(boolean isStatic, int argumentCount, String result) {
        String owner = getClassConstantOperand();
        String name = getNameConstantOperand();
        String signature = getSigConstantOperand();
        if (isStatic) {
            if (FUNCTION.equals(owner) && "identity".equals(name) && "()Ljava/util/function/Function;".equals(signature)) {
                return true;
            }
            return PREDICATE.equals(owner) && "not".equals(name)
                    && "(Ljava/util/function/Predicate;)Ljava/util/function/Predicate;".equals(signature)
                    && usesJdkDefault(deferredValues.get(stack.getStackItem(0).getPC()), PREDICATE, "negate",
                            "()Ljava/util/function/Predicate;");
        }
        if (!result.startsWith("L" + FUNCTION_PACKAGE)) {
            return false;
        }
        String defaultOwner = result.substring(1, result.length() - 1);
        String simpleName = defaultOwner.substring(FUNCTION_PACKAGE.length());
        boolean knownDefault;
        switch (simpleName) {
        case "Predicate":
        case "BiPredicate":
        case "IntPredicate":
        case "LongPredicate":
        case "DoublePredicate":
            knownDefault = ("negate".equals(name) && ("()" + result).equals(signature))
                    || (("and".equals(name) || "or".equals(name)) && ("(" + result + ")" + result).equals(signature));
            break;
        case "Consumer":
        case "BiConsumer":
        case "IntConsumer":
        case "LongConsumer":
        case "DoubleConsumer":
            knownDefault = "andThen".equals(name) && ("(" + result + ")" + result).equals(signature);
            break;
        case "Function":
            knownDefault = ("compose".equals(name) || "andThen".equals(name)) && ("(" + result + ")" + result).equals(signature);
            break;
        case "BiFunction":
            knownDefault = "andThen".equals(name) && ("(Ljava/util/function/Function;)" + result).equals(signature);
            break;
        default:
            return false;
        }
        return knownDefault && usesJdkDefault(deferredValues.get(stack.getStackItem(argumentCount).getPC()), defaultOwner, name, signature);
    }

    private boolean usesJdkDefault(DeferredValue receiver, String owner, String name, String signature) {
        if (receiver == null || receiver.interfaceSignature.isEmpty()) {
            return false;
        }
        String receiverClass = receiver.interfaceSignature.substring(1, receiver.interfaceSignature.length() - 1);
        if (owner.equals(receiverClass)) {
            return true;
        }
        ClassDescriptor descriptor = DescriptorFactory.instance().getClassDescriptor(receiverClass);
        try {
            return Hierarchy.isSubtype(descriptor.getDottedClassName(), owner.replace('/', '.'))
                    && !overridesDefault(descriptor, owner, name, signature, new HashSet<>());
        } catch (ClassNotFoundException | CheckedAnalysisException e) {
            return false;
        }
    }

    private boolean overridesDefault(ClassDescriptor type, String owner, String name, String signature, Set<ClassDescriptor> seen)
            throws CheckedAnalysisException {
        if (owner.equals(type.getClassName()) || !seen.add(type)) {
            return false;
        }
        XClass xClass = type.getXClass();
        if (xClass.findMethod(name, signature, false) != null) {
            return true;
        }
        for (ClassDescriptor parent : xClass.getInterfaceDescriptorList()) {
            if (overridesDefault(parent, owner, name, signature, seen)) {
                return true;
            }
        }
        return false;
    }

    private Set<ConstructorReference> referencesAtStackOffset(int offset) {
        DeferredValue value = deferredValues.get(stack.getStackItem(offset).getPC());
        return value == null ? Set.of() : value.references;
    }

    private static void markPossiblyInvoked(Set<ConstructorReference> references) {
        references.forEach(ref -> ref.possiblyInvoked = true);
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

    boolean checkAndRecordDirectCase(XMethod caller, XMethod method, String bugType, int priority, SourceLineAnnotation sourceLine) {
        if (!method.isPrivate() && !method.isFinal() && isSelfCall(method)) {
            bugAccumulator.accumulateBug(new BugInstance(this, bugType, priority)
                    .addClass(this)
                    .addMethod(caller)
                    .addCalledMethod(method), sourceLine);
            return false;
        }
        return true;
    }

    private boolean isSelfCall(XMethod method) {
        // We're only interested in method calls on the object itself
        // Calling ObjectInputStream.readInt() is not considered risky here because we assume that the object stream is not under the control of the attacker.
        // Checking for vulnerabilities when the object stream IS under control of the attacker is beyond the scope of this detector.
        @DottedClassName
        String className = getClassContext().getClassDescriptor().getDottedClassName();
        @DottedClassName
        String methodClassName = method.getClassName();

        try {
            return className.equals(methodClassName) || Hierarchy.isSubtype(className, methodClassName);
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);

            return false;
        }
    }

    private void checkAndRecordCallFromConstructor(XMethod constructor, XMethod callee, SourceLineAnnotation sourceLine) {
        XMethod overridable = getIndirectlyCalledOverridable(callee);
        if (overridable != null && isSelfCall(overridable)) {
            bugAccumulator.accumulateBug(new BugInstance(this, CONSTRUCTOR_BUG, LOW_PRIORITY)
                    .addClass(this)
                    .addMethod(constructor)
                    .addCalledMethod(overridable), sourceLine);
            return;
        }
        callerConstructors.put(callee, new CallerInfo(constructor, sourceLine));
    }

    private void checkAndRecordCallFromClone(XMethod clone, XMethod callee, SourceLineAnnotation sourceLine) {
        XMethod overridable = getIndirectlyCalledOverridable(callee);
        if (overridable != null && isSelfCall(overridable)) {
            bugAccumulator.accumulateBug(new BugInstance(this, CLONE_BUG, NORMAL_PRIORITY)
                    .addClass(this)
                    .addMethod(clone)
                    .addCalledMethod(overridable), sourceLine);
            return;
        }
        callerClones.put(callee, new CallerInfo(clone, sourceLine));
    }

    private void checkAndRecordCallFromReadObject(XMethod readObject, XMethod callee, SourceLineAnnotation sourceLine) {
        XMethod overridable = getIndirectlyCalledOverridable(callee);
        if (overridable != null && isSelfCall(overridable)) {
            bugAccumulator.accumulateBug(new BugInstance(this, READ_OBJECT_BUG, NORMAL_PRIORITY)
                    .addClass(this)
                    .addMethod(readObject)
                    .addCalledMethod(overridable), sourceLine);
            return;
        }
        callerReadObjects.put(callee, new CallerInfo(readObject, sourceLine));
    }

    private void checkAndRecordCallToOverridable(XMethod caller, XMethod overridable) {
        CallerInfo constructor = getIndirectCallerConstructor(caller);
        if (constructor != null && isSelfCall(overridable)) {
            bugAccumulator.accumulateBug(new BugInstance(this, CONSTRUCTOR_BUG, LOW_PRIORITY)
                    .addClassAndMethod(constructor.method)
                    .addCalledMethod(overridable), constructor.sourceLine);
        }

        CallerInfo clone = getIndirectCallerClone(caller);
        if (clone != null && isSelfCall(overridable)) {
            bugAccumulator.accumulateBug(new BugInstance(this, CLONE_BUG, NORMAL_PRIORITY)
                    .addClassAndMethod(clone.method)
                    .addCalledMethod(overridable), clone.sourceLine);
        }

        CallerInfo readObject = getIndirectCallerReadObject(caller);
        if (readObject != null && isSelfCall(overridable)) {
            bugAccumulator.accumulateBug(new BugInstance(this, READ_OBJECT_BUG, NORMAL_PRIORITY)
                    .addClassAndMethod(readObject.method)
                    .addCalledMethod(overridable), readObject.sourceLine);
        }

        if ((constructor != null || clone != null || readObject != null) && isSelfCall(overridable)) {
            return;
        }

        callsToOverridable.put(caller, overridable);
    }

    private void checkAndRecordCallBetweenNonOverridableMethods(XMethod caller, XMethod callee) {
        CallerInfo constructor = getIndirectCallerConstructor(caller);
        CallerInfo clone = getIndirectCallerClone(caller);
        CallerInfo readObject = getIndirectCallerReadObject(caller);

        if (constructor != null || clone != null || readObject != null) {
            XMethod overridable = getIndirectlyCalledOverridable(callee);
            if (overridable != null) {
                if (constructor != null && isSelfCall(overridable)) {
                    bugAccumulator.accumulateBug(new BugInstance(this, CONSTRUCTOR_BUG, LOW_PRIORITY)
                            .addClassAndMethod(constructor.method)
                            .addCalledMethod(overridable),
                            constructor.sourceLine);

                }

                if (clone != null && isSelfCall(overridable)) {
                    bugAccumulator.accumulateBug(new BugInstance(this, CLONE_BUG, NORMAL_PRIORITY)
                            .addClassAndMethod(clone.method)
                            .addCalledMethod(overridable),
                            clone.sourceLine);
                }

                if (readObject != null && isSelfCall(overridable)) {
                    bugAccumulator.accumulateBug(new BugInstance(this, READ_OBJECT_BUG, NORMAL_PRIORITY)
                            .addClassAndMethod(readObject.method)
                            .addCalledMethod(overridable),
                            readObject.sourceLine);
                }

                return;
            }
        }

        callerToCalleeMap.add(caller, callee);
        calleeToCallerMap.add(callee, caller);
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
