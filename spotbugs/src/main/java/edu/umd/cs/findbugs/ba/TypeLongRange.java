package edu.umd.cs.findbugs.ba;

import java.util.Set;

public class TypeLongRange {
    long min, max;
    String signature;

    public TypeLongRange(long min, long max, String signature) {
        this.min = min;
        this.max = max;
        this.signature = signature;
    }

    public void addBordersTo(Set<Long> borders) {
        borders.add(min);
        if (min > Long.MIN_VALUE) {
            borders.add(min - 1);
        }
        borders.add(max);
        if (max < Long.MAX_VALUE) {
            borders.add(max + 1);
        }
    }
}
