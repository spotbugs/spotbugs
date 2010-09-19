/* ****************************************
 * $Id$
 * SF bug Bug2177967:
 *   Missed open stream warning
 *
 * JVM:  1.5.0_16 (OS X, PPC)
 * FBv:  1.3.7-dev-20081215
 *
 * Test case based on example code from bug report
 *
 * Test class has inner class an return open stream, which is never closed.
 * An open stream warning seems like it would be appropriate in this case,
 * since the test method leaves the returned FileInputStream open when
 * it returns.
 *
 * **************************************** */

package sfBugs;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Bug2177967 {

    Properties properties = new Properties();

    public void init(Class loader, String propertiesFile) throws IOException {

        // Load the properties from the file
        InputStream in = loader.getResourceAsStream(propertiesFile);
        if (in == null) {
            throw new RuntimeException("Cound not locate " + propertiesFile);
        }
        properties.load(in);

    }

    public void init(String propertiesFile) throws IOException {

        // Load the properties from the file
        InputStream in = new FileInputStream(propertiesFile);
        if (in == null) {
            throw new RuntimeException("Cound not locate " + propertiesFile);
        }
        properties.load(in);

    }
}
