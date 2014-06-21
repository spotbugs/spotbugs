/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

import java.util.BitSet;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.UseAnnotationDatabase;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CheckReturnAnnotationDatabase;
import edu.umd.cs.findbugs.ba.CheckReturnValueAnnotation;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

/**
 * Look for calls to methods where the return value is erroneously ignored. This
 * detector is meant as a simpler and faster replacement for
 * BCPMethodReturnCheck.
 *
 * @author David Hovemeyer
 */
public class MethodReturnCheck extends OpcodeStackDetector implements UseAnnotationDatabase {
    private static final boolean DEBUG = SystemProperties.getBoolean("mrc.debug");

    private static final int SCAN = 0;

    private static final int SAW_INVOKE = 1;

    private static final BitSet INVOKE_OPCODE_SET = new BitSet();
    static {
        INVOKE_OPCODE_SET.set(Constants.INVOKEINTERFACE);
        INVOKE_OPCODE_SET.set(Constants.INVOKESPECIAL);
        INVOKE_OPCODE_SET.set(Constants.INVOKESTATIC);
        INVOKE_OPCODE_SET.set(Constants.INVOKEVIRTUAL);
    }

    boolean previousOpcodeWasNEW;

    private final BugAccumulator bugAccumulator;

    private CheckReturnAnnotationDatabase checkReturnAnnotationDatabase;

    private XMethod callSeen;

    private int state;

    private int callPC;

    public MethodReturnCheck(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        checkReturnAnnotationDatabase = AnalysisContext.currentAnalysisContext().getCheckReturnAnnotationDatabase();
        super.visitClassContext(classContext);
    }

    @Override
    public void visitAfter(Code code) {
        bugAccumulator.reportAccumulatedBugs();
    }

    private boolean badUseOfCompareResult(Item left, Item right) {
        XMethod m = left.getReturnValueOf();

        if (m == null) {
            return false;
        }
        String name = m.getName();

        if (!name.startsWith("compare")) {
            return false;
        }

        Object value = right.getConstant();
        if (!(value instanceof Integer) || ((Integer) value).intValue() == 0) {
            return false;
        }
        if (!m.isPublic() && m.isResolved()) {
            return false;
        }


        if (m.isStatic() || !m.isResolved()) {
            if (name.equals("compare") && m.getClassName().startsWith("com.google.common.primitives.")) {
                return true;
            }
        }
        if (!m.isStatic() || !m.isResolved()) {
            if (name.equals("compareTo") && m.getSignature().equals("(Ljava/lang/Object;)I")) {
                return true;
            }
            if (name.equals("compare") && m.getSignature().equals("(Ljava/lang/Object;Ljava/lang/Object;)I")) {
                return true;
            }
        }

        return false;

    }
    @Override
    public void sawOpcode(int seen) {

        if (DEBUG) {
            System.out.printf("%3d %10s %3s %s%n", getPC(), OPCODE_NAMES[seen], state, stack);
        }

        switch (seen) {
        case Constants.IF_ICMPEQ:

        case Constants.IF_ICMPNE:
            OpcodeStack.Item left = stack.getStackItem(1);
            OpcodeStack.Item right = stack.getStackItem(0);
            if (badUseOfCompareResult(left, right)) {
                XMethod returnValueOf = left.getReturnValueOf();
                assert returnValueOf != null;
                bugAccumulator.accumulateBug(new BugInstance(this, "RV_CHECK_COMPARETO_FOR_SPECIFIC_RETURN_VALUE", NORMAL_PRIORITY)
                .addClassAndMethod(this).addMethod(returnValueOf).describe(MethodAnnotation.METHOD_CALLED).addValueSource(right, this), this);
            } else if (badUseOfCompareResult(right, left)) {
                XMethod returnValueOf = right.getReturnValueOf();
                assert returnValueOf != null;
                bugAccumulator.accumulateBug(new BugInstance(this, "RV_CHECK_COMPARETO_FOR_SPECIFIC_RETURN_VALUE", NORMAL_PRIORITY)
                .addClassAndMethod(this).addMethod(returnValueOf).describe(MethodAnnotation.METHOD_CALLED).addValueSource(left, this), this);
            }
            break;
        default:
            break;
        }

        checkForInitWithoutCopyOnStack: if (seen == INVOKESPECIAL && getNameConstantOperand().equals("<init>")) {
            int arguments = PreorderVisitor.getNumberArguments(getSigConstantOperand());
            OpcodeStack.Item invokedOn = stack.getStackItem(arguments);
            if (invokedOn.isNewlyAllocated() && (!getMethodName().equals("<init>") || invokedOn.getRegisterNumber() != 0)) {

                for (int i = arguments + 1; i < stack.getStackDepth(); i++) {
                    OpcodeStack.Item item = stack.getStackItem(i);
                    if (item.isNewlyAllocated() && item.getSignature().equals(invokedOn.getSignature())) {
                        break checkForInitWithoutCopyOnStack;
                    }
                }

                callSeen = XFactory.createReferencedXMethod(this);
                callPC = getPC();
                sawMethodCallWithIgnoredReturnValue();
                state = SCAN;
                previousOpcodeWasNEW = false;
                return;

            }
        }

        if (state == SAW_INVOKE && isPop(seen)) {
            sawMethodCallWithIgnoredReturnValue();
        } else if (INVOKE_OPCODE_SET.get(seen)) {
            callPC = getPC();
            callSeen = XFactory.createReferencedXMethod(this);
            state = SAW_INVOKE;
            if (DEBUG) {
                System.out.println("  invoking " + callSeen);
            }
        } else {
            state = SCAN;
        }

        if (seen == NEW) {
            previousOpcodeWasNEW = true;
        } else {
            if (seen == INVOKESPECIAL && previousOpcodeWasNEW) {
                CheckReturnValueAnnotation annotation = checkReturnAnnotationDatabase.getResolvedAnnotation(callSeen, false);
                if (annotation != null && annotation != CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE) {
                    int priority = annotation.getPriority();
                    if (!checkReturnAnnotationDatabase.annotationIsDirect(callSeen)
                            && !callSeen.getSignature().endsWith(callSeen.getClassName().replace('.', '/') + ";")) {
                        priority++;
                    }
                    bugAccumulator.accumulateBug(new BugInstance(this, annotation.getPattern(), priority).addClassAndMethod(this)
                            .addCalledMethod(this), this);
                }

            }
            previousOpcodeWasNEW = false;
        }

    }

