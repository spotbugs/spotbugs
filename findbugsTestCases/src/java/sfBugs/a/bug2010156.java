/* ****************************************
 * $Id$
 * SF bug 2010156:
 *   Line numbers not reported/highlighted for certain
 *   error types (some of which report errors in textui mode)
 *
 * JVM:  1.5.0_16 (OS X, PPC)
 * FBv:  1.3.7-dev-20081212
 *
 * Certain errors reported by the GUI do not highlight the code
 * with which they are associated, nor are line numbers reported;
 * in certain cases, the line numbers for the same bugs /are/
 * reported when using the text UI.
 *
 * see sfBugs/b/bug2010156.java for more details
 *
 * **************************************** */

package sfBugs.a;

/* this class only exists to induce a warning in another class;
 * see sfBugs/b/bug2010156.java */
public class bug2010156 {
    private int i, j, k;

    public int testMethod1(int a, int b, int c) {
        i = a;
        j = b;
        k = c;
        return i + j + k;
    }

}
