package bugIdeas;

import java.util.HashSet;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Ideas_2008_10_14 {

    @DesireNoWarning("RV_ABSOLUTE_VALUE_OF_HASHCODE")
    static int falsePositive(Object key) {
        int rawHash = key.hashCode();
        return rawHash == Integer.MIN_VALUE ? 0 : Math.abs(rawHash);
    }

    String foo;

    Ideas_2008_10_14(String f00) {
        this.foo = foo;
    }

    String getFoo() {
        return foo;
    }

    Set<?> set = new HashSet();

    boolean falsePositive(Set<?> s) {
        return set.contains(s);
    }

    static class A {
    }

    Set<A> setA = new HashSet<A>();

    @ExpectWarning("SA")
    boolean contained(A a) {
        return setA.contains(setA);
    }

}
