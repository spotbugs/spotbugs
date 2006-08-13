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

package edu.umd.cs.findbugs.ba.type;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.*;

import edu.umd.cs.findbugs.ba.AbstractFrameModelingVisitor;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Debug;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.InvalidBytecodeException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

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
			
	private ValueNumberDataflow valueNumberDataflow;

	private short lastOpcode;
	private boolean instanceOfFollowedByBranch;
	private Type instanceOfType;
	private ValueNumber instanceOfValueNumber;
	
	private FieldStoreTypeDatabase database;

	/**
	 * Constructor.
	 *
	 * @param cpg the ConstantPoolGen of the method whose instructions we are examining
	 */
	public TypeFrameModelingVisitor(ConstantPoolGen cpg) {
		super(cpg);
	}

	/**
	 * Set ValueNumberDataflow for the method being analyzed.
	 * This is optional; if set, we will use the information to more
	 * accurately model the effects of instanceof instructions.
	 * 
	 * @param valueNumberDataflow the ValueNumberDataflow
	 */
	public void setValueNumberDataflow(ValueNumberDataflow valueNumberDataflow) {
		this.valueNumberDataflow = valueNumberDataflow;
	}
	
	/**
	 * Get the last opcode analyzed by this visitor.
	 * The TypeAnalysis may use this to get more precise types in
	 * the resulting frame.
	 * 
	 * @return the last opcode analyzed by this visitor
	 */
	public short getLastOpcode() {
		return lastOpcode;
	}
	
	/**
	 * Return whether an instanceof instruction was followed by a branch.
	 * The TypeAnalysis may use this to get more precise types in
	 * the resulting frame.
	 * 
	 * @return true if an instanceof instruction was followed by a branch,
	 *         false if not
	 */
	public boolean isInstanceOfFollowedByBranch() {
		return instanceOfFollowedByBranch;
	}
	
	/**
	 * Get the type of the most recent instanceof instruction modeled.
	 * The TypeAnalysis may use this to get more precise types in
	 * the resulting frame.
	 * 
	 * @return the Type checked by the most recent instanceof instruction
	 */
	public Type getInstanceOfType() {
		return instanceOfType;
	}
	
	/**
	 * Get the value number of the most recent instanceof instruction modeled.
	 * The TypeAnalysis may use this to get more precise types in
	 * the resulting frame.
	 * 
	 * @return the ValueNumber checked by the most recent instanceof instruction
	 */
	public ValueNumber getInstanceOfValueNumber() {
		return instanceOfValueNumber;
	}
	
	/**
	 * Set the field store type database.
	 * We can use this to get more accurate types for values loaded
	 * from fields.
	 * 
	 * @param database the FieldStoreTypeDatabase
	 */
	public void setFieldStoreTypeDatabase(FieldStoreTypeDatabase database) {
		this.database = database;
	}

	@Override
         public Type getDefaultValue() {
		return TypeFrame.getBottomType();
	}
	
	@Override
         public void analyzeInstruction(Instruction ins) throws DataflowAnalysisException {
		Location location = getLocation();
		
		if (location.isFirstInstructionInBasicBlock()) {
			startBasicBlock();
		}
		
		instanceOfFollowedByBranch = false;
		super.analyzeInstruction(ins);
		lastOpcode = ins.getOpcode();
		
//		if (location.isLastInstructionInBasicBlock()) {
//			endBasicBlock();
//		}
	}

	private void startBasicBlock() {
		lastOpcode = -1;
		instanceOfType = null;
		instanceOfValueNumber = null;
//		getFrame().clearInstanceOfValueNumberAndType();
	}

