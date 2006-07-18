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

import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;

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
	 * 
	 * @param location    the Location of the null dereference
	 * @param valueNumber the ValueNumber of the possibly-null value
	 * @param refValue    the kind of possibly-null value dereferenced
	 */
	public void foundNullDeref(Location location, ValueNumber valueNumber, IsNullValue refValue);

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
	 * 
	 * @param assignedNullLocation set of locations where the value becomes null
	 * @param derefLocationSet     set of locations where dereferences occur
	 * @param refValue             the null value
	 */
	public void foundGuaranteedNullDeref(
			Set<Location> assignedNullLocationSet,
			Set<Location> derefLocationSet,
			ValueNumber refValue);
}
