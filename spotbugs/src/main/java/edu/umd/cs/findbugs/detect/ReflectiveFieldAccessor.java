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
package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.detect.ReflectiveAccessTracker.AccessType;

/**
 * A reflective field accessor (VarHandle, MethodHandle or AtomicFieldUpdater).
 * <p>
 * A plain mutable value class. It is created when the accessor handle is obtained (findGetter,
 * findVarHandle, newUpdater, ...) and completed once the handle is stored into a field, at which
 * point {@link #accessorField} is filled in. For a MethodHandle the {@link #accessType} is fixed
 * at creation (GETTER or SETTER); for VarHandle/AtomicFieldUpdater it is {@link AccessType#BOTH}
 * because the actual access type is only known at invocation time.
 */
class ReflectiveFieldAccessor {

    /** The field the accessor reads or writes. */
    private final XField actualField;

    /** GETTER/SETTER for MethodHandle, BOTH for VarHandle/AtomicFieldUpdater. */
    private final AccessType accessType;

    /** Source line where the accessor was declared. */
    private final SourceLineAnnotation declarationSourceLine;

    /** The field holding the handle; set once the handle is stored. */
    private XField accessorField;

    /** Set during resolution if the accessor is actually invoked. */
    private boolean wasUsed;

    ReflectiveFieldAccessor(final XField actualField, final AccessType accessType,
            final SourceLineAnnotation declarationSourceLine) {
        this.actualField = actualField;
        this.accessType = accessType;
        this.declarationSourceLine = declarationSourceLine;
    }

    void markUsed() {
        wasUsed = true;
    }

    boolean wasUsed() {
        return wasUsed;
    }

    XField accessorField() {
        return accessorField;
    }

    void setAccessorField(final XField accessorField) {
        this.accessorField = accessorField;
    }

    XField actualField() {
        return actualField;
    }

    AccessType accessType() {
        return accessType;
    }

    SourceLineAnnotation sourceLine() {
        return declarationSourceLine;
    }
}
