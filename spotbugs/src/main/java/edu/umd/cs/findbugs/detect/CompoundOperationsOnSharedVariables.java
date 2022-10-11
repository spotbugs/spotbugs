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
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.util.MultiThreadedCodeIdentifierUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class CompoundOperationsOnSharedVariables extends OpcodeStackDetector {

    private static final List<Short> readOpCodes = Arrays.asList(Const.GETFIELD, Const.GETSTATIC,
            Const.ALOAD, Const.ALOAD_0, Const.ALOAD_1, Const.ALOAD_2, Const.ALOAD_3,
            Const.DLOAD, Const.DLOAD_0, Const.DLOAD_1, Const.DLOAD_2, Const.DLOAD_3,
            Const.LLOAD, Const.LLOAD_0, Const.LLOAD_1, Const.LLOAD_2, Const.LLOAD_3,
            Const.FLOAD, Const.FLOAD_0, Const.FLOAD_1, Const.FLOAD_2, Const.FLOAD_3,
            Const.ILOAD, Const.ILOAD_0, Const.ILOAD_1, Const.ILOAD_2, Const.ILOAD_3,
            Const.DALOAD, Const.LALOAD, Const.FALOAD, Const.IALOAD);

    private static final List<Short> pushOpCodes = Arrays.asList(Const.DCONST_0, Const.DCONST_1,
            Const.LCONST_0, Const.LCONST_1,
            Const.FCONST_0, Const.FCONST_1, Const.FCONST_2,
            Const.ICONST_0, Const.ICONST_1, Const.ICONST_2, Const.ICONST_3, Const.ICONST_4, Const.ICONST_5,
            Const.LDC, Const.LDC_W, Const.LDC2_W);

    private static final List<Short> possibleCompoundOperationOpCodes = Arrays.asList(
            // +=,++,       -=,--       *=,         /=,         %=
            Const.DADD, Const.DSUB, Const.DMUL, Const.DDIV, Const.DREM,
            Const.FADD, Const.FSUB, Const.FMUL, Const.FDIV, Const.FREM,
            Const.LADD, Const.LSUB, Const.LMUL, Const.LDIV, Const.LREM,
            Const.IADD, Const.ISUB, Const.IMUL, Const.IDIV, Const.IREM,
            // <<=,         >>=,        >>>=
            Const.ISHL, Const.ISHR, Const.IUSHR,
            Const.LSHL, Const.LSHR, Const.LUSHR,
            // &=
            Const.IAND, Const.LAND,
            // |=, ^=
            Const.IOR, Const.IXOR, Const.LOR, Const.LXOR);

    private final BugAccumulator bugAccumulator;
    private final Map<XMethod, List<XField>> compoundlyWrittenNotSecuredFieldsByMethods;
    private final Map<XMethod, List<XField>> readNotSecuredFieldsByMethods;
    private boolean isInsideSynchronizedOrLockingMethod;
    private XField maybeCompoundlyOperatedField;
    private int stepsMadeInCompoundOperationProcess;

    public CompoundOperationsOnSharedVariables(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
        this.compoundlyWrittenNotSecuredFieldsByMethods = new HashMap<>();
        this.readNotSecuredFieldsByMethods = new HashMap<>();
        this.isInsideSynchronizedOrLockingMethod = false;
        this.maybeCompoundlyOperatedField = null;
        this.stepsMadeInCompoundOperationProcess = Const.UNDEFINED;
    }

    @Override
    public void visit(Method method) {
        ClassContext currentClassContext = getClassContext();
        isInsideSynchronizedOrLockingMethod = MultiThreadedCodeIdentifierUtils.isMethodMultiThreaded(method, currentClassContext);
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if (MultiThreadedCodeIdentifierUtils.isPartOfMultiThreadedCode(classContext)) {
            isInsideSynchronizedOrLockingMethod = false;
            maybeCompoundlyOperatedField = null;
            stepsMadeInCompoundOperationProcess = Const.UNDEFINED;

            super.visitClassContext(classContext);
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (!isInsideSynchronizedOrLockingMethod) {
            if (seen == Const.GETFIELD || seen == Const.GETSTATIC) {
                XMethod readMethod = getXMethod();
                Optional<XField> maybeFieldToRead = Optional.ofNullable(getXFieldOperand());
                lookForUnsecuredOperationsOnFieldInOtherMethods(
                        maybeFieldToRead, readMethod, compoundlyWrittenNotSecuredFieldsByMethods, readNotSecuredFieldsByMethods);
                maybeCompoundlyOperatedField = maybeFieldToRead.orElse(null);
                stepsMadeInCompoundOperationProcess = 1;
                return;
            }

            // Possible invoke virtual first if it is a wrapper type
            if (stepsMadeInCompoundOperationProcess == 1 && seen == Const.INVOKEVIRTUAL) {
                return;
            }

            if (stepsMadeInCompoundOperationProcess == 1 && (isReadOpCode(seen) || isPushConstant(seen))) {
                stepsMadeInCompoundOperationProcess = 2;
                return;
            }

            // In case of the field's type is a wrapper type
            if (stepsMadeInCompoundOperationProcess == 2 && seen == Const.INVOKEVIRTUAL) {
                return;
            }

            if (stepsMadeInCompoundOperationProcess == 2 && possibleCompoundOperation(seen)) {
                stepsMadeInCompoundOperationProcess = 3;
                return;
            }

            // In case of wrapper types valueOf() method
            if (stepsMadeInCompoundOperationProcess == 3 && seen == Const.INVOKESTATIC) {
                return;
            }

            if (stepsMadeInCompoundOperationProcess == 3 && (seen == Const.PUTFIELD || seen == Const.PUTSTATIC)) {
                XMethod modificationMethod = getXMethod();
                Optional<XField> maybeFieldToModify = Optional.ofNullable(getXFieldOperand());

                boolean matchWithReadField = Optional.ofNullable(maybeCompoundlyOperatedField)
                        .map(compoundlyOperatedField -> maybeFieldToModify
                                .map(modifiedField -> modifiedField.getName().equals(compoundlyOperatedField.getName()))
                                .orElse(false))
                        .orElse(false);
                if (matchWithReadField) {
                    lookForUnsecuredOperationsOnFieldInOtherMethods(
                            maybeFieldToModify, modificationMethod, readNotSecuredFieldsByMethods, compoundlyWrittenNotSecuredFieldsByMethods);

                }

                maybeCompoundlyOperatedField = null;
                stepsMadeInCompoundOperationProcess = Const.UNDEFINED;
            }
        } else {
            maybeCompoundlyOperatedField = null;
            stepsMadeInCompoundOperationProcess = Const.UNDEFINED;
        }
    }

    private void lookForUnsecuredOperationsOnFieldInOtherMethods(
            Optional<XField> maybeField, XMethod operatingMethod,
            Map<XMethod, List<XField>> checkAgainstMap, Map<XMethod, List<XField>> putInMap) {
        boolean isFieldSecured = maybeField
                .map(field -> MultiThreadedCodeIdentifierUtils.signatureIsFromAtomicPackage(field.getSignature())
                        || field.isFinal())
                .orElse(true);
        if (!isFieldSecured) {
            XField field = maybeField.get();
            boolean fieldIsDangerouslyUsedByOtherMethod = checkAgainstMap.entrySet().stream()
                    .anyMatch(entry -> entry.getValue().contains(field) && entry.getKey() != operatingMethod);
            if (fieldIsDangerouslyUsedByOtherMethod) {
                bugAccumulator.accumulateBug(
                        new BugInstance(this, "COSV_COMPOUND_OPERATIONS_ON_SHARED_VARIABLES", NORMAL_PRIORITY)
                                .addClassAndMethod(this)
                                .addMethod(operatingMethod)
                                .addField(field)
                                .addString(field.getName()),
                        this);
            } else {
                boolean appendedExisting = putInMap.computeIfPresent(
                        operatingMethod, (method, fields) -> appendToList(field, fields)) != null;
                if (!appendedExisting) {
                    putInMap.put(operatingMethod, new ArrayList<>(Collections.singletonList(field)));
                }
            }
        }
    }

    private List<XField> appendToList(XField field, List<XField> list) {
        list.add(field);
        return list;
    }

    private boolean isReadOpCode(int opCode) {
        return readOpCodes.contains(Integer.valueOf(opCode).shortValue());
    }

    private boolean isPushConstant(int opCode) {
        return pushOpCodes.contains(Integer.valueOf(opCode).shortValue());
    }

    private boolean possibleCompoundOperation(int opCode) {
        return possibleCompoundOperationOpCodes.contains(Integer.valueOf(opCode).shortValue());
    }
}
