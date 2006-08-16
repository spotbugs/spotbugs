/*
 * Bytecode Analysis Framework
 * Copyright (C) 2005, University of Maryland
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
package edu.umd.cs.findbugs.ba.npe;

import java.util.BitSet;

/**
 * Method property recording which parameters are (or should be)
 * non-null, meaning that null values should not be passed
 * as their arguments.
 * 
 * @author David Hovemeyer
 */
public class ParameterNullnessProperty {
	/**
	 * Maximum number of parameters that can be represented by a ParameterNullnessProperty.
	 */
	public static final int MAX_PARAMS = 32;
	
	private int nonNullParamSet;
	
	/**
	 * Constructor.
	 * Parameters are all assumed not to be non-null.
	 */
	public ParameterNullnessProperty() {
		this.nonNullParamSet = 0;
	}
	
	/**
	 * Get the non-null param bitset.
	 * 
	 * @return the non-null param bitset
	 */
	int getNonNullParamSet() {
		return nonNullParamSet;
	}
	
	/**
	 * Set the non-null param bitset.
	 * 
	 * @param nonNullParamSet the non-null param bitset
	 */
	void setNonNullParamSet(int nonNullParamSet) {
		this.nonNullParamSet = nonNullParamSet;
	}

	/**
	 * Set the non-null param set from given BitSet.
	 * 
	 * @param nonNullSet BitSet indicating which parameters are
	 *                              non-null
	 */
	public void setNonNullParamSet(BitSet nonNullSet) {
		for (int i = 0; i < 32; ++i) {
			setNonNull(i, nonNullSet.get(i));
		}
	}
	
	/**
	 * Set whether or not a parameter might be non-null.
	 * 
	 * @param param              the parameter index
	 * @param nonNull true if the parameter might be non-null, false otherwise
	 */
	public void setNonNull(int param, boolean nonNull) {
		if (param < 0 || param > 31)
			return;
		if (nonNull) {
			nonNullParamSet |= (1 << param);
		} else {
			nonNullParamSet &= ~(1 << param);
		}
	}
	
	/**
	 * Return whether or not a parameter might be non-null.
	 * 
	 * @param param the parameter index
	 * @return true if the parameter might be non-null, false otherwise
	 */
	public boolean isNonNull(int param) {
		if (param < 0 || param > 31)
			return false;
		else
			return (nonNullParamSet & (1 << param)) != 0;
	}
	
	/**
	 * Given a bitset of null arguments passed to the method represented
	 * by this property, return a bitset indicating which null arguments
	 * correspond to an non-null param.
	 * 
	 * @param nullArgSet bitset of null arguments
	 * @return bitset intersecting null arguments and non-null params
	 */
	public BitSet getViolatedParamSet(BitSet nullArgSet) {
		BitSet result = new BitSet();
		for (int i = 0; i < 32; ++i) {
			result.set(i, nullArgSet.get(i) && isNonNull(i));
		}
		return result;
	}
	
	/**
	 * Return whether or not the set of non-null parameters
	 * is empty.
	 * 
	 * @return true if the set is empty, false if it contains at least one parameter
	 */
	public boolean isEmpty() {
		return nonNullParamSet == 0;
	}
	
	@Override
         public String toString() {
		StringBuffer buf = new StringBuffer();
		
		buf.append('{');
		for (int i = 0; i < 32; ++i) {
			if (isNonNull(i)) {
				if (buf.length() > 1)
					buf.append(',');
				buf.append(i);
			}
		}
		buf.append('}');
		
		return buf.toString();
	}

	/**
	 * Intersect this set with the given set.
	 * Useful for summarizing the properties of multiple methods.
	 * 
	 * @param targetDerefParamSet another set
	 */
	public void intersectWith(ParameterNullnessProperty targetDerefParamSet) {
		nonNullParamSet &= targetDerefParamSet.nonNullParamSet;
	}
}
