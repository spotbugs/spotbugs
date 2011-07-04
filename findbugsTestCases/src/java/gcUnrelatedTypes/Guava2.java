package gcUnrelatedTypes;

import gcUnrelatedTypes.Guava.Pair;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

public class Guava2 {

    
    public static void testIterables(Iterable<String> i, Collection<Integer> c) {
        Iterables.contains(i, 1);
        Iterables.removeAll(i, c);
        Iterables.retainAll(i, c);
        Iterables.elementsEqual(i, c);
        Iterables.frequency(i, 1);
    }

    public static void testIterators(Iterator<String> i, Collection<Integer> c) {
        Iterators.contains(i, 1);
        Iterators.removeAll(i,c);
        Iterators.retainAll(i, c);
        Iterators.elementsEqual(i, c.iterator());
        Iterators.frequency(i, 1);
    }


}
