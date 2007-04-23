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

import edu.umd.cs.findbugs.SystemProperties;

/**
 * Scan the raw bytecodes of a method.
 * This is useful in order to find out quickly whether or not
 * a method uses particular instructions.
 *
 * @author David Hovemeyer
 */
public class BytecodeScanner implements org.apache.bcel.Constants {
	private static final boolean DEBUG = SystemProperties.getBoolean("bs.debug");

	/**
	 * Callback interface to report scanned instructions.
	 */
	public interface Callback {
		/**
		 * Called to indicate that a particular bytecode has been scanned.
		 *
		 * @param opcode the opcode of the instruction
		 * @param index  the bytecode offset of the instruction
		 */
		public void handleInstruction(int opcode, int index);
	}

	/**
	 * Convert the unsigned value of a byte into a short.
	 *
	 * @param value the byte
	 * @return the byte's unsigned value as a short
	 */
	private static short unsignedValueOf(byte value) {
		short result;
		if ((value & 0x80) != 0) {
			result = (short) (value & 0x7F);
			result |= 0x80;
		} else {
			result = (short) value;
		}
		return result;
	}

	/**
	 * Extract an int from bytes at the given offset in the array.
	 *
	 * @param arr    the array
	 * @param offset the offset in the array
	 */
	private static int extractInt(byte[] arr, int offset) {
		return ((arr[offset] & 0xFF) << 24) |
				((arr[offset + 1] & 0xFF) << 16) |
				((arr[offset + 2] & 0xFF) << 8) |
				(arr[offset + 3] & 0xFF);
	}

	private static final int PAD[] = {0, 3, 2, 1};

