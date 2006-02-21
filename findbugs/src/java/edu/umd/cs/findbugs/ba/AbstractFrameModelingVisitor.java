/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.*;

/**
 * A common base class for frame modeling visitors.
 * This class provides a default implementation which copies values
 * between frame slots whenever appropriate.  For example, its handler
 * for the ALOAD bytecode will get the value from the referenced
 * local in the frame and push it onto the stack.  Bytecodes which
 * do something other than copying values are modeled by popping
 * values as appropriate, and pushing the "default" value onto the stack
 * for each stack slot produced, where the default value is the one
 * returned by the getDefaultValue() method.
 * <p/>
 * <p> Subclasses should override the visit methods for any bytecode instructions
 * which require special handling.
 * </p>
 * <p>
 * Users of AbstractFrameModelingVisitors should call the
 * analyzeInstruction() method instead of directly using the accept()
 * method of the instruction.  This allows a checked DataflowAnalysisException
 * to be thrown when invalid bytecode is detected.  E.g.,
 * stack underflows.
 * </p>
 *
 * @author David Hovemeyer
 * @see Frame
 * @see DataflowAnalysis
 */
public abstract class AbstractFrameModelingVisitor <Value, FrameType extends Frame<Value>> implements Visitor {
	private FrameType frame;
	private Location location;
	protected ConstantPoolGen cpg;

	/**
	 * Constructor.
	 *
	 * @param cpg the ConstantPoolGen of the method to be analyzed
	 */
	public AbstractFrameModelingVisitor(ConstantPoolGen cpg) {
		this.frame = null;
		this.cpg = cpg;
	}
	
	/**
	 * Analyze the given Instruction.
	 * 
	 * @param ins the Instruction
	 * @throws DataflowAnalysisException if an error occurs analyzing the instruction;
	 *                                   in most cases, this indicates that the bytecode
	 *                                   for the method being analyzed is invalid
	 */
	public void analyzeInstruction(Instruction ins) throws DataflowAnalysisException {
		try {
			ins.accept(this);
		} catch (InvalidBytecodeException e) {
			System.out.println("Could not analyze " + ins);
			e.printStackTrace(System.out);
			throw new DataflowAnalysisException("Invalid bytecode", e);
		}
	}

	/**
	 * Get the ConstantPoolGen for the method.
	 */
	public ConstantPoolGen getCPG() {
		return cpg;
	}

	/**
	 * Set the frame and Location for the instruction about to
	 * be modeled.
	 *
	 * @param frame    the Frame
	 * @param location the Location
	 */
	public void setFrameAndLocation(FrameType frame, Location location) {
		this.frame = frame;
		this.location = location;
	}

	/**
	 * Get the frame.
	 *
	 * @return the Frame object
	 */
	public FrameType getFrame() {
		return frame;
	}
	
	/**
	 * Get the Location.
	 * 
	 * @return the Location
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * Produce a "default" value.
	 * This is what is pushed onto the stack by the
	 * handleNormalInstruction() method for instructions which produce stack values.
	 */
	public abstract Value getDefaultValue();

	/**
	 * Get the number of words consumed by given instruction.
	 */
	public int getNumWordsConsumed(Instruction ins) {
		int numWordsConsumed = ins.consumeStack(cpg);
		if (numWordsConsumed == Constants.UNPREDICTABLE)
			throw new InvalidBytecodeException("Unpredictable stack consumption");
		return numWordsConsumed;
	}

	/**
	 * Get the number of words produced by given instruction.
	 */
	public int getNumWordsProduced(Instruction ins) {
		int numWordsProduced = ins.produceStack(cpg);
		if (numWordsProduced == Constants.UNPREDICTABLE)
			throw new InvalidBytecodeException("Unpredictable stack productions");
		return numWordsProduced;
	}

	/**
	 * This is called for illegal bytecodes.
	 *
	 * @throws InvalidBytecodeException
	 */
	private void illegalBytecode(Instruction ins) {
		throw new InvalidBytecodeException("Illegal bytecode: " + ins);
	}

	/* ---------------------------------------------------------------------- 
	 * Empty visit methods
	 * ---------------------------------------------------------------------- */

	public void visitStackInstruction(StackInstruction obj) {
	}

	public void visitLocalVariableInstruction(LocalVariableInstruction obj) {
	}

	public void visitBranchInstruction(BranchInstruction obj) {
	}

	public void visitLoadClass(LoadClass obj) {
	}

	public void visitFieldInstruction(FieldInstruction obj) {
	}

