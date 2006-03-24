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

import java.util.BitSet;

import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.ba.Frame;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;

/**
 * A specialization of {@link Frame} for determining the types
 * of values in the Java stack frame (locals and operand stack).
 *
 * @author David Hovemeyer
 * @see Frame
 * @see TypeAnalysis
 */
public class TypeFrame extends Frame<Type> {
	// These are used for more precise modeling of instanceof instructions
	private ValueNumber instanceOfValueNumber;
	private Type instanceOfType;
	private BitSet exactTypeSet;
	
	/**
	 * Constructor.
	 */
	public TypeFrame(int numLocals) {
		super(numLocals);
		this.exactTypeSet = new BitSet();
	}

	/**
	 * Set whether or not a type in a given slot is exact.
	 * 
	 * @param slot    the slot
	 * @param isExact true if the slot contains an exact type, false if just an upper bound
	 */
	public void setExact(int slot, boolean isExact) {
		exactTypeSet.set(slot, isExact);
	}

	/**
	 * Get whether or not a type in a given slot is exact.
	 * 
	 * @param slot the slot
	 * @return true if the slot contains an exact type, false if just an upper bound
	 */
	public boolean isExact(int slot) {
		return exactTypeSet.get(slot);
	}
	
	/**
	 * Clear the exact type set.
	 * The result is that all slots will be assumed <em>not</em> to
	 * contain an exact type.
	 */
	public void clearExactSet() {
		exactTypeSet.clear();
	}
	
	//@Override
	@Override
         public void setTop() {
		super.setTop();
		clearExactSet();
	}
 	
	//@Override
	@Override
         public void copyFrom(Frame<Type> other_) {
		clearExactSet();
		exactTypeSet.or(((TypeFrame) other_).exactTypeSet);
		super.copyFrom(other_);
	}
	
	/**
	 * Reset information for modeling of instanceof branches.
	 */
	public void clearInstanceOfValueNumberAndType() {
		this.instanceOfValueNumber = null;
		this.instanceOfType = null;
	}

	/**
	 * Set information for modeling of instanceof branches.
	 * 
	 * @param instanceOfValueNumber ValueNumber of checked instance
	 * @param instanceOfType        instanceof Type
	 */
	public void setInstanceOfValueNumberAndType(ValueNumber instanceOfValueNumber, Type instanceOfType) {
		this.instanceOfValueNumber = instanceOfValueNumber;
		this.instanceOfType = instanceOfType;
	}

	/**
	 * Get the value number of the value checked by the instanceof branch.
	 * 
	 * @return the ValueNumber of the value checked by the instanceof branch
	 */
	public ValueNumber getInstanceOfValueNumber() {
		return instanceOfValueNumber;
	}
	
	/**
	 * Get the instanceof Type.
	 * 
	 * @return the instanceof Type
	 */
	public Type getInstanceOfType() {
		return instanceOfType;
	}

	@Override
         protected String valueToString(Type value) {
		return value.toString() + ",";
	}

	/**
	 * Get the single instance of the "Top" type.
	 */
	public static Type getTopType() {
		return TopType.instance();
	}

	/**
	 * Get the single instance of the "Bottom" type.
	 */
	public static Type getBottomType() {
		return BottomType.instance();
	}

	/**
	 * Get the single instance of the "LongExtra" type.
	 */
	public static Type getLongExtraType() {
		return LongExtraType.instance();
	}

	/**
	 * Get the single instance of the "DoubleExtra" type.
	 */
	public static Type getDoubleExtraType() {
		return DoubleExtraType.instance();
	}

	/**
	 * Get the single instance of the "Null" type.
	 */
	public static Type getNullType() {
		return NullType.instance();
	}

}

// vim:ts=4
