/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs.visitclass;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.text.NumberFormat;

import javax.annotation.CheckForNull;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.LineNumberTable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.Hierarchy2;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;

abstract public class DismantleBytecode extends AnnotationVisitor {

    private int opcode;

    private boolean opcodeIsWide;

    private int PC, nextPC;

    private int branchOffset;

    private int branchTarget;

    private int branchFallThrough;

    private int[] switchOffsets;

    private int[] switchLabels;

    private final int[] prevOpcode = new int[32];

    private int currentPosInPrevOpcodeBuffer;

    private int sizePrevOpcodeBuffer;

    private int defaultSwitchOffset;

    private @SlashedClassName String classConstantOperand;

    private ClassDescriptor referencedClass;

    private XClass referencedXClass;

    private MethodDescriptor referencedMethod;

    private XMethod referencedXMethod;

    private FieldDescriptor referencedField;

    private XField referencedXField;

    private String dottedClassConstantOperand;

    private String nameConstantOperand;

    private String sigConstantOperand;

    private String stringConstantOperand;

    private String refConstantOperand;

    private boolean refFieldIsStatic;

    private Constant constantRefOperand;

    private int intConstant;

    private long longConstant;

    private float floatConstant;

    private double doubleConstant;

    private int registerOperand;

    private boolean isRegisterLoad;

    private boolean isRegisterStore;

    private static final int INVALID_OFFSET = Integer.MIN_VALUE;

    private static final String NOT_AVAILABLE = SlashedClassName.NOT_AVAILABLE;

    static String replaceSlashesWithDots(String c) {
        return c.replace('/', '.');
    }

    /**
     * Meaning of bytecode operands
     */
    public static final byte M_INT = 1;

    public static final byte M_UINT = 2;

    public static final byte M_CP = 3;

    public static final byte M_R = 4;

    public static final byte M_BR = 5;

    public static final byte M_PAD = 6;

