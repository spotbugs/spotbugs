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

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

import org.apache.bcel.classfile.*;

abstract public class DismantleBytecode extends PreorderVisitor implements Constants2 {

	private int opcode;
	private boolean opcodeIsWide;
	private int PC;
	private int branchOffset;
	private int branchTarget;
	private int branchFallThrough;
	private int[] switchOffsets;
	private int[] switchLabels;
	private int defaultSwitchOffset;
	private String classConstantOperand;
	private String dottedClassConstantOperand;
	private String nameConstantOperand;
	private String sigConstantOperand;
	private String dottedSigConstantOperand;
	private String stringConstantOperand;
	private String refConstantOperand;
	private boolean refFieldIsStatic;
	private Constant constantRefOperand;
	private int intConstant;
	private long longConstant;
	private float floatConstant;
	private double doubleConstant;
	private int registerOperand;

	private static final int INVALID_OFFSET = Integer.MIN_VALUE;
	private static final String NOT_AVAILABLE = "none";

	protected static final int R_INT = 0;
	protected static final int R_LONG = 1;
	protected static final int R_FLOAT = 2;
	protected static final int R_DOUBLE = 3;
	protected static final int R_REF = 4;
	protected int registerKind;

	private static HashMap<String, String> replaceSlashesWithDotsCache = new HashMap<String, String>();

