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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EditDistanceTest {

    @Test
    void identicalStringsHaveZeroDistance() {
        assertEquals(0, EditDistance.editDistance("hello", "hello"));
    }

    @Test
    void emptyStringToWordCostsInsertions() {
        // Each insertion costs 2
        assertEquals(10, EditDistance.editDistance("", "hello"));
    }

    @Test
    void wordToEmptyStringCostsDeletions() {
        // Each deletion costs 2
        assertEquals(10, EditDistance.editDistance("hello", ""));
    }

    @Test
    void singleCharacterSubstitutionCostsTwoForDifferentCase() {
        // 'a' vs 'b': different characters, cost 2
        assertEquals(2, EditDistance.editDistance("a", "b"));
    }

    @Test
    void caseOnlyDifferenceHasCostOne() {
        // 'a' vs 'A': same letter different case, cost 1
        assertEquals(1, EditDistance.editDistance("a", "A"));
    }

    @Test
    void singleInsertionCostsTwo() {
        // "cat" vs "cats": one insertion, cost 2
        assertEquals(2, EditDistance.editDistance("cat", "cats"));
    }

    @Test
    void singleDeletionCostsTwo() {
        // "cats" vs "cat": one deletion, cost 2
        assertEquals(2, EditDistance.editDistance("cats", "cat"));
    }

    @Test
    void longLengthDifferenceReturnsMaxCost() {
        // If length difference > 6, return 2 * max(len1, len2)
        String short1 = "ab";
        String long1 = "abcdefghijk"; // length 11, diff = 9 > 6
        int expected = 2 * long1.length();
        assertEquals(expected, EditDistance.editDistance(short1, long1));
    }

    @Test
    void editDistance0MatchesEditDistance1() {
        // verify both implementations give the same result
        String[][] pairs = {
            { "kitten", "sitting" },
            { "sunday", "saturday" },
            { "abc", "abc" },
            { "", "test" },
            { "test", "" }
        };
        for (String[] pair : pairs) {
            assertEquals(
                    EditDistance.editDistance0(pair[0], pair[1]),
                    EditDistance.editDistance1(pair[0], pair[1]),
                    "editDistance0 and editDistance1 should agree for: " + pair[0] + " vs " + pair[1]);
        }
    }

    @Test
    void ratioForIdenticalStringsIsZero() {
        assertEquals(0.0, EditDistance.editDistanceRatio("hello", "hello"), 0.001);
    }

    @Test
    void ratioForVeryDifferentStringsIsPositive() {
        double ratio = EditDistance.editDistanceRatio("abc", "xyz");
        assertTrue(ratio > 0);
    }

    @Test
    void ratioIsAtMostDistanceDividedByFour() {
        // editDistanceRatio = min(distance/maxDistance, distance/4)
        String s1 = "a";
        String s2 = "b";
        double distance = EditDistance.editDistance(s1, s2);
        double ratio = EditDistance.editDistanceRatio(s1, s2);
        assertTrue(ratio <= distance / 4.0 + 0.001);
    }
}
