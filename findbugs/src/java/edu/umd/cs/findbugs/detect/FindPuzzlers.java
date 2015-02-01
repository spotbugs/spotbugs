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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.IntAnnotation;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.visitclass.Util;

public class FindPuzzlers extends OpcodeStackDetector {

    static FieldDescriptor SYSTEM_OUT = new FieldDescriptor("java/lang/System", "out", "Ljava/io/PrintStream;", true);

    static FieldDescriptor SYSTEM_ERR = new FieldDescriptor("java/lang/System", "err", "Ljava/io/PrintStream;", true);

    final BugReporter bugReporter;

    final BugAccumulator bugAccumulator;

    private final boolean testingEnabled;

    public FindPuzzlers(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
        testingEnabled = SystemProperties.getBoolean("report_TESTING_pattern_in_standard_detectors");
    }

    @Override
    public void visit(Code obj) {
        prevOpcodeIncrementedRegister = -1;
        best_priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG = LOW_PRIORITY + 1;
        prevOpCode = NOP;
        previousMethodInvocation = null;
        badlyComputingOddState = 0;
        resetIMulCastLong();
        imul_distance = 10000;
        ternaryConversionState = 0;
        becameTop = -1;
        super.visit(obj);
        bugAccumulator.reportAccumulatedBugs();
        pendingUnreachableBranch = null;
    }

    int becameTop;
    int imul_constant;

    int imul_distance;

    boolean imul_operand_is_parameter;

    int prevOpcodeIncrementedRegister;

    int valueOfConstantArgumentToShift;

    int best_priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG;

    boolean constantArgumentToShift;

    boolean shiftOfNonnegativeValue;

    int ternaryConversionState = 0;

    int badlyComputingOddState;

    int prevOpCode;

    XMethod previousMethodInvocation;

    boolean isTigerOrHigher;

    static ClassDescriptor ITERATOR = DescriptorFactory.createClassDescriptor(Iterator.class);
    static ClassDescriptor MAP_ENTRY = DescriptorFactory.createClassDescriptor(Map.Entry.class);


