/*
 * SpotBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs.ba;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * This class stores the set of possible values that a variable can store at a given point of the CFG.
 * The set of these values is represented by a set of ranges.
 */

public class LongRangeSet implements Iterable<LongRangeSet> {
    private static final Map<String, TypeLongRange> TYPE_RANGES;

    static {
        Map<String, TypeLongRange> typeRanges = new HashMap<>();
        typeRanges.put("Z", new TypeLongRange(0, 1, "Z"));
        typeRanges.put("B", new TypeLongRange(Byte.MIN_VALUE, Byte.MAX_VALUE, "B"));
        typeRanges.put("S", new TypeLongRange(Short.MIN_VALUE, Short.MAX_VALUE, "S"));
        typeRanges.put("I", new TypeLongRange(Integer.MIN_VALUE, Integer.MAX_VALUE, "I"));
        typeRanges.put("J", new TypeLongRange(Long.MIN_VALUE, Long.MAX_VALUE, "J"));
        typeRanges.put("C", new TypeLongRange(Character.MIN_VALUE, Character.MAX_VALUE, "C"));
        TYPE_RANGES = Collections.unmodifiableMap(typeRanges);
    }

    public static boolean isSignatureSupported(String signature) {
        return TYPE_RANGES.containsKey(signature);
    }

    private SortedMap<Long, Long> rangeMap = new TreeMap<>();
    private TypeLongRange range;

    public LongRangeSet(String type) {
        TypeLongRange range = TYPE_RANGES.get(type);
        if (range == null) {
            throw new IllegalArgumentException("Type is not supported: " + type);
        }
        rangeMap.put(range.min, range.max);
        this.range = range;
    }

    private LongRangeSet(TypeLongRange range, long from, long to) {
        this.range = range;
        if (from < range.min) {
            from = range.min;
        }
        if (to > range.max) {
            to = range.max;
        }
        if (from <= to) {
            rangeMap.put(from, to);
        }
    }

    private LongRangeSet(TypeLongRange range) {
        this.range = range;
    }

    private LongRangeSet(TypeLongRange range, SortedMap<Long, Long> map) {
        this.range = range;
        this.rangeMap = map;
    }

    public LongRangeSet(LongRangeSet other) {
        range = other.range;
        rangeMap = new TreeMap<>(other.rangeMap);
    }

    public LongRangeSet gt(long value) {
        if (value == Long.MAX_VALUE) {
            return new LongRangeSet(range);
        }
        return ge(value + 1);
    }

    public LongRangeSet ge(long value) {
        return new LongRangeSet(range, splitGE(rangeMap, value));
    }

    public LongRangeSet lt(long value) {
        if (value == Long.MIN_VALUE) {
            return new LongRangeSet(range);
        }
        return le(value - 1);
    }

    public LongRangeSet le(long value) {
        return new LongRangeSet(range, splitLE(rangeMap, value));
    }

    public LongRangeSet eq(long value) {
        if (contains(value)) {
            return new LongRangeSet(range, value, value);
        } else {
            return new LongRangeSet(range);
        }
    }

    public LongRangeSet ne(long value) {
        if (value == Long.MIN_VALUE) {
            return gt(value);
        }

        if (value == Long.MAX_VALUE) {
            return lt(value);
        }

        SortedMap<Long, Long> newMap = splitLE(rangeMap, value - 1);
        newMap.putAll(splitGE(rangeMap, value + 1));
        return new LongRangeSet(range, newMap);
    }

    public Set<Long> getBorders() {
        Set<Long> borders = new HashSet<>();
        if (rangeMap.isEmpty()) {
            return borders;
        }

        long min = rangeMap.firstKey();
        borders.add(min);
        if (min > Long.MIN_VALUE) {
            borders.add(min - 1);
        }

        long max = rangeMap.get(rangeMap.lastKey());
        borders.add(max);
        if (max < Long.MAX_VALUE) {
            borders.add(max + 1);
        }

        return borders;
    }

    public LongRangeSet empty() {
        return new LongRangeSet(range);
    }

    public long getTypeMin() {
        return range.min;
    }

    public long getTypeMax() {
        return range.max;
    }

    public boolean inTypeRange(long value) {
        return value >= range.min && value <= range.max;
    }

    public boolean contains(long value) {
        if (rangeMap.isEmpty()) {
            return false;
        }
        if (value == Long.MAX_VALUE) {
            return rangeMap.get(rangeMap.lastKey()) == Long.MAX_VALUE;
        }
        SortedMap<Long, Long> headMap = rangeMap.headMap(value + 1);
        if (headMap.isEmpty()) {
            return false;
        }
        return headMap.get(headMap.lastKey()) >= value;
    }

