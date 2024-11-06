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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final Map<XMethod, List<XField>> modifiedNotSecuredFieldsByMethods = new HashMap<>();
    private final Map<XMethod, List<XField>> comparedNotSecuredFieldsByMethods = new HashMap<>();
    private boolean isInsideSynchronizedOrLockingMethod = false;

    public BadVisibilityOnSharedPrimitiveVariables(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(Method method) {
        isInsideSynchronizedOrLockingMethod = MultiThreadedCodeIdentifierUtils.isMethodMultiThreaded(method, getClassContext());
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
        modifiedNotSecuredFieldsByMethods.clear();
        comparedNotSecuredFieldsByMethods.clear();
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
                if (!Const.CONSTRUCTOR_NAME.equals(modificationMethod.getName())) {
                    lookForUnsecuredOperationsOnFieldInOtherMethods(
                            getXFieldOperand(), modificationMethod, comparedNotSecuredFieldsByMethods, modifiedNotSecuredFieldsByMethods);
                }
            } else if (seen == Const.IFGE || seen == Const.IFGT || seen == Const.IFLT || seen == Const.IFLE || seen == Const.IFNE
                    || seen == Const.IFEQ) {
                XMethod comparingMethod = getXMethod();
                XField lhs = stack.getStackItem(0).getXField();
                XField rhs = stack.getStackDepth() > 1 ? stack.getStackItem(1).getXField() : null;
                lookForUnsecuredOperationsOnFieldInOtherMethods(
                        lhs, comparingMethod, modifiedNotSecuredFieldsByMethods, comparedNotSecuredFieldsByMethods);
                lookForUnsecuredOperationsOnFieldInOtherMethods(
                        rhs, comparingMethod, modifiedNotSecuredFieldsByMethods, comparedNotSecuredFieldsByMethods);
            }
        }
    }

    private void lookForUnsecuredOperationsOnFieldInOtherMethods(XField field, XMethod operatingMethod,
            Map<XMethod, List<XField>> checkAgainstMap, Map<XMethod, List<XField>> putInMap) {
        if (field != null && isPrimitive(field.getSignature()) && !field.isFinal() && !MultiThreadedCodeIdentifierUtils.isFieldMultiThreaded(field)) {
            boolean fieldGotOperatedInAnyOtherMethod = checkAgainstMap.entrySet().stream()
                    .anyMatch(entry -> entry.getValue().contains(field) && entry.getKey() != operatingMethod);
            if (fieldGotOperatedInAnyOtherMethod) {
                bugAccumulator.accumulateBug(
                        new BugInstance(this, "SPV_BAD_VISIBILITY_ON_SHARED_PRIMITIVE_VARIABLES", NORMAL_PRIORITY)
                                .addClass(this)
                                .addMethod(operatingMethod)
                                .addField(field),
                        this);
            } else {
                putInMap.computeIfAbsent(operatingMethod, k -> new ArrayList<>()).add(field);
            }
        }
    }

    private boolean isPrimitive(String signature) {
        return PRIMITIVE_TYPES.contains(Utility.signatureToString(signature));
    }
}
