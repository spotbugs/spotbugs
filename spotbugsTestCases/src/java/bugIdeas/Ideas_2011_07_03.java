package bugIdeas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

import annotations.DetectorUnderTest;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.detect.FindUnrelatedTypesInGenericContainer;

@DetectorUnderTest(FindUnrelatedTypesInGenericContainer.class)
public class Ideas_2011_07_03 {

    @NoWarning("EC_UNRELATED_TYPES")
    public boolean test(HashSet<Integer> s1, TreeSet<Integer> s2) {
        return s1.equals(s2);
    }
    @ExpectWarning("EC_UNRELATED_TYPES")
    public boolean test(HashSet<Integer> s1, ArrayList<Integer> s2) {
        return s1.equals(s2);
    }

    @ExpectWarning("EC_UNRELATED_TYPES")
    public boolean test(HashSet<Integer> s1, HashSet<String> s2) {
        return s1.equals(s2);
    }
    @ExpectWarning("GC_UNRELATED_TYPES")
    public boolean test2(HashSet<Integer> s1, HashSet<String> s2) {
        return s1.removeAll(s2);
    }
}
