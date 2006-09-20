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

import java.util.BitSet;
import java.util.Set;
import java.util.SortedSet;

import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * Callback interface for collecting null pointer derefs and
 * redundant null comparisons.
 * 
 * @see edu.umd.cs.findbugs.ba.npe.NullDerefAndRedundantComparisonFinder
 * @author David Hovemeyer
 */
public interface NullDerefAndRedundantComparisonCollector {
	/**
	 * Subclasses should override this method to capture locations where
	 * a null pointer is dereferenced.
	 * @param classContext TODO
	 * @param location    the Location of the null dereference
	 * @param valueNumber the ValueNumber of the possibly-null value
	 * @param refValue    the kind of possibly-null value dereferenced
	 * @param vnaFrame 	  The ValueNumber Frame at the point where the dereference occurred
	 */
	public void foundNullDeref(ClassContext classContext, Location location, ValueNumber valueNumber, IsNullValue refValue, ValueNumberFrame vnaFrame);

	/**
	 * Subclasses should override this method to capture locations where
	 * a redundant null comparision is performed.
	 * 
	 * @param location        the Location of the redundant null check
	 * @param redundantBranch the RedundantBranch
	 */
	public void foundRedundantNullCheck(Location location, RedundantBranch redundantBranch);
	
	/**
	 * Subclasses should override this method to capture values
	 * assigned null (or that become null through a comparison and branch)
	 * that are guaranteed to reach a dereference (ignoring
	 * implicit exception paths).
	 * @param derefLocationSet     set of locations where dereferences occur
	 * @param doomedLocations TODO
	 * @param vna TODO
	 * @param refValue             the null value
	 * @param alwaysOnExceptionPath true if the location(s) where the value was observed
	 *                               to be null and unconditionally dereferenced were
	 *                               all on exception paths
	 * @param npeIfStatementCovered TODO
	 * @param assignedNullLocationSet set of locations where the value becomes null
	 */
	public void foundGuaranteedNullDeref(
			Set<Location> assignedNullLocationSet,
			Set<Location> derefLocationSet,
			SortedSet<Location> doomedLocations,
			ValueNumberDataflow vna,
			ValueNumber refValue,
			boolean alwaysOnExceptionPath,
			boolean npeIfStatementCovered);
}
