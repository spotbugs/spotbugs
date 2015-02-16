/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

import java.math.BigDecimal;
import java.util.Iterator;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantValue;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Synthetic;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.IntAnnotation;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.Util;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class DumbMethods extends OpcodeStackDetector {

    private abstract class SubDetector {
        public void initMethod(Method method) {}

        abstract public void sawOpcode(int seen);
    }

    private class InvalidMinMaxSubDetector extends SubDetector {
        Number lowerBound, upperBound;

        @Override
        public void initMethod(Method method) {
            lowerBound = upperBound = null;
        }

        @Override
        public void sawOpcode(int seen) {
            if(seen == INVOKESTATIC && getClassConstantOperand().equals("java/lang/Math") && (getMethodDescriptorOperand().getName().equals("max")
                    || getMethodDescriptorOperand().getName().equals("min"))) {
                Object const1 = stack.getStackItem(0).getConstant();
                Object const2 = stack.getStackItem(1).getConstant();
                Number n = null;
                if(const1 != null ^ const2 != null) {
                    n = (const1 instanceof Number) ? (Number)const1 : (Number)const2;
                    if(getMethodDescriptorOperand().getName().equals("min")) {
                        upperBound = n;
                    } else {
                        lowerBound = n;
                    }
                } else {
                    upperBound = lowerBound = null;
                }
                XMethod rvo1 = stack.getStackItem(0).getReturnValueOf();
                XMethod rvo2 = stack.getStackItem(1).getReturnValueOf();
                if(rvo1 != null ^ rvo2 != null) {
                    XMethod rvo = rvo1 == null ? rvo2 : rvo1;
                    if (lowerBound instanceof Comparable && upperBound != null && upperBound.getClass() == lowerBound.getClass()
                            && rvo.getClassDescriptor().getClassName().equals("java/lang/Math")
                            && (rvo.getName().equals("max") || rvo.getName().equals("min"))) {
                        @SuppressWarnings("unchecked")
                        int result = ((Comparable<Number>)lowerBound).compareTo(upperBound);
                        if(result > 0) {
                            accumulator.accumulateBug(
                                    new BugInstance("DM_INVALID_MIN_MAX", HIGH_PRIORITY).addClassAndMethod(DumbMethods.this)
                                    .addString(String.valueOf(n)),
                                    DumbMethods.this);
                        }
                    }
                }
            }
        }
    }

    private class NullMethodsSubDetector extends SubDetector {

        @Override
        public void sawOpcode(int seen) {
            if (seen == INVOKESTATIC && ("com/google/common/base/Preconditions".equals(getClassConstantOperand())
                    && "checkNotNull".equals(getNameConstantOperand())
                    || "com/google/common/base/Strings".equals(getClassConstantOperand())
                    && ("nullToEmpty".equals(getNameConstantOperand()) ||
                            "emptyToNull".equals(getNameConstantOperand()) ||
                            "isNullOrEmpty".equals(getNameConstantOperand())))
                    ) {
                int args = PreorderVisitor.getNumberArguments(getSigConstantOperand());

                OpcodeStack.Item item = stack.getStackItem(args - 1);
                Object o = item.getConstant();
                if (o instanceof String) {

                    OpcodeStack.Item secondArgument = null;
                    String bugPattern = "DMI_DOH";
                    if (args > 1) {
                        secondArgument = stack.getStackItem(args - 2);
                        Object secondConstant = secondArgument.getConstant();
                        if (!(secondConstant instanceof String)) {
                            bugPattern = "DMI_ARGUMENTS_WRONG_ORDER";
                        }
                    }

                    BugInstance bug = new BugInstance(DumbMethods.this, bugPattern, NORMAL_PRIORITY).addClassAndMethod(DumbMethods.this)
                            .addCalledMethod(DumbMethods.this)
                            .addString("Passing String constant as value that should be null checked").describe(StringAnnotation.STRING_MESSAGE)
                            .addString((String) o).describe(StringAnnotation.STRING_CONSTANT_ROLE);
                    if (secondArgument != null) {
                        bug.addValueSource(secondArgument, DumbMethods.this);
                    }

                    accumulator.accumulateBug(bug, DumbMethods.this);
                }
            }

            if (seen == INVOKESTATIC && ("junit/framework/Assert".equals(getClassConstantOperand()) || "org/junit/Assert".equals(getClassConstantOperand()))
                    && "assertNotNull".equals(getNameConstantOperand())) {
                int args = PreorderVisitor.getNumberArguments(getSigConstantOperand());

                OpcodeStack.Item item = stack.getStackItem(0);
                Object o = item.getConstant();
                if (o instanceof String) {

                    OpcodeStack.Item secondArgument = null;
                    String bugPattern = "DMI_DOH";
                    if (args == 2) {
                        secondArgument = stack.getStackItem(1);
                        Object secondConstant = secondArgument.getConstant();
                        if (!(secondConstant instanceof String)) {
                            bugPattern = "DMI_ARGUMENTS_WRONG_ORDER";
                        }
                    }

                    BugInstance bug = new BugInstance(DumbMethods.this, bugPattern, NORMAL_PRIORITY).addClassAndMethod(DumbMethods.this)
                            .addCalledMethod(DumbMethods.this).addString("Passing String constant as value that should be null checked").describe(StringAnnotation.STRING_MESSAGE)
                            .addString((String) o).describe(StringAnnotation.STRING_CONSTANT_ROLE);
                    if (secondArgument != null) {
                        bug.addValueSource(secondArgument, DumbMethods.this);
                    }

                    accumulator.accumulateBug(bug, DumbMethods.this);
                }
            }
        }
    }

    private class FutilePoolSizeSubDetector extends SubDetector {
        @Override
        public void sawOpcode(int seen) {
            if (seen == INVOKEVIRTUAL && "java/util/concurrent/ScheduledThreadPoolExecutor".equals(getClassConstantOperand())
                    && "setMaximumPoolSize".equals(getNameConstantOperand())) {
                accumulator.accumulateBug(new BugInstance(DumbMethods.this,
                        "DMI_FUTILE_ATTEMPT_TO_CHANGE_MAXPOOL_SIZE_OF_SCHEDULED_THREAD_POOL_EXECUTOR", HIGH_PRIORITY)
                .addClassAndMethod(DumbMethods.this), DumbMethods.this);
            }
        }
    }

    static int saturatingIncrement(int value) {
        if (value == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return value+1;
    }

    private class RangeCheckSubDetector extends SubDetector {



        private void checkRange(Item item, Object minValue, Object maxValue, String pattern) {
            if(!(item.getConstant() instanceof Number)) {
                return;
            }
            int value = ((Number)item.getConstant()).intValue();
            int intMin = Integer.MIN_VALUE;
            int intMax = Integer.MAX_VALUE;
            if(minValue instanceof Number) {
                intMin = ((Number)minValue).intValue();
            }
            if(maxValue instanceof Number) {
                intMax = ((Number)maxValue).intValue();
            } else if(maxValue instanceof String) {
                intMax = ((String)maxValue).length()-1;
            } else if (maxValue instanceof OpcodeStack.Item){
                OpcodeStack.Item maxItem = (OpcodeStack.Item ) maxValue;
                if (maxItem.getSignature().charAt(0) == '[' && maxItem.getConstant() instanceof Integer) {
                    intMax = ((Integer)maxItem.getConstant())-1;

                }
            }

            if(value < intMin || value > intMax) {
                BugInstance bug = new BugInstance(pattern, NORMAL_PRIORITY ).addClassAndMethod(DumbMethods.this).addSourceLine(DumbMethods.this)
                        .addInt(value).describe(IntAnnotation.INT_VALUE);

                if (intMin <= intMax) {
                    if (value < intMin) {
                        bug.addInt(intMin).describe(IntAnnotation.INT_MIN_VALUE);
                    }
                    if (value > intMax) {
                        bug.addInt(intMax) .describe(IntAnnotation.INT_MAX_VALUE);
                    }
                }


                if (isMethodCall()) {
                    bug.addCalledMethod(DumbMethods.this);
                }



                accumulator.accumulateBug(bug, DumbMethods.this);
            }
        }


        @Override
        public void sawOpcode(int seen) {
            // System.out.printf("%4d %s%n", getPC(), OPCODE_NAMES[seen]);
            switch(seen) {
            case IALOAD:
            case AALOAD:
            case SALOAD:
            case CALOAD:
            case BALOAD:
            case LALOAD:
            case DALOAD:
            case FALOAD: {
                checkRange(stack.getStackItem(0), 0, stack.getStackItem(1), "RANGE_ARRAY_INDEX");
                break;
            }
            case IASTORE:
            case AASTORE:
            case SASTORE:
            case CASTORE:
            case BASTORE:
            case LASTORE:
            case DASTORE:
            case FASTORE: {

                checkRange(stack.getStackItem(1), 0,  stack.getStackItem(2), "RANGE_ARRAY_INDEX");
                break;
            }
            case INVOKESTATIC: {
                MethodDescriptor m = getMethodDescriptorOperand();
                if(m.getSlashedClassName().equals("java/lang/System") && m.getName().equals("arraycopy")) {
                    // void arraycopy(Object src, int srcPos, Object dest, int destPos, int length)
                    Item length = stack.getStackItem(0);
                    Object constantLength = length.getConstant();
                    //                    if (constantLength instanceof Number && constantLength.equals(0)) {
                    //                        break;
                    //                    }
                    Item srcPos = stack.getStackItem(3);
                    Item src = stack.getStackItem(4);
                    checkRange(srcPos, 0, src, "RANGE_ARRAY_OFFSET");
                    Item dest =  stack.getStackItem(2);
                    Item destPos = stack.getStackItem(1);
                    checkRange(destPos, 0, dest, "RANGE_ARRAY_OFFSET");

                    if(constantLength instanceof Number) {
                        int length1 = Integer.MAX_VALUE;
                        if(src.getConstant() instanceof Integer) {
                            length1 = (int) src.getConstant();
                        }
                        if(srcPos.getConstant() instanceof Integer) {
                            length1 -= (int) srcPos.getConstant();
                        }
                        int length2 = Integer.MAX_VALUE;
                        if(dest.getConstant() instanceof Integer) {
                            length2 = (int) stack.getStackItem(2).getConstant();
                        }
                        if(destPos.getConstant() instanceof Integer) {
                            length2 -= (int) stack.getStackItem(1).getConstant();
                        }
                        checkRange(length, 0, Math.min(length1, length2), "RANGE_ARRAY_LENGTH");
                    }
                }
                break;
            }
            case INVOKEVIRTUAL:
            case INVOKESPECIAL: {
                MethodDescriptor m = getMethodDescriptorOperand();
                if(m.getSlashedClassName().equals("java/lang/String")) {
                    if((m.getName().equals("charAt") || m.getName().equals("codePointAt")) && m.getSignature().startsWith("(I)")) {
                        checkRange(stack.getStackItem(0), 0, stack.getStackItem(1).getConstant(), "RANGE_STRING_INDEX");
                    }
                    if(m.getName().equals("substring") || m.getName().equals("subSequence")) {
                        int nArgs = getNumberArguments(m.getSignature());
                        Item thisArg = stack.getStackItem(nArgs);
                        Item firstArg = stack.getStackItem(nArgs-1);
                        Object thisConstantValue = thisArg.getConstant();
                        int maxLength = thisConstantValue instanceof String ? ((String)thisConstantValue).length() : Integer.MAX_VALUE;
                        checkRange(firstArg, 0,maxLength, "RANGE_STRING_INDEX");
                        if(nArgs == 2) {
                            Item secondArg = stack.getStackItem(0);
                            checkRange(secondArg, firstArg.getConstant() == null ? 0 : firstArg.getConstant(),
                                    maxLength,
                                    "RANGE_STRING_INDEX");
                        }
                    }
                }
                if ((m.getSignature().startsWith("([BII)") || m.getSignature().startsWith("([CII)") || m.getSignature().startsWith("([III)"))
                        && (((m.getName().equals("write") || m.getName().equals("read")) && m.getSlashedClassName().startsWith(
                                "java/io/")) || (m.getName().equals("<init>") && m.getSlashedClassName().equals("java/lang/String")))) {
                    Item arrayArg = stack.getStackItem(2);
                    Item offsetArg = stack.getStackItem(1);
                    Item lengthArg = stack.getStackItem(0);
                    int length = Integer.MAX_VALUE;
                    if(arrayArg.getConstant() instanceof Integer) {
                        length = (int) arrayArg.getConstant();
                    }
                    if(offsetArg.getConstant() instanceof Integer) {
                        checkRange(offsetArg, 0, saturatingIncrement(length), "RANGE_ARRAY_OFFSET");
                        length -= (int) offsetArg.getConstant();
                    }
                    checkRange(lengthArg, 0, saturatingIncrement(length), "RANGE_ARRAY_LENGTH");
                }
                break;
            }
            default:
                break;
            }
        }
    }

    private class UrlCollectionSubDetector extends SubDetector {
        @Override
        public void sawOpcode(int seen) {
            if ((seen == INVOKEVIRTUAL && "java/util/HashMap".equals(getClassConstantOperand()) && "get".equals(getNameConstantOperand()))
                    || (seen == INVOKEINTERFACE && "java/util/Map".equals(getClassConstantOperand()) && "get".equals(getNameConstantOperand()))
                    || (seen == INVOKEVIRTUAL && "java/util/HashSet".equals(getClassConstantOperand()) && "contains".equals(getNameConstantOperand()))
                    || (seen == INVOKEINTERFACE && "java/util/Set".equals(getClassConstantOperand()) && "contains".equals(getNameConstantOperand()))) {
                OpcodeStack.Item top = stack.getStackItem(0);
                if ("Ljava/net/URL;".equals(top.getSignature())) {
                    accumulator.accumulateBug(new BugInstance(DumbMethods.this, "DMI_COLLECTION_OF_URLS", HIGH_PRIORITY)
                    .addClassAndMethod(DumbMethods.this), DumbMethods.this);
                }
            }
        }
    }

    private class VacuousComparisonSubDetector extends SubDetector {
        @Override
        public void sawOpcode(int seen) {
            boolean foundVacuousComparison = false;
            if (seen == IF_ICMPGT || seen == IF_ICMPLE) {
                OpcodeStack.Item rhs = stack.getStackItem(0);
                Object rhsConstant = rhs.getConstant();
                if (rhsConstant instanceof Integer && ((Integer) rhsConstant).intValue() == Integer.MAX_VALUE) {
                    foundVacuousComparison = true;
                }
                OpcodeStack.Item lhs = stack.getStackItem(1);
                Object lhsConstant = lhs.getConstant();
                if (lhsConstant instanceof Integer && ((Integer) lhsConstant).intValue() == Integer.MIN_VALUE) {
                    foundVacuousComparison = true;
                }

            }
            if (seen == IF_ICMPLT || seen == IF_ICMPGE) {
                OpcodeStack.Item rhs = stack.getStackItem(0);
                Object rhsConstant = rhs.getConstant();
                if (rhsConstant instanceof Integer && ((Integer) rhsConstant).intValue() == Integer.MIN_VALUE) {
                    foundVacuousComparison = true;
                }
                OpcodeStack.Item lhs = stack.getStackItem(1);
                Object lhsConstant = lhs.getConstant();
                if (lhsConstant instanceof Integer && ((Integer) lhsConstant).intValue() == Integer.MAX_VALUE) {
                    foundVacuousComparison = true;
                }

            }
            if (foundVacuousComparison) {
                accumulator.accumulateBug(new BugInstance(DumbMethods.this, "INT_VACUOUS_COMPARISON", getBranchOffset() < 0 ? HIGH_PRIORITY
                        : NORMAL_PRIORITY).addClassAndMethod(DumbMethods.this), DumbMethods.this);
            }
        }
    }

    private class BadCastInEqualsSubDetector extends SubDetector {
        private boolean isEqualsObject;

        private boolean sawInstanceofCheck;

        private boolean reportedBadCastInEquals;

        @Override
        public void initMethod(Method method) {
            isEqualsObject = "equals".equals(getMethodName()) && "(Ljava/lang/Object;)Z".equals(getMethodSig()) && !method.isStatic();
            sawInstanceofCheck = false;
            reportedBadCastInEquals = false;
        }

        @Override
        public void sawOpcode(int seen) {
            if (isEqualsObject && !reportedBadCastInEquals) {
                if (seen == INVOKEVIRTUAL && "isInstance".equals(getNameConstantOperand())
                        && "java/lang/Class".equals(getClassConstantOperand())) {
                    OpcodeStack.Item item = stack.getStackItem(0);
                    if (item.getRegisterNumber() == 1) {
                        sawInstanceofCheck = true;
                    }
                } else if (seen == INSTANCEOF || seen == INVOKEVIRTUAL && "getClass".equals(getNameConstantOperand())
                        && "()Ljava/lang/Class;".equals(getSigConstantOperand())) {
                    OpcodeStack.Item item = stack.getStackItem(0);
                    if (item.getRegisterNumber() == 1) {
                        sawInstanceofCheck = true;
                    }
                } else if (seen == INVOKESPECIAL && "equals".equals(getNameConstantOperand())
                        && "(Ljava/lang/Object;)Z".equals(getSigConstantOperand())) {
                    OpcodeStack.Item item0 = stack.getStackItem(0);
                    OpcodeStack.Item item1 = stack.getStackItem(1);
                    if (item1.getRegisterNumber() + item0.getRegisterNumber() == 1) {
                        sawInstanceofCheck = true;
                    }
                } else if (seen == CHECKCAST && !sawInstanceofCheck) {
                    OpcodeStack.Item item = stack.getStackItem(0);
                    if (item.getRegisterNumber() == 1) {
                        if (getSizeOfSurroundingTryBlock(getPC()) == Integer.MAX_VALUE) {
                            accumulator.accumulateBug(new BugInstance(DumbMethods.this, "BC_EQUALS_METHOD_SHOULD_WORK_FOR_ALL_OBJECTS",
                                    NORMAL_PRIORITY).addClassAndMethod(DumbMethods.this), DumbMethods.this);
                        }

                        reportedBadCastInEquals = true;
                    }
                }
            }
        }
    }

    private class RandomOnceSubDetector extends SubDetector {
        private boolean freshRandomOnTos = false;

        private boolean freshRandomOneBelowTos = false;

        @Override
        public void initMethod(Method method) {
            freshRandomOnTos = false;
        }

        @Override
        public void sawOpcode(int seen) {
            if (seen == INVOKEVIRTUAL && "java/util/Random".equals(getClassConstantOperand())
                    && (freshRandomOnTos || freshRandomOneBelowTos)) {
                accumulator.accumulateBug(new BugInstance(DumbMethods.this, "DMI_RANDOM_USED_ONLY_ONCE", HIGH_PRIORITY)
                .addClassAndMethod(DumbMethods.this).addCalledMethod(DumbMethods.this), DumbMethods.this);

            }
            freshRandomOneBelowTos = freshRandomOnTos && isRegisterLoad();
            freshRandomOnTos = seen == INVOKESPECIAL && "java/util/Random".equals(getClassConstantOperand())
                    && "<init>".equals(getNameConstantOperand());
        }
    }

    private final SubDetector[] subDetectors = new SubDetector[] { new VacuousComparisonSubDetector(),
            new RangeCheckSubDetector(), new BadCastInEqualsSubDetector(), new FutilePoolSizeSubDetector(),
            new UrlCollectionSubDetector(), new RandomOnceSubDetector(), new NullMethodsSubDetector(),
            new InvalidMinMaxSubDetector()};

    private static final ObjectType CONDITION_TYPE = ObjectTypeFactory.getInstance("java.util.concurrent.locks.Condition");

    private final BugReporter bugReporter;

    private boolean sawCurrentTimeMillis;

    private BugInstance gcInvocationBugReport;

    private int gcInvocationPC;

    private CodeException[] exceptionTable;

    /*
     * private boolean sawLDCEmptyString;
     */
    private String primitiveObjCtorSeen;

    private boolean ctorSeen;

    private boolean prevOpcodeWasReadLine;

    private int prevOpcode;

    private boolean isPublicStaticVoidMain;

    private int sawCheckForNonNegativeSignedByte;

    private int sinceBufferedInputStreamReady;

    private int randomNextIntState;

    private boolean checkForBitIorofSignedByte;

    /**
     * A heuristic - how long a catch block for OutOfMemoryError might be.
     */
    private static final int OOM_CATCH_LEN = 20;

    private final boolean testingEnabled;

    private final BugAccumulator accumulator;
    private final BugAccumulator absoluteValueAccumulator;

    private static final int MICROS_PER_DAY_OVERFLOWED_AS_INT
    = 24 * 60 * 60 * 1000 * 1000;

    public DumbMethods(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        accumulator = new BugAccumulator(bugReporter);
        absoluteValueAccumulator = new BugAccumulator(bugReporter);
        testingEnabled = SystemProperties.getBoolean("report_TESTING_pattern_in_standard_detectors");
    }

    boolean isSynthetic;

    @Override
    public void visit(JavaClass obj) {
        String superclassName = obj.getSuperclassName();
        isSynthetic = "java.rmi.server.RemoteStub".equals(superclassName);
        Attribute[] attributes = obj.getAttributes();
        if (attributes != null) {
            for (Attribute a : attributes) {
                if (a instanceof Synthetic) {
                    isSynthetic = true;
                }
            }
        }

    }

    @Override
    public void visitAfter(JavaClass obj) {
        accumulator.reportAccumulatedBugs();
    }

    public static boolean isTestMethod(Method method) {
        return method.getName().startsWith("test");
    }

    @Override
    public void visit(Field field) {
        ConstantValue value = field.getConstantValue();
        if (value == null) {
            return;
        }
        Constant c = getConstantPool().getConstant(value.getConstantValueIndex());

        if (testingEnabled && c instanceof ConstantLong && ((ConstantLong)c).getBytes() == MICROS_PER_DAY_OVERFLOWED_AS_INT) {
            bugReporter.reportBug( new BugInstance(this, "TESTING", HIGH_PRIORITY).addClass(this).addField(this)
                    .addString("Did you mean MICROS_PER_DAY")
                    .addInt(MICROS_PER_DAY_OVERFLOWED_AS_INT)
                    .describe(IntAnnotation.INT_VALUE));

        }
    }
    @Override
    public void visit(Method method) {
        String cName = getDottedClassName();

        for(SubDetector subDetector : subDetectors) {
            subDetector.initMethod(method);
        }

        // System.out.println(getFullyQualifiedMethodName());
        isPublicStaticVoidMain = method.isPublic() && method.isStatic() && "main".equals(getMethodName())
                || cName.toLowerCase().indexOf("benchmark") >= 0;
                prevOpcodeWasReadLine = false;
                Code code = method.getCode();
                if (code != null) {
                    this.exceptionTable = code.getExceptionTable();
                }
                if (this.exceptionTable == null) {
                    this.exceptionTable = new CodeException[0];
                }
                primitiveObjCtorSeen = null;
                ctorSeen = false;
                randomNextIntState = 0;
                checkForBitIorofSignedByte = false;
                sinceBufferedInputStreamReady = 100000;
                sawCheckForNonNegativeSignedByte = -1000;
                sawLoadOfMinValue = false;
                previousMethodCall = null;

    }

    int opcodesSincePendingAbsoluteValueBug;

    BugInstance pendingAbsoluteValueBug;

    SourceLineAnnotation pendingAbsoluteValueBugSourceLine;

    boolean sawLoadOfMinValue = false;

    MethodDescriptor previousMethodCall = null;

    @Override
    public void sawOpcode(int seen) {

        if (isMethodCall()) {
            MethodDescriptor called = getMethodDescriptorOperand();

            if (previousMethodCall != null && !stack.isJumpTarget(getPC())) {
                if ("toString".equals(called.getName())
                        && "java/lang/Integer".equals(called.getClassDescriptor().getClassName())
                        && "valueOf".equals(previousMethodCall.getName())
                        && "(I)Ljava/lang/Integer;".equals(previousMethodCall.getSignature())
                        ) {
                    MethodAnnotation preferred = new MethodAnnotation("java.lang.Integer", "toString", "(I)Ljava/lang/String;", true);
                    BugInstance bug = new BugInstance(this, "DM_BOXED_PRIMITIVE_TOSTRING", HIGH_PRIORITY).addClassAndMethod(this)
                            .addCalledMethod(this).addMethod(preferred).describe(MethodAnnotation.SHOULD_CALL);
                    accumulator.accumulateBug(bug, this);

                }  else if ("intValue".equals(called.getName())
                        && "java/lang/Integer".equals(called.getClassDescriptor().getClassName())
                        && "java/lang/Integer".equals(previousMethodCall.getSlashedClassName())
                        && ("<init>".equals(previousMethodCall.getName())
                                && "(Ljava/lang/String;)V".equals(previousMethodCall.getSignature())
                                || "valueOf".equals(previousMethodCall.getName())
                                && "(Ljava/lang/String;)Ljava/lang/Integer;".equals(previousMethodCall.getSignature())
                                )) {

                    MethodAnnotation preferred = new MethodAnnotation("java.lang.Integer", "parseInt", "(Ljava/lang/String;)I", true);

                    BugInstance bug = new BugInstance(this, "DM_BOXED_PRIMITIVE_FOR_PARSING", HIGH_PRIORITY).addClassAndMethod(this)
                            .addCalledMethod(this).addMethod(preferred).describe(MethodAnnotation.SHOULD_CALL);
                    accumulator.accumulateBug(bug, this);
                }  else if ("longValue".equals(called.getName())
                        && "java/lang/Long".equals(called.getClassDescriptor().getClassName())
                        && "java/lang/Long".equals(previousMethodCall.getSlashedClassName())
                        && ( "<init>".equals(previousMethodCall.getName())
                                && "(Ljava/lang/String;)V".equals(previousMethodCall.getSignature())
                                ||  "valueOf".equals(previousMethodCall.getName())
                                && "(Ljava/lang/String;)Ljava/lang/Long;".equals(previousMethodCall.getSignature()))
                        ) {
                    MethodAnnotation preferred = new MethodAnnotation("java.lang.Long", "parseLong", "(Ljava/lang/String;)J", true);

                    BugInstance bug = new BugInstance(this, "DM_BOXED_PRIMITIVE_FOR_PARSING", HIGH_PRIORITY).addClassAndMethod(this)
                            .addCalledMethod(this).addMethod(preferred).describe(MethodAnnotation.SHOULD_CALL);
                    accumulator.accumulateBug(bug, this);
                } else if("compareTo".equals(called.getName())
                        && "valueOf".equals(previousMethodCall.getName())
                        && called.getClassDescriptor().equals(previousMethodCall.getClassDescriptor()) && !previousMethodCall.getSignature().startsWith("(Ljava/lang/String;")
                        ) {
                    String primitiveType = ClassName.getPrimitiveType(called.getClassDescriptor().getClassName());
                    XMethod rvo = stack.getStackItem(1).getReturnValueOf();
                    XField field = stack.getStackItem(1).getXField();
                    String signature;
                    if (rvo != null) {
                        signature = new SignatureParser(rvo.getSignature()).getReturnTypeSignature();
                    } else if (field != null) {
                        signature = field.getSignature();
                    } else {
                        signature = "";
                    }
                    if (primitiveType != null
                            && (previousMethodCall.equals(rvo) || signature.equals(primitiveType))
                            && (getThisClass().getMajor() >= MAJOR_1_7 || getThisClass().getMajor() >= MAJOR_1_4
                            && (primitiveType.equals("D") || primitiveType.equals("F")))) {
                        MethodDescriptor shouldCall = new MethodDescriptor(called.getClassDescriptor().getClassName(), "compare",
                                "(" + primitiveType + primitiveType + ")I", true);
                        BugInstance bug = new BugInstance(this, "DM_BOXED_PRIMITIVE_FOR_COMPARE",
                                primitiveType.equals("Z") ? LOW_PRIORITY : primitiveType.equals("B") ? NORMAL_PRIORITY
                                        : HIGH_PRIORITY).addClassAndMethod(this).addCalledMethod(this).addMethod(shouldCall)
                                        .describe(MethodAnnotation.SHOULD_CALL);
                        accumulator.accumulateBug(bug, this);
                    }
                }
            }
            previousMethodCall = called;
        } else {
            previousMethodCall = null;
        }


        if (seen == LDC || seen == LDC_W || seen == LDC2_W) {
            Constant c = getConstantRefOperand();
            if (testingEnabled && (c instanceof ConstantInteger && ((ConstantInteger) c).getBytes() == MICROS_PER_DAY_OVERFLOWED_AS_INT
                    || c instanceof ConstantLong && ((ConstantLong) c).getBytes() == MICROS_PER_DAY_OVERFLOWED_AS_INT)) {
                BugInstance bug = new BugInstance(this, "TESTING", HIGH_PRIORITY).addClassAndMethod(this)
                        .addString("Did you mean MICROS_PER_DAY").addInt(MICROS_PER_DAY_OVERFLOWED_AS_INT)
                        .describe(IntAnnotation.INT_VALUE);
                accumulator.accumulateBug(bug, this);
            }
            if ((c instanceof ConstantInteger && ((ConstantInteger) c).getBytes() == Integer.MIN_VALUE
                    || c instanceof ConstantLong && ((ConstantLong) c).getBytes() == Long.MIN_VALUE)) {
                sawLoadOfMinValue = true;
                pendingAbsoluteValueBug = null;
                pendingAbsoluteValueBugSourceLine = null;
                absoluteValueAccumulator.clearBugs();
            }
        }


        if (seen == LCMP) {
            OpcodeStack.Item left = stack.getStackItem(1);
            OpcodeStack.Item right = stack.getStackItem(0);
            checkForCompatibleLongComparison(left, right);
            checkForCompatibleLongComparison(right, left);
        }

        if (stack.getStackDepth() >= 2) {
            switch (seen) {
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLE:
            case IF_ICMPGE:
            case IF_ICMPLT:
            case IF_ICMPGT:
                OpcodeStack.Item item0 = stack.getStackItem(0);
                OpcodeStack.Item item1 = stack.getStackItem(1);
                if (item0.getConstant() instanceof Integer) {
                    OpcodeStack.Item tmp = item0;
                    item0 = item1;
                    item1 = tmp;
                }
                Object constant1 = item1.getConstant();
                XMethod returnValueOf = item0.getReturnValueOf();
                if (constant1 instanceof Integer
                        && returnValueOf != null
                        && "getYear".equals(returnValueOf.getName())
                        && ("java.util.Date".equals(returnValueOf.getClassName()) || "java.sql.Date".equals(returnValueOf.getClassName()))) {
                    int year = (Integer) constant1;
                    if (testingEnabled && year > 1900) {
                        accumulator.accumulateBug(
                                new BugInstance(this, "TESTING", HIGH_PRIORITY).addClassAndMethod(this)
                                .addString("Comparison of getYear does understand that it returns year-1900")
                                .addMethod(returnValueOf).describe(MethodAnnotation.METHOD_CALLED).addInt(year)
                                .describe(IntAnnotation.INT_VALUE), this);
                    }
                }
                break;
            default:
                break;
            }
        }

        // System.out.printf("%4d %10s: %s\n", getPC(), OPCODE_NAMES[seen],
        // stack);
        if (seen == IFLT && stack.getStackDepth() > 0 && stack.getStackItem(0).getSpecialKind() == OpcodeStack.Item.SIGNED_BYTE) {
            sawCheckForNonNegativeSignedByte = getPC();
        }

        if (pendingAbsoluteValueBug != null) {
            if (opcodesSincePendingAbsoluteValueBug == 0) {
                opcodesSincePendingAbsoluteValueBug++;
            } else {
                if (seen == IREM) {
                    OpcodeStack.Item top = stack.getStackItem(0);
                    Object constantValue = top.getConstant();
                    if (constantValue instanceof Number && Util.isPowerOfTwo(((Number) constantValue).intValue())) {
                        pendingAbsoluteValueBug.setPriority(Priorities.LOW_PRIORITY);
                    }
                }
                /*
                if (false)
                    try {
                        pendingAbsoluteValueBug.addString(OPCODE_NAMES[getPrevOpcode(1)] + ":" + OPCODE_NAMES[seen] + ":"
                                + OPCODE_NAMES[getNextOpcode()]);
                    } catch (Exception e) {
                        pendingAbsoluteValueBug.addString(OPCODE_NAMES[getPrevOpcode(1)] + ":" + OPCODE_NAMES[seen]);

                    }
                 */
                absoluteValueAccumulator.accumulateBug(pendingAbsoluteValueBug, pendingAbsoluteValueBugSourceLine);
                pendingAbsoluteValueBug = null;
                pendingAbsoluteValueBugSourceLine = null;
            }
        }

        if (seen == INVOKESTATIC
                && "org/easymock/EasyMock".equals(getClassConstantOperand())
                && ("replay".equals(getNameConstantOperand()) || "verify".equals(getNameConstantOperand()) || getNameConstantOperand()
                        .startsWith("reset")) && "([Ljava/lang/Object;)V".equals(getSigConstantOperand())
                        && getPrevOpcode(1) == ANEWARRAY && getPrevOpcode(2) == ICONST_0) {
            accumulator.accumulateBug(new BugInstance(this, "DMI_VACUOUS_CALL_TO_EASYMOCK_METHOD", NORMAL_PRIORITY)
            .addClassAndMethod(this).addCalledMethod(this), this);
        }

        if ((seen == INVOKESTATIC || seen == INVOKEVIRTUAL || seen == INVOKESPECIAL || seen == INVOKEINTERFACE)
                && getSigConstantOperand().indexOf("Ljava/lang/Runnable;") >= 0) {
            SignatureParser parser = new SignatureParser(getSigConstantOperand());
            int count = 0;
            for (Iterator<String> i = parser.parameterSignatureIterator(); i.hasNext(); count++) {
                String parameter = i.next();
                if ("Ljava/lang/Runnable;".equals(parameter)) {
                    OpcodeStack.Item item = stack.getStackItem(parser.getNumParameters() - 1 - count);
                    if ("Ljava/lang/Thread;".equals(item.getSignature())) {
                        accumulator.accumulateBug(new BugInstance(this, "DMI_THREAD_PASSED_WHERE_RUNNABLE_EXPECTED",
                                NORMAL_PRIORITY).addClassAndMethod(this).addCalledMethod(this), this);
                    }

                }
            }

        }

        if (prevOpcode == I2L && seen == INVOKESTATIC && "java/lang/Double".equals(getClassConstantOperand())
                && "longBitsToDouble".equals(getNameConstantOperand())) {
            accumulator.accumulateBug(new BugInstance(this, "DMI_LONG_BITS_TO_DOUBLE_INVOKED_ON_INT", HIGH_PRIORITY)
            .addClassAndMethod(this).addCalledMethod(this), this);
        }

        /**
         * Since you can change the number of core threads for a scheduled
         * thread pool executor, disabling this for now
         *
        if (false && seen == INVOKESPECIAL
                && getClassConstantOperand().equals("java/util/concurrent/ScheduledThreadPoolExecutor")
                && getNameConstantOperand().equals("<init>")) {

            int arguments = getNumberArguments(getSigConstantOperand());
            OpcodeStack.Item item = stack.getStackItem(arguments - 1);
            Object value = item.getConstant();
            if (value instanceof Integer && ((Integer) value).intValue() == 0)
                accumulator.accumulateBug(new BugInstance(this, "DMI_SCHEDULED_THREAD_POOL_EXECUTOR_WITH_ZERO_CORE_THREADS",
                        HIGH_PRIORITY).addClassAndMethod(this), this);

        }
         */
        for(SubDetector subDetector : subDetectors) {
            subDetector.sawOpcode(seen);
        }

        if (!sawLoadOfMinValue && seen == INVOKESTATIC &&
                ClassName.isMathClass(getClassConstantOperand()) && "abs".equals(getNameConstantOperand())
                ) {
            OpcodeStack.Item item0 = stack.getStackItem(0);
            int special = item0.getSpecialKind();
            if (special == OpcodeStack.Item.RANDOM_INT) {
                pendingAbsoluteValueBug = new BugInstance(this, "RV_ABSOLUTE_VALUE_OF_RANDOM_INT", HIGH_PRIORITY)
                .addClassAndMethod(this);
                pendingAbsoluteValueBugSourceLine = SourceLineAnnotation.fromVisitedInstruction(this);
                opcodesSincePendingAbsoluteValueBug = 0;
            }

            else if (special == OpcodeStack.Item.HASHCODE_INT) {
                pendingAbsoluteValueBug = new BugInstance(this, "RV_ABSOLUTE_VALUE_OF_HASHCODE", HIGH_PRIORITY)
                .addClassAndMethod(this);
                pendingAbsoluteValueBugSourceLine = SourceLineAnnotation.fromVisitedInstruction(this);
                opcodesSincePendingAbsoluteValueBug = 0;
            }

        }

        try {
            int stackLoc = stackEntryThatMustBeNonnegative(seen);
            if (stackLoc >= 0) {
                OpcodeStack.Item tos = stack.getStackItem(stackLoc);
                switch (tos.getSpecialKind()) {
                case OpcodeStack.Item.HASHCODE_INT_REMAINDER:
                    accumulator.accumulateBug(new BugInstance(this, "RV_REM_OF_HASHCODE", HIGH_PRIORITY).addClassAndMethod(this),
                            this);

                    break;
                case OpcodeStack.Item.RANDOM_INT:
                case OpcodeStack.Item.RANDOM_INT_REMAINDER:
                    accumulator.accumulateBug(
                            new BugInstance(this, "RV_REM_OF_RANDOM_INT", HIGH_PRIORITY).addClassAndMethod(this), this);
                    break;
                default:
                    break;
                }

            }
            if (seen == IREM) {
                OpcodeStack.Item item0 = stack.getStackItem(0);
                Object constant0 = item0.getConstant();
                if (constant0 instanceof Integer && ((Integer) constant0).intValue() == 1) {
                    accumulator.accumulateBug(new BugInstance(this, "INT_BAD_REM_BY_1", HIGH_PRIORITY).addClassAndMethod(this),
                            this);
                }

            }

            if (stack.getStackDepth() >= 1 && (seen == LOOKUPSWITCH || seen == TABLESWITCH)) {
                OpcodeStack.Item item0 = stack.getStackItem(0);
                if (item0.getSpecialKind() == OpcodeStack.Item.SIGNED_BYTE) {
                    int[] switchLabels = getSwitchLabels();
                    int[] switchOffsets = getSwitchOffsets();
                    for (int i = 0; i < switchLabels.length; i++) {
                        int v = switchLabels[i];
                        if (v <= -129 || v >= 128) {
                            accumulator.accumulateBug(new BugInstance(this, "INT_BAD_COMPARISON_WITH_SIGNED_BYTE", HIGH_PRIORITY)
                            .addClassAndMethod(this).addInt(v).describe(IntAnnotation.INT_VALUE),
                            SourceLineAnnotation.fromVisitedInstruction(this, getPC() + switchOffsets[i]));
                        }

                    }
                }
            }
            // check for use of signed byte where is it assumed it can be out of
            // the -128...127 range
            if (stack.getStackDepth() >= 2) {
                switch (seen) {
                case IF_ICMPEQ:
                case IF_ICMPNE:
                case IF_ICMPLT:
                case IF_ICMPLE:
                case IF_ICMPGE:
                case IF_ICMPGT:
                    OpcodeStack.Item item0 = stack.getStackItem(0);
                    OpcodeStack.Item item1 = stack.getStackItem(1);
                    int seen2 = seen;
                    if (item0.getConstant() != null) {
                        OpcodeStack.Item tmp = item0;
                        item0 = item1;
                        item1 = tmp;
                        switch (seen) {
                        case IF_ICMPLT:
                            seen2 = IF_ICMPGT;
                            break;
                        case IF_ICMPGE:
                            seen2 = IF_ICMPLE;
                            break;
                        case IF_ICMPGT:
                            seen2 = IF_ICMPLT;
                            break;
                        case IF_ICMPLE:
                            seen2 = IF_ICMPGE;
                            break;
                        default:
                            break;
                        }
                    }
                    Object constant1 = item1.getConstant();
                    if (item0.getSpecialKind() == OpcodeStack.Item.SIGNED_BYTE && constant1 instanceof Number) {
                        int v1 = ((Number) constant1).intValue();
                        if (v1 <= -129 || v1 >= 128 || v1 == 127 && !(seen2 == IF_ICMPEQ || seen2 == IF_ICMPNE

                                )) {
                            int priority = HIGH_PRIORITY;
                            if (v1 == 127) {
                                switch (seen2) {
                                case IF_ICMPGT: // 127 > x
                                    priority = LOW_PRIORITY;
                                    break;
                                case IF_ICMPGE: // 127 >= x : always true
                                    priority = NORMAL_PRIORITY;
                                    break;
                                case IF_ICMPLT: // 127 < x : never true
                                    priority = NORMAL_PRIORITY;
                                    break;
                                case IF_ICMPLE: // 127 <= x
                                    priority = LOW_PRIORITY;
                                    break;
                                }
                            } else if (v1 == 128) {
                                switch (seen2) {
                                case IF_ICMPGT: // 128 > x; always true
                                    priority = NORMAL_PRIORITY;
                                    break;
                                case IF_ICMPGE: // 128 >= x
                                    priority = HIGH_PRIORITY;
                                    break;
                                case IF_ICMPLT: // 128 < x
                                    priority = HIGH_PRIORITY;
                                    break;
                                case IF_ICMPLE: // 128 <= x; never true
                                    priority = NORMAL_PRIORITY;
                                    break;
                                }
                            } else if (v1 <= -129) {
                                priority = NORMAL_PRIORITY;
                            }

                            if (getPC() - sawCheckForNonNegativeSignedByte < 10) {
                                priority++;
                            }

                            accumulator.accumulateBug(new BugInstance(this, "INT_BAD_COMPARISON_WITH_SIGNED_BYTE", priority)
                            .addClassAndMethod(this).addInt(v1).describe(IntAnnotation.INT_VALUE).addValueSource(item0, this), this);

                        }
                    } else if (item0.getSpecialKind() == OpcodeStack.Item.NON_NEGATIVE && constant1 instanceof Number) {
                        int v1 = ((Number) constant1).intValue();
                        if (v1 < 0) {
                            accumulator.accumulateBug(new BugInstance(this, "INT_BAD_COMPARISON_WITH_NONNEGATIVE_VALUE",
                                    HIGH_PRIORITY).addClassAndMethod(this).addInt(v1).describe(IntAnnotation.INT_VALUE).addValueSource(item0, this), this);
                        }

                    }

                }
            }

            switch (seen) {
            case IFGE:
            case IFLT:
                if(stack.getStackDepth() > 0 && stack.getStackItem(0).getSpecialKind() == OpcodeStack.Item.NON_NEGATIVE) {
                    OpcodeStack.Item top = stack.getStackItem(0);
                    if (top.getRegisterNumber() != -1 && getMaxPC() > getNextPC() + 6) {
                        if (false) {
                            for(int i = -2; i <= 0; i++) {
                                int o = getPrevOpcode(-i);
                                System.out.printf("%2d %3d  %2x %s%n",  i, o, o, OPCODE_NAMES[o]);
                            }
                            for(int i = 0; i < 7; i++) {
                                int o = getNextCodeByte(i);
                                System.out.printf("%2d %3d %2x %s%n",  i, o, o, OPCODE_NAMES[o]);

                            }
                        }
                        int jump1, jump2;
                        if (seen == IFGE) {
                            jump1 = IF_ICMPLT;
                            jump2 = IF_ICMPLE;
                        } else {
                            jump1 = IF_ICMPGE;
                            jump2 = IF_ICMPGT;
                        }
                        int nextCodeByte0 = getNextCodeByte(0);
                        int loadConstant = 1;
                        if (nextCodeByte0 == ILOAD) {
                            loadConstant = 2;
                        }
                        int nextCodeByte1 = getNextCodeByte(loadConstant);
                        int nextCodeByte2 = getNextCodeByte(loadConstant+1);
                        int nextJumpOffset = loadConstant+2;
                        if (nextCodeByte1 == SIPUSH) {
                            nextJumpOffset++;
                        }
                        int nextCodeByteJump = getNextCodeByte(nextJumpOffset);

                        if (nextCodeByte0 == getPrevOpcode(1)
                                && (nextCodeByte1 == BIPUSH || nextCodeByte1 == SIPUSH)
                                && (IF_ICMPLT <= nextCodeByteJump && nextCodeByteJump <= IF_ICMPLE))
                        {
                            break;
                        }
                    }
                    accumulator.accumulateBug(new BugInstance(this, "INT_BAD_COMPARISON_WITH_NONNEGATIVE_VALUE",
                            NORMAL_PRIORITY).addClassAndMethod(this).addInt(0).describe(IntAnnotation.INT_VALUE).addValueSource(top, this), this);
                }
                break;
            case IAND:
            case LAND:
            case IOR:
            case LOR:
            case IXOR:
            case LXOR:
                long badValue = (seen == IAND || seen == LAND) ? -1 : 0;
                OpcodeStack.Item rhs = stack.getStackItem(0);
                OpcodeStack.Item lhs = stack.getStackItem(1);
                int prevOpcode = getPrevOpcode(1);
                int prevPrevOpcode = getPrevOpcode(2);
                if (rhs.hasConstantValue(badValue)
                        && (prevOpcode == LDC || prevOpcode == ICONST_0 || prevOpcode == ICONST_M1 || prevOpcode == LCONST_0)
                        && prevPrevOpcode != GOTO) {
                    reportVacuousBitOperation(seen, lhs);
                }

            }

            if (checkForBitIorofSignedByte && seen != I2B) {
                String pattern = (prevOpcode == LOR || prevOpcode == IOR) ? "BIT_IOR_OF_SIGNED_BYTE" : "BIT_ADD_OF_SIGNED_BYTE";
                int priority = (prevOpcode == LOR || prevOpcode == LADD) ? HIGH_PRIORITY : NORMAL_PRIORITY;
                accumulator.accumulateBug(new BugInstance(this, pattern, priority).addClassAndMethod(this), this);

                checkForBitIorofSignedByte = false;
            } else if ((seen == IOR || seen == LOR || seen == IADD || seen == LADD) && stack.getStackDepth() >= 2) {
                OpcodeStack.Item item0 = stack.getStackItem(0);
                OpcodeStack.Item item1 = stack.getStackItem(1);

                int special0 = item0.getSpecialKind();
                int special1 = item1.getSpecialKind();
                if (special0 == OpcodeStack.Item.SIGNED_BYTE && special1 == OpcodeStack.Item.LOW_8_BITS_CLEAR
                        && !item1.hasConstantValue(256) || special0 == OpcodeStack.Item.LOW_8_BITS_CLEAR
                        && !item0.hasConstantValue(256) && special1 == OpcodeStack.Item.SIGNED_BYTE) {
                    checkForBitIorofSignedByte = true;
                } else {
                    checkForBitIorofSignedByte = false;
                }
            } else {
                checkForBitIorofSignedByte = false;
            }

            if (prevOpcodeWasReadLine && sinceBufferedInputStreamReady >= 100 && seen == INVOKEVIRTUAL
                    && "java/lang/String".equals(getClassConstantOperand()) && getSigConstantOperand().startsWith("()")) {
                accumulator.accumulateBug(
                        new BugInstance(this, "NP_IMMEDIATE_DEREFERENCE_OF_READLINE", NORMAL_PRIORITY).addClassAndMethod(this),
                        this);
            }

            if (seen == INVOKEVIRTUAL && "java/io/BufferedReader".equals(getClassConstantOperand())
                    && "ready".equals(getNameConstantOperand()) && "()Z".equals(getSigConstantOperand())) {
                sinceBufferedInputStreamReady = 0;
            } else {
                sinceBufferedInputStreamReady++;
            }

            prevOpcodeWasReadLine = (seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
                    && "readLine".equals(getNameConstantOperand()) && "()Ljava/lang/String;".equals(getSigConstantOperand());

            // System.out.println(randomNextIntState + " " + OPCODE_NAMES[seen]
            // + " " + getMethodName());
            switch (randomNextIntState) {
            case 0:
                if (seen == INVOKEVIRTUAL && "java/util/Random".equals(getClassConstantOperand())
                && "nextDouble".equals(getNameConstantOperand()) || seen == INVOKESTATIC
                && ClassName.isMathClass(getClassConstantOperand()) && "random".equals(getNameConstantOperand())) {
                    randomNextIntState = 1;
                }
                break;
            case 1:
                if (seen == D2I) {
                    accumulator.accumulateBug(new BugInstance(this, "RV_01_TO_INT", HIGH_PRIORITY).addClassAndMethod(this), this);
                    randomNextIntState = 0;
                } else if (seen == DMUL) {
                    randomNextIntState = 4;
                } else if (seen == LDC2_W && getConstantRefOperand() instanceof ConstantDouble
                        && ((ConstantDouble) getConstantRefOperand()).getBytes() == Integer.MIN_VALUE) {
                    randomNextIntState = 0;
                } else {
                    randomNextIntState = 2;
                }

                break;
            case 2:
                if (seen == I2D) {
                    randomNextIntState = 3;
                } else if (seen == DMUL) {
                    randomNextIntState = 4;
                } else {
                    randomNextIntState = 0;
                }
                break;
            case 3:
                if (seen == DMUL) {
                    randomNextIntState = 4;
                } else {
                    randomNextIntState = 0;
                }
                break;
            case 4:
                if (seen == D2I) {
                    accumulator.accumulateBug(
                            new BugInstance(this, "DM_NEXTINT_VIA_NEXTDOUBLE", NORMAL_PRIORITY).addClassAndMethod(this), this);
                }
                randomNextIntState = 0;
                break;
            default:
                throw new IllegalStateException();
            }
            if (isPublicStaticVoidMain
                    && seen == INVOKEVIRTUAL
                    && getClassConstantOperand().startsWith("javax/swing/")
                    && ("show".equals(getNameConstantOperand()) && "()V".equals(getSigConstantOperand())
                            || "pack".equals(getNameConstantOperand()) && "()V".equals(getSigConstantOperand()) || "setVisible".equals(getNameConstantOperand()) && "(Z)V".equals(getSigConstantOperand()))) {
                accumulator.accumulateBug(
                        new BugInstance(this, "SW_SWING_METHODS_INVOKED_IN_SWING_THREAD", LOW_PRIORITY).addClassAndMethod(this),
                        this);
            }

            // if ((seen == INVOKEVIRTUAL)
            // && getClassConstantOperand().equals("java/lang/String")
            // && getNameConstantOperand().equals("substring")
            // && getSigConstantOperand().equals("(I)Ljava/lang/String;")
            // && stack.getStackDepth() > 1) {
            // OpcodeStack.Item item = stack.getStackItem(0);
            // Object o = item.getConstant();
            // if (o != null && o instanceof Integer) {
            // int v = ((Integer) o).intValue();
            // if (v == 0)
            // accumulator.accumulateBug(new BugInstance(this,
            // "DMI_USELESS_SUBSTRING", NORMAL_PRIORITY)
            // .addClassAndMethod(this)
            // .addSourceLine(this));
            // }
            // }

            if ((seen == INVOKEVIRTUAL) && "isAnnotationPresent".equals(getNameConstantOperand())
                    && "(Ljava/lang/Class;)Z".equals(getSigConstantOperand()) && stack.getStackDepth() > 0) {
                OpcodeStack.Item item = stack.getStackItem(0);
                Object value = item.getConstant();
                if (value instanceof String) {
                    String annotationClassName = (String) value;
                    boolean lacksClassfileRetention = AnalysisContext.currentAnalysisContext().getAnnotationRetentionDatabase()
                            .lacksRuntimeRetention(annotationClassName.replace('/', '.'));
                    if (lacksClassfileRetention) {
                        ClassDescriptor annotationClass = DescriptorFactory.createClassDescriptor(annotationClassName);
                        accumulator.accumulateBug(
                                new BugInstance(this, "DMI_ANNOTATION_IS_NOT_VISIBLE_TO_REFLECTION", HIGH_PRIORITY)
                                .addClassAndMethod(this).addCalledMethod(this).addClass(annotationClass)
                                .describe(ClassAnnotation.ANNOTATION_ROLE), this);
                    }

                }

            }
            if ((seen == INVOKEVIRTUAL) && "next".equals(getNameConstantOperand())
                    && "()Ljava/lang/Object;".equals(getSigConstantOperand()) && "hasNext".equals(getMethodName())
                    && "()Z".equals(getMethodSig()) && stack.getStackDepth() > 0) {
                OpcodeStack.Item item = stack.getStackItem(0);

                accumulator.accumulateBug(new BugInstance(this, "DMI_CALLING_NEXT_FROM_HASNEXT", item.isInitialParameter()
                        && item.getRegisterNumber() == 0 ? NORMAL_PRIORITY : LOW_PRIORITY).addClassAndMethod(this)
                        .addCalledMethod(this), this);

            }

            if ((seen == INVOKESPECIAL) && "java/lang/String".equals(getClassConstantOperand())
                    && "<init>".equals(getNameConstantOperand()) && "(Ljava/lang/String;)V".equals(getSigConstantOperand())
                    && !Subtypes2.isJSP(getThisClass())) {

                accumulator.accumulateBug(new BugInstance(this, "DM_STRING_CTOR", NORMAL_PRIORITY).addClassAndMethod(this), this);

            }

            if (seen == INVOKESTATIC && "java/lang/System".equals(getClassConstantOperand())
                    && "runFinalizersOnExit".equals(getNameConstantOperand()) || seen == INVOKEVIRTUAL
                    && "java/lang/Runtime".equals(getClassConstantOperand())
                    && "runFinalizersOnExit".equals(getNameConstantOperand())) {
                accumulator.accumulateBug(
                        new BugInstance(this, "DM_RUN_FINALIZERS_ON_EXIT", HIGH_PRIORITY).addClassAndMethod(this), this);
            }

            if ((seen == INVOKESPECIAL) && "java/lang/String".equals(getClassConstantOperand())
                    && "<init>".equals(getNameConstantOperand()) && "()V".equals(getSigConstantOperand())) {

                accumulator.accumulateBug(new BugInstance(this, "DM_STRING_VOID_CTOR", NORMAL_PRIORITY).addClassAndMethod(this),
                        this);

            }

            if (!isPublicStaticVoidMain && seen == INVOKESTATIC && "java/lang/System".equals(getClassConstantOperand())
                    && "exit".equals(getNameConstantOperand()) && !"processWindowEvent".equals(getMethodName())
                    && !getMethodName().startsWith("windowClos") && getMethodName().indexOf("exit") == -1
                    && getMethodName().indexOf("Exit") == -1 && getMethodName().indexOf("crash") == -1
                    && getMethodName().indexOf("Crash") == -1 && getMethodName().indexOf("die") == -1
                    && getMethodName().indexOf("Die") == -1 && getMethodName().indexOf("main") == -1) {
                accumulator.accumulateBug(new BugInstance(this, "DM_EXIT", getMethod().isStatic() ? LOW_PRIORITY
                        : NORMAL_PRIORITY).addClassAndMethod(this), SourceLineAnnotation.fromVisitedInstruction(this));
            }
            if (((seen == INVOKESTATIC && "java/lang/System".equals(getClassConstantOperand())) || (seen == INVOKEVIRTUAL && "java/lang/Runtime".equals(getClassConstantOperand())))
                    && "gc".equals(getNameConstantOperand())
                    && "()V".equals(getSigConstantOperand())
                    && !getDottedClassName().startsWith("java.lang")
                    && !getMethodName().startsWith("gc") && !getMethodName().endsWith("gc")) {
                if (gcInvocationBugReport == null) {
                    // System.out.println("Saw call to GC");
                    if (isPublicStaticVoidMain) {
                        // System.out.println("Skipping GC complaint in main method");
                        return;
                    }
                    if (isTestMethod(getMethod())) {
                        return;
                    }
                    // Just save this report in a field; it will be flushed
                    // IFF there were no calls to System.currentTimeMillis();
                    // in the method.
                    gcInvocationBugReport = new BugInstance(this, "DM_GC", HIGH_PRIORITY).addClassAndMethod(this).addSourceLine(
                            this);
                    gcInvocationPC = getPC();
                    // System.out.println("GC invocation at pc " + PC);
                }
            }
            if (!isSynthetic && (seen == INVOKESPECIAL) && "java/lang/Boolean".equals(getClassConstantOperand())
                    && "<init>".equals(getNameConstantOperand()) && !"java/lang/Boolean".equals(getClassName())) {
                int majorVersion = getThisClass().getMajor();
                if (majorVersion >= MAJOR_1_4) {
                    accumulator.accumulateBug(new BugInstance(this, "DM_BOOLEAN_CTOR", NORMAL_PRIORITY).addClassAndMethod(this),
                            this);
                }

            }
            if ((seen == INVOKESTATIC) && "java/lang/System".equals(getClassConstantOperand())
                    && ("currentTimeMillis".equals(getNameConstantOperand()) || "nanoTime".equals(getNameConstantOperand()))) {
                sawCurrentTimeMillis = true;
            }
            if ((seen == INVOKEVIRTUAL) && "java/lang/String".equals(getClassConstantOperand())
                    && "toString".equals(getNameConstantOperand()) && "()Ljava/lang/String;".equals(getSigConstantOperand())) {

                accumulator
                .accumulateBug(new BugInstance(this, "DM_STRING_TOSTRING", LOW_PRIORITY).addClassAndMethod(this), this);

            }

            if ((seen == INVOKEVIRTUAL) && "java/lang/String".equals(getClassConstantOperand())
                    && ("toUpperCase".equals(getNameConstantOperand()) || "toLowerCase".equals(getNameConstantOperand()))
                    && "()Ljava/lang/String;".equals(getSigConstantOperand())) {

                accumulator.accumulateBug(new BugInstance(this, "DM_CONVERT_CASE", LOW_PRIORITY).addClassAndMethod(this), this);

            }

            if ((seen == INVOKESPECIAL) && "<init>".equals(getNameConstantOperand())) {
                String cls = getClassConstantOperand();
                String sig = getSigConstantOperand();
                String primitiveType = ClassName.getPrimitiveType(cls);
                if (primitiveType != null && sig.charAt(1) == primitiveType.charAt(0)) {
                    primitiveObjCtorSeen = cls;
                } else {
                    primitiveObjCtorSeen = null;
                }
            } else if ((primitiveObjCtorSeen != null) && (seen == INVOKEVIRTUAL) && "toString".equals(getNameConstantOperand())
                    && getClassConstantOperand().equals(primitiveObjCtorSeen)
                    && "()Ljava/lang/String;".equals(getSigConstantOperand())) {
                BugInstance bug = new BugInstance(this, "DM_BOXED_PRIMITIVE_TOSTRING", NORMAL_PRIORITY).addClassAndMethod(this).addCalledMethod(this);
                MethodAnnotation preferred = new MethodAnnotation(ClassName.toDottedClassName(primitiveObjCtorSeen),
                        "toString", "("+ClassName.getPrimitiveType(primitiveObjCtorSeen)+")Ljava/lang/String;", true);
                bug.addMethod(preferred).describe(MethodAnnotation.SHOULD_CALL);
                accumulator.accumulateBug(
                        bug, this);

                primitiveObjCtorSeen = null;
            } else {
                primitiveObjCtorSeen = null;
            }

            if ((seen == INVOKESPECIAL) && "<init>".equals(getNameConstantOperand())) {
                ctorSeen = true;
            } else if (ctorSeen && (seen == INVOKEVIRTUAL) && "java/lang/Object".equals(getClassConstantOperand())
                    && "getClass".equals(getNameConstantOperand()) && "()Ljava/lang/Class;".equals(getSigConstantOperand())) {
                accumulator.accumulateBug(new BugInstance(this, "DM_NEW_FOR_GETCLASS", NORMAL_PRIORITY).addClassAndMethod(this),
                        this);
                ctorSeen = false;
            } else {
                ctorSeen = false;
            }

            if ((seen == INVOKEVIRTUAL) && isMonitorWait(getNameConstantOperand(), getSigConstantOperand())) {
                checkMonitorWait();
            }

            if ((seen == INVOKESPECIAL) && "<init>".equals(getNameConstantOperand())
                    && "java/lang/Thread".equals(getClassConstantOperand())) {
                String sig = getSigConstantOperand();
                if ("()V".equals(sig) || "(Ljava/lang/String;)V".equals(sig)
                        || "(Ljava/lang/ThreadGroup;Ljava/lang/String;)V".equals(sig)) {
                    OpcodeStack.Item invokedOn = stack.getItemMethodInvokedOn(this);
                    if (!"<init>".equals(getMethodName()) || invokedOn.getRegisterNumber() != 0) {
                        accumulator.accumulateBug(
                                new BugInstance(this, "DM_USELESS_THREAD", LOW_PRIORITY).addClassAndMethod(this), this);

                    }
                }
            }

            if (seen == INVOKESPECIAL && "java/math/BigDecimal".equals(getClassConstantOperand())
                    && "<init>".equals(getNameConstantOperand()) && "(D)V".equals(getSigConstantOperand())) {
                OpcodeStack.Item top = stack.getStackItem(0);
                Object value = top.getConstant();
                if (value instanceof Double && !((Double)value).isInfinite() && !((Double)value).isNaN()) {
                    double arg = ((Double) value).doubleValue();
                    String dblString = Double.toString(arg);
                    String bigDecimalString = new BigDecimal(arg).toString();
                    boolean ok = dblString.equals(bigDecimalString) || dblString.equals(bigDecimalString + ".0");

                    if (!ok) {
                        boolean scary = dblString.length() <= 8 && bigDecimalString.length() > 12
                                && dblString.toUpperCase().indexOf('E') == -1;
                        bugReporter.reportBug(new BugInstance(this, "DMI_BIGDECIMAL_CONSTRUCTED_FROM_DOUBLE",
                                scary ? NORMAL_PRIORITY : LOW_PRIORITY).addClassAndMethod(this).addCalledMethod(this)
                                .addMethod("java.math.BigDecimal", "valueOf", "(D)Ljava/math/BigDecimal;", true)
                                .describe(MethodAnnotation.METHOD_ALTERNATIVE_TARGET).addString(dblString)
                                .addString(bigDecimalString).addSourceLine(this));
                    }
                }

            }

        } finally {
            prevOpcode = seen;
        }
    }

    private void checkForCompatibleLongComparison(OpcodeStack.Item left, OpcodeStack.Item right) {
        if (left.getSpecialKind() == Item.RESULT_OF_I2L && right.getConstant() != null) {
            long value = ((Number) right.getConstant()).longValue();
            if ( (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE)) {
                int priority  = Priorities.HIGH_PRIORITY;
                if (value == Integer.MAX_VALUE+1L || value == Integer.MIN_VALUE-1L) {
                    priority = Priorities.NORMAL_PRIORITY;
                }
                String stringValue = IntAnnotation.getShortInteger(value)+"L";
                if (value == 0xffffffffL) {
                    stringValue = "0xffffffffL";
                } else if (value == 0x80000000L) {
                    stringValue = "0x80000000L";
                }
                accumulator.accumulateBug(new BugInstance(this, "INT_BAD_COMPARISON_WITH_INT_VALUE", priority ).addClassAndMethod(this)
                        .addString(stringValue).describe(StringAnnotation.STRING_NONSTRING_CONSTANT_ROLE)
                        .addValueSource(left, this) , this);
            }
        }
    }

    /**
     * @param seen
     * @param item
     */
    private void reportVacuousBitOperation(int seen, OpcodeStack.Item item) {
        if (item.getConstant() == null) {
            accumulator
            .accumulateBug(
                    new BugInstance(this, "INT_VACUOUS_BIT_OPERATION", NORMAL_PRIORITY)
                    .addClassAndMethod(this)
                    .addString(OPCODE_NAMES[seen])
                    .addOptionalAnnotation(
                            LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), item, getPC())), this);
        }
    }

    /**
     * Return index of stack entry that must be nonnegative.
     *
     * Return -1 if no stack entry is required to be nonnegative.
     */
    private int stackEntryThatMustBeNonnegative(int seen) {
        switch (seen) {
        case INVOKEINTERFACE:
            if ("java/util/List".equals(getClassConstantOperand())) {
                return getStackEntryOfListCallThatMustBeNonnegative();
            }
            break;
        case INVOKEVIRTUAL:
            if ("java/util/LinkedList".equals(getClassConstantOperand())
                    || "java/util/ArrayList".equals(getClassConstantOperand())) {
                return getStackEntryOfListCallThatMustBeNonnegative();
            }
            break;

        case IALOAD:
        case AALOAD:
        case SALOAD:
        case CALOAD:
        case BALOAD:
        case LALOAD:
        case DALOAD:
        case FALOAD:
            return 0;
        case IASTORE:
        case AASTORE:
        case SASTORE:
        case CASTORE:
        case BASTORE:
        case LASTORE:
        case DASTORE:
        case FASTORE:
            return 1;
        }
        return -1;
    }

    private int getStackEntryOfListCallThatMustBeNonnegative() {
        String name = getNameConstantOperand();
        if (("add".equals(name) || "set".equals(name)) && getSigConstantOperand().startsWith("(I")) {
            return 1;
        }
        if (("get".equals(name) || "remove".equals(name)) && getSigConstantOperand().startsWith("(I)")) {
            return 0;
        }
        return -1;
    }

    private void checkMonitorWait() {
        try {
            TypeDataflow typeDataflow = getClassContext().getTypeDataflow(getMethod());
            TypeDataflow.LocationAndFactPair pair = typeDataflow.getLocationAndFactForInstruction(getPC());

            if (pair == null) {
                return;
            }

            Type receiver = pair.frame.getInstance(pair.location.getHandle().getInstruction(), getClassContext()
                    .getConstantPoolGen());

            if (!(receiver instanceof ReferenceType)) {
                return;
            }

            if (Hierarchy.isSubtype((ReferenceType) receiver, CONDITION_TYPE)) {
                accumulator.accumulateBug(
                        new BugInstance(this, "DM_MONITOR_WAIT_ON_CONDITION", HIGH_PRIORITY).addClassAndMethod(this), this);

            }
        } catch (ClassNotFoundException e) {
            bugReporter.reportMissingClass(e);
        } catch (DataflowAnalysisException e) {
            bugReporter.logError("Exception caught by DumbMethods", e);
        } catch (CFGBuilderException e) {
            bugReporter.logError("Exception caught by DumbMethods", e);
        }
    }

    private boolean isMonitorWait(String name, String sig) {
        // System.out.println("Check call " + name + "," + sig);
        return "wait".equals(name) && ("()V".equals(sig) || "(J)V".equals(sig) || "(JI)V".equals(sig));
    }

    @Override
    public void visit(Code obj) {

        super.visit(obj);
        flush();
    }

    /**
     * Flush out cached state at the end of a method.
     */
    private void flush() {

        if (pendingAbsoluteValueBug != null) {
            absoluteValueAccumulator.accumulateBug(pendingAbsoluteValueBug, pendingAbsoluteValueBugSourceLine);
            pendingAbsoluteValueBug = null;
            pendingAbsoluteValueBugSourceLine = null;
        }
        accumulator.reportAccumulatedBugs();
        if (sawLoadOfMinValue) {
            absoluteValueAccumulator.clearBugs();
        } else {
            absoluteValueAccumulator.reportAccumulatedBugs();
        }
        if (gcInvocationBugReport != null && !sawCurrentTimeMillis) {
            // Make sure the GC invocation is not in an exception handler
            // for OutOfMemoryError.
            boolean outOfMemoryHandler = false;
            for (CodeException handler : exceptionTable) {
                if (gcInvocationPC < handler.getHandlerPC() || gcInvocationPC > handler.getHandlerPC() + OOM_CATCH_LEN) {
                    continue;
                }
                int catchTypeIndex = handler.getCatchType();
                if (catchTypeIndex > 0) {
                    ConstantPool cp = getThisClass().getConstantPool();
                    Constant constant = cp.getConstant(catchTypeIndex);
                    if (constant instanceof ConstantClass) {
                        String exClassName = (String) ((ConstantClass) constant).getConstantValue(cp);
                        if ("java/lang/OutOfMemoryError".equals(exClassName)) {
                            outOfMemoryHandler = true;
                            break;
                        }
                    }
                }
            }

            if (!outOfMemoryHandler) {
                bugReporter.reportBug(gcInvocationBugReport);
            }
        }

        sawCurrentTimeMillis = false;
        gcInvocationBugReport = null;

        exceptionTable = null;
    }
}
