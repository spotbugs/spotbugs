package ghIssues;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/4020">#4020</a>.
 */
public class Issue4020 {

    @NoWarning("OBL_UNSATISFIED_OBLIGATION")
    public void writeDirectNullCheck() throws IOException {
        FileOutputStream out = new FileOutputStream("demo.txt");
        try {
            out.write(1);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    @NoWarning("OBL_UNSATISFIED_OBLIGATION")
    public void writeObjectsNonNull() throws IOException {
        FileOutputStream out = new FileOutputStream("demo.txt");
        try {
            out.write(1);
        } finally {
            if (Objects.nonNull(out)) {
                out.close();
            }
        }
    }

    @ExpectWarning("OBL_UNSATISFIED_OBLIGATION")
    public void writeLeaked() throws IOException {
        FileOutputStream out = new FileOutputStream("demo.txt");
        out.write(1);
    }
}
