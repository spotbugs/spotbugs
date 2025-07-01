package multithreaded.compoundoperation;

/**
* @see <a href="https://github.com/spotbugs/spotbugs/issues/3363">GitHub issue #3363</a>
*/
public class InnerLocalVarFields {
    private volatile int number = 1;

    public int getNumber() {
        return number;
    }

    public void snapshot() {
        InnerLocalVarFields p = new InnerLocalVarFields();
        p.number = number + 1; // AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE
        number = 2;
        System.out.println(p);
    }
}
