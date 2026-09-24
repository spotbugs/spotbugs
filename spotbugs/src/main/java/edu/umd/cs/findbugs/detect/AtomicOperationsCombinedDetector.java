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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;
import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.LockDataflow;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ca.Call;
import edu.umd.cs.findbugs.ba.ca.CallListDataflow;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.CollectionAnalysis;
import edu.umd.cs.findbugs.util.MethodAnalysis;

public class AtomicOperationsCombinedDetector implements Detector {

    private static final String COMBINED_NOT_ATOMIC = "AT_COMBINED_ATOMIC_OPERATIONS_ARE_NOT_ATOMIC";
    private static final String NEEDS_SYNCHRONIZATION = "AT_ATOMIC_OPERATION_NEEDS_SYNCHRONIZATION";

    private static class BugPrototype {
        final ClassContext classContext;
        final Method method;
        final Location location;
        XField field;
        XMethod calledMethod;
        LocalVariableAnnotation localVariable;

        BugPrototype(ClassContext classContext, Method method, Location location) {
            this.classContext = classContext;
            this.method = method;
            this.location = location;
        }

        BugInstance toBugInstance(Detector detector, String type) {
            BugInstance bug = new BugInstance(detector, type, LOW_PRIORITY)
                    .addClassAndMethod(classContext.getJavaClass(), method)
                    .addSourceLine(classContext, method, location);
            if (field != null) {
                bug.addField(field);
            } else if (localVariable != null) {
                bug.add(localVariable);
            }
            if (calledMethod != null) {
                bug.addCalledMethod(calledMethod);
            }
            return bug;
        }
    }

    /**
     * Captured operand stack data at an InvokeInstruction site.
     */
    private static class InvokeStackSnapshot {
        final int stackDepth;
        final List<XField> xFields;
        final String topSignature;
        final XMethod topReturnValueOf;
        final int topRegisterNumber;
        final int topItemPC;
        final boolean allAtomicReferences;

        InvokeStackSnapshot(OpcodeStack stack) {
            this.stackDepth = stack.getStackDepth();
            this.xFields = new ArrayList<>(stackDepth);
            boolean allAtomic = stackDepth > 1;
            for (int i = 0; i < stackDepth; i++) {
                OpcodeStack.Item item = stack.getStackItem(i);
                xFields.add(item.getXField());
                if (allAtomic) {
                    XMethod rv = item.getReturnValueOf();
                    if (rv == null || !rv.getClassName().startsWith("java.util.concurrent.atomic")) {
                        allAtomic = false;
                    }
                }
            }
            this.allAtomicReferences = allAtomic;
            if (stackDepth > 0) {
                OpcodeStack.Item top = stack.getStackItem(0);
                this.topSignature = top.getSignature();
                this.topReturnValueOf = top.getReturnValueOf();
                this.topRegisterNumber = top.getRegisterNumber();
                this.topItemPC = top.getPC();
            } else {
                this.topSignature = "";
                this.topReturnValueOf = null;
                this.topRegisterNumber = -1;
                this.topItemPC = -1;
            }
        }

        boolean isTopLocalAtomicVariable() {
            return topSignature.startsWith("Ljava/util/concurrent/atomic/")
                    && (topReturnValueOf == null || !topReturnValueOf.getName().contains(Const.CONSTRUCTOR_NAME));
        }
    }

    /**
     * Scans an entire method's bytecode once and collects operand stack state
     * at all PUTFIELD/PUTSTATIC and InvokeInstruction sites.
     */
    private static class MethodStackScanner extends OpcodeStackDetector {
        final Map<Integer, XMethod> putFieldReturnValues = new HashMap<>();
        final Map<Integer, InvokeStackSnapshot> invokeSnapshots = new HashMap<>();

        private final JavaClass theClass;
        private final Method theMethod;

        MethodStackScanner(JavaClass theClass, Method method) {
            this.theClass = theClass;
            this.theMethod = method;
        }

        void execute() {
            theClass.accept(this);
        }

        @Override
        public void visitJavaClass(JavaClass obj) {
            setupVisitorForClass(obj);
            getConstantPool().accept(this);
            doVisitMethod(theMethod);
        }

        @Override
        public void sawOpcode(int seen) {
        }

