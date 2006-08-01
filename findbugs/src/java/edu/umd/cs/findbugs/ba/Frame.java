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

import java.util.ArrayList;
import java.util.BitSet;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.StackConsumer;

import edu.umd.cs.findbugs.SystemProperties;
import static edu.umd.cs.findbugs.ba.Debug.*;

/**
 * Generic class for representing a Java stack frame as a dataflow value.
 * A frame consists of "slots", which represent the local variables
 * and values on the Java operand stack.
 * Slots 0 .. <code>getNumLocals() - 1</code> represent the local variables.
 * Slots <code>getNumLocals()</code> .. <code>getNumSlots() - 1</code>
 * represent the Java operand stack.
 * <p/>
 * <p> Frame is parametized by "ValueType", which is the type of value
 * to be stored in the Frame's slots.  This type must form a lattice,
 * according to the abstract mergeValues() operation
 * in the corresponding analysis class (which should be
 * derived from FrameDataflowAnalysis).
 * When a Frame is constructed, all of its slots will contain
 * null.  The analysis is responsible for initializing
 * created Frames with default values at the appropriate time.
 * Typically, only initEntryFact() will need to do this.
 * <p/>
 * <p> A Frame may have the special "TOP" value. Such frames serve as
 * the identity element for the meet operation operation.
 * <p/>
 * <p> A Frame may have the special "BOTTOM" value. The result of merging
 * any frame with BOTTOM is BOTTOM.
 *
 * @author David Hovemeyer
 * @see FrameDataflowAnalysis
 */
public abstract class Frame <ValueType>   {

	////////////////////////////////////////////////////////////////////////////////////
	// Instance variables
	////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Number of local variables in the method.
	 */
	private int numLocals;

	/**
	 * Array storing the values of local variables and operand stack slots.
	 */
	private ArrayList<ValueType> slotList;

	/**
	 * Flag marking this frame as a special "TOP" value.
	 * Such Frames serve as the identity element when merging.
	 */
	private boolean isTop;

	/**
	 * Flag marking this frame as a special "BOTTOM" value.
	 * Such Frames arise when merging two frames of different size.
	 */
	private boolean isBottom;

	/**
	 * Default number of stack slots to preallocate space for.
	 */
	private static final int DEFAULT_STACK_CAPACITY = 10;

	////////////////////////////////////////////////////////////////////////////////////
	// Methods
	////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Constructor.
	 * This version of the constructor is for subclasses for which it is
	 * always safe to call getDefaultValue(), even when the object is not
	 * fully initialized.
	 *
	 * @param numLocals number of local variable slots in the method
	 */
	public Frame(int numLocals) {
		this.numLocals = numLocals;
		this.slotList = new ArrayList<ValueType>(numLocals + DEFAULT_STACK_CAPACITY);
		for (int i = 0; i < numLocals; ++i)
			slotList.add(null);
	}

	/**
	 * Return whether or not this object the special "TOP" value for Frames.
	 * Such Frames are the identity element of the meet operation.
	 */
	public boolean isTop() {
		return isTop;
	}

	/**
	 * Make this frame the special "TOP" value.
	 * Such Frames are the identity element of the meet operation.
	 */
	public void setTop() {
		isTop = true;
		isBottom = false;
	}

	/**
	 * Return whether or not this object is the special "BOTTOM" value for Frames.
	 * Such Frames arise when merging two frames of different size.
	 */
	public boolean isBottom() {
		return isBottom;
	}

	/**
	 * Make this Frame the special "BOTTOM" value.
	 * Such Frames arise when merging two frames of different size.
	 */
	public void setBottom() {
		isBottom = true;
		isTop = false;
	}

	/**
	 * Set the Frame to be valid (neither TOP nor BOTTOM).
	 */
	public void setValid() {
		isTop = isBottom = false;
	}

	/**
	 * Is the frame valid (meaning it is not TOP or BOTTOM)?
	 */
	public boolean isValid() {
		return !isTop() && !isBottom();
	}

