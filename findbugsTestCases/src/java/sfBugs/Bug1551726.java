package sfBugs;

import junit.framework.TestCase;

public class Bug1551726 extends TestCase {
	  /** Test will fail. */
    public void falsePositive()
    {
        final Integer x = null;
        assertNotNull(x); // throws AssertionFailedError if condition is not met.
        assertTrue(x.intValue() > 0);
    }

	

}