    @Override
    public void visit(JavaClass obj) {
        isTigerOrHigher = obj.getMajor() >= MAJOR_1_5;
        try {
            Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
            ClassDescriptor me = getClassDescriptor();
            if (subtypes2.isSubtype(me, MAP_ENTRY) && subtypes2.isSubtype(me, ITERATOR)) {
                bugReporter.reportBug(new BugInstance(this, "PZ_DONT_REUSE_ENTRY_OBJECTS_IN_ITERATORS", NORMAL_PRIORITY)
                .addClass(this).addString("shouldn't reuse Iterator as a Map.Entry"));
            }
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
    }

    private void resetIMulCastLong() {
        imul_constant = 1;
        imul_operand_is_parameter = false;
    }

    private int adjustPriority(int factor, int priority) {
        if (factor <= 4) {
            return LOW_PRIORITY + 2;
        }
        if (factor <= 10000) {
            return priority + 1;
        }
        if (factor <= 60 * 60 * 1000) {
            return priority;
        }
        return priority - 1;
    }

    private int adjustMultiplier(Object constant, int mul) {
        if (!(constant instanceof Integer)) {
            return mul;
        }
        return Math.abs(((Integer) constant).intValue()) * mul;

    }

    @Override
    public boolean beforeOpcode(int seen) {
        super.beforeOpcode(seen);
        return true;
    }

    BugInstance pendingUnreachableBranch;

    @Override
    public void sawOpcode(int seen) {

        if (stack.isTop()) {
            pendingUnreachableBranch = null;
            if (becameTop == -1) {
                becameTop = getPC();
            }
            if (testingEnabled && seen == GOTO && getBranchTarget() < becameTop)  {
                pendingUnreachableBranch = new BugInstance(this, "TESTING", NORMAL_PRIORITY)
                .addClassAndMethod(this).addString("Unreachable loop body").addSourceLineRange(this, becameTop, getPC());
            }
            return;

        }
        if (pendingUnreachableBranch != null) {
            bugReporter.reportBug(pendingUnreachableBranch);
            pendingUnreachableBranch = null;
        }
        becameTop = -1;

        if (seen == INVOKESPECIAL && "<init>".equals(getNameConstantOperand()) && "(Ljava/util/Collection;)V".equals(getSigConstantOperand())
                && getClassConstantOperand().contains("Set")
                || (seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE) && "addAll".equals(getNameConstantOperand()) && "(Ljava/util/Collection;)Z".equals(getSigConstantOperand())) {
            OpcodeStack.Item top = stack.getStackItem(0);
            XMethod returnValueOf = top.getReturnValueOf();
            if (returnValueOf != null && "entrySet".equals(returnValueOf.getName())) {
                String name = returnValueOf.getClassName();
                int priority = Priorities.LOW_PRIORITY;
                if ("java.util.Map".equals(name)) {
                    priority = Priorities.NORMAL_PRIORITY;
                } else if (name.equals(EnumMap.class.getName())
                        || name.equals(IdentityHashMap.class.getName())) {
                    priority = Priorities.HIGH_PRIORITY;
                }
                bugReporter.reportBug(new BugInstance(this, "DMI_ENTRY_SETS_MAY_REUSE_ENTRY_OBJECTS", priority)
                .addClassAndMethod(this).addCalledMethod(returnValueOf).addCalledMethod(this).addValueSource(top, this).addSourceLine(this));
            }


        }

        if (seen == INVOKEVIRTUAL && "hashCode".equals(getNameConstantOperand()) && "()I".equals(getSigConstantOperand())
                && stack.getStackDepth() > 0) {
            OpcodeStack.Item item0 = stack.getStackItem(0);
            if (item0.getSignature().charAt(0) == '[') {
                bugReporter.reportBug(new BugInstance(this, "DMI_INVOKING_HASHCODE_ON_ARRAY", NORMAL_PRIORITY)
                .addClassAndMethod(this).addValueSource(item0, this).addSourceLine(this));
            }
        }
        if (seen != RETURN && isReturn(seen) && isRegisterStore(getPrevOpcode(1))) {

            int priority = Priorities.NORMAL_PRIORITY;
            if (getMethodSig().endsWith(")Z")) {
                priority = Priorities.HIGH_PRIORITY;
            } else {
                if (getMethodSig().endsWith(")Ljava/lang/String;")) {
                    priority = Priorities.LOW_PRIORITY;
                }
                if (getPC() == getCode().getCode().length - 1) {
                    priority++;
                }
            }
            bugReporter.reportBug(new BugInstance(this, "DLS_DEAD_LOCAL_STORE_IN_RETURN", priority).addClassAndMethod(this)
                    .addSourceLine(this));
        }
        // System.out.println(getPC() + " " + OPCODE_NAMES[seen] + " " +
        // ternaryConversionState);
        if (seen == IMUL) {
            if (imul_distance != 1) {
                resetIMulCastLong();
            }
            imul_distance = 0;
            if (stack.getStackDepth() > 1) {
                OpcodeStack.Item item0 = stack.getStackItem(0);
                OpcodeStack.Item item1 = stack.getStackItem(1);
                imul_constant = adjustMultiplier(item0.getConstant(), imul_constant);
                imul_constant = adjustMultiplier(item1.getConstant(), imul_constant);

                if (item0.isInitialParameter() || item1.isInitialParameter()) {
                    imul_operand_is_parameter = true;
                }
            }
        } else {
            imul_distance++;
        }

        if (prevOpCode == IMUL && seen == I2L) {
            int priority = adjustPriority(imul_constant, NORMAL_PRIORITY);
            if (priority >= LOW_PRIORITY && imul_constant != 1000 && imul_constant != 60 && imul_operand_is_parameter) {
                priority = NORMAL_PRIORITY;
            }
            if (priority <= best_priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG) {
                best_priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG = priority;
                bugAccumulator.accumulateBug(
                        new BugInstance(this, "ICAST_INTEGER_MULTIPLY_CAST_TO_LONG", priority).addClassAndMethod(this), this);
            }
        }

        if ("<clinit>".equals(getMethodName()) && (seen == PUTSTATIC || seen == GETSTATIC || seen == INVOKESTATIC)) {
            String clazz = getClassConstantOperand();
            if (!clazz.equals(getClassName())) {
                try {
                    JavaClass targetClass = Repository.lookupClass(clazz);
                    if (Repository.instanceOf(targetClass, getThisClass())) {
                        int priority = NORMAL_PRIORITY;
                        if (seen == GETSTATIC) {
                            priority--;
                        }
                        if (!targetClass.isPublic()) {
                            priority++;
                        }
                        bugAccumulator.accumulateBug(new BugInstance(this, "IC_SUPERCLASS_USES_SUBCLASS_DURING_INITIALIZATION",
                                priority).addClassAndMethod(this).addClass(getClassConstantOperand()), this);

                    }
                } catch (ClassNotFoundException e) {
                    // ignore it
                }

            }
        }
        /*
        if (false && (seen == INVOKEVIRTUAL) && getNameConstantOperand().equals("equals")
                && getSigConstantOperand().equals("(Ljava/lang/Object;)Z") && stack.getStackDepth() > 1) {
            OpcodeStack.Item item0 = stack.getStackItem(0);
            OpcodeStack.Item item1 = stack.getStackItem(1);

            if (item0.isArray() || item1.isArray()) {
                bugAccumulator.accumulateBug(
                        new BugInstance(this, "EC_BAD_ARRAY_COMPARE", NORMAL_PRIORITY).addClassAndMethod(this), this);
            }
        }
         */

        if (seen >= IALOAD && seen <= SALOAD || seen >= IASTORE && seen <= SASTORE) {
            Item index = stack.getStackItem(0);
            if (index.getSpecialKind() == Item.AVERAGE_COMPUTED_USING_DIVISION) {
                SourceLineAnnotation where;
                if (index.getPC() >= 0) {
                    where = SourceLineAnnotation.fromVisitedInstruction(this, index.getPC());
                } else {
                    where = SourceLineAnnotation.fromVisitedInstruction(this);
                }
                bugAccumulator.accumulateBug(
                        new BugInstance(this, "IM_AVERAGE_COMPUTATION_COULD_OVERFLOW", NORMAL_PRIORITY).addClassAndMethod(this),
                        where);
            }

        }

        if ((seen == IFEQ || seen == IFNE) && getPrevOpcode(1) == IMUL
                && (getPrevOpcode(2) == SIPUSH || getPrevOpcode(2) == BIPUSH) && getPrevOpcode(3) == IREM) {
            bugAccumulator.accumulateBug(
                    new BugInstance(this, "IM_MULTIPLYING_RESULT_OF_IREM", LOW_PRIORITY).addClassAndMethod(this), this);
        }

        if (seen == I2S && getPrevOpcode(1) == IUSHR && !shiftOfNonnegativeValue
                && (!constantArgumentToShift || valueOfConstantArgumentToShift % 16 != 0) || seen == I2B
                && getPrevOpcode(1) == IUSHR && !shiftOfNonnegativeValue
                && (!constantArgumentToShift || valueOfConstantArgumentToShift % 8 != 0)) {
            bugAccumulator.accumulateBug(
                    new BugInstance(this, "ICAST_QUESTIONABLE_UNSIGNED_RIGHT_SHIFT", NORMAL_PRIORITY).addClassAndMethod(this),
                    this);
        }


        if (seen == IADD && (getNextOpcode() == ISHL || getNextOpcode() == LSHL) && stack.getStackDepth() >=3) {
            OpcodeStack.Item l = stack.getStackItem(2);
            OpcodeStack.Item v = stack.getStackItem(1);
            Object constantValue = v.getConstant();
            // Ignore 1 << (const + var) as it's usually intended
            if (constantValue instanceof Integer && !Integer.valueOf(1).equals(l.getConstant())) {
                int c = ((Integer) constantValue).intValue();
                int priority = LOW_PRIORITY;
                // If (foo << 32 + var) encountered, then ((foo << 32) + var) is absolutely meaningless,
                // but (foo << (32 + var)) can be meaningful for negative var values
                if (c < 32 || (c < 64 && getNextOpcode() == LSHL)) {
                    if (c == 8) {
                        priority--;
                    }
                    if (getPrevOpcode(1) == IAND) {
                        priority--;
                    }
                    if (getMethodName().equals("hashCode") && getMethodSig().equals("()I")
                            && (getCode().getCode()[getNextPC() + 1] & 0xFF) == IRETURN) {
                        // commonly observed error is hashCode body like "return foo << 16 + bar;"
                        priority = HIGH_PRIORITY;
                    }
                    bugAccumulator.accumulateBug(new BugInstance(this, "BSHIFT_WRONG_ADD_PRIORITY", priority)
                    .addClassAndMethod(this)
                    .addInt(c).describe(IntAnnotation.INT_SHIFT)
                    .addValueSource(stack.getStackItem(2), this)
                    .addValueSource(stack.getStackItem(0), this)
                    , this);
                }
            }

        }
        constantArgumentToShift = false;
        shiftOfNonnegativeValue = false;
        if ((seen == IUSHR || seen == ISHR || seen == ISHL)) {
            if (stack.getStackDepth() <= 1) {
                // don't understand; lie so other detectors won't get concerned
                constantArgumentToShift = true;
                valueOfConstantArgumentToShift = 8;
            } else {
                Object rightHandSide = stack.getStackItem(0).getConstant();

                Object leftHandSide = stack.getStackItem(1).getConstant();
                shiftOfNonnegativeValue = stack.getStackItem(1).isNonNegative();
                if (rightHandSide instanceof Integer) {
                    constantArgumentToShift = true;
                    valueOfConstantArgumentToShift = ((Integer) rightHandSide);
                    if (valueOfConstantArgumentToShift < 0 || valueOfConstantArgumentToShift >= 32) {
                        bugAccumulator.accumulateBug(new BugInstance(this, "ICAST_BAD_SHIFT_AMOUNT",
                                valueOfConstantArgumentToShift < 0 ? LOW_PRIORITY : (valueOfConstantArgumentToShift == 32
                                && "hashCode".equals(getMethodName()) ? NORMAL_PRIORITY : HIGH_PRIORITY))
                        .addClassAndMethod(this).addInt(valueOfConstantArgumentToShift).describe(IntAnnotation.INT_SHIFT)
                        .addValueSource(stack.getStackItem(1), this), this);
                    }
                }
                if (leftHandSide != null && leftHandSide instanceof Integer && ((Integer) leftHandSide) > 0) {
                    // boring; lie so other detectors won't get concerned
                    constantArgumentToShift = true;
                    valueOfConstantArgumentToShift = 8;
                }

            }
        }

        if (seen == INVOKEVIRTUAL && stack.getStackDepth() > 0
                && ("java/util/Date".equals(getClassConstantOperand()) || "java/sql/Date".equals(getClassConstantOperand()))
                && "setMonth".equals(getNameConstantOperand()) && "(I)V".equals(getSigConstantOperand())) {
            OpcodeStack.Item item = stack.getStackItem(0);
            Object o = item.getConstant();
            if (o != null && o instanceof Integer) {
                int v = (Integer) o;
                if (v < 0 || v > 11) {
                    bugReporter.reportBug(new BugInstance(this, "DMI_BAD_MONTH", HIGH_PRIORITY).addClassAndMethod(this).addInt(v)
                            .describe(IntAnnotation.INT_VALUE).addCalledMethod(this).addSourceLine(this));
                }
            }
        }

        if (seen == INVOKEVIRTUAL && stack.getStackDepth() > 1 && "java/util/Calendar".equals(getClassConstantOperand())
                && "set".equals(getNameConstantOperand())

                || seen == INVOKESPECIAL && stack.getStackDepth() > 1
                && "java/util/GregorianCalendar".equals(getClassConstantOperand()) && "<init>".equals(getNameConstantOperand())

                ) {
            String sig = getSigConstantOperand();
            if (sig.startsWith("(III")) {
                int pos = sig.length() - 5;
                OpcodeStack.Item item = stack.getStackItem(pos);
                Object o = item.getConstant();
                if (o != null && o instanceof Integer) {
                    int v = (Integer) o;
                    if (v < 0 || v > 11) {
                        bugReporter.reportBug(new BugInstance(this, "DMI_BAD_MONTH", NORMAL_PRIORITY).addClassAndMethod(this)
                                .addInt(v).describe(IntAnnotation.INT_VALUE).addCalledMethod(this).addSourceLine(this));
                    }
                }
            }
        }

        if (isRegisterStore() && (seen == ISTORE || seen == ISTORE_0 || seen == ISTORE_1 || seen == ISTORE_2 || seen == ISTORE_3)
                && getRegisterOperand() == prevOpcodeIncrementedRegister) {
            bugAccumulator.accumulateBug(
                    new BugInstance(this, "DLS_OVERWRITTEN_INCREMENT", HIGH_PRIORITY).addClassAndMethod(this), this);

        }
        if (seen == IINC) {
            prevOpcodeIncrementedRegister = getRegisterOperand();
        } else {
            prevOpcodeIncrementedRegister = -1;
        }

        // Java Puzzlers, Chapter 2, puzzle 1
        // Look for ICONST_2 IREM ICONST_1 IF_ICMPNE L1

        switch (badlyComputingOddState) {
        case 0:
            if (seen == ICONST_2) {
                badlyComputingOddState++;
            }
            break;
        case 1:
            if (seen == IREM) {
                OpcodeStack.Item item = stack.getStackItem(1);
                if (!item.isNonNegative() && item.getSpecialKind() != OpcodeStack.Item.MATH_ABS) {
                    badlyComputingOddState++;
                } else {
                    badlyComputingOddState = 0;
                }
            } else {
                badlyComputingOddState = 0;
            }
            break;
        case 2:
            if (seen == ICONST_1) {
                badlyComputingOddState++;
            } else {
                badlyComputingOddState = 0;
            }
            break;
        case 3:
            if (seen == IF_ICMPEQ || seen == IF_ICMPNE) {
                bugAccumulator.accumulateBug(
                        new BugInstance(this, "IM_BAD_CHECK_FOR_ODD", NORMAL_PRIORITY).addClassAndMethod(this), this);
            }
            badlyComputingOddState = 0;
            break;
        default:
            break;
        }

        // Java Puzzlers, chapter 3, puzzle 12
        if (seen == INVOKEVIRTUAL
                && stack.getStackDepth() > 0
                && ("toString".equals(getNameConstantOperand()) && "()Ljava/lang/String;".equals(getSigConstantOperand())
                        || "append".equals(getNameConstantOperand())
                        && "(Ljava/lang/Object;)Ljava/lang/StringBuilder;".equals(getSigConstantOperand())
                        && "java/lang/StringBuilder".equals(getClassConstantOperand())
                        || "append".equals(getNameConstantOperand())
                        && "(Ljava/lang/Object;)Ljava/lang/StringBuffer;".equals(getSigConstantOperand())
                        && "java/lang/StringBuffer".equals(getClassConstantOperand()) || ("print".equals(getNameConstantOperand()) || "println".equals(getNameConstantOperand()))
                        && "(Ljava/lang/Object;)V".equals(getSigConstantOperand()))) {
            OpcodeStack.Item item = stack.getStackItem(0);
            String signature = item.getSignature();
            if (signature != null && signature.startsWith("[")) {
                boolean debuggingContext = "[Ljava/lang/StackTraceElement;".equals(signature);

                if (!debuggingContext) {
                    for (CodeException e : getCode().getExceptionTable()) {
                        if (e.getHandlerPC() <= getPC() && e.getHandlerPC() + 30 >= getPC()) {
                            debuggingContext = true;
                        }
                    }

                    for (int i = 1; !debuggingContext && i < stack.getStackDepth(); i++) {
                        OpcodeStack.Item e = stack.getStackItem(i);

                        if (e.getSignature().indexOf("Logger") >= 0 || e.getSignature().indexOf("Exception") >= 0) {
                            debuggingContext = true;
                        }

                        XField f = e.getXField();
                        if (f != null && (SYSTEM_ERR.equals(f.getFieldDescriptor()) || SYSTEM_OUT.equals(f.getFieldDescriptor()))) {
                            debuggingContext = true;
                        }
                    }
                }
                //                String name = null;
                int reg = item.getRegisterNumber();
                Collection<BugAnnotation> as = new ArrayList<BugAnnotation>();
                XField field = item.getXField();
                FieldAnnotation fieldAnnotation = null;
                if (field != null) {
                    fieldAnnotation = FieldAnnotation.fromXField(field);
                    fieldAnnotation.setDescription(FieldAnnotation.LOADED_FROM_ROLE);
                }

                if (reg != -1) {
                    LocalVariableAnnotation lva = LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), reg, getPC(),
                            getPC() - 1);
                    if (lva.isNamed()) {
                        as.add(lva);
                        if (fieldAnnotation != null) {
                            as.add(fieldAnnotation);
                        }
                    } else {
                        if (fieldAnnotation != null) {
                            as.add(fieldAnnotation);
                        }
                        as.add(lva);
                    }
                } else if (fieldAnnotation != null) {
                    as.add(fieldAnnotation);
                } else {
                    XMethod m = item.getReturnValueOf();
                    if (m != null) {
                        MethodAnnotation methodAnnotation = MethodAnnotation.fromXMethod(m);
                        methodAnnotation.setDescription(MethodAnnotation.METHOD_RETURN_VALUE_OF);
                        as.add(methodAnnotation);
                    }
                }
                int priority = debuggingContext ? NORMAL_PRIORITY : HIGH_PRIORITY;
                if (!as.isEmpty()) {
                    bugAccumulator.accumulateBug(new BugInstance(this, "DMI_INVOKING_TOSTRING_ON_ARRAY", priority)
                    .addClassAndMethod(this).addAnnotations(as), this);
                } else {
                    bugAccumulator.accumulateBug(
                            new BugInstance(this, "DMI_INVOKING_TOSTRING_ON_ANONYMOUS_ARRAY", priority).addClassAndMethod(this),
                            this);
                }
            }
        }