	/**
	 * Push a value onto the Java operand stack.
	 *
	 * @param value the ValueType to push
	 */
	public void pushValue(ValueType value) {
		if (VERIFY_INTEGRITY && value == null) throw new IllegalArgumentException();
		if (!isValid()) throw new IllegalStateException("accessing top or bottom frame");
		slotList.add(value);
	}

	/**
	 * Pop a value off of the Java operand stack.
	 *
	 * @return the value that was popped
	 * @throws DataflowAnalysisException if the Java operand stack is empty
	 */
	public ValueType popValue() throws DataflowAnalysisException {
		if (!isValid()) throw new DataflowAnalysisException("accessing top or bottom frame");
		if (slotList.size() == numLocals) throw new DataflowAnalysisException("operand stack empty");
		return slotList.remove(slotList.size() - 1);
	}

	/**
	 * Get the value on the top of the Java operand stack.
	 *
	 * @throws DataflowAnalysisException if the Java operand stack is empty
	 */
	public ValueType getTopValue() throws DataflowAnalysisException {
		if (!isValid()) throw new DataflowAnalysisException("accessing top or bottom frame");
		assert slotList.size() >= numLocals;
		if (slotList.size() == numLocals)
			throw new DataflowAnalysisException("operand stack is empty");
		return slotList.get(slotList.size() - 1);
	}

	/**
	 * Get the values on the top of the Java operand stack.
	 * The top stack item is placed at the end of the array,
	 * so that to restore the values to the stack, you would push
	 * them in the order they appear in the array.
	 */
	public void getTopStackWords(ValueType[] valueList) throws DataflowAnalysisException {
		int stackDepth = getStackDepth();
		if (valueList.length > stackDepth)
			throw new DataflowAnalysisException("not enough values on stack");
		int numSlots = slotList.size();
		for (int i = numSlots - valueList.length, j = 0; i < numSlots; ++i, ++j) {
			valueList[j] = slotList.get(i);
		}
	}

	/**
	 * Get a value on the operand stack.
	 *
	 * @param loc the stack location, counting downwards from the
	 *            top (location 0)
	 */
	public ValueType getStackValue(int loc) throws DataflowAnalysisException {
		if (!isValid())
			throw new DataflowAnalysisException("Accessing TOP or BOTTOM frame!");
		int stackDepth = getStackDepth();
		if (loc >= stackDepth)
			throw new DataflowAnalysisException("not enough values on stack: access=" + loc + ", avail=" + stackDepth);
		return slotList.get(slotList.size() - (loc + 1));
	}
	
	/**
	 * Get the value corresponding to the object instance used in
	 * the given instruction.  This relies on the observation that in
	 * instructions which use an object instance (such as getfield,
	 * invokevirtual, etc.), the object instance is the first
	 * operand used by the instruction.
	 *
	 * @param ins the instruction
	 * @param cpg the ConstantPoolGen for the method
	 */
	public ValueType getInstance(Instruction ins, ConstantPoolGen cpg) throws DataflowAnalysisException {
		return getStackValue(getInstanceStackLocation(ins, cpg));
	}

	/**
	 * Get the stack location (counting down from top of stack,
	 * starting at 0) containing the object instance referred to
	 * by given instruction.  This relies on the observation that in
	 * instructions which use an object instance (such as getfield,
	 * invokevirtual, etc.), the object instance is the first
	 * operand used by the instruction.
	 * 
	 * <p>The value returned may be passed to getStackValue(int).</p>
	 * 
	 * @param ins the Instruction
	 * @param cpg the ConstantPoolGen for the method
	 * @return stack location (counting down from top of stack, starting at 0)
	 *         containing the object instance
	 * @throws DataflowAnalysisException
	 */
	public int getInstanceStackLocation(Instruction ins, ConstantPoolGen cpg) throws DataflowAnalysisException {
		int numConsumed = ins.consumeStack(cpg);
		if (numConsumed == Constants.UNPREDICTABLE)
			throw new DataflowAnalysisException("Unpredictable stack consumption in " + ins);
		return numConsumed - 1;
	}
	
