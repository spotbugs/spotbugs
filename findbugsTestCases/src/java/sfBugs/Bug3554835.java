package sfBugs;

import java.math.BigInteger;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug3554835 {
    @DesireNoWarning("NP_NULL_PARAM_DEREF")
    void x() {
        BigInteger i = BigInteger.ZERO;

        if (i == null) {
            i = BigInteger.ZERO;
        } else {
            i = BigInteger.ZERO;
        }

        System.out.println(new BigInteger("1").add(i));
    }
    
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
