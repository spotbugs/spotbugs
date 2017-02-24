package sfBugs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class RFE1627291 {
    public void sample() {
        InputStream inputStream = null;
        BufferedInputStream bufferedInputStream = null;

        try {
            inputStream = new FileInputStream(new File("x"));
            bufferedInputStream = new BufferedInputStream(inputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (bufferedInputStream != null) {
                try {
                    bufferedInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
