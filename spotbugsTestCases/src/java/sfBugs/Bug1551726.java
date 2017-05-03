package sfBugs;

import junit.framework.TestCase;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1551726 extends TestCase {
    /** Test will fail. */
    public void testFalsePositive() {
        final Integer x = null;
        assertNotNull(x); // throws AssertionFailedError if condition is not
                          // met.
        assertTrue(x.intValue() > 0);
    }

    int f() {
        return 42;
    }

    @NoWarning("NP")
    public void testFalsePositive2() {
        int i = f();
        Integer x = null;
        if (i > 0)
            x = i;
        assertNotNull(x); // throws AssertionFailedError if condition is not
                          // met.
        assertTrue(x.intValue() > 0);
    }

}