        @Override
        public void afterOpcode(int seen) {
            int pc = getPC();
            if ((seen == Const.PUTFIELD || seen == Const.PUTSTATIC) && stack.getStackDepth() > 0) {
                putFieldReturnValues.put(pc, stack.getStackItem(0).getReturnValueOf());
            }
            if (seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE
                    || seen == Const.INVOKESPECIAL || seen == Const.INVOKESTATIC) {
                invokeSnapshots.put(pc, new InvokeStackSnapshot(stack));
            }
            super.afterOpcode(seen);
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
            reportFieldBugs();
            reportLocalVariableBugs();
        } catch (CheckedAnalysisException e) {
            bugReporter.logError(String.format("Detector %s caught exception while analyzing class %s",
                    getClass().getName(), classContext.getJavaClass().getClassName()), e);
        } finally {
            fieldsForAtomicityCheck.clear();
            combinedAtomicFields.clear();
            fieldAccessBugPrototypes.clear();
            localVariableInvocations.clear();
            unsynchronizedPrivateMethods.clear();
        }
    }

    @Override
    public void report() {
        bugAccumulator.reportAccumulatedBugs();
    }

    // --- Phase 1: Collect fields assigned synchronized collections or atomic types ---

    private void collectFieldsForAtomicityCheck(ClassContext classContext, Method method) {
        JavaClass javaClass = classContext.getJavaClass();
        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        MethodStackScanner scanner = new MethodStackScanner(javaClass, method);
        scanner.execute();

        CFG cfg;
        try {
            cfg = classContext.getCFG(method);
        } catch (CheckedAnalysisException e) {
            return;
        }

        for (Location location : cfg.orderedLocations()) {
            Instruction instruction = location.getHandle().getInstruction();
            if (instruction instanceof PUTFIELD || instruction instanceof PUTSTATIC) {
                XMethod returnValueOf = scanner.putFieldReturnValues.get(location.getHandle().getPosition());
                if (isAtomicFieldInitializer(returnValueOf)) {
                    fieldsForAtomicityCheck.add(XFactory.createXField((FieldInstruction) instruction, cpg));
                }
            }
        }
    }

    private static boolean isAtomicFieldInitializer(XMethod xMethod) {
        if (xMethod == null) {
            return false;
        }
        return CollectionAnalysis.isSynchronizedCollection(xMethod)
                || (xMethod.getClassName().startsWith("java.util.concurrent.atomic")
                        && xMethod.getSignature().endsWith(")V"));
    }

    // --- Phase 2: Find unsynchronized accesses to tracked fields ---

    private void analyzeFieldsForAtomicityViolations(ClassContext classContext, Method method) throws CheckedAnalysisException {
        if (Const.CONSTRUCTOR_NAME.equals(method.getName()) || Const.STATIC_INITIALIZER_NAME.equals(method.getName())
                || method.isSynchronized()) {
            return;
        }

        JavaClass javaClass = classContext.getJavaClass();
        CFG cfg = classContext.getCFG(method);
        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        MethodDescriptor methodDescriptor = BCELUtil.getMethodDescriptor(javaClass, method);
        LockDataflow lockDataflow = classContext.getLockDataflow(method);
        MethodStackScanner scanner = new MethodStackScanner(javaClass, method);
        scanner.execute();

        for (Location location : cfg.orderedLocations()) {
            Instruction instruction = location.getHandle().getInstruction();
            int pc = location.getHandle().getPosition();

            if (!lockDataflow.getFactAtLocation(location).isEmpty()
                    || (pc >= 0 && MethodAnalysis.isDuplicatedLocation(methodDescriptor, pc))) {
                continue;
            }

            if (instruction instanceof PUTFIELD || instruction instanceof PUTSTATIC) {
                analyzeFieldReassignment(classContext, method, location, cpg);
            } else if (instruction instanceof InvokeInstruction && !(instruction instanceof INVOKEDYNAMIC)) {
                analyzeInvocation(classContext, method, location, scanner, cpg, javaClass);
            }
        }
    }

    private void analyzeFieldReassignment(ClassContext classContext, Method method, Location location, ConstantPoolGen cpg) {
        XField xField = XFactory.createXField((FieldInstruction) location.getHandle().getInstruction(), cpg);
        if (!fieldsForAtomicityCheck.contains(xField)) {
            return;
        }
        BugPrototype proto = new BugPrototype(classContext, method, location);
        proto.field = xField;
        addFieldAccessPrototype(method, xField, proto);
    }

