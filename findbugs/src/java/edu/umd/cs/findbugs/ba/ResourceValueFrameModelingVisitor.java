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

import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.CHECKCAST;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;

public abstract class ResourceValueFrameModelingVisitor extends AbstractFrameModelingVisitor<ResourceValue, ResourceValueFrame> {
	public ResourceValueFrameModelingVisitor(ConstantPoolGen cpg) {
		super(cpg);
	}

	@Override
		 public ResourceValue getDefaultValue() {
		return ResourceValue.notInstance();
	}

	/**
	 * Subclasses must override this to model the effect of the
	 * given instruction on the current frame.
	 */
	public abstract void transferInstruction(InstructionHandle handle, BasicBlock basicBlock) throws DataflowAnalysisException;

	// Things to do:
	// Automatically detect when resource instances escape:
	//   - putfield, putstatic
	//   - parameters to invoke, but subclasses may override
	//   - return (areturn)

	private void handleFieldStore(FieldInstruction ins) {
		try {
			// If the resource instance is stored in a field, then it escapes
			ResourceValueFrame frame = getFrame();
			ResourceValue topValue = frame.getTopValue();
			if (topValue.equals(ResourceValue.instance()))
				frame.setStatus(ResourceValueFrame.ESCAPED);
		} catch (DataflowAnalysisException e) {
			throw new InvalidBytecodeException("Stack underflow", e);
		}

		handleNormalInstruction(ins);
	}

	@Override
		 public void visitPUTFIELD(PUTFIELD putfield) {
		handleFieldStore(putfield);
	}

	@Override
		 public void visitPUTSTATIC(PUTSTATIC putstatic) {
		handleFieldStore(putstatic);
	}

	/**
	 * Override this to check for methods that it is legal to
	 * pass the instance to without the instance escaping.
	 * By default, we consider all methods to be possible escape routes.
	 *
	 * @param inv            the InvokeInstruction to which the resource instance
	 *                       is passed as an argument
	 * @param instanceArgNum the first argument the instance is passed in
	 */
	protected boolean instanceEscapes(InvokeInstruction inv, int instanceArgNum) {
		return true;
	}

	private void handleInvoke(InvokeInstruction inv) {
		ResourceValueFrame frame = getFrame();
		int numSlots = frame.getNumSlots();
		int numConsumed = getNumWordsConsumed(inv);

		// See if the resource instance is passed as an argument
		int instanceArgNum = -1;
		for (int i = numSlots - numConsumed, argCount = 0; i < numSlots; ++i, ++argCount) {
			ResourceValue value = frame.getValue(i);
			if (value.equals(ResourceValue.instance())) {
				instanceArgNum = argCount;
				break;
			}
		}

		if (instanceArgNum >= 0 && instanceEscapes(inv, instanceArgNum))
			frame.setStatus(ResourceValueFrame.ESCAPED);

		handleNormalInstruction(inv);
	}

	@Override
	public void visitCHECKCAST(CHECKCAST obj) {
		try {
			ResourceValueFrame frame = getFrame();
			ResourceValue topValue;

			topValue = frame.getTopValue();

			if (topValue.equals(ResourceValue.instance()))
				frame.setStatus(ResourceValueFrame.ESCAPED);
		} catch (DataflowAnalysisException e) {
			AnalysisContext.logError("Analysis error", e);
		}
	}
	@Override
		 public void visitINVOKEVIRTUAL(INVOKEVIRTUAL inv) {
		handleInvoke(inv);
	}

	@Override
		 public void visitINVOKEINTERFACE(INVOKEINTERFACE inv) {
		handleInvoke(inv);
	}

	@Override
		 public void visitINVOKESPECIAL(INVOKESPECIAL inv) {
		handleInvoke(inv);
	}

	@Override
		 public void visitINVOKESTATIC(INVOKESTATIC inv) {
		handleInvoke(inv);
	}

	@Override
		 public void visitARETURN(ARETURN ins) {
		try {
			ResourceValueFrame frame = getFrame();
			ResourceValue topValue = frame.getTopValue();
			if (topValue.equals(ResourceValue.instance()))
				frame.setStatus(ResourceValueFrame.ESCAPED);
		} catch (DataflowAnalysisException e) {
			throw new InvalidBytecodeException("Stack underflow", e);
		}

		handleNormalInstruction(ins);
	}

}

// vim:ts=4
