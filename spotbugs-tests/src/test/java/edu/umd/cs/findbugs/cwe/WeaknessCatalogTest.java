package edu.umd.cs.findbugs.cwe;

import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WeaknessCatalogTest {
    @Test
    public void testGetInstanceConstruction() {
        assertNotNull(WeaknessCatalog.getInstance());
    }

    @Test
    public void testGetInstanceCalledTwiceSameInstance() {
        WeaknessCatalog instance1 = WeaknessCatalog.getInstance();
        WeaknessCatalog instance2 = WeaknessCatalog.getInstance();

        assertTrue(instance1 == instance2);
    }

    @Test
    public void testGetWeaknessByCweIdOrNullExistingWeakness() {
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
    public void testGetWeaknessByCweIdOrNullNonExistingWeakness() {
        int cweid = Integer.MAX_VALUE;

        WeaknessCatalog weaknessCatalog = WeaknessCatalog.getInstance();
        Weakness weakness = weaknessCatalog.getWeaknessByCweIdOrNull(cweid);

        assertNull(weakness);
    }

    @Test
    public void testGetWeaknessByCweIdOrNullNonInvalidCweId() {
        int cweid = Integer.MIN_VALUE;

        WeaknessCatalog weaknessCatalog = WeaknessCatalog.getInstance();
        Weakness weakness = weaknessCatalog.getWeaknessByCweIdOrNull(cweid);

        assertNull(weakness);
    }

    @Test
    public void testGetWeaknessByCweIdOrNullCweIdIsZero() {
        int cweid = 0;

        WeaknessCatalog weaknessCatalog = WeaknessCatalog.getInstance();
        Weakness weakness = weaknessCatalog.getWeaknessByCweIdOrNull(cweid);

        assertNull(weakness);
    }
}
