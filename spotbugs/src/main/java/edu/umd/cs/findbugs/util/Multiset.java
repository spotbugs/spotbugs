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
import java.util.Set;
import java.util.TreeSet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author pwilliam
 */
public class Multiset<K> {
    final Map<K, Integer> map;

    public Multiset() {
        map = new HashMap<K, Integer>();
    }

    public Multiset(Map<K, Integer> map) {
        this.map = map;
    }

    public Multiset(Multiset<K> mset) {
        this.map = new HashMap<K, Integer>(mset.map);
    }

    public void clear() {
        map.clear();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public int numKeys() {
        return map.size();
    }

    public void add(K k) {
        add(k, 1);
    }

    public boolean remove(K k) {
        Integer v = map.get(k);
        if (v == null || v.intValue() == 0) {
            return false;
        }
        if (v.intValue() == 1) {
            map.remove(k);
            return true;
        }
        map.put(k, v.intValue() - 1);
        return true;
    }

    public void add(K k, int val) {
        Integer v = map.get(k);
        if (v == null) {
            map.put(k, val);
        } else {
            map.put(k, v + val);
        }
    }

    public void addAll(Iterable<K> c) {
        for (K k : c) {
            add(k);
        }
    }

    public int getCount(K k) {
        Integer v = map.get(k);
        if (v == null) {
            return 0;
        }
        return v;
    }

    public Iterable<Map.Entry<K, Integer>> entrySet() {
        return map.entrySet();
    }

    public Set<K> uniqueKeys() {
        return map.keySet();
    }

    @SuppressFBWarnings("DMI_ENTRY_SETS_MAY_REUSE_ENTRY_OBJECTS")
    public Iterable<Map.Entry<K, Integer>> entriesInDecreasingFrequency() {
        TreeSet<Map.Entry<K, Integer>> result = new TreeSet<Map.Entry<K, Integer>>(new EntryComparator<K>());
        result.addAll(map.entrySet());
        if (result.size() != map.size()) {
            throw new IllegalStateException("Map " + map.getClass().getSimpleName()
                    + " reuses Map.Entry objects; entrySet can't be passed to addAll");
        }
        return result;
    }

    static class EntryComparator<E> implements Comparator<Map.Entry<E, Integer>>, Serializable {

        @Override
        public int compare(Entry<E, Integer> o1, Entry<E, Integer> o2) {
            int c1 = o1.getValue();
            int c2 = o2.getValue();
            if (c1 < c2) {
                return 1;
            }
            if (c1 > c2) {
                return -1;
            }
            return System.identityHashCode(o1.getKey()) - System.identityHashCode(o2.getKey());
        }

    }

}
