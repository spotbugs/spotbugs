/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.util;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author pwilliam
 */
public class FractionalMultiset<K> {
    final Map<K, Double> map;

    public FractionalMultiset() {
        map = new HashMap<K, Double>();
    }

    public FractionalMultiset(Map<K, Double> map) {
        this.map = map;
    }

    public void clear() {
        map.clear();
    }

    public int numKeys() {
        return map.size();
    }

    public void add(K k, double val) {
        Double v = map.get(k);
        if (v == null) {
            map.put(k, val);
        } else {
            map.put(k, v + val);
        }
    }

    public double getValue(K k) {
        Double v = map.get(k);
        if (v == null) {
            return 0;
        }
        return v;
    }

    public void turnTotalIntoAverage(Multiset<K> counts) {
        for (Map.Entry<K, Double> e : map.entrySet()) {
            int count = counts.getCount(e.getKey());
            if (count == 0) {
                e.setValue(Double.NaN);
            } else {
                e.setValue(e.getValue() / count);
            }

        }
    }

    public Iterable<Map.Entry<K, Double>> entrySet() {
        return map.entrySet();
    }

    @SuppressFBWarnings("DMI_ENTRY_SETS_MAY_REUSE_ENTRY_OBJECTS")
    public Iterable<Map.Entry<K, Double>> entriesInDecreasingOrder() {
        TreeSet<Map.Entry<K, Double>> result = new TreeSet<Map.Entry<K, Double>>(new DecreasingOrderEntryComparator<K>());
        result.addAll(map.entrySet());
        if (result.size() != map.size()) {
            throw new IllegalStateException("Map " + map.getClass().getSimpleName()
                    + " reuses Map.Entry objects; entrySet can't be passed to addAll");
        }
        return result;
    }

    @SuppressFBWarnings("DMI_ENTRY_SETS_MAY_REUSE_ENTRY_OBJECTS")
    public Iterable<Map.Entry<K, Double>> entriesInIncreasingOrder() {
        TreeSet<Map.Entry<K, Double>> result = new TreeSet<Map.Entry<K, Double>>(new DecreasingOrderEntryComparator<K>());
        result.addAll(map.entrySet());
        if (result.size() != map.size()) {
            throw new IllegalStateException("Map " + map.getClass().getSimpleName()
                    + " reuses Map.Entry objects; entrySet can't be passed to addAll");
        }
        return result;
    }


    private static <E> int compareValues(Entry<E, Double> o1, Entry<E, Double> o2) {
        double c1 = o1.getValue();
        double c2 = o2.getValue();
        if (c1 < c2) {
            return 1;
        }
        if (c1 > c2) {
            return -1;
        }
        return System.identityHashCode(o1.getKey()) - System.identityHashCode(o2.getKey());
    }

    static class DecreasingOrderEntryComparator<E> implements Comparator<Map.Entry<E, Double>>, Serializable {
        @Override
        public int compare(Entry<E, Double> o1, Entry<E, Double> o2) {
            return compareValues(o1, o2);
        }
    }

    static class IncreasingOrderEntryComparator<E> implements Comparator<Map.Entry<E, Double>>, Serializable {
        @Override
        public int compare(Entry<E, Double> o1, Entry<E, Double> o2) {
            return -compareValues(o1, o2);
        }
    }

}
