package gcUnrelatedTypes;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class EvenUnknownTypesAreNotRecursive {

    /** Really, we want this to be a class that is not available for analysis */

    static class A {

    }

    /**
     * @param args
     */
    @ExpectWarning("DMI,GC")
    public static void main(String args[]) {

        Set<A> sa = new HashSet<A>();
        Set<A> sa2 = new HashSet<A>();

        TreeSet<A> tsa2 = new TreeSet<A>();

        sa.contains(sa);
        sa.contains(sa2);
        sa.contains(tsa2);

        sa.containsAll(sa);
        sa.containsAll(sa2);
        sa.containsAll(tsa2);
    }
}
