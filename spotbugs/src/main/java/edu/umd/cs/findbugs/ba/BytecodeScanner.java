/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba;

import org.apache.bcel.Const;

import edu.umd.cs.findbugs.SystemProperties;

/**
 * Scan the raw bytecodes of a method. This is useful in order to find out
 * quickly whether or not a method uses particular instructions.
 *
 * @author David Hovemeyer
 */
public class BytecodeScanner {
    private static final boolean DEBUG = SystemProperties.getBoolean("bs.debug");

    /**
     * Callback interface to report scanned instructions.
     */
    public interface Callback {
        /**
         * Called to indicate that a particular bytecode has been scanned.
         *
         * @param opcode
         *            the opcode of the instruction
         * @param index
         *            the bytecode offset of the instruction
         */
        public void handleInstruction(int opcode, int index);
    }

    /**
     * Convert the unsigned value of a byte into a short.
     *
     * @param value
     *            the byte
     * @return the byte's unsigned value as a short
     */
    private static short unsignedValueOf(byte value) {
        short result;
        if ((value & 0x80) != 0) {
            result = (short) (value & 0x7F);
            result |= 0x80;
        } else {
            result = value;
        }
        return result;
    }

    /**
     * Extract an int from bytes at the given offset in the array.
     *
     * @param arr
     *            the array
     * @param offset
     *            the offset in the array
     */
    private static int extractInt(byte[] arr, int offset) {
        return ((arr[offset] & 0xFF) << 24) | ((arr[offset + 1] & 0xFF) << 16) | ((arr[offset + 2] & 0xFF) << 8)
                | (arr[offset + 3] & 0xFF);
    }

    private static final int PAD[] = { 0, 3, 2, 1 };

