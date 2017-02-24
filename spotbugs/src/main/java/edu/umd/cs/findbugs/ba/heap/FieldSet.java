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

package edu.umd.cs.findbugs.ba.heap;

import java.util.HashSet;
import java.util.Set;

import edu.umd.cs.findbugs.ba.XField;

/**
 * @author David Hovemeyer
 */
public class FieldSet {
    private boolean isTop, isBottom;

    private final Set<XField> fieldSet;

    public FieldSet() {
        fieldSet = new HashSet<XField>();
    }

    public void setTop() {
        clear();
        isTop = true;
    }

    public boolean isTop() {
        return isTop;
    }

    public void setBottom() {
        clear();
        isBottom = true;
    }

    public boolean isBottom() {
        return isBottom;
    }

    public boolean isValid() {
        return !isTop && !isBottom;
    }

    public boolean isEmpty() {
        return !isTop && !isBottom && fieldSet.isEmpty();
    }

    public void clear() {
        isTop = isBottom = false;
        fieldSet.clear();
    }

    public void addField(XField field) {
        if (!isValid()) {
            throw new IllegalStateException();
        }
        fieldSet.add(field);
    }

    public boolean contains(XField field) {
        return fieldSet.contains(field);
    }

    public void mergeWith(FieldSet other) {
        if (other.isTop() || this.isBottom()) {
            return;
        }

        if (other.isBottom() || this.isTop()) {
            this.copyFrom(other);
            return;
        }

        fieldSet.addAll(other.fieldSet);
    }

    public boolean sameAs(FieldSet other) {
        return this.isTop == other.isTop && this.isBottom == other.isBottom && this.fieldSet.equals(other.fieldSet);
    }

    public void copyFrom(FieldSet other) {
        this.isTop = other.isTop;
        this.isBottom = other.isBottom;
        this.fieldSet.clear();
        this.fieldSet.addAll(other.fieldSet);
    }

    public boolean isIntersectionNonEmpty(FieldSet other) {
        for (XField field : fieldSet) {
            if (other.fieldSet.contains(field)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        if (isTop) {
            return "TOP";
        } else if (isBottom) {
            return "BOTTOM";
        } else {
            return fieldSet.toString();
        }
    }
}
