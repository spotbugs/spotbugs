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

import edu.umd.cs.findbugs.util.MapCache;
import edu.umd.cs.findbugs.util.Util;



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
	static MapCache<ValueNumber, ValueNumber> cache = new MapCache<ValueNumber, ValueNumber>(200);
	
	static int valueNumbersCreated = 0;
	static int valueNumbersReused = 0;
	
	public static synchronized  ValueNumber createValueNumber(int number, int flags) {
	    ValueNumber probe = new ValueNumber(number, flags);
	    ValueNumber result = cache.get(probe);
	    if (result != null) {
	    	valueNumbersReused++;
	    	return result;
	    }
	    cache.put(probe, probe);
	    valueNumbersCreated++;
	    return probe;
    }
	public static ValueNumber createValueNumber(int number) {
	    return createValueNumber(number, 0);
    }
	static {
		Util.runLogAtShutdown(new Runnable(){

			public void run() {
	            System.out.println("Value number statistics: " + valueNumbersCreated + " created, " + valueNumbersReused + " reused");
	            
            }});
	}
	/**
	 * The value number.
	 */
	final int number;

	/**
	 * Flags representing meta information about the value.
	 */
	final int flags;

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
	private ValueNumber(int number) {
		this.number = number;
		this.flags = 0;
	}
	private ValueNumber(int number, int flags) {
		this.number = number;
		this.flags = flags;
	}
	public int getNumber() {
		return number;
	}

	public int getFlags() {
		return flags;
	}

	@Deprecated
	public void setFlags(int flags) {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	public void setFlag(int flag) {
		throw new UnsupportedOperationException();
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
		return number*17+flags;
	}
	@Override
	public boolean equals(Object o) {
		if (o instanceof ValueNumber) {
			return number == ((ValueNumber)o).number && flags == ((ValueNumber)o).flags;
		}
		return false;
	}
	public int compareTo(ValueNumber other) {
		int result = number - other.number;
		if (result != 0) return result;
		return flags - other.flags;

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