//	private void endBasicBlock() {
//		if (instanceOfFollowedByBranch) {
//			getFrame().setInstanceOfValueNumberAndType(instanceOfValueNumber, instanceOfType);
//		}
//	}

	/**
	 * Consume stack.  This is a convenience method for instructions
	 * where the types of popped operands can be ignored.
	 */
	protected void consumeStack(Instruction ins) {
		ConstantPoolGen cpg = getCPG();
		TypeFrame frame = getFrame();

		int numWordsConsumed = ins.consumeStack(cpg);
		if (numWordsConsumed == Constants.UNPREDICTABLE)
			throw new InvalidBytecodeException("Unpredictable stack consumption for " + ins);
		try {
			while (numWordsConsumed-- > 0) {
				frame.popValue();
			}
		} catch (DataflowAnalysisException e) {
			throw new InvalidBytecodeException("Stack underflow for " + ins + ": " + e.getMessage());
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
	@Override
         public void modelNormalInstruction(Instruction ins, int numWordsConsumed, int numWordsProduced) {
		if (VERIFY_INTEGRITY) {
			if (numWordsProduced > 0)
				throw new InvalidBytecodeException("missing visitor method for " + ins);
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
		pushValue(TypeFrame.getNullType());
	}

	@Override
         public void visitDCONST(DCONST obj) {
		pushValue(Type.DOUBLE);
	}

	@Override
         public void visitFCONST(FCONST obj) {
		pushValue(Type.FLOAT);
	}

	@Override
         public void visitICONST(ICONST obj) {
		pushValue(Type.INT);
	}

	@Override
         public void visitLCONST(LCONST obj) {
		pushValue(Type.LONG);
	}

	@Override
         public void visitLDC(LDC obj) {
		pushValue(obj.getType(getCPG()));
	}

	@Override
         public void visitLDC2_W(LDC2_W obj) {
		pushValue(obj.getType(getCPG()));
	}

	@Override
         public void visitBIPUSH(BIPUSH obj) {
		pushValue(Type.INT);
	}

	@Override
         public void visitSIPUSH(SIPUSH obj) {
		pushValue(Type.INT);
	}

	@Override
         public void visitGETSTATIC(GETSTATIC obj) {
		modelFieldLoad(obj);
	}

	@Override
         public void visitGETFIELD(GETFIELD obj) {
		modelFieldLoad(obj);
	}
	
	public void modelFieldLoad(FieldInstruction obj) {
		consumeStack(obj);

		Type loadType = obj.getType(getCPG());
		if (database != null && (loadType instanceof ReferenceType)) {
			// Check the field store type database to see if we can
			// get a more precise type for this load.
			try {
				XField xfield = Hierarchy.findXField(obj, getCPG());
				if (xfield != null) {
					FieldStoreType property = database.getProperty(xfield);
					if (property != null) {
						loadType = property.getLoadType((ReferenceType) loadType);
					}
				}
			} catch (ClassNotFoundException e) {
				AnalysisContext.reportMissingClass(e);
			}
		}
		
		pushValue(loadType);
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
		if (obj.getMethodName(cpg).equals("initCause") && obj.getSignature(cpg).equals("(Ljava/lang/Throwable;)Ljava/lang/Throwable;") && obj.getClassName(cpg).endsWith("Exception")) {
			try {
				TypeFrame frame = getFrame();
				frame.popValue();
				return;
			} catch (DataflowAnalysisException e) {
				
			}
		}
		consumeStack(obj);
		pushReturnType(obj);
	}

	@Override
         public void visitCHECKCAST(CHECKCAST obj) {
		consumeStack(obj);
		pushValue(obj.getType(getCPG()));
	}

	@Override
         public void visitINSTANCEOF(INSTANCEOF obj) {
		if (valueNumberDataflow != null) {
			// Record the value number of the value checked by this instruction,
			// and the type the value was compared to.
			try {
				ValueNumberFrame vnaFrame = valueNumberDataflow.getFactAtLocation(getLocation());
				if (vnaFrame.isValid()) {
					instanceOfValueNumber = vnaFrame.getTopValue();
					instanceOfType = obj.getType(getCPG());
				}
			} catch (DataflowAnalysisException e) {
				// Ignore
			}
		}
		
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitFCMPL(FCMPL obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitFCMPG(FCMPG obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitDCMPL(DCMPL obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitDCMPG(DCMPG obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitLCMP(LCMP obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitD2F(D2F obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
         public void visitD2I(D2I obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitD2L(D2L obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
         public void visitF2D(F2D obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
         public void visitF2I(F2I obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitF2L(F2L obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
         public void visitI2B(I2B obj) {
		consumeStack(obj);
		pushValue(Type.BYTE);
	}

	@Override
         public void visitI2C(I2C obj) {
		consumeStack(obj);
		pushValue(Type.CHAR);
	}

	@Override
         public void visitI2D(I2D obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
         public void visitI2F(I2F obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
         public void visitI2L(I2L obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
         public void visitI2S(I2S obj) {
	} // no change

	@Override
         public void visitL2D(L2D obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
         public void visitL2F(L2F obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
         public void visitL2I(L2I obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitIAND(IAND obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitLAND(LAND obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
         public void visitIOR(IOR obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitLOR(LOR obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
         public void visitIXOR(IXOR obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitLXOR(LXOR obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
         public void visitISHR(ISHR obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitIUSHR(IUSHR obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitLSHR(LSHR obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
         public void visitLUSHR(LUSHR obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
         public void visitISHL(ISHL obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitLSHL(LSHL obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
         public void visitDADD(DADD obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
         public void visitFADD(FADD obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
         public void visitIADD(IADD obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitLADD(LADD obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
         public void visitDSUB(DSUB obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
         public void visitFSUB(FSUB obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
         public void visitISUB(ISUB obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitLSUB(LSUB obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
         public void visitDMUL(DMUL obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
         public void visitFMUL(FMUL obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
         public void visitIMUL(IMUL obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitLMUL(LMUL obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
         public void visitDDIV(DDIV obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
         public void visitFDIV(FDIV obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
         public void visitIDIV(IDIV obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitLDIV(LDIV obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
         public void visitDREM(DREM obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
         public void visitFREM(FREM obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
         public void visitIREM(IREM obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitLREM(LREM obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
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
		pushValue(Type.INT);
	}

	@Override
         public void visitLNEG(LNEG obj) {
	} // no change

	@Override
         public void visitARRAYLENGTH(ARRAYLENGTH obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
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
			throw new InvalidBytecodeException("Stack underflow: " + e.getMessage());
		}
	}

	@Override
         public void visitBALOAD(BALOAD obj) {
		consumeStack(obj);
		pushValue(Type.BYTE);
	}

	@Override
         public void visitCALOAD(CALOAD obj) {
		consumeStack(obj);
		pushValue(Type.CHAR);
	}

	@Override
         public void visitDALOAD(DALOAD obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
         public void visitFALOAD(FALOAD obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
         public void visitIALOAD(IALOAD obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
         public void visitLALOAD(LALOAD obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
         public void visitSALOAD(SALOAD obj) {
		consumeStack(obj);
		pushValue(Type.SHORT);
	}

	// The various xASTORE instructions only consume stack.

	@Override
         public void visitNEW(NEW obj) {
		// FIXME: type is technically "uninitialized"
		// However, we don't model that yet.
		pushValue(obj.getType(getCPG()));
		
		// We now have an exact type for this value.
		setTopOfStackIsExact();
	}

	@Override
         public void visitNEWARRAY(NEWARRAY obj) {
		consumeStack(obj);
		Type elementType = obj.getType();
		pushValue(new ArrayType(elementType, 1));
		
		// We now have an exact type for this value.
		setTopOfStackIsExact();
	}

	@Override
         public void visitANEWARRAY(ANEWARRAY obj) {
		consumeStack(obj);
		Type elementType = obj.getType(getCPG());
		pushValue(new ArrayType(elementType, 1));
		
		// We now have an exact type for this value.
		setTopOfStackIsExact();
	}

	@Override
         public void visitMULTIANEWARRAY(MULTIANEWARRAY obj) {
		consumeStack(obj);
		Type elementType = obj.getType(getCPG());
		pushValue(elementType);
		// We now have an exact type for this value.
		setTopOfStackIsExact();
	}

	private void setTopOfStackIsExact() {
		TypeFrame frame = getFrame();
		frame.setExact(frame.getNumSlots() - 1, true);
	}

	@Override
         public void visitJSR(JSR obj) {
		pushValue(ReturnaddressType.NO_TARGET);
	}

	@Override
         public void visitJSR_W(JSR_W obj) {
		pushValue(ReturnaddressType.NO_TARGET);
	}

	@Override
         public void visitRET(RET obj) {
	} // no change

	@Override
         public void visitIFEQ(IFEQ obj) {
		if (lastOpcode == Constants.INSTANCEOF)
			instanceOfFollowedByBranch = true;
		super.visitIFEQ(obj);
	}

	@Override
         public void visitIFGT(IFGT obj) {
		if (lastOpcode == Constants.INSTANCEOF)
			instanceOfFollowedByBranch = true;
		super.visitIFGT(obj);
	}

	@Override
         public void visitIFLE(IFLE obj) {
		if (lastOpcode == Constants.INSTANCEOF)
			instanceOfFollowedByBranch = true;
		super.visitIFLE(obj);
	}

	@Override
         public void visitIFNE(IFNE obj) {
		if (lastOpcode == Constants.INSTANCEOF)
			instanceOfFollowedByBranch = true;
		super.visitIFNE(obj);
	}

}

// vim:ts=4
