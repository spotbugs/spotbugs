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

import java.util.Comparator;

/**
 * Very sloppy bug comparator: if the warnings are of the same type,
 * and in the same class/method/field, assume they are the same.
 * 
 * @author David Hovemeyer
 */
public class SloppyBugComparator implements Comparator<BugInstance> {
	
	private int compareAllowingNull(BugAnnotation lhs, BugAnnotation rhs) {
		if (lhs == null || rhs == null) {
			if (lhs == null && rhs == null)
				return 0;
			else
				return (lhs == null) ? -1 : 1;
		}
		return lhs.compareTo(rhs);
	}
	
	
	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(T, T)
	 */
	public int compare(BugInstance lhs, BugInstance rhs) {
		int cmp;
		
		// Bug types must match
		cmp = lhs.getType().compareTo(rhs.getType());
		if (cmp != 0)
			return cmp;
		
		// Primary class must match
		cmp = compareAllowingNull(lhs.getPrimaryClass(), rhs.getPrimaryClass());
		if (cmp != 0)
			return cmp;
		
		// Primary method must match (if any)
		cmp = compareAllowingNull(lhs.getPrimaryMethod(), rhs.getPrimaryMethod());
		if (cmp != 0)
			return cmp;
		
		// Primary field must match (if any)
		cmp = compareAllowingNull(lhs.getPrimaryField(), rhs.getPrimaryField());
		if (cmp != 0)
			return cmp;
		
		// Assume they're the same
		return 0;
	}
}
