/**
 * Issues from http://www.cigital.com/securitypack/view/index.html
 * 
 * No comment on whether these issues are worth reporting
 * 
 */
public class CigitalJavaSecurityRulepack {

    public static void emptyTryBlocks() {
        try {
        } catch (Exception e) {
            System.err.println("Caught Exception: " + e.getMessage());
        }
    }

    public static void deprecatedThreadMethods(Thread t) {
        t.suspend();
        t.resume();
        t.stop();
        t.destroy();
        Thread.yield();
    }
}
