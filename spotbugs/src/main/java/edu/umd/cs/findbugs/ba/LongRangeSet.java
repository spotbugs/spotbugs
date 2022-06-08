package edu.umd.cs.findbugs.ba;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.Map.Entry;

public class LongRangeSet implements Iterable<LongRangeSet> {
    private static final Map<String, TypeLongRange> typeRanges;

    static {
        typeRanges = new HashMap<>();
        typeRanges.put("Z", new TypeLongRange(0, 1, "Z"));
        typeRanges.put("B", new TypeLongRange(Byte.MIN_VALUE, Byte.MAX_VALUE, "B"));
        typeRanges.put("S", new TypeLongRange(Short.MIN_VALUE, Short.MAX_VALUE, "S"));
        typeRanges.put("I", new TypeLongRange(Integer.MIN_VALUE, Integer.MAX_VALUE, "I"));
        typeRanges.put("J", new TypeLongRange(Long.MIN_VALUE, Long.MAX_VALUE, "J"));
        typeRanges.put("C", new TypeLongRange(Character.MIN_VALUE, Character.MAX_VALUE, "C"));
    }

    public static boolean isSignatureSupported(String signature) {
        return typeRanges.containsKey(signature);
    }

    private SortedMap<Long, Long> map = new ConcurrentSkipListMap<>();
    private TypeLongRange range;

    public LongRangeSet(String type) {
        TypeLongRange range = typeRanges.get(type);
        if (range == null) {
            throw new IllegalArgumentException("Type is not supported: " + type);
        }
        map.put(range.min, range.max);
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
            map.put(from, to);
        }
    }

    private LongRangeSet(TypeLongRange range) {
        this.range = range;
    }

    private LongRangeSet(TypeLongRange range, SortedMap<Long, Long> map) {
        this.range = range;
        this.map = map;
    }

    public LongRangeSet(LongRangeSet other) {
        range = other.range;
        map = new ConcurrentSkipListMap<>(other.map);
    }

    public LongRangeSet gt(long value) {
        if (value == Long.MAX_VALUE) {
            return new LongRangeSet(range);
        }
        return ge(value + 1);
    }

    public LongRangeSet ge(long value) {
        return new LongRangeSet(range, splitGE(map, value));
    }

    public LongRangeSet lt(long value) {
        if (value == Long.MIN_VALUE) {
            return new LongRangeSet(range);
        }
        return le(value - 1);
    }

    public LongRangeSet le(long value) {
        return new LongRangeSet(range, splitLE(map, value));
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

        SortedMap<Long, Long> newMap = splitLE(map, value - 1);
        newMap.putAll(splitGE(map, value + 1));
        return new LongRangeSet(range, newMap);
    }

    public void addBordersTo(Set<Long> borders) {
        if (map.isEmpty()) {
            return;
        }

        long min = map.firstKey();
        borders.add(min);
        if (min > Long.MIN_VALUE) {
            borders.add(min - 1);
        }

        long max = map.get(map.lastKey());
        borders.add(max);
        if (max < Long.MAX_VALUE) {
            borders.add(max + 1);
        }
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
        if (map.isEmpty()) {
            return false;
        }
        if (value == Long.MAX_VALUE) {
            return map.get(map.lastKey()) == Long.MAX_VALUE;
        }
        SortedMap<Long, Long> headMap = map.headMap(value + 1);
        if (headMap.isEmpty()) {
            return false;
        }
        return headMap.get(headMap.lastKey()) >= value;
    }

    public boolean intersects(LongRangeSet other) {
        for (Entry<Long, Long> entry : map.entrySet()) {
            SortedMap<Long, Long> subMap = entry.getValue() == Long.MAX_VALUE ? other.map.tailMap(entry.getKey())
                    : other.map
                            .subMap(entry.getKey(), entry.getValue() + 1);
            if (!subMap.isEmpty()) {
                return true;
            }
            SortedMap<Long, Long> headMap = other.map.headMap(entry.getKey());
            if (!headMap.isEmpty() && headMap.get(headMap.lastKey()) >= entry.getKey()) {
                return true;
            }
        }
        return false;
    }

    private void restrict(Long start, Long end) {
        map = splitGE(map, start);
        map = splitLE(map, end);
    }

    public void restrict(String signature) {
        range = typeRanges.get(signature);
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
        map = new ConcurrentSkipListMap<>(map.tailMap(number));
        if (number <= lastValue) {
            map.put(number, lastValue);
        }
        return map;
    }

    private static SortedMap<Long, Long> splitLE(SortedMap<Long, Long> map, long number) {
        if (number == Long.MAX_VALUE) {
            return map;
        }

        map = new ConcurrentSkipListMap<>(map.headMap(number + 1));
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
        return map.isEmpty();
    }

    public boolean isFull() {
        if (map.size() != 1) {
            return false;
        }
        Long min = map.firstKey();
        Long max = map.get(min);
        return min <= range.min && max >= range.max;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<Long, Long> entry : map.entrySet()) {
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
        final Iterator<Entry<Long, Long>> iterator = map.entrySet().iterator();
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
        if (map.isEmpty()) {
            map.put(start, end);
            return;
        }

        SortedMap<Long, Long> headMap;
        if (end < Long.MAX_VALUE) {
            headMap = map.headMap(end + 1);
            Long tailEnd = map.remove(end + 1);
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
        headMap = map.headMap(start);
        if (!headMap.isEmpty()) {
            Long headStart = headMap.lastKey();
            Long headEnd = map.get(headStart);
            if (headEnd >= start - 1) {
                map.remove(headStart);
                start = headStart;
            }
        }
        map.subMap(start, end).clear();
        map.remove(end);
        map.put(start, end);
    }

    public LongRangeSet add(LongRangeSet rangeSet) {
        for (Entry<Long, Long> entry : rangeSet.map.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public boolean same(LongRangeSet rangeSet) {
        return map.equals(rangeSet.map);
    }
}
