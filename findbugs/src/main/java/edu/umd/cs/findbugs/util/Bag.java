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

package edu.umd.cs.findbugs.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Simple implementation of a Bag
 *
 * @author pugh
 */
public class Bag<E> {
    final Map<E, Integer> map;

    public Bag() {
        map = new HashMap<E, Integer>();
    }

    public Bag(Map<E, Integer> map) {
        this.map = map;
    }

    public boolean add(E e) {
        Integer v = map.get(e);
        if (v == null) {
            map.put(e, 1);
        } else {
            map.put(e, v + 1);
        }
        return true;
    }

    public boolean add(E e, int count) {
        Integer v = map.get(e);
        if (v == null) {
            map.put(e, count);
        } else {
            map.put(e, v + count);
        }
        return true;
    }

    public Set<E> keySet() {
        return map.keySet();
    }

    public Collection<Map.Entry<E, Integer>> entrySet() {
        return map.entrySet();
    }

    public int getCount(E e) {
        Integer v = map.get(e);
        if (v == null) {
            return 0;
        } else {
            return v;
        }
    }

}
