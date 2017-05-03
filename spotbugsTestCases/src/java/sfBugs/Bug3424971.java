package sfBugs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3424971 {

    @ExpectWarning("OS_OPEN_STREAM_EXCEPTION_PATH")
    public static void writeInFile(final File file, final byte[] content) throws FileNotFoundException, IOException {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);
            fos.write(content);
        } finally {
            fos.flush();
            fos.close();
        }
    }

    @NoWarning("OS_OPEN_STREAM_EXCEPTION_PATH")
    public static void writeInFile2(final File file, final byte[] content) throws FileNotFoundException, IOException {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);
            fos.write(content);
        } finally {
            fos.close();
        }
    }

}