        if (isTigerOrHigher) {
            if (previousMethodInvocation != null && prevOpCode == INVOKEVIRTUAL && seen == INVOKESTATIC) {
                String classNameForPreviousMethod = previousMethodInvocation.getClassName();
                String classNameForThisMethod = getClassConstantOperand();
                if (classNameForPreviousMethod.startsWith("java.lang.")
                        && classNameForPreviousMethod.equals(classNameForThisMethod.replace('/', '.'))
                        && previousMethodInvocation.getName().endsWith("Value")
                        && previousMethodInvocation.getSignature().length() == 3
                        && "valueOf".equals(getNameConstantOperand())
                        && getSigConstantOperand().charAt(1) == previousMethodInvocation.getSignature().charAt(2)) {
                    bugAccumulator.accumulateBug(
                            new BugInstance(this, "BX_UNBOXING_IMMEDIATELY_REBOXED", NORMAL_PRIORITY).addClassAndMethod(this)
                            .addCalledMethod(this),
                            this);
                }

            }

            if (previousMethodInvocation != null && prevOpCode == INVOKESPECIAL && seen == INVOKEVIRTUAL) {
                String classNameForPreviousMethod = previousMethodInvocation.getClassName();
                String classNameForThisMethod = getClassConstantOperand();
                if (classNameForPreviousMethod.startsWith("java.lang.")
                        && classNameForPreviousMethod.equals(classNameForThisMethod.replace('/', '.'))
                        && getNameConstantOperand().endsWith("Value") && getSigConstantOperand().length() == 3) {
                    if (getSigConstantOperand().charAt(2) == previousMethodInvocation.getSignature().charAt(1)) {
                        bugAccumulator.accumulateBug(
                                new BugInstance(this, "BX_BOXING_IMMEDIATELY_UNBOXED", NORMAL_PRIORITY).addClassAndMethod(this),
                                this);
                    } else {
                        bugAccumulator.accumulateBug(new BugInstance(this, "BX_BOXING_IMMEDIATELY_UNBOXED_TO_PERFORM_COERCION",
                                NORMAL_PRIORITY).addClassAndMethod(this), this);
                    }

                    ternaryConversionState = 1;
                } else {
                    ternaryConversionState = 0;
                }

            } else if (seen == INVOKEVIRTUAL) {
                if (getClassConstantOperand().startsWith("java/lang") && getNameConstantOperand().endsWith("Value")
                        && getSigConstantOperand().length() == 3) {
                    ternaryConversionState = 1;
                } else {
                    ternaryConversionState = 0;
                }
            } else if (ternaryConversionState == 1) {
                if (I2L < seen && seen <= I2S) {
                    ternaryConversionState = 2;
                } else {
                    ternaryConversionState = 0;
                }
            } else if (ternaryConversionState == 2) {
                ternaryConversionState = 0;
                if (seen == GOTO) {
                    bugReporter.reportBug(new BugInstance(this, "BX_UNBOXED_AND_COERCED_FOR_TERNARY_OPERATOR", NORMAL_PRIORITY)
                    .addClassAndMethod(this).addSourceLine(this));
                }
            } else {
                ternaryConversionState = 0;
            }
        }

