package sfBugsNew;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug1145 {

    // Method called to copy files
    @ExpectWarning("OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE,RV,NP")
    public static void copyDirectory(File sourceLocation, File targetLocation) throws IOException {
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
            }
        } else {
            InputStream in = null;
            OutputStream out = null;
            byte[] buf = new byte[1024];
            int len;
            try {
                in = new FileInputStream(sourceLocation);
                out = new FileOutputStream(targetLocation);
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                in.close();
                out.close();
            }
        }
    }
}
