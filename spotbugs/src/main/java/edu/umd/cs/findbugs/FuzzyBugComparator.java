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

package edu.umd.cs.findbugs;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

import edu.umd.cs.findbugs.internalAnnotations.StaticConstant;
import edu.umd.cs.findbugs.model.ClassNameRewriter;

/**
 * A slightly more intellegent way of comparing BugInstances from two versions
 * to see if they are the "same". Uses class and method hashes to try to handle
 * renamings, at least for simple cases. (<em>Hashes disabled for the
 * time being.</em>) Uses opcode context to try to identify code that is the
 * same, even if it moves within the method. Also compares by bug abbreviation
 * rather than bug type, since the "same" bug can change type if the context
 * changes (e.g., "definitely null" to "null on simple path" for a null pointer
 * dereference). Also, we often change bug types between different versions of
 * FindBugs.
 *
 * @see edu.umd.cs.findbugs.BugInstance
 * @see edu.umd.cs.findbugs.VersionInsensitiveBugComparator
 * @author David Hovemeyer
 */
public class FuzzyBugComparator implements WarningComparator {
    private static final boolean DEBUG = false;

    // Don't use hashes for now. Still ironing out issues there.
    //    private static final boolean USE_HASHES = false;

    //    private static final long serialVersionUID = 1L;

    /**
     * Filter ignored BugAnnotations from given Iterator.
     */
    private static class FilteringBugAnnotationIterator implements Iterator<BugAnnotation> {

        Iterator<BugAnnotation> iter;

        BugAnnotation next;

        public FilteringBugAnnotationIterator(Iterator<BugAnnotation> iter) {
            this.iter = iter;
        }

        private void findNext() {
            if (next == null) {
                while (iter.hasNext()) {
                    BugAnnotation candidate = iter.next();
                    if (!ignore(candidate)) {
                        next = candidate;
                        break;
                    }
                }
            }
        }

        @Override
        public boolean hasNext() {
            findNext();
            return next != null;
        }

