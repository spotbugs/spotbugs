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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.ba.Frame;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberAnalysisFeatures;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.util.Strings;
import edu.umd.cs.findbugs.util.Util;

public class IsNullValueFrame extends Frame<IsNullValue> {

    static class PointerEqualityInfo {
        final ValueNumber addr1, addr2;

        final boolean areEqual;

        public PointerEqualityInfo(ValueNumber addr1, ValueNumber addr2, boolean areEqual) {
            if (addr1.getNumber() > addr2.getNumber()) {
                ValueNumber tmp = addr1;
                addr1 = addr2;
                addr2 = tmp;
            }
            this.addr1 = addr1;
            this.addr2 = addr2;
            this.areEqual = areEqual;
        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PointerEqualityInfo)) {
                return false;
            }
            PointerEqualityInfo other = (PointerEqualityInfo) obj;
            return this.addr1.equals(other.addr1) && this.addr2.equals(other.addr2) && this.areEqual == other.areEqual;
        }

    }

    private IsNullConditionDecision decision;

    private final boolean trackValueNumbers;

    public boolean isTrackValueNumbers() {
        return trackValueNumbers;
    }

    private Map<ValueNumber, IsNullValue> knownValueMap;

    public IsNullValueFrame(int numLocals, boolean trackValueNumbers) {
        super(numLocals);
        this.trackValueNumbers = trackValueNumbers;
        if (trackValueNumbers) {
            this.knownValueMap = new HashMap<ValueNumber, IsNullValue>(3);
        }
    }

    public void cleanStaleKnowledge(ValueNumberFrame vnaFrameAfter) {
        if (vnaFrameAfter.isTop() && !isTop()) {
            throw new IllegalArgumentException("VNA frame is top");
        }
        if (!trackValueNumbers) {
            return;
        }
        if (!ValueNumberAnalysisFeatures.REDUNDANT_LOAD_ELIMINATION) {
            return;
        }
        for (Iterator<ValueNumber> i = knownValueMap.keySet().iterator(); i.hasNext();) {
            ValueNumber v = i.next();
            if (vnaFrameAfter.getLoad(v) == null) {
                if (IsNullValueAnalysis.DEBUG) {
                    System.out.println("PURGING " + v);
                }
                i.remove();
            }
        }

    }

    @Override
    public void setTop() {
        super.setTop();
        if (trackValueNumbers) {
            knownValueMap.clear();
        }
        decision = null;
    }

    public void toExceptionValues() {
        for (int i = 0; i < getNumSlots(); ++i) {
            setValue(i, getValue(i).toExceptionValue());
        }

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

    public @CheckForNull
    IsNullConditionDecision getDecision() {
        return decision;
    }

    public void setKnownValue(@Nonnull ValueNumber valueNumber, @Nonnull IsNullValue knownValue) {
        assert trackValueNumbers;
        if (valueNumber == null || knownValue == null) {
            throw new NullPointerException();
        }
        knownValueMap.put(valueNumber, knownValue);
        if (IsNullValueAnalysis.DEBUG) {
            System.out.println("Updated information for " + valueNumber);
            System.out.println("                    now " + this);
        }
    }

    public void useNewValueNumberForLoad(ValueNumber oldValueNumber, ValueNumber newValueNumber) {
        if (oldValueNumber == null || newValueNumber == null) {
            throw new NullPointerException();
        }
        if (newValueNumber.equals(oldValueNumber) || !trackValueNumbers) {
            return;
        }
        IsNullValue isNullValue = knownValueMap.get(oldValueNumber);
        if (isNullValue != null) {
            knownValueMap.put(newValueNumber, isNullValue);
            knownValueMap.remove(oldValueNumber);
        }
    }

    public @CheckForNull
    IsNullValue getKnownValue(ValueNumber valueNumber) {
        assert trackValueNumbers;
        return knownValueMap.get(valueNumber);
    }

    public Collection<ValueNumber> getKnownValues() {
        if (trackValueNumbers) {
            return knownValueMap.keySet();
        } else {
            return Collections.<ValueNumber> emptySet();
        }
    }

    public Collection<Map.Entry<ValueNumber, IsNullValue>> getKnownValueMapEntrySet() {
        if (trackValueNumbers) {
            return knownValueMap.entrySet();
        } else {
            return Collections.<Map.Entry<ValueNumber, IsNullValue>> emptySet();
        }
    }

    public void mergeKnownValuesWith(IsNullValueFrame otherFrame) {
        assert trackValueNumbers;
        if (IsNullValueAnalysis.DEBUG) {
            System.out.println("merge");
            System.out.println("     " + this);
            System.out.println(" with" + otherFrame);
        }
        Map<ValueNumber, IsNullValue> replaceMap = new HashMap<ValueNumber, IsNullValue>();
        for (Map.Entry<ValueNumber, IsNullValue> entry : knownValueMap.entrySet()) {
            IsNullValue otherKnownValue = otherFrame.knownValueMap.get(entry.getKey());
            if (otherKnownValue == null) {
                if (IsNullValueAnalysis.DEBUG) {
                    System.out.println("No match for " + entry.getKey());
                }
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
        if (IsNullValueAnalysis.DEBUG) {
            System.out.println("resulting in " + this);

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.Frame#copyFrom(edu.umd.cs.findbugs.ba.Frame)
     */
    @Override
    public void copyFrom(Frame<IsNullValue> other) {
        super.copyFrom(other);
        decision = ((IsNullValueFrame) other).decision;
        if (trackValueNumbers) {
            knownValueMap = Util.makeSmallHashMap(((IsNullValueFrame) other).knownValueMap);
        }
    }

    @Override
    public boolean sameAs(Frame<IsNullValue> other) {
        if (!(other instanceof IsNullValueFrame)) {
            return false;
        }
        if (!super.sameAs(other)) {
            return false;
        }
        IsNullValueFrame o2 = (IsNullValueFrame) other;
        if (!Util.nullSafeEquals(decision, o2.decision)) {
            return false;
        }
        if (trackValueNumbers && !Util.nullSafeEquals(knownValueMap, o2.knownValueMap)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        String result = super.toString();
        if (decision != null) {
            result = result + ", [decision=" + decision.toString() + "]";
        }
        if (knownValueMap != null) {
            // result = result + ", [known=" + knownValueMap.toString() + "]";
            StringBuilder buf = new StringBuilder();
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
     * Downgrade all NSP values in frame. Should be called when a non-exception
     * control split occurs.
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

