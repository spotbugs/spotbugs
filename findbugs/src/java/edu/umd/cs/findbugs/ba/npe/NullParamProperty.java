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

import edu.umd.cs.findbugs.ba.interproc.MethodProperty;

/**
 * Method property recording which parameters might be null,
 * as inferred from call sites.
 * 
 * @author David Hovemeyer
 */
public class NullParamProperty implements MethodProperty<NullParamProperty> {
	private int nullParamSet;
	
	/**
	 * Constructor.
	 * Parameters are all assumed to be non-null.
	 */
	public NullParamProperty() {
		this.nullParamSet = 0;
	}
	
	/**
	 * Get the null param bitset.
	 * 
	 * @return the null param bitset
	 */
	int getNullParamSet() {
		return nullParamSet;
	}
	
	/**
	 * Set the null param bitset.
	 * 
	 * @param nullParamSet the null param bitset
	 */
	void setNullParamSet(int nullParamSet) {
		this.nullParamSet = nullParamSet;
	}
	
	/**
	 * Set whether or not a parameter might be null.
	 * 
	 * @param param       the parameter index
	 * @param mightBeNull true if the parameter might be null, false otherwise
	 */
	public void setParamMightBeNull(int param, boolean mightBeNull) {
		if (param < 0 || param > 31)
			return;
		if (mightBeNull) {
			nullParamSet |= (1 << param);
		} else {
			nullParamSet &= ~(1 << param);
		}
	}
	
	/**
	 * Return whether or not a parameter might be null.
	 * 
	 * @param param the parameter index
	 * @return true if the parameter might be null, false otherwise
	 */
	public boolean paramMightBeNull(int param) {
		if (param < 0 || param > 31)
			return false;
		else
			return (nullParamSet & (1 << param)) != 0;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.interproc.MethodProperty#duplicate()
	 */
	public NullParamProperty duplicate() {
		NullParamProperty dup = new NullParamProperty();
		dup.nullParamSet = this.nullParamSet;
		return dup;
	}

}
