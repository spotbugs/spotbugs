/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.daveho.ba;

import java.util.*;

// We require BCEL 5.0 or later.
import org.apache.bcel.*;
import org.apache.bcel.generic.*;

/**
 * Visitor to model the effects of bytecode instructions on the
 * types of the values (local and operand stack) in Java stack frames.
 * This visitor does not verify that the types are sensible
 * for the bytecodes executed.  In other words, this isn't a bytecode
 * verifier, although it wouldn't be too hard to turn it into one.
 *
 * @see TypedFrame
 * @see FrameAnalysis
 * @author David Hovemeyer
 */
public class FrameTypeModelingVisitor implements Visitor, Constants {
	private TypedFrame frame;
	private ConstantPoolGen cpg;
	private ArrayList<Type> poppedTypeList;

	// FIXME: major bug in dup bytecodes!
	// It assumes that values can be popped off the stack.
	// However, these values have already been popped in the
	// StackConsumer visit method, and saved in the poppedTypeList.
	// Need to get them from there!!

	/**
	 * Constructor.
	 * @param frame the TypedFrame object to be operated on
	 * @param cpg the ConstantPoolGen of the method whose instructions we are examining
	 */
	public FrameTypeModelingVisitor(TypedFrame frame, ConstantPoolGen cpg) {
		this.frame = frame;
		this.cpg = cpg;
	}

	/**
	 * Work around some weirdness in BCEL (inherited from JVM Spec 1):
	 * BCEL considers long and double types to consume two slots on the
	 * stack.  This method ensures that we push two types for
	 * each double or long value.
	 */
	private void pushValue(Type type) {
		if (type.getType() == T_LONG) {
			frame.pushValue(Type.LONG);
			frame.pushValue(TypedFrame.getLongExtraType());
		} else if (type.getType() == T_DOUBLE) {
			frame.pushValue(Type.DOUBLE);
			frame.pushValue(TypedFrame.getDoubleExtraType());
		} else
			frame.pushValue(type);
	}

	/**
	 * Helper for pushing the return type of an invoke instruction.
	 */
	private void pushReturnType(InvokeInstruction ins) {
		Type type = ins.getType(cpg);
		if (type.getType() != T_VOID)
			pushValue(type);
	}

	/**
	 * BCEL supports some bytecodes which are not real.
	 */
	private void unimplementedBytecode() {
		throw new IllegalStateException("Bugger alle thys for a larke");
	}

	public void visitStackInstruction(StackInstruction obj) { }
	public void visitLocalVariableInstruction(LocalVariableInstruction obj) { }
	public void visitBranchInstruction(BranchInstruction obj) { }
	public void visitLoadClass(LoadClass obj) { }
	public void visitFieldInstruction(FieldInstruction obj) { }
	public void visitIfInstruction(IfInstruction obj) { }
	public void visitConversionInstruction(ConversionInstruction obj) { }
	public void visitPopInstruction(PopInstruction obj) { }
	public void visitStoreInstruction(StoreInstruction obj) { }
	public void visitTypedInstruction(TypedInstruction obj) { }
	public void visitSelect(Select obj) { }
	public void visitJsrInstruction(JsrInstruction obj) { }
	public void visitGotoInstruction(GotoInstruction obj) { }
	public void visitUnconditionalBranch(UnconditionalBranch obj) { }
	public void visitPushInstruction(PushInstruction obj) { }
	public void visitArithmeticInstruction(ArithmeticInstruction obj) { }
	public void visitCPInstruction(CPInstruction obj) { }
	public void visitInvokeInstruction(InvokeInstruction obj) { }
	public void visitArrayInstruction(ArrayInstruction obj) { }
	public void visitAllocationInstruction(AllocationInstruction obj) { }
	public void visitReturnInstruction(ReturnInstruction obj) { }
	public void visitFieldOrMethod(FieldOrMethod obj) { }
	public void visitConstantPushInstruction(ConstantPushInstruction obj) { }
	public void visitExceptionThrower(ExceptionThrower obj) { }
	public void visitLoadInstruction(LoadInstruction obj) { }
	public void visitVariableLengthInstruction(VariableLengthInstruction obj) { }
	public void visitStackProducer(StackProducer obj) { }

