package sfBugs;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Bug1678225 {
    public void foo(String fileName) {
        InputStream is = null;
        try {
            is = new FileInputStream(fileName);
            useStream(is);
            is.close();
        } catch (IOException ex) {
            try {
                is.close();
            } catch (IOException exx) {
                exx.printStackTrace();
            }
            ex.printStackTrace();
        }
    }

    public void useStream(InputStream is) throws IOException {
    }
}
