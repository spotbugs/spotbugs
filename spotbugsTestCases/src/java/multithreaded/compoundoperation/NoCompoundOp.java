package multithreaded.compoundoperation;

/**
* @see <a href="https://github.com/spotbugs/spotbugs/issues/3363">GitHub issue #3363</a>
*/
public class NoCompoundOp {

    private volatile boolean hasCalled = false;

    public void test1() {
        assertEquals(false, hasCalled);
        // do something that changes the flag
        hasCalled = false; // AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE
    }

    public void test2() {
        assertEquals(false, hasCalled);
        // do something that changes the flag
        hasCalled = false; // AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE
    }

    public void snapshot() {
        NoCompoundOp p = new NoCompoundOp();
        p.hasCalled = hasCalled; // AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE
        System.out.println(p);
    }

    public static void assertEquals(Object expected, Object actual) {
    }
}
