/* ****************************************
 * $Id$
 * SF bug Bug2177967:
 *   Missed open stream warning
 *
 * JVM:  1.5.0_16 (OS X, PPC)
 * FBv:  1.3.7-dev-20081215
 *
 * Test case based on example code from bug report
 *
 * Test class has inner class an return open stream, which is never closed.
 * An open stream warning seems like it would be appropriate in this case,
 * since the test method leaves the returned FileInputStream open when
 * it returns.
 *
 * **************************************** */

package sfBugs;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class Bug2177967 {

    static class TestReader {
        String fileName;

        public TestReader(String newFileName) {
            fileName = newFileName;
        }

        /* creates and immediately returns open resource */
        public FileInputStream getStream() {
            try {
                return new FileInputStream(fileName);
            } catch (IOException ex) {
                System.out.println("Got IO Execption");
            }
            return null;
        }
    }

    /* ********************
     * Behavior at filing:
     *   no warning thrown
     *
     * Expected:
     *  Open stream error for 'in', which is never closed
     *    (might alternatively/additionally be an OBL warning)
     * ******************** */
    @ExpectWarning("OS")
    public void testMethod(String fileName) {
        TestReader reader = new TestReader(fileName);
        byte[] data = new byte[100];

        // test code adapted from bug report

        // accepts open file stream from inner class
        FileInputStream in = reader.getStream();
        if(in == null) {
            System.out.println("input stream is null!");
        } else {
            try{
                // do something with the stream we just received
                int read_bytes = in.read(data);
                if(read_bytes == -1) {
                    System.out.println("No bytes read");
                }
            } catch (IOException e) {
                System.out.println("Got IO Exception");
            }
        }
        // 'in' is not closed before return
        return;
    }
}
