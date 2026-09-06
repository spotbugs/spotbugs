package edu.umd.cs.findbugs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectPackagePrefixesTest {

    private ProjectPackagePrefixes prefixes;

    @BeforeEach
    void setUp() {
        prefixes = new ProjectPackagePrefixes();
        prefixes.map.clear();
        prefixes.count.clear();
        prefixes.missingProjectCount.clear();
        prefixes.rawPackageCount.clear();
        prefixes.totalCount = 0;
    }

    @Test
    void prefixFilterMatchesConfiguredPrefixes() {
        ProjectPackagePrefixes.PrefixFilter filter = new ProjectPackagePrefixes.PrefixFilter("com.example, org.demo:net.sample");

        assertTrue(filter.matches("com.example.Type"));
        assertTrue(filter.matches("org.demo.sub.Type"));
        assertTrue(filter.matches("net.sample.Other"));
        assertFalse(filter.matches("io.other.Type"));
        assertEquals("com.example, org.demo, net.sample", filter.toString());
    }

    @Test
    void emptyFilterMatchesEverything() {
        ProjectPackagePrefixes.PrefixFilter filter = new ProjectPackagePrefixes.PrefixFilter("   ");

        assertTrue(filter.matches("any.package.Name"));
        assertEquals("", filter.toString());
    }

    @Test
    void getProjectsAndCountersAreUpdated() {
        prefixes.map.put("alpha", new ProjectPackagePrefixes.PrefixFilter("com.alpha"));
        prefixes.map.put("common", new ProjectPackagePrefixes.PrefixFilter("com"));

        TreeSet<String> projects = prefixes.getProjects("com.alpha.MyClass");
        assertEquals(new TreeSet<>(Set.of("alpha", "common")), projects);

        prefixes.countPackageMember("com.alpha");
        prefixes.countPackageMember("com.alpha");
        prefixes.countPackageMember("org.unknown");

        assertEquals(3, prefixes.totalCount);
        assertEquals(2, prefixes.rawPackageCount.get("com.alpha"));
        assertEquals(1, prefixes.rawPackageCount.get("org.unknown"));
        assertEquals(1, prefixes.missingProjectCount.get("org.unknown"));
        assertTrue(prefixes.count.containsKey(new TreeSet<>(Set.of("alpha", "common"))));
        assertTrue(prefixes.count.containsKey(new TreeSet<>()));
    }

    @Test
    void incrementCountAddsAndAccumulates() {
        Map<String, Integer> counter = new HashMap<>();

        ProjectPackagePrefixes.incrementCount(counter, "x");
        ProjectPackagePrefixes.incrementCount(counter, "x", 2);
        ProjectPackagePrefixes.incrementCount(counter, "y", 5);

        assertEquals(3, counter.get("x"));
        assertEquals(5, counter.get("y"));
    }
}
