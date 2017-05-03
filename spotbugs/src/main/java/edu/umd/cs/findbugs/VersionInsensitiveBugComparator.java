/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import java.util.Iterator;
import java.util.NoSuchElementException;

import edu.umd.cs.findbugs.model.ClassNameRewriter;
import edu.umd.cs.findbugs.model.ClassNameRewriterUtil;
import edu.umd.cs.findbugs.model.IdentityClassNameRewriter;

/**
 * Compare bug instances by only those criteria which we would expect to remain
 * constant between versions.
 */
public class VersionInsensitiveBugComparator implements WarningComparator {

    private ClassNameRewriter classNameRewriter = IdentityClassNameRewriter.instance();

    private boolean exactBugPatternMatch = true;

    private boolean comparePriorities;

    public VersionInsensitiveBugComparator() {
    }

    @Override
    public void setClassNameRewriter(ClassNameRewriter classNameRewriter) {
        this.classNameRewriter = classNameRewriter;
    }

    public void setComparePriorities(boolean b) {
        comparePriorities = b;
    }

    /**
     * Wrapper for BugAnnotation iterators, which filters out annotations we
     * don't care about.
     */
    private class FilteringAnnotationIterator implements Iterator<BugAnnotation> {
        private final Iterator<BugAnnotation> iter;

        private BugAnnotation next;

        public FilteringAnnotationIterator(Iterator<BugAnnotation> iter) {
            this.iter = iter;
            this.next = null;
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

        private void findNext() {
            while (next == null) {
                if (!iter.hasNext()) {
                    break;
                }
                BugAnnotation candidate = iter.next();
                if (!isBoring(candidate)) {
                    next = candidate;
                    break;
                }
            }
        }

    }

    private boolean isBoring(BugAnnotation annotation) {
        return !(annotation instanceof LocalVariableAnnotation || annotation.isSignificant());
    }

    /*
    private static int compareNullElements(Object a, Object b) {
        if (a != null)
            return 1;
        else if (b != null)
            return -1;
        else
            return 0;
    }

    private static String getCode(String pattern) {
        int sep = pattern.indexOf('_');
        if (sep < 0) {
            return "";
        }
        return pattern.substring(0, sep);
    }

    private void dump(BugInstance bug) {
        System.out.println(bug.getMessage());
        Iterator<BugAnnotation> i = bug.annotationIterator();
        while (i.hasNext()) {
            System.out.println("  " + i.next());
        }
    }*/

