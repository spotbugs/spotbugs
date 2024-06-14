package edu.umd.cs.findbugs.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import edu.umd.cs.findbugs.ba.ClassMember;

/**
 * Utility class for analyzing collections.
 */
public final class CollectionAnalysis {

    private CollectionAnalysis() {
    }

    /**
     * Check if a class member is a synchronized collection.
     *
     * @param classMember the class member
     * @return {@code true} if the class member is a synchronized collection, {@code false} otherwise
     */
    public static boolean isSynchronizedCollection(ClassMember classMember) {
        Set<String> interestingCollectionMethodNames = new HashSet<>(Arrays.asList(
                "synchronizedCollection", "synchronizedSet", "synchronizedSortedSet",
                "synchronizedNavigableSet", "synchronizedList", "synchronizedMap",
                "synchronizedSortedMap", "synchronizedNavigableMap"));
        return "java.util.Collections".equals(classMember.getClassName()) && interestingCollectionMethodNames.contains(classMember.getName());
    }
}