	public void visitIfInstruction(IfInstruction obj) {
	}

	public void visitConversionInstruction(ConversionInstruction obj) {
	}

	public void visitPopInstruction(PopInstruction obj) {
	}

	public void visitJsrInstruction(JsrInstruction obj) {
	}

	public void visitGotoInstruction(GotoInstruction obj) {
	}

	public void visitStoreInstruction(StoreInstruction obj) {
	}

	public void visitTypedInstruction(TypedInstruction obj) {
	}

	public void visitSelect(Select obj) {
	}

	public void visitUnconditionalBranch(UnconditionalBranch obj) {
	}

	public void visitPushInstruction(PushInstruction obj) {
	}

	public void visitArithmeticInstruction(ArithmeticInstruction obj) {
	}

	public void visitCPInstruction(CPInstruction obj) {
	}

	public void visitInvokeInstruction(InvokeInstruction obj) {
	}

	public void visitArrayInstruction(ArrayInstruction obj) {
	}

	public void visitAllocationInstruction(AllocationInstruction obj) {
	}

	public void visitReturnInstruction(ReturnInstruction obj) {
	}

	public void visitFieldOrMethod(FieldOrMethod obj) {
	}

	public void visitConstantPushInstruction(ConstantPushInstruction obj) {
	}

	public void visitExceptionThrower(ExceptionThrower obj) {
	}

	public void visitLoadInstruction(LoadInstruction obj) {
	}

	public void visitVariableLengthInstruction(VariableLengthInstruction obj) {
	}

	public void visitStackProducer(StackProducer obj) {
	}

	public void visitStackConsumer(StackConsumer obj) {
	}

	/* ---------------------------------------------------------------------- 
	 * General instruction handlers
	 * ---------------------------------------------------------------------- */

	/**
	 * Handler for all instructions which pop values from the stack
	 * and store them in a local variable.  Note that two locals
	 * are stored into for long and double stores.
	 */
	public void handleStoreInstruction(StoreInstruction obj) {
		try {
			int numConsumed = obj.consumeStack(cpg);
			if (numConsumed == Constants.UNPREDICTABLE)
				throw new InvalidBytecodeException("Unpredictable stack consumption");

			int index = obj.getIndex();

			// Store values into consecutive locals corresponding
			// to the order in which the values appeared on the stack.
			while (numConsumed-- > 0) {
				Value value = frame.popValue();
				frame.setValue(index++, value);
			}
		} catch (DataflowAnalysisException e) {
			throw new InvalidBytecodeException(e.toString());
		}
	}

	/**
	 * Handler for all instructions which load values from a local variable
	 * and push them on the stack.  Note that two locals are loaded for
	 * long and double loads.
	 */
	public void handleLoadInstruction(LoadInstruction obj) {
		int numProduced = obj.produceStack(cpg);
		if (numProduced == Constants.UNPREDICTABLE)
			throw new InvalidBytecodeException("Unpredictable stack production");

		int index = obj.getIndex() + numProduced;

		// Load values from locals in reverse order.
		// This restores them to the stack in a way consistent
		// with visitStoreInstruction().
		while (numProduced-- > 0) {
			Value value = frame.getValue(--index);
			frame.pushValue(value);
		}
	}

	/**
	 * This is called to handle any instruction which does not simply
	 * copy values between stack slots.  The default value
	 * is pushed (if the instruction is a stack producer).
	 */
	public void handleNormalInstruction(Instruction ins) {
		modelNormalInstruction(ins, getNumWordsConsumed(ins), getNumWordsProduced(ins));
	}

	/**
	 * Model the stack for instructions handled by handleNormalInstruction().
	 * Subclasses may override to provide analysis-specific behavior.
	 * 
	 * @param ins              the Instruction to model
	 * @param numWordsConsumed number of stack words consumed
	 * @param numWordsProduced number of stack words produced
	 */
	public void modelNormalInstruction(
			Instruction ins,
			int numWordsConsumed,
			int numWordsProduced) {
		modelInstruction(ins, numWordsConsumed, numWordsProduced, getDefaultValue());
	}

	/**
	 * Primitive to model the stack effect of a single instruction.
	 * 
	 * @param ins              the Instruction to model
	 * @param numWordsConsumed number of stack words consumed
	 * @param numWordsProduced number of stack words produced
	 * @param pushValue        value to push on the stack
	 */
	public void modelInstruction(
			Instruction ins,
			int numWordsConsumed,
			int numWordsProduced,
			Value pushValue) {
		try {
			while (numWordsConsumed-- > 0)
				frame.popValue();
		} catch (DataflowAnalysisException e) {
			throw new InvalidBytecodeException(e.toString());
		}

		while (numWordsProduced-- > 0)
			frame.pushValue(pushValue);
	}

