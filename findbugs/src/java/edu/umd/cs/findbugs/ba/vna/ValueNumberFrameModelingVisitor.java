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

package edu.umd.cs.findbugs.ba.vna;

import java.util.HashMap;
import java.util.IdentityHashMap;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.generic.AALOAD;
import org.apache.bcel.generic.ArrayInstruction;
import org.apache.bcel.generic.CHECKCAST;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.MONITORENTER;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;

import edu.umd.cs.findbugs.ba.AbstractFrameModelingVisitor;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Debug;
import edu.umd.cs.findbugs.ba.Frame;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.InstanceField;
import edu.umd.cs.findbugs.ba.InvalidBytecodeException;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;
import edu.umd.cs.findbugs.ba.StaticField;
import edu.umd.cs.findbugs.ba.XField;

/**
 * Visitor which models the effects of bytecode instructions
 * on value numbers of values in the operand stack frames.
 *
 * @see ValueNumber
 * @see ValueNumberFrame
 * @see ValueNumberAnalysis
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
	private HashMap<Object, ValueNumber> constantValueMap;
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
		this.constantValueMap = new HashMap<Object, ValueNumber>();
		this.stringConstantMap = new HashMap<ValueNumber, String>();
		this.lookupFailureCallback = lookupFailureCallback;
	}

	@Override
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
	 * Determine whether redundant load elimination
	 * should be performed for the heap location referenced by
	 * the current instruction.
	 *
	 * @return true if we should do redundant load elimination
	 *         for the current instruction, false if not
	 */
	private boolean doRedundantLoadElimination() {
		if (!REDUNDANT_LOAD_ELIMINATION)
			return false;

		XField xfield = loadedFieldSet.getField(handle);
		if (xfield == null)
			return false;

		if (!xfield.isReferenceType())
			return false;

		// Don't do redundant load elimination for fields that
		// are loaded in only one place.
		if (false && loadedFieldSet.getLoadStoreCount(xfield).getLoadCount() <= 1)
			return false;

		return true;
	}

	/**
	 * Determine whether forward substitution
	 * should be performed for the heap location referenced by
	 * the current instruction.
	 *
	 * @return true if we should do forward substitution
	 *         for the current instruction, false if not
	 */
	private boolean doForwardSubstitution() {
		if (!REDUNDANT_LOAD_ELIMINATION)
			return false;

		XField xfield = loadedFieldSet.getField(handle);
		if (xfield == null)
			return false;

		if (!xfield.isReferenceType())
			return false;

		// Don't do forward substitution for fields that
		// are never read.
		if (!loadedFieldSet.isLoaded(xfield))
			return false;

		return true;
	}

	private void checkConsumedAndProducedValues(Instruction ins, ValueNumber[] consumedValueList,
			ValueNumber[] producedValueList) {
		int numConsumed = ins.consumeStack(getCPG());
		int numProduced = ins.produceStack(getCPG());

		if (numConsumed == Constants.UNPREDICTABLE)
			throw new InvalidBytecodeException("Unpredictable stack consumption for " + ins);
		if (numProduced == Constants.UNPREDICTABLE)
			throw new InvalidBytecodeException("Unpredictable stack production for " + ins);

		if (consumedValueList.length != numConsumed) {
			throw new IllegalStateException("Wrong number of values consumed for " + ins +
				": expected " + numConsumed + ", got " + consumedValueList.length);
		}

		if (producedValueList.length != numProduced) {
			throw new IllegalStateException("Wrong number of values produced for " + ins +
				": expected " + numProduced + ", got " + producedValueList.length);
		}
	}

	/**
	 * This is the default instruction modeling method.
	 */
	@Override
	public void modelNormalInstruction(Instruction ins, int numWordsConsumed, int numWordsProduced) {

		int flags = 0;
		if (ins instanceof InvokeInstruction) 
			flags = ValueNumber.RETURN_VALUE;
		else if (ins instanceof ArrayInstruction) flags = ValueNumber.ARRAY_VALUE;


		// Get the input operands to this instruction.
		ValueNumber[] inputValueList = popInputValues(numWordsConsumed);

		// See if we have the output operands in the cache.
		// If not, push default (fresh) values for the output,
		// and add them to the cache.
		ValueNumber[] outputValueList = getOutputValues(inputValueList, numWordsProduced, flags);

		if (VERIFY_INTEGRITY) {
			checkConsumedAndProducedValues(ins, inputValueList, outputValueList);
		}

		// Push output operands on stack.
		pushOutputValues(outputValueList);
	}

	@Override
	public void visitGETFIELD(GETFIELD obj) {
		if (doRedundantLoadElimination()) {

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

	@Override
	public void visitPUTFIELD(PUTFIELD obj) {
		if (doForwardSubstitution()) {
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

	@Override
	public void visitGETSTATIC(GETSTATIC obj) {
		ConstantPoolGen cpg = getCPG();

		String fieldName = obj.getName(cpg);
		String fieldSig = obj.getSignature(cpg);
		ValueNumberFrame frame = getFrame();

		if (RLE_DEBUG) {
			System.out.println("GETSTATIC of " + fieldName + " : " + fieldSig);
		}
		// Is this an access of a Class object?
		if (fieldName.startsWith("class$") && fieldSig.equals("Ljava/lang/Class;")) {
			String className = fieldName.substring("class$".length()).replace('$', '.');
			if (RLE_DEBUG) System.out.println("[found load of class object " + className + "]");
			ValueNumber value = getClassObjectValue(className);
			frame.pushValue(value);
			return;
		}
		if (doRedundantLoadElimination()) {
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

	@Override
	public void visitPUTSTATIC(PUTSTATIC obj) {
		if (doForwardSubstitution()) {
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

	@Override
	public void visitINVOKESTATIC(INVOKESTATIC obj) {
		if (REDUNDANT_LOAD_ELIMINATION) {
			ConstantPoolGen cpg = getCPG();
			String targetClassName = obj.getClassName(cpg);
			String methodName = obj.getName(cpg);
			String methodSig = obj.getSignature(cpg);

			if ((methodName.equals("forName") && targetClassName.equals("java.lang.Class") || methodName.equals("class$")) && methodSig.equals("(Ljava/lang/String;)Ljava/lang/Class;")) {
				// Access of a Class object
				ValueNumberFrame frame = getFrame();
				try {
					ValueNumber arg = frame.getTopValue();
					String className = stringConstantMap.get(arg);
					if (className != null) {
						frame.popValue();
						if (RLE_DEBUG) System.out.println("[found access of class object " + className + "]");
						frame.pushValue(getClassObjectValue(className));
						return;
					}
				} catch (DataflowAnalysisException e) {
					throw new InvalidBytecodeException("stack underflow", methodGen, handle, e);
				}
			} else if (Hierarchy.isInnerClassAccess(obj, cpg)) {
				// Possible access of field via an inner-class access method
				XField xfield = loadedFieldSet.getField(handle);
				if (xfield != null) {
					if (loadedFieldSet.instructionIsLoad(handle)) {
						// Load via inner-class accessor
						if (doRedundantLoadElimination()) {
							if (xfield.isStatic())
								loadStaticField((StaticField) xfield, obj);
							else
								loadInstanceField((InstanceField) xfield, obj);
							return;
						}
					} else {
						// Store via inner-class accessor
						if (doForwardSubstitution()) {
							// Some inner class access store methods
							// return the value stored.
							boolean pushValue = !methodSig.endsWith(")V");
	
							if (xfield.isStatic())
								storeStaticField((StaticField) xfield, obj, pushValue);
							else
								storeInstanceField((InstanceField) xfield, obj, pushValue);

							return;
						}
					}
				}
			} else {
				// Don't know what this method invocation is doing.
				// Kill all loads.
				killLoadsOfObjectsPassed(obj);
				getFrame().killAllLoadsOf(null);
			}
		}

		handleNormalInstruction(obj);
	}

	private void killLoadsOfObjectsPassed(Instruction ins) {
		int passed = getNumWordsConsumed(ins);
		ValueNumber [] arguments = new ValueNumber[passed];
		try {
			getFrame().getTopStackWords(arguments);
			for(ValueNumber v : arguments)
				getFrame().killAllLoadsOf(v);
			
		} catch (DataflowAnalysisException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public void visitMONITORENTER(MONITORENTER obj) {
		// Don't know what this sync invocation is doing.
		// Kill all loads.
		getFrame().killAllLoads();
		handleNormalInstruction(obj);
	}

	@Override
	public void visitINVOKESPECIAL(INVOKESPECIAL obj) {
		// Don't know what this method invocation is doing.
		// Kill all loads.
		killLoadsOfObjectsPassed(obj);
		handleNormalInstruction(obj);
	}

	@Override
	public void visitINVOKEINTERFACE(INVOKEINTERFACE obj) {
		// Don't know what this method invocation is doing.
		// Kill all loads.
		if (obj.getMethodName(cpg).equals("lock"))
			getFrame().killAllLoads();
		else killLoadsOfObjectsPassed(obj);
		handleNormalInstruction(obj);
	}

	@Override
	public void visitINVOKEVIRTUAL(INVOKEVIRTUAL obj) {
		// Don't know what this method invocation is doing.
		// Kill all loads.
		if (obj.getMethodName(cpg).equals("lock"))
			getFrame().killAllLoads();
		else killLoadsOfObjectsPassed(obj);
		handleNormalInstruction(obj);
	}

	@Override
	public void visitLDC(LDC obj) {
		Object constantValue = obj.getValue(cpg);
		ValueNumber value;
		if (constantValue instanceof ConstantClass) {
			ConstantClass constantClass = (ConstantClass) constantValue;
			String className = constantClass.getBytes(cpg.getConstantPool());
			value = getClassObjectValue(className);
		} else {
			value = constantValueMap.get(constantValue);
			if (value == null) {
				value = factory.createFreshValue();
				constantValueMap.put(constantValue, value);

				// Keep track of String constants

				if (constantValue instanceof String) {
					stringConstantMap.put(value, (String) constantValue);
				}
			}
		}
		getFrame().pushValue(value);
	}
	
	@Override
	public void visitIINC(IINC obj) {
		if (obj.getIncrement() == 0) {
			// A no-op.
			return;
		}
		
		// IINC is a special case because its input and output are not on the stack.
		// However, we still want to use the value number cache to ensure that
		// this operation is modeled consistently.  (If we do nothing, we miss
		// the fact that the referenced local is modified.)
		
		int local = obj.getIndex();
		
		ValueNumber[] input = new ValueNumber[]{ getFrame().getValue(local) };
		ValueNumberCache.Entry entry = new ValueNumberCache.Entry(handle, input);
		ValueNumber[] output = cache.lookupOutputValues(entry);
		if (output == null) {
			output = new ValueNumber[]{ factory.createFreshValue() };
			cache.addOutputValues(entry, output);
		}
		
		getFrame().setValue(local, output[0]);
	}

	@Override
	public void visitCHECKCAST(CHECKCAST obj) {
		// Do nothing
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
			throw new InvalidBytecodeException("Error getting input operands", e);
		}

		return inputValueList;
	}

	/**
	 * Push given output values onto the current frame.
	 */
	private void pushOutputValues(ValueNumber[] outputValueList) {
		ValueNumberFrame frame = getFrame();
		for (ValueNumber aOutputValueList : outputValueList)
			frame.pushValue(aOutputValueList);
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
			if (false && RLE_DEBUG) {
				System.out.println("<<cache fill for " + handle.getPosition() + ": " +
					vlts(inputValueList) + " ==> " + vlts(outputValueList) + ">>");
			}
			cache.addOutputValues(entry, outputValueList);
		} else if (false && RLE_DEBUG) {
			System.out.println("<<cache hit for " + handle.getPosition() + ": " +
				vlts(inputValueList) + " ==> " + vlts(outputValueList) + ">>");
		}
		return outputValueList;
	}

	private static String vlts(ValueNumber[] vl) {
		StringBuffer buf = new StringBuffer();
		for (ValueNumber aVl : vl) {
			if (buf.length() > 0) buf.append(',');
			buf.append(aVl.getNumber());
		}
		return buf.toString();
	}

	/**
	 * Load an instance field.
	 *
	 * @param instanceField the field
	 * @param obj           the Instruction loading the field
	 */
	private void loadInstanceField(InstanceField instanceField, Instruction obj) {
		if (RLE_DEBUG) {
			System.out.println("[loadInstanceField for field " + instanceField + " in instruction " + handle);
		}

		ValueNumberFrame frame = getFrame();

		try {
			ValueNumber reference = frame.popValue();

			AvailableLoad availableLoad = new AvailableLoad(reference, instanceField);
			if (RLE_DEBUG) System.out.println("[getfield of " + availableLoad + "]");
			ValueNumber[] loadedValue = frame.getAvailableLoad(availableLoad);

			if (loadedValue == null) {
				// Get (or create) the cached result for this instruction
				ValueNumber[] inputValueList = new ValueNumber[]{reference};
				loadedValue = getOutputValues(inputValueList, getNumWordsProduced(obj));
	
				// Make the load available
				frame.addAvailableLoad(availableLoad, loadedValue);
				if (RLE_DEBUG) {
					System.out.println("[Making load available " +
						availableLoad + " <- " +
						vlts(loadedValue) + "]");
				}
			} else {
				// Found an available load!
				if (RLE_DEBUG) {
					System.out.println("[Found available load " +
						availableLoad + " <- " + vlts(loadedValue) + "]");
				}
			}

			pushOutputValues(loadedValue);

			if (VERIFY_INTEGRITY) {
				checkConsumedAndProducedValues(obj, new ValueNumber[]{reference}, loadedValue);
			}
		} catch (DataflowAnalysisException e) {
			throw new InvalidBytecodeException("Error loading from instance field", e);
		}
	}

	/**
	 * Load a static field.
	 *
	 * @param staticField the field
	 * @param obj         the Instruction loading the field
	 */
	private void loadStaticField(StaticField staticField, Instruction obj) {
		if (RLE_DEBUG) {
			System.out.println("[loadStaticField for field " + staticField + " in instruction " + handle);
		}

		ValueNumberFrame frame = getFrame();

		AvailableLoad availableLoad = new AvailableLoad(staticField);
		ValueNumber[] loadedValue = frame.getAvailableLoad(availableLoad);

		if (loadedValue == null) {
			// Make the load available
			int numWordsProduced = getNumWordsProduced(obj);
			loadedValue = getOutputValues(EMPTY_INPUT_VALUE_LIST, numWordsProduced);

			frame.addAvailableLoad(availableLoad, loadedValue);

			if (RLE_DEBUG) System.out.println("[making load of " + staticField + " available]");
		} else {
			if (RLE_DEBUG) System.out.println("[found available load of " + staticField + "]");
		}

		if (VERIFY_INTEGRITY) {
			checkConsumedAndProducedValues(obj, new ValueNumber[0], loadedValue);
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
		if (RLE_DEBUG) {
			System.out.println("[storeInstanceField for field " + instanceField + " in instruction " + handle);
		}

		ValueNumberFrame frame = getFrame();

		int numWordsConsumed = getNumWordsConsumed(obj);
/*
		System.out.println("Instruction is " + handle);
		System.out.println("numWordsConsumed="+numWordsConsumed);
*/
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

		if (RLE_DEBUG) System.out.println("[making store of " + instanceField + " available]");

		if (VERIFY_INTEGRITY) {
/*
			System.out.println("pushStoredValue="+pushStoredValue);
*/
			checkConsumedAndProducedValues(obj, inputValueList,
				pushStoredValue ? storedValue : new ValueNumber[0]);
		}
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
		if (RLE_DEBUG) {
			System.out.println("[storeStaticField for field " + staticField + " in instruction " + handle);
		}

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

		if (RLE_DEBUG) System.out.println("[making store of " + staticField + " available]");

		if (VERIFY_INTEGRITY) {
			checkConsumedAndProducedValues(obj, inputValueList,
				pushStoredValue ? inputValueList : new ValueNumber[0]);
		}
	}

	/**
	 * Get the ValueNumber for given class's Class object.
	 *
	 * @param className the class
	 */
	public ValueNumber getClassObjectValue(String className) {
		// TODO: Check to see if we need to do this
		className = className.replace('/','.');
		ValueNumber value = classObjectValueMap.get(className);
		if (value == null) {
			value = factory.createFreshValue();
			value.setFlag(ValueNumber.CONSTANT_CLASS_OBJECT);
			classObjectValueMap.put(className, value);
		}
		return value;
	}

}

// vim:ts=4
