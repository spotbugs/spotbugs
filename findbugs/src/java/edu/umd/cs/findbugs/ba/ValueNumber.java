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

import java.util.*;

/**
 * A "value number" is a value produced somewhere in a methods.
 * We use value numbers as dataflow values in Frames.  When two frame
 * slots have the same value number, and that number is not one of the
 * special TOP, BOTTOM, and DEFAULT values, then the same value is definitely
 * in both of those slots.
 *
 * <p> Meaning of special values:
 * <ul>
 * <li> A TOP value is an uninitialized location.
 * <li> A BOTTOM value is the result of merging two different non-TOP values.
 * <li> A DEFAULT value is one which is not analyzed.  The analysis
 *      may produce this value if it results from an operation that is
 *      not "interesting".
 * </ul>
 *
 * <p> Instances of ValueNumbers produced by the same
 * {@link ValueNumberFactory ValueNumberFactory} are unique, so reference equality may
 * be used to determine whether or not two value numbers are the same.
 * In general, ValueNumbers from different factories cannot be compared.
 *
 * @author David Hovemeyer
 */
public class ValueNumber {
	/** The value number. */
	private int number;

	/** Number of the special TOP value. */
	private static final int TOP = -1;

	/** Number of the special BOTTOM value. */
	private static final int BOTTOM = -2;

	/** Number of the special DEFAULT value. */
	private static final int DEFAULT = -3;

	/**
	 * Constructor.
	 * @param number the value number
	 */
	ValueNumber(int number) {
		this.number = number;
	}

	/** Single instance of the special TOP value. */
	static final ValueNumber topValue = new ValueNumber(TOP);

	/** Single instance of the special BOTTOM value. */
	static final ValueNumber bottomValue = new ValueNumber(BOTTOM);

	/** Single instance of the special DEFAULT value. */
	static final ValueNumber defaultValue = new ValueNumber(DEFAULT);

	/**
	 * Dataflow merge of this ValueNumber with given ValueNumber.
	 * @param other the other ValueNumber
	 * @return the ValueNumber representing the dataflow merge of the two ValueNumbers
	 */
	public ValueNumber mergeWith(ValueNumber other) {
		if (this == bottomValue || other == bottomValue) // bottom merged with anything is bottom
			return bottomValue;
		else if (this == topValue) // top merged with any value is the same value
			return other;
		else if (other == topValue) // top merged with any value is the same value
			return this;
		else if (this == other) // identical values
			return this;
		else
			return bottomValue;
	}

	public String toString() {
		if (this == topValue)
			return "(TOP)";
		else if (this == bottomValue)
			return "(BOTTOM)";
		else if (this == defaultValue)
			return "(DEFAULT)";
		else
			return "(" + number + ")";
	}

	public int hashCode() {
		return System.identityHashCode(this);
	}

	public boolean equals(Object o) {
		if (!(o instanceof Object))
			return false;
		return number == ((ValueNumber) o).number;
	}

}

// vim:ts=4
