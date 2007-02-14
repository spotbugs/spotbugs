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



/**
 * A "value number" is a value produced somewhere in a methods.
 * We use value numbers as dataflow values in Frames.  When two frame
 * slots have the same value number, then the same value is in both
 * of those slots.
 * <p/>
 * <p> Instances of ValueNumbers produced by the same
 * {@link ValueNumberFactory ValueNumberFactory} are unique, so reference equality may
 * be used to determine whether or not two value numbers are the same.
 * In general, ValueNumbers from different factories cannot be compared.
 *
 * @author David Hovemeyer
 * @see ValueNumberAnalysis
 */
public class ValueNumber implements Comparable<ValueNumber> {
	/**
	 * The value number.
	 */
	int number;

	/**
	 * Flags representing meta information about the value.
	 */
	int flags;

	/**
	 * Flag specifying that this value was the return value
	 * of a called method.
	 */
	public static final int RETURN_VALUE = 1;
	
	public static final int ARRAY_VALUE = 2;
	
	public static final int CONSTANT_CLASS_OBJECT = 4;
	
	public static final int PHI_NODE = 8;

	/**
	 * Constructor.
	 *
	 * @param number the value number
	 */
	ValueNumber(int number) {
		this.number = number;
		this.flags = 0;
	}

	public int getNumber() {
		return number;
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public void setFlag(int flag) {
		flags |= flag;
	}

	public boolean hasFlag(int flag) {
		return (flags & flag) == flag;
	}

	@Override
         public String toString() {
		if (flags != 0) return number+"("+flags+"),";
		return number + ",";
	}

	@Override
    public int hashCode() {
		return number;
	}
	@Override
    public boolean equals(Object o) {
		if (o instanceof ValueNumber) 
			return number == ((ValueNumber)o).number;
		return false;
	}
	public int compareTo(ValueNumber other) {
		return number - other.number;
	}
/*
	
	public int hashCode() {
		return number;
	}
	
	
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass())
			return false;
		ValueNumber other = (ValueNumber) obj;
		return this.number == other.number;
	}
*/
}

// vim:ts=4
