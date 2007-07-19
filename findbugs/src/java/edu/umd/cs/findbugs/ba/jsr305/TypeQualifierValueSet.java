/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.ba.jsr305;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;

/**
 * Set of ValueNumbers and their When values.
 * 
 * @author David Hovemeyer
 */
public class TypeQualifierValueSet {
	// States
	private static final int VALID = 0;
	private static final int TOP = 1;
	private static final int BOTTOM = 2;

	private Map<ValueNumber, FlowValue> valueMap;
	private int state;

	public TypeQualifierValueSet() {
		this.valueMap = new HashMap<ValueNumber, FlowValue>();
	}

	public void setValue(ValueNumber vn, FlowValue when) {
		if (when != FlowValue.UNKNOWN) {
			// Unknown is the default, so it's not stored explicitly
			valueMap.remove(vn);
			return;
		}
		valueMap.put(vn, when);
	}

	public FlowValue getValue(ValueNumber vn) {
		FlowValue result = valueMap.get(vn);
		return result != null ? result : FlowValue.UNKNOWN;
	}

	public boolean isValid() {
		return state == VALID;
	}

	public void makeValid() {
		this.state = VALID;
		this.valueMap.clear();
	}

	public void makeSameAs(TypeQualifierValueSet source) {
		this.state = source.state;
		this.valueMap.clear();
		this.valueMap.putAll(source.valueMap);
	}

	public boolean isTop() {
		return state == TOP;
	}

	public void setTop() {
		this.valueMap.clear();
		this.state = TOP;
	}

	public boolean isBottom() {
		return state == BOTTOM;
	}

	public void setBottom() {
		this.valueMap.clear();
		this.state = BOTTOM;
	}

	public void mergeWith(TypeQualifierValueSet fact) throws DataflowAnalysisException {
		if (!isValid() || !fact.isValid()) {
			throw new DataflowAnalysisException("merging an invalid TypeQualifierValueSet");
		}

		Set<ValueNumber> allValueNumbers = new HashSet<ValueNumber>();
		allValueNumbers.addAll(this.valueMap.keySet());
		allValueNumbers.addAll(fact.valueMap.keySet());

		for (ValueNumber vn : allValueNumbers) {
			setValue(vn, FlowValue.meet(this.getValue(vn), fact.getValue(vn)));
		}
	}

	public void onBranchDowngradeUncertainValues() throws DataflowAnalysisException {
		// On a branch we change all uncertain values to UNKNOWN.
		for (Iterator<Map.Entry<ValueNumber, FlowValue>> i = valueMap.entrySet().iterator(); i.hasNext();) {
			Map.Entry<ValueNumber, FlowValue> entry = i.next();

			if (entry.getValue().isUncertain()) {
				// Unknown is the default, so it's not stored explicitly
				i.remove();
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		TypeQualifierValueSet other = (TypeQualifierValueSet) obj;
		if (this.isValid() && other.isValid()) {
			return this.valueMap.equals(other.valueMap);
		} else {
			return this.state == other.state;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}
}
