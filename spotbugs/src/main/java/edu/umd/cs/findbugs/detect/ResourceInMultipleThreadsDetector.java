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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.BootstrapMethods;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.ClassMember;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.engine.bcel.FinallyDuplicatesInfoFactory;
import edu.umd.cs.findbugs.util.BootstrapMethodsUtil;
import edu.umd.cs.findbugs.util.MutableClasses;

public class ResourceInMultipleThreadsDetector extends OpcodeStackDetector {

    private static final class FieldData {

        private boolean modified = false;
        private boolean onlySynchronized = true;
        private boolean onlyPutField = true;
        private final Map<Method, Set<BugInstance>> methodBugs = new HashMap<>();
    }

    private final BugReporter bugReporter;

    private final Set<XField> synchronizedCollectionTypedFields = new HashSet<>();
    private final Set<MethodDescriptor> methodsUsedInThreads = new HashSet<>();
    private final Map<XField, FieldData> fieldsUsedInThreads = new HashMap<>();

    private boolean synchronizedBlock = false;
    private boolean firstPass = true;

    public ResourceInMultipleThreadsDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(JavaClass obj) {
        resetState();
        for (Method m : obj.getMethods()) {
            doVisitMethod(m);
        }
        firstPass = false;
    }

    @Override
    public void visit(Method method) {
        synchronizedBlock = method.isSynchronized();
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.MONITORENTER) {
            synchronizedBlock = true;
        } else if (seen == Const.MONITOREXIT) {
            synchronizedBlock = false;
        }

