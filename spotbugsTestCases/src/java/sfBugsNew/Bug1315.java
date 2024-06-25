package sfBugsNew;

import java.util.List;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug1315 {
    @ExpectWarning("INT_BAD_COMPARISON_WITH_NONNEGATIVE_VALUE")
    public String getLastListElement(List<String> list) {
        int s = list.size();
        if(s >= 0) {
            return list.get(s-1);
        }
        return null;
    }
}
