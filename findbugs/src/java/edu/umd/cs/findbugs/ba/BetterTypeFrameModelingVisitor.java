/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
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

import edu.umd.cs.findbugs.ba.type.ArrayType;
import edu.umd.cs.findbugs.ba.type.InvalidSignatureException;
import edu.umd.cs.findbugs.ba.type.Type;
import edu.umd.cs.findbugs.ba.type.TypeRepository;
import org.apache.bcel.Constants;
import org.apache.bcel.generic.*;

/**
 * Dataflow analysis to determine types for slots in
 * Java stack frames, using a TypeRepository to create the
 * type objects (rather than BCEL's Type classes).
 * <p/>
 * <p> This is still experimental.
 *
 * @author David Hovemeyer
 * @see TypeRepository
 */
public class BetterTypeFrameModelingVisitor extends AbstractFrameModelingVisitor<Type, BetterTypeFrame> {

	private TypeRepository typeRepository;
	private MethodGen methodGen;

	public BetterTypeFrameModelingVisitor(TypeRepository typeRepository, MethodGen methodGen) {
		super(methodGen.getConstantPool());
		this.typeRepository = typeRepository;
		this.methodGen = methodGen;
	}

	public Type getDefaultValue() {
		return typeRepository.getBottomType();
	}

	/**
	 * Consume stack.  This is a convenience method for instructions
	 * where the types of popped operands can be ignored.
	 */
	protected void consumeStack(Instruction ins) {
		ConstantPoolGen cpg = getCPG();
		BetterTypeFrame frame = getFrame();

		int numWordsConsumed = ins.consumeStack(cpg);
		if (numWordsConsumed == Constants.UNPREDICTABLE)
			throw new AnalysisException("Unpredictable stack consumption", methodGen, ins);
		try {
			while (numWordsConsumed-- > 0) {
				frame.popValue();
			}
		} catch (DataflowAnalysisException e) {
			throw new AnalysisException("Stack underflow", methodGen, ins, e);
		}
	}

	/**
	 * Work around some weirdness in BCEL (inherited from JVM Spec 1):
	 * BCEL considers long and double types to consume two slots on the
	 * stack.  This method ensures that we push two types for
	 * each double or long value.
	 */
	protected void pushValue(Type type) {
		BetterTypeFrame frame = getFrame();
		if (type.getTypeCode() == Constants.T_LONG) {
			frame.pushValue(typeRepository.getLongType());
			frame.pushValue(typeRepository.getLongExtraType());
		} else if (type.getTypeCode() == Constants.T_DOUBLE) {
			frame.pushValue(typeRepository.getDoubleType());
			frame.pushValue(typeRepository.getDoubleExtraType());
		} else
			frame.pushValue(type);
	}

	/**
	 * Temporary hack to ease conversion from BCEL types to
	 * TypeRepository types.
	 */
	protected void pushValue(TypedInstruction ins) {
		try {
			pushValue(typeRepository.typeFromSignature(ins.getType(getCPG()).getSignature()));
		} catch (InvalidSignatureException e) {
			throw new AnalysisException("Invalid signature for pushed value", methodGen, (Instruction) ins, e);
		}
	}

	/**
	 * Helper for pushing the return type of an invoke instruction.
	 */
	protected void pushReturnType(InvokeInstruction ins) {
		try {
			ConstantPoolGen cpg = getCPG();
			String returnTypeSig = ins.getSignature(cpg);
			if (!returnTypeSig.equals("V"))
				pushValue(typeRepository.typeFromSignature(returnTypeSig));
		} catch (InvalidSignatureException e) {
			throw new AnalysisException("Invalid signature for pushed value", methodGen, ins, e);
		}
	}

