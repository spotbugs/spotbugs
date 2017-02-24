package sfBugs;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug2990536 {
    @NoWarning("RCN_REDUNDANT_NULLCHECK_OF_NULL")
    public void foo(Bar bar) throws IOException {

        while (true) {
            Closeable c = new StringReader("foo");
            try {
                try {
                    bar.bar(c);
                    c = null;
                } catch (InterruptedException ex) {
                    continue;
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
    }

    interface Bar {
        void bar(Closeable c) throws InterruptedException;
    }

}
