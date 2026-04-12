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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MultisetTest {

    private Multiset<String> multiset;

    @BeforeEach
    void setUp() {
        multiset = new Multiset<>();
    }

    @Test
    void emptyMultisetIsEmpty() {
        assertTrue(multiset.isEmpty());
        assertEquals(0, multiset.numKeys());
    }

    @Test
    void addSingleElement() {
        multiset.add("a");
        assertFalse(multiset.isEmpty());
        assertEquals(1, multiset.getCount("a"));
        assertEquals(1, multiset.numKeys());
    }

    @Test
    void addSameElementTwice() {
        multiset.add("a");
        multiset.add("a");
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.numKeys());
    }

    @Test
    void addDifferentElements() {
        multiset.add("a");
        multiset.add("b");
        assertEquals(1, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(2, multiset.numKeys());
    }

    @Test
    void addWithExplicitCount() {
        multiset.add("a", 5);
        assertEquals(5, multiset.getCount("a"));
    }

    @Test
    void addIncrements() {
        multiset.add("a", 3);
        multiset.add("a", 2);
        assertEquals(5, multiset.getCount("a"));
    }

    @Test
    void getCountForAbsentKeyIsZero() {
        assertEquals(0, multiset.getCount("absent"));
    }

    @Test
    void removeDecrements() {
        multiset.add("a", 3);
        assertTrue(multiset.remove("a"));
        assertEquals(2, multiset.getCount("a"));
    }

    @Test
    void removeLastOccurrenceRemovesKey() {
        multiset.add("a");
        assertTrue(multiset.remove("a"));
        assertEquals(0, multiset.getCount("a"));
        assertTrue(multiset.isEmpty());
    }

    @Test
    void removeAbsentKeyReturnsFalse() {
        assertFalse(multiset.remove("absent"));
    }

    @Test
    void addAllFromIterable() {
        multiset.addAll(java.util.Arrays.asList("a", "b", "a", "c"));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
    }

    @Test
    void clearRemovesAllElements() {
        multiset.add("a");
        multiset.add("b");
        multiset.clear();
        assertTrue(multiset.isEmpty());
        assertEquals(0, multiset.getCount("a"));
    }

    @Test
    void uniqueKeysContainsAllKeys() {
        multiset.add("x");
        multiset.add("y");
        multiset.add("x");
        assertTrue(multiset.uniqueKeys().contains("x"));
        assertTrue(multiset.uniqueKeys().contains("y"));
        assertEquals(2, multiset.uniqueKeys().size());
    }

    @Test
    void entrySetReturnsAllEntries() {
        multiset.add("a", 3);
        multiset.add("b", 1);
        int totalCount = 0;
        for (Map.Entry<String, Integer> entry : multiset.entrySet()) {
            totalCount += entry.getValue();
        }
        assertEquals(4, totalCount);
    }

    @Test
    void entriesInDecreasingFrequencyOrderedCorrectly() {
        multiset.add("low", 1);
        multiset.add("high", 5);
        multiset.add("mid", 3);

        Iterator<Map.Entry<String, Integer>> it = multiset.entriesInDecreasingFrequency().iterator();
        Map.Entry<String, Integer> first = it.next();
        Map.Entry<String, Integer> second = it.next();
        Map.Entry<String, Integer> third = it.next();

        assertTrue(first.getValue() >= second.getValue(), "first entry should have highest frequency");
        assertTrue(second.getValue() >= third.getValue(), "second entry should have higher frequency than third");
    }

    @Test
    void copyConstructorCreatesCopy() {
        multiset.add("a", 4);
        multiset.add("b", 2);

        Multiset<String> copy = new Multiset<>(multiset);
        assertEquals(4, copy.getCount("a"));
        assertEquals(2, copy.getCount("b"));

        // Mutating the copy should not affect the original
        copy.add("a", 10);
        assertEquals(4, multiset.getCount("a"));
    }
}
