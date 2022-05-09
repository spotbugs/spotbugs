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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class CompoundOperationsOnSharedVariables extends OpcodeStackDetector {

    private final BugAccumulator bugAccumulator;
    private final Map<XMethod, List<XField>> compoundlyWroteNotSecuredFieldsByMethods;
    private final Map<XMethod, List<XField>> redNotSecuredFieldsByMethods;
    // This field might not be needed at all
    private boolean isInsideSynchronizedBlock;
    private boolean isInsideSynchronizedOrLockingMethod;
    private Optional<XField> maybeCompoundlyOperatedField;
    private int stepsMadeInCompoundOperationProcess;

    public CompoundOperationsOnSharedVariables(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
        this.compoundlyWroteNotSecuredFieldsByMethods = new HashMap();
        this.redNotSecuredFieldsByMethods = new HashMap();
        this.isInsideSynchronizedBlock = false;
        this.isInsideSynchronizedOrLockingMethod = false;
        this.maybeCompoundlyOperatedField = Optional.empty();
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
            isInsideSynchronizedBlock = false;
            isInsideSynchronizedOrLockingMethod = false;
            maybeCompoundlyOperatedField = Optional.empty();
            stepsMadeInCompoundOperationProcess = Const.UNDEFINED;

            super.visitClassContext(classContext);
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.MONITORENTER) {
            isInsideSynchronizedBlock = true;
            maybeCompoundlyOperatedField = Optional.empty();
            stepsMadeInCompoundOperationProcess = Const.UNDEFINED;
            return;
        } else if (seen == Const.MONITOREXIT) {
            isInsideSynchronizedBlock = false;
            maybeCompoundlyOperatedField = Optional.empty();
            stepsMadeInCompoundOperationProcess = Const.UNDEFINED;
            return;
        }

        if (!isInsideSynchronizedBlock && !isInsideSynchronizedOrLockingMethod) {
            if (seen == Const.GETFIELD || seen == Const.GETSTATIC) {
                XMethod readMethod = getXMethod();
                Optional<XField> maybeFieldToRead = Optional.ofNullable(getXFieldOperand());
                lookForUnsecuredOperationsOnFieldInOtherMethods(
                        maybeFieldToRead, readMethod, compoundlyWroteNotSecuredFieldsByMethods, redNotSecuredFieldsByMethods);
                maybeCompoundlyOperatedField = maybeFieldToRead;
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
                // This might be a compound operation:
                XMethod modificationMethod = getXMethod();
                Optional<XField> maybeFieldToModify = Optional.ofNullable(getXFieldOperand());

                boolean matchWithReadField = maybeCompoundlyOperatedField
                        .map(compoundlyOperatedField -> maybeFieldToModify
                                .map(modifiedField -> modifiedField.getName().equals(compoundlyOperatedField.getName()))
                                .orElse(false))
                        .orElse(false);
                if (matchWithReadField) {
                    lookForUnsecuredOperationsOnFieldInOtherMethods(
                            maybeFieldToModify, modificationMethod, redNotSecuredFieldsByMethods, compoundlyWroteNotSecuredFieldsByMethods);

                }

                maybeCompoundlyOperatedField = Optional.empty();
                stepsMadeInCompoundOperationProcess = Const.UNDEFINED;
            }
        } else {
            maybeCompoundlyOperatedField = Optional.empty();
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
                    putInMap.put(operatingMethod, new ArrayList(Collections.singletonList(field)));
                }
            }
        }
    }

    private List<XField> appendToList(XField field, List<XField> list) {
        list.add(field);
        return list;
    }

    private boolean isReadOpCode(int opCode) {
        return opCode == Const.GETFIELD || opCode == Const.GETSTATIC
                || opCode == Const.ALOAD || opCode == Const.ALOAD_0 || opCode == Const.ALOAD_1 || opCode == Const.ALOAD_2 || opCode == Const.ALOAD_3
                || opCode == Const.DLOAD || opCode == Const.DLOAD_0 || opCode == Const.DLOAD_1 || opCode == Const.DLOAD_2 || opCode == Const.DLOAD_3
                || opCode == Const.LLOAD || opCode == Const.LLOAD_0 || opCode == Const.LLOAD_1 || opCode == Const.LLOAD_2 || opCode == Const.LLOAD_3
                || opCode == Const.FLOAD || opCode == Const.FLOAD_0 || opCode == Const.FLOAD_1 || opCode == Const.FLOAD_2 || opCode == Const.FLOAD_3
                || opCode == Const.ILOAD || opCode == Const.ILOAD_0 || opCode == Const.ILOAD_1 || opCode == Const.ILOAD_2 || opCode == Const.ILOAD_3
                || opCode == Const.DALOAD || opCode == Const.LALOAD || opCode == Const.FALOAD || opCode == Const.IALOAD;
    }

    private boolean isPushConstant(int opCode) {
        return opCode == Const.DCONST_0 || opCode == Const.DCONST_1
                || opCode == Const.LCONST_0 || opCode == Const.LCONST_1
                || opCode == Const.FCONST_0 || opCode == Const.FCONST_1 || opCode == Const.FCONST_2
                || opCode == Const.ICONST_0 || opCode == Const.ICONST_1 || opCode == Const.ICONST_2 || opCode == Const.ICONST_3 || opCode == Const.ICONST_4
                || opCode == Const.ICONST_5
                || opCode == Const.LDC || opCode == Const.LDC_W || opCode == Const.LDC2_W;
    }

    private boolean possibleCompoundOperation(int opCode) {
        // +=, -=, *=, /=, %=, ++, --
        return opCode == Const.DADD || opCode == Const.DSUB || opCode == Const.DMUL || opCode == Const.DDIV || opCode == Const.DREM
                || opCode == Const.FADD || opCode == Const.FSUB || opCode == Const.FMUL || opCode == Const.FDIV || opCode == Const.FREM
                || opCode == Const.LADD || opCode == Const.LSUB || opCode == Const.LMUL || opCode == Const.LDIV || opCode == Const.LREM
                || opCode == Const.IADD || opCode == Const.ISUB || opCode == Const.IMUL || opCode == Const.IDIV || opCode == Const.IREM
                // <<=, >>=, >>>=
                || opCode == Const.ISHL || opCode == Const.ISHR || opCode == Const.IUSHR
                || opCode == Const.LSHL || opCode == Const.LSHR || opCode == Const.LUSHR
                // &=
                || opCode == Const.IAND || opCode == Const.LAND
                // |=, ^=
                || opCode == Const.IOR || opCode == Const.IXOR || opCode == Const.LOR || opCode == Const.LXOR;
    }
}