    /**
     * Meaning of bytecode operands
     */
    static final byte[][] MEANING_OF_OPERANDS = {
        // 0 1 2 3 4 5 6 7 8 9
        {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, { M_INT }, { M_INT }, { M_CP }, { M_CP }, { M_CP },
        { M_R }, { M_R }, { M_R }, { M_R }, { M_R }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, { M_R }, { M_R }, { M_R }, { M_R }, { M_R }, {}, {}, {}, {}, {}, {}, {}, {},
        {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        {},
        {},
        {},
        {},
        {},
        // 130 1 2 3 4 5 6 7 8 9
        {}, {}, { M_R, M_INT }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, { M_BR },
        { M_BR }, { M_BR }, { M_BR }, { M_BR }, { M_BR }, { M_BR }, { M_BR }, { M_BR }, { M_BR }, { M_BR }, { M_BR },
        { M_BR }, { M_BR }, { M_BR },
        { M_BR },
        { M_R },
        // 170 1 2 3 4 5 6 7 8 9
        {}, {}, {}, {}, {}, {}, {}, {}, { M_CP }, { M_CP }, { M_CP }, { M_CP }, { M_CP }, { M_CP }, { M_CP },
        { M_CP, M_PAD, M_PAD }, { M_CP, M_PAD, M_PAD }, { M_CP }, { M_UINT },
        { M_CP },
        // 190 1 2 3 4 5 6 7 8 9
        {}, {}, { M_CP }, { M_CP }, {}, {}, { M_PAD }, { M_CP, M_UINT }, { M_BR }, { M_BR }, { M_BR }, { M_BR }, {}, {}, {},
        {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {} };

    protected byte[] codeBytes;

    protected LineNumberTable lineNumberTable;

    // Accessors

    public ClassDescriptor getClassDescriptorOperand() {
        if (referencedClass == null) {
            throw new IllegalStateException("getClassDescriptorOperand called but value not available");
        }
        return referencedClass;
    }

    public @CheckForNull XClass getXClassOperand() {
        return getReferencedXClass();
    }

    public boolean isMethodCall() {
        switch (opcode) {
        default:
            return false;
        case Const.INVOKEINTERFACE:
        case Const.INVOKESPECIAL:
        case Const.INVOKEVIRTUAL:
        case Const.INVOKESTATIC:
            return true;

        }
    }

    @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
    public MethodDescriptor getMethodDescriptorOperand() {
        if (nameConstantOperand == NOT_AVAILABLE || classConstantOperand == NOT_AVAILABLE) {
            throw new IllegalStateException("getMethodDescriptorOperand called but value not available");
        }

        if (referencedMethod == null) {
            referencedMethod = DescriptorFactory.instance().getMethodDescriptor(classConstantOperand, nameConstantOperand,
                    sigConstantOperand, opcode == Const.INVOKESTATIC);
        }
        return referencedMethod;
    }

    public @CheckForNull @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ") XMethod getXMethodOperand() {
        if (nameConstantOperand == NOT_AVAILABLE || classConstantOperand == NOT_AVAILABLE) {
            throw new IllegalStateException("getXMethodOperand called but value not available");
        }

        if (getReferencedXClass() != null && referencedXMethod == null) {
            referencedXMethod = Hierarchy2.findInvocationLeastUpperBound(getReferencedXClass(), nameConstantOperand,
                    sigConstantOperand, opcode == Const.INVOKESTATIC, opcode == Const.INVOKEINTERFACE);
        }

        return referencedXMethod;
    }

    @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
    public FieldDescriptor getFieldDescriptorOperand() {
        if (nameConstantOperand == NOT_AVAILABLE) {
            throw new IllegalStateException("getFieldDescriptorOperand called but value not available");
        }

        if (referencedField == null) {
            referencedField = DescriptorFactory.instance().getFieldDescriptor(classConstantOperand, nameConstantOperand,
                    sigConstantOperand, opcode == Const.GETSTATIC || opcode == Const.PUTSTATIC);
        }
        return referencedField;
    }

    public @CheckForNull XField getXFieldOperand() {
        if (getReferencedXClass() != null && referencedXField == null) {
            referencedXField = getReferencedXClass().findField(nameConstantOperand, sigConstantOperand,
                    opcode == Const.GETSTATIC || opcode == Const.PUTSTATIC);
        }

        return referencedXField;
    }

    /**
     * If the current opcode has a class operand, get the associated class
     * constant, dot-formatted
     */
    @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
    public String getDottedClassConstantOperand() {
        if (dottedClassConstantOperand != null) {
            assert dottedClassConstantOperand != NOT_AVAILABLE;
            return dottedClassConstantOperand;
        }
        if (classConstantOperand == NOT_AVAILABLE) {
            throw new IllegalStateException("getDottedClassConstantOperand called but value not available");
        }
        dottedClassConstantOperand = ClassName.toDottedClassName(classConstantOperand);
        return dottedClassConstantOperand;
    }

    /**
     * If the current opcode has a reference constant operand, get its string
     * representation
     */
    @Deprecated
    @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
    public String getRefConstantOperand() {
        if (refConstantOperand == NOT_AVAILABLE) {
            throw new IllegalStateException("getRefConstantOperand called but value not available");
        }
        if (refConstantOperand == null) {
            String dottedClassConstantOperand = getDottedClassConstantOperand();
            StringBuilder ref = new StringBuilder(dottedClassConstantOperand.length() + nameConstantOperand.length()
                    + sigConstantOperand.length() + 5);
            ref.append(dottedClassConstantOperand).append(".").append(nameConstantOperand).append(" : ")
                    .append(replaceSlashesWithDots(sigConstantOperand));
            refConstantOperand = ref.toString();
        }
        return refConstantOperand;
    }

    /** If the current opcode has a reference constant operand, get its name */
    @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
    public String getNameConstantOperand() {
        if (nameConstantOperand == NOT_AVAILABLE) {
            throw new IllegalStateException("getNameConstantOperand called but value not available");
        }
        return nameConstantOperand;
    }

    /**
     * If the current opcode has a reference constant operand, get its
     * signature, slash-formatted
     */
    @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
    public String getSigConstantOperand() {
        if (sigConstantOperand == NOT_AVAILABLE) {
            throw new IllegalStateException("getSigConstantOperand called but value not available");
        }
        return sigConstantOperand;
    }

    /**
     * If the current opcode has a class constant operand, get the classname,
     * slash-formatted.
     */
    public @SlashedClassName @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ") String getClassConstantOperand() {
        if (classConstantOperand == NOT_AVAILABLE) {
            throw new IllegalStateException("getClassConstantOperand called but value not available");
        }
        return classConstantOperand;
    }

    /** If the current opcode has a string constant operand, get its name */
    @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
    public String getStringConstantOperand() {
        if (stringConstantOperand == NOT_AVAILABLE) {
            throw new IllegalStateException("getStringConstantOperand called but value not available");
        }
        return stringConstantOperand;
    }

    public Constant getConstantRefOperand() {
        if (constantRefOperand == null) {
            throw new IllegalStateException("getConstantRefOperand called but value not available");
        }
        return constantRefOperand;
    }

    public boolean isRegisterLoad() {
        return isRegisterLoad;
    }

    public boolean isRegisterStore() {
        return isRegisterStore;
    }

    public int getRegisterOperand() {
        if (registerOperand == -1) {
            throw new IllegalStateException("getRegisterOperand called but value not available");
        }
        return registerOperand;
    }

    public int getIntConstant() {
        assert getOpcode() != Const.LDC || getConstantRefOperand() instanceof ConstantInteger;
        return intConstant;
    }

    public long getLongConstant() {
        assert getOpcode() != Const.LDC2_W || getConstantRefOperand() instanceof ConstantLong;
        return longConstant;
    }

    public int getBranchOffset() {
        if (branchOffset == INVALID_OFFSET) {
            throw new IllegalStateException("getBranchOffset called but value not available");
        }
        return branchOffset;
    }

    public int getBranchTarget() {
        if (branchTarget == INVALID_OFFSET) {
            throw new IllegalStateException("getBranchTarget called but value not available");
        }
        return branchTarget;
    }

    public int getBranchFallThrough() {
        if (branchFallThrough == INVALID_OFFSET) {
            throw new IllegalStateException("getBranchFallThrough called but value not available");
        }
        return branchFallThrough;
    }

    public int getDefaultSwitchOffset() {
        if (defaultSwitchOffset == INVALID_OFFSET) {
            throw new IllegalStateException("getDefaultSwitchOffset called but value not available");
        }
        return defaultSwitchOffset;
    }

    public boolean getRefFieldIsStatic() {
        return refFieldIsStatic;
    }

    public int getPC() {
        return PC;
    }

    /**
     * return previous opcode;
     *
     * @param offset
     *            0 for current opcode, 1 for one before that, etc.
     */
    public int getPrevOpcode(int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset (" + offset + ") must be nonnegative");
        }
        if (offset >= prevOpcode.length || offset > sizePrevOpcodeBuffer) {
            return Const.NOP;
        }
        int pos = currentPosInPrevOpcodeBuffer - offset;
        if (pos < 0) {
            pos += prevOpcode.length;
        }
        return prevOpcode[pos];
    }

