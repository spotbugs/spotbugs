/* ****************************************
 * $Id$
 * SF bug 2487936:
 *   GC false positive for Map<String, String> get() call using
 *   Map.Entry object from getKey() call as parameter
 *
 * JVM:  1.6.0_07 (OS X, x86)
 * FBv:  1.3.8-dev-20090108
 *
 * Test code adapted directly from 'FalsePositiveGenericCastViaMapEntry.java',
 * contributed to SF bug by user chrisdolan, redistributed with
 * permission (see bug report).
 *
 * Notes:  GC warning is only reported if '-low' flag is used,
 *         class must be compiled with debugging info;
 *         false positive first appears in version 1.3.7
 * **************************************** */

package sfBugs;

import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NoWarning;

/* ********************
 * Behavior at filing:
 *   GC false positive caused by entry.getKey() call in test1();
 *   no warning thrown for test2().
 *
 * warning thrown =>
 *   L B GC_UNCHECKED_TYPE_IN_GENERIC_CALL GC: Unchecked argument of type \
 *   Object provided where type String is expected in \
 *   sfBugs.Bug2487936.test1()  At Bug2487936.java:[line 52]
 *
 * The following code was contributed by SF user Chris Dolan (chrisdolan)
 * (class name changed and annotations added for FB test case)
 ******************** */

/**
 * Demonstration of false positive "L B GC"
 */
public class Bug2487936 {
    /**
     * Exhibits the false positive under FindBugs 1.3.7
     */
    @NoWarning("GC")
    public void test1() {
        Map<String, String> map1 = new HashMap<String, String>();
        Map<String, String> map2 = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : map1.entrySet()) {
            String s2 = map2.get(entry.getKey()); // This line causes "L B GC"
            System.out.println(s2);
        }
    }

    /**
     * Does not exhibit the false positive.
     */
    @NoWarning("GC")
    public void test2() {
        Map<String, String> map1 = new HashMap<String, String>();
        Map<String, String> map2 = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : map1.entrySet()) {
            String s1 = entry.getKey(); // This local var is the only difference
                                        // from the other method
            String s2 = map2.get(s1);
            System.out.println(s2);
        }
    }
}