        AssertInvokedFromRun: if (seen == INVOKESTATIC) {
            if ((getNameConstantOperand().startsWith("assert") || getNameConstantOperand().startsWith("fail"))
                    && "run".equals(getMethodName()) && implementsRunnable(getThisClass())) {
                int size1 = Util.getSizeOfSurroundingTryBlock(getConstantPool(), getMethod().getCode(), "java/lang/Throwable",
                        getPC());
                int size2 = Util.getSizeOfSurroundingTryBlock(getConstantPool(), getMethod().getCode(), "java/lang/Error",
                        getPC());
                int size3 = Util.getSizeOfSurroundingTryBlock(getConstantPool(), getMethod().getCode(),
                        "java/lang/AssertionFailureError", getPC());
                int size = Math.min(Math.min(size1, size2), size3);
                if (size == Integer.MAX_VALUE) {
                    String dottedClassName = getClassConstantOperand().replace('/', '.');
                    if (!dottedClassName.startsWith("junit")) {
                        try {
                            JavaClass targetClass = AnalysisContext.currentAnalysisContext().lookupClass(dottedClassName);
                            if (!targetClass.getSuperclassName().startsWith("junit")) {
                                break AssertInvokedFromRun;
                            }
                        } catch (ClassNotFoundException e) {
                            AnalysisContext.reportMissingClass(e);
                            break AssertInvokedFromRun;
                        }
                    }

                    bugAccumulator.accumulateBug(new BugInstance(this, "IJU_ASSERT_METHOD_INVOKED_FROM_RUN_METHOD",
                            extendsThread(getThisClass()) ? NORMAL_PRIORITY : LOW_PRIORITY).addClassAndMethod(this), this);

                }

            }
        }
        if (seen == INVOKESPECIAL && getClassConstantOperand().startsWith("java/lang/")
                && "<init>".equals(getNameConstantOperand()) && getSigConstantOperand().length() == 4) {
            previousMethodInvocation = XFactory.createReferencedXMethod(this);
        } else if (seen == INVOKESTATIC && getClassConstantOperand().startsWith("java/lang/")
                && "valueOf".equals(getNameConstantOperand()) && getSigConstantOperand().length() == 4) {
            previousMethodInvocation = XFactory.createReferencedXMethod(this);
        } else if (seen == INVOKEVIRTUAL && getClassConstantOperand().startsWith("java/lang/")
                && getNameConstantOperand().endsWith("Value")
                && getSigConstantOperand().length() == 3) {
            previousMethodInvocation = XFactory.createReferencedXMethod(this);
        } else {
            previousMethodInvocation = null;
        }

