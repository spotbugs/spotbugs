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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.Frame;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.util.Strings;

public class IsNullValueFrame extends Frame<IsNullValue> {
	private IsNullConditionDecision decision;
	private boolean trackValueNumbers;
	private Map<ValueNumber, IsNullValue> knownValueMap;

	public IsNullValueFrame(int numLocals, boolean trackValueNumbers) {
		super(numLocals);
		this.trackValueNumbers = trackValueNumbers;
		if (trackValueNumbers) {
			this.knownValueMap = new HashMap<ValueNumber, IsNullValue>();
		}
	}

	public void toExceptionValues() {
		for (int i = 0; i < getNumSlots(); ++i)
			setValue(i, getValue(i).toExceptionValue());

		if (trackValueNumbers) {
			Map<ValueNumber, IsNullValue> replaceMap = new HashMap<ValueNumber, IsNullValue>();
			for (Map.Entry<ValueNumber, IsNullValue> entry : knownValueMap.entrySet()) {
				replaceMap.put(entry.getKey(), entry.getValue().toExceptionValue());
			}
			this.knownValueMap = replaceMap;
		}
	}

	public void setDecision(@CheckForNull IsNullConditionDecision decision) {
		this.decision = decision;
	}

	public @CheckForNull IsNullConditionDecision getDecision() {
		return decision;
	}
	
	public void setKnownValue(ValueNumber valueNumber, IsNullValue knownValue) {
		knownValueMap.put(valueNumber, knownValue);
	}
	
	public IsNullValue getKnownValue(ValueNumber valueNumber) {
		return knownValueMap.get(valueNumber);
	}
	
	public Collection<Map.Entry<ValueNumber, IsNullValue>> getKnownValueMapEntrySet() {
		if (trackValueNumbers) {
			return knownValueMap.entrySet();
		} else {
			return null;
		}
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
		knownValueMap.clear();
		knownValueMap.putAll(replaceMap);
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.Frame#copyFrom(edu.umd.cs.findbugs.ba.Frame)
	 */
	@Override
	public void copyFrom(Frame<IsNullValue> other) {
		super.copyFrom(other);
		if (trackValueNumbers) {
			knownValueMap = new HashMap<ValueNumber, IsNullValue>(((IsNullValueFrame)other).knownValueMap);
		}
	}

	@Override
	public String toString() {
		String result = super.toString();
		if (decision != null) {
			result = result + ", [decision=" + decision.toString() + "]";
		}
		if (knownValueMap != null) {
//			result = result + ", [known=" + knownValueMap.toString() + "]";
			StringBuffer buf = new StringBuffer();
			buf.append("{");
			boolean first = true;
			for (Map.Entry<ValueNumber, IsNullValue> entry : knownValueMap.entrySet()) {
				if (!first) {
					buf.append(", ");
				} else {
					first = false;
				}
				buf.append(Strings.trimComma(entry.getKey().toString()));
				buf.append("->");
				buf.append(Strings.trimComma(entry.getValue().toString()));
			}
			buf.append("}");
			result += ", [known=" + buf.toString() + "]";
		}
		return result;
	}

	/**
	 * Downgrade all NSP values in frame.
	 * Should be called when a non-exception control split occurs.
	 */
	public void downgradeOnControlSplit() {
		final int numSlots = getNumSlots();
		for (int i = 0; i < numSlots; ++i) {
			IsNullValue value = getValue(i);
			value = value.downgradeOnControlSplit();
			setValue(i, value);
		}
		
		if (knownValueMap != null) {
			for (Map.Entry<ValueNumber, IsNullValue> entry : knownValueMap.entrySet()) {
				entry.setValue(entry.getValue().downgradeOnControlSplit());
			}
		}
	}
}

// vim:ts=4
