package edu.umd.cs.daveho.ba;

import org.apache.bcel.*;
import org.apache.bcel.generic.*;

/**
 * Model the effects of Java bytecode instructions in order to
 * analyze which stack slots definitely contain the "this" reference.
 *
 * <p> <b>Important note</b>: there is a bug in BCEL 5.0 that prevents
 * this class from working.  Use a later version.
 *
 * @see ThisValueFrame
 * @see ThisValueAnalysis
 * @author David Hovemeyer
 */
public class ThisValueFrameModelingVisitor implements Visitor {
	private ThisValueFrame frame;
	private ConstantPoolGen cpg;

	/**
	 * Constructor.
	 * @param frame the frame to be transformed
	 * @param cpg the ConstantPoolGen of the method to be analyzed
	 */
	public ThisValueFrameModelingVisitor(ThisValueFrame frame, ConstantPoolGen cpg) {
		this.frame = frame;
		this.cpg = cpg;
	}

	private void barf() {
		throw new IllegalStateException("Bugger alle thys for a larke!");
	}

	public void visitStackInstruction(StackInstruction obj) { }
	public void visitLocalVariableInstruction(LocalVariableInstruction obj) { }
	public void visitBranchInstruction(BranchInstruction obj) { }
	public void visitLoadClass(LoadClass obj) { }
	public void visitFieldInstruction(FieldInstruction obj) { }
	public void visitIfInstruction(IfInstruction obj) { }
	public void visitConversionInstruction(ConversionInstruction obj) { }
	public void visitPopInstruction(PopInstruction obj) { }
	public void visitJsrInstruction(JsrInstruction obj) { }
	public void visitGotoInstruction(GotoInstruction obj) { }
	public void visitStoreInstruction(StoreInstruction obj) { }
	public void visitTypedInstruction(TypedInstruction obj) { }
	public void visitSelect(Select obj) { }
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
	public void visitStackConsumer(StackConsumer obj) { }

	/**
	 * "Normal" handler.
	 * This models the stack for all instructions which destroy
	 * any "this" values they consume, and produce only "non-this" values.
	 */
	private void normal(Instruction ins) {
		int nwords;

		nwords = ins.consumeStack(cpg);
		if (nwords == Constants.UNPREDICTABLE) throw new IllegalStateException();
		try {
			while (nwords-- > 0)
				frame.popValue();
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.getMessage());
		}

