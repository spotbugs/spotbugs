package sfBugs;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Bug2118696 {

    public static void writeToFile(File file, byte[] bytes) throws IOException {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
            fout.write(bytes);
        } finally {
            closeStream(fout);
        }
    }

    public static void closeStream(Closeable stream) throws IOException {
        if (stream != null) {
            stream.close();
        }
    }

}
