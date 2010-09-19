/* ****************************************
 * $Id$
 * SF bug 2353694:
 *   OBL False positive on properly closed input stream
 *
 * JVM:  1.5.0_16 (OS X, PPC)
 * FBv:  1.3.7-dev-20081212
 *
 * Test case based on example code from bug report
 *
 * The original false positive OBL warning on 'in' in fp_warning has been
 * corrected in the FB version above; however, only one OS warning is reported
 * for fp_warning_OS (on in2) despite the second unclosed stream (in3).
 * **************************************** */

package sfBugs;

import java.io.FileInputStream;
import java.io.IOException;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug2353694 {

    /* ********************
     * Behavior at filing: OBL false positive already resolved correct OS
     * warning thrown for in2 warning thrown => M B OS_OPEN_STREAM OS:
     * Bug2353694.fp_warning_OS() may fail to close \ stream At
     * Bug2353694.java:[line 26]
     * 
     * *no* OS warning thrown for in3 -- maybe false negative?
     * ********************
     */
    @ExpectWarning("OS")
    public void fp_warning_OS() throws IOException {
        FileInputStream in2 = new FileInputStream("test");
        in2.read();

        FileInputStream in = new FileInputStream("test");
        try {
            in.read();
        } finally {
            in.close();
        }

        FileInputStream in3 = new FileInputStream("test");
        in3.read();
    }

    /* ********************
     * Behavior at filing: correct OS warning thrown for in3 warning thrown =>
     * oM B OS_OPEN_STREAM OS: Bug2353694.correct_warning_OS() may fail to \
     * close stream At Bug2353694.java:[line 50] ********************
     */
    @ExpectWarning("OS")
    public void correct_warning_OS() throws IOException {
        FileInputStream in = new FileInputStream("test");
        try {
            in.read();
        } finally {
            in.close();
        }

        FileInputStream in3 = new FileInputStream("test");
        in3.read();
    }
}
