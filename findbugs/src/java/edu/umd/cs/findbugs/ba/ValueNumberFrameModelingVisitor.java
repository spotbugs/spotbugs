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

public class ValueNumberFrameModelingVisitor extends AbstractFrameModelingVisitor<ValueNumber, ValueNumberFrame>
	implements Debug {

	private static final boolean REDUNDANT_LOAD_ELIMINATION = Boolean.getBoolean("vna.rle");

	private ValueNumberFactory factory;
	private ValueNumberCache cache;
	private RepositoryLookupFailureCallback lookupFailureCallback;
	private InstructionHandle handle;

	public ValueNumberFrameModelingVisitor(ConstantPoolGen cpg, ValueNumberFactory factory,
		ValueNumberCache cache, RepositoryLookupFailureCallback lookupFailureCallback) {

		super(cpg);
		this.factory = factory;
		this.cache = cache;
		this.lookupFailureCallback = lookupFailureCallback;
	}

	public ValueNumber getDefaultValue() {
		return factory.createFreshValue();
	}

	/**
	 * Set the instruction handle of the instruction currently being visited.
	 * This must be called before the instruction accepts this visitor!
	 */
	public void setHandle(InstructionHandle handle) {
		this.handle = handle;
	}

	public void modelNormalInstruction(Instruction ins, int numWordsConsumed, int numWordsProduced) {
		ValueNumberFrame frame = getFrame();

		// Get the input operands to this instruction.
		ValueNumber[] inputValueList = popInputValues(frame, numWordsConsumed);

		// See if we have the output operands in the cache.
		// If not, push default (fresh) values for the output,
		// and add them to the cache.
		ValueNumberCache.Entry entry = new ValueNumberCache.Entry(handle, inputValueList);
		ValueNumber[] outputValueList = cache.lookupOutputValues(entry);
		if (outputValueList == null) {
			outputValueList = new ValueNumber[numWordsProduced];
			for (int i = 0; i < numWordsProduced; ++i)
				outputValueList[i] = factory.createFreshValue();
			cache.addOutputValues(entry, outputValueList);
		}

		if (VERIFY_INTEGRITY) {
			if (outputValueList.length != numWordsProduced)
				throw new IllegalStateException("cache produced wrong num words");
		}

		// Push output operands on stack.
		for (int i = 0; i < outputValueList.length; ++i)
			frame.pushValue(outputValueList[i]);
	}

	private ValueNumber[] popInputValues(ValueNumberFrame frame, int numWordsConsumed) {
		ValueNumber[] inputValueList = new ValueNumber[numWordsConsumed];

		// Pop off the input operands.
		try {
			frame.getTopStackWords(inputValueList);
			while (numWordsConsumed-- > 0) {
				frame.popValue();
			}
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException("ValueNumberFrameModelingVisitor caught exception: " + e.toString());
		}

		return inputValueList;
	}

	public void visitGETFIELD(GETFIELD obj) {
		if (REDUNDANT_LOAD_ELIMINATION) {
			ValueNumberFrame frame = getFrame();
	
			try {
				InstanceField instanceField = Lookup.findInstanceField(obj, getCPG());
				if (instanceField != null) {
					ValueNumber reference = frame.getTopValue();
					AvailableLoad availableLoad = new AvailableLoad(reference, instanceField);
					ValueNumber loadedValue = frame.getAvailableLoad(availableLoad);
	
					if (loadedValue != null) {
						// Found an available load!
						frame.popValue();
						frame.pushValue(loadedValue);
						return;
					}
				}
			} catch (ClassNotFoundException e) {
				lookupFailureCallback.reportMissingClass(e);
			} catch (DataflowAnalysisException e) {
				throw new IllegalStateException("ValueNumberFrameModelingVisitor caught exception: " + e.toString());
			}
		}
		handleNormalInstruction(obj);
	}

	public void visitPUTFIELD(PUTFIELD obj) {
		if (REDUNDANT_LOAD_ELIMINATION) {
		}
		handleNormalInstruction(obj);
	}

}

// vim:ts=4
