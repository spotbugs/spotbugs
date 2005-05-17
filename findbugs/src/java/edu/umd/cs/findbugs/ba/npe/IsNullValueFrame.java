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

package edu.umd.cs.findbugs.ba.npe;

import java.util.BitSet;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Frame;

public class IsNullValueFrame extends Frame<IsNullValue> {
	private IsNullConditionDecision decision;

	public IsNullValueFrame(int numLocals) {
		super(numLocals);
	}
	
	/**
	 * Get set of arguments passed to the given InvokeInstruction
	 * which might be null.
	 * 
	 * @param invokeInstruction the InvokeInstruction
	 * @param cpg               the ConstantPoolGen
	 * @return BitSet specifying which arguments might be null
	 * @throws DataflowAnalysisException
	 */
	public BitSet getNullArgumentSet(InvokeInstruction invokeInstruction, ConstantPoolGen cpg) throws DataflowAnalysisException {
		BitSet nullArgSet = new BitSet();
		int numArguments = getNumArguments(invokeInstruction, cpg);

		for (int i = 0; i < numArguments; ++i) {
			IsNullValue value = getArgument(invokeInstruction, cpg, i);
			if (value.mightBeNull())
				nullArgSet.set(i);
		}
	
		return nullArgSet;
	}

	public void toExceptionValues() {
		for (int i = 0; i < getNumSlots(); ++i)
			setValue(i, getValue(i).toExceptionValue());
	}

	public void setDecision(IsNullConditionDecision decision) {
		this.decision = decision;
	}

	public IsNullConditionDecision getDecision() {
		return decision;
	}

	public String toString() {
		String result = super.toString();
		if (decision != null) {
			result = result + ", [decision=" + decision.toString() + "]";
		}
		return result;
	}
}

// vim:ts=4
