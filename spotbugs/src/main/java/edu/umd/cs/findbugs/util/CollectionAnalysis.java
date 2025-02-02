/*
 * SpotBugs - Find bugs in Java programs
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.util;

import java.util.Set;

import edu.umd.cs.findbugs.ba.ClassMember;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * Utility class for analyzing collections.
 */
public final class CollectionAnalysis {

    /**
     * Private constructor to prevent instantiation, because it is a utility class.
     */
    private CollectionAnalysis() {
    }

    /**
     * Check if a class member is a synchronized collection.
     *
     * @param classMember the class member
     * @return {@code true} if the class member is a synchronized collection, {@code false} otherwise
     */
    public static boolean isSynchronizedCollection(ClassMember classMember) {
        return isSynchronizedCollection(classMember.getClassName(), classMember.getName());
    }

    /**
     * Checks if a method is a synchronized collection creating one.
     *
     * @param className name of the class containing the method
     * @param methodName the name of the method
     * @return {@code true} if it's a synchronized collection creating method, {@code false} otherwise
     */
    public static boolean isSynchronizedCollection(@DottedClassName String className, String methodName) {
        final Set<String> interestingCollectionMethodNames = Set.of(
                "synchronizedCollection", "synchronizedSet", "synchronizedSortedSet",
                "synchronizedNavigableSet", "synchronizedList", "synchronizedMap",
                "synchronizedSortedMap", "synchronizedNavigableMap");
        return "java.util.Collections".equals(className) && interestingCollectionMethodNames.contains(methodName);
    }
}