    public boolean isWideOpcode() {
        return opcodeIsWide;
    }

    /**
     * Return whether or not given opcode is a branch instruction.
     *
     * @param opcode
     *            the opcode
     * @return true if instruction is a branch, false if not
     */
    public static boolean isBranch(int opcode) {
        byte[] operands = MEANING_OF_OPERANDS[opcode];
        return operands.length > 0 && operands[0] == M_BR;
    }

    /**
     * Return whether or not given opcode is a switch instruction.
     *
     * @param opcode
     *            the opcode
     * @return true if instruction is a switch, false if not
     */
    public static boolean isSwitch(int opcode) {
        return opcode == Const.LOOKUPSWITCH || opcode == Const.TABLESWITCH;
    }

    @SuppressFBWarnings("EI")
    public int[] getSwitchOffsets() {
        if (switchOffsets == null) {
            throw new IllegalStateException("getSwitchOffsets called but value not available");
        }
        return switchOffsets;
    }

    @SuppressFBWarnings("EI")
    public int[] getSwitchLabels() {
        if (switchLabels == null) {
            throw new IllegalStateException("getSwitchLabels called but value not available");
        }
        return switchLabels;
    }

    private void resetState() {
        classConstantOperand = nameConstantOperand = sigConstantOperand = stringConstantOperand = refConstantOperand = NOT_AVAILABLE;
        refFieldIsStatic = false;
        constantRefOperand = null;
        registerOperand = -1;
        isRegisterLoad = false;
        isRegisterStore = false;
        branchOffset = branchTarget = branchFallThrough = defaultSwitchOffset = INVALID_OFFSET;
        switchOffsets = switchLabels = null;
        dottedClassConstantOperand = null;
        referencedClass = null;
        setReferencedXClass(null);
        referencedMethod = null;
        referencedXMethod = null;
        referencedField = null;
        referencedXField = null;
    }

