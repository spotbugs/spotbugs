package ghIssues.issue2547;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Issue2547 {
    // compliant
    public void throwingExCtor() throws MyEx {
        throw new MyEx("");
    }

    // non-compliant
    public void notThrowingExCtor() {
        new MyEx("");
    }

    // compliant
    public void createAndThrowExCtor() throws MyEx {
        MyEx e = new MyEx("");
        throw e;
    }

    // non-compliant
    public void notThrowingExCtorCaller() {
        MyEx.somethingWentWrong("");
    }

    // compliant
    public void createAndThrowExCtorCaller() throws MyEx {
        MyEx e = MyEx.somethingWentWrong("");
        throw e;
    }

    // non-compliant
    public void notThrowingExCtorCallerOutside() {
        ExceptionFactory.createMyException(5);
    }

    // compliant
    public void createAndThrowExCtorCallerOutside() throws MyEx {
        MyEx e = ExceptionFactory.createMyException(5);
        throw e;
    }

    // compliant
    public void createAndThrowExCtorCallerOutside23(File f) throws IOException {
        FileWriter fw = null;
        try {
            fw = new FileWriter(f);
            fw.write("string");
        } catch (IOException e) {
            IOException ioe = new IOException("IOException caught");
            ioe.initCause(e);
            throw ioe;
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    System.err.println("Exception caught, no error");
                }
            }
        }
    }
}
