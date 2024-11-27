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
    private final Map<XMethod, List<XField>> readFieldsByMethods = new HashMap<>();
    private final Set<XField> relevantFields = new HashSet<>();

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
    }

    @Override
    public void sawOpcode(int seen) {
        if (Const.CONSTRUCTOR_NAME.equals(getMethodName()) || Const.STATIC_INITIALIZER_NAME.equals(getMethodName())
                || MultiThreadedCodeIdentifierUtils.isLocked(currentMethod, currentCFG, currentLockDataFlow, getPC())) {
            return;
        }
        XMethod method = getXMethod();
        if (isFirstVisit) {
            collectFieldReads(seen, method);
        } else {
            checkAndReportBug(seen, method);
        }
    }

    private void collectFieldReads(int seen, XMethod method) {
        if (seen == Const.GETFIELD || seen == Const.GETSTATIC) {
            addNonFinalFields(getXFieldOperand(), method, readFieldsByMethods);

        } else if (seen == Const.IFGE || seen == Const.IFGT || seen == Const.IFLT || seen == Const.IFLE || seen == Const.IFNE || seen == Const.IFEQ) {
            XField lhs = stack.getStackItem(0).getXField();
            XField rhs = stack.getStackDepth() > 1 ? stack.getStackItem(1).getXField() : null;
            addNonFinalFields(lhs, method, readFieldsByMethods);
            addNonFinalFields(rhs, method, readFieldsByMethods);
        }
    }

    private void addNonFinalFields(XField field, XMethod method, Map<XMethod, List<XField>> map) {
        if (field != null && !field.isFinal()) {
            map.computeIfAbsent(method, k -> new ArrayList<>()).add(field);
        }
    }

    private boolean mapContainsFieldWithOtherMethod(XField field, XMethod method, Map<XMethod, List<XField>> map) {
        return map.entrySet().stream().anyMatch(entry -> entry.getValue().contains(field) && entry.getKey() != method);
    }

    private void checkAndReportBug(int seen, XMethod method) {
        if (seen == Const.GETFIELD || seen == Const.GETSTATIC) {
            XField field = getXFieldOperand();
            if (field != null) {
                relevantFields.add(field);
            }
        } else if (seen == Const.PUTFIELD || seen == Const.PUTSTATIC) {
            XField field = getXFieldOperand();
            if (field != null && !field.isFinal()) {
                boolean fieldReadInOtherMethod = mapContainsFieldWithOtherMethod(field, method, readFieldsByMethods);
                if (fieldReadInOtherMethod) {
                    if (!relevantFields.isEmpty() && relevantFields.contains(field) && !MultiThreadedCodeIdentifierUtils.isFromAtomicPackage(field.getSignature())) {
                        bugAccumulator.accumulateBug(
                                new BugInstance(this, "AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE", NORMAL_PRIORITY)
                                        .addClass(this)
                                        .addMethod(method)
                                        .addField(field),
                                this);
                    } else if (!field.isVolatile() && ClassName.isValidBaseTypeFieldDescriptor(field.getSignature())) {
                        String bugType = is64bitPrimitive(field.getSignature()) ? "AT_NONATOMIC_64BIT_PRIMITIVE" : "AT_STALE_THREAD_WRITE_OF_PRIMITIVE";
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
        }
    }

    private boolean is64bitPrimitive(String className) {
        return "D".equals(className) || "J".equals(className);
    }
}