	synchronized static String replaceSlashesWithDots(String c) {
		String result = replaceSlashesWithDotsCache.get(c);
		if (result != null) return result;
		result = c.replace('/', '.');
		replaceSlashesWithDotsCache.put(c, result);
		return result;
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

	// REVIEW brian@quiotix.com -- Either this array should be byte[][], or the constants above should be short
	static final short[][] MEANING_OF_OPERANDS = {
		// 0   1   2   3   4   5   6   7   8   9
		{}, {}, {}, {}, {}, {}, {}, {}, {}, {},
		{}, {}, {}, {}, {}, {}, {M_INT}, {M_INT}, {M_CP}, {M_CP},
		{M_CP}, {M_R}, {M_R}, {M_R}, {M_R}, {M_R}, {}, {}, {}, {},
		{}, {}, {}, {}, {}, {}, {}, {}, {}, {},
		{}, {}, {}, {}, {}, {}, {}, {}, {}, {},
		{}, {}, {}, {}, {M_R}, {M_R}, {M_R}, {M_R}, {M_R}, {},
		{}, {}, {}, {}, {}, {}, {}, {}, {}, {},
		{}, {}, {}, {}, {}, {}, {}, {}, {}, {},
		{}, {}, {}, {}, {}, {}, {}, {}, {}, {},
		{}, {}, {}, {}, {}, {}, {}, {}, {}, {},
		{}, {}, {}, {}, {}, {}, {}, {}, {}, {},
		{}, {}, {}, {}, {}, {}, {}, {}, {}, {},
		{}, {}, {}, {}, {}, {}, {}, {}, {}, {},
// 130   1   2   3   4   5   6   7   8   9
		{}, {}, {M_R, M_INT}, {}, {}, {}, {}, {}, {}, {},
		{}, {}, {}, {}, {}, {}, {}, {}, {}, {},
		{}, {}, {}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR},
		{M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_R},
// 170   1   2   3   4   5   6   7   8   9
		{}, {}, {}, {}, {}, {}, {}, {}, {M_CP}, {M_CP},
		{M_CP}, {M_CP}, {M_CP}, {M_CP}, {M_CP},
		{M_CP, M_PAD, M_PAD}, {}, {M_CP}, {M_UINT}, {M_CP},
// 190   1       2       3   4   5       6                7       8       9
		{}, {}, {M_CP}, {M_CP}, {}, {}, {M_PAD}, {M_CP, M_UINT}, {M_BR}, {M_BR},
		{M_BR}, {M_BR}, {}, {}, {}, {}, {},
		{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
		{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
		{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
	};


	protected byte[] codeBytes;
	protected LineNumberTable lineNumberTable;

	// Accessors

	/** If the current opcode has a class operand, get the associated class constant, dot-formatted */
	public String getDottedClassConstantOperand() {
		if (dottedClassConstantOperand == NOT_AVAILABLE)
			throw new IllegalStateException("getDottedClassConstantOperand called but value not available");
		return dottedClassConstantOperand;
	}

	/** If the current opcode has a reference constant operand, get its string representation */
	public String getRefConstantOperand() {
		if (refConstantOperand == NOT_AVAILABLE)
			throw new IllegalStateException("getRefConstantOperand called but value not available");
		return refConstantOperand;
	}

	/** If the current opcode has a reference constant operand, get its name */
	public String getNameConstantOperand() {
		if (nameConstantOperand == NOT_AVAILABLE)
			throw new IllegalStateException("getNameConstantOperand called but value not available");
		return nameConstantOperand;
	}

	/** If the current opcode has a reference constant operand, get its signature, dot-formatted */
	public String getDottedSigConstantOperand() {
		if (dottedSigConstantOperand == NOT_AVAILABLE)
			throw new IllegalStateException("getDottedSigConstantOperand called but value not available");
		return dottedSigConstantOperand;
	}

	/** If the current opcode has a reference constant operand, get its signature, slash-formatted */
	public String getSigConstantOperand() {
		if (sigConstantOperand == NOT_AVAILABLE)
			throw new IllegalStateException("getSigConstantOperand called but value not available");
		return sigConstantOperand;
	}

	public String getClassConstantOperand() {
		if (classConstantOperand == NOT_AVAILABLE)
			throw new IllegalStateException("getClassConstantOperand called but value not available");
		return classConstantOperand;
	}

	/** If the current opcode has a string constant operand, get its name */
	public String getStringConstantOperand() {
		if (stringConstantOperand == NOT_AVAILABLE)
			throw new IllegalStateException("getStringConstantOperand called but value not available");
		return stringConstantOperand;
	}

	public Constant getConstantRefOperand() {
		if (constantRefOperand == null)
			throw new IllegalStateException("getConstantRefOperand called but value not available");
		return constantRefOperand;
	}

	public int getRegisterOperand() {
		return registerOperand;
	}

	public int getIntConstant() {
		return intConstant;
	}

	public int getBranchOffset() {
		if (branchOffset == INVALID_OFFSET)
			throw new IllegalStateException("getBranchOffset called but value not available");
		return branchOffset;
	}

	public int getBranchTarget() {
		if (branchTarget == INVALID_OFFSET)
			throw new IllegalStateException("getBranchTarget called but value not available");
		return branchTarget;
	}

	public int getBranchFallThrough() {
		if (branchFallThrough == INVALID_OFFSET)
			throw new IllegalStateException("getBranchFallThrough called but value not available");
		return branchFallThrough;
	}

	public int getDefaultSwitchOffset() {
		if (defaultSwitchOffset == INVALID_OFFSET)
			throw new IllegalStateException("getDefaultSwitchOffset called but value not available");
		return defaultSwitchOffset;
	}

	public boolean getRefFieldIsStatic() {
		return refFieldIsStatic;
	}

	public int getPC() {
		return PC;
	}

	@SuppressWarnings("EI")
	public int[] getSwitchOffsets() {
		if (switchOffsets == null)
			throw new IllegalStateException("getSwitchOffsets called but value not available");
		return switchOffsets;
	}

	@SuppressWarnings("EI")
	public int[] getSwitchLabels() {
		if (switchLabels == null)
			throw new IllegalStateException("getSwitchOffsets called but value not available");
		return switchLabels;
	}

	private void resetState() {
		dottedClassConstantOperand = classConstantOperand = nameConstantOperand = sigConstantOperand = dottedSigConstantOperand = stringConstantOperand = refConstantOperand = NOT_AVAILABLE;
		refFieldIsStatic = false;
		constantRefOperand = null;
		branchOffset = branchTarget = branchFallThrough = defaultSwitchOffset = INVALID_OFFSET;
		switchOffsets = switchLabels = null;
	}

	private static void sortByOffset(int[] switchOffsets, int[] switchLabels) {
		int npairs = switchOffsets.length;
		// Sort by offset
		for (int j = 0; j < npairs; j++) {
			int min = j;
			for (int k = j + 1; k < npairs; k++)
				if (switchOffsets[min] > switchOffsets[k])
					min = k;
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

	public void visit(Code obj) {

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
				i++;
				// System.out.println(OPCODE_NAMES[opCode]);
				int byteStreamArgCount = NO_OF_OPERANDS[opcode];
				if (byteStreamArgCount == UNPREDICTABLE) {

					if (opcode == LOOKUPSWITCH) {
						int pad = 4 - (i & 3);
						if (pad == 4) pad = 0;
						byteStream.skipBytes(pad);
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
						;
						sortByOffset(switchOffsets, switchLabels);
					} else if (opcode == TABLESWITCH) {
						int pad = 4 - (i & 3);
						if (pad == 4) pad = 0;
						byteStream.skipBytes(pad);
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
						;
						sortByOffset(switchOffsets, switchLabels);
					} else if (opcode == WIDE) {
						opcodeIsWide = true;
						opcode = byteStream.readUnsignedByte();
						i++;
						switch (opcode) {
						case ILOAD:
						case FLOAD:
						case ALOAD:
						case LLOAD:
						case DLOAD:
						case ISTORE:
						case FSTORE:
						case ASTORE:
						case LSTORE:
						case DSTORE:
						case RET:
							registerOperand = byteStream.readUnsignedShort();
							i += 2;
							break;
						case IINC:
							registerOperand = byteStream.readUnsignedShort();
							i += 2;
							intConstant = byteStream.readShort();
							i += 2;
							break;
						default:
							throw new IllegalStateException("bad wide bytecode: " + OPCODE_NAMES[opcode]);
						}
					} else
						throw new IllegalStateException("bad unpredicatable bytecode: " + OPCODE_NAMES[opcode]);
				} else {
					if (byteStreamArgCount < 0) throw new IllegalStateException("bad length for bytecode: " + OPCODE_NAMES[opcode]);
					for (int k = 0; k < TYPE_OF_OPERANDS[opcode].length; k++) {

						int v;
						int t = TYPE_OF_OPERANDS[opcode][k];
						int m = MEANING_OF_OPERANDS[opcode][k];
						boolean unsigned = (m == M_CP || m == M_R || m == M_UINT);
						switch (t) {
						case T_BYTE:
							if (unsigned)
								v = byteStream.readUnsignedByte();
							else
								v = byteStream.readByte();
							/*
							System.out.print("Read byte " + v);
							System.out.println(" with meaning" + m);
							*/
							i++;
							break;
						case T_SHORT:
							if (unsigned)
								v = byteStream.readUnsignedShort();
							else
								v = byteStream.readShort();
							i += 2;
							break;
						case T_INT:
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
								classConstantOperand = getStringFromIndex(clazz.getNameIndex()).intern();
								dottedClassConstantOperand = replaceSlashesWithDots(classConstantOperand);
							}
							if (constantRefOperand instanceof ConstantInteger)
								intConstant = ((ConstantInteger) constantRefOperand).getBytes();
							else if (constantRefOperand instanceof ConstantLong)
								longConstant = ((ConstantLong) constantRefOperand).getBytes();
							else if (constantRefOperand instanceof ConstantFloat)
								floatConstant = ((ConstantFloat) constantRefOperand).getBytes();
							else if (constantRefOperand instanceof ConstantDouble)
								doubleConstant = ((ConstantDouble) constantRefOperand).getBytes();
							else if (constantRefOperand instanceof ConstantString) {
								int s = ((ConstantString) constantRefOperand).getStringIndex();

								stringConstantOperand = getStringFromIndex(s);
							} else if (constantRefOperand instanceof ConstantCP) {
								ConstantCP cp = (ConstantCP) constantRefOperand;
								ConstantClass clazz
								        = (ConstantClass) getConstantPool().getConstant(cp.getClassIndex());
								classConstantOperand = getStringFromIndex(clazz.getNameIndex()).intern();
								dottedClassConstantOperand = replaceSlashesWithDots(classConstantOperand);
								ConstantNameAndType sig
								        = (ConstantNameAndType) getConstantPool().getConstant(cp.getNameAndTypeIndex());
								nameConstantOperand = getStringFromIndex(sig.getNameIndex());
								sigConstantOperand = getStringFromIndex(sig.getSignatureIndex()).intern();
								dottedSigConstantOperand = replaceSlashesWithDots(sigConstantOperand);
								StringBuffer ref = new StringBuffer(5 + dottedClassConstantOperand.length()
								        + nameConstantOperand.length()
								        + dottedSigConstantOperand.length());

								ref.append(dottedClassConstantOperand)
								        .append(".")
								        .append(nameConstantOperand)
								        .append(" : ")
								        .append(dottedSigConstantOperand);
								refConstantOperand = ref.toString();
							}
							break;
						case M_R:
							registerOperand = v;
							break;
						case M_UINT:
						case M_INT:
							intConstant = v;
						}
					}

				}
				switch (opcode) {
				case ILOAD:
				case FLOAD:
				case ALOAD:
				case LLOAD:
				case DLOAD:
					registerKind = opcode - ILOAD;
					break;
				case ISTORE:
				case FSTORE:
				case ASTORE:
				case LSTORE:
				case DSTORE:
					registerKind = opcode - ISTORE;
					break;
				case RET:
					registerKind = R_REF;
					break;
				case GETSTATIC:
				case PUTSTATIC:
					refFieldIsStatic = true;
					break;
				case GETFIELD:
				case PUTFIELD:
					refFieldIsStatic = false;
					break;
				}
				sawOpcode(opcode);

				if (opcode == TABLESWITCH) {
					sawInt(switchLow);
					sawInt(switchHigh);
					int prevOffset = i - PC;
					for (int o = 0; o <= switchHigh - switchLow; o++) {
						sawOffset(switchOffsets[o] - prevOffset);
						prevOffset = switchOffsets[o];
					}
					sawOffset(defaultSwitchOffset - prevOffset);
				} else if (opcode == LOOKUPSWITCH) {
					sawInt(switchOffsets.length);
					int prevOffset = i - PC;
					for (int o = 0; o < switchOffsets.length; o++) {
						sawOffset(switchOffsets[o] - prevOffset);
						prevOffset = switchOffsets[o];
						sawInt(switchLabels[o]);
					}
					sawOffset(defaultSwitchOffset - prevOffset);
				} else
					for (int k = 0; k < TYPE_OF_OPERANDS[opcode].length; k++) {
						int m = MEANING_OF_OPERANDS[opcode][k];
						switch (m) {
						case M_BR:
							if (branchOffset > 0)
								sawOffset(branchOffset - (i - PC));
							else
								sawOffset(branchOffset);
							break;
						case M_CP:
							if (constantRefOperand instanceof ConstantInteger)
								sawInt(intConstant);
							else if (constantRefOperand instanceof ConstantLong)
								sawLong(longConstant);
							else if (constantRefOperand instanceof ConstantFloat)
								sawFloat(floatConstant);
							else if (constantRefOperand instanceof ConstantDouble)
								sawDouble(doubleConstant);
							else if (constantRefOperand instanceof ConstantString)
								sawString(stringConstantOperand);
							else if (constantRefOperand instanceof ConstantFieldref)
								sawField();
							else if (constantRefOperand instanceof ConstantMethodref)
								sawMethod();
							else if (constantRefOperand instanceof ConstantInterfaceMethodref)
								sawIMethod();
							else if (constantRefOperand instanceof ConstantClass)
								sawClass();
							break;
						case M_R:
							sawRegister(registerOperand);
							break;
						case M_INT:
							sawInt(intConstant);
							break;
						}
					}
			}
		} catch (IOException e) {
			System.out.println("Got IO Exception:");
			e.printStackTrace();
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

	public void sawOffset(int seen) {
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
}