	/* ---------------------------------------------------------------------- 
	 * Visit methods for scalar STORE instructions
	 * ---------------------------------------------------------------------- */

	public void visitASTORE(ASTORE obj) {
		handleStoreInstruction(obj);
	}

	public void visitDSTORE(DSTORE obj) {
		handleStoreInstruction(obj);
	}

	public void visitFSTORE(FSTORE obj) {
		handleStoreInstruction(obj);
	}

	public void visitISTORE(ISTORE obj) {
		handleStoreInstruction(obj);
	}

	public void visitLSTORE(LSTORE obj) {
		handleStoreInstruction(obj);
	}

	/* ---------------------------------------------------------------------- 
	 * Visit methods for scalar LOAD instructions
	 * ---------------------------------------------------------------------- */

	public void visitALOAD(ALOAD obj) {
		handleLoadInstruction(obj);
	}

	public void visitDLOAD(DLOAD obj) {
		handleLoadInstruction(obj);
	}

	public void visitFLOAD(FLOAD obj) {
		handleLoadInstruction(obj);
	}

	public void visitILOAD(ILOAD obj) {
		handleLoadInstruction(obj);
	}

	public void visitLLOAD(LLOAD obj) {
		handleLoadInstruction(obj);
	}

	/* ---------------------------------------------------------------------- 
	 * Visit methods for POP, DUP, and SWAP instructions
	 * ---------------------------------------------------------------------- */

	public void visitPOP(POP obj) {
		handleNormalInstruction(obj);
	}

	public void visitPOP2(POP2 obj) {
		handleNormalInstruction(obj);
	}

	public void visitDUP(DUP obj) {
		try {
			Value value = frame.popValue();
			frame.pushValue(value);
			frame.pushValue(value);
		} catch (DataflowAnalysisException e) {
			throw new InvalidBytecodeException(e.toString());
		}
	}

	public void visitDUP_X1(DUP_X1 obj) {
		try {
			Value value1 = frame.popValue();
			Value value2 = frame.popValue();
			frame.pushValue(value1);
			frame.pushValue(value2);
			frame.pushValue(value1);
		} catch (DataflowAnalysisException e) {
			throw new InvalidBytecodeException(e.toString());
		}
	}

	public void visitDUP_X2(DUP_X2 obj) {
		try {
			Value value1 = frame.popValue();
			Value value2 = frame.popValue();
			Value value3 = frame.popValue();
			frame.pushValue(value1);
			frame.pushValue(value3);
			frame.pushValue(value2);
			frame.pushValue(value1);
		} catch (DataflowAnalysisException e) {
			throw new InvalidBytecodeException(e.toString());
		}
	}

	public void visitDUP2(DUP2 obj) {
		try {
			Value value1 = frame.popValue();
			Value value2 = frame.popValue();
			frame.pushValue(value2);
			frame.pushValue(value1);
			frame.pushValue(value2);
			frame.pushValue(value1);
		} catch (DataflowAnalysisException e) {
			throw new InvalidBytecodeException(e.toString());
		}
	}

	public void visitDUP2_X1(DUP2_X1 obj) {
		try {
			Value value1 = frame.popValue();
			Value value2 = frame.popValue();
			Value value3 = frame.popValue();
			frame.pushValue(value2);
			frame.pushValue(value1);
			frame.pushValue(value3);
			frame.pushValue(value2);
			frame.pushValue(value1);
		} catch (DataflowAnalysisException e) {
			throw new InvalidBytecodeException(e.toString());
		}
	}

	public void visitDUP2_X2(DUP2_X2 obj) {
		try {
			Value value1 = frame.popValue();
			Value value2 = frame.popValue();
			Value value3 = frame.popValue();
			Value value4 = frame.popValue();
			frame.pushValue(value2);
			frame.pushValue(value1);
			frame.pushValue(value4);
			frame.pushValue(value3);
			frame.pushValue(value2);
			frame.pushValue(value1);
		} catch (DataflowAnalysisException e) {
			throw new InvalidBytecodeException(e.toString());
		}
	}

	public void visitSWAP(SWAP obj) {
		try {
			Value value1 = frame.popValue();
			Value value2 = frame.popValue();
			frame.pushValue(value1);
			frame.pushValue(value2);
		} catch (DataflowAnalysisException e) {
			throw new InvalidBytecodeException(e.toString());
		}
	}

