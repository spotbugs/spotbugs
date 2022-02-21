package edu.umd.cs.findbugs.ba;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;

public class ValueRangeMap {
    private Map<ValueNumber, LongRangeSet> variableRanges;
    private Branch branch;

    public ValueRangeMap() {
        variableRanges = new HashMap<>();
    }

    public ValueRangeMap(ValueRangeMap other) {
        variableRanges = new HashMap<>(other.variableRanges);
        branch = other.branch;
    }

    public LongRangeSet getRange(ValueNumber vna) {
        return variableRanges.get(vna);
    }

    public void setRange(ValueNumber vna, LongRangeSet range) {
        variableRanges.put(vna, range);
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(@Nullable Branch branch) {
        this.branch = branch;
    }

    public void copyFrom(ValueRangeMap source) {
        variableRanges = new HashMap<ValueNumber, LongRangeSet>(source.variableRanges);
        branch = source.branch;
    }

    public void clear() {
        variableRanges.clear();
        branch = null;
    }

    public void meetWith(ValueRangeMap other) {
        for (Entry<ValueNumber, LongRangeSet> entry : other.variableRanges.entrySet()) {
            if (variableRanges.containsKey(entry.getKey())) {
                variableRanges.get(entry.getKey()).add(entry.getValue());
            } else {
                variableRanges.put(entry.getKey(), entry.getValue());
            }
        }

        addMissingEntries(other);

        if (other.branch != null) {
            branch = other.branch;
        }
    }

    public void addMissingEntries(ValueRangeMap other) {
        for (ValueNumber vn : variableRanges.keySet()) {
            if (!other.variableRanges.containsKey(vn)) {
                variableRanges.put(vn, new LongRangeSet(variableRanges.get(vn).getSignature()));
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ValueRangeMap)) {
            return false;
        }
        ValueRangeMap other = (ValueRangeMap) obj;
        return variableRanges.equals(other.variableRanges);
    }

    @Override
    public int hashCode() {
        return variableRanges.hashCode();
    }
}
