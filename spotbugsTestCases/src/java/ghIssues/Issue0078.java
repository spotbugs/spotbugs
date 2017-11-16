package ghIssues;

import java.io.File;
import java.io.PrintWriter;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/78">GitHub issue</a>
 */
public class Issue0078 {
    @SuppressWarnings("unused")
    public void test() throws Exception {
        {
            String a;
            String b;
            String c = "";
        }
        {
            String d;
            for (int i = 0; i < 3; i++) {
            }
            String e;
            for (int i = 0; i < 3; i++) {
                try (PrintWriter writer = new PrintWriter(new File(""), "UTF-8")) {
                }
            }
        }
    }
}
