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

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.LockDataflow;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.MultiThreadedCodeIdentifierUtils;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SharedVariableAtomicityDetector extends OpcodeStackDetector {
    private final BugAccumulator bugAccumulator;
    private Method currentMethod;
    private CFG currentCFG;
    private LockDataflow currentLockDataFlow;
    private boolean isFirstVisit = true;
    private boolean hadOperation = false;
    // Field reads and inner-method call edges are accumulated across every
    // analyzed multi-threaded class for the lifetime of the detector: a field
    // owned by an enclosing class can be read in another method of the
    // enclosing class (or a sibling inner class), and that "is this shared
    // field actually read elsewhere?" signal must survive the per-class visit
    // boundary. Bug emission is deferred to report() so the shared/read
    // evaluation sees the fully accumulated map regardless of class visit order.
    private final Map<XMethod, Set<XField>> readFieldsByMethods = new HashMap<>();
    private final Set<XField> relevantFields = new HashSet<>();
    private final Map<XMethod, Set<XMethod>> nonSyncedMethodCallsByCallingMethods = new HashMap<>();
    private final List<PendingBug> pendingBugs = new ArrayList<>();

    /** A candidate bug captured at the PUTFIELD/PUTSTATIC site, evaluated in report(). */
    private static final class PendingBug {
        final BugInstance bug;
        final SourceLineAnnotation sourceLine;
        final XField field;
        final XMethod method;

        PendingBug(BugInstance bug, SourceLineAnnotation sourceLine, XField field, XMethod method) {
            this.bug = bug;
            this.sourceLine = sourceLine;
            this.field = field;
            this.method = method;
        }
    }

    private static final Set<Short> readOpCodes = Set.of(Const.GETFIELD, Const.GETSTATIC,
            Const.ALOAD, Const.ALOAD_0, Const.ALOAD_1, Const.ALOAD_2, Const.ALOAD_3,
            Const.DLOAD, Const.DLOAD_0, Const.DLOAD_1, Const.DLOAD_2, Const.DLOAD_3,
            Const.LLOAD, Const.LLOAD_0, Const.LLOAD_1, Const.LLOAD_2, Const.LLOAD_3,
            Const.FLOAD, Const.FLOAD_0, Const.FLOAD_1, Const.FLOAD_2, Const.FLOAD_3,
            Const.ILOAD, Const.ILOAD_0, Const.ILOAD_1, Const.ILOAD_2, Const.ILOAD_3,
            Const.DALOAD, Const.LALOAD, Const.FALOAD, Const.IALOAD);

    private static final Set<Short> pushOpCodes = Set.of(Const.DCONST_0, Const.DCONST_1,
            Const.LCONST_0, Const.LCONST_1,
            Const.FCONST_0, Const.FCONST_1, Const.FCONST_2,
            Const.ICONST_0, Const.ICONST_1, Const.ICONST_2, Const.ICONST_3, Const.ICONST_4, Const.ICONST_5,
            Const.LDC, Const.LDC_W, Const.LDC2_W);

    private static final Set<Short> operationOpCodes = Set.of(
            // +=,++,       -=,--       *=,         /=,         %=          -
            Const.DADD, Const.DSUB, Const.DMUL, Const.DDIV, Const.DREM, Const.DNEG,
            Const.FADD, Const.FSUB, Const.FMUL, Const.FDIV, Const.FREM, Const.FNEG,
            Const.LADD, Const.LSUB, Const.LMUL, Const.LDIV, Const.LREM, Const.LNEG,
            Const.IADD, Const.ISUB, Const.IMUL, Const.IDIV, Const.IREM, Const.INEG,
            // <<=,         >>=,        >>>=
            Const.ISHL, Const.ISHR, Const.IUSHR,
            Const.LSHL, Const.LSHR, Const.LUSHR,
            // &=
            Const.IAND, Const.LAND,
            // |=, ^=
            Const.IOR, Const.IXOR, Const.LOR, Const.LXOR);

    private static final Set<Short> methodCallOpCodes = Set.of(Const.INVOKEVIRTUAL, Const.INVOKESPECIAL, Const.INVOKESTATIC, Const.INVOKEINTERFACE);

    public SharedVariableAtomicityDetector(BugReporter reporter) {
        this.bugAccumulator = new BugAccumulator(reporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if (MultiThreadedCodeIdentifierUtils.isPartOfMultiThreadedCode(classContext)
                && !MultiThreadedCodeIdentifierUtils.isNotThreadSafe(classContext)) {
            currentMethod = null;
            currentCFG = null;
            currentLockDataFlow = null;
            super.visitClassContext(classContext);
        }
    }

    @Override
    public void visit(JavaClass javaClass) {
        isFirstVisit = true;
        for (Method m : javaClass.getMethods()) {
            doVisitMethod(m);
        }
        isFirstVisit = false;
    }

    @Override
    public void visit(Method method) {
        try {
            relevantFields.clear();
            hadOperation = false;
            currentMethod = method;
            currentLockDataFlow = getClassContext().getLockDataflow(currentMethod);
            currentCFG = getClassContext().getCFG(currentMethod);
        } catch (CFGBuilderException | DataflowAnalysisException e) {
            AnalysisContext.logError("There was an error while SharedVariableAtomicityDetector analyzed " + getClassName(), e);
        }
    }

    @Override
    public void visitAfter(JavaClass obj) {
        // readFieldsByMethods / nonSyncedMethodCallsByCallingMethods intentionally
        // keep accumulating across classes; bug emission happens in report().
        relevantFields.clear();
        hadOperation = false;
    }

    @Override
    public void sawOpcode(int seen) {
        if (Const.CONSTRUCTOR_NAME.equals(getMethodName()) || Const.STATIC_INITIALIZER_NAME.equals(getMethodName())
                || MultiThreadedCodeIdentifierUtils.isLocked(currentMethod, currentCFG, currentLockDataFlow, getPC())) {
            return;
        }
        XMethod method = getXMethod();
        if (isFirstVisit) {
            collectFieldReadsAndInnerMethodCalls(seen, method);
        } else {
            checkAndReportBug(seen, method);
        }
    }

    private void collectFieldReadsAndInnerMethodCalls(int seen, XMethod method) {
        if (seen == Const.GETFIELD || seen == Const.GETSTATIC) {
            addNonFinalFieldsOfClass(getXFieldOperand(), method, readFieldsByMethods);

        } else if (seen == Const.IFGE || seen == Const.IFGT || seen == Const.IFLT || seen == Const.IFLE || seen == Const.IFNE || seen == Const.IFEQ) {
            XField lhs = stack.getStackDepth() > 0 ? stack.getStackItem(0).getXField() : null;
            XField rhs = stack.getStackDepth() > 1 ? stack.getStackItem(1).getXField() : null;
            addNonFinalFieldsOfClass(lhs, method, readFieldsByMethods);
            addNonFinalFieldsOfClass(rhs, method, readFieldsByMethods);

        } else if (seen == Const.INVOKEINTERFACE || seen == Const.INVOKESPECIAL || seen == Const.INVOKEVIRTUAL || seen == Const.INVOKESTATIC) {
            XMethod calledMethod = getXMethodOperand();
            if (!method.equals(calledMethod)) {
                nonSyncedMethodCallsByCallingMethods.computeIfAbsent(calledMethod, k -> new HashSet<>()).add(method);
            }
        }
    }

    private void addNonFinalFieldsOfClass(XField field, XMethod method, Map<XMethod, Set<XField>> map) {
        if (field != null && !field.isFinal() && !field.isSynthetic() && isFieldOfMethodClassOrEnclosing(field, method)) {
            map.computeIfAbsent(method, k -> new HashSet<>()).add(field);
        }
    }

    private boolean hasNonSyncedNonPrivateCallToMethod(XMethod method, Set<XMethod> visitedMethods) {
        if (!method.isPrivate()) {
            return true;
        }
        boolean result = false;
        if (nonSyncedMethodCallsByCallingMethods.containsKey(method)) {
            for (XMethod callingMethod : nonSyncedMethodCallsByCallingMethods.get(method)) {
                if (visitedMethods.contains(callingMethod)) {
                    return false;
                } else {
                    visitedMethods.add(callingMethod);
                    result |= hasNonSyncedNonPrivateCallToMethod(callingMethod, visitedMethods);
                    visitedMethods.remove(callingMethod);
                }
            }
        }
        return result;
    }

    private boolean mapContainsFieldWithOtherMethod(XField field, XMethod method, Map<XMethod, Set<XField>> map) {
        return map.entrySet().stream()
                .filter(entry -> entry.getValue().contains(field) && entry.getKey() != method)
                .map(Map.Entry::getKey) // other methods containing the field
                .anyMatch(m -> hasNonSyncedNonPrivateCallToMethod(m, new HashSet<>()));
    }

    private void checkAndReportBug(int seen, XMethod method) {
        if (seen == Const.GETFIELD || seen == Const.GETSTATIC) {
            XField field = getXFieldOperand();
            if (field != null && !field.isSynthetic()) {
                relevantFields.add(field);
            }
        } else if (seen == Const.PUTFIELD || seen == Const.PUTSTATIC) {
            XField field = getXFieldOperand();
            if (field != null && !field.isFinal() && !field.isSynthetic()
                    && (seen == Const.PUTSTATIC || stack.getStackItem(1).getRegisterNumber() == 0
                            || isEnclosingThisReference(stack.getStackItem(1).getXField()))
                    && isFieldOfMethodClassOrEnclosing(field, method)) {
                // Whether the field is actually shared (read in another reachable
                // method) is evaluated in report() once the cross-class read map
                // is fully accumulated. Capture the candidate bug now, while the
                // per-instruction state (hadOperation, relevantFields) is live.
                if (hadOperation && !relevantFields.isEmpty() && relevantFields.contains(field)
                        && isPrimitiveOrItsBoxingType(field.getSignature())) {
                    addPendingBug("AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE", method, field);
                } else if (!field.isVolatile() && ClassName.isValidBaseTypeFieldDescriptor(field.getSignature())) {
                    String bugType = is64bitPrimitive(field.getSignature()) ? "AT_NONATOMIC_64BIT_PRIMITIVE"
                            : "AT_STALE_THREAD_WRITE_OF_PRIMITIVE";
                    addPendingBug(bugType, method, field);
                }
            }
            relevantFields.clear();
        } else {
            short opcode = (short) seen;
            if (operationOpCodes.contains(opcode)) {
                if (!relevantFields.isEmpty()) {
                    hadOperation = true;
                }
            } else if (!readOpCodes.contains(opcode) && !pushOpCodes.contains(opcode) && !methodCallOpCodes.contains(opcode)) {
                // if the opcode is something different then it is not the calculation of the assigned value
                relevantFields.clear();
                hadOperation = false;
            }
        }
    }

    private boolean isPrimitiveOrItsBoxingType(String className) {
        if (ClassName.isValidBaseTypeFieldDescriptor(className)) {
            return true;
        }

        String clsName = ClassName.fromFieldSignature(className);
        return clsName != null && ClassName.isValidBaseTypeFieldDescriptor(ClassName.getPrimitiveType(clsName));
    }

    private boolean is64bitPrimitive(String className) {
        return "D".equals(className) || "J".equals(className);
    }

    /**
     * True if {@code field} is declared on {@code method}'s own class or on one
     * of its enclosing classes. A non-static inner class can read and write
     * fields of its enclosing instance via the synthetic {@code this$0}
     * reference, so the detector must follow that chain instead of only
     * matching the inner class itself.
     */
    private boolean isFieldOfMethodClassOrEnclosing(XField field, XMethod method) {
        ClassDescriptor fieldClass = field.getClassDescriptor();
        ClassDescriptor enclosing = method.getClassDescriptor();
        while (enclosing != null) {
            if (fieldClass.equals(enclosing)) {
                return true;
            }
            try {
                enclosing = enclosing.getXClass().getImmediateEnclosingClass();
            } catch (CheckedAnalysisException e) {
                AnalysisContext.logError("Error walking enclosing-class chain for field " + field, e);
                return false;
            }
        }
        return false;
    }

    private static boolean isEnclosingThisReference(XField field) {
        // javac names the synthetic enclosing-instance reference this$0,
        // this$1, ... and marks it ACC_SYNTHETIC; it is the only synthetic
        // field whose load can be the target of a PUTFIELD on an outer field.
        return field != null && field.isSynthetic() && field.getName().startsWith("this$");
    }

    private void addPendingBug(String bugType, XMethod method, XField field) {
        BugInstance bug = new BugInstance(this, bugType, NORMAL_PRIORITY)
                .addClass(this)
                .addMethod(method)
                .addField(field);
        pendingBugs.add(new PendingBug(bug, SourceLineAnnotation.fromVisitedInstruction(this), field, method));
    }

    @Override
    public void report() {
        for (PendingBug pendingBug : pendingBugs) {
            if (hasNonSyncedNonPrivateCallToMethod(pendingBug.method, new HashSet<>())
                    && mapContainsFieldWithOtherMethod(pendingBug.field, pendingBug.method, readFieldsByMethods)) {
                bugAccumulator.accumulateBug(pendingBug.bug, pendingBug.sourceLine);
            }
        }
        bugAccumulator.reportAccumulatedBugs();
        pendingBugs.clear();
    }
}
