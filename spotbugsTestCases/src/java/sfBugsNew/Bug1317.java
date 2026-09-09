package sfBugsNew;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug1317 extends FileInputStream {
    private final File tmpFile;

    @DesireNoWarning("OBL_UNSATISFIED_OBLIGATION")
    public Bug1317(File file) throws FileNotFoundException {
        super(file);
        this.tmpFile = file;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            tmpFile.delete();
        }
    }
}