        if (firstPass) {
            collectMethodsUsedInThreads(seen);
        } else {
            try {
                collectFieldsUsedInThreads(seen);
            } catch (CheckedAnalysisException e) {
                bugReporter.logError(String.format("Detector %s caught exception while analyzing class %s", getClass().getName(), getClassName()), e);
            }
        }
    }

    private void collectMethodsUsedInThreads(int seen) {
        if (seen == Const.INVOKEDYNAMIC && getStack().getStackDepth() > 1
                && "Ljava/lang/Thread;".equals(getStack().getStackItem(1).getSignature())
                && !isJavaRuntimeMethod()) {
            getMethodFromBootstrap(getThisClass(), (ConstantInvokeDynamic) getConstantRefOperand()).ifPresent(methodsUsedInThreads::add);
        } else if ((seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE || seen == Const.INVOKESPECIAL || seen == Const.INVOKESTATIC)
                && getXMethodOperand() != null && methodsUsedInThreads.contains(getMethodDescriptor())
                && getClassDescriptor().equals(getXMethodOperand().getClassDescriptor())) {
            methodsUsedInThreads.add(getMethodDescriptorOperand());
        }
    }

    /**
     * Ignore a special case where a Thread is passed to the {@code java.lang.Runtime} class,
     * so it is used as a shutdown hook.
     *
     * @return {@code true} if the Thread is passed to the {@code java.lang.Runtime} class, {@code false} otherwise
     */
    private boolean isJavaRuntimeMethod() {
        return IntStream.range(0, getStack().getStackDepth())
                .mapToObj(getStack()::getStackItem)
                .map(OpcodeStack.Item::getReturnValueOf)
                .filter(Objects::nonNull)
                .anyMatch(method -> "java.lang.Runtime".equals(method.getClassName()));
    }

    private Optional<MethodDescriptor> getMethodFromBootstrap(JavaClass javaClass, ConstantInvokeDynamic constDyn) {
        for (Attribute attr : javaClass.getAttributes()) {
            if (attr instanceof BootstrapMethods) {
                Optional<Method> method = BootstrapMethodsUtil.getMethodFromBootstrap((BootstrapMethods) attr,
                        constDyn.getBootstrapMethodAttrIndex(), getConstantPool(), javaClass);
                if (method.isPresent()) {
                    return Optional.of(BCELUtil.getMethodDescriptor(javaClass, method.get()));
                }
            }
        }
        return Optional.empty();
    }

    private void collectFieldsUsedInThreads(int seen) throws CheckedAnalysisException {
        if ((seen == Const.PUTFIELD || seen == Const.PUTSTATIC) && getStack().getStackDepth() > 0
                && !isDuplicatedLocation(getMethodDescriptor(), getPC())
                && methodsUsedInThreads.contains(getMethodDescriptor())) {
            OpcodeStack.Item stackItem = getStack().getStackItem(0);
            if (stackItem.getReturnValueOf() != null && isSynchronizedCollection(stackItem.getReturnValueOf())) {
                synchronizedCollectionTypedFields.add(getXFieldOperand());
            } else if (!isAtomicTypedField(getXFieldOperand())
                    && !(Const.CONSTRUCTOR_NAME.equals(getMethodName()) || Const.STATIC_INITIALIZER_NAME.equals(getMethodName()))) {
                createOrUpdateFieldData(getXFieldOperand(), true, getMethod(), getXMethodOperand());
            }
        } else if ((seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE || seen == Const.INVOKESPECIAL || seen == Const.INVOKESTATIC)
                && getXMethodOperand() != null && getStack().getStackDepth() > 0 && !isDuplicatedLocation(getMethodDescriptor(), getPC())
                && methodsUsedInThreads.contains(getMethodDescriptor())) {
            // The field is accessed always be the last item in the stack, because the earlier elements are the arguments
            XField xField = getStack().getStackItem(getStack().getStackDepth() - 1).getXField();
            if (xField != null && !isAtomicTypedField(xField)) {
                createOrUpdateFieldData(xField, false, getMethod(), getXMethodOperand());
            }
        }
    }

    /**
     * Check if the method is a synchronized collection method.
     * @todo: This method should be moved to a utility class when VNA03-J detector is merged
     *
     * @param classMember the class member
     * @return {@code true} if the method is a synchronized collection method, {@code false} otherwise
     */
    private static boolean isSynchronizedCollection(ClassMember classMember) {
        Set<String> interestingCollectionMethodNames = new HashSet<>(Arrays.asList(
                "synchronizedCollection", "synchronizedSet", "synchronizedSortedSet",
                "synchronizedNavigableSet", "synchronizedList", "synchronizedMap",
                "synchronizedSortedMap", "synchronizedNavigableMap"));
        return "java.util.Collections".equals(classMember.getClassName()) && interestingCollectionMethodNames.contains(classMember.getName());
    }

    /**
     * Check if the location is duplicated in the method.
     * @todo: This method should be moved to a utility class when VNA03-J detector is merged
     *
     * @return {@code true} if the location is duplicated in the method, {@code false} otherwise
     * @throws CheckedAnalysisException if an error occurs during the analysis
     */
    private static boolean isDuplicatedLocation(MethodDescriptor methodDescriptor, int pc) throws CheckedAnalysisException {
        FinallyDuplicatesInfoFactory.FinallyDuplicatesInfo methodAnalysis = Global.getAnalysisCache().getMethodAnalysis(
                FinallyDuplicatesInfoFactory.FinallyDuplicatesInfo.class, methodDescriptor);
        return methodAnalysis.getDuplicates(pc).stream().anyMatch(duplicatePc -> duplicatePc < pc);
    }

    private boolean isAtomicTypedField(XField xField) {
        return xField.getSignature().contains("java/util/concurrent/atomic") || synchronizedCollectionTypedFields.contains(xField);
    }

    private void createOrUpdateFieldData(XField xField, boolean putfield, Method method, XMethod xMethod) {
        BugInstance bug = new BugInstance(this, "AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", LOW_PRIORITY)
                .addClassAndMethod(this)
                .addSourceLine(this)
                .addField(xField);
        if (!putfield) {
            bug.addCalledMethod(this);
        }

        FieldData data = fieldsUsedInThreads.computeIfAbsent(xField, value -> new FieldData());
        data.methodBugs.computeIfAbsent(method, value -> new HashSet<>()).add(bug);
        data.onlySynchronized &= synchronizedBlock;
        data.onlyPutField &= putfield;
        data.modified |= putfield || MutableClasses.looksLikeASetter(xMethod.getName());
    }

    @Override
    public void visitAfter(JavaClass javaClass) {
        super.visit(javaClass);
        fieldsUsedInThreads.entrySet().stream()
                .filter(entry -> isBug(entry.getValue()))
                .flatMap(entry -> entry.getValue().methodBugs.values().stream().flatMap(Set::stream))
                .collect(Collectors.toSet())
                .forEach(bugReporter::reportBug);
    }

    /**
     * A bug is reported if the field is modified in multiple methods, it is not only accessed in synchronized blocks,
     * and it is not a synchronized collection or an atomic typed field.
     *
     * @param data the field data
     * @return {@code true} if the field is a bug, {@code false} otherwise
     */
    private static boolean isBug(FieldData data) {
        return data.modified && !data.onlySynchronized && data.methodBugs.size() > 1 && !data.onlyPutField;
    }

    private void resetState() {
        firstPass = true;
        synchronizedCollectionTypedFields.clear();
        methodsUsedInThreads.clear();
        fieldsUsedInThreads.clear();
    }
}
