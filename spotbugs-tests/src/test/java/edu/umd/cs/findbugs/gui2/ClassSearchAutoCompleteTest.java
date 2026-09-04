/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006-2026 University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class ClassSearchAutoCompleteTest {

    @Test
    void testFindCurrentTokenRangeSingleToken() {
        String text = "MyClass";
        assertArrayEquals(new int[] { 0, 7 }, ClassSearchAutoComplete.findCurrentTokenRange(text, 0));
        assertArrayEquals(new int[] { 0, 7 }, ClassSearchAutoComplete.findCurrentTokenRange(text, 3));
        assertArrayEquals(new int[] { 0, 7 }, ClassSearchAutoComplete.findCurrentTokenRange(text, 7));
    }

    @Test
    void testFindCurrentTokenRangeMultipleTokensWithSeparators() {
        String text = "com.foo.First, Second Third:Fourth";

        // Caret in "com.foo.First" (positions 0-13)
        assertArrayEquals(new int[] { 0, 13 }, ClassSearchAutoComplete.findCurrentTokenRange(text, 5));

        // Caret in "Second" (positions 15-21)
        assertArrayEquals(new int[] { 15, 21 }, ClassSearchAutoComplete.findCurrentTokenRange(text, 17));

        // Caret in "Third" (positions 22-27)
        assertArrayEquals(new int[] { 22, 27 }, ClassSearchAutoComplete.findCurrentTokenRange(text, 24));

        // Caret in "Fourth" (positions 28-34)
        assertArrayEquals(new int[] { 28, 34 }, ClassSearchAutoComplete.findCurrentTokenRange(text, 30));
    }

    @Test
    void testFindCurrentTokenRangeEmptyOrEdgeCases() {
        assertArrayEquals(new int[] { 0, 0 }, ClassSearchAutoComplete.findCurrentTokenRange("", 0));
        assertArrayEquals(new int[] { 0, 0 }, ClassSearchAutoComplete.findCurrentTokenRange(null, 0));
        assertArrayEquals(new int[] { 0, 3 }, ClassSearchAutoComplete.findCurrentTokenRange("Foo", 10));
    }

    @Test
    void testReplaceTokenAtRange() {
        String text = "com.foo.First, Sec, Third";
        // replace "Sec" (index 15 to 18) with "SecondClass"
        String replaced = ClassSearchAutoComplete.replaceTokenAtRange(text, 15, 18, "SecondClass");
        assertEquals("com.foo.First, SecondClass, Third", replaced);
    }

    @Test
    void testComputeSuggestionsPrefixAndCaseInsensitive() {
        List<String> candidates = Arrays.asList("String", "StringBuilder", "StringBuffer", "Object", "System");

        List<String> suggestions = ClassSearchAutoComplete.computeSuggestions("str", candidates, 10);
        assertEquals(3, suggestions.size());
        assertTrue(suggestions.contains("String"));
        assertTrue(suggestions.contains("StringBuffer"));
        assertTrue(suggestions.contains("StringBuilder"));
    }

    @Test
    void testComputeSuggestionsSubstringMatch() {
        List<String> candidates = Arrays.asList("StringBuilder", "StringBuffer", "DomBuilderFactory", "Object");

        List<String> suggestions = ClassSearchAutoComplete.computeSuggestions("Builder", candidates, 10);
        assertTrue(suggestions.contains("StringBuilder"));
        assertTrue(suggestions.contains("DomBuilderFactory"));
        assertFalse(suggestions.contains("Object"));
    }

    @Test
    void testComputeSuggestionsFuzzyMatch() {
        List<String> candidates = Arrays.asList("ClassContext", "AnalysisContext", "Project");

        // Typo: "ClassContxt" (missing 'e')
        List<String> suggestions = ClassSearchAutoComplete.computeSuggestions("ClassContxt", candidates, 10);
        assertTrue(suggestions.contains("ClassContext"));
    }

    @Test
    void testComputeSuggestionsEmptyOrNull() {
        List<String> candidates = Arrays.asList("String", "Object");
        assertTrue(ClassSearchAutoComplete.computeSuggestions("", candidates, 10).isEmpty());
        assertTrue(ClassSearchAutoComplete.computeSuggestions("   ", candidates, 10).isEmpty());
        assertTrue(ClassSearchAutoComplete.computeSuggestions("str", null, 10).isEmpty());
        assertTrue(ClassSearchAutoComplete.computeSuggestions("str", Collections.emptyList(), 10).isEmpty());
    }

    @Test
    void testComputeSuggestionsLimitsMaxResults() {
        List<String> candidates = Arrays.asList(
                "TestClass1", "TestClass2", "TestClass3", "TestClass4", "TestClass5");

        List<String> suggestions = ClassSearchAutoComplete.computeSuggestions("Test", candidates, 3);
        assertEquals(3, suggestions.size());
    }
}
