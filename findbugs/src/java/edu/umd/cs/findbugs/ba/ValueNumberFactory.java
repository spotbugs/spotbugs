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
 * Factory for ValueNumbers.
 * A single Factory must be used to create all of the ValueNumbers
 * for a method.
 *
 * @see ValueNumber
 * @author David Hovemeyer
 */
public class ValueNumberFactory {
	/** Map of numbers to the ValueNumber instances. */
	private final HashMap<Integer, ValueNumber> instanceMap = new HashMap<Integer, ValueNumber>();

	/**
	 * Get the ValueNumber instance for given number.
	 * The number must be non-negative.
	 * @param number the number
	 * @return the ValueNumber instance
	 */
	public ValueNumber getValueNumber(int number) {
		if (number < 0)
			throw new IllegalArgumentException("Bad value number: " + number);

		Integer key = new Integer(number);
		ValueNumber valueNumber = instanceMap.get(key);
		if (valueNumber == null) {
			valueNumber = new ValueNumber(number);
			instanceMap.put(key, valueNumber);
		}
		return valueNumber;
	}

	/** Return the instance of the special TOP value. */
	public ValueNumber topValue() { return ValueNumber.topValue; }

	/** Return the instance of the special BOTTOM value. */
	public ValueNumber bottomValue() { return ValueNumber.bottomValue; }

	/** Return the instance of the special default value. */
	public ValueNumber defaultValue() { return ValueNumber.defaultValue; }

}

// vim:ts=4
