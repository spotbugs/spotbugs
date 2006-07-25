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

package edu.umd.cs.findbugs.ba.npe;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.ba.Frame;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;

public class IsNullValueFrame extends Frame<IsNullValue> {
	private IsNullConditionDecision decision;
	private Map<ValueNumber, IsNullValue> knownValueMap;

	public IsNullValueFrame(int numLocals) {
		super(numLocals);
		if (IsNullValueAnalysisFeatures.TRACK_KNOWN_VALUES) {
			this.knownValueMap = new HashMap<ValueNumber, IsNullValue>();
		}
	}

	public void toExceptionValues() {
		for (int i = 0; i < getNumSlots(); ++i)
			setValue(i, getValue(i).toExceptionValue());

		if (IsNullValueAnalysisFeatures.TRACK_KNOWN_VALUES) {
			Map<ValueNumber, IsNullValue> replaceMap = new HashMap<ValueNumber, IsNullValue>();
			for (Map.Entry<ValueNumber, IsNullValue> entry : knownValueMap.entrySet()) {
				replaceMap.put(entry.getKey(), entry.getValue().toExceptionValue());
			}
			this.knownValueMap = replaceMap;
		}
	}

	public void setDecision(IsNullConditionDecision decision) {
		this.decision = decision;
	}

	public IsNullConditionDecision getDecision() {
		return decision;
	}
	
	public void setKnownValue(ValueNumber valueNumber, IsNullValue knownValue) {
		knownValueMap.put(valueNumber, knownValue);
	}
	
	public IsNullValue getKnownValue(ValueNumber valueNumber) {
		return knownValueMap.get(valueNumber);
	}
	
	public void mergeKnownValuesWith(IsNullValueFrame otherFrame) {
		Map<ValueNumber, IsNullValue> replaceMap = new HashMap<ValueNumber, IsNullValue>();
		for (Map.Entry<ValueNumber, IsNullValue> entry : knownValueMap.entrySet()) {
			IsNullValue otherKnownValue = otherFrame.knownValueMap.get(entry.getKey());
			if (otherKnownValue == null) {
				continue;
			}
			replaceMap.put(entry.getKey(), IsNullValue.merge(entry.getValue(), otherKnownValue));
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.Frame#copyFrom(edu.umd.cs.findbugs.ba.Frame)
	 */
	@Override
	public void copyFrom(Frame<IsNullValue> other) {
		super.copyFrom(other);
		knownValueMap = new HashMap<ValueNumber, IsNullValue>(((IsNullValueFrame)other).knownValueMap);
	}

	@Override
	public String toString() {
		String result = super.toString();
		if (decision != null) {
			result = result + ", [decision=" + decision.toString() + "]";
		}
		if (knownValueMap != null) {
			result = result + ", [known=" + knownValueMap.toString() + "]";
		}
		return result;
	}
}

// vim:ts=4
