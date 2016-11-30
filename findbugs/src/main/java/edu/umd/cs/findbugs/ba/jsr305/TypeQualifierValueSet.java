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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;

/**
 * Set of ValueNumbers and their corresponding FlowValues.
 *
 * @author David Hovemeyer
 */
public class TypeQualifierValueSet {
    // States
    enum State {
        VALID, TOP, BOTTOM
    }

    private final Map<ValueNumber, FlowValue> valueMap;

    private final Map<ValueNumber, Set<SourceSinkInfo>> whereAlways;

    private final Map<ValueNumber, Set<SourceSinkInfo>> whereNever;

    private State state = State.VALID;

    final boolean  isStrict;

    public TypeQualifierValueSet(TypeQualifierValue<?> typeQualifierValue) {
        this.valueMap = new HashMap<ValueNumber, FlowValue>(3);
        this.whereAlways = new HashMap<ValueNumber, Set<SourceSinkInfo>>(3);
        this.whereNever = new HashMap<ValueNumber, Set<SourceSinkInfo>>(3);
        this.state = State.TOP;
        isStrict = typeQualifierValue.isStrictQualifier();
    }

    public void modelSourceSink(SourceSinkInfo sourceSinkInfo) {
        assert sourceSinkInfo != null;
        ValueNumber vn = sourceSinkInfo.getValueNumber();
        FlowValue flowValue = FlowValue.flowValueFromWhen(sourceSinkInfo.getWhen());

        setValue(vn, flowValue);
        if (true) {
            switch (flowValue) {
            case ALWAYS:
                addSourceSinkInfo(whereAlways, vn, sourceSinkInfo);
                break;
            case NEVER:
                addSourceSinkInfo(whereNever, vn, sourceSinkInfo);
                break;
            default:
                break;
            }
        }/* else {

            if (flowValue.isYes()) {
                addSourceSinkInfo(whereAlways, vn, sourceSinkInfo);
            }

            if (flowValue.isNo()) {
                addSourceSinkInfo(whereNever, vn, sourceSinkInfo);
            }
        }*/
    }

    private void setValue(ValueNumber vn, FlowValue flowValue) {
        if (flowValue == FlowValue.TOP) {
            pruneValue(vn);
        } else {
            valueMap.put(vn, flowValue);
        }
    }

    static <K, V> void copyMapValue(Map<K, V> map, K from, K to) {
        if (!map.containsKey(from)) {
            return;
        }
        map.put(to, map.get(from));
    }

    void copyInfo(ValueNumber from, ValueNumber to) {
        if (state == State.TOP) {
            return;
        }
        copyMapValue(valueMap, from, to);
        copyMapValue(whereAlways, from, to);
        copyMapValue(whereNever, from, to);
    }

    private static void addSourceSinkInfo(Map<ValueNumber, Set<SourceSinkInfo>> sourceSinkInfoSetMap, ValueNumber vn,
            SourceSinkInfo sourceSinkInfo) {
        Set<SourceSinkInfo> sourceSinkInfoSet = sourceSinkInfoSetMap.get(vn);
        if (sourceSinkInfoSet == null) {
            sourceSinkInfoSet = new HashSet<SourceSinkInfo>(3);
            sourceSinkInfoSetMap.put(vn, sourceSinkInfoSet);
        }
        sourceSinkInfoSet.add(sourceSinkInfo);
    }

    public void pruneValue(ValueNumber vn) {
        assert isValid();
        valueMap.remove(vn);
        whereAlways.remove(vn);
        whereNever.remove(vn);
    }

    public Set<? extends SourceSinkInfo> getWhere(ValueNumber vn) {
        FlowValue f = getValue(vn);
        switch (f) {
        case ALWAYS:  return getSourceSinkInfoSet(whereAlways, vn);
        case NEVER:  return getSourceSinkInfoSet(whereNever, vn);
        }
        return Collections.emptySet();

    }
    public Set<? extends SourceSinkInfo> getWhereAlways(ValueNumber vn) {
        return getSourceSinkInfoSet(whereAlways, vn);
    }

