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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Frame;
import edu.umd.cs.findbugs.ba.vna.MergeTree;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.util.Strings;
import edu.umd.cs.findbugs.util.Util;
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

	public void cleanStaleKnowledge(ValueNumberFrame vnaFrameAfter) {
		if (vnaFrameAfter.isTop() && !isTop()) throw new IllegalArgumentException("VNA frame is top");
		for(Iterator<ValueNumber> i = knownValueMap.keySet().iterator(); i.hasNext(); ) {
			ValueNumber v = i.next();
			if (vnaFrameAfter.getLoad(v) == null) {
				if (IsNullValueAnalysis.DEBUG) 
					System.out.println("PURGING " + v);
				i.remove();
			}
		}
		
	}
	@Override
	public void setTop() {
		super.setTop();
		knownValueMap.clear();
		decision = null;
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
		assert trackValueNumbers;
		knownValueMap.put(valueNumber, knownValue);
		if (IsNullValueAnalysis.DEBUG) {
			System.out.println("Updated information for " + valueNumber);
			System.out.println("                    now " + this);
		}
	}
	public void useNewValueNumberForLoad(ValueNumber oldValueNumber, ValueNumber newValueNumber) {
		if (newValueNumber.equals(oldValueNumber) || !trackValueNumbers) return;
		knownValueMap.put(newValueNumber, knownValueMap.get(oldValueNumber));
		knownValueMap.remove(oldValueNumber);
	}
	public IsNullValue getKnownValue(ValueNumber valueNumber) {
		assert trackValueNumbers;
		return knownValueMap.get(valueNumber);
	}
	
	public Collection<ValueNumber> getKnownValues() {
		if (trackValueNumbers) {
			return knownValueMap.keySet();
		} else {
			return Collections.EMPTY_SET;
		}
	}

	public Collection<Map.Entry<ValueNumber, IsNullValue>> getKnownValueMapEntrySet() {
		if (trackValueNumbers) {
			return knownValueMap.entrySet();
		} else {
			return Collections.EMPTY_SET;
		}
	}
	
	public void mergeKnownValuesWith(IsNullValueFrame otherFrame) {
		assert trackValueNumbers;
		Map<ValueNumber, IsNullValue> replaceMap = new HashMap<ValueNumber, IsNullValue>();
		for (Map.Entry<ValueNumber, IsNullValue> entry : knownValueMap.entrySet()) {
			IsNullValue otherKnownValue = otherFrame.knownValueMap.get(entry.getKey());
			if (otherKnownValue == null) {
				continue;
			}
			IsNullValue mergedValue = IsNullValue.merge(entry.getValue(), otherKnownValue);
			replaceMap.put(entry.getKey(), mergedValue);
			if (IsNullValueAnalysis.DEBUG && !mergedValue.equals(entry.getValue())) {

					System.out.println("Updated information for " + entry.getKey());
					System.out.println("                    was " + entry.getValue());
					System.out.println("           merged value " + mergedValue);

			}
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
		decision = ((IsNullValueFrame)other).decision;
		if (trackValueNumbers) {
			knownValueMap = new HashMap<ValueNumber, IsNullValue>(((IsNullValueFrame)other).knownValueMap);
		}
	}
	
	@Override
	public boolean sameAs(Frame<IsNullValue> other) {
		if (!(other instanceof IsNullValueFrame)) return false;
		if (!super.sameAs(other)) return false;
		IsNullValueFrame o2 = (IsNullValueFrame) other;
		if (!Util.nullSafeEquals(decision, o2.decision)) return false;
		if (trackValueNumbers && !Util.nullSafeEquals(knownValueMap, o2.knownValueMap)) return false;

		return true;
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
