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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Utility;

public class BadVisibilityOnSharedPrimitiveVariables extends OpcodeStackDetector {

    private static final Set<String> PRIMITIVE_TYPES = new HashSet<>();

    static {
        PRIMITIVE_TYPES.add("boolean");
        PRIMITIVE_TYPES.add("byte");
        PRIMITIVE_TYPES.add("char");
        PRIMITIVE_TYPES.add("double");
        PRIMITIVE_TYPES.add("float");
        PRIMITIVE_TYPES.add("int");
        PRIMITIVE_TYPES.add("long");
        PRIMITIVE_TYPES.add("short");
    }

    private final BugAccumulator bugAccumulator;
    private final Map<XMethod, List<XField>> modifiedNotSecuredFieldsByMethods;
    private final Map<XMethod, List<XField>> comparedNotSecuredFieldsByMethods;
    private boolean isInsideSynchronizedOrLockingMethod;

    public BadVisibilityOnSharedPrimitiveVariables(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
        this.modifiedNotSecuredFieldsByMethods = new HashMap();
        this.comparedNotSecuredFieldsByMethods = new HashMap();
        this.isInsideSynchronizedOrLockingMethod = false;
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

            super.visitClassContext(classContext);
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (!isInsideSynchronizedOrLockingMethod) {
            if (seen == Const.PUTFIELD || seen == Const.PUTSTATIC) {
                XMethod modificationMethod = getXMethod();
                Optional<XField> maybeFieldToModify = Optional.ofNullable(getXFieldOperand());
                if (!nameIsConstructor(modificationMethod.getName())) {
                    lookForUnsecuredOperationsOnFieldInOtherMethods(
                            maybeFieldToModify, modificationMethod, comparedNotSecuredFieldsByMethods, modifiedNotSecuredFieldsByMethods);
                }
                return;
            }

            if (seen == Const.IFGE || seen == Const.IFGT || seen == Const.IFLT || seen == Const.IFLE || seen == Const.IFNE || seen == Const.IFEQ) {
                XMethod comparingMethod = getXMethod();
                Optional<XField> maybeLHS = Optional.ofNullable(stack.getStackItem(0).getXField());
                Optional<XField> maybeRHS = stack.getStackDepth() > 2
                        ? Optional.ofNullable(stack.getStackItem(1).getXField())
                        : Optional.empty();
                lookForUnsecuredOperationsOnFieldInOtherMethods(
                        maybeLHS, comparingMethod, modifiedNotSecuredFieldsByMethods, comparedNotSecuredFieldsByMethods);
                lookForUnsecuredOperationsOnFieldInOtherMethods(
                        maybeRHS, comparingMethod, modifiedNotSecuredFieldsByMethods, comparedNotSecuredFieldsByMethods);
            }
        }
    }

    private void lookForUnsecuredOperationsOnFieldInOtherMethods(
            Optional<XField> maybeField, XMethod operatingMethod,
            Map<XMethod, List<XField>> checkAgainstMap, Map<XMethod, List<XField>> putInMap) {
        boolean isFieldSecured = maybeField
                .map(field -> MultiThreadedCodeIdentifierUtils.isFieldMultiThreaded(field) || field.isFinal())
                .orElse(true);
        boolean isFieldPrimitive = maybeField
                .map(field -> isPrimitive(field.getSignature()))
                .orElse(false);
        if (!isFieldSecured && isFieldPrimitive) {
            XField field = maybeField.get();
            boolean fieldGotOperatedInAnyOtherMethod = checkAgainstMap.entrySet().stream()
                    .anyMatch(entry -> entry.getValue().contains(field) && entry.getKey() != operatingMethod);
            if (fieldGotOperatedInAnyOtherMethod) {
                bugAccumulator.accumulateBug(
                        new BugInstance(this, "SPV_BAD_VISIBILITY_ON_SHARED_PRIMITIVE_VARIABLES", NORMAL_PRIORITY)
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

    private boolean isPrimitive(String signature) {
        return PRIMITIVE_TYPES.contains(Utility.signatureToString(signature));
    }

    private boolean nameIsConstructor(String name) {
        return name.equals(Const.CONSTRUCTOR_NAME);
    }
}
