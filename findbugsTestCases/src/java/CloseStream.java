import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class CloseStream {
    public static void writeFile(File f, Object o) throws IOException {

        OutputStream out = new FileOutputStream(f);
        int i = o.hashCode();
        out.close();
    }

}
