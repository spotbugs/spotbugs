package sfBugsNew;

import java.io.File;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1319 {
    @ExpectWarning("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public void win32PathD() {
        new File("d:\\test.txt");
    }

    @NoWarning("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public void strangePath() {
        new File("_:/test.txt");
    }

    @ExpectWarning("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public void win32PathDNoSlash() {
        new File("d:test.txt");
    }

    @ExpectWarning("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public void win32PathUNC() {
        new File("\\\\server\\share\\fileName.txt");
    }
}
