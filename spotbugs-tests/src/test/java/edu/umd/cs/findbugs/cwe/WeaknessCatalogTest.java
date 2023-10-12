package edu.umd.cs.findbugs.cwe;

import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WeaknessCatalogTest {
    @Test
    void testGetInstanceConstruction() {
        assertNotNull(WeaknessCatalog.getInstance());
    }

    @Test
    void testGetInstanceCalledTwiceSameInstance() {
        WeaknessCatalog instance1 = WeaknessCatalog.getInstance();
        WeaknessCatalog instance2 = WeaknessCatalog.getInstance();

        assertTrue(instance1 == instance2);
    }

    @Test
    void testGetWeaknessByCweIdOrNullExistingWeakness() {
        int cweid = 78;
        String name = "Improper Neutralization of Special Elements used in an OS Command ('OS Command Injection')";
        String description = "The product constructs all or part of an OS command using externally-influenced input from an upstream component, but";

        WeaknessCatalog weaknessCatalog = WeaknessCatalog.getInstance();
        Weakness weakness = weaknessCatalog.getWeaknessByCweIdOrNull(cweid);

        assertEquals(cweid, weakness.getCweId());
        assertEquals(name, weakness.getName());
        assertThat(weakness.getDescription(), startsWith(description));
        assertEquals(WeaknessSeverity.HIGH, weakness.getSeverity());
    }

    @Test
    void testGetWeaknessByCweIdOrNullNonExistingWeakness() {
        int cweid = Integer.MAX_VALUE;

        WeaknessCatalog weaknessCatalog = WeaknessCatalog.getInstance();
        Weakness weakness = weaknessCatalog.getWeaknessByCweIdOrNull(cweid);

        assertNull(weakness);
    }

    @Test
    void testGetWeaknessByCweIdOrNullNonInvalidCweId() {
        int cweid = Integer.MIN_VALUE;

        WeaknessCatalog weaknessCatalog = WeaknessCatalog.getInstance();
        Weakness weakness = weaknessCatalog.getWeaknessByCweIdOrNull(cweid);

        assertNull(weakness);
    }

    @Test
    void testGetWeaknessByCweIdOrNullCweIdIsZero() {
        int cweid = 0;

        WeaknessCatalog weaknessCatalog = WeaknessCatalog.getInstance();
        Weakness weakness = weaknessCatalog.getWeaknessByCweIdOrNull(cweid);

        assertNull(weakness);
    }
}