    private void analyzeInvocation(ClassContext classContext, Method method, Location location,
            MethodStackScanner scanner, ConstantPoolGen cpg, JavaClass javaClass) throws CheckedAnalysisException {
        int pc = location.getHandle().getPosition();
        InvokeStackSnapshot snapshot = scanner.invokeSnapshots.get(pc);
        if (snapshot == null) {
            return;
        }

        int consumed = location.getHandle().getInstruction().consumeStack(cpg);
        XField trackedField = findTrackedFieldInConsumedItems(snapshot, consumed);
        XMethod xMethod = XFactory.createXMethod((InvokeInstruction) location.getHandle().getInstruction(), cpg);
        BugPrototype proto = new BugPrototype(classContext, method, location);

        if (trackedField != null) {
            proto.field = trackedField;
            proto.calledMethod = xMethod;
            addFieldAccessPrototype(method, trackedField, proto);
            if (javaClass.getClassName().equals(xMethod.getClassName())) {
                unsynchronizedPrivateMethods.add(xMethod);
            }
            return;
        }

        if (snapshot.stackDepth <= 0) {
            return;
        }

        if (snapshot.isTopLocalAtomicVariable()) {
            LocalVariableAnnotation annotation = snapshot.topRegisterNumber >= 0
                    ? LocalVariableAnnotation.getLocalVariableAnnotation(method,
                            snapshot.topRegisterNumber, pc, snapshot.topItemPC)
                    : null;
            if (annotation != null) {
                proto.localVariable = annotation;
                proto.calledMethod = xMethod;
                localVariableInvocations.computeIfAbsent(method, k -> new LinkedList<>()).add(proto);
            }
        } else if (snapshot.allAtomicReferences) {
            CallListDataflow callListDataflow = classContext.getCallListDataflow(method);
            collectCombinedAtomicFields(location, callListDataflow);
        }
    }

    private XField findTrackedFieldInConsumedItems(InvokeStackSnapshot snapshot, int consumed) {
        return snapshot.xFields.stream().limit(consumed)
                .filter(fieldsForAtomicityCheck::contains)
                .findFirst().orElse(null);
    }

    private void addFieldAccessPrototype(Method method, XField field, BugPrototype proto) {
        fieldAccessBugPrototypes.computeIfAbsent(method, k -> new HashMap<>())
                .computeIfAbsent(field, k -> new LinkedList<>())
                .add(proto);
    }

    private void collectCombinedAtomicFields(Location location, CallListDataflow callListDataflow)
            throws DataflowAnalysisException {
        java.util.Iterator<Call> it = callListDataflow.getFactAtLocation(location).callIterator();
        while (it.hasNext()) {
            it.next().getAttributes().stream()
                    .filter(fieldsForAtomicityCheck::contains)
                    .forEach(combinedAtomicFields::add);
        }
    }

    // --- Phase 3: Filter false positives from pre-synchronized private methods ---

    private void removePresynchronizedPrivateMethodCalls() {
        localVariableInvocations.entrySet().removeIf(entry -> entry.getKey().isPrivate()
                && unsynchronizedPrivateMethods.stream()
                        .noneMatch(m -> m.getName().equals(entry.getKey().getName())
                                && m.getSignature().equals(entry.getKey().getSignature())));
    }

    // --- Phase 4: Report accumulated bugs ---

    private void reportFieldBugs() {
        Set<XField> fieldsWithCombinedAccess = fieldAccessBugPrototypes.values().stream()
                .flatMap(map -> map.entrySet().stream())
                .filter(entry -> entry.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        fieldAccessBugPrototypes.values().stream()
                .flatMap(map -> map.entrySet().stream())
                .forEach(entry -> reportFieldBug(entry.getKey(), entry.getValue(), fieldsWithCombinedAccess));
    }

    private void reportFieldBug(XField field, List<BugPrototype> prototypes, Set<XField> fieldsWithCombinedAccess) {
        if (prototypes.isEmpty()) {
            return;
        }
        BugPrototype last = prototypes.get(prototypes.size() - 1);
        if (prototypes.size() > 1 || combinedAtomicFields.contains(field)) {
            accumulateBug(last, COMBINED_NOT_ATOMIC);
        } else if (fieldsWithCombinedAccess.contains(field)) {
            accumulateBug(last, NEEDS_SYNCHRONIZATION);
        }
    }

    private void reportLocalVariableBugs() {
        for (List<BugPrototype> prototypes : localVariableInvocations.values()) {
            if (prototypes.size() > 1) {
                accumulateBug(prototypes.get(prototypes.size() - 1), COMBINED_NOT_ATOMIC);
            }
        }
    }

    private void accumulateBug(BugPrototype proto, String bugType) {
        BugInstance bug = proto.toBugInstance(this, bugType);
        bugAccumulator.accumulateBug(bug, bug.getPrimarySourceLineAnnotation());
    }
}
