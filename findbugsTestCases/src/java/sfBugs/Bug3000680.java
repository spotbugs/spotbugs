package sfBugs;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug3000680 {

    /**
     * Case1: FB doesn't report Infinite Loop in 'private' 'static' methods
     */
    @ExpectWarning("IL")
    private static void case1() {
        case1();
    }

    /**
     * Case1: FB doesn't report Infinite Loop in 'private' 'static' methods
     */
    @ExpectWarning("IL")
    static void case1a() {
        case1a();
    }

    /**
     * Case2: 'if' condition causes FB ignores Infinite loop check
     */
    @ExpectWarning("IL")
    public void case2() {
        // this naive condition causes infinite loop doesn't report
        if (true) {
            System.out.println("Hello world!");
        }

        String text = null;
        if (text != null) {
            case2();
        } else {
            case2();
        }
    }

    @ExpectWarning("IL")
    public void case2a() {

        String text = null;
        if (text != null) {
            case2a();
        } else {
            case2a();
        }
    }

    public void case2b() {
        System.out.println("Hello world!");

        String text = null;
        if (text != null) {
            case2b();
        } else {
            case2b();
        }
    }

    public void case2c(boolean b) {
        System.out.println("Hello world!");

        if (b) {
            case2c(b);
        } else {
            case2c(b);
        }
    }

}