    /**
     * Scan the raw bytecodes of a method.
     *
     * @param instructionList
     *            the bytecodes
     * @param callback
     *            the callback object
     */
    public void scan(byte[] instructionList, Callback callback) {

        boolean wide = false;

        for (int index = 0; index < instructionList.length;) {
            short opcode = unsignedValueOf(instructionList[index]);
            callback.handleInstruction(opcode, index);

            if (DEBUG) {
                System.out.println(index + ": " + Const.getOpcodeName(opcode));
            }

            switch (opcode) {

            // Single byte instructions.
            case Const.NOP:
            case Const.ACONST_NULL:
            case Const.ICONST_M1:
            case Const.ICONST_0:
            case Const.ICONST_1:
            case Const.ICONST_2:
            case Const.ICONST_3:
            case Const.ICONST_4:
            case Const.ICONST_5:
            case Const.LCONST_0:
            case Const.LCONST_1:
            case Const.FCONST_0:
            case Const.FCONST_1:
            case Const.FCONST_2:
            case Const.DCONST_0:
            case Const.DCONST_1:
            case Const.ILOAD_0:
            case Const.ILOAD_1:
            case Const.ILOAD_2:
            case Const.ILOAD_3:
            case Const.LLOAD_0:
            case Const.LLOAD_1:
            case Const.LLOAD_2:
            case Const.LLOAD_3:
            case Const.FLOAD_0:
            case Const.FLOAD_1:
            case Const.FLOAD_2:
            case Const.FLOAD_3:
            case Const.DLOAD_0:
            case Const.DLOAD_1:
            case Const.DLOAD_2:
            case Const.DLOAD_3:
            case Const.ALOAD_0:
            case Const.ALOAD_1:
            case Const.ALOAD_2:
            case Const.ALOAD_3:
            case Const.IALOAD:
            case Const.LALOAD:
            case Const.FALOAD:
            case Const.DALOAD:
            case Const.AALOAD:
            case Const.BALOAD:
            case Const.CALOAD:
            case Const.SALOAD:
            case Const.ISTORE_0:
            case Const.ISTORE_1:
            case Const.ISTORE_2:
            case Const.ISTORE_3:
            case Const.LSTORE_0:
            case Const.LSTORE_1:
            case Const.LSTORE_2:
            case Const.LSTORE_3:
            case Const.FSTORE_0:
            case Const.FSTORE_1:
            case Const.FSTORE_2:
            case Const.FSTORE_3:
            case Const.DSTORE_0:
            case Const.DSTORE_1:
            case Const.DSTORE_2:
            case Const.DSTORE_3:
            case Const.ASTORE_0:
            case Const.ASTORE_1:
            case Const.ASTORE_2:
            case Const.ASTORE_3:
            case Const.IASTORE:
            case Const.LASTORE:
            case Const.FASTORE:
            case Const.DASTORE:
            case Const.AASTORE:
            case Const.BASTORE:
            case Const.CASTORE:
            case Const.SASTORE:
            case Const.POP:
            case Const.POP2:
            case Const.DUP:
            case Const.DUP_X1:
            case Const.DUP_X2:
            case Const.DUP2:
            case Const.DUP2_X1:
            case Const.DUP2_X2:
            case Const.SWAP:
            case Const.IADD:
            case Const.LADD:
            case Const.FADD:
            case Const.DADD:
            case Const.ISUB:
            case Const.LSUB:
            case Const.FSUB:
            case Const.DSUB:
            case Const.IMUL:
            case Const.LMUL:
            case Const.FMUL:
            case Const.DMUL:
            case Const.IDIV:
            case Const.LDIV:
            case Const.FDIV:
            case Const.DDIV:
            case Const.IREM:
            case Const.LREM:
            case Const.FREM:
            case Const.DREM:
            case Const.INEG:
            case Const.LNEG:
            case Const.FNEG:
            case Const.DNEG:
            case Const.ISHL:
            case Const.LSHL:
            case Const.ISHR:
            case Const.LSHR:
            case Const.IUSHR:
            case Const.LUSHR:
            case Const.IAND:
            case Const.LAND:
            case Const.IOR:
            case Const.LOR:
            case Const.IXOR:
            case Const.LXOR:
            case Const.I2L:
            case Const.I2F:
            case Const.I2D:
            case Const.L2I:
            case Const.L2F:
            case Const.L2D:
            case Const.F2I:
            case Const.F2L:
            case Const.F2D:
            case Const.D2I:
            case Const.D2L:
            case Const.D2F:
            case Const.I2B:
            case Const.I2C:
            case Const.I2S:
            case Const.LCMP:
            case Const.FCMPL:
            case Const.FCMPG:
            case Const.DCMPL:
            case Const.DCMPG:
            case Const.IRETURN:
            case Const.LRETURN:
            case Const.FRETURN:
            case Const.DRETURN:
            case Const.ARETURN:
            case Const.RETURN:
            case Const.ARRAYLENGTH:
            case Const.ATHROW:
            case Const.MONITORENTER:
            case Const.MONITOREXIT:
                ++index;
                break;

            // Two byte instructions.
            case Const.BIPUSH:
            case Const.LDC:
            case Const.NEWARRAY:
                index += 2;
                break;

            // Instructions that can be used with the WIDE prefix.
            case Const.ILOAD:
            case Const.LLOAD:
            case Const.FLOAD:
            case Const.DLOAD:
            case Const.ALOAD:
            case Const.ISTORE:
            case Const.LSTORE:
            case Const.FSTORE:
            case Const.DSTORE:
            case Const.ASTORE:
            case Const.RET:
                if (wide) {
                    // Skip opcode and two immediate bytes.
                    index += 3;
                    wide = false;
                } else {
                    // Skip opcode and one immediate byte.
                    index += 2;
                }
                break;

            // IINC is a special case Const.for WIDE handling
            case Const.IINC:
                if (wide) {
                    // Skip opcode, two byte index, and two byte immediate
                    // value.
                    index += 5;
                    wide = false;
                } else {
                    // Skip opcode, one byte index, and one byte immedate value.
                    index += 3;
                }
                break;

            // Three byte instructions.
            case Const.SIPUSH:
            case Const.LDC_W:
            case Const.LDC2_W:
            case Const.IFEQ:
            case Const.IFNE:
            case Const.IFLT:
            case Const.IFGE:
            case Const.IFGT:
            case Const.IFLE:
            case Const.IF_ICMPEQ:
            case Const.IF_ICMPNE:
            case Const.IF_ICMPLT:
            case Const.IF_ICMPGE:
            case Const.IF_ICMPGT:
            case Const.IF_ICMPLE:
            case Const.IF_ACMPEQ:
            case Const.IF_ACMPNE:
            case Const.GOTO:
            case Const.JSR:
            case Const.GETSTATIC:
            case Const.PUTSTATIC:
            case Const.GETFIELD:
            case Const.PUTFIELD:
            case Const.INVOKEVIRTUAL:
            case Const.INVOKESPECIAL:
            case Const.INVOKESTATIC:
            case Const.NEW:
            case Const.ANEWARRAY:
            case Const.CHECKCAST:
            case Const.INSTANCEOF:
            case Const.IFNULL:
            case Const.IFNONNULL:
                index += 3;
                break;

            // Four byte instructions.
            case Const.MULTIANEWARRAY:
                index += 4;
                break;

            // Five byte instructions.
            case Const.INVOKEINTERFACE:
            case Const.INVOKEDYNAMIC:
            case Const.GOTO_W:
            case Const.JSR_W:
                index += 5;
                break;

            // TABLESWITCH - variable length.
            case Const.TABLESWITCH: {
                // Skip padding.
                int offset = index + 1; // skip the opcode
                offset += PAD[offset & 3];
                assert (offset & 3) == 0;

                // offset should now be posited at the default value

                // Extract min and max values.
                int low = extractInt(instructionList, offset + 4);
                int high = extractInt(instructionList, offset + 8);
                int tableSize = (high - low) + 1;
                if (DEBUG) {
                    System.out.println("tableswitch: low=" + low + ", high=" + high + ", tableSize=" + tableSize);
                }

                // Skip to next instruction.
                index = offset + 12 + (tableSize * 4);
            }
                break;

            // LOOKUPSWITCH - variable length.
            case Const.LOOKUPSWITCH: {
                // Skip padding.
                int offset = index + 1; // skip the opcode
                offset += PAD[offset & 3];
                assert (offset & 3) == 0;

                // offset should now be posited at the default value

                // Extract number of value/offset pairs.
                int numPairs = extractInt(instructionList, offset + 4);
                if (DEBUG) {
                    System.out.println("lookupswitch: numPairs=" + numPairs);
                }

                // Skip to next instruction.
                index = offset + 8 + (numPairs * 8);
            }
                break;

            // Wide prefix.
            case Const.WIDE:
                wide = true;
                ++index;
                break;

            default:
                throw new IllegalArgumentException("Bad opcode " + opcode + " at offset " + index);
            }

            if (index < 0) {
                throw new IllegalStateException("index=" + index + ", opcode=" + opcode);
            }

        }
    }
}
