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

import java.util.*;

import org.apache.bcel.generic.*;

/**
 * Visitor which models the effects of bytecode instructions
 * on value numbers of values in the operand stack frames.
 *
 * @see ValueNumber
 * @see ValueNumberFrame
 * @see ValueNumberAnalaysis
 * @author David Hovemeyer
 */
public class ValueNumberFrameModelingVisitor
        extends AbstractFrameModelingVisitor<ValueNumber, ValueNumberFrame>
        implements Debug, ValueNumberAnalysisFeatures {

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private MethodGen methodGen;
	private ValueNumberFactory factory;
	private ValueNumberCache cache;
	private LoadedFieldSet loadedFieldSet;
	private HashMap<String, ValueNumber> classObjectValueMap;
	private IdentityHashMap<InstructionHandle, ValueNumber> constantValueMap;
	private HashMap<ValueNumber, String> stringConstantMap;
	private RepositoryLookupFailureCallback lookupFailureCallback;
	private InstructionHandle handle;

	/* ----------------------------------------------------------------------
	 * Public interface
	 * ---------------------------------------------------------------------- */

	/**
	 * Constructor.
	 *
	 * @param methodGen             the method being analyzed
	 * @param factory               factory for ValueNumbers for the method
	 * @param cache                 cache of input/output transformations for each instruction
	 * @param loadedFieldSet        fields loaded/stored by each instruction and entire method
	 * @param lookupFailureCallback callback to use to report class lookup failures
	 */
	public ValueNumberFrameModelingVisitor(MethodGen methodGen, ValueNumberFactory factory,
	                                       ValueNumberCache cache,
	                                       LoadedFieldSet loadedFieldSet,
	                                       RepositoryLookupFailureCallback lookupFailureCallback) {

		super(methodGen.getConstantPool());
		this.methodGen = methodGen;
		this.factory = factory;
		this.cache = cache;
		this.loadedFieldSet = loadedFieldSet;
		this.classObjectValueMap = new HashMap<String, ValueNumber>();
		this.constantValueMap = new IdentityHashMap<InstructionHandle, ValueNumber>();
		this.stringConstantMap = new HashMap<ValueNumber, String>();
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

	/* ----------------------------------------------------------------------
	 * Instruction modeling
	 * ---------------------------------------------------------------------- */

	/**
	 * Determine whether redundant load elimination and forward substitution
	 * should be performed for the heap location referenced by
	 * the current instruction.  We don't want to bother doing RLE/FS for
	 * non-reference types.
	 * @return true if we should do RLE/FS for the current instruction, false if not
	 */
	private boolean doRedundantLoadElimination() {
		if (!REDUNDANT_LOAD_ELIMINATION)
			return false;
		XField xfield = loadedFieldSet.getField(handle);
		if (xfield == null)
			return false;
		if (!xfield.isReferenceType())
			return false;

/*
		// NOTYET
		// Don't do FS for fields that are never read
		if (!loadedFieldSet.isLoaded(xfield))
			return false;
*/
		return true;
	}

	/**
	 * This is the default instruction modeling method.
	 */
	public void modelNormalInstruction(Instruction ins, int numWordsConsumed, int numWordsProduced) {
		ValueNumberFrame frame = getFrame();

		int flags = (ins instanceof InvokeInstruction) ? ValueNumber.RETURN_VALUE : 0;

		// Get the input operands to this instruction.
		ValueNumber[] inputValueList = popInputValues(numWordsConsumed);

		// See if we have the output operands in the cache.
		// If not, push default (fresh) values for the output,
		// and add them to the cache.
		ValueNumber[] outputValueList = getOutputValues(inputValueList, numWordsProduced, flags);

		if (VERIFY_INTEGRITY) {
			if (outputValueList.length != numWordsProduced)
				throw new IllegalStateException("cache produced wrong num words");
		}

		// Push output operands on stack.
		pushOutputValues(outputValueList);
	}

	public void visitGETFIELD(GETFIELD obj) {
		if (doRedundantLoadElimination()) {
			ValueNumberFrame frame = getFrame();

			try {
				XField xfield = Hierarchy.findXField(obj, getCPG());
				if (xfield != null) {
					loadInstanceField((InstanceField) xfield, obj);
					return;
				}
			} catch (ClassNotFoundException e) {
				lookupFailureCallback.reportMissingClass(e);
			}
		}
		handleNormalInstruction(obj);
	}

	public void visitPUTFIELD(PUTFIELD obj) {
		if (doRedundantLoadElimination()) {
			try {
				XField xfield = Hierarchy.findXField(obj, getCPG());
				if (xfield != null) {
					storeInstanceField((InstanceField) xfield, obj, false);
					return;
				}
			} catch (ClassNotFoundException e) {
				lookupFailureCallback.reportMissingClass(e);
			}
		}
		handleNormalInstruction(obj);
	}

	private static final ValueNumber[] EMPTY_INPUT_VALUE_LIST = new ValueNumber[0];

	public void visitGETSTATIC(GETSTATIC obj) {
		if (doRedundantLoadElimination()) {
			ValueNumberFrame frame = getFrame();
			ConstantPoolGen cpg = getCPG();

			String fieldName = obj.getName(cpg);
			String fieldSig = obj.getSignature(cpg);

			// Is this an access of a Class object?
			if (fieldName.startsWith("class$") && fieldSig.equals("Ljava/lang/Class;")) {
				String className = fieldName.substring("class$".length()).replace('$', '.');
				if (RLE_DEBUG) System.out.print("[found load of class object " + className + "]");
				ValueNumber value = getClassObjectValue(className);
				frame.pushValue(value);
				return;
			}

			try {
				XField xfield = Hierarchy.findXField(obj, getCPG());
				if (xfield != null) {
					loadStaticField((StaticField) xfield, obj);
					return;
				}
			} catch (ClassNotFoundException e) {
				lookupFailureCallback.reportMissingClass(e);
			}
		}

		handleNormalInstruction(obj);
	}

	public void visitPUTSTATIC(PUTSTATIC obj) {
		if (doRedundantLoadElimination()) {
			try {
				XField xfield = Hierarchy.findXField(obj, getCPG());
				if (xfield != null) {
					storeStaticField((StaticField) xfield, obj, false);
					return;
				}
			} catch (ClassNotFoundException e) {
				lookupFailureCallback.reportMissingClass(e);
			}
		}
		handleNormalInstruction(obj);
	}

	public void visitINVOKESTATIC(INVOKESTATIC obj) {
		if (REDUNDANT_LOAD_ELIMINATION) {
			ConstantPoolGen cpg = getCPG();
			String methodName = obj.getName(cpg);
			String methodSig = obj.getSignature(cpg);

			if (methodName.equals("class$") && methodSig.equals("(Ljava/lang/String;)Ljava/lang/Class;")) {
				// Access of a Class object
				ValueNumberFrame frame = getFrame();
				try {
					ValueNumber arg = frame.getTopValue();
					String className = stringConstantMap.get(arg);
					if (className != null) {
						frame.popValue();
						if (RLE_DEBUG) System.out.print("[found access of class object " + className + "]");
						frame.pushValue(getClassObjectValue(className));
						return;
					}
				} catch (DataflowAnalysisException e) {
					throw new AnalysisException("stack underflow", methodGen, handle, e);
				}
			} else if (Hierarchy.isInnerClassAccess(obj, cpg)) {
				// Possible access of field via an inner-class access method
				XField xfield = loadedFieldSet.getField(handle);
				if (xfield != null /*&& doRedundantLoadElimination(xfield)*/) {
					if (loadedFieldSet.instructionIsLoad(handle)) {
						if (xfield.isStatic())
							loadStaticField((StaticField) xfield, obj);
						else
							loadInstanceField((InstanceField) xfield, obj);
					} else {
						// Some inner class access store methods
						// return the value stored.
						boolean pushValue = !methodSig.endsWith(")V");

						if (xfield.isStatic())
							storeStaticField((StaticField) xfield, obj, pushValue);
						else
							storeInstanceField((InstanceField) xfield, obj, pushValue);
					}
					return;
				}
			}
		}

		handleNormalInstruction(obj);
	}

/*
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

	public void visitLDC(LDC obj) {
		ValueNumber value = constantValueMap.get(handle);
		if (value == null) {
			ConstantPoolGen cpg = getCPG();
			value = factory.createFreshValue();
			constantValueMap.put(handle, value);

			// Keep track of String constants
			Object constantValue = obj.getValue(cpg);
			if (constantValue instanceof String) {
				stringConstantMap.put(value, (String) constantValue);
			}
		}

		getFrame().pushValue(value);
	}

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	/**
	 * Pop the input values for the given instruction from the
	 * current frame.
	 */
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
			throw new AnalysisException("ValueNumberFrameModelingVisitor caught exception: " + e.toString(), e);
		}

		return inputValueList;
	}

	/**
	 * Push given output values onto the current frame.
	 */
	private void pushOutputValues(ValueNumber[] outputValueList) {
		ValueNumberFrame frame = getFrame();
		for (int i = 0; i < outputValueList.length; ++i)
			frame.pushValue(outputValueList[i]);
	}

	/**
	 * Get output values for current instruction from the ValueNumberCache.
	 */
	private ValueNumber[] getOutputValues(ValueNumber[] inputValueList, int numWordsProduced) {
		return getOutputValues(inputValueList, numWordsProduced, 0);
	}

	private ValueNumber[] getOutputValues(ValueNumber[] inputValueList, int numWordsProduced, int flags) {
		ValueNumberCache.Entry entry = new ValueNumberCache.Entry(handle, inputValueList);
		ValueNumber[] outputValueList = cache.lookupOutputValues(entry);
		if (outputValueList == null) {
			outputValueList = new ValueNumber[numWordsProduced];
			for (int i = 0; i < numWordsProduced; ++i) {
				ValueNumber freshValue = factory.createFreshValue();
				freshValue.setFlags(flags);
				outputValueList[i] = freshValue;
			}
			cache.addOutputValues(entry, outputValueList);
		}
		return outputValueList;
	}

	/**
	 * Load an instance field.
	 *
	 * @param instanceField the field
	 * @param reference     the ValueNumber of the object reference
	 * @param obj           the Instruction loading the field
	 */
	private void loadInstanceField(InstanceField instanceField, Instruction obj) {
		ValueNumberFrame frame = getFrame();

		try {
			ValueNumber reference = frame.popValue();

			AvailableLoad availableLoad = new AvailableLoad(reference, instanceField);
			if (RLE_DEBUG) System.out.print("[getfield of " + availableLoad + "]");
			ValueNumber[] loadedValue = frame.getAvailableLoad(availableLoad);

			if (loadedValue == null) {
				// Get (or create) the cached result for this instruction
				ValueNumber[] inputValueList = new ValueNumber[]{reference};
				loadedValue = getOutputValues(inputValueList, getNumWordsProduced(obj));
	
				// Make the load available
				frame.addAvailableLoad(availableLoad, loadedValue);
				if (RLE_DEBUG) System.out.print("[Making load available " + loadedValue[0] + "]");
			} else {
				// Found an available load!
				if (RLE_DEBUG) System.out.print("[Found available load " + availableLoad + "]");
			}

			pushOutputValues(loadedValue);
		} catch (DataflowAnalysisException e) {
			throw new AnalysisException("ValueNumberFrameModelingVisitor caught exception: " + e.toString(), e);
		}
	}

	/**
	 * Load a static field.
	 *
	 * @param staticField the field
	 * @param obj         the Instruction loading the field
	 */
	private void loadStaticField(StaticField staticField, Instruction obj) {
		ValueNumberFrame frame = getFrame();

		AvailableLoad availableLoad = new AvailableLoad(staticField);
		ValueNumber[] loadedValue = frame.getAvailableLoad(availableLoad);

		if (loadedValue == null) {
			// Make the load available
			int numWordsProduced = getNumWordsProduced(obj);
			loadedValue = getOutputValues(EMPTY_INPUT_VALUE_LIST, numWordsProduced);

			frame.addAvailableLoad(availableLoad, loadedValue);

			if (RLE_DEBUG) System.out.print("[making load of " + staticField + " available]");
		} else {
			if (RLE_DEBUG) System.out.print("[found available load of " + staticField + "]");
		}

		pushOutputValues(loadedValue);
	}

	/**
	 * Store an instance field.
	 *
	 * @param instanceField   the field
	 * @param obj             the instruction which stores the field
	 * @param pushStoredValue push the stored value onto the stack
	 *                        (because we are modeling an inner-class field access method)
	 */
	private void storeInstanceField(InstanceField instanceField, Instruction obj, boolean pushStoredValue) {
		ValueNumberFrame frame = getFrame();

		int numWordsConsumed = getNumWordsConsumed(obj);
		ValueNumber[] inputValueList = popInputValues(numWordsConsumed);
		ValueNumber reference = inputValueList[0];
		ValueNumber[] storedValue = new ValueNumber[inputValueList.length - 1];
		System.arraycopy(inputValueList, 1, storedValue, 0, inputValueList.length - 1);

		if (pushStoredValue)
			pushOutputValues(storedValue);

		// Kill all previous loads of the same field,
		// in case there is aliasing we don't know about
		frame.killLoadsOfField(instanceField);

		// Forward substitution
		frame.addAvailableLoad(new AvailableLoad(reference, instanceField), storedValue);
	}

	/**
	 * Store a static field.
	 *
	 * @param staticField     the static field
	 * @param obj             the instruction which stores the field
	 * @param pushStoredValue push the stored value onto the stack
	 *                        (because we are modeling an inner-class field access method)
	 */
	private void storeStaticField(StaticField staticField, Instruction obj, boolean pushStoredValue) {
		ValueNumberFrame frame = getFrame();

		AvailableLoad availableLoad = new AvailableLoad(staticField);

		int numWordsConsumed = getNumWordsConsumed(obj);
		ValueNumber[] inputValueList = popInputValues(numWordsConsumed);

		if (pushStoredValue)
			pushOutputValues(inputValueList);

		// Kill loads of this field
		frame.killLoadsOfField(staticField);

		// Make load available
		frame.addAvailableLoad(availableLoad, inputValueList);
	}

	/**
	 * Get the ValueNumber for given class's Class object.
	 *
	 * @param className the class
	 */
	private ValueNumber getClassObjectValue(String className) {
		ValueNumber value = classObjectValueMap.get(className);
		if (value == null) {
			value = factory.createFreshValue();
			classObjectValueMap.put(className, value);
		}
		return value;
	}

}

// vim:ts=4
