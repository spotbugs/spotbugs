/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.ba.jsr305;

/**
 * This is the FindBugs representation of the When value
 * of a type qualifier.  (Corresponding to the 
 * javax.annotation.meta.When enumeration.)
 * 
 * @author David Hovemeyer
 */
public enum When {
	ASSUME_ALWAYS, ALWAYS, UNKNOWN, MAYBE_NOT, NEVER;


	// Dataflow lattice:
	//
	// Always    Never
	//   |        |
	// Assume     |
	// always   Unknown
	//     \    /
	//      \  /
	//      Maybe
	//       not
	private static final When[][] mergeMatrix = {
                           // ASSUME_                                    MAYBE_
                           // ALWAYS          ALWAYS          UNKNOWN    NOT         NEVER
                           // ----------------------------------------------------------------
		/* ASSUME_ALWAYS */ { ASSUME_ALWAYS,  ASSUME_ALWAYS,  MAYBE_NOT, MAYBE_NOT,  MAYBE_NOT  },
		/* ALWAYS */        { ASSUME_ALWAYS,  ALWAYS,         MAYBE_NOT, MAYBE_NOT },
		/* UNKNOWN */       { MAYBE_NOT,      MAYBE_NOT,      UNKNOWN },
		/* MAYBE_NOT */     { MAYBE_NOT,      MAYBE_NOT, },
		/* NEVER */         { MAYBE_NOT },
	};
	


	
	public static When meet(When a, When b) {
		int aIndex = a.ordinal();
		int bIndex = b.ordinal();
		
		if (aIndex > bIndex) {
			int tmp = aIndex;
			aIndex = bIndex;
			bIndex = tmp;
		}
		
		return mergeMatrix[aIndex][bIndex];
	}
}
