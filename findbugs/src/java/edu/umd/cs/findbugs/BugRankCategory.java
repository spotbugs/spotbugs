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

package edu.umd.cs.findbugs;

public enum BugRankCategory { SCARIEST, SCARY, TROUBLING, OF_CONCERN, UNRANKED;
  static public BugRankCategory getRank(int rank) {
	  if (rank <= 4) return SCARIEST;
	  if (rank <= 9) return SCARY;
	  if (rank <= 14) return TROUBLING;
	  if (rank <= 20) return OF_CONCERN;
	  return UNRANKED;
  }
}