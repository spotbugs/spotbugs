/*
 * Bytecode analysis framework
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

package edu.umd.cs.findbugs.ba.npe;

import java.util.Set;
import java.util.SortedSet;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * Callback interface for collecting null pointer derefs and redundant null
 * comparisons.
 *
 * @see edu.umd.cs.findbugs.ba.npe.NullDerefAndRedundantComparisonFinder
 * @author David Hovemeyer
 */
public interface NullDerefAndRedundantComparisonCollector {
    /**
     * Subclasses should override this method to capture locations where a null
     * pointer is dereferenced.
     *
     * @param location
     *            the Location of the null dereference
     * @param valueNumber
     *            the ValueNumber of the possibly-null value
     * @param refValue
     *            the kind of possibly-null value dereferenced
     * @param vnaFrame
     *            The ValueNumber Frame at the point where the dereference
     *            occurred
     * @deprecated Use
     *             {@link #foundNullDeref(Location,ValueNumber,IsNullValue,ValueNumberFrame,boolean)}
     *             instead
     */
    @Deprecated
    public void foundNullDeref(Location location, ValueNumber valueNumber, IsNullValue refValue, ValueNumberFrame vnaFrame);

    /**
     * Subclasses should override this method to capture locations where a null
     * pointer is dereferenced.
     *
     * @param location
     *            the Location of the null dereference
     * @param valueNumber
     *            the ValueNumber of the possibly-null value
     * @param refValue
     *            the kind of possibly-null value dereferenced
     * @param vnaFrame
     *            The ValueNumber Frame at the point where the dereference
     *            occurred
     * @param isConsistent
     *            true if the refValue is identical at all clones of the same
     *            instruction
     */
    public void foundNullDeref(Location location, ValueNumber valueNumber, IsNullValue refValue, ValueNumberFrame vnaFrame,
            boolean isConsistent);

    /**
     * Subclasses should override this method to capture locations where a
     * redundant null comparison is performed.
     *
     * @param location
     *            the Location of the redundant null check
     * @param redundantBranch
     *            the RedundantBranch
     */
    public void foundRedundantNullCheck(Location location, RedundantBranch redundantBranch);

    /**
     * Subclasses should override this method to capture values assigned null
     * (or that become null through a comparison and branch) that are guaranteed
     * to reach a dereference (ignoring implicit exception paths).
     *
     * @param assignedNullLocationSet
     *            set of locations where the value becomes null
     * @param derefLocationSet
     *            set of locations where dereferences occur
     * @param doomedLocations
     *            locations at which the value is doomed
     * @param vna
     *            ValueNumberDataflow
     * @param refValue
     *            the null value
     * @param variableAnnotation
     *            TODO
     * @param deref
     *            TODO
     * @param npeIfStatementCovered
     *            true if doom location is a statement
     */
    public void foundGuaranteedNullDeref(@Nonnull Set<Location> assignedNullLocationSet, @Nonnull Set<Location> derefLocationSet,
            SortedSet<Location> doomedLocations, ValueNumberDataflow vna, ValueNumber refValue,
            @CheckForNull BugAnnotation variableAnnotation, NullValueUnconditionalDeref deref, boolean npeIfStatementCovered);
}
