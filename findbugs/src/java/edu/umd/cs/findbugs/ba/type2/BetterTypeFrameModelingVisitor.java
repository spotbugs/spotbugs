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

package edu.umd.cs.findbugs.ba.type2;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.AALOAD;
import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.ANEWARRAY;
import org.apache.bcel.generic.ARRAYLENGTH;
import org.apache.bcel.generic.BALOAD;
import org.apache.bcel.generic.BIPUSH;
import org.apache.bcel.generic.CALOAD;
import org.apache.bcel.generic.CHECKCAST;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.D2F;
import org.apache.bcel.generic.D2I;
import org.apache.bcel.generic.D2L;
import org.apache.bcel.generic.DADD;
import org.apache.bcel.generic.DALOAD;
import org.apache.bcel.generic.DCMPG;
import org.apache.bcel.generic.DCMPL;
import org.apache.bcel.generic.DCONST;
import org.apache.bcel.generic.DDIV;
import org.apache.bcel.generic.DMUL;
import org.apache.bcel.generic.DNEG;
import org.apache.bcel.generic.DREM;
import org.apache.bcel.generic.DSUB;
import org.apache.bcel.generic.F2D;
import org.apache.bcel.generic.F2I;
import org.apache.bcel.generic.F2L;
import org.apache.bcel.generic.FADD;
import org.apache.bcel.generic.FALOAD;
import org.apache.bcel.generic.FCMPG;
import org.apache.bcel.generic.FCMPL;
import org.apache.bcel.generic.FCONST;
import org.apache.bcel.generic.FDIV;
import org.apache.bcel.generic.FMUL;
import org.apache.bcel.generic.FNEG;
import org.apache.bcel.generic.FREM;
import org.apache.bcel.generic.FSUB;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.I2B;
import org.apache.bcel.generic.I2C;
import org.apache.bcel.generic.I2D;
import org.apache.bcel.generic.I2F;
import org.apache.bcel.generic.I2L;
import org.apache.bcel.generic.I2S;
import org.apache.bcel.generic.IADD;
import org.apache.bcel.generic.IALOAD;
import org.apache.bcel.generic.IAND;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.IDIV;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.IMUL;
import org.apache.bcel.generic.INEG;
import org.apache.bcel.generic.INSTANCEOF;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.IOR;
import org.apache.bcel.generic.IREM;
import org.apache.bcel.generic.ISHL;
import org.apache.bcel.generic.ISHR;
import org.apache.bcel.generic.ISUB;
import org.apache.bcel.generic.IUSHR;
import org.apache.bcel.generic.IXOR;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.JSR;
import org.apache.bcel.generic.JSR_W;
import org.apache.bcel.generic.L2D;
import org.apache.bcel.generic.L2F;
import org.apache.bcel.generic.L2I;
import org.apache.bcel.generic.LADD;
import org.apache.bcel.generic.LALOAD;
import org.apache.bcel.generic.LAND;
import org.apache.bcel.generic.LCMP;
import org.apache.bcel.generic.LCONST;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.LDIV;
import org.apache.bcel.generic.LMUL;
import org.apache.bcel.generic.LNEG;
import org.apache.bcel.generic.LOR;
import org.apache.bcel.generic.LREM;
import org.apache.bcel.generic.LSHL;
import org.apache.bcel.generic.LSHR;
import org.apache.bcel.generic.LSUB;
import org.apache.bcel.generic.LUSHR;
import org.apache.bcel.generic.LXOR;
import org.apache.bcel.generic.MULTIANEWARRAY;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.NEWARRAY;
import org.apache.bcel.generic.RET;
import org.apache.bcel.generic.SALOAD;
import org.apache.bcel.generic.SIPUSH;
import org.apache.bcel.generic.TypedInstruction;