    @Override
    public int compare(BugInstance lhs, BugInstance rhs) {
        // Attributes of BugInstance.
        // Compare abbreviation
        // Compare class and method annotations (ignoring line numbers).
        // Compare field annotations.

        int cmp;

        BugPattern lhsPattern = lhs.getBugPattern();
        BugPattern rhsPattern = rhs.getBugPattern();

        // Compare by abbrev instead of type. The specific bug type can
        // change
        // (e.g., "definitely null" to "null on simple path"). Also, we
        // often
        // change bug pattern types from one version of FindBugs to the
        // next.
        //
        // Source line and field name are still matched precisely, so this
        // shouldn't
        // cause loss of precision.
        if ((cmp = lhsPattern.getAbbrev().compareTo(rhsPattern.getAbbrev())) != 0) {
            return cmp;
        }
        if (isExactBugPatternMatch() && (cmp = lhsPattern.getType().compareTo(rhsPattern.getType())) != 0) {
            return cmp;
        }

        if (comparePriorities) {
            cmp = lhs.getPriority() - rhs.getPriority();
            if (cmp != 0) {
                return cmp;
            }
        }

        Iterator<BugAnnotation> lhsIter = new FilteringAnnotationIterator(lhs.annotationIterator());
        Iterator<BugAnnotation> rhsIter = new FilteringAnnotationIterator(rhs.annotationIterator());

        while (lhsIter.hasNext() && rhsIter.hasNext()) {
            BugAnnotation lhsAnnotation = lhsIter.next();
            BugAnnotation rhsAnnotation = rhsIter.next();
            Class<? extends BugAnnotation> lhsClass;
            while (true) {
                // Different annotation types obviously cannot be equal,
                // so just compare by class name.
                lhsClass = lhsAnnotation.getClass();
                Class<? extends BugAnnotation> rhsClass = rhsAnnotation.getClass();
                if (lhsClass == rhsClass) {
                    break;
                }
                if (lhsClass == LocalVariableAnnotation.class && !((LocalVariableAnnotation) lhsAnnotation).isSignificant()
                        && lhsIter.hasNext()) {
                    lhsAnnotation = lhsIter.next();
                } else if (rhsClass == LocalVariableAnnotation.class && !((LocalVariableAnnotation) rhsAnnotation).isSignificant()
                        && rhsIter.hasNext()) {
                    rhsAnnotation = rhsIter.next();
                } else {
                    return lhsClass.getName().compareTo(rhsClass.getName());
                }
            }

            if (lhsClass == ClassAnnotation.class) {
                // ClassAnnotations should have their class names rewritten to
                // handle moved and renamed classes.

                String lhsClassName = classNameRewriter.rewriteClassName(((ClassAnnotation) lhsAnnotation).getClassName());
                String rhsClassName = classNameRewriter.rewriteClassName(((ClassAnnotation) rhsAnnotation).getClassName());

                cmp = lhsClassName.compareTo(rhsClassName);

            } else if (lhsClass == MethodAnnotation.class) {
                // Rewrite class names in MethodAnnotations
                MethodAnnotation lhsMethod = ClassNameRewriterUtil.convertMethodAnnotation(classNameRewriter,
                        (MethodAnnotation) lhsAnnotation);
                MethodAnnotation rhsMethod = ClassNameRewriterUtil.convertMethodAnnotation(classNameRewriter,
                        (MethodAnnotation) rhsAnnotation);
                cmp = lhsMethod.compareTo(rhsMethod);

            } else if (lhsClass == FieldAnnotation.class) {
                // Rewrite class names in FieldAnnotations
                FieldAnnotation lhsField = ClassNameRewriterUtil.convertFieldAnnotation(classNameRewriter,
                        (FieldAnnotation) lhsAnnotation);
                FieldAnnotation rhsField = ClassNameRewriterUtil.convertFieldAnnotation(classNameRewriter,
                        (FieldAnnotation) rhsAnnotation);
                cmp = lhsField.compareTo(rhsField);

            } else if (lhsClass == StringAnnotation.class) {
                String lhsString = ((StringAnnotation) lhsAnnotation).getValue();
                String rhsString = ((StringAnnotation) rhsAnnotation).getValue();
                cmp = lhsString.compareTo(rhsString);

            } else if (lhsClass == LocalVariableAnnotation.class) {
                String lhsName = ((LocalVariableAnnotation) lhsAnnotation).getName();
                String rhsName = ((LocalVariableAnnotation) rhsAnnotation).getName();
                if ("?".equals(lhsName) || "?".equals(rhsName)) {
                    continue;
                }
                cmp = lhsName.compareTo(rhsName);

            } else if (lhsClass == TypeAnnotation.class) {
                String lhsType = ((TypeAnnotation) lhsAnnotation).getTypeDescriptor();
                String rhsType = ((TypeAnnotation) rhsAnnotation).getTypeDescriptor();
                lhsType = ClassNameRewriterUtil.rewriteSignature(classNameRewriter, lhsType);
                rhsType = ClassNameRewriterUtil.rewriteSignature(classNameRewriter, rhsType);
                cmp = lhsType.compareTo(rhsType);

            } else if (lhsClass == IntAnnotation.class) {
                int lhsValue = ((IntAnnotation) lhsAnnotation).getValue();
                int rhsValue = ((IntAnnotation) rhsAnnotation).getValue();
                cmp = lhsValue - rhsValue;

            } else if (isBoring(lhsAnnotation)) {
                throw new IllegalStateException("Impossible");
            } else {
                throw new IllegalStateException("Unknown annotation type: " + lhsClass.getName());
            }
            if (cmp != 0) {
                return cmp;
            }
        }

        if (interestingNext(rhsIter)) {
            return -1;
        } else if (interestingNext(lhsIter)) {
            return 1;
        } else {
            return 0;
        }
    }

    private boolean interestingNext(Iterator<BugAnnotation> i) {
        while (i.hasNext()) {
            BugAnnotation a = i.next();
            if (isBoring(a)) {
                continue;
            }
            if (!(a instanceof LocalVariableAnnotation)) {
                return true;
            }
            if (((LocalVariableAnnotation) a).isSignificant()) {
                return true;
            }
        }
        return false;
    }

    public void setExactBugPatternMatch(boolean exactBugPatternMatch) {
        this.exactBugPatternMatch = exactBugPatternMatch;
    }

    public boolean isExactBugPatternMatch() {
        return exactBugPatternMatch;
    }
}
