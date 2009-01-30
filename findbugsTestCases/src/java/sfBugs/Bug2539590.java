/* ****************************************
 * $Id$
 * SF bug 2539590:
 *   SF_SWITCH_FALLTHROUGH reports incorrect warning message
 *   when thrown due to missing default case
 *
 * JVM:  1.6.0 (OS X, x86)
 * FBv:  1.3.8-dev-20090128
 *
 * Test case based on example code from bug report
 * **************************************** */

package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug2539590 {

    /* ****************************************
     * Behavior at filing:
     *   warning message thrown for fallthrough in switch statement
     *   does not mention missing default case
     *
     * warning thrown => 
     *   M D SF_SWITCH_FALLTHROUGH SF: Switch statement found in \
     *   sfBugs.Bug2539590.fallthroughMethod(int) where one case falls  \
     *   through to the next case  At Bug2539590.java:[lines 33-35]
     * **************************************** */
    @ExpectWarning("SF")
    public static void fallthroughMethod(int which) {
        switch(which) {
        case 0: doSomething(); break;
        }
    }

    /* ****************************************
     * Behavior at filing:
     *   correct, no warning thrown
     * **************************************** */
    @NoWarning("SF")
    public static void noFallthroughMethod(int which) {
        switch(which) {
        case 0: doSomething(); break;
        default: break;
        }
    }

    public static void doSomething() {
        System.out.println("Hello world!");
        return;
    }
}
