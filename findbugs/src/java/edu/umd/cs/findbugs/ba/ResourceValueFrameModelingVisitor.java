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

import org.apache.bcel.generic.*;

public abstract class ResourceValueFrameModelingVisitor extends AbstractFrameModelingVisitor<ResourceValue, ResourceValueFrame> {
	public ResourceValueFrameModelingVisitor(ResourceValueFrame frame, ConstantPoolGen cpg) {
		super(frame, cpg);
	}

	public ResourceValue getDefaultValue() {
		return ResourceValue.notInstance();
	}

	// Things to do:
	// Automatically detect when resource instances escape:
	//   - putfield, putstatic
	//   - parameters to invoke, but subclasses may override
	//   - return (areturn)

	protected void handleFieldStore(FieldInstruction ins) {
		try {
			// If the resource instance is stored in a field, then it escapes
			ResourceValueFrame frame = getFrame();
			ResourceValue topValue = frame.getTopValue();
			if (topValue.equals(ResourceValue.instance()))
				frame.setStatus(ResourceValueFrame.ESCAPED);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.toString());
		}
	}

	public void visitPUTFIELD(PUTFIELD putfield) {
		handleFieldStore(putfield);
	}

	public void visitPUTSTATIC(PUTSTATIC putstatic) {
		handleFieldStore(putstatic);
	}

	/**
	 * Override this to handle instructions that it is OK to
	 * pass the resource instance to.
	 */
	protected void handleInvoke(InvokeInstruction inv) {
		ResourceValueFrame frame = getFrame();
		int numSlots = frame.getNumSlots();
		int numConsumed = getNumWordsConsumed(inv);
		for (int i = numSlots - numConsumed; i < numSlots; ++i) {
			ResourceValue value = frame.getValue(i);
			if (value.equals(ResourceValue.instance())) {
				frame.setStatus(ResourceValueFrame.ESCAPED);
				return;
			}
		}
	}

	public void visitINVOKEINSTANCE(INVOKEINTERFACE inv) {
		handleInvoke(inv);
	}

	public void visitINVOKEINTERFACE(INVOKEINTERFACE inv) {
		handleInvoke(inv);
	}

	public void visitINVOKESPECIAL(INVOKESPECIAL inv) {
		handleInvoke(inv);
	}

	public void visitINVOKESTATIC(INVOKESTATIC inv) {
		handleInvoke(inv);
	}

	public void visitARETURN(ARETURN ins) {
		try {
			ResourceValueFrame frame = getFrame();
			ResourceValue topValue = frame.getTopValue();
			if (topValue.equals(ResourceValue.instance()))
				frame.setStatus(ResourceValueFrame.ESCAPED);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.toString());
		}
	}

}

// vim:ts=4
