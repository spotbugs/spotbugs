package sfBugs;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug2644506 {

    @DesireNoWarning("NP_NULL_ON_SOME_PATH")
    static boolean same(String a, String b) {
        if (a == null ^ b == null)
            return false;
        if (a == null && b == null)
            return true;
        return a.equals(b);
    }
}
