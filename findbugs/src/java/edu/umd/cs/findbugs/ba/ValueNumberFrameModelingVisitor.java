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

public class ValueNumberFrameModelingVisitor
	extends AbstractFrameModelingVisitor<ValueNumber, ValueNumberFrame>
	implements Debug, ValueNumberAnalysisFeatures {

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
		ValueNumber[] inputValueList = popInputValues(numWordsConsumed);

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
		pushOutputValues(outputValueList);
	}

	private ValueNumber[] popInputValues(int numWordsConsumed) {
		ValueNumberFrame frame = getFrame();
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

	private void pushOutputValues(ValueNumber[] outputValueList) {
		ValueNumberFrame frame = getFrame();
		for (int i = 0; i < outputValueList.length; ++i)
			frame.pushValue(outputValueList[i]);
	}

	public void visitGETFIELD(GETFIELD obj) {
		if (REDUNDANT_LOAD_ELIMINATION) {
			ValueNumberFrame frame = getFrame();
	
			try {
				InstanceField instanceField = Lookup.findInstanceField(obj, getCPG());
				if (instanceField != null) {
					ValueNumber reference = frame.getTopValue();
					AvailableLoad availableLoad = new AvailableLoad(reference, instanceField);
					ValueNumber[] loadedValue = frame.getAvailableLoad(availableLoad);
	
					if (loadedValue != null) {
						if (RLE_DEBUG) System.out.print("[Found available load " + availableLoad + "]");
						// Found an available load!
						frame.popValue();
						pushOutputValues(loadedValue);
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
			ValueNumberFrame frame = getFrame();
	
			try {
				InstanceField instanceField = Lookup.findInstanceField(obj, getCPG());

				if (instanceField != null) {
					int numWordsConsumed = getNumWordsConsumed(obj);
					ValueNumber[] inputValueList = popInputValues(numWordsConsumed);
					ValueNumber reference = inputValueList[0];
					ValueNumber[] loadedValue = new ValueNumber[inputValueList.length - 1];
					System.arraycopy(inputValueList, 1, loadedValue, 0, inputValueList.length - 1);

					// Kill all previous loads of the same field,
					// in case there is aliasing we don't know about
					frame.killLoadsOfField(instanceField);

					// Forward substitution
					frame.addAvailableLoad(new AvailableLoad(reference, instanceField), loadedValue);

					return;
				}

			} catch (ClassNotFoundException e) {
				lookupFailureCallback.reportMissingClass(e);
			}
			
		}
		handleNormalInstruction(obj);
	}

/*
	public void visitINVOKESTATIC(INVOKESTATIC obj) {
		handleNormalInstruction(obj);
	}

	public void visitINVOKESPECIAL(INVOKESPECIAL obj) {
		handleNormalInstruction(obj);
	}

	public void visitINVOKEINTERFACE(INVOKEINTERFACE obj) {
		handleNormalInstruction(obj);
	}

	public void visitINVOKEVIRTUAL(INVOKEVIRTUAL obj) {
		handleNormalInstruction(obj);
	}
*/

}

// vim:ts=4
