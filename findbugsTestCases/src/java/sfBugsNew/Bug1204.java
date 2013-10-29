package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1204 {

    static volatile Object ob;

    @NoWarning("DC_DOUBLECHECK")
    static Object lm() {
        Object result = ob;
        if (result == null) { // Bug: Possible doublecheck on snippet.Snippet.ob
            // in snippet.Snippet.lm()
            synchronized (Bug1204.class) {
                result = ob;
                if (result == null)
                    result = ob = new Object();
            }
        }
        return result;
    }
    
    static Object ob2;
    @ExpectWarning("DC_DOUBLECHECK")
    static Object lm2() {
        Object result = ob2;
        if (result == null) { // Bug: Possible doublecheck on snippet.Snippet.ob
            // in snippet.Snippet.lm()
            synchronized (Bug1204.class) {
                result = ob2;
                if (result == null)
                    result = ob2 = new Object();
            }
        }
        return result;
    }
}
