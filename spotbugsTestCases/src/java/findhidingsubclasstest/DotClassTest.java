package findhidingsubclasstest;

abstract class SuperDotClassTest{
    private static SuperDotClassTest defaultSuperDotClassTest = null;

    public SuperDotClassTest(){
    }

    public static SuperDotClassTest getDefaultSuperDotClassTest() {
        if (defaultSuperDotClassTest == null) {
            defaultSuperDotClassTest = new DotClassTest();
        }
        return defaultSuperDotClassTest;
    }

}
public class DotClassTest extends SuperDotClassTest {
    private static boolean debug = false;

    public DotClassTest() {
        if (debug) {
            System.out.println("MailcapCommandMap: load HOME");
        }
        /*
        synchronized (class$findhidingsubclasstest$DotClassTest == null?class$("findhidingsubclasstest.DotClassTest"):class$findhidingsubclasstest$DotClassTest ) {

        }
        */
    }

    public DotClassTest(String fileName) {
        this();
        if (debug) {
            System.out.println("MailcapCommandMap: load PROG from " + fileName);
        }
    }

    static {
        try {
            debug = Boolean.getBoolean("javax.activation.debug");
        } catch (Throwable var1) {
        }

    }
}