	/* ----------------------------------------------------------------------
	 * Illegal bytecodes
	 * ---------------------------------------------------------------------- */

	public void visitIMPDEP1(IMPDEP1 obj) {
		illegalBytecode(obj);
	}

	public void visitIMPDEP2(IMPDEP2 obj) {
		illegalBytecode(obj);
	}

	public void visitBREAKPOINT(BREAKPOINT obj) {
		illegalBytecode(obj);
	}

	/* ----------------------------------------------------------------------
	 * Bytecodes that have "default" semantics
	 * ---------------------------------------------------------------------- */

	public void visitACONST_NULL(ACONST_NULL obj) {
		handleNormalInstruction(obj);
	}

	public void visitGETSTATIC(GETSTATIC obj) {
		handleNormalInstruction(obj);
	}

	public void visitIF_ICMPLT(IF_ICMPLT obj) {
		handleNormalInstruction(obj);
	}

	public void visitMONITOREXIT(MONITOREXIT obj) {
		handleNormalInstruction(obj);
	}

	public void visitIFLT(IFLT obj) {
		handleNormalInstruction(obj);
	}

	public void visitBASTORE(BASTORE obj) {
		handleNormalInstruction(obj);
	}

	public void visitCHECKCAST(CHECKCAST obj) {
		handleNormalInstruction(obj);
	}

	public void visitFCMPG(FCMPG obj) {
		handleNormalInstruction(obj);
	}

	public void visitI2F(I2F obj) {
		handleNormalInstruction(obj);
	}

	public void visitATHROW(ATHROW obj) {
		handleNormalInstruction(obj);
	}

	public void visitDCMPL(DCMPL obj) {
		handleNormalInstruction(obj);
	}

	public void visitARRAYLENGTH(ARRAYLENGTH obj) {
		handleNormalInstruction(obj);
	}

	public void visitINVOKESTATIC(INVOKESTATIC obj) {
		handleNormalInstruction(obj);
	}

	public void visitLCONST(LCONST obj) {
		handleNormalInstruction(obj);
	}

	public void visitDREM(DREM obj) {
		handleNormalInstruction(obj);
	}

	public void visitIFGE(IFGE obj) {
		handleNormalInstruction(obj);
	}

	public void visitCALOAD(CALOAD obj) {
		handleNormalInstruction(obj);
	}

	public void visitLASTORE(LASTORE obj) {
		handleNormalInstruction(obj);
	}

	public void visitI2D(I2D obj) {
		handleNormalInstruction(obj);
	}

	public void visitDADD(DADD obj) {
		handleNormalInstruction(obj);
	}

	public void visitINVOKESPECIAL(INVOKESPECIAL obj) {
		handleNormalInstruction(obj);
	}

	public void visitIAND(IAND obj) {
		handleNormalInstruction(obj);
	}

	public void visitPUTFIELD(PUTFIELD obj) {
		handleNormalInstruction(obj);
	}

	public void visitDCONST(DCONST obj) {
		handleNormalInstruction(obj);
	}

	public void visitNEW(NEW obj) {
		handleNormalInstruction(obj);
	}

	public void visitIFNULL(IFNULL obj) {
		handleNormalInstruction(obj);
	}

	public void visitLSUB(LSUB obj) {
		handleNormalInstruction(obj);
	}

	public void visitL2I(L2I obj) {
		handleNormalInstruction(obj);
	}

	public void visitISHR(ISHR obj) {
		handleNormalInstruction(obj);
	}

	public void visitTABLESWITCH(TABLESWITCH obj) {
		handleNormalInstruction(obj);
	}

	public void visitIINC(IINC obj) {
		handleNormalInstruction(obj);
	}

	public void visitDRETURN(DRETURN obj) {
		handleNormalInstruction(obj);
	}

	public void visitDASTORE(DASTORE obj) {
		handleNormalInstruction(obj);
	}

	public void visitIALOAD(IALOAD obj) {
		handleNormalInstruction(obj);
	}

	public void visitDDIV(DDIV obj) {
		handleNormalInstruction(obj);
	}

	public void visitIF_ICMPGE(IF_ICMPGE obj) {
		handleNormalInstruction(obj);
	}

	public void visitLAND(LAND obj) {
		handleNormalInstruction(obj);
	}

	public void visitIDIV(IDIV obj) {
		handleNormalInstruction(obj);
	}

