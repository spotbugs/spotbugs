package edu.umd.cs.findbugs.ba;

import java.util.HashSet;
import java.util.Set;

import edu.umd.cs.findbugs.ba.vna.ValueNumber;

public class Branch {
    final ValueNumber valueNumber;
    final LongRangeSet trueSet;
    final LongRangeSet trueReachedSet, falseReachedSet;
    final String trueCondition, falseCondition;
    final Number number;
    final Set<Long> numbers = new HashSet<>();
    final String varName;

    public Branch(ValueNumber valueNumber, String varName, String trueCondition, String falseCondition, LongRangeSet trueSet, Number number) {

        this.valueNumber = valueNumber;
        this.trueSet = trueSet;
        this.trueCondition = trueCondition;
        this.falseCondition = falseCondition;
        this.trueReachedSet = trueSet.empty();
        this.falseReachedSet = trueSet.empty();
        trueSet.addBordersTo(numbers);
        this.number = number;
        this.varName = varName;
    }

    public ValueNumber getValueNumber() {
        return valueNumber;
    }

    public LongRangeSet getTrueReachedSet() {
        return trueReachedSet;
    }

    public LongRangeSet getFalseReachedSet() {
        return falseReachedSet;
    }

    public String getVarName() {
        return varName;
    }

    public String getTrueCondition() {
        return trueCondition;
    }

    public String getFalseCondition() {
        return falseCondition;
    }

    public LongRangeSet getTrueSet() {
        return trueSet;
    }

    public Number getNumber() {
        return number;
    }

    public Set<Long> getNumbers() {
        return numbers;
    }
}