        @Override
        public BugAnnotation next() {
            findNext();
            if (next == null) {
                throw new NoSuchElementException();
            }
            BugAnnotation result = next;
            next = null;
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Keep track of which BugCollections the various BugInstances have come
     * from.
     */
    private final IdentityHashMap<BugInstance, BugCollection> bugCollectionMap;

    private ClassNameRewriter classNameRewriter;

    /**
     * Map of class hashes to canonicate class names used for comparison
     * purposes.
     */
    // private Map<ClassHash, String> classHashToCanonicalClassNameMap;

    public FuzzyBugComparator() {
        if (DEBUG) {
            System.out.println("Created fuzzy comparator");
        }
        this.bugCollectionMap = new IdentityHashMap<>();
        // this.classHashToCanonicalClassNameMap = new TreeMap<ClassHash,
        // String>();
    }

    /**
     * Register a BugCollection. This allows us to find the class and method
     * hashes for BugInstances to be compared.
     *
     * @param bugCollection
     *            a BugCollection
     */
    public void registerBugCollection(BugCollection bugCollection) {
        // For now, nothing to do
    }

    @Override
    public void setClassNameRewriter(ClassNameRewriter classNameRewriter) {
        this.classNameRewriter = classNameRewriter;
    }

    @Override
    public int compare(BugInstance lhs, BugInstance rhs) {
        int cmp;

        if (DEBUG) {
            System.out.println("Fuzzy comparison");
        }

        // Bug abbreviations must match.
        BugPattern lhsPattern = lhs.getBugPattern();
        BugPattern rhsPattern = rhs.getBugPattern();

        if ((cmp = lhsPattern.getAbbrev().compareTo(rhsPattern.getAbbrev())) != 0) {
            return cmp;
        }

        BugCollection lhsCollection = bugCollectionMap.get(lhs);
        BugCollection rhsCollection = bugCollectionMap.get(rhs);

        // Scan through bug annotations, comparing fuzzily if possible

        Iterator<BugAnnotation> lhsIter = new FilteringBugAnnotationIterator(lhs.annotationIterator());
        Iterator<BugAnnotation> rhsIter = new FilteringBugAnnotationIterator(rhs.annotationIterator());

        while (lhsIter.hasNext() && rhsIter.hasNext()) {
            BugAnnotation lhsAnnotation = lhsIter.next();
            BugAnnotation rhsAnnotation = rhsIter.next();

            if (DEBUG) {
                System.out.println("Compare annotations: " + lhsAnnotation + "," + rhsAnnotation);
            }

            // Annotation classes must match exactly
            cmp = lhsAnnotation.getClass().getName().compareTo(rhsAnnotation.getClass().getName());
            if (cmp != 0) {
                if (DEBUG) {
                    System.out.println("annotation class mismatch: " + lhsAnnotation.getClass().getName() + ","
                            + rhsAnnotation.getClass().getName());
                }
                return cmp;
            }

            if (lhsAnnotation.getClass() == ClassAnnotation.class) {
                cmp = compareClasses(lhsCollection, rhsCollection, (ClassAnnotation) lhsAnnotation,
                        (ClassAnnotation) rhsAnnotation);
            } else if (lhsAnnotation.getClass() == MethodAnnotation.class) {
                cmp = compareMethods(lhsCollection, rhsCollection, (MethodAnnotation) lhsAnnotation,
                        (MethodAnnotation) rhsAnnotation);
            } else if (lhsAnnotation.getClass() == SourceLineAnnotation.class) {
                cmp = compareSourceLines(lhsCollection, rhsCollection, (SourceLineAnnotation) lhsAnnotation,
                        (SourceLineAnnotation) rhsAnnotation);
            } else {
                // everything else just compare directly
                cmp = lhsAnnotation.compareTo(rhsAnnotation);
            }

            if (cmp != 0) {
                return cmp;
            }
        }

        // Number of bug annotations must match
        if (!lhsIter.hasNext() && !rhsIter.hasNext()) {
            if (DEBUG) {
                System.out.println("Match!");
            }
            return 0;
        } else {
            return (lhsIter.hasNext() ? 1 : -1);
        }
    }

    /*
     * @param type
     * @return the code of the Bug
     *
    private String getCode(String type) {
        int bar = type.indexOf('_');
        if (bar < 0)
            return "";
        else
            return type.substring(0, bar);
    }*/

    private static int compareNullElements(Object a, Object b) {
        if (a != null) {
            return 1;
        } else if (b != null) {
            return -1;
        } else {
            return 0;
        }
    }

    public int compareClasses(BugCollection lhsCollection, BugCollection rhsCollection, ClassAnnotation lhsClass,
            ClassAnnotation rhsClass) {
        if (lhsClass == null || rhsClass == null) {
            return compareNullElements(lhsClass, rhsClass);
        } else {
            return compareClassesByName(lhsCollection, rhsCollection, lhsClass.getClassName(), rhsClass.getClassName());
        }
    }

    // Compare classes: either exact fully qualified name must match, or class
    // hash must match
    public int compareClassesByName(BugCollection lhsCollection, BugCollection rhsCollection, String lhsClassName,
            String rhsClassName) {

        lhsClassName = rewriteClassName(lhsClassName);
        rhsClassName = rewriteClassName(rhsClassName);

        return lhsClassName.compareTo(rhsClassName);
    }

    /**
     * @param className
     * @return the rewritten class name
     */
    private String rewriteClassName(String className) {
        if (classNameRewriter != null) {
            className = classNameRewriter.rewriteClassName(className);
        }
        return className;
    }

    // Compare methods: either exact name and signature must match, or method
    // hash must match
    public int compareMethods(BugCollection lhsCollection, BugCollection rhsCollection, MethodAnnotation lhsMethod,
            MethodAnnotation rhsMethod) {
        if (lhsMethod == null || rhsMethod == null) {
            return compareNullElements(lhsMethod, rhsMethod);
        }

        // Compare for exact match
        int cmp = lhsMethod.compareTo(rhsMethod);

        return cmp;
    }

    /**
     * For now, just look at the 2 preceding and succeeding opcodes for fuzzy
     * source line matching.
     */
    //    private static final int NUM_CONTEXT_OPCODES = 2;

    /**
     * Compare source line annotations.
     *
     * @param rhsCollection
     *            lhs BugCollection
     * @param lhsCollection
     *            rhs BugCollection
     * @param lhs
     *            a SourceLineAnnotation
     * @param rhs
     *            another SourceLineAnnotation
     * @return comparison of lhs and rhs
     */
    public int compareSourceLines(BugCollection lhsCollection, BugCollection rhsCollection, SourceLineAnnotation lhs,
            SourceLineAnnotation rhs) {
        if (lhs == null || rhs == null) {
            return compareNullElements(lhs, rhs);
        }

        // Classes must match fuzzily.
        int cmp = compareClassesByName(lhsCollection, rhsCollection, lhs.getClassName(), rhs.getClassName());
        if (cmp != 0) {
            return cmp;
        }

        return 0;
    }

    // See "FindBugsAnnotationDescriptions.properties"
    @StaticConstant
    private static final HashSet<String> significantDescriptionSet = new HashSet<>();
    static {
        // Classes, methods, and fields are significant.
        significantDescriptionSet.add("CLASS_DEFAULT");
        significantDescriptionSet.add("CLASS_EXCEPTION");
        significantDescriptionSet.add("CLASS_REFTYPE");
        significantDescriptionSet.add(ClassAnnotation.SUPERCLASS_ROLE);
        significantDescriptionSet.add(ClassAnnotation.IMPLEMENTED_INTERFACE_ROLE);
        significantDescriptionSet.add(ClassAnnotation.INTERFACE_ROLE);
        significantDescriptionSet.add("METHOD_DEFAULT");
        significantDescriptionSet.add(MethodAnnotation.METHOD_CALLED);
        significantDescriptionSet.add("METHOD_DANGEROUS_TARGET"); // but do NOT
        // use safe
        // targets
        significantDescriptionSet.add("METHOD_DECLARED_NONNULL");
        significantDescriptionSet.add("FIELD_DEFAULT");
        significantDescriptionSet.add("FIELD_ON");
        significantDescriptionSet.add("FIELD_SUPER");
        significantDescriptionSet.add("FIELD_MASKED");
        significantDescriptionSet.add("FIELD_MASKING");
        significantDescriptionSet.add("FIELD_STORED");
        significantDescriptionSet.add("TYPE_DEFAULT");
        significantDescriptionSet.add("TYPE_EXPECTED");
        significantDescriptionSet.add("TYPE_FOUND");

        significantDescriptionSet.add("LOCAL_VARIABLE_NAMED");

        // Many int annotations are NOT significant: e.g., sync %, biased locked
        // %, bytecode offset, etc.
        // The null parameter annotations, however, are definitely significant.
        significantDescriptionSet.add("INT_NULL_ARG");
        significantDescriptionSet.add("INT_MAYBE_NULL_ARG");
        significantDescriptionSet.add("INT_NONNULL_PARAM");
        // Only DEFAULT source line annotations are significant.
        significantDescriptionSet.add("SOURCE_LINE_DEFAULT");
    }

    public static boolean ignore(BugAnnotation annotation) {
        return !significantDescriptionSet.contains(annotation.getDescription());
    }
}
