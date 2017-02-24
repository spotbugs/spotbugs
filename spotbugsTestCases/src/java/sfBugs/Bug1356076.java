package sfBugs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import edu.umd.cs.findbugs.annotations.NoWarning;

/**
 * Submitted By: Len Trigg Summary:
 * 
 * OS reports failure to close stream when the stream is assigned to a member
 * array.
 * 
 * Test case attached that demonstrates the problem (and shows that the OS is
 * not reported upon assignment to non-array member).
 */
public class Bug1356076 {
    PrintWriter[] mWriters = new PrintWriter[1];

    PrintWriter mWriter = null;

    @NoWarning("OS")
    public void writeString1(final String s) throws IOException {
        if (mWriters[0] == null) {
            final File ff = File.createTempFile("sfq", ".lst");
            ff.deleteOnExit();
            mWriter = new PrintWriter(new BufferedWriter(new FileWriter(ff)));
            // mWriters[0] = new PrintWriter(new BufferedWriter(new
            // FileWriter(ff)));
        }
        mWriter.println(s);
        // mWriters[0].println(s);
    }

    // FindBugs fails to notice that this nWriters escapes too
    // // grep -A 1 OS_OPEN_STREAM | grep Bug1356076
    @NoWarning("OS")
    public void writeString2(final String s) throws IOException {
        if (mWriters[0] == null) {
            final File ff = File.createTempFile("sfq", ".lst");
            ff.deleteOnExit();
            // mWriter = new PrintWriter(new BufferedWriter(new
            // FileWriter(ff)));
            mWriters[0] = new PrintWriter(new BufferedWriter(new FileWriter(ff)));
        }
        // mWriter.println(s);
        mWriters[0].println(s);
    }
}
