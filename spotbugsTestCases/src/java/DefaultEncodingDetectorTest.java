import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Scanner;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

/**
 * Used by <code>DefaultEncodingDetectorTest</code> as sample code to test
 * <code>DefaultEncodingDetector</code>.
 * <p>
 * Methods containing deliberate encoding bugs are marked with
 * <code>//@ExpectBug</code>.
 * <p>
 * If unexpected bugs are detected, or expected bugs are missing,
 * <code>DefaultEncodingDetectorTest</code> fails.
 */
public class DefaultEncodingDetectorTest {

    /**
     * Does not override the parent class's problematic method. Invocations of
     * that method on instances of this class should be flagged.
     */
    public class MyBAOS extends ByteArrayOutputStream {
        @DesireWarning("DM_DEFAULT_ENCODING")
// XXX bug  @ExpectWarning("DM_DEFAULT_ENCODING")
        public void bar() {
            // Problem - should be flagged
            this.toString();
        }
    }

    /**
     * Overrides the parent class's problematic method. Invocations of the
     * overriding method should NOT be flagged. However, direct calls to the
     * superclass's problematic method via super should be flagged.
     */
    public class MyOtherBAOS extends ByteArrayOutputStream {

        @NoWarning("DM_DEFAULT_ENCODING")
        @Override
        public String toString() {
            try {
                // not a problem
                return super.toString("UTF-8");
            } catch (UnsupportedEncodingException e) {
            }
            return "";
        }

        @NoWarning("DM_DEFAULT_ENCODING")
        public void bar() {
            // not a problem
            this.toString();
        }

        @DesireWarning("DM_DEFAULT_ENCODING")
// XXX bug    @ExpectWarning("DM_DEFAULT_ENCODING")
        public void foo() {
            // Problem - should be flagged
            super.toString();
        }

    }

    @ExpectWarning(value="DM_DEFAULT_ENCODING", num=3)
    public void string() {
        new String(new byte[] {});
        new String(new byte[] {}, 0, 0);
        "".getBytes();
    }

    @ExpectWarning(value="DM_DEFAULT_ENCODING", num=6)
    public void fileReaderWriter() throws IOException {
        new FileReader("");
        new FileReader(new File(""));
        new FileReader(new FileDescriptor());
        new FileWriter("");
        new FileWriter(new File(""));
        new FileWriter(new FileDescriptor());
    }

    @ExpectWarning(value="DM_DEFAULT_ENCODING", num=8)
    public void printStreamWriter() throws IOException {
        new PrintStream(new File(""));
        new PrintStream(new FileOutputStream(""));
        new PrintStream(new FileOutputStream(""), true);
        new PrintStream("");
        new PrintWriter(new File(""));
        new PrintWriter(new FileOutputStream(""));
        new PrintWriter(new FileOutputStream(""), true);
        new PrintWriter("");
    }

    @ExpectWarning(value="DM_DEFAULT_ENCODING", num=7)
    public void misc() throws IOException {
        new ByteArrayOutputStream().toString();
        new InputStreamReader(new FileInputStream(""));
        new OutputStreamWriter(new FileOutputStream(""));
        new Scanner(new FileInputStream(""));
        new Formatter("");
        new Formatter(new File(""));
        new Formatter(new FileOutputStream(""));
    }

    /**
     * These are all fine and should not be flagged.
     */
    @NoWarning(value="DM_DEFAULT_ENCODING")
    public void notBugs() throws IOException {
        String a = "foobar";
        a.getBytes(Charset.forName("UTF-8"));
        a.getBytes("UTF-8");
        new String(new byte[] {}, "UTF-8");
        new String(new byte[] {}, 0, 0, "UTF-8");
        (new ByteArrayOutputStream()).toString("UTF-8");
        new InputStreamReader(new FileInputStream(""), "UTF-8");
        new OutputStreamWriter(new FileOutputStream(""), "UTF-8");
        new PrintStream(new File(""), "UTF-8");
        new PrintStream(new FileOutputStream(""), true, "UTF-8");
        new PrintStream("", "UTF-8");
        new PrintWriter(new File(""), "UTF-8");
        new PrintWriter("", "UTF-8");
        new Scanner(new FileInputStream(""), "UTF-8");
        new Formatter("", "UTF-8");
        new Formatter(new File(""), "UTF-8");
        new Formatter(new FileOutputStream(""), "UTF-8");
        new StringBuilder().toString();
        new ArrayList<Object>().toString();
        List<String> failures = new ArrayList<String>();
        failures.toString();
    }

}
