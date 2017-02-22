/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.SystemProperties;

/**
 * Build a map of added class names to removed class names. Serves as a
 * ClassNameRewriter that can match up renamed classes in two BugCollections.
 *
 * @author David Hovemeyer
 */
public class MovedClassMap implements ClassNameRewriter {

    private static final boolean DEBUG = SystemProperties.getBoolean("movedClasses.debug");

    private final BugCollection before;

    private final BugCollection after;

    private final Map<String, String> rewriteMap;

    public MovedClassMap(BugCollection before, BugCollection after) {
        this.before = before;
        this.after = after;
        this.rewriteMap = new HashMap<String, String>();
    }

    public MovedClassMap execute() {
        Set<String> beforeClasses = buildClassSet(before);
        Set<String> afterClasses = buildClassSet(after);

        Set<String> removedClasses = new HashSet<String>(beforeClasses);
        removedClasses.removeAll(afterClasses);

        Set<String> addedClasses = new HashSet<String>(afterClasses);
        addedClasses.removeAll(beforeClasses);

        Map<String, String> removedShortNameToFullNameMap = buildShortNameToFullNameMap(removedClasses);

        // Map names of added classes to names of removed classes if
        // they have the same short name.
        for (String fullAddedName : addedClasses) {

            // FIXME: could use a similarity metric to match added and removed
            // classes. Instead, we just match based on the short class name.

            String shortAddedName = getShortClassName(fullAddedName);
            String fullRemovedName = removedShortNameToFullNameMap.get(shortAddedName);
            if (fullRemovedName != null) {
                if (DEBUG) {
                    System.err.println(fullAddedName + " --> " + fullRemovedName);
                }
                rewriteMap.put(fullAddedName, fullRemovedName);
            }

        }

        return this;
    }

    public boolean isEmpty() {
        return rewriteMap.isEmpty();
    }

    @Override
    public String rewriteClassName(String className) {
        String rewrittenClassName = rewriteMap.get(className);
        if (rewrittenClassName != null) {
            className = rewrittenClassName;
        }
        return className;
    }

    /**
     * Find set of classes referenced in given BugCollection.
     *
     * @param bugCollection
     * @return set of classes referenced in the BugCollection
     */
    private Set<String> buildClassSet(BugCollection bugCollection) {
        Set<String> classSet = new HashSet<String>();

        for (Iterator<BugInstance> i = bugCollection.iterator(); i.hasNext();) {
            BugInstance warning = i.next();
            for (Iterator<BugAnnotation> j = warning.annotationIterator(); j.hasNext();) {
                BugAnnotation annotation = j.next();
                if (!(annotation instanceof ClassAnnotation)) {
                    continue;
                }
                classSet.add(((ClassAnnotation) annotation).getClassName());
            }
        }

        return classSet;
    }

    /**
     * Build a map of short class names (without package) to full class names.
     *
     * @param classSet
     *            set of fully-qualified class names
     * @return map of short class names to fully-qualified class names
     */
    private Map<String, String> buildShortNameToFullNameMap(Set<String> classSet) {
        Map<String, String> result = new HashMap<String, String>();
        for (String className : classSet) {
            String shortClassName = getShortClassName(className);
            result.put(shortClassName, className);
        }
        return result;
    }

    /**
     * Get a short class name (no package part).
     *
     * @param className
     *            a class name
     * @return short class name
     */
    private String getShortClassName(String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot >= 0) {
            className = className.substring(lastDot + 1);
        }
        return className.toLowerCase(Locale.US).replace('+', '$');
    }

}