	/**
	 * This is overridden only to ensure that we don't rely on the
	 * base class to handle instructions that produce stack operands.
	 */
	public void modelNormalInstruction(Instruction ins, int numWordsConsumed, int numWordsProduced) {
		if (Debug.VERIFY_INTEGRITY) {
			if (numWordsProduced > 0)
				throw new AnalysisException("Missing visitor method", methodGen, ins);
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
		pushValue(typeRepository.getNullType());
	}

	public void visitDCONST(DCONST obj) {
		pushValue(typeRepository.getDoubleType());
	}

	public void visitFCONST(FCONST obj) {
		pushValue(typeRepository.getFloatType());
	}

	public void visitICONST(ICONST obj) {
		pushValue(typeRepository.getIntType());
	}

	public void visitLCONST(LCONST obj) {
		pushValue(typeRepository.getLongType());
	}

	public void visitLDC(LDC obj) {
		pushValue(obj);
	}

	public void visitLDC2_W(LDC2_W obj) {
		pushValue(obj);
	}

	public void visitBIPUSH(BIPUSH obj) {
		pushValue(typeRepository.getIntType());
	}

	public void visitSIPUSH(SIPUSH obj) {
		pushValue(typeRepository.getIntType());
	}

	public void visitGETSTATIC(GETSTATIC obj) {
		consumeStack(obj);
		pushValue(obj);
	}

	public void visitGETFIELD(GETFIELD obj) {
		consumeStack(obj);
		pushValue(obj);
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
		pushValue(obj);
	}

	public void visitINSTANCEOF(INSTANCEOF obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitFCMPL(FCMPL obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitFCMPG(FCMPG obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitDCMPL(DCMPL obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitDCMPG(DCMPG obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitLCMP(LCMP obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitD2F(D2F obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	public void visitD2I(D2I obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitD2L(D2L obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	public void visitF2D(F2D obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	public void visitF2I(F2I obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitF2L(F2L obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	public void visitI2B(I2B obj) {
		consumeStack(obj);
		pushValue(typeRepository.getByteType());
	}

	public void visitI2C(I2C obj) {
		consumeStack(obj);
		pushValue(typeRepository.getCharType());
	}

	public void visitI2D(I2D obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	public void visitI2F(I2F obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	public void visitI2L(I2L obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	public void visitI2S(I2S obj) {
	} // no change

	public void visitL2D(L2D obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	public void visitL2F(L2F obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	public void visitL2I(L2I obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitIAND(IAND obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitLAND(LAND obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	public void visitIOR(IOR obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitLOR(LOR obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	public void visitIXOR(IXOR obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitLXOR(LXOR obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	public void visitISHR(ISHR obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitIUSHR(IUSHR obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitLSHR(LSHR obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	public void visitLUSHR(LUSHR obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	public void visitISHL(ISHL obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitLSHL(LSHL obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	public void visitDADD(DADD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	public void visitFADD(FADD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	public void visitIADD(IADD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitLADD(LADD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	public void visitDSUB(DSUB obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	public void visitFSUB(FSUB obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	public void visitISUB(ISUB obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitLSUB(LSUB obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	public void visitDMUL(DMUL obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	public void visitFMUL(FMUL obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	public void visitIMUL(IMUL obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitLMUL(LMUL obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	public void visitDDIV(DDIV obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	public void visitFDIV(FDIV obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	public void visitIDIV(IDIV obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitLDIV(LDIV obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	public void visitDREM(DREM obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	public void visitFREM(FREM obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	public void visitIREM(IREM obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitLREM(LREM obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	public void visitIINC(IINC obj) {
	} // no change to types of stack or locals

	public void visitDNEG(DNEG obj) {
	} // no change

	public void visitFNEG(FNEG obj) {
	} // no change

	public void visitINEG(INEG obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitLNEG(LNEG obj) {
	} // no change

	public void visitARRAYLENGTH(ARRAYLENGTH obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitAALOAD(AALOAD obj) {
		// To determine the type pushed on the stack,
		// we look at the type of the array reference which was
		// popped off of the stack.
		BetterTypeFrame frame = getFrame();
		try {
			frame.popValue(); // index
			Type arrayType = frame.popValue(); // arrayref
			if (arrayType instanceof ArrayType) {
				ArrayType arr = (ArrayType) arrayType;
				pushValue(arr.getElementType(typeRepository));
			} else {
				pushValue(typeRepository.getBottomType());
			}
		} catch (DataflowAnalysisException e) {
			throw new AnalysisException("Stack underflow", methodGen, obj, e);
		}
	}

	public void visitBALOAD(BALOAD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getByteType());
	}

	public void visitCALOAD(CALOAD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getCharType());
	}

	public void visitDALOAD(DALOAD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	public void visitFALOAD(FALOAD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	public void visitIALOAD(IALOAD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	public void visitLALOAD(LALOAD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	public void visitSALOAD(SALOAD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getShortType());
	}

	// The various xASTORE instructions only consume stack.

	public void visitNEW(NEW obj) {
		// FIXME: type is technically "uninitialized"
		// However, we don't model that yet.
		pushValue(obj);
	}

	public void visitNEWARRAY(NEWARRAY obj) {
		consumeStack(obj);
		try {
			Type elementType = typeRepository.typeFromSignature(obj.getType().getSignature());
			pushValue(typeRepository.arrayTypeFromElementType(elementType));
		} catch (InvalidSignatureException e) {
			throw new AnalysisException("Invalid signature for pushed value", methodGen, obj, e);
		}
	}

	public void visitANEWARRAY(ANEWARRAY obj) {
		consumeStack(obj);
		try {
			Type elementType = typeRepository.typeFromSignature(obj.getType(getCPG()).getSignature());
			pushValue(typeRepository.arrayTypeFromElementType(elementType));
		} catch (InvalidSignatureException e) {
			throw new AnalysisException("Invalid signature for pushed value", methodGen, obj, e);
		}
	}

	public void visitMULTIANEWARRAY(MULTIANEWARRAY obj) {
		consumeStack(obj);
		try {
			Type elementType = typeRepository.typeFromSignature(obj.getType(getCPG()).getSignature());
			pushValue(typeRepository.arrayTypeFromElementType(elementType));
		} catch (InvalidSignatureException e) {
			throw new AnalysisException("Invalid signature for pushed value", methodGen, obj, e);
		}
	}

	public void visitJSR(JSR obj) {
		pushValue(typeRepository.getReturnAddressType());
	}

	public void visitJSR_W(JSR_W obj) {
		pushValue(typeRepository.getReturnAddressType());
	}

	public void visitRET(RET obj) {
	} // no change

}

// vim:ts=4
