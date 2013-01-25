package sfBugs;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3600307 {
    public static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ioex) {
                // Oh well, we tried at least!
            }
        }
    }

    @NoWarning("OBL")
    public static void test(String someFile) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(someFile);
            doSomethingWith(in);
        } catch (IOException ie) {
            log(ie);
        } finally {
            closeQuietly(in);
        }
    }

    private static void log(IOException ie) {


    }

    private static void doSomethingWith(FileInputStream in) throws IOException {


    }

}
