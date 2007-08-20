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

import javax.annotation.meta.When;

/**
 * Flow value type for type qualifier dataflow analysis.
 * 
 * @author David Hovemeyer
 */
public enum FlowValue {

	TOP(0),
	ALWAYS(Bits.YES),
	NEVER(Bits.NO),
	UNKNOWN(Bits.YES|Bits.NO|Bits.UNCERTAIN);

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
		return (bits & Bits.NO) != 0;
	}

	// Dataflow lattice:
	//
	//       Top
	//      /    \
	//  Always  Never
	//      \    /
	//      Unknown
	//
	private static final FlowValue[][] mergeMatrix = {
		//              TOP       ALWAYS    NEVER     UNKNOWN
		/* TOP */     { TOP,  },
		/* ALWAYS */  { ALWAYS,   ALWAYS, },
		/* NEVER */   { NEVER,    UNKNOWN,  NEVER, },
		/* UNKNOWN */ { UNKNOWN,  UNKNOWN,  UNKNOWN,  UNKNOWN },
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

	/**
	 * Determine whether given flow values conflict.
	 * 
	 * @param forward  a forwards flow value
	 * @param backward a backwards flow value
	 * @param isStrictQualifier  true if the type qualifier being checked is strict
	 * @return true if values conflict, false otherwise
	 */
	public static boolean valuesConflict(FlowValue forward, FlowValue backward) {
		if (forward == TOP || backward == TOP) {
			return false;
		}
		return (forward == ALWAYS && backward == NEVER)
			|| (forward == NEVER && backward == ALWAYS);
	}

	/**
	 * Convert a When value to a FlowValue value.
	 * 
	 * @param when a When value
	 * @return the corresponding FlowValue
	 */
	public static FlowValue flowValueFromWhen(When when) {
		switch (when) {
		case ALWAYS:
			return FlowValue.ALWAYS;
		case MAYBE:
			return FlowValue.UNKNOWN;
		case NEVER:
			return FlowValue.NEVER;
		case UNKNOWN:
			return FlowValue.UNKNOWN;
		default:
			throw new IllegalStateException();
		}
	}

	/**
	 * Determine whether given backwards FlowValue
	 * conflicts with given source.
	 * 
	 * @param backwardsFlowValue a backwards FlowValue
	 * @param source             SourceSinkInfo object representing a source
	 *                           reached by the backwards flow value
	 * @param typeQualifierValue TypeQualifierValue being checked
	 * @return true if backwards value conflicts with source, false if not
	 */
	public static boolean backwardsValueConflictsWithSource(FlowValue backwardsFlowValue, SourceSinkInfo source,
			TypeQualifierValue typeQualifierValue) {
		// Never report a warning for UNKNOWN sources
		if (source.getWhen() == When.UNKNOWN) {
			return false;
		}

		if (typeQualifierValue.isStrictQualifier()) {
			// strict checking
			return (backwardsFlowValue == ALWAYS && source.getWhen() != When.ALWAYS)
				|| (backwardsFlowValue == NEVER && source.getWhen() != When.NEVER);
		} else {
			// NOT strict checking
			return (backwardsFlowValue == ALWAYS && source.getWhen() == When.NEVER)
				|| (backwardsFlowValue == NEVER && source.getWhen() == When.ALWAYS);
		}
	}

}
