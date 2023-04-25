package VulnerableSecurityCheckMethodsTest;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class GoodVulnerableSecurityCheckMethodsTest {
    Logger logger = Logger.getAnonymousLogger();

    /***
     * This method is not vulnerable as it is in final class.
     * It is using the Security Manager to perform checkRead Operation.
     * But as it is in a final class so this class cannot be inherited anymore.
     * Moreover, it is final by default.
     */
    public void goodVulnerableSecurityCheckMethodsTestCheck() {
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

    /***
     * This method is not vulnerable as it is in final class.
     * It is using the Security Manager to perform checkRead Operation.
     * But as it is in a final class so this class cannot be inherited anymore.
     * Moreover, it is declared private.
     */
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

    /***
     * This method is not vulnerable as it is in final class.
     * Moreover, it is not using Security Manager to perform any check.
     */
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