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

package edu.umd.cs.findbugs.ba;

/**
 * @author pugh
 */
public class AnnotationEnumeration<E extends AnnotationEnumeration<E>> implements Comparable<E> {
    private final int index;

    protected final String name;

    protected AnnotationEnumeration(String s, int i) {
        name = s;
        index = i;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof AnnotationEnumeration<?>)) {
            return false;
        }
        return index == ((AnnotationEnumeration<?>) o).getIndex();
    }

    @Override
    public final int hashCode() {
        return index;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * @return Returns the index.
     */
    public int getIndex() {
        return index;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(T)
     */
    @Override
    public int compareTo(E a) {
        return index - a.getIndex();
    }
}