	public void visitLOR(LOR obj) {
		handleNormalInstruction(obj);
	}

	public void visitCASTORE(CASTORE obj) {
		handleNormalInstruction(obj);
	}

	public void visitFREM(FREM obj) {
		handleNormalInstruction(obj);
	}

	public void visitLDC(LDC obj) {
		handleNormalInstruction(obj);
	}

	public void visitBIPUSH(BIPUSH obj) {
		handleNormalInstruction(obj);
	}

	public void visitF2L(F2L obj) {
		handleNormalInstruction(obj);
	}

	public void visitFMUL(FMUL obj) {
		handleNormalInstruction(obj);
	}

	public void visitJSR(JSR obj) {
		handleNormalInstruction(obj);
	}

	public void visitFSUB(FSUB obj) {
		handleNormalInstruction(obj);
	}

	public void visitSASTORE(SASTORE obj) {
		handleNormalInstruction(obj);
	}

	public void visitRETURN(RETURN obj) {
		handleNormalInstruction(obj);
	}

	public void visitDALOAD(DALOAD obj) {
		handleNormalInstruction(obj);
	}

	public void visitSIPUSH(SIPUSH obj) {
		handleNormalInstruction(obj);
	}

	public void visitDSUB(DSUB obj) {
		handleNormalInstruction(obj);
	}

	public void visitL2F(L2F obj) {
		handleNormalInstruction(obj);
	}

	public void visitIF_ICMPGT(IF_ICMPGT obj) {
		handleNormalInstruction(obj);
	}

	public void visitF2D(F2D obj) {
		handleNormalInstruction(obj);
	}

	public void visitI2L(I2L obj) {
		handleNormalInstruction(obj);
	}

	public void visitIF_ACMPNE(IF_ACMPNE obj) {
		handleNormalInstruction(obj);
	}

	public void visitI2S(I2S obj) {
		handleNormalInstruction(obj);
	}

	public void visitIFEQ(IFEQ obj) {
		handleNormalInstruction(obj);
	}

	public void visitIOR(IOR obj) {
		handleNormalInstruction(obj);
	}

	public void visitIREM(IREM obj) {
		handleNormalInstruction(obj);
	}

	public void visitIASTORE(IASTORE obj) {
		handleNormalInstruction(obj);
	}

	public void visitNEWARRAY(NEWARRAY obj) {
		handleNormalInstruction(obj);
	}

	public void visitINVOKEINTERFACE(INVOKEINTERFACE obj) {
		handleNormalInstruction(obj);
	}

	public void visitINEG(INEG obj) {
		handleNormalInstruction(obj);
	}

	public void visitLCMP(LCMP obj) {
		handleNormalInstruction(obj);
	}

	public void visitJSR_W(JSR_W obj) {
		handleNormalInstruction(obj);
	}

	public void visitMULTIANEWARRAY(MULTIANEWARRAY obj) {
		handleNormalInstruction(obj);
	}

	public void visitSALOAD(SALOAD obj) {
		handleNormalInstruction(obj);
	}

	public void visitIFNONNULL(IFNONNULL obj) {
		handleNormalInstruction(obj);
	}

	public void visitDMUL(DMUL obj) {
		handleNormalInstruction(obj);
	}

	public void visitIFNE(IFNE obj) {
		handleNormalInstruction(obj);
	}

	public void visitIF_ICMPLE(IF_ICMPLE obj) {
		handleNormalInstruction(obj);
	}

	public void visitLDC2_W(LDC2_W obj) {
		handleNormalInstruction(obj);
	}

	public void visitGETFIELD(GETFIELD obj) {
		handleNormalInstruction(obj);
	}

	public void visitLADD(LADD obj) {
		handleNormalInstruction(obj);
	}

	public void visitNOP(NOP obj) {
		handleNormalInstruction(obj);
	}

	public void visitFALOAD(FALOAD obj) {
		handleNormalInstruction(obj);
	}

	public void visitINSTANCEOF(INSTANCEOF obj) {
		handleNormalInstruction(obj);
	}

	public void visitIFLE(IFLE obj) {
		handleNormalInstruction(obj);
	}

	public void visitLXOR(LXOR obj) {
		handleNormalInstruction(obj);
	}

	public void visitLRETURN(LRETURN obj) {
		handleNormalInstruction(obj);
	}

	public void visitFCONST(FCONST obj) {
		handleNormalInstruction(obj);
	}

	public void visitIUSHR(IUSHR obj) {
		handleNormalInstruction(obj);
	}

