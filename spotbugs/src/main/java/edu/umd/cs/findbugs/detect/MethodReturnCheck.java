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

import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.Values;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.UseAnnotationDatabase;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.detect.FindNoSideEffectMethods.NoSideEffectMethodsDatabase;
import edu.umd.cs.findbugs.util.ClassName;
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

    private static enum State {
        SCAN,
        SAW_INVOKE;
    }

    private static final BitSet INVOKE_OPCODE_SET = new BitSet();
    static {
        INVOKE_OPCODE_SET.set(Const.INVOKEINTERFACE);
        INVOKE_OPCODE_SET.set(Const.INVOKESPECIAL);
        INVOKE_OPCODE_SET.set(Const.INVOKESTATIC);
        INVOKE_OPCODE_SET.set(Const.INVOKEVIRTUAL);
    }

    boolean previousOpcodeWasNEW;

    private final BugAccumulator bugAccumulator;

    private CheckReturnAnnotationDatabase checkReturnAnnotationDatabase;

    private XMethod callSeen;

    private State state;

    private int callPC;

    private final NoSideEffectMethodsDatabase noSideEffectMethods;

    private boolean sawExcludedNSECall;

    private boolean sawMockitoInvoke;

    public MethodReturnCheck(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
        this.noSideEffectMethods = Global.getAnalysisCache().getDatabase(NoSideEffectMethodsDatabase.class);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        checkReturnAnnotationDatabase = AnalysisContext.currentAnalysisContext().getCheckReturnAnnotationDatabase();
        super.visitClassContext(classContext);
    }

    @Override
    public void visitAfter(Code code) {
        if (bugAccumulator.getLastBugLocation() == null && !sawExcludedNSECall && noSideEffectMethods.useless(getMethodDescriptor())) {
            // Do not report UC_USELESS_VOID_METHOD if something was already reported inside the current method
            // it's likely that UC_USELESS_VOID_METHOD is just the consequence of the previous report
            bugAccumulator.accumulateBug(new BugInstance(this, "UC_USELESS_VOID_METHOD",
                    code.getCode().length > 40 ? HIGH_PRIORITY : code.getCode().length > 15 ? NORMAL_PRIORITY : LOW_PRIORITY)
                            .addClassAndMethod(getMethodDescriptor()), this);
        }
        sawExcludedNSECall = false;
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
            if ("compare".equals(name) && m.getClassName().startsWith("com.google.common.primitives.")) {
                return true;
            }
        }
        if (!m.isStatic() || !m.isResolved()) {
            if ("compareTo".equals(name) && "(Ljava/lang/Object;)I".equals(m.getSignature())) {
                return true;
            }
            if ("compare".equals(name) && "(Ljava/lang/Object;Ljava/lang/Object;)I".equals(m.getSignature())) {
                return true;
            }
        }

        return false;

    }

    @Override
    public void sawOpcode(int seen) {

        if (DEBUG) {
            System.out.printf("%3d %10s %3s %s%n", getPC(), Const.getOpcodeName(seen), state, stack);
        }

        switch (seen) {
        case Const.IF_ICMPEQ:

        case Const.IF_ICMPNE:
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

        checkForInitWithoutCopyOnStack: if (seen == Const.INVOKESPECIAL && Const.CONSTRUCTOR_NAME.equals(getNameConstantOperand())) {
            int arguments = PreorderVisitor.getNumberArguments(getSigConstantOperand());
            OpcodeStack.Item invokedOn = stack.getStackItem(arguments);
            if (invokedOn.isNewlyAllocated() && (!Const.CONSTRUCTOR_NAME.equals(getMethodName()) || invokedOn.getRegisterNumber() != 0)) {

                for (int i = arguments + 1; i < stack.getStackDepth(); i++) {
                    OpcodeStack.Item item = stack.getStackItem(i);
                    if (item.isNewlyAllocated() && item.getSignature().equals(invokedOn.getSignature())) {
                        break checkForInitWithoutCopyOnStack;
                    }
                }

                callSeen = XFactory.createReferencedXMethod(this);
                callPC = getPC();
                sawMethodCallWithIgnoredReturnValue();
                state = State.SCAN;
                previousOpcodeWasNEW = false;
                return;

            }
        }

        if (state == State.SAW_INVOKE && isPop(seen)) {
            if (!sawMockitoInvoke) {
                sawMethodCallWithIgnoredReturnValue();
            }
            sawMockitoInvoke = false;
        } else if (INVOKE_OPCODE_SET.get(seen)) {
            callPC = getPC();
            callSeen = XFactory.createReferencedXMethod(this);
            state = State.SAW_INVOKE;
            sawMockitoInvoke |= isCallMockitoVerifyInvocation(callSeen);
            if (DEBUG) {
                System.out.println("  invoking " + callSeen);
            }
        } else {
            state = State.SCAN;
        }

        if (seen == Const.NEW) {
            previousOpcodeWasNEW = true;
        } else {
            if (seen == Const.INVOKESPECIAL && previousOpcodeWasNEW && !sawMockitoInvoke) {
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

    private boolean isCallMockitoVerifyInvocation(XMethod method) {
        return method.isStatic() && "verify".equals(method.getName()) && "org.mockito.Mockito".equals(method.getClassName());
    }

    /**
     *
     */
    private void sawMethodCallWithIgnoredReturnValue() {
        {
            CheckReturnValueAnnotation annotation = checkReturnAnnotationDatabase.getResolvedAnnotation(callSeen, false);
            if (annotation == null) {
                if (noSideEffectMethods.excluded(callSeen.getMethodDescriptor())) {
                    sawExcludedNSECall = true;
                }
                if (noSideEffectMethods.hasNoSideEffect(callSeen.getMethodDescriptor())) {
                    int priority = NORMAL_PRIORITY;
                    Type callReturnType = Type.getReturnType(callSeen.getMethodDescriptor().getSignature());
                    Type methodReturnType = Type.getReturnType(getMethodSig());
                    if (callReturnType.equals(methodReturnType) && callReturnType != Type.BOOLEAN && callReturnType != Type.VOID) {
                        priority = HIGH_PRIORITY;
                    } else {
                        String callReturnClass = callSeen.getName().equals(Const.CONSTRUCTOR_NAME) ? callSeen.getClassDescriptor().getClassName()
                                : ClassName.fromFieldSignature(callReturnType.getSignature());

                        String methodReturnClass = ClassName.fromFieldSignature(methodReturnType.getSignature());
                        if (callReturnClass != null && methodReturnClass != null &&
                                Subtypes2.instanceOf(ClassName.toDottedClassName(callReturnClass), ClassName.toDottedClassName(methodReturnClass))) {
                            priority = HIGH_PRIORITY;
                        }
                    }
                    int catchSize = getSizeOfSurroundingTryBlock(getPC());
                    if (catchSize <= 2) {
                        priority++;
                    }
                    BugInstance warning = new BugInstance(this, "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", priority)
                            .addClassAndMethod(this).addMethod(callSeen).describe(MethodAnnotation.METHOD_CALLED);
                    bugAccumulator.accumulateBug(warning, SourceLineAnnotation.fromVisitedInstruction(this, callPC));
                } else {
                    XFactory xFactory = AnalysisContext.currentXFactory();

                    if (xFactory.isFunctionshatMightBeMistakenForProcedures(callSeen.getMethodDescriptor())) {
                        annotation = CheckReturnValueAnnotation.CHECK_RETURN_VALUE_INFERRED;
                    }
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
                if ("clone".equals(callSeen.getName()) || callSeen.getName().startsWith("get")) {
                    priority++;
                }
                String pattern = annotation.getPattern();
                if (Const.CONSTRUCTOR_NAME.equals(callSeen.getName())
                        && Subtypes2.instanceOf(callSeen.getClassName(), Values.DOTTED_JAVA_LANG_THROWABLE)) {
                    pattern = "RV_EXCEPTION_NOT_THROWN";
                }
                BugInstance warning = new BugInstance(this, pattern, priority).addClassAndMethod(this).addMethod(callSeen)
                        .describe(MethodAnnotation.METHOD_CALLED);
                bugAccumulator.accumulateBug(warning, SourceLineAnnotation.fromVisitedInstruction(this, callPC));
            } else {
                MethodDescriptor methodDescriptor = callSeen.getMethodDescriptor();
                SignatureParser methodSigParser = new SignatureParser(methodDescriptor.getSignature());
                String returnTypeSig = methodSigParser.getReturnTypeSignature();
                String returnType = ClassName.fromFieldSignature(returnTypeSig);
                if (returnType != null
                        && Subtypes2.instanceOf(ClassName.toDottedClassName(returnType), Values.DOTTED_JAVA_LANG_THROWABLE)
                        && !("initCause".equals(methodDescriptor.getName()) && methodSigParser.getArguments().length == 1
                                && "Ljava/lang/Throwable;".equals(methodSigParser.getArguments()[0]))
                        && !("getCause".equals(methodDescriptor.getName()) && methodSigParser.getArguments().length == 0)) {
                    BugInstance warning = new BugInstance(this, "RV_EXCEPTION_NOT_THROWN", 1).addClassAndMethod(this).addMethod(callSeen)
                            .describe(MethodAnnotation.METHOD_CALLED);
                    bugAccumulator.accumulateBug(warning, SourceLineAnnotation.fromVisitedInstruction(this, callPC));
                }
            }
            state = State.SCAN;
        }
    }

    private boolean isPop(int seen) {
        return seen == Const.POP || seen == Const.POP2;
    }

}
