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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author pugh
 */
public abstract class MergeMap<K, V> {

    public static class MinMap<K, V extends Comparable<? super V>> extends MergeMap<K, V> {
        @Override
        protected V mergeValues(V oldValue, V newValue) {

            if (oldValue.compareTo(newValue) > 0) {
                return newValue;
            }
            return oldValue;
        }
    }

    public static class MaxMap<K, V extends Comparable<? super V>> extends MergeMap<K, V> {

        @Override
        protected V mergeValues(V oldValue, V newValue) {

            if (oldValue.compareTo(newValue) < 0) {
                return newValue;
            }
            return oldValue;
        }
    }

    final Map<K, V> map;

    protected abstract V mergeValues(V oldValue, V newValue);

    public MergeMap() {
        map = new HashMap<K, V>();
    }

    public MergeMap(Map<K, V> map) {
        this.map = map;
    }

    public V put(K k, V v) {
        V currentValue = map.get(k);
        if (currentValue == null) {
            map.put(k, v);
            return v;
        }
        V result = mergeValues(currentValue, v);
        if (currentValue != result) {
            map.put(k, v);
        }

        return result;
    }

    public V get(K k) {
        return map.get(k);
    }

    public boolean containsKey(K k) {
        return map.containsKey(k);
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    public static void main(String args[]) {

        MergeMap<String, Integer> m = new MaxMap<String, Integer>();

        m.put("a", 1);
        m.put("a", 2);
        m.put("b", 2);
        m.put("b", 1);
        System.out.println(m.entrySet());

    }

}
