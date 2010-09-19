package sfBugs;

/* ****************************************
 * $Id$
 * SF bug 1993328:
 *   false positive DM_USELESS_THREAD
 *
 * JVM:  1.5.0_16 (OS X, PPC)
 * FBv:  1.3.7-dev-20081123
 *
 * Test case based on example code from bug report
 *
 * Notes:
 *   repro requires use of '-low' option
 * **************************************** */

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1969563 extends Thread {
    private static long getNextId() {
        return 1L;
    }

    /* ********************
     * Behavior at filing: warning thrown => L M Dm: Method new Bug1969563()
     * creates a thread using the default empty run method At
     * Bug1969563.java:[line 19] ********************
     */
    @NoWarning("Dm")
    public Bug1969563() {
        super("TestThread-" + getNextId());
    }

    /* ********************
     * Behavior at filing: no warning thrown ********************
     */
    public Bug1969563(int id) {
        super("TestThread-" + id);
    }

    @Override
    public void run() {
        System.out.println("Hello World");
    }
}