    /**
     *
     */
    private void sawMethodCallWithIgnoredReturnValue() {
        {
            CheckReturnValueAnnotation annotation = checkReturnAnnotationDatabase.getResolvedAnnotation(callSeen, false);
            if (annotation == null) {
                XFactory xFactory = AnalysisContext.currentXFactory();

                if (xFactory.isFunctionshatMightBeMistakenForProcedures(callSeen.getMethodDescriptor())) {
                    annotation = CheckReturnValueAnnotation.CHECK_RETURN_VALUE_INFERRED;
                }
            }
            if (annotation != null && annotation.getPriority() <= LOW_PRIORITY) {
                int popPC = getPC();
                if (DEBUG) {
                    System.out.println("Saw POP @" + popPC);
                }
                int catchSize = getSizeOfSurroundingTryBlock(popPC);

                int priority = annotation.getPriority();
                if (catchSize <= 1) {
                    priority += 2;
                } else if (catchSize <= 2) {
                    priority += 1;
                }
                if (!checkReturnAnnotationDatabase.annotationIsDirect(callSeen)
                        && !callSeen.getSignature().endsWith(callSeen.getClassName().replace('.', '/') + ";")) {
                    priority++;
                }
                if (callSeen.isPrivate()) {
                    priority++;
                }
                if (callSeen.getName().equals("clone") || callSeen.getName().startsWith("get")) {
                    priority++;
                }
                String pattern = annotation.getPattern();
                if (callSeen.getName().equals("<init>")
                        && (callSeen.getClassName().endsWith("Exception") || callSeen.getClassName().endsWith("Error"))) {
                    pattern = "RV_EXCEPTION_NOT_THROWN";
                }
                BugInstance warning = new BugInstance(this, pattern, priority).addClassAndMethod(this).addMethod(callSeen)
                        .describe(MethodAnnotation.METHOD_CALLED);
                bugAccumulator.accumulateBug(warning, SourceLineAnnotation.fromVisitedInstruction(this, callPC));
            }
            state = SCAN;
        }
    }

    private boolean isPop(int seen) {
        return seen == Constants.POP || seen == Constants.POP2;
    }

}
