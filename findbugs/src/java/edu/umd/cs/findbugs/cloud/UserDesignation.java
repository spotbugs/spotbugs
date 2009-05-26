/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.cloud;

/**
 * @author pwilliam
 */
public enum UserDesignation {
	UNCLASSIFIED,
	NEEDS_STUDY,
	BAD_ANALYSIS,
	NOT_A_BUG,
	MOSTLY_HARMLESS,
	SHOULD_FIX,
	MUST_FIX,
	I_WILL_FIX,
	OBSOLETE_CODE;

	
	public int score() {
		switch (this) {

		case BAD_ANALYSIS:
			return -3;
		case NOT_A_BUG:
		case OBSOLETE_CODE:
			return  -2;
		case MOSTLY_HARMLESS:
			return -1;
		case SHOULD_FIX:
			return  1;
		case MUST_FIX:
		case I_WILL_FIX:
			return 2;	
		default:
			return 0;
		}
	}
}