	/**
	 * Get the slot the object instance referred to by given instruction
	 * is located in.
	 * 
	 * @param ins the Instruction
	 * @param cpg the ConstantPoolGen for the method
	 * @return stack slot the object instance is in
	 * @throws DataflowAnalysisException
	 */
	public int getInstanceSlot(Instruction ins, ConstantPoolGen cpg) throws DataflowAnalysisException {
		int numConsumed = ins.consumeStack(cpg);
		if (numConsumed == Constants.UNPREDICTABLE)
			throw new DataflowAnalysisException("Unpredictable stack consumption in " + ins);
		if (numConsumed > getStackDepth())
			throw new DataflowAnalysisException("Stack underflow " + ins);
		return getNumSlots() - numConsumed;
	}
	
	/**
	 * Get the number of arguments passed to given method invocation.
	 * 
	 * @param ins the method invocation instruction
	 * @param cpg the ConstantPoolGen for the class containing the method
	 * @return number of arguments; note that this excludes the
	 *         object instance for instance methods
	 * @throws DataflowAnalysisException
	 */
	public int getNumArguments(InvokeInstruction ins, ConstantPoolGen cpg) throws DataflowAnalysisException {
		int numConsumed = getNumArgumentsIncludingObjectInstance(ins, cpg);
		return (ins instanceof INVOKESTATIC) ? numConsumed : numConsumed - 1;
	}
	
	/**
	 * Get the number of arguments passed to given method invocation,
	 * including the object instance if the call is to an instance method. 
	 * 
	 * @param ins the method invocation instruction
	 * @param cpg the ConstantPoolGen for the class containing the method
	 * @return number of arguments, including object instance if appropriate
	 * @throws DataflowAnalysisException
	 */
	public int getNumArgumentsIncludingObjectInstance(InvokeInstruction ins, ConstantPoolGen cpg) throws DataflowAnalysisException {
		int numConsumed = ins.consumeStack(cpg);
		if (numConsumed == Constants.UNPREDICTABLE)
			throw new DataflowAnalysisException("Unpredictable stack consumption in " + ins);
		return numConsumed;
	}
	
	/**
	 * Get the <i>i</i>th argument passed to given method invocation.
	 * 
	 * @param ins the method invocation instruction
	 * @param cpg the ConstantPoolGen for the class containing the method
	 * @param i   index of the argument; 0 for the first argument, etc.
	 * @param numArguments total number of arguments to the method
	 * @return the <i>i</i>th argument
	 * @throws DataflowAnalysisException
	 */
	public ValueType getArgument(InvokeInstruction ins, ConstantPoolGen cpg, int i, int numArguments)
			throws DataflowAnalysisException {
		if (i >= numArguments)
			throw new IllegalArgumentException();
		return getStackValue((numArguments - 1) - i);
	}
	
	/**
	 * Get the <i>i</i>th operand used by given instruction.
	 * 
	 * @param ins the instruction, which must be a StackConsumer
	 * @param cpg the ConstantPoolGen
	 * @param i   index of operand to get: 0 for the first operand, etc.
	 * @return the <i>i</i>th operand used by the given instruction
	 * @throws DataflowAnalysisException 
	 */
	public ValueType getOperand(StackConsumer ins, ConstantPoolGen cpg, int i) throws DataflowAnalysisException {
		int numOperands = ins.consumeStack(cpg);
		if (numOperands == Constants.UNPREDICTABLE)
			throw new DataflowAnalysisException("Unpredictable stack consumption in " + ins);
		return getStackValue((numOperands - 1) - i);
	}
	
	/**
	 * Get set of arguments passed to a method invocation
	 * which match given predicate.
	 * 
	 * @param invokeInstruction the InvokeInstruction
	 * @param cpg               the ConstantPoolGen
	 * @param chooser           predicate to choose which argument values should
	 *                          be in the returned set
	 * @return BitSet specifying which arguments match the predicate,
	 *                indexed by argument number (starting from 0)
	 * @throws DataflowAnalysisException
	 */
	public BitSet getArgumentSet(
			InvokeInstruction invokeInstruction,
			ConstantPoolGen cpg,
			DataflowValueChooser<ValueType> chooser) throws DataflowAnalysisException {
		BitSet chosenArgSet = new BitSet();
		int numArguments = getNumArguments(invokeInstruction, cpg);

		for (int i = 0; i < numArguments; ++i) {
			ValueType value = getArgument(invokeInstruction, cpg, i, numArguments);
			if (chooser.choose(value))
				chosenArgSet.set(i);
		}
	
		return chosenArgSet;
	}