	public void visitBALOAD(BALOAD obj) {
		handleNormalInstruction(obj);
	}

	public void visitIF_ACMPEQ(IF_ACMPEQ obj) {
		handleNormalInstruction(obj);
	}

	public void visitMONITORENTER(MONITORENTER obj) {
		handleNormalInstruction(obj);
	}

	public void visitLSHL(LSHL obj) {
		handleNormalInstruction(obj);
	}

	public void visitDCMPG(DCMPG obj) {
		handleNormalInstruction(obj);
	}

	public void visitD2L(D2L obj) {
		handleNormalInstruction(obj);
	}

	public void visitL2D(L2D obj) {
		handleNormalInstruction(obj);
	}

	public void visitRET(RET obj) {
		handleNormalInstruction(obj);
	}

	public void visitIFGT(IFGT obj) {
		handleNormalInstruction(obj);
	}

	public void visitIXOR(IXOR obj) {
		handleNormalInstruction(obj);
	}

	public void visitINVOKEVIRTUAL(INVOKEVIRTUAL obj) {
		handleNormalInstruction(obj);
	}

	public void visitFASTORE(FASTORE obj) {
		handleNormalInstruction(obj);
	}

	public void visitIRETURN(IRETURN obj) {
		handleNormalInstruction(obj);
	}

	public void visitIF_ICMPNE(IF_ICMPNE obj) {
		handleNormalInstruction(obj);
	}

	public void visitLDIV(LDIV obj) {
		handleNormalInstruction(obj);
	}

	public void visitPUTSTATIC(PUTSTATIC obj) {
		handleNormalInstruction(obj);
	}

	public void visitAALOAD(AALOAD obj) {
		handleNormalInstruction(obj);
	}

	public void visitD2I(D2I obj) {
		handleNormalInstruction(obj);
	}

	public void visitIF_ICMPEQ(IF_ICMPEQ obj) {
		handleNormalInstruction(obj);
	}

	public void visitAASTORE(AASTORE obj) {
		handleNormalInstruction(obj);
	}

	public void visitARETURN(ARETURN obj) {
		handleNormalInstruction(obj);
	}

	public void visitFNEG(FNEG obj) {
		handleNormalInstruction(obj);
	}

	public void visitGOTO_W(GOTO_W obj) {
		handleNormalInstruction(obj);
	}

	public void visitD2F(D2F obj) {
		handleNormalInstruction(obj);
	}

	public void visitGOTO(GOTO obj) {
		handleNormalInstruction(obj);
	}

	public void visitISUB(ISUB obj) {
		handleNormalInstruction(obj);
	}

	public void visitF2I(F2I obj) {
		handleNormalInstruction(obj);
	}

	public void visitDNEG(DNEG obj) {
		handleNormalInstruction(obj);
	}

	public void visitICONST(ICONST obj) {
		handleNormalInstruction(obj);
	}

	public void visitFDIV(FDIV obj) {
		handleNormalInstruction(obj);
	}

	public void visitI2B(I2B obj) {
		handleNormalInstruction(obj);
	}

	public void visitLNEG(LNEG obj) {
		handleNormalInstruction(obj);
	}

	public void visitLREM(LREM obj) {
		handleNormalInstruction(obj);
	}

	public void visitIMUL(IMUL obj) {
		handleNormalInstruction(obj);
	}

	public void visitIADD(IADD obj) {
		handleNormalInstruction(obj);
	}

	public void visitLSHR(LSHR obj) {
		handleNormalInstruction(obj);
	}

	public void visitLOOKUPSWITCH(LOOKUPSWITCH obj) {
		handleNormalInstruction(obj);
	}

	public void visitFCMPL(FCMPL obj) {
		handleNormalInstruction(obj);
	}

	public void visitI2C(I2C obj) {
		handleNormalInstruction(obj);
	}

	public void visitLMUL(LMUL obj) {
		handleNormalInstruction(obj);
	}

	public void visitLUSHR(LUSHR obj) {
		handleNormalInstruction(obj);
	}

	public void visitISHL(ISHL obj) {
		handleNormalInstruction(obj);
	}

	public void visitLALOAD(LALOAD obj) {
		handleNormalInstruction(obj);
	}

	public void visitANEWARRAY(ANEWARRAY obj) {
		handleNormalInstruction(obj);
	}

	public void visitFRETURN(FRETURN obj) {
		handleNormalInstruction(obj);
	}

	public void visitFADD(FADD obj) {
		handleNormalInstruction(obj);
	}
}

// vim:ts=4