    private static void sortByOffset(int[] switchOffsets, int[] switchLabels) {
        int npairs = switchOffsets.length;
        // Sort by offset
        for (int j = 0; j < npairs; j++) {
            int min = j;
            for (int k = j + 1; k < npairs; k++) {
                if (switchOffsets[min] > switchOffsets[k]) {
                    min = k;
                }
            }
            if (min > j) {
                int tmp = switchOffsets[min];
                switchOffsets[min] = switchOffsets[j];
                switchOffsets[j] = tmp;
                tmp = switchLabels[min];
                switchLabels[min] = switchLabels[j];
                switchLabels[j] = tmp;
            }
        }
    }

    public int getMaxPC() {
        return codeBytes.length - 1;
    }

    public int getCodeByte(int offset) {
        return 0xff & codeBytes[offset];
    }

    public int getOpcode() {
        return opcode;
    }

    public boolean atCatchBlock() {
        for (CodeException e : getCode().getExceptionTable()) {
            if (e.getHandlerPC() == getPC()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visit(Code obj) {
        //        if (getXMethod().usesInvokeDynamic()) {
        //            AnalysisContext.currentAnalysisContext().analysisSkippedDueToInvokeDynamic(getXMethod());
        //            return;
        //        }
        sizePrevOpcodeBuffer = 0;
        currentPosInPrevOpcodeBuffer = prevOpcode.length - 1;

        int switchLow = 1000000;
        int switchHigh = -1000000;
        codeBytes = obj.getCode();
        DataInputStream byteStream = new DataInputStream(new ByteArrayInputStream(codeBytes));

        lineNumberTable = obj.getLineNumberTable();

        try {
            for (int i = 0; i < codeBytes.length;) {
                resetState();
                PC = i;
                opcodeIsWide = false;
                opcode = byteStream.readUnsignedByte();

                sizePrevOpcodeBuffer++;
                currentPosInPrevOpcodeBuffer++;
                if (currentPosInPrevOpcodeBuffer >= prevOpcode.length) {
                    currentPosInPrevOpcodeBuffer = 0;
                }
                prevOpcode[currentPosInPrevOpcodeBuffer] = opcode;
                i++;
                // System.out.println(Const.getOpcodeName(opCode));
                int byteStreamArgCount = Const.getNoOfOperands(opcode);
                if (byteStreamArgCount == Const.UNPREDICTABLE) {

                    if (opcode == Const.LOOKUPSWITCH) {
                        int pad = 4 - (i & 3);
                        if (pad == 4) {
                            pad = 0;
                        }
                        int count = pad;
                        while (count > 0) {
                            count -= byteStream.skipBytes(count);
                        }
                        i += pad;
                        defaultSwitchOffset = byteStream.readInt();
                        branchOffset = defaultSwitchOffset;
                        branchTarget = branchOffset + PC;
                        i += 4;
                        int npairs = byteStream.readInt();
                        i += 4;
                        switchOffsets = new int[npairs];
                        switchLabels = new int[npairs];
                        for (int o = 0; o < npairs; o++) {
                            switchLabels[o] = byteStream.readInt();
                            switchOffsets[o] = byteStream.readInt();
                            i += 8;
                        }
                        sortByOffset(switchOffsets, switchLabels);
                    } else if (opcode == Const.TABLESWITCH) {
                        int pad = 4 - (i & 3);
                        if (pad == 4) {
                            pad = 0;
                        }
                        int count = pad;
                        while (count > 0) {
                            count -= byteStream.skipBytes(count);
                        }
                        i += pad;
                        defaultSwitchOffset = byteStream.readInt();
                        branchOffset = defaultSwitchOffset;
                        branchTarget = branchOffset + PC;
                        i += 4;
                        switchLow = byteStream.readInt();
                        i += 4;
                        switchHigh = byteStream.readInt();
                        i += 4;
                        int npairs = switchHigh - switchLow + 1;
                        switchOffsets = new int[npairs];
                        switchLabels = new int[npairs];
                        for (int o = 0; o < npairs; o++) {
                            switchLabels[o] = o + switchLow;
                            switchOffsets[o] = byteStream.readInt();
                            i += 4;
                        }
                        sortByOffset(switchOffsets, switchLabels);
                    } else if (opcode == Const.WIDE) {
                        opcodeIsWide = true;
                        opcode = byteStream.readUnsignedByte();
                        i++;
                        switch (opcode) {
                        case Const.ILOAD:
                        case Const.FLOAD:
                        case Const.ALOAD:
                        case Const.LLOAD:
                        case Const.DLOAD:
                        case Const.ISTORE:
                        case Const.FSTORE:
                        case Const.ASTORE:
                        case Const.LSTORE:
                        case Const.DSTORE:
                        case Const.RET:
                            registerOperand = byteStream.readUnsignedShort();
                            i += 2;
                            break;
                        case Const.IINC:
                            registerOperand = byteStream.readUnsignedShort();
                            i += 2;
                            intConstant = byteStream.readShort();
                            i += 2;
                            break;
                        default:
                            throw new IllegalStateException(String.format("bad wide bytecode %d: %s", opcode, Const.getOpcodeName(opcode)));
                        }
                    } else {
                        throw new IllegalStateException(String.format("bad unpredicatable bytecode %d: %s", opcode, Const.getOpcodeName(opcode)));
                    }
                } else {
                    if (byteStreamArgCount < 0) {
                        throw new IllegalStateException(String.format("bad length for bytecode %d: %s", opcode, Const.getOpcodeName(opcode)));
                    }
                    for (int k = 0; k < Const.getOperandTypeCount(opcode); k++) {

                        int v;
                        int t = Const.getOperandType(opcode, k);
                        int m = MEANING_OF_OPERANDS[opcode][k];
                        boolean unsigned = (m == M_CP || m == M_R || m == M_UINT);
                        switch (t) {
                        case Const.T_BYTE:
                            if (unsigned) {
                                v = byteStream.readUnsignedByte();
                            } else {
                                v = byteStream.readByte();
                            }
                            /*
                             * System.out.print("Read byte " + v);
                             * System.out.println(" with meaning" + m);
                             */
                            i++;
                            break;
                        case Const.T_SHORT:
                            if (unsigned) {
                                v = byteStream.readUnsignedShort();
                            } else {
                                v = byteStream.readShort();
                            }
                            i += 2;
                            break;
                        case Const.T_INT:
                            v = byteStream.readInt();
                            i += 4;
                            break;
                        default:
                            throw new IllegalStateException();
                        }
                        switch (m) {
                        case M_BR:
                            branchOffset = v;
                            branchTarget = v + PC;
                            branchFallThrough = i;
                            break;
                        case M_CP:
                            constantRefOperand = getConstantPool().getConstant(v);
                            if (constantRefOperand instanceof ConstantClass) {
                                ConstantClass clazz = (ConstantClass) constantRefOperand;
                                classConstantOperand = getStringFromIndex(clazz.getNameIndex());
                                referencedClass = DescriptorFactory.createClassDescriptor(classConstantOperand);

                            } else if (constantRefOperand instanceof ConstantInteger) {
                                intConstant = ((ConstantInteger) constantRefOperand).getBytes();
                            } else if (constantRefOperand instanceof ConstantLong) {
                                longConstant = ((ConstantLong) constantRefOperand).getBytes();
                            } else if (constantRefOperand instanceof ConstantFloat) {
                                floatConstant = ((ConstantFloat) constantRefOperand).getBytes();
                            } else if (constantRefOperand instanceof ConstantDouble) {
                                doubleConstant = ((ConstantDouble) constantRefOperand).getBytes();
                            } else if (constantRefOperand instanceof ConstantString) {
                                int s = ((ConstantString) constantRefOperand).getStringIndex();

                                stringConstantOperand = getStringFromIndex(s);
                            } else if (constantRefOperand instanceof ConstantInvokeDynamic) {
                                ConstantInvokeDynamic id = (ConstantInvokeDynamic) constantRefOperand;
                                ConstantNameAndType sig = (ConstantNameAndType) getConstantPool().getConstant(
                                        id.getNameAndTypeIndex());
                                nameConstantOperand = getStringFromIndex(sig.getNameIndex());
                                sigConstantOperand = getStringFromIndex(sig.getSignatureIndex());
                            } else if (constantRefOperand instanceof ConstantCP) {
                                ConstantCP cp = (ConstantCP) constantRefOperand;
                                ConstantClass clazz = (ConstantClass) getConstantPool().getConstant(cp.getClassIndex());
                                classConstantOperand = getStringFromIndex(clazz.getNameIndex());
                                referencedClass = DescriptorFactory.createClassDescriptor(classConstantOperand);
                                referencedXClass = null;
                                ConstantNameAndType sig = (ConstantNameAndType) getConstantPool().getConstant(
                                        cp.getNameAndTypeIndex());
                                nameConstantOperand = getStringFromIndex(sig.getNameIndex());
                                sigConstantOperand = getStringFromIndex(sig.getSignatureIndex());
                                refConstantOperand = null;
                            }
                            break;
                        case M_R:
                            registerOperand = v;
                            break;
                        case M_UINT:
                        case M_INT:
                            intConstant = v;
                            break;
                        case M_PAD:
                            break;
                        default:
                            throw new IllegalStateException("Unexpecting meaning " + m);
                        }
                    }

                }
                switch (opcode) {
                case Const.IINC:
                    isRegisterLoad = true;
                    isRegisterStore = true;
                    break;
                case Const.ILOAD_0:
                case Const.ILOAD_1:
                case Const.ILOAD_2:
                case Const.ILOAD_3:
                    registerOperand = opcode - Const.ILOAD_0;
                    isRegisterLoad = true;
                    break;

                case Const.ALOAD_0:
                case Const.ALOAD_1:
                case Const.ALOAD_2:
                case Const.ALOAD_3:
                    registerOperand = opcode - Const.ALOAD_0;
                    isRegisterLoad = true;
                    break;

                case Const.FLOAD_0:
                case Const.FLOAD_1:
                case Const.FLOAD_2:
                case Const.FLOAD_3:
                    registerOperand = opcode - Const.FLOAD_0;
                    isRegisterLoad = true;
                    break;

                case Const.DLOAD_0:
                case Const.DLOAD_1:
                case Const.DLOAD_2:
                case Const.DLOAD_3:
                    registerOperand = opcode - Const.DLOAD_0;
                    isRegisterLoad = true;
                    break;

                case Const.LLOAD_0:
                case Const.LLOAD_1:
                case Const.LLOAD_2:
                case Const.LLOAD_3:
                    registerOperand = opcode - Const.LLOAD_0;
                    isRegisterLoad = true;
                    break;
                case Const.ILOAD:
                case Const.FLOAD:
                case Const.ALOAD:
                case Const.LLOAD:
                case Const.DLOAD:
                    isRegisterLoad = true;
                    break;

                case Const.ISTORE_0:
                case Const.ISTORE_1:
                case Const.ISTORE_2:
                case Const.ISTORE_3:
                    registerOperand = opcode - Const.ISTORE_0;
                    isRegisterStore = true;
                    break;

                case Const.ASTORE_0:
                case Const.ASTORE_1:
                case Const.ASTORE_2:
                case Const.ASTORE_3:
                    registerOperand = opcode - Const.ASTORE_0;
                    isRegisterStore = true;
                    break;

                case Const.FSTORE_0:
                case Const.FSTORE_1:
                case Const.FSTORE_2:
                case Const.FSTORE_3:
                    registerOperand = opcode - Const.FSTORE_0;
                    isRegisterStore = true;
                    break;

                case Const.DSTORE_0:
                case Const.DSTORE_1:
                case Const.DSTORE_2:
                case Const.DSTORE_3:
                    registerOperand = opcode - Const.DSTORE_0;
                    isRegisterStore = true;
                    break;

                case Const.LSTORE_0:
                case Const.LSTORE_1:
                case Const.LSTORE_2:
                case Const.LSTORE_3:
                    registerOperand = opcode - Const.LSTORE_0;
                    isRegisterStore = true;
                    break;
                case Const.ISTORE:
                case Const.FSTORE:
                case Const.ASTORE:
                case Const.LSTORE:
                case Const.DSTORE:
                    isRegisterStore = true;
                    break;
                default:
                    break;
                }

                switch (opcode) {
                case Const.ILOAD:
                case Const.FLOAD:
                case Const.ALOAD:
                case Const.LLOAD:
                case Const.DLOAD:
                    // registerKind = opcode - ILOAD;
                    break;
                case Const.ISTORE:
                case Const.FSTORE:
                case Const.ASTORE:
                case Const.LSTORE:
                case Const.DSTORE:
                    // registerKind = opcode - ISTORE;
                    break;
                case Const.RET:
                    // registerKind = R_REF;
                    break;
                case Const.GETSTATIC:
                case Const.PUTSTATIC:
                    refFieldIsStatic = true;
                    break;
                case Const.GETFIELD:
                case Const.PUTFIELD:
                    refFieldIsStatic = false;
                    break;
                default:
                    break;
                }

                nextPC = i;
                if (beforeOpcode(opcode)) {
                    sawOpcode(opcode);
                }
                afterOpcode(opcode);

                if (opcode == Const.TABLESWITCH) {
                    sawInt(switchLow);
                    sawInt(switchHigh);
                    //                    int prevOffset = i - PC;
                    for (int o = 0; o <= switchHigh - switchLow; o++) {
                        sawBranchTo(switchOffsets[o] + PC);
                        //                        prevOffset = switchOffsets[o];
                    }
                    sawBranchTo(defaultSwitchOffset + PC);
                } else if (opcode == Const.LOOKUPSWITCH) {
                    sawInt(switchOffsets.length);
                    //                    int prevOffset = i - PC;
                    for (int o = 0; o < switchOffsets.length; o++) {
                        sawBranchTo(switchOffsets[o] + PC);
                        //                        prevOffset = switchOffsets[o];
                        sawInt(switchLabels[o]);
                    }
                    sawBranchTo(defaultSwitchOffset + PC);
                } else {
                    for (int k = 0; k < Const.getOperandTypeCount(opcode); k++) {
                        int m = MEANING_OF_OPERANDS[opcode][k];
                        switch (m) {
                        case M_BR:
                            sawBranchTo(branchOffset + PC);
                            break;
                        case M_CP:
                            if (constantRefOperand instanceof ConstantInteger) {
                                sawInt(intConstant);
                            } else if (constantRefOperand instanceof ConstantLong) {
                                sawLong(longConstant);
                            } else if (constantRefOperand instanceof ConstantFloat) {
                                sawFloat(floatConstant);
                            } else if (constantRefOperand instanceof ConstantDouble) {
                                sawDouble(doubleConstant);
                            } else if (constantRefOperand instanceof ConstantString) {
                                sawString(stringConstantOperand);
                            } else if (constantRefOperand instanceof ConstantFieldref) {
                                sawField();
                            } else if (constantRefOperand instanceof ConstantMethodref) {
                                sawMethod();
                            } else if (constantRefOperand instanceof ConstantInterfaceMethodref) {
                                sawIMethod();
                            } else if (constantRefOperand instanceof ConstantClass) {
                                sawClass();
                            }
                            break;
                        case M_R:
                            sawRegister(registerOperand);
                            break;
                        case M_INT:
                            sawInt(intConstant);
                            break;
                        default:
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            AnalysisContext.logError("Error while dismantling bytecode", e);
            assert false;
        }

        try {
            byteStream.close();
        } catch (IOException e) {
            assert false;
        }
    }

    public void sawDouble(double seen) {
    }

    public void sawFloat(float seen) {
    }

    public void sawRegister(int r) {
    }

    public void sawInt(int seen) {
    }

    public void sawLong(long seen) {
    }

    public void sawBranchTo(int targetPC) {
    }

    /** return false if we should skip calling sawOpcode */
    public boolean beforeOpcode(int seen) {
        return true;
    }

    public void afterOpcode(int seen) {
    }

    public void sawOpcode(int seen) {
    }

    public void sawString(String seen) {
    }

    public void sawField() {
    }

    public void sawMethod() {
    }

    public void sawIMethod() {
    }

    public void sawClass() {
    }

    static private NumberFormat formatter = NumberFormat.getIntegerInstance();
    static {
        formatter.setMinimumIntegerDigits(4);
        formatter.setGroupingUsed(false);
    }

    public void printOpCode(int seen) {
        System.out.print("  " + this.getClass().getSimpleName() + ": [" + formatter.format(getPC()) + "]  " + Const.getOpcodeName(seen));
        if ((seen == Const.INVOKEVIRTUAL) || (seen == Const.INVOKESPECIAL) || (seen == Const.INVOKEINTERFACE) || (seen == Const.INVOKESTATIC)) {
            System.out.print("   " + getClassConstantOperand() + "." + getNameConstantOperand() + " " + getSigConstantOperand());
        } else if (seen == Const.LDC || seen == Const.LDC_W || seen == Const.LDC2_W) {
            Constant c = getConstantRefOperand();
            if (c instanceof ConstantString) {
                System.out.print("   \"" + getStringConstantOperand() + "\"");
            } else if (c instanceof ConstantClass) {
                System.out.print("   " + getClassConstantOperand());
            } else {
                System.out.print("   " + c);
            }
        } else if ((seen == Const.ALOAD) || (seen == Const.ASTORE)) {
            System.out.print("   " + getRegisterOperand());
        } else if ((seen == Const.GOTO) || (seen == Const.GOTO_W) || (seen == Const.IF_ACMPEQ) || (seen == Const.IF_ACMPNE)
                || (seen == Const.IF_ICMPEQ)
                || (seen == Const.IF_ICMPGE) || (seen == Const.IF_ICMPGT) || (seen == Const.IF_ICMPLE) || (seen == Const.IF_ICMPLT)
                || (seen == Const.IF_ICMPNE) || (seen == Const.IFEQ) || (seen == Const.IFGE) || (seen == Const.IFGT) || (seen == Const.IFLE)
                || (seen == Const.IFLT)
                || (seen == Const.IFNE) || (seen == Const.IFNONNULL) || (seen == Const.IFNULL)) {
            System.out.print("   " + getBranchTarget());
        } else if ((seen == Const.NEW) || (seen == Const.INSTANCEOF)) {
            System.out.print("   " + getClassConstantOperand());
        } else if ((seen == Const.TABLESWITCH) || (seen == Const.LOOKUPSWITCH)) {
            System.out.print("    [");
            int switchPC = getPC();
            int[] offsets = getSwitchOffsets();
            for (int offset : offsets) {
                System.out.print((switchPC + offset) + ",");
            }
            System.out.print((switchPC + getDefaultSwitchOffset()) + "]");
        }

        System.out.println();
    }

    /**
     * @return Returns the nextPC.
     */
    public int getNextPC() {
        return nextPC;
    }

    public int getNextOpcode() {
        return codeBytes[nextPC] & 0xff;
    }

    public int getNextCodeByte(int offset) {
        return codeBytes[nextPC + offset] & 0xff;
    }

    public boolean isReturn(int opcode) {
        switch (opcode) {
        case Const.IRETURN:
        case Const.ARETURN:
        case Const.LRETURN:
        case Const.DRETURN:
        case Const.FRETURN:
        case Const.RETURN:
            return true;
        default:
            return false;
        }
    }

    public boolean isShift(int opcode) {
        switch (opcode) {
        case Const.IUSHR:
        case Const.ISHR:
        case Const.ISHL:
        case Const.LUSHR:
        case Const.LSHR:
        case Const.LSHL:
            return true;
        default:
            return false;
        }
    }

    public static boolean areOppositeBranches(int opcode1, int opcode2) {
        if (!isBranch(opcode1)) {
            throw new IllegalArgumentException(Const.getOpcodeName(opcode1) + " isn't a branch");
        }
        if (!isBranch(opcode2)) {
            throw new IllegalArgumentException(Const.getOpcodeName(opcode2) + " isn't a branch");
        }
        switch (opcode1) {
        case Const.IF_ACMPEQ:
        case Const.IF_ACMPNE:
        case Const.IF_ICMPEQ:
        case Const.IF_ICMPNE:
        case Const.IF_ICMPLT:
        case Const.IF_ICMPLE:
        case Const.IF_ICMPGT:
        case Const.IF_ICMPGE:
        case Const.IFNE:
        case Const.IFEQ:
        case Const.IFLT:
        case Const.IFLE:
        case Const.IFGT:
        case Const.IFGE:
            return ((opcode1 + 1) ^ 1) == opcode2 + 1;
        case Const.IFNONNULL:
            return opcode2 == Const.IFNULL;
        case Const.IFNULL:
            return opcode2 == Const.IFNONNULL;
        default:
            return false;

        }
    }

    public boolean isRegisterStore(int opcode) {
        switch (opcode) {
        case Const.ISTORE_0:
        case Const.ISTORE_1:
        case Const.ISTORE_2:
        case Const.ISTORE_3:

        case Const.ASTORE_0:
        case Const.ASTORE_1:
        case Const.ASTORE_2:
        case Const.ASTORE_3:

        case Const.FSTORE_0:
        case Const.FSTORE_1:
        case Const.FSTORE_2:
        case Const.FSTORE_3:

        case Const.DSTORE_0:
        case Const.DSTORE_1:
        case Const.DSTORE_2:
        case Const.DSTORE_3:

        case Const.LSTORE_0:
        case Const.LSTORE_1:
        case Const.LSTORE_2:
        case Const.LSTORE_3:

        case Const.ISTORE:
        case Const.FSTORE:
        case Const.ASTORE:
        case Const.LSTORE:
        case Const.DSTORE:
            return true;
        default:
            return false;
        }
    }

    /**
     * @param referencedXClass
     *            The referencedXClass to set.
     */
    private void setReferencedXClass(XClass referencedXClass) {
        this.referencedXClass = referencedXClass;
    }

    /**
     * @return Returns the referencedXClass.
     */
    private XClass getReferencedXClass() {
        if (referencedXClass == null && referencedClass != null) {
            try {
                referencedXClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, referencedClass);
            } catch (CheckedAnalysisException e) {
                assert true;
            }
        }
        return referencedXClass;
    }
}
