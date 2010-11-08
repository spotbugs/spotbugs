package sfBugs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3102322 {

    @NoWarning("NP_CLOSING_NULL")
    public static void foo() {
        java.io.InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(new File("foo")));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
