package sfBugs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class Bug2843453 {
    /**
     * An extending class.
     * 
     * Note: the issue does not occur when using {@link FileOutputStream}
     * directly.
     */
    static class ExtendFileOutputStream extends FileOutputStream {
        ExtendFileOutputStream(File base) throws FileNotFoundException {
            super(base, true);
        }
    }

    void exposeIssue() throws Exception {
        File file = new File("someFile.txt");
        FileOutputStream fos = new ExtendFileOutputStream(file);
        fos.close();
    }
}