import edu.umd.cs.findbugs.ba.AbstractFrameModelingVisitor;
import edu.umd.cs.findbugs.ba.AnalysisException;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Debug;

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

	@Override
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
	@Override
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

	@Override
         public void visitACONST_NULL(ACONST_NULL obj) {
		pushValue(typeRepository.getNullType());
	}

	@Override
         public void visitDCONST(DCONST obj) {
		pushValue(typeRepository.getDoubleType());
	}

	@Override
         public void visitFCONST(FCONST obj) {
		pushValue(typeRepository.getFloatType());
	}

	@Override
         public void visitICONST(ICONST obj) {
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitLCONST(LCONST obj) {
		pushValue(typeRepository.getLongType());
	}

	@Override
         public void visitLDC(LDC obj) {
		pushValue(obj);
	}

	@Override
         public void visitLDC2_W(LDC2_W obj) {
		pushValue(obj);
	}

	@Override
         public void visitBIPUSH(BIPUSH obj) {
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitSIPUSH(SIPUSH obj) {
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitGETSTATIC(GETSTATIC obj) {
		consumeStack(obj);
		pushValue(obj);
	}

	@Override
         public void visitGETFIELD(GETFIELD obj) {
		consumeStack(obj);
		pushValue(obj);
	}

	@Override
         public void visitINVOKESTATIC(INVOKESTATIC obj) {
		consumeStack(obj);
		pushReturnType(obj);
	}

	@Override
         public void visitINVOKESPECIAL(INVOKESPECIAL obj) {
		consumeStack(obj);
		pushReturnType(obj);
	}

	@Override
         public void visitINVOKEINTERFACE(INVOKEINTERFACE obj) {
		consumeStack(obj);
		pushReturnType(obj);
	}

	@Override
         public void visitINVOKEVIRTUAL(INVOKEVIRTUAL obj) {
		consumeStack(obj);
		pushReturnType(obj);
	}

	@Override
         public void visitCHECKCAST(CHECKCAST obj) {
		consumeStack(obj);
		pushValue(obj);
	}

	@Override
         public void visitINSTANCEOF(INSTANCEOF obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitFCMPL(FCMPL obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitFCMPG(FCMPG obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitDCMPL(DCMPL obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitDCMPG(DCMPG obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitLCMP(LCMP obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitD2F(D2F obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	@Override
         public void visitD2I(D2I obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitD2L(D2L obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	@Override
         public void visitF2D(F2D obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	@Override
         public void visitF2I(F2I obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitF2L(F2L obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	@Override
         public void visitI2B(I2B obj) {
		consumeStack(obj);
		pushValue(typeRepository.getByteType());
	}

	@Override
         public void visitI2C(I2C obj) {
		consumeStack(obj);
		pushValue(typeRepository.getCharType());
	}

	@Override
         public void visitI2D(I2D obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	@Override
         public void visitI2F(I2F obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	@Override
         public void visitI2L(I2L obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	@Override
         public void visitI2S(I2S obj) {
	} // no change

	@Override
         public void visitL2D(L2D obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	@Override
         public void visitL2F(L2F obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	@Override
         public void visitL2I(L2I obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitIAND(IAND obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitLAND(LAND obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	@Override
         public void visitIOR(IOR obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitLOR(LOR obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	@Override
         public void visitIXOR(IXOR obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitLXOR(LXOR obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	@Override
         public void visitISHR(ISHR obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitIUSHR(IUSHR obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitLSHR(LSHR obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	@Override
         public void visitLUSHR(LUSHR obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	@Override
         public void visitISHL(ISHL obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitLSHL(LSHL obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	@Override
         public void visitDADD(DADD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	@Override
         public void visitFADD(FADD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	@Override
         public void visitIADD(IADD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitLADD(LADD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	@Override
         public void visitDSUB(DSUB obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	@Override
         public void visitFSUB(FSUB obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	@Override
         public void visitISUB(ISUB obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitLSUB(LSUB obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	@Override
         public void visitDMUL(DMUL obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	@Override
         public void visitFMUL(FMUL obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	@Override
         public void visitIMUL(IMUL obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitLMUL(LMUL obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	@Override
         public void visitDDIV(DDIV obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	@Override
         public void visitFDIV(FDIV obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	@Override
         public void visitIDIV(IDIV obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitLDIV(LDIV obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	@Override
         public void visitDREM(DREM obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	@Override
         public void visitFREM(FREM obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	@Override
         public void visitIREM(IREM obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitLREM(LREM obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	@Override
         public void visitIINC(IINC obj) {
	} // no change to types of stack or locals

	@Override
         public void visitDNEG(DNEG obj) {
	} // no change

	@Override
         public void visitFNEG(FNEG obj) {
	} // no change

	@Override
         public void visitINEG(INEG obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitLNEG(LNEG obj) {
	} // no change

	@Override
         public void visitARRAYLENGTH(ARRAYLENGTH obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
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

	@Override
         public void visitBALOAD(BALOAD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getByteType());
	}

	@Override
         public void visitCALOAD(CALOAD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getCharType());
	}

	@Override
         public void visitDALOAD(DALOAD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getDoubleType());
	}

	@Override
         public void visitFALOAD(FALOAD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getFloatType());
	}

	@Override
         public void visitIALOAD(IALOAD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getIntType());
	}

	@Override
         public void visitLALOAD(LALOAD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getLongType());
	}

	@Override
         public void visitSALOAD(SALOAD obj) {
		consumeStack(obj);
		pushValue(typeRepository.getShortType());
	}

	// The various xASTORE instructions only consume stack.

	@Override
         public void visitNEW(NEW obj) {
		// FIXME: type is technically "uninitialized"
		// However, we don't model that yet.
		pushValue(obj);
	}

	@Override
         public void visitNEWARRAY(NEWARRAY obj) {
		consumeStack(obj);
		try {
			Type elementType = typeRepository.typeFromSignature(obj.getType().getSignature());
			pushValue(typeRepository.arrayTypeFromElementType(elementType));
		} catch (InvalidSignatureException e) {
			throw new AnalysisException("Invalid signature for pushed value", methodGen, obj, e);
		}
	}

	@Override
         public void visitANEWARRAY(ANEWARRAY obj) {
		consumeStack(obj);
		try {
			Type elementType = typeRepository.typeFromSignature(obj.getType(getCPG()).getSignature());
			pushValue(typeRepository.arrayTypeFromElementType(elementType));
		} catch (InvalidSignatureException e) {
			throw new AnalysisException("Invalid signature for pushed value", methodGen, obj, e);
		}
	}

	@Override
         public void visitMULTIANEWARRAY(MULTIANEWARRAY obj) {
		consumeStack(obj);
		try {
			Type elementType = typeRepository.typeFromSignature(obj.getType(getCPG()).getSignature());
			pushValue(elementType);
		} catch (InvalidSignatureException e) {
			throw new AnalysisException("Invalid signature for pushed value", methodGen, obj, e);
		}
	}

	@Override
         public void visitJSR(JSR obj) {
		pushValue(typeRepository.getReturnAddressType());
	}

	@Override
         public void visitJSR_W(JSR_W obj) {
		pushValue(typeRepository.getReturnAddressType());
	}

	@Override
         public void visitRET(RET obj) {
	} // no change

}

// vim:ts=4