		nwords = ins.produceStack(cpg);
		if (nwords == Constants.UNPREDICTABLE) throw new IllegalStateException();
		while (nwords-- > 0)
			frame.pushValue(ThisValue.notThisValue());
	}

	public void visitACONST_NULL(ACONST_NULL obj) { normal(obj); }
	public void visitGETSTATIC(GETSTATIC obj) { normal(obj); }
	public void visitIF_ICMPLT(IF_ICMPLT obj) { normal(obj); }
	public void visitMONITOREXIT(MONITOREXIT obj) { normal(obj); }
	public void visitIFLT(IFLT obj) { normal(obj); }
	public void visitLSTORE(LSTORE obj) {
		// Model the stack
		normal(obj);
		// Set values in locals written by the instruction
		int local = obj.getIndex();
		frame.setValue(local, ThisValue.notThisValue());
		frame.setValue(local + 1, ThisValue.notThisValue());
	}
	public void visitPOP2(POP2 obj) { normal(obj); }
	public void visitBASTORE(BASTORE obj) { normal(obj); }
	public void visitISTORE(ISTORE obj) {
		// Model the stack
		normal(obj);
		// Set value in local written by the instruction
		int local = obj.getIndex();
		frame.setValue(local, ThisValue.notThisValue());
	}
	public void visitCHECKCAST(CHECKCAST obj) { normal(obj); }
	public void visitFCMPG(FCMPG obj) { normal(obj); }
	public void visitI2F(I2F obj) { normal(obj); }
	public void visitATHROW(ATHROW obj) { normal(obj); }
	public void visitDCMPL(DCMPL obj) { normal(obj); }
	public void visitARRAYLENGTH(ARRAYLENGTH obj) { normal(obj); }
	public void visitDUP(DUP obj) {
		try {
			ThisValue value = frame.popValue();
			frame.pushValue(value);
			frame.pushValue(value);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}
	public void visitINVOKESTATIC(INVOKESTATIC obj) { normal(obj); }
	public void visitLCONST(LCONST obj) { normal(obj); }
	public void visitDREM(DREM obj) { normal(obj); }
	public void visitIFGE(IFGE obj) { normal(obj); }
	public void visitCALOAD(CALOAD obj) { normal(obj); }
	public void visitLASTORE(LASTORE obj) { normal(obj); }
	public void visitI2D(I2D obj) { normal(obj); }
	public void visitDADD(DADD obj) { normal(obj); }
	public void visitINVOKESPECIAL(INVOKESPECIAL obj) { normal(obj); }
	public void visitIAND(IAND obj) { normal(obj); }
	public void visitPUTFIELD(PUTFIELD obj) { normal(obj); }
	public void visitILOAD(ILOAD obj) { normal(obj); } // only ALOAD can move "this"
	public void visitDLOAD(DLOAD obj) { normal(obj); } // only ALOAD can move "this"
	public void visitDCONST(DCONST obj) { normal(obj); }
	public void visitNEW(NEW obj) { normal(obj); }
	public void visitIFNULL(IFNULL obj) { normal(obj); }
	public void visitLSUB(LSUB obj) { normal(obj); }
	public void visitL2I(L2I obj) { normal(obj); }
	public void visitISHR(ISHR obj) { normal(obj); }
	public void visitTABLESWITCH(TABLESWITCH obj) { normal(obj); }
	public void visitIINC(IINC obj) { normal(obj); } // note that this ins should never refer to "this"
	public void visitDRETURN(DRETURN obj) { normal(obj); }
	public void visitFSTORE(FSTORE obj) {
		// Model the stack
		normal(obj);
		// Set value in local written by the instruction
		int local = obj.getIndex();
		frame.setValue(local, ThisValue.notThisValue());
	}
	public void visitDASTORE(DASTORE obj) { normal(obj); }
	public void visitIALOAD(IALOAD obj) { normal(obj); }
	public void visitDDIV(DDIV obj) { normal(obj); }
	public void visitIF_ICMPGE(IF_ICMPGE obj) { normal(obj); }
	public void visitLAND(LAND obj) { normal(obj); }
	public void visitIDIV(IDIV obj) { normal(obj); }
	public void visitLOR(LOR obj) { normal(obj); }
	public void visitCASTORE(CASTORE obj) { normal(obj); }
	public void visitFREM(FREM obj) { normal(obj); }
	public void visitLDC(LDC obj) { normal(obj); }
	public void visitBIPUSH(BIPUSH obj) { normal(obj); }
	public void visitDSTORE(DSTORE obj) {
		// Model the stack
		normal(obj);
		// Set values in locals written by the instruction
		int local = obj.getIndex();
		frame.setValue(local, ThisValue.notThisValue());
		frame.setValue(local + 1, ThisValue.notThisValue());
	}
	public void visitF2L(F2L obj) { normal(obj); }
	public void visitFMUL(FMUL obj) { normal(obj); }
	public void visitLLOAD(LLOAD obj) { normal(obj); } // only ALOAD can move "this"
	public void visitJSR(JSR obj) { normal(obj); }
	public void visitFSUB(FSUB obj) { normal(obj); }
	public void visitSASTORE(SASTORE obj) { normal(obj); }
	public void visitALOAD(ALOAD obj) {
		int local = obj.getIndex();
		ThisValue value = frame.getValue(local);
		frame.pushValue(value);
	}
	public void visitDUP2_X2(DUP2_X2 obj) {
		try {
			ThisValue value1 = frame.popValue();
			ThisValue value2 = frame.popValue();
			ThisValue value3 = frame.popValue();
			ThisValue value4 = frame.popValue();
			frame.pushValue(value2);
			frame.pushValue(value1);
			frame.pushValue(value4);
			frame.pushValue(value3);
			frame.pushValue(value2);
			frame.pushValue(value1);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}
	public void visitRETURN(RETURN obj) { normal(obj); }
	public void visitDALOAD(DALOAD obj) { normal(obj); }
	public void visitSIPUSH(SIPUSH obj) { normal(obj); }
	public void visitDSUB(DSUB obj) { normal(obj); }
	public void visitL2F(L2F obj) { normal(obj); }
	public void visitIF_ICMPGT(IF_ICMPGT obj) { normal(obj); }
	public void visitF2D(F2D obj) { normal(obj); }
	public void visitI2L(I2L obj) { normal(obj); }
	public void visitIF_ACMPNE(IF_ACMPNE obj) { normal(obj); }
	public void visitPOP(POP obj) { normal(obj); }
	public void visitI2S(I2S obj) { normal(obj); }
	public void visitIFEQ(IFEQ obj) { normal(obj); }
	public void visitSWAP(SWAP obj) {
		try {
			ThisValue value1 = frame.popValue();
			ThisValue value2 = frame.popValue();
			frame.pushValue(value1);
			frame.pushValue(value2);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}
	public void visitIOR(IOR obj) { normal(obj); }
	public void visitIREM(IREM obj) { normal(obj); }
	public void visitIASTORE(IASTORE obj) { normal(obj); }
	public void visitNEWARRAY(NEWARRAY obj) { normal(obj); }
	public void visitINVOKEINTERFACE(INVOKEINTERFACE obj) { normal(obj); }
	public void visitINEG(INEG obj) { normal(obj); }
	public void visitLCMP(LCMP obj) { normal(obj); }
	public void visitJSR_W(JSR_W obj) { normal(obj); }
	public void visitMULTIANEWARRAY(MULTIANEWARRAY obj) { normal(obj); }
	public void visitDUP_X2(DUP_X2 obj) {
		try {
			ThisValue value1 = frame.popValue();
			ThisValue value2 = frame.popValue();
			ThisValue value3 = frame.popValue();
			frame.pushValue(value1);
			frame.pushValue(value3);
			frame.pushValue(value2);
			frame.pushValue(value1);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}
	public void visitSALOAD(SALOAD obj) { normal(obj); }
	public void visitIFNONNULL(IFNONNULL obj) { normal(obj); }
	public void visitDMUL(DMUL obj) { normal(obj); }
	public void visitIFNE(IFNE obj) { normal(obj); }
	public void visitIF_ICMPLE(IF_ICMPLE obj) { normal(obj); }
	public void visitLDC2_W(LDC2_W obj) { normal(obj); }
	public void visitGETFIELD(GETFIELD obj) { normal(obj); }
	public void visitLADD(LADD obj) { normal(obj); }
	public void visitNOP(NOP obj) { normal(obj); }
	public void visitFALOAD(FALOAD obj) { normal(obj); }
	public void visitINSTANCEOF(INSTANCEOF obj) { normal(obj); }
	public void visitIFLE(IFLE obj) { normal(obj); }
	public void visitLXOR(LXOR obj) { normal(obj); }
	public void visitLRETURN(LRETURN obj) { normal(obj); }
	public void visitFCONST(FCONST obj) { normal(obj); }
	public void visitIUSHR(IUSHR obj) { normal(obj); }
	public void visitBALOAD(BALOAD obj) { normal(obj); }
	public void visitDUP2(DUP2 obj) {
		try {
			ThisValue value1 = frame.popValue();
			ThisValue value2 = frame.popValue();
			frame.pushValue(value2);
			frame.pushValue(value1);
			frame.pushValue(value2);
			frame.pushValue(value1);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}
	public void visitIF_ACMPEQ(IF_ACMPEQ obj) { normal(obj); }
	public void visitIMPDEP1(IMPDEP1 obj) { barf(); }
	public void visitMONITORENTER(MONITORENTER obj) { normal(obj); }
	public void visitLSHL(LSHL obj) { normal(obj); }
	public void visitDCMPG(DCMPG obj) { normal(obj); }
	public void visitD2L(D2L obj) { normal(obj); }
	public void visitIMPDEP2(IMPDEP2 obj) { barf(); }
	public void visitL2D(L2D obj) { normal(obj); }
	public void visitRET(RET obj) { normal(obj); }
	public void visitIFGT(IFGT obj) { normal(obj); }
	public void visitIXOR(IXOR obj) { normal(obj); }
	public void visitINVOKEVIRTUAL(INVOKEVIRTUAL obj) { normal(obj); }
	public void visitFASTORE(FASTORE obj) { normal(obj); }
	public void visitIRETURN(IRETURN obj) { normal(obj); }
	public void visitIF_ICMPNE(IF_ICMPNE obj) { normal(obj); }
	public void visitFLOAD(FLOAD obj) { normal(obj); } // only ALOAD can move "this"
	public void visitLDIV(LDIV obj) { normal(obj); }
	public void visitPUTSTATIC(PUTSTATIC obj) { normal(obj); }
	public void visitAALOAD(AALOAD obj) { normal(obj); }
	public void visitD2I(D2I obj) { normal(obj); }
	public void visitIF_ICMPEQ(IF_ICMPEQ obj) { normal(obj); }
	public void visitAASTORE(AASTORE obj) { normal(obj); }
	public void visitARETURN(ARETURN obj) { normal(obj); }
	public void visitDUP2_X1(DUP2_X1 obj) {
		try {
			ThisValue value1 = frame.popValue();
			ThisValue value2 = frame.popValue();
			ThisValue value3 = frame.popValue();
			frame.pushValue(value2);
			frame.pushValue(value1);
			frame.pushValue(value3);
			frame.pushValue(value2);
			frame.pushValue(value1);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}
	public void visitFNEG(FNEG obj) { normal(obj); }
	public void visitGOTO_W(GOTO_W obj) { normal(obj); }
	public void visitD2F(D2F obj) { normal(obj); }
	public void visitGOTO(GOTO obj) { normal(obj); }
	public void visitISUB(ISUB obj) { normal(obj); }
	public void visitF2I(F2I obj) { normal(obj); }
	public void visitDNEG(DNEG obj) { normal(obj); }
	public void visitICONST(ICONST obj) { normal(obj); }
	public void visitFDIV(FDIV obj) { normal(obj); }
	public void visitI2B(I2B obj) { normal(obj); }
	public void visitLNEG(LNEG obj) { normal(obj); }
	public void visitLREM(LREM obj) { normal(obj); }
	public void visitIMUL(IMUL obj) { normal(obj); }
	public void visitIADD(IADD obj) { normal(obj); }
	public void visitLSHR(LSHR obj) { normal(obj); }
	public void visitLOOKUPSWITCH(LOOKUPSWITCH obj) { normal(obj); }
	public void visitDUP_X1(DUP_X1 obj) {
		try {
			ThisValue value1 = frame.popValue();
			ThisValue value2 = frame.popValue();
			frame.pushValue(value1);
			frame.pushValue(value2);
			frame.pushValue(value1);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}
	public void visitFCMPL(FCMPL obj) { normal(obj); }
	public void visitI2C(I2C obj) { normal(obj); }
	public void visitLMUL(LMUL obj) { normal(obj); }
	public void visitLUSHR(LUSHR obj) { normal(obj); }
	public void visitISHL(ISHL obj) { normal(obj); }
	public void visitLALOAD(LALOAD obj) { normal(obj); }
	public void visitASTORE(ASTORE obj) {
		try {
			ThisValue value = frame.popValue();
			int local = obj.getIndex();
			frame.setValue(local, value);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}
	public void visitANEWARRAY(ANEWARRAY obj) { normal(obj); }
	public void visitFRETURN(FRETURN obj) { normal(obj); }
	public void visitFADD(FADD obj) { normal(obj); }
	public void visitBREAKPOINT(BREAKPOINT obj) { barf(); }
}

// vim:ts=4
