/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.bcel.classfile.JavaClass;

/**
 * Provide a HashMap that can only grow to a specified maximum capacity, with
 * entries discarded using a LRU policy to keep the size of the HashMap within
 * that bound.
 *
 * @author pugh
 */
public class MapCache<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = 0L;

    int maxCapacity;

    /**
     * Create a new MapCache
     *
     * @param maxCapacity
     *            - maximum number of entries in the map
     */
    public MapCache(int maxCapacity) {
        super(4 * maxCapacity / 3 + 5, 0.75f, true);
        this.maxCapacity = maxCapacity;

    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        boolean result = size() > maxCapacity;
        if (false && result && eldest.getKey() instanceof JavaClass) {
            System.out.println("Dropping " + ((JavaClass) eldest.getKey()).getClassName());
        }
        return result;
    }

}
