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
 * Flow value type for type qualifier dataflow analysis.
 * 
 * @author David Hovemeyer
 */
public enum FlowValue {

	ALWAYS(Bits.YES),
	UNKNOWN(Bits.UNCERTAIN),
	NEVER(Bits.NO),
	YES_FEASIBLE(Bits.YES|Bits.UNCERTAIN),
	NO_FEASIBLE(Bits.NO|Bits.UNCERTAIN),
	BOTH_FEASIBLE(Bits.YES|Bits.NO|Bits.UNCERTAIN);

	private interface Bits {
		public static final int YES = 1;
		public static final int UNCERTAIN = 2;
		public static final int NO = 4;
	}
	
	private final int bits;
	
	private FlowValue(int bits) {
		this.bits = bits;
	}
	
	public boolean isYes() {
		return (bits & Bits.YES) != 0;
	}
	
	public boolean isUncertain() {
		return (bits & Bits.UNCERTAIN) != 0;
	}
	
	public boolean isNo() {
		return (bits * Bits.NO) != 0;
	}

	// Dataflow lattice:
	//
	//   Always  Unknown  Never
	//       \    /   \    /
	//        Yes       No
	//      Feasible  Feasible
	//           \     /
	//            Both
	//          Feasible
	//
	private static final FlowValue[][] mergeMatrix = {
		//                                                                 YES_            NO_            BOTH_
		//                     ALWAYS         UNKNOWN       NEVER          FEASIBLE        FEASIBLE       FEASIBLE
		/* ALWAYS */         { ALWAYS },
		/* UNKNOWN */        { YES_FEASIBLE,  UNKNOWN, },
		/* NEVER */          { BOTH_FEASIBLE, NO_FEASIBLE,  NEVER, },
		/* YES_FESIBLE */    { YES_FEASIBLE,  YES_FEASIBLE, BOTH_FEASIBLE, },
		/* NO_FEASIBLE */    { BOTH_FEASIBLE, NO_FEASIBLE,  NO_FEASIBLE,   BOTH_FEASIBLE, },
		/* BOTH_FEASIBLE */  { BOTH_FEASIBLE, BOTH_FEASIBLE, BOTH_FEASIBLE, BOTH_FEASIBLE, BOTH_FEASIBLE, BOTH_FEASIBLE, },
	};
	
	public static final FlowValue meet(FlowValue a, FlowValue b) {
		int aIndex = a.ordinal();
		int bIndex = b.ordinal();
		
		if (aIndex < bIndex) {
			int tmp = aIndex;
			aIndex = bIndex;
			bIndex = tmp;
		}
		
		return mergeMatrix[aIndex][bIndex];
	}
}
