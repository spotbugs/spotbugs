/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MultiMapTest {

    private MultiMap<String, String> map;

    @BeforeEach
    void setUp() {
        map = new MultiMap<>(ArrayList.class);
    }

    @Test
    void addAndRetrieveSingleValue() {
        map.add("key", "value");
        Collection<String> values = map.get("key");
        assertEquals(1, values.size());
        assertTrue(values.contains("value"));
    }

    @Test
    void addMultipleValuesToSameKey() {
        map.add("key", "val1");
        map.add("key", "val2");
        Collection<String> values = map.get("key");
        assertEquals(2, values.size());
        assertTrue(values.contains("val1"));
        assertTrue(values.contains("val2"));
    }

    @Test
    void getMissingKeyReturnsEmptyCollection() {
        Collection<String> values = map.get("missing");
        assertTrue(values.isEmpty());
    }

    @Test
    void containsKeyReturnsTrueAfterAdd() {
        map.add("key", "value");
        assertTrue(map.containsKey("key"));
    }

    @Test
    void containsKeyReturnsFalseForAbsent() {
        assertFalse(map.containsKey("absent"));
    }

    @Test
    void removeSpecificValue() {
        map.add("key", "val1");
        map.add("key", "val2");
        map.remove("key", "val1");
        Collection<String> values = map.get("key");
        assertEquals(1, values.size());
        assertFalse(values.contains("val1"));
        assertTrue(values.contains("val2"));
    }

    @Test
    void removeLastValueDeletesKey() {
        map.add("key", "only");
        map.remove("key", "only");
        assertFalse(map.containsKey("key"));
        assertTrue(map.get("key").isEmpty());
    }

    @Test
    void removeAllDeletesKey() {
        map.add("key", "val1");
        map.add("key", "val2");
        map.removeAll("key");
        assertFalse(map.containsKey("key"));
    }

    @Test
    void clearRemovesAllKeys() {
        map.add("a", "1");
        map.add("b", "2");
        map.clear();
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsKey("b"));
        assertTrue(map.keySet().isEmpty());
    }

    @Test
    void keySetContainsAllKeys() {
        map.add("x", "1");
        map.add("y", "2");
        assertEquals(2, map.keySet().size());
        assertTrue(map.keySet().contains("x"));
        assertTrue(map.keySet().contains("y"));
    }

    @Test
    void asMapReturnsUnderlyingMap() {
        map.add("k", "v");
        assertTrue(map.asMap().containsKey("k"));
        assertEquals(1, map.asMap().size());
    }
}
