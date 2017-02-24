package bugIdeas;

import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2009_03_13 {

    @NoWarning("NP")
    static void bar(Object o) {
        if (o == null)
            throw new NullPointerException();
        return;
    }

    @ExpectWarning("RCN")
    @NoWarning("NP")
    static int foo(Object x) {
        bar(x);
        if (x == null)
            return x.hashCode();
        return 42;
    }

    static int foo2(Object x) {
        if (x == null) {
            bar(x);
            return x.hashCode();
        }
        return 42;
    }

    Map<Integer, Integer> map = new HashMap<Integer, Integer>();

    @ExpectWarning(value="GC_UNRELATED_TYPES", confidence=Confidence.MEDIUM)
    /** This bug would manifest itself as a NPE, so we have some confidence it isn't occurring in practice */
    int noisyBug(String x) {
        return map.get(x);
    }

    @ExpectWarning(value="GC_UNRELATED_TYPES", confidence=Confidence.HIGH)
    /** This bug would manifest itself by indicating that so matching key was found, so this might be occurring 
     * in production without being noticed */
    Integer silentBug(String x) {
        return map.get(x);
    }

}