	/**
	 * Scan the raw bytecodes of a method.
	 *
	 * @param instructionList the bytecodes
	 * @param callback        the callback object
	 */
	public void scan(byte[] instructionList, Callback callback) {

		boolean wide = false;

		for (int index = 0; index < instructionList.length;) {
			short opcode = unsignedValueOf(instructionList[index]);
			callback.handleInstruction(opcode, index);

			if (DEBUG) System.out.println(index + ": " + OPCODE_NAMES[opcode]);

			switch (opcode) {

			// Single byte instructions.
			case NOP:
			case ACONST_NULL:
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
			case LCONST_0:
			case LCONST_1:
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
			case DCONST_0:
			case DCONST_1:
			case ILOAD_0:
			case ILOAD_1:
			case ILOAD_2:
			case ILOAD_3:
			case LLOAD_0:
			case LLOAD_1:
			case LLOAD_2:
			case LLOAD_3:
			case FLOAD_0:
			case FLOAD_1:
			case FLOAD_2:
			case FLOAD_3:
			case DLOAD_0:
			case DLOAD_1:
			case DLOAD_2:
			case DLOAD_3:
			case ALOAD_0:
			case ALOAD_1:
			case ALOAD_2:
			case ALOAD_3:
			case IALOAD:
			case LALOAD:
			case FALOAD:
			case DALOAD:
			case AALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
			case ISTORE_0:
			case ISTORE_1:
			case ISTORE_2:
			case ISTORE_3:
			case LSTORE_0:
			case LSTORE_1:
			case LSTORE_2:
			case LSTORE_3:
			case FSTORE_0:
			case FSTORE_1:
			case FSTORE_2:
			case FSTORE_3:
			case DSTORE_0:
			case DSTORE_1:
			case DSTORE_2:
			case DSTORE_3:
			case ASTORE_0:
			case ASTORE_1:
			case ASTORE_2:
			case ASTORE_3:
			case IASTORE:
			case LASTORE:
			case FASTORE:
			case DASTORE:
			case AASTORE:
			case BASTORE:
			case CASTORE:
			case SASTORE:
			case POP:
			case POP2:
			case DUP:
			case DUP_X1:
			case DUP_X2:
			case DUP2:
			case DUP2_X1:
			case DUP2_X2:
			case SWAP:
			case IADD:
			case LADD:
			case FADD:
			case DADD:
			case ISUB:
			case LSUB:
			case FSUB:
			case DSUB:
			case IMUL:
			case LMUL:
			case FMUL:
			case DMUL:
			case IDIV:
			case LDIV:
			case FDIV:
			case DDIV:
			case IREM:
			case LREM:
			case FREM:
			case DREM:
			case INEG:
			case LNEG:
			case FNEG:
			case DNEG:
			case ISHL:
			case LSHL:
			case ISHR:
			case LSHR:
			case IUSHR:
			case LUSHR:
			case IAND:
			case LAND:
			case IOR:
			case LOR:
			case IXOR:
			case LXOR:
			case I2L:
			case I2F:
			case I2D:
			case L2I:
			case L2F:
			case L2D:
			case F2I:
			case F2L:
			case F2D:
			case D2I:
			case D2L:
			case D2F:
			case I2B:
			case I2C:
			case I2S:
			case LCMP:
			case FCMPL:
			case FCMPG:
			case DCMPL:
			case DCMPG:
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
			case ARRAYLENGTH:
			case ATHROW:
			case MONITORENTER:
			case MONITOREXIT:
				++index;
				break;

				// Two byte instructions.
			case BIPUSH:
			case LDC:
			case NEWARRAY:
				index += 2;
				break;

				// Instructions that can be used with the WIDE prefix.
			case ILOAD:
			case LLOAD:
			case FLOAD:
			case DLOAD:
			case ALOAD:
			case ISTORE:
			case LSTORE:
			case FSTORE:
			case DSTORE:
			case ASTORE:
			case RET:
				if (wide) {
					// Skip opcode and two immediate bytes.
					index += 3;
					wide = false;
				} else {
					// Skip opcode and one immediate byte.
					index += 2;
				}
				break;

				// IINC is a special case for WIDE handling
			case IINC:
				if (wide) {
					// Skip opcode, two byte index, and two byte immediate value.
					index += 5;
					wide = false;
				} else {
					// Skip opcode, one byte index, and one byte immedate value.
					index += 3;
				}
				break;

				// Three byte instructions.
			case SIPUSH:
			case LDC_W:
			case LDC2_W:
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE:
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
			case IF_ACMPEQ:
			case IF_ACMPNE:
			case GOTO:
			case JSR:
			case GETSTATIC:
			case PUTSTATIC:
			case GETFIELD:
			case PUTFIELD:
			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
			case NEW:
			case ANEWARRAY:
			case CHECKCAST:
			case INSTANCEOF:
			case IFNULL:
			case IFNONNULL:
				index += 3;
				break;

				// Four byte instructions.
			case MULTIANEWARRAY:
				index += 4;
				break;

				// Five byte instructions.
			case INVOKEINTERFACE:
			case GOTO_W:
			case JSR_W:
				index += 5;
				break;

				// TABLESWITCH - variable length.
			case TABLESWITCH:
				{
					// Skip padding.
					int offset = index + 1; // skip the opcode
					offset += PAD[offset & 3];
					assert (offset & 3) == 0;

					// offset should now be posited at the default value

					// Extract min and max values.
					int low = extractInt(instructionList, offset + 4);
					int high = extractInt(instructionList, offset + 8);
					int tableSize = (high - low) + 1;
					if (DEBUG) System.out.println("tableswitch: low=" + low + ", high=" + high + ", tableSize=" + tableSize);

					// Skip to next instruction.
					index = offset + 12 + (tableSize * 4);
				}
				break;

				// LOOKUPSWITCH - variable length.
			case LOOKUPSWITCH:
				{
					// Skip padding.
					int offset = index + 1; // skip the opcode
					offset += PAD[offset & 3];
					assert (offset & 3) == 0;

					// offset should now be posited at the default value

					// Extract number of value/offset pairs.
					int numPairs = extractInt(instructionList, offset + 4);
					if (DEBUG) System.out.println("lookupswitch: numPairs=" + numPairs);

					// Skip to next instruction.
					index = offset + 8 + (numPairs * 8);
				}
				break;

				// Wide prefix.
			case WIDE:
				wide = true;
				++index;
				break;

			default:
				throw new IllegalArgumentException("Bad opcode " + opcode + " at offset " + index);
			}

			if (index < 0)
				throw new IllegalStateException("index=" + index + ", opcode=" + opcode);

		}
	}
}

// vim:ts=4
