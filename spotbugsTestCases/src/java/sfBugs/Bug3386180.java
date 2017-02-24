package sfBugs;

import java.util.Comparator;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3386180 {
    @NoWarning("NP")
    public void test(final Comparator<String> comparator) {
        if (comparator.compare(null, null) == 0) {
            System.out.println("OK");
        }
    }

}
