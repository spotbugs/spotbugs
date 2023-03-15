package VulnerableSecurityCheckMethodsTest;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FindVulnerableSecurityCheckMethodsTest {
    Logger logger = Logger.getAnonymousLogger();

    public void badFindVulnerableSecurityCheckMethodsCheck() {
        try {
            SecurityManager sm = System.getSecurityManager();
            //Temporary Variable for the sake of completeness if testing
            /*
            SecurityManager sm2 = System.getSecurityManager();


            int i = 0;
            double f = 1.0;
            int[] ia = {1,2,3};

            i = i+ia.length;
            f = f+1.0;

            if(sm2 != null) {
                sm.checkRead("/temp/tempFile");
                f = (double)i*2;
            }
            */

            if (sm != null) {  // Check for permission to read file
                sm.checkRead("/temp/tempFile");
            }
            // Access the file
        } catch (SecurityException se) {
            logger.log(Level.WARNING, se.toString());
        }
    }

    public final void goodVulnerableSecurityCheckMethodsTestCheck() {
        try {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {  // Check for permission to read file
                sm.checkRead("/temp/tempFile");
            }
            // Access the file
        } catch (SecurityException se) {
            logger.log(Level.WARNING, se.toString());
        }
    }

    private void goodVulnerableSecurityCheckMethodsTestCheck2() {
        try {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {  // Check for permission to read file
                sm.checkRead("/temp/tempFile");
            }
            // Access the file
        } catch (SecurityException se) {
            logger.log(Level.WARNING, se.toString());
        }
    }

    public void goodVulnerableSecurityCheckMethodsTestCheck4() {
        try {
            SecurityManager sm = System.getSecurityManager();
            //We have the SecurityManager object, but we do not perform any security check with it.

            // Access the file
        } catch (SecurityException se) {
            logger.log(Level.WARNING, se.toString());
        }
    }
}