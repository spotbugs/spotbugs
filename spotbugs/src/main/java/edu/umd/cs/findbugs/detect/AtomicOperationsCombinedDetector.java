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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MONITORENTER;
import org.apache.bcel.generic.MONITOREXIT;
import org.apache.bcel.generic.PUTFIELD;
import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.OpcodeStackScanner;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ca.Call;
import edu.umd.cs.findbugs.ba.ca.CallList;
import edu.umd.cs.findbugs.ba.ca.CallListDataflow;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.CollectionAnalysis;
import edu.umd.cs.findbugs.util.MethodAnalysis;

public class AtomicOperationsCombinedDetector implements Detector {

    private static class BugPrototype {
        final ClassContext classContext;
        final Method method;
        final Location location;
        XField invokedField;
        XMethod invokedMethod;
        LocalVariableAnnotation localVariableAnnotation;

        private BugPrototype(ClassContext classContext, Method method, Location location) {
            this.classContext = classContext;
            this.method = method;
            this.location = location;
        }

        private BugInstance toBugInstance(Detector detector, String type) {
            BugInstance bugInstance = new BugInstance(detector, type, LOW_PRIORITY)
                    .addClassAndMethod(classContext.getJavaClass(), method)
                    .addSourceLine(classContext, method, location);
            if (invokedField != null) {
                bugInstance.addField(invokedField);
            } else if (localVariableAnnotation != null) {
                bugInstance.add(localVariableAnnotation);
            }
            if (invokedMethod != null) {
                bugInstance.addCalledMethod(invokedMethod);
            }
            return bugInstance;
        }
    }

    private final BugReporter bugReporter;
    private final BugAccumulator bugAccumulator;

    private final Set<XField> fieldsForAtomicityCheck = new HashSet<>();
    private final Set<XField> combinedAtomicFields = new HashSet<>();

    private final Map<Method, Map<XField, List<BugPrototype>>> fieldAccessBugPrototypes = new HashMap<>();
    private final Map<Method, List<BugPrototype>> localVariableInvocations = new HashMap<>();

    private final Set<XMethod> unsynchronizedPrivateMethods = new HashSet<>();

    public AtomicOperationsCombinedDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        try {
            for (Method method : classContext.getMethodsInCallOrder()) {
                collectFieldsForAtomicityCheck(classContext, method);
            }
            for (Method method : classContext.getMethodsInCallOrder()) {
                analyzeFieldsForAtomicityViolations(classContext, method);
            }
            removePresynchronizedPrivateMethodCalls();
            accumulateFieldsForAtomicityAnalysis();
            accumulateLocalVariables();
        } catch (CheckedAnalysisException e) {
            bugReporter.logError(String.format("Detector %s caught exception while analyzing class %s",
                    getClass().getName(), classContext.getJavaClass().getClassName()), e);
        } finally {
            clearProperties();
        }
    }

    private void collectFieldsForAtomicityCheck(ClassContext classContext, Method method) throws CFGBuilderException {
        CFG cfg = classContext.getCFG(method);
        ConstantPoolGen cpg = classContext.getConstantPoolGen();

        for (Location location : cfg.orderedLocations()) {
            InstructionHandle handle = location.getHandle();
            Instruction instruction = handle.getInstruction();

            if (instruction instanceof PUTFIELD) {
                OpcodeStack stack = OpcodeStackScanner.getStackAt(classContext.getJavaClass(), method, handle.getPosition());
                OpcodeStack.Item stackItem = stack.getStackItem(0);
                if (isAtomicField(stackItem.getReturnValueOf())) {
                    fieldsForAtomicityCheck.add(XFactory.createXField((FieldInstruction) instruction, cpg));
                }
            }
        }
    }

    private static boolean isAtomicField(XMethod xMethod) {
        if (xMethod == null) {
            return false;
        }
        return CollectionAnalysis.isSynchronizedCollection(xMethod)
                || (xMethod.getClassName().startsWith("java.util.concurrent.atomic") && xMethod.getSignature().endsWith(")V"));
    }

    private void analyzeFieldsForAtomicityViolations(ClassContext classContext, Method method) throws CheckedAnalysisException {
        if (Const.CONSTRUCTOR_NAME.equals(method.getName()) || Const.STATIC_INITIALIZER_NAME.equals(method.getName()) || method.isSynchronized()) {
            return;
        }

        CFG cfg = classContext.getCFG(method);
        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        CallListDataflow callListDataflow = classContext.getCallListDataflow(method);
        JavaClass javaClass = classContext.getJavaClass();
        MethodDescriptor methodDescriptor = BCELUtil.getMethodDescriptor(javaClass, method);
        boolean synchronizedBlock = false;

        for (Location location : cfg.orderedLocations()) {
            BugPrototype bugPrototype = new BugPrototype(classContext, method, location);
            InstructionHandle handle = location.getHandle();
            Instruction instruction = handle.getInstruction();
            int pc = handle.getPosition();

            if (instruction instanceof MONITORENTER) {
                synchronizedBlock = true;
            } else if (instruction instanceof MONITOREXIT) {
                synchronizedBlock = false;
            } else if (instruction instanceof PUTFIELD && !synchronizedBlock && !MethodAnalysis.isDuplicatedLocation(methodDescriptor, pc)) {
                XField xField = XFactory.createXField((FieldInstruction) instruction, cpg);
                if (fieldsForAtomicityCheck.contains(xField)) {
                    bugPrototype.invokedField = xField;
                    fieldAccessBugPrototypes.computeIfAbsent(method, value -> new HashMap<>())
                            .computeIfAbsent(xField, value -> new LinkedList<>())
                            .add(bugPrototype);
                }
            } else if (instruction instanceof InvokeInstruction && !(instruction instanceof INVOKEDYNAMIC)
                    && !synchronizedBlock && !MethodAnalysis.isDuplicatedLocation(methodDescriptor, pc)) {
                OpcodeStack stack = OpcodeStackScanner.getStackAt(javaClass, method, pc);
                Optional<XField> fieldRequiringAtomicityCheck = findFieldRequiringAtomicityCheck(stack);
                XMethod xMethod = XFactory.createXMethod((InvokeInstruction) instruction, cpg);
                if (fieldRequiringAtomicityCheck.isPresent()) {
                    bugPrototype.invokedField = fieldRequiringAtomicityCheck.get();
                    bugPrototype.invokedMethod = xMethod;
                    fieldAccessBugPrototypes.computeIfAbsent(method, value -> new HashMap<>())
                            .computeIfAbsent(fieldRequiringAtomicityCheck.get(), value -> new LinkedList<>())
                            .add(bugPrototype);

                    if (javaClass.getClassName().equals(xMethod.getClassName())) {
                        unsynchronizedPrivateMethods.add(xMethod);
                    }
                } else if (stack.getStackDepth() > 0) {
                    OpcodeStack.Item stackItem = stack.getStackItem(0);
                    if (isLocalVariableRequiringAtomicityCheck(stackItem)) {
                        LocalVariableAnnotation annotation = LocalVariableAnnotation.getLocalVariableAnnotation(method, stackItem, pc);
                        if (annotation != null) {
                            bugPrototype.localVariableAnnotation = annotation;
                            bugPrototype.invokedMethod = xMethod;
                            localVariableInvocations.computeIfAbsent(method, value -> new LinkedList<>())
                                    .add(bugPrototype);
                        }
                    } else if (isAtomicOperationsCombined(stack)) {
                        combinedAtomicFields.addAll(findFieldsInvolvedInAtomicOperations(location, callListDataflow));
                    }
                }
            }
        }
    }

    private Optional<XField> findFieldRequiringAtomicityCheck(OpcodeStack stack) {
        if (stack.getStackDepth() > 1 && stack.getStackItem(0).getReturnValueOf() != null) {
            return Optional.empty();
        }
        return IntStream.range(0, stack.getStackDepth())
                .mapToObj(stack::getStackItem)
                .map(OpcodeStack.Item::getXField)
                .filter(fieldsForAtomicityCheck::contains)
                .findFirst();
    }

    private static boolean isLocalVariableRequiringAtomicityCheck(OpcodeStack.Item stackItem) {
        return isAtomicSignature(stackItem.getSignature())
                && (stackItem.getReturnValueOf() == null || !stackItem.getReturnValueOf().getName().contains(Const.CONSTRUCTOR_NAME));
    }

    private static boolean isAtomicSignature(String signature) {
        return signature.startsWith("Ljava/util/concurrent/atomic/");
    }

    private boolean isAtomicOperationsCombined(OpcodeStack stack) {
        return stack.getStackDepth() > 1
                && IntStream.range(0, stack.getStackDepth()).mapToObj(stack::getStackItem)
                        .allMatch(AtomicOperationsCombinedDetector::isAtomicReference);
    }

    private static boolean isAtomicReference(OpcodeStack.Item stackItem) {
        return stackItem.getReturnValueOf() != null && stackItem.getReturnValueOf().getClassName().startsWith("java.util.concurrent.atomic");
    }

    private Set<XField> findFieldsInvolvedInAtomicOperations(Location location, CallListDataflow callListDataflow) throws DataflowAnalysisException {
        Set<XField> involvedFields = new HashSet<>();
        CallList factAtLocation = callListDataflow.getFactAtLocation(location);
        Iterator<Call> callIterator = factAtLocation.callIterator();
        while (callIterator.hasNext()) {
            Call call = callIterator.next();
            call.getAttributes().stream()
                    .filter(fieldsForAtomicityCheck::contains)
                    .forEach(involvedFields::add);
        }
        return involvedFields;
    }

    private void removePresynchronizedPrivateMethodCalls() {
        localVariableInvocations.entrySet().removeIf(entry -> entry.getKey().isPrivate()
                && unsynchronizedPrivateMethods.stream().noneMatch(privMethod -> equalMethods(entry.getKey(), privMethod)));
    }

    private static boolean equalMethods(Method method, XMethod unSyncMethod) {
        return method.getName().equals(unSyncMethod.getName()) && method.getSignature().equals(unSyncMethod.getSignature());
    }

    @Override
    public void report() {
        bugAccumulator.reportAccumulatedBugs();
    }

    private void accumulateFieldsForAtomicityAnalysis() {
        Set<XField> fieldsToProcess = fieldAccessBugPrototypes.values().stream()
                .flatMap(map -> map.entrySet().stream())
                .filter(entry -> entry.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        fieldAccessBugPrototypes.entrySet().stream()
                .flatMap(entry -> entry.getValue().entrySet().stream())
                .forEach(entry -> processFieldData(entry.getKey(), entry.getValue(), fieldsToProcess));
    }

    private void accumulateLocalVariables() {
        localVariableInvocations.values().forEach(this::processFieldData);
    }

    private void processFieldData(List<BugPrototype> fieldCallData) {
        processFieldData(null, fieldCallData, new HashSet<>());
    }

    private void processFieldData(XField field, List<BugPrototype> fieldCallData, Set<XField> fieldsWithMultipleCalls) {
        if (!fieldCallData.isEmpty()) {
            BugPrototype bugPrototype = fieldCallData.get(fieldCallData.size() - 1);
            if (fieldCallData.size() > 1 || combinedAtomicFields.contains(field)) {
                BugInstance bugInstance = bugPrototype.toBugInstance(this, "AT_COMBINED_ATOMIC_OPERATIONS_ARE_NOT_ATOMIC");
                bugAccumulator.accumulateBug(bugInstance, bugInstance.getPrimarySourceLineAnnotation());
            } else if (fieldsWithMultipleCalls.contains(field)) {
                BugInstance bugInstance = bugPrototype.toBugInstance(this, "AT_ATOMIC_OPERATION_NEEDS_SYNCHRONIZATION");
                bugAccumulator.accumulateBug(bugInstance, bugInstance.getPrimarySourceLineAnnotation());
            }
        }
    }

    private void clearProperties() {
        fieldsForAtomicityCheck.clear();
        combinedAtomicFields.clear();
        fieldAccessBugPrototypes.clear();
        localVariableInvocations.clear();
        unsynchronizedPrivateMethods.clear();
    }
}
