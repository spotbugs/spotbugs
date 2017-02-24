package sfBugs;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1650397 {

    @NoWarning("BC")
    public static <T> HashSet<T> toHashSet(final Iterator<T> it) {
        return addAll(it, new HashSet<T>());
    }

    @NoWarning("BC")
    public static <T, C extends Collection<? super T>> C addAll(final Iterator<T> it, final C collection) {
        if (it != null) {
            while (it.hasNext()) {
                collection.add(it.next());
            }
        }
        return collection;
    }

}
