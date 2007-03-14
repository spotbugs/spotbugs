package sfBugs;

import edu.umd.cs.findbugs.annotations.NonNull;

public class Bug1678228 {
    @NonNull String str;
    int val;

    public Bug1678228(String str) {
        if (str == null) {
            val = 0;
        } else {
            val = Integer.parseInt(str);
        }
        this.str = str;
    }
}