    public boolean intersects(LongRangeSet other) {
        for (Entry<Long, Long> entry : rangeMap.entrySet()) {
            SortedMap<Long, Long> subMap = entry.getValue() == Long.MAX_VALUE ? other.rangeMap.tailMap(entry.getKey())
                    : other.rangeMap
                            .subMap(entry.getKey(), entry.getValue() + 1);
            if (!subMap.isEmpty()) {
                return true;
            }
            SortedMap<Long, Long> headMap = other.rangeMap.headMap(entry.getKey());
            if (!headMap.isEmpty() && headMap.get(headMap.lastKey()) >= entry.getKey()) {
                return true;
            }
        }
        return false;
    }

    private void restrict(Long start, Long end) {
        rangeMap = splitGE(rangeMap, start);
        rangeMap = splitLE(rangeMap, end);
    }

    public void restrict(String signature) {
        range = TYPE_RANGES.get(signature);
        restrict(range.min, range.max);
    }

    private static SortedMap<Long, Long> splitGE(SortedMap<Long, Long> map, long number) {
        if (number == Long.MIN_VALUE) {
            return map;
        }

        SortedMap<Long, Long> headMap = map.headMap(number);
        if (headMap.isEmpty()) {
            return map;
        }
        Long lastKey = headMap.lastKey();
        Long lastValue = headMap.get(lastKey);
        map = new TreeMap<>(map.tailMap(number));
        if (number <= lastValue) {
            map.put(number, lastValue);
        }
        return map;
    }

    private static SortedMap<Long, Long> splitLE(SortedMap<Long, Long> map, long number) {
        if (number == Long.MAX_VALUE) {
            return map;
        }

        map = new TreeMap<>(map.headMap(number + 1));
        if (map.isEmpty()) {
            return map;
        }

        Long lastKey = map.lastKey();
        Long lastValue = map.get(lastKey);

        if (number < lastValue) {
            map.put(lastKey, number);
        }
        return map;
    }

    public String getSignature() {
        return range.signature;
    }

    public boolean isEmpty() {
        return rangeMap.isEmpty();
    }

    public boolean isFull() {
        if (rangeMap.size() != 1) {
            return false;
        }
        Long min = rangeMap.firstKey();
        Long max = rangeMap.get(min);
        return min <= range.min && max >= range.max;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<Long, Long> entry : rangeMap.entrySet()) {
            if (sb.length() > 0) {
                sb.append("+");
            }
            if (entry.getKey().equals(entry.getValue())) {
                sb.append("{").append(entry.getKey()).append("}");
            } else {
                sb.append("[").append(entry.getKey()).append(", ").append(entry.getValue()).append("]");
            }
        }
        return sb.toString();
    }

    @Override
    public Iterator<LongRangeSet> iterator() {
        final Iterator<Entry<Long, Long>> iterator = rangeMap.entrySet().iterator();
        return new Iterator<LongRangeSet>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public LongRangeSet next() {
                Entry<Long, Long> entry = iterator.next();
                return new LongRangeSet(range, entry.getKey(), entry.getValue());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private void add(Long start, Long end) {
        if (rangeMap.isEmpty()) {
            rangeMap.put(start, end);
            return;
        }

        SortedMap<Long, Long> headMap;
        if (end < Long.MAX_VALUE) {
            headMap = rangeMap.headMap(end + 1);
            Long tailEnd = rangeMap.remove(end + 1);
            if (tailEnd != null) {
                end = tailEnd;
            }
            if (!headMap.isEmpty()) {
                tailEnd = headMap.get(headMap.lastKey());
                if (tailEnd > end) {
                    end = tailEnd;
                }
            }
        }
        headMap = rangeMap.headMap(start);
        if (!headMap.isEmpty()) {
            Long headStart = headMap.lastKey();
            Long headEnd = rangeMap.get(headStart);
            if (headEnd >= start - 1) {
                rangeMap.remove(headStart);
                start = headStart;
            }
        }
        rangeMap.subMap(start, end).clear();
        rangeMap.remove(end);
        rangeMap.put(start, end);
    }

    public LongRangeSet add(LongRangeSet rangeSet) {
        if (rangeSet == this) {
            return this;
        }

        for (Entry<Long, Long> entry : rangeSet.rangeMap.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public boolean same(LongRangeSet rangeSet) {
        return rangeMap.equals(rangeSet.rangeMap);
    }
}