    public Set<? extends SourceSinkInfo> getWhereNever(ValueNumber vn) {
        return getSourceSinkInfoSet(whereNever, vn);
    }

    private static Set<? extends SourceSinkInfo> getSourceSinkInfoSet(Map<ValueNumber, Set<SourceSinkInfo>> sourceSinkInfoSetMap,
            ValueNumber vn) {
        Set<SourceSinkInfo> sourceSinkInfoSet = sourceSinkInfoSetMap.get(vn);
        if (sourceSinkInfoSet == null || sourceSinkInfoSet.isEmpty()) {
            return Collections.emptySet();
        }

        return sourceSinkInfoSet;
    }
    private static Set<SourceSinkInfo> getOrCreateSourceSinkInfoSet(Map<ValueNumber, Set<SourceSinkInfo>> sourceSinkInfoSetMap,
            ValueNumber vn) {
        Set<SourceSinkInfo> sourceSinkInfoSet = sourceSinkInfoSetMap.get(vn);
        if (sourceSinkInfoSet == null) {
            sourceSinkInfoSet = new HashSet<SourceSinkInfo>(3);
            sourceSinkInfoSetMap.put(vn, sourceSinkInfoSet);
        }
        return sourceSinkInfoSet;
    }



    public FlowValue getValue(ValueNumber vn) {
        FlowValue result = valueMap.get(vn);
        return result != null ? result : FlowValue.TOP;
    }

    public Collection<? extends ValueNumber> getValueNumbers() {
        return valueMap.keySet();
    }

    public boolean isValid() {
        return state == State.VALID;
    }

    public void makeValid() {
        /*
         * this.state = State.VALID; this.valueMap.clear();
         * this.whereAlways.clear(); this.whereNever.clear();
         */
        reset(State.VALID);
    }

    public void makeSameAs(TypeQualifierValueSet source) {
        /*
         * this.state = source.state; this.valueMap.clear();
         */
        reset(source.state);
        this.valueMap.putAll(source.valueMap);
        copySourceSinkInfoSetMap(this.whereAlways, source.whereAlways);
        copySourceSinkInfoSetMap(this.whereNever, source.whereNever);
    }

    private void copySourceSinkInfoSetMap(Map<ValueNumber, Set<SourceSinkInfo>> dest, Map<ValueNumber, Set<SourceSinkInfo>> source) {
        dest.clear();

        for (Map.Entry<ValueNumber, Set<SourceSinkInfo>> entry : source.entrySet()) {
            HashSet<SourceSinkInfo> copy = new HashSet<SourceSinkInfo>(entry.getValue());
            dest.put(entry.getKey(), copy);
        }
    }

    public boolean isTop() {
        return state == State.TOP;
    }

    public void setTop() {
        /*
         * this.valueMap.clear(); this.state = State.TOP;
         */
        reset(State.TOP);
    }

    public boolean isBottom() {
        return state == State.BOTTOM;
    }

    public void setBottom() {
        /*
         * this.valueMap.clear(); this.state = State.BOTTOM;
         */
        reset(State.BOTTOM);
    }

    private void reset(State state) {
        valueMap.clear();
        whereAlways.clear();
        whereNever.clear();
        this.state = state;
    }

    public void propagateAcrossPhiNode(ValueNumber fromVN, ValueNumber toVN) {
        assert isValid();

        setValue(toVN, getValue(fromVN));

        // Propagate source/sink information
        transferSourceSinkInfoSet(whereAlways, fromVN, toVN);
        transferSourceSinkInfoSet(whereNever, fromVN, toVN);

        // Remove all information about the "from" value
        valueMap.remove(fromVN);
        whereAlways.remove(fromVN);
        whereNever.remove(fromVN);
    }

    private static void transferSourceSinkInfoSet(Map<ValueNumber, Set<SourceSinkInfo>> sourceSinkInfoSetMap, ValueNumber fromVN,
            ValueNumber toVN) {
        Set<? extends SourceSinkInfo> locSet = getSourceSinkInfoSet(sourceSinkInfoSetMap, fromVN);

        for (SourceSinkInfo loc : locSet) {
            addSourceSinkInfo(sourceSinkInfoSetMap, toVN, loc);
        }
    }

