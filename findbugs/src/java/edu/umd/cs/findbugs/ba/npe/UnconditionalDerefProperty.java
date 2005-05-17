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

import edu.umd.cs.findbugs.ba.interproc.MethodProperty;

/**
 * Method property recording which parameters might be 
 * dereferenced unconditionally.
 * 
 * @author David Hovemeyer
 */
public class UnconditionalDerefProperty implements MethodProperty<UnconditionalDerefProperty> {
	private int unconditionalDerefParamSet;
	
	/**
	 * Constructor.
	 * Parameters are all assumed not to be unconditionally dereferenced.
	 */
	public UnconditionalDerefProperty() {
		this.unconditionalDerefParamSet = 0;
	}
	
	/**
	 * Get the unconditional deref bitset.
	 * 
	 * @return the unconditional deref bitset
	 */
	int getUnconditionalDerefParamSet() {
		return unconditionalDerefParamSet;
	}
	
	/**
	 * Set the unconditional deref bitset.
	 * 
	 * @param unconditionalDerefParamSet the unconditional deref bitset
	 */
	void setUnconditionalDerefParamSet(int unconditionalDerefParamSet) {
		this.unconditionalDerefParamSet = unconditionalDerefParamSet;
	}
	
	/**
	 * Set whether or not a parameter might be unconditionally dereferenced.
	 * 
	 * @param param              the parameter index
	 * @param unconditionalDeref true if the parameter might be unconditionally dereferenced, false otherwise
	 */
	public void setUnconditionalDeref(int param, boolean unconditionalDeref) {
		if (param < 0 || param > 31)
			return;
		if (unconditionalDeref) {
			unconditionalDerefParamSet |= (1 << param);
		} else {
			unconditionalDerefParamSet &= ~(1 << param);
		}
	}
	
	/**
	 * Return whether or not a parameter might be unconditionally dereferenced.
	 * 
	 * @param param the parameter index
	 * @return true if the parameter might be unconditionally dereferenced, false otherwise
	 */
	public boolean isUnconditionalDeref(int param) {
		if (param < 0 || param > 31)
			return false;
		else
			return (unconditionalDerefParamSet & (1 << param)) != 0;
	}
	
	/**
	 * Given a bitset of null arguments passed to the method represented
	 * by this property, return a bitset indicating which null arguments
	 * correspond to an unconditionally dereferenced param.
	 * 
	 * @param nullArgSet bitset of null arguments
	 * @return bitset intersecting null arguments and unconditionally derefed params
	 */
	public BitSet getUnconditionallyDereferencedNullArgSet(BitSet nullArgSet) {
		BitSet result = new BitSet();
		for (int i = 0; i < 32; ++i) {
			result.set(i, nullArgSet.get(i) && isUnconditionalDeref(i));
		}
		return result;
	}
	
	/**
	 * Return whether or not the set of unconditionally dereferenced parameters
	 * is null.
	 * 
	 * @return true if the set is null, false if it contains at least one parameter
	 */
	public boolean isEmpty() {
		return unconditionalDerefParamSet == 0;
	}

	public void makeSameAs(UnconditionalDerefProperty other) {
		this.unconditionalDerefParamSet = other.unconditionalDerefParamSet;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		
		buf.append('{');
		for (int i = 0; i < 32; ++i) {
			if (isUnconditionalDeref(i)) {
				if (buf.length() > 1)
					buf.append(',');
				buf.append(i);
			}
		}
		buf.append('}');
		
		return buf.toString();
	}
}
