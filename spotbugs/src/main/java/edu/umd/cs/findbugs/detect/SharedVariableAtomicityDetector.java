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
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.LockDataflow;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.MultiThreadedCodeIdentifierUtils;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SharedVariableAtomicityDetector extends OpcodeStackDetector {
    private final BugAccumulator bugAccumulator;
    private Method currentMethod;
    private CFG currentCFG;
    private LockDataflow currentLockDataFlow;
    private boolean isFirstVisit = true;
    private final Map<XMethod, Set<XField>> readFieldsByMethods = new HashMap<>();
    private final Set<XField> relevantFields = new HashSet<>();
    private final Map<XMethod, Set<XMethod>> nonSyncedMethodCallsByCallingMethods = new HashMap<>();

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
        if (MultiThreadedCodeIdentifierUtils.isPartOfMultiThreadedCode(classContext)) {
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
            currentMethod = method;
            currentLockDataFlow = getClassContext().getLockDataflow(currentMethod);
            currentCFG = getClassContext().getCFG(currentMethod);
        } catch (CFGBuilderException | DataflowAnalysisException e) {
            AnalysisContext.logError("There was an error while SharedVariableAtomicityDetector analyzed " + getClassName(), e);
        }
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
        relevantFields.clear();
        readFieldsByMethods.clear();
        nonSyncedMethodCallsByCallingMethods.clear();
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
        if (field != null && !field.isFinal() && !field.isSynthetic() && field.getClassDescriptor().equals(method.getClassDescriptor())) {
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
            if (field != null && !field.isFinal() && !field.isSynthetic() && field.getClassDescriptor().equals(method.getClassDescriptor())) {
                boolean fieldReadInOtherMethod = mapContainsFieldWithOtherMethod(field, method, readFieldsByMethods);
                if (fieldReadInOtherMethod) {
                    if (!relevantFields.isEmpty() && relevantFields.contains(field) && isPrimitiveOrItsBoxingType(field.getSignature())) {
                        bugAccumulator.accumulateBug(
                                new BugInstance(this, "AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE", NORMAL_PRIORITY)
                                        .addClass(this)
                                        .addMethod(method)
                                        .addField(field),
                                this);
                    } else if (!field.isVolatile() && ClassName.isValidBaseTypeFieldDescriptor(field.getSignature())) {
                        String bugType = is64bitPrimitive(field.getSignature()) ? "AT_NONATOMIC_64BIT_PRIMITIVE"
                                : "AT_STALE_THREAD_WRITE_OF_PRIMITIVE";
                        bugAccumulator.accumulateBug(
                                new BugInstance(this, bugType, NORMAL_PRIORITY)
                                        .addClass(this)
                                        .addMethod(method)
                                        .addField(field),
                                this);
                    }
                }
            }
            relevantFields.clear();
        } else {
            short opcode = (short) seen;
            // if the opcode is something different then it is not the calculation of the assigned value
            if (!readOpCodes.contains(opcode) && !pushOpCodes.contains(opcode) && !operationOpCodes.contains(opcode)
                    && !methodCallOpCodes.contains(opcode)) {
                relevantFields.clear();
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
}
