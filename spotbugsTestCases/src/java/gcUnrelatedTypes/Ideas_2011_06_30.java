package gcUnrelatedTypes;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.util.Collection;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2011_06_30 {

    @ExpectWarning("DMI_COLLECTIONS_SHOULD_NOT_CONTAIN_THEMSELVES")
    public static void testTP(Collection<Integer> c) {
        assertTrue(c.contains(c));

    }

    @ExpectWarning("DMI_COLLECTIONS_SHOULD_NOT_CONTAIN_THEMSELVES")
    public static boolean testTP2(Collection<Integer> c) {
        return c.contains(c);

    }

    @NoWarning("DMI_COLLECTIONS_SHOULD_NOT_CONTAIN_THEMSELVES")
    public static void testFP(Collection<Integer> c) {
        assertFalse(c.contains(c));
    }

}
