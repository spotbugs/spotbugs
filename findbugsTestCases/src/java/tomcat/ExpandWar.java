package tomcat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
//   /home/cvs/jakarta-tomcat-4.0/catalina/src/share/org/apache/catalina/startup/ExpandWar.java,v 1.2 2003/01/14 17:44:47 glenn Exp $

public class ExpandWar {

    /**
     * Expand the specified input stream into the specified directory, creating
     * a file named from the specified relative path.
     *
     * @param input InputStream to be copied
     * @param docBase Document base directory into which we are expanding
     * @param name Relative pathname of the file to be created
     *
     * @exception IOException if an input/output error occurs
     */
    protected static void expand(InputStream input, File docBase, String name)
        throws IOException {

        File file = new File(docBase, name);
        // TODO: generate an OS warning for the output stream
        BufferedOutputStream output =
            new BufferedOutputStream(new FileOutputStream(file));
        byte buffer[] = new byte[2048];
        while (true) {
            int n = input.read(buffer);
            if (n <= 0)
                break;
            output.write(buffer, 0, n);
        }
        output.close();

    }

}