    public void mergeWith(TypeQualifierValueSet fact) throws DataflowAnalysisException {
        if (!isValid() || !fact.isValid()) {
            throw new DataflowAnalysisException("merging an invalid TypeQualifierValueSet");
        }

        Set<ValueNumber> interesting = new HashSet<ValueNumber>();
        interesting.addAll(this.valueMap.keySet());
        interesting.addAll(fact.valueMap.keySet());

        for (ValueNumber vn : interesting) {
            setValue(vn, FlowValue.meet(this.getValue(vn), fact.getValue(vn)));
            mergeSourceSinkInfoSets(this.whereAlways, fact.whereAlways, vn);
            mergeSourceSinkInfoSets(this.whereNever, fact.whereNever, vn);
        }
    }

    private void mergeSourceSinkInfoSets(Map<ValueNumber, Set<SourceSinkInfo>> sourceSinkInfoSetMapToUpdate,
            Map<ValueNumber, Set<SourceSinkInfo>> otherSourceSinkInfoSetMap, ValueNumber vn) {
        if (!otherSourceSinkInfoSetMap.containsKey(vn)) {
            return;
        }
        Set<? extends SourceSinkInfo> otherInfo = getSourceSinkInfoSet(otherSourceSinkInfoSetMap, vn);
        if (!otherInfo.isEmpty()) {
            Set<SourceSinkInfo> sourceSinkInfoSetToUpdate = getOrCreateSourceSinkInfoSet(sourceSinkInfoSetMapToUpdate, vn);
            sourceSinkInfoSetToUpdate.addAll(otherInfo);
        }
    }

    /*
     * (non-Javadoc)
     *
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

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        if (state != State.VALID) {
            return state.toString();
        }

        TreeSet<ValueNumber> interesting = new TreeSet<ValueNumber>();
        interesting.addAll(valueMap.keySet());

        StringBuilder buf = new StringBuilder();

        buf.append("{");
        //        boolean first = true;

        for (ValueNumber vn : interesting) {
            FlowValue value = getValue(vn);
            if (value == FlowValue.TOP ||  /* !isStrict && */ value == FlowValue.UNKNOWN) {
                continue;
            }
            if (buf.length() > 1) {
                buf.append(", ");
            }
            buf.append(valueNumberToString(vn, value));
        }

        buf.append("}");

        return buf.toString();
    }

    public String valueNumberToString(ValueNumber vn ) {
        return valueNumberToString(vn, getValue(vn));
    }
    public String valueNumberToString(ValueNumber vn, FlowValue value ) {
        StringBuilder buf = new StringBuilder();

        buf.append(vn.getNumber());
        buf.append("->");

        buf.append(value);
        if (value != FlowValue.TOP) {
            Set<? extends SourceSinkInfo> always = getSourceSinkInfoSet(whereAlways, vn);
            Set<? extends SourceSinkInfo> never = getSourceSinkInfoSet(whereNever, vn);
            if (value != FlowValue.UNKNOWN || !always.equals(never)) {
                buf.append("[");
                if (!always.isEmpty()) {
                    appendSourceSinkInfos(buf, "YES=", always);
                }
                if (!always.isEmpty() && !never.isEmpty()) {
                    buf.append(",");
                }
                if (!never.isEmpty()) {
                    appendSourceSinkInfos(buf, "NO=", never);
                }
                buf.append("]");
            }
        }

        return buf.toString();
    }

    private static void appendSourceSinkInfos(StringBuilder buf, String key, Set<? extends SourceSinkInfo> sourceSinkInfoSet) {
        TreeSet<SourceSinkInfo> sortedLocSet = new TreeSet<SourceSinkInfo>();
        sortedLocSet.addAll(sourceSinkInfoSet);
        boolean first = true;
        buf.append(key);
        buf.append("(");
        for (SourceSinkInfo loc : sortedLocSet) {
            if (first) {
                first = false;
            } else {
                buf.append(",");
            }
            buf.append(loc.getLocation().toCompactString());
        }
        buf.append(")");
    }
}
