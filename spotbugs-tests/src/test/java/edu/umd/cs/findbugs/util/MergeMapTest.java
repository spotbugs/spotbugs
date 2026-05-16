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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MergeMapTest {

    @Test
    void minMapKeepsSmallestValue() {
        MergeMap.MinMap<String, Integer> map = new MergeMap.MinMap<>();
        map.put("key", 5);
        map.put("key", 3);
        map.put("key", 8);
        assertEquals(3, map.get("key"));
    }

    @Test
    void maxMapKeepsLargestValue() {
        MergeMap.MaxMap<String, Integer> map = new MergeMap.MaxMap<>();
        map.put("key", 5);
        map.put("key", 3);
        map.put("key", 8);
        assertEquals(8, map.get("key"));
    }

    @Test
    void minMapWithSingleEntry() {
        MergeMap.MinMap<String, Integer> map = new MergeMap.MinMap<>();
        map.put("k", 10);
        assertEquals(10, map.get("k"));
    }

    @Test
    void maxMapWithSingleEntry() {
        MergeMap.MaxMap<String, Integer> map = new MergeMap.MaxMap<>();
        map.put("k", 10);
        assertEquals(10, map.get("k"));
    }

    @Test
    void getMissingKeyReturnsNull() {
        MergeMap.MinMap<String, Integer> map = new MergeMap.MinMap<>();
        assertNull(map.get("missing"));
    }

    @Test
    void containsKeyReturnsTrueForPresentKey() {
        MergeMap.MinMap<String, Integer> map = new MergeMap.MinMap<>();
        map.put("present", 1);
        assertTrue(map.containsKey("present"));
    }

    @Test
    void containsKeyReturnsFalseForAbsentKey() {
        MergeMap.MinMap<String, Integer> map = new MergeMap.MinMap<>();
        assertFalse(map.containsKey("absent"));
    }

    @Test
    void entrySetContainsAllKeys() {
        MergeMap.MaxMap<String, Integer> map = new MergeMap.MaxMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.entrySet().size());
    }

    @Test
    void minMapPreservesExistingMinWhenLargerValueAdded() {
        MergeMap.MinMap<String, Integer> map = new MergeMap.MinMap<>();
        map.put("x", 2);
        map.put("x", 10);
        assertEquals(2, map.get("x"));
    }

    @Test
    void maxMapPreservesExistingMaxWhenSmallerValueAdded() {
        MergeMap.MaxMap<String, Integer> map = new MergeMap.MaxMap<>();
        map.put("x", 10);
        map.put("x", 2);
        assertEquals(10, map.get("x"));
    }

    @Test
    void multipleKeysAreIndependent() {
        MergeMap.MinMap<String, Integer> map = new MergeMap.MinMap<>();
        map.put("a", 5);
        map.put("b", 3);
        map.put("a", 1);
        map.put("b", 7);
        assertEquals(1, map.get("a"));
        assertEquals(3, map.get("b"));
    }
}
