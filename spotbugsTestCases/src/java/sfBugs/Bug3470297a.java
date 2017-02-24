package sfBugs;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import annotations.DetectorUnderTest;
import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.detect.FindUnrelatedTypesInGenericContainer;

@DetectorUnderTest(FindUnrelatedTypesInGenericContainer.class)
public class Bug3470297a {
    @NoWarning("GC_UNRELATED_TYPES")
    public static void main(String[] args) {
        Foo<String, Long> foo = new Foo<String, Long>();
        foo.remove("", Collections.emptySet());
    }

    static class Foo<A, B> extends Bar<A, Set<B>> {
    }

    static class Bar<X, Y> extends ConcurrentHashMap<X, Y> {
        @Override
        public boolean remove(final Object key, final Object value) {
            // ...
            return super.remove(key, value);
        }
    }

}
