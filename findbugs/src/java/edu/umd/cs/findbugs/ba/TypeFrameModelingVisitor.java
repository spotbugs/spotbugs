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

import org.apache.bcel.Constants;
import org.apache.bcel.generic.*;

/**
 * Visitor to model the effects of bytecode instructions on the
 * types of the values (local and operand stack) in Java stack frames.
 * This visitor does not verify that the types are sensible
 * for the bytecodes executed.  In other words, this isn't a bytecode
 * verifier, although it wouldn't be too hard to turn it into
 * something vaguely verifier-like.
 *
 * @author David Hovemeyer
 * @see TypeFrame
 * @see TypeAnalysis
 */
public class TypeFrameModelingVisitor extends AbstractFrameModelingVisitor<Type, TypeFrame>
        implements Constants, Debug {

	/**
	 * Constructor.
	 *
	 * @param cpg the ConstantPoolGen of the method whose instructions we are examining
	 */
	public TypeFrameModelingVisitor(ConstantPoolGen cpg) {
		super(cpg);
	}

	public Type getDefaultValue() {
		return TypeFrame.getBottomType();
	}

	/**
	 * Consume stack.  This is a convenience method for instructions
	 * where the types of popped operands can be ignored.
	 */
	protected void consumeStack(Instruction ins) {
		ConstantPoolGen cpg = getCPG();
		TypeFrame frame = getFrame();

		int numWordsConsumed = ins.consumeStack(cpg);
		if (numWordsConsumed == Constants.UNPREDICTABLE)
			throw new IllegalStateException("Unpredictable stack consumption for " + ins);
		try {
			while (numWordsConsumed-- > 0) {
				frame.popValue();
			}
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException("Stack underflow for " + ins + ": " + e.getMessage());
		}
	}

	/**
	 * Work around some weirdness in BCEL (inherited from JVM Spec 1):
	 * BCEL considers long and double types to consume two slots on the
	 * stack.  This method ensures that we push two types for
	 * each double or long value.
	 */
	protected void pushValue(Type type) {
		TypeFrame frame = getFrame();
		if (type.getType() == T_LONG) {
			frame.pushValue(Type.LONG);
			frame.pushValue(TypeFrame.getLongExtraType());
		} else if (type.getType() == T_DOUBLE) {
			frame.pushValue(Type.DOUBLE);
			frame.pushValue(TypeFrame.getDoubleExtraType());
		} else
			frame.pushValue(type);
	}

	/**
	 * Helper for pushing the return type of an invoke instruction.
	 */
	protected void pushReturnType(InvokeInstruction ins) {
		ConstantPoolGen cpg = getCPG();
		Type type = ins.getType(cpg);
		if (type.getType() != T_VOID)
			pushValue(type);
	}

	/**
	 * This is overridden only to ensure that we don't rely on the
	 * base class to handle instructions that produce stack operands.
	 */
	public void modelNormalInstruction(Instruction ins, int numWordsConsumed, int numWordsProduced) {
		if (VERIFY_INTEGRITY) {
			if (numWordsProduced > 0)
				throw new IllegalStateException("missing visitor method for " + ins);
		}
		super.modelNormalInstruction(ins, numWordsConsumed, numWordsProduced);
	}

	// ----------------------------------------------------------------------
	// Instruction visitor methods
	// ----------------------------------------------------------------------

	// NOTES:
	// - Instructions that only consume operands need not be overridden,
	//   because the base class visit methods handle them correctly.
	// - Instructions that simply move values around in the frame,
	//   such as DUP, xLOAD, etc., do not need to be overridden because
	//   the base class handles them.
	// - Instructions that consume and produce should call
	//   consumeStack(Instruction) and then explicitly push produced operands.

	public void visitACONST_NULL(ACONST_NULL obj) {
		pushValue(TypeFrame.getNullType());
	}

	public void visitDCONST(DCONST obj) {
		pushValue(Type.DOUBLE);
	}

	public void visitFCONST(FCONST obj) {
		pushValue(Type.FLOAT);
	}

	public void visitICONST(ICONST obj) {
		pushValue(Type.INT);
	}

	public void visitLCONST(LCONST obj) {
		pushValue(Type.LONG);
	}

	public void visitLDC(LDC obj) {
		pushValue(obj.getType(getCPG()));
	}

	public void visitLDC2_W(LDC2_W obj) {
		pushValue(obj.getType(getCPG()));
	}

	public void visitBIPUSH(BIPUSH obj) {
		pushValue(Type.INT);
	}

	public void visitSIPUSH(SIPUSH obj) {
		pushValue(Type.INT);
	}

	public void visitGETSTATIC(GETSTATIC obj) {
		consumeStack(obj);
		pushValue(obj.getType(getCPG()));
	}

	public void visitGETFIELD(GETFIELD obj) {
		consumeStack(obj);
		pushValue(obj.getType(getCPG()));
	}

	public void visitINVOKESTATIC(INVOKESTATIC obj) {
		consumeStack(obj);
		pushReturnType(obj);
	}

	public void visitINVOKESPECIAL(INVOKESPECIAL obj) {
		consumeStack(obj);
		pushReturnType(obj);
	}

	public void visitINVOKEINTERFACE(INVOKEINTERFACE obj) {
		consumeStack(obj);
		pushReturnType(obj);
	}

	public void visitINVOKEVIRTUAL(INVOKEVIRTUAL obj) {
		consumeStack(obj);
		pushReturnType(obj);
	}

	public void visitCHECKCAST(CHECKCAST obj) {
		consumeStack(obj);
		pushValue(obj.getType(getCPG()));
	}

	public void visitINSTANCEOF(INSTANCEOF obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitFCMPL(FCMPL obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitFCMPG(FCMPG obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitDCMPL(DCMPL obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitDCMPG(DCMPG obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitLCMP(LCMP obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitD2F(D2F obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	public void visitD2I(D2I obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitD2L(D2L obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	public void visitF2D(F2D obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	public void visitF2I(F2I obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitF2L(F2L obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	public void visitI2B(I2B obj) {
		consumeStack(obj);
		pushValue(Type.BYTE);
	}

	public void visitI2C(I2C obj) {
		consumeStack(obj);
		pushValue(Type.CHAR);
	}

	public void visitI2D(I2D obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	public void visitI2F(I2F obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	public void visitI2L(I2L obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	public void visitI2S(I2S obj) {
	} // no change

	public void visitL2D(L2D obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	public void visitL2F(L2F obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	public void visitL2I(L2I obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitIAND(IAND obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitLAND(LAND obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	public void visitIOR(IOR obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitLOR(LOR obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	public void visitIXOR(IXOR obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitLXOR(LXOR obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	public void visitISHR(ISHR obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitIUSHR(IUSHR obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitLSHR(LSHR obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	public void visitLUSHR(LUSHR obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	public void visitISHL(ISHL obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitLSHL(LSHL obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	public void visitDADD(DADD obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	public void visitFADD(FADD obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	public void visitIADD(IADD obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitLADD(LADD obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	public void visitDSUB(DSUB obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	public void visitFSUB(FSUB obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	public void visitISUB(ISUB obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitLSUB(LSUB obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	public void visitDMUL(DMUL obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	public void visitFMUL(FMUL obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	public void visitIMUL(IMUL obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitLMUL(LMUL obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	public void visitDDIV(DDIV obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	public void visitFDIV(FDIV obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	public void visitIDIV(IDIV obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitLDIV(LDIV obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	public void visitDREM(DREM obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	public void visitFREM(FREM obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	public void visitIREM(IREM obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitLREM(LREM obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	public void visitIINC(IINC obj) {
	} // no change to types of stack or locals

	public void visitDNEG(DNEG obj) {
	} // no change

	public void visitFNEG(FNEG obj) {
	} // no change

	public void visitINEG(INEG obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitLNEG(LNEG obj) {
	} // no change

	public void visitARRAYLENGTH(ARRAYLENGTH obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitAALOAD(AALOAD obj) {
		// To determine the type pushed on the stack,
		// we look at the type of the array reference which was
		// popped off of the stack.
		TypeFrame frame = getFrame();
		try {
			frame.popValue(); // index
			Type arrayType = frame.popValue(); // arrayref
			if (arrayType instanceof ArrayType) {
				ArrayType arr = (ArrayType) arrayType;
				pushValue(arr.getElementType());
			} else {
				pushValue(TypeFrame.getBottomType());
			}
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException("Stack underflow: " + e.getMessage());
		}
	}

	public void visitBALOAD(BALOAD obj) {
		consumeStack(obj);
		pushValue(Type.BYTE);
	}

	public void visitCALOAD(CALOAD obj) {
		consumeStack(obj);
		pushValue(Type.CHAR);
	}

	public void visitDALOAD(DALOAD obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	public void visitFALOAD(FALOAD obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	public void visitIALOAD(IALOAD obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	public void visitLALOAD(LALOAD obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	public void visitSALOAD(SALOAD obj) {
		consumeStack(obj);
		pushValue(Type.SHORT);
	}

	// The various xASTORE instructions only consume stack.

	public void visitNEW(NEW obj) {
		// FIXME: type is technically "uninitialized"
		// However, we don't model that yet.
		pushValue(obj.getType(getCPG()));
	}

	public void visitNEWARRAY(NEWARRAY obj) {
		consumeStack(obj);
		Type elementType = obj.getType();
		pushValue(new ArrayType(elementType, 1));
	}

	public void visitANEWARRAY(ANEWARRAY obj) {
		consumeStack(obj);
		Type elementType = obj.getType(getCPG());
		pushValue(new ArrayType(elementType, 1));
	}

	public void visitMULTIANEWARRAY(MULTIANEWARRAY obj) {
		consumeStack(obj);
		Type elementType = obj.getType(getCPG());
		pushValue(new ArrayType(elementType, obj.getDimensions()));
	}

	public void visitJSR(JSR obj) {
		pushValue(ReturnaddressType.NO_TARGET);
	}

	public void visitJSR_W(JSR_W obj) {
		pushValue(ReturnaddressType.NO_TARGET);
	}

	public void visitRET(RET obj) {
	} // no change

}

// vim:ts=4
