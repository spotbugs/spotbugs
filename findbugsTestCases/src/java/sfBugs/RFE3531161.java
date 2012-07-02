package sfBugs;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class RFE3531161 {

    @ExpectWarning("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    public void doSomething() {
        if (neverReturnsNull() == null) {
            System.out.println(" can never happen ");
        } else {
            System.out.println("  will always invoke code here ");
        }
    }

    private List<Object> neverReturnsNull() {
        return new ArrayList<Object>();
    }

}
