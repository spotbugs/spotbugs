package sfBugs;

import java.math.BigInteger;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3554835 {
    @NoWarning("NP_NULL_PARAM_DEREF")
    @ExpectWarning("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE,DB_DUPLICATE_BRANCHES")
    void x() {
        BigInteger i = BigInteger.ZERO;

        if (i == null) {
            i = BigInteger.ZERO;
        } else {
            i = BigInteger.ZERO;
        }

        System.out.println(new BigInteger("1").add(i));
    }
    
    @ExpectWarning("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    void y() {
        BigInteger i = BigInteger.ZERO;

        if (i == null) {
            i = BigInteger.ONE;
        } else {
            i = BigInteger.TEN;
        }

        System.out.println(new BigInteger("1").add(i));

    }
}