        if (testingEnabled && seen == IAND || seen == LAND) {
            OpcodeStack.Item rhs = stack.getStackItem(0);
            OpcodeStack.Item lhs = stack.getStackItem(1);
            Object constant = rhs.getConstant();
            OpcodeStack.Item value = lhs;
            if (constant == null) {
                constant = lhs.getConstant();
                value = rhs;
            }
            if (constant instanceof Number && (seen == LAND || value.getSpecialKind() == OpcodeStack.Item.RESULT_OF_L2I)) {
                long constantValue = ((Number) constant).longValue();
                if ((constantValue == 0xEFFFFFFFL || constantValue == 0xEFFFFFFFFFFFFFFFL || seen == IAND
                        && constantValue == 0xEFFFFFFF)) {
                    bugAccumulator.accumulateBug(new BugInstance(this, "TESTING", seen == LAND ? HIGH_PRIORITY : NORMAL_PRIORITY)
                    .addClassAndMethod(this).addString("Possible failed attempt to mask lower 31 bits of an int")
                    .addValueSource(value, this), this);
                }

            }
        }

        if (seen == INEG) {
            OpcodeStack.Item top = stack.getStackItem(0);
            XMethod m = top.getReturnValueOf();
            if (m != null) {
                if ("compareTo".equals(m.getName()) || "compare".equals(m.getName())) {
                    bugAccumulator.accumulateBug(new BugInstance(this, "RV_NEGATING_RESULT_OF_COMPARETO", NORMAL_PRIORITY)
                    .addClassAndMethod(this)
                    .addCalledMethod(m).addValueSource(top, this), this);
                }
            }

        }
        prevOpCode = seen;

    }

    boolean implementsRunnable(JavaClass obj) {
        return Subtypes2.instanceOf(obj, "java.lang.Runnable");
    }

    boolean extendsThread(JavaClass obj) {
        return Subtypes2.instanceOf(obj, "java.lang.Thread");
    }

}
