package sfBugs;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import annotations.DetectorUnderTest;
import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.detect.FindUnrelatedTypesInGenericContainer;

@DetectorUnderTest(FindUnrelatedTypesInGenericContainer.class)
public class Bug3470297 {

    @NoWarning("GC_UNRELATED_TYPES")
    public <A, B> void foo(A a, Foo<A, B> foo) {

        Set<B> empty = Collections.emptySet();
        foo.remove(a, empty); // This generated a false positive because it
                              // thinks that the second parameter should be of
                              // type B, not Set<B>
    }

    static class Foo<X, Y> extends Bar<X, Set<Y>> {

    }

    static class Bar<T, S> extends ConcurrentHashMap<T, S> {
        @Override
        public boolean remove(Object key, Object value) {
            return false;
        }
    }
}
