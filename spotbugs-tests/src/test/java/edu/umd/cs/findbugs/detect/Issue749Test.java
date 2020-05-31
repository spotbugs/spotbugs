package edu.umd.cs.findbugs.detect.MyTest;
import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.util.ClassName;
import org.junit.Test;

import static org.testng.Assert.*;

/**
 * @author Lin
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/749">GitHub issue</a>
 */
public class Issue749Test extends AbstractIntegrationTest {
    /**
     * After implement the issue, when calling method
     * {@code ClassName.matchedPrefixes(String className)}
     * by a filter containing lowercase letter and a class
     * name containing uppercase letter, it should return true.
     */
    @Test
    public void testCaseInsensitive1() {
        // classes which have bugs
        String testClassName1 = "androidAnnotations.CheckedNullableReturn";
        String testClassName2 = "androidAnnotations.CheckedNullableReturn2";
        // filter
        String[] filters = {"null","?","234"};
        assertTrue(ClassName.matchedPrefixes(filters,testClassName1));
        assertTrue(ClassName.matchedPrefixes(filters,testClassName2));
    }

    /**
     * Before implement the issue, when calling method
     * {@code ClassName.matchedPrefixes(String className)}
     * by a filter containing lowercase letter and a class
     * name containing uppercase letter, it should return false.
     */
    @Test
    public void testCaseInsensitive2() {
        // original test
        // classes which have bugs
        String testClassName = "androidAnnotations.CheckedNullableReturn2";
        // filter
        String[] filters = {"null","78"};
        assertFalse(ClassName.matchedPrefixes(filters,testClassName));
    }
}