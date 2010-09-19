package sfBugs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.logging.StreamHandler;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3063206 extends StreamHandler {
    @NoWarning("OBL_UNSATISFIED_OBLIGATION")
    public void warnme() throws FileNotFoundException {
        setOutputStream(new FileOutputStream(new File("a", "b"), true));
    }
}