	/**
	 * Clear the Java operand stack.
	 * Only local variable slots will remain in the frame.
	 */
	public void clearStack() {
		if (!isValid()) throw new IllegalStateException("accessing top or bottom frame");
		assert slotList.size() >= numLocals;
		if (slotList.size() > numLocals)
			slotList.subList(numLocals, slotList.size()).clear();
	}

	/**
	 * Get the depth of the Java operand stack.
	 */
	public int getStackDepth() {
		return slotList.size() - numLocals;
	}

	/**
	 * Get the number of locals.
	 */
	public int getNumLocals() {
		return numLocals;
	}

	/**
	 * Get the number of slots (locals plus stack values).
	 */
	public int getNumSlots() {
		return slotList.size();
	}

	/**
	 * Get the value at the <i>n</i>th slot.
	 *
	 * @param n the slot to get the value of
	 * @return the value in the slot
	 */
	public ValueType getValue(int n) {
		if (!isValid()) throw new IllegalStateException("accessing top or bottom frame");
		return slotList.get(n);
	}

	/**
	 * Set the value at the <i>n</i>th slot.
	 *
	 * @param n     the slot in which to set a new value
	 * @param value the value to set
	 */
	public void setValue(int n, ValueType value) {
		if (VERIFY_INTEGRITY && value == null) throw new IllegalArgumentException();
		if (!isValid()) throw new IllegalStateException("accessing top or bottom frame");
		slotList.set(n, value);
	}

	/**
	 * Return true if this stack frame is the same as the one given
	 * as a parameter.
	 *
	 * @param other the other Frame
	 * @return true if the frames are the same, false otherwise
	 */
	public boolean sameAs(Frame<ValueType> other) {
		if (isTop != other.isTop)
			return false;

		if (isTop && other.isTop)
			return true;

		if (isBottom != other.isBottom)
			return false;

		if (isBottom && other.isBottom)
			return true;

		if (getNumSlots() != other.getNumSlots())
			return false;

		for (int i = 0; i < getNumSlots(); ++i)
			if (!getValue(i).equals(other.getValue(i)))
				return false;

		return true;
	}

	/**
	 * Make this Frame exactly the same as the one given as a parameter.
	 *
	 * @param other the Frame to make this object the same as
	 */
	public void copyFrom(Frame<ValueType> other) {
		if (slotList.size() == other.slotList.size()) {
			for(int i = 0; i < slotList.size(); i++)
				slotList.set(i, other.slotList.get(i));
		} else {
			slotList.clear();
			for(ValueType v : other.slotList) 
				slotList.add(v);
		}
		isTop = other.isTop;
		isBottom = other.isBottom;
	}

	private static final boolean STACK_ONLY = SystemProperties.getBoolean("dataflow.stackonly");

	/**
	 * Convert to string.
	 */
	@Override
         public String toString() {
		if (isTop()) return "[TOP]";
		if (isBottom()) return "[BOTTOM]";
		StringBuffer buf = new StringBuffer();
		buf.append('[');
		int numSlots = getNumSlots();
		int start = STACK_ONLY ? getNumLocals() : 0;
		for (int i = start; i < numSlots; ++i) {
			if (!STACK_ONLY && i == getNumLocals()) {
				// Use a "|" character to visually separate locals from
				// the operand stack.
				int last = buf.length() - 1;
				if (last >= 0) {
					if (buf.charAt(last) == ',')
						buf.deleteCharAt(last);
				}
				buf.append('|');
			}
			String value = valueToString(getValue(i));
			if (i == numSlots - 1 && value.endsWith(","))
				value = value.substring(0, value.length() - 1);
			buf.append(value);
			//buf.append(' ');
		}
		buf.append(']');
		return buf.toString();
	}

	/**
	 * Subclasses may override this if they want to do something special
	 * to convert Value objects to Strings.  By default, we just call
	 * toString() on the values.
	 */
	protected String valueToString(ValueType value) {
		return value.toString();
	}

}

// vim:ts=4