	/**
	 * This method gets called by ALL instructions which consume operands.
	 * Because we don't actually check that the right types are present
	 * (as the bytecode verifier would), we can handle all stack consumers here.
	 * The popped types are saved in a list, because we may occasionally
	 * need to use them.
	 */
	public void visitStackConsumer(StackConsumer obj) {
		int n = obj.consumeStack(cpg);
		if (n == Constants.UNPREDICTABLE)
			throw new IllegalStateException("Unpredictable stack consumption from instruction " + obj);
		try {
			poppedTypeList.clear();
			while (n-- > 0)
				poppedTypeList.add(frame.popValue());
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	public void visitACONST_NULL(ACONST_NULL obj)	{ pushValue(TypedFrame.getNullType()); }

	public void visitGETSTATIC(GETSTATIC obj)		{ pushValue(obj.getType(cpg)); }

	public void visitIF_ICMPLT(IF_ICMPLT obj)		{ } // consumes stack only

	public void visitMONITOREXIT(MONITOREXIT obj)	{ } // consumes stack only

	public void visitIFLT(IFLT obj)					{ } // consumes stack only

	public void visitLSTORE(LSTORE obj) {
		// Update types of locals
		int local = obj.getIndex();
		frame.setValue(local, Type.LONG);
		frame.setValue(local + 1, TypedFrame.getLongExtraType());
	}

	public void visitPOP2(POP2 obj)					{ } // consumes stack only

	public void visitBASTORE(BASTORE obj)			{ } // consumes stack only

	public void visitISTORE(ISTORE obj)	{
		int local = obj.getIndex();
		frame.setValue(local, Type.INT);
	}

	public void visitCHECKCAST(CHECKCAST obj)		{ pushValue(obj.getType(cpg)); }

	public void visitFCMPG(FCMPG obj)				{ pushValue(Type.INT); }

	public void visitI2F(I2F obj)					{ pushValue(Type.FLOAT); }

	public void visitATHROW(ATHROW obj) {
		// Does not matter what we do here.
	}

	public void visitDCMPL(DCMPL obj)				{ pushValue(Type.INT); }

	public void visitARRAYLENGTH(ARRAYLENGTH obj)	{ pushValue(Type.INT); }

	public void visitDUP(DUP obj) {
		try {
			pushValue(frame.getTopValue());
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	public void visitINVOKESTATIC(INVOKESTATIC obj)	{ pushReturnType(obj); }

	public void visitLCONST(LCONST obj)				{ pushValue(Type.LONG); }

	public void visitDREM(DREM obj)					{ pushValue(Type.DOUBLE); }

	public void visitIFGE(IFGE obj)					{ } // consumes stack only

	public void visitCALOAD(CALOAD obj)				{ pushValue(Type.CHAR); }

	public void visitLASTORE(LASTORE obj)			{ } // consumes stack only

	public void visitI2D(I2D obj)					{ pushValue(Type.DOUBLE); }

	public void visitDADD(DADD obj)					{ pushValue(Type.DOUBLE); }

	public void visitINVOKESPECIAL(INVOKESPECIAL obj) { pushReturnType(obj); }

	public void visitIAND(IAND obj)					{ pushValue(Type.INT); }

	public void visitPUTFIELD(PUTFIELD obj)			{ } // consumes stack only

	public void visitILOAD(ILOAD obj)				{ pushValue(Type.INT); }

	public void visitDLOAD(DLOAD obj)				{ pushValue(Type.DOUBLE); }

	public void visitDCONST(DCONST obj)				{ pushValue(Type.DOUBLE); }

	public void visitNEW(NEW obj) {
		// FIXME: type is technically "uninitialized"
		// However, we don't model that yet.
		pushValue(obj.getType(cpg));
	}

	public void visitIFNULL(IFNULL obj)				{ } // consumes stack only

	public void visitLSUB(LSUB obj)					{ pushValue(Type.LONG); }

	public void visitL2I(L2I obj)					{ pushValue(Type.INT); }

	public void visitISHR(ISHR obj)					{ pushValue(Type.INT); }

	public void visitTABLESWITCH(TABLESWITCH obj)	{ } // consumes stack only

	public void visitIINC(IINC obj)					{ } // no change to types of stack or locals

	public void visitDRETURN(DRETURN obj)			{ } // consumes stack only

	public void visitFSTORE(FSTORE obj) {
		// Update type of local
		int local = obj.getIndex();
		frame.setValue(local, Type.FLOAT);
	}

	public void visitDASTORE(DASTORE obj)			{ } // consumes stack only

	public void visitIALOAD(IALOAD obj)				{ pushValue(Type.INT); }

	public void visitDDIV(DDIV obj)					{ pushValue(Type.DOUBLE); }

	public void visitIF_ICMPGE(IF_ICMPGE obj)		{ } // consumes stack only

	public void visitLAND(LAND obj)					{ pushValue(Type.LONG); }

	public void visitIDIV(IDIV obj)					{ pushValue(Type.INT); }

	public void visitLOR(LOR obj)					{ pushValue(Type.LONG); }

	public void visitCASTORE(CASTORE obj)			{ } // consumes stack only

	public void visitFREM(FREM obj)					{ pushValue(Type.FLOAT); }

	public void visitLDC(LDC obj)					{ pushValue(obj.getType(cpg)); }

	public void visitBIPUSH(BIPUSH obj)				{ pushValue(Type.INT); }

	public void visitDSTORE(DSTORE obj) {
		// Set type of locals
		int local = obj.getIndex();
		frame.setValue(local, Type.DOUBLE);
		frame.setValue(local + 1, TypedFrame.getDoubleExtraType());
 	}

	public void visitF2L(F2L obj)					{ pushValue(Type.LONG); }

	public void visitFMUL(FMUL obj)					{ pushValue(Type.FLOAT); }

	public void visitLLOAD(LLOAD obj)				{ pushValue(Type.LONG); }

	public void visitJSR(JSR obj)					{ pushValue(ReturnaddressType.NO_TARGET); }

	public void visitFSUB(FSUB obj)					{ pushValue(Type.FLOAT); }

	public void visitSASTORE(SASTORE obj)			{ } // consumes stack only

	public void visitALOAD(ALOAD obj) {
		// Type moves from local slot onto top of stack
		int local = obj.getIndex();
		Type refType = frame.getValue(local);
		pushValue(refType);
	}

	public void visitDUP2_X2(DUP2_X2 obj) {
		// This is a weird one.
		try {
			Type value1 = frame.popValue();
			Type value2 = frame.popValue();
			Type value3 = frame.popValue();
			Type value4 = frame.popValue();
			pushValue(value2);
			pushValue(value1);
			pushValue(value4);
			pushValue(value3);
			pushValue(value2);
			pushValue(value1);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	public void visitRETURN(RETURN obj)				{ } // no change

	public void visitDALOAD(DALOAD obj)				{ pushValue(Type.DOUBLE); }

	public void visitSIPUSH(SIPUSH obj)				{ pushValue(Type.INT); }

	public void visitDSUB(DSUB obj)					{ pushValue(Type.DOUBLE); }

	public void visitL2F(L2F obj)					{ pushValue(Type.FLOAT); }

	public void visitIF_ICMPGT(IF_ICMPGT obj)		{ } // consumes stack only

	public void visitF2D(F2D obj)					{ pushValue(Type.DOUBLE); }

	public void visitI2L(I2L obj)					{ pushValue(Type.INT); }

	public void visitIF_ACMPNE(IF_ACMPNE obj)		{ } // consumes stack only

	public void visitPOP(POP obj)					{ } // consumes stack only

	public void visitI2S(I2S obj)					{ } // no change

	public void visitIFEQ(IFEQ obj)					{ } // consumes stack only

	public void visitSWAP(SWAP obj) {
		try {
			Type value1 = frame.popValue();
			Type value2 = frame.popValue();
			pushValue(value1);
			pushValue(value2);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	public void visitIOR(IOR obj)					{ pushValue(Type.INT); }

	public void visitIREM(IREM obj)					{ pushValue(Type.INT); }

	public void visitIASTORE(IASTORE obj)			{ } // consumes stack only

	public void visitNEWARRAY(NEWARRAY obj)			{ pushValue(obj.getType()); }

	public void visitINVOKEINTERFACE(INVOKEINTERFACE obj) { pushReturnType(obj); }

	public void visitINEG(INEG obj)					{ pushValue(Type.INT); }

	public void visitLCMP(LCMP obj)					{ pushValue(Type.INT); }

	public void visitJSR_W(JSR_W obj)				{ pushValue(ReturnaddressType.NO_TARGET); }

	public void visitMULTIANEWARRAY(MULTIANEWARRAY obj) {
		// TODO: I'm a bit skeptical that the implementation of getType() used
		// by MULTIANEWARRAY is correct.  Trust it for now.
		pushValue(obj.getType(cpg));
	}

	public void visitDUP_X2(DUP_X2 obj) {
		try {
			Type value1 = frame.popValue();
			Type value2 = frame.popValue();
			Type value3 = frame.popValue();
			pushValue(value1);
			pushValue(value3);
			pushValue(value2);
			pushValue(value1);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	public void visitSALOAD(SALOAD obj)				{ pushValue(Type.SHORT); }

	public void visitIFNONNULL(IFNONNULL obj)		{ } // consumes stack only

	public void visitDMUL(DMUL obj)					{ pushValue(Type.DOUBLE); }

	public void visitIFNE(IFNE obj)					{ } // consumes stack only

	public void visitIF_ICMPLE(IF_ICMPLE obj)		{ } // consumes stack only

	public void visitLDC2_W(LDC2_W obj)				{ pushValue(obj.getType(cpg)); }

	public void visitGETFIELD(GETFIELD obj)			{ pushValue(obj.getType(cpg)); }

	public void visitLADD(LADD obj)					{ pushValue(Type.LONG); }

	public void visitNOP(NOP obj)					{ } // no change

	public void visitFALOAD(FALOAD obj)				{ pushValue(Type.FLOAT); }

	public void visitINSTANCEOF(INSTANCEOF obj)		{ pushValue(Type.INT); }

	public void visitIFLE(IFLE obj)					{ } // consumes stack only

	public void visitLXOR(LXOR obj)					{ pushValue(Type.LONG); }

	public void visitLRETURN(LRETURN obj)			{ } // consumes stack only

	public void visitFCONST(FCONST obj)				{ pushValue(Type.FLOAT); }

	public void visitIUSHR(IUSHR obj)				{ pushValue(Type.INT); }

	public void visitBALOAD(BALOAD obj)				{ pushValue(Type.BOOLEAN); }

	public void visitDUP2(DUP2 obj) {
		try {
			Type value1 = frame.popValue();
			Type value2 = frame.popValue();
			pushValue(value2);
			pushValue(value1);
			pushValue(value2);
			pushValue(value1);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	public void visitIF_ACMPEQ(IF_ACMPEQ obj)		{ } // consumes stack only

	public void visitIMPDEP1(IMPDEP1 obj)			{ unimplementedBytecode(); }

	public void visitMONITORENTER(MONITORENTER obj) { } // consumes stack only

	public void visitLSHL(LSHL obj)					{ pushValue(Type.LONG); }

	public void visitDCMPG(DCMPG obj)				{ pushValue(Type.INT); }

	public void visitD2L(D2L obj)					{ pushValue(Type.LONG); }

	public void visitIMPDEP2(IMPDEP2 obj)			{ unimplementedBytecode(); }

	public void visitL2D(L2D obj)					{ pushValue(Type.DOUBLE); }

	public void visitRET(RET obj)					{ } // no change

	public void visitIFGT(IFGT obj)					{ } // consumes stack only

	public void visitIXOR(IXOR obj)					{ pushValue(Type.INT); }

	public void visitINVOKEVIRTUAL(INVOKEVIRTUAL obj) { pushReturnType(obj); }

	public void visitFASTORE(FASTORE obj)			{ } // consumes stack only

	public void visitIRETURN(IRETURN obj)			{ } // consumes stack only

	public void visitIF_ICMPNE(IF_ICMPNE obj)		{ } // consumes stack only

	public void visitFLOAD(FLOAD obj)				{ pushValue(Type.FLOAT); }

	public void visitLDIV(LDIV obj)					{ pushValue(Type.LONG); }

	public void visitPUTSTATIC(PUTSTATIC obj)		{ } // consumes stack only

	public void visitAALOAD(AALOAD obj) {
		// To determine the type pushed on the stack,
		// we look at the type of the array reference which was
		// popped off of the stack.
		Type arrayType = poppedTypeList.get(1);
		ArrayType arr = (ArrayType) arrayType;
		pushValue(arr.getElementType());
	}

	public void visitD2I(D2I obj)					{ pushValue(Type.INT); }

	public void visitIF_ICMPEQ(IF_ICMPEQ obj)		{ } // consumes stack only

	public void visitAASTORE(AASTORE obj)			{ } // consumes stack only

	public void visitARETURN(ARETURN obj)			{ } // consumes stack only

	public void visitDUP2_X1(DUP2_X1 obj) {
		try {
			Type value1 = frame.popValue();
			Type value2 = frame.popValue();
			Type value3 = frame.popValue();
			pushValue(value2);
			pushValue(value1);
			pushValue(value3);
			pushValue(value2);
			pushValue(value1);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	public void visitFNEG(FNEG obj)					{ } // no change

	public void visitGOTO_W(GOTO_W obj)				{ } // no change

	public void visitD2F(D2F obj)					{ pushValue(Type.FLOAT); }

	public void visitGOTO(GOTO obj)					{ } // no change

	public void visitISUB(ISUB obj)					{ pushValue(Type.INT); }

	public void visitF2I(F2I obj)					{ pushValue(Type.INT); }

	public void visitDNEG(DNEG obj)					{ } // no change

	public void visitICONST(ICONST obj)				{ pushValue(Type.INT); }

	public void visitFDIV(FDIV obj)					{ pushValue(Type.FLOAT); }

	public void visitI2B(I2B obj)					{ pushValue(Type.BYTE); }

	public void visitLNEG(LNEG obj)					{ pushValue(Type.LONG); }

	public void visitLREM(LREM obj)					{ pushValue(Type.LONG); }

	public void visitIMUL(IMUL obj)					{ pushValue(Type.INT); }

	public void visitIADD(IADD obj)					{ pushValue(Type.INT); }

	public void visitLSHR(LSHR obj)					{ pushValue(Type.LONG); }

	public void visitLOOKUPSWITCH(LOOKUPSWITCH obj) { } // consumes stack only

	public void visitDUP_X1(DUP_X1 obj) {
		try {
			Type value1 = frame.popValue();
			Type value2 = frame.popValue();
			pushValue(value1);
			pushValue(value2);
			pushValue(value1);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	public void visitFCMPL(FCMPL obj)				{ pushValue(Type.INT); }

	public void visitI2C(I2C obj)					{ pushValue(Type.CHAR); }

	public void visitLMUL(LMUL obj)					{ pushValue(Type.LONG); }

	public void visitLUSHR(LUSHR obj)				{ pushValue(Type.LONG); }

	public void visitISHL(ISHL obj)					{ pushValue(Type.INT); }

	public void visitLALOAD(LALOAD obj)				{ pushValue(Type.LONG); }

	public void visitASTORE(ASTORE obj) {
		// Change type of local to type of value popped from the stack
		int local = obj.getIndex();
		Type type = poppedTypeList.get(0);
		frame.setValue(local, type);
	}

	public void visitANEWARRAY(ANEWARRAY obj)		{ pushValue(obj.getType(cpg)); }

	public void visitFRETURN(FRETURN obj)			{ } // consumes stack only

	public void visitFADD(FADD obj)					{ pushValue(Type.FLOAT); }

	public void visitBREAKPOINT(BREAKPOINT obj)		{ unimplementedBytecode(); }
}

// vim:ts=4
