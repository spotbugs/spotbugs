package bugIdeas;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Ideas_2011_06_16 {

    static int firstByte(File f) throws IOException {
        FileInputStream fs;

        try {
            fs = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            return -1;
        }
        try {
            return fs.read();
        } catch (IOException e) {
            return -2;
        } finally {
            fs.close();
        }

    }

}
