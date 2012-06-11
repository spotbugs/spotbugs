package sfBugs;

import java.util.Collection;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3531425 {

    public void precondition(String msg, boolean b) {
        if (!b)
            throw new IllegalStateException(msg);
    }

    @ExpectWarning("NP")
    @DesireNoWarning("NP")
    public int test(Collection c) {
        precondition("dataset connection is not null", c != null && !c.isEmpty());
        return c.size();
    }

    @NoWarning("NP")
    public int test2(Collection c) {
        precondition("dataset connection is not null", c != null && !c.isEmpty());
        return 0;
        
    }

}
