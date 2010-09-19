/* ****************************************
 * $Id$
 * SF bug 2515908:
 *   IM_BAD_CHECK_FOR_ODD does not take value sign checks into account
 *
 * JVM:  1.6.0 (OS X, x86)
 * FBv:  1.3.8-dev-20090118
 *
 * Test case based on example code from bug report
 * **************************************** */

package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug2515908 {
    /* ********************
     * Behavior at filing: IM false positive caused by int%2 test on necessarily
     * positive number
     * 
     * warning thrown => M D IM_BAD_CHECK_FOR_ODD IM: Check for oddness that
     * won't work for \ negative numbers in
     * sfBugs.Bug2515908.testPositiveOdd(int) \ At Bug2515908.java:[line 29]
     * *******************
     */
    @NoWarning("IM")
    public static boolean testPositiveOdd(int testInt) {
        if (testInt > 0) {
            if ((testInt % 2) == 1) {
                return true;
            }
            return false;
        }
        return false;
    }
}
