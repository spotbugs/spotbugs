package edu.umd.cs.findbugs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class FractionalMultisetTest {

    @Test
    void addGetAndClearWork() {
        FractionalMultiset<String> set = new FractionalMultiset<>();

        set.add("a", 1.5);
        set.add("a", 0.5);
        set.add("b", 2.0);

        assertEquals(2, set.numKeys());
        assertEquals(2.0, set.getValue("a"));
        assertEquals(2.0, set.getValue("b"));
        assertEquals(0.0, set.getValue("missing"));

        set.clear();

        assertEquals(0, set.numKeys());
    }

    @Test
    void turnTotalIntoAverageHandlesMissingCounts() {
        FractionalMultiset<String> set = new FractionalMultiset<>();
        Multiset<String> counts = new Multiset<>();

        set.add("a", 6.0);
        set.add("b", 9.0);
        counts.add("a", 3);

        set.turnTotalIntoAverage(counts);

        assertEquals(2.0, set.getValue("a"));
        assertTrue(Double.isNaN(set.getValue("b")));
    }

    @Test
    void sortedEntryViewsContainAllEntries() {
        Map<String, Double> backing = new HashMap<>();
        backing.put("x", 1.0);
        backing.put("y", 3.0);
        backing.put("z", 2.0);
        FractionalMultiset<String> set = new FractionalMultiset<>(backing);

        List<String> decreasing = keys(set.entriesInDecreasingOrder());
        List<String> increasing = keys(set.entriesInIncreasingOrder());

        assertEquals(3, decreasing.size());
        assertEquals(3, increasing.size());
        assertTrue(decreasing.containsAll(backing.keySet()));
        assertTrue(increasing.containsAll(backing.keySet()));
        assertEquals(List.of("y", "z", "x"), decreasing);
        assertEquals(List.of("x", "z", "y"), increasing);
    }

    private static List<String> keys(Iterable<Map.Entry<String, Double>> entries) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Double> entry : entries) {
            result.add(entry.getKey());
        }
        return result;
    }
}
