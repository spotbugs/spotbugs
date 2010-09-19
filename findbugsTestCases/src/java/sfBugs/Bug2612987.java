/* ****************************************
 * $Id$
 * SF bug 2612987:
 *   Type qualifier false positive with JSR 305 @Nonnegative annotation
 *
 * JVM:  1.6.0 (OS X, x86)
 * FBv:  1.3.8-dev-20090217
 *
 * Test case based on example code from bug report
 * **************************************** */

package sfBugs;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnegative;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug2612987 {
    @Nonnegative
    private int nonNegativeValue = 1;

    @Nonnegative
    public int get() {
        return nonNegativeValue;
    }

    /* ********************
     * Behavior at filing: TQ warning thrown for explicitly checked (and
     * annotated) parameter ********************
     */
    @DesireNoWarning("TQ")
    public void set(@CheckForSigned int possibleNegativeValue) {
        if (possibleNegativeValue >= 0)
            nonNegativeValue = possibleNegativeValue;
    }
}
