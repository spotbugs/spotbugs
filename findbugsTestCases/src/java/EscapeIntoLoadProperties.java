import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class EscapeIntoLoadProperties {

    @ExpectWarning("OBL_UNSATISFIED_OBLIGATION,OS_OPEN_STREAM")
    static Properties f(File f) throws FileNotFoundException, IOException {
        Properties p = new Properties();
        p.load(new FileInputStream(f));
        return p;
    }
}
