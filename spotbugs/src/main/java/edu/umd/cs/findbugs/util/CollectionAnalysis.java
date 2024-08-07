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
