/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import java.util.*;

public class BugCodeMatcher implements Matcher {
	private List<String> bugCodeList = new LinkedList<String>();

	public BugCodeMatcher(String bugCodeNames) {
		StringTokenizer tok = new StringTokenizer(bugCodeNames, ",");
		while (tok.hasMoreTokens()) {
			bugCodeList.add(tok.nextToken());
		}
	}

	public boolean match(BugInstance bugInstance) {
		String bugCode = bugInstance.getAbbrev();
		Iterator<String> i = bugCodeList.iterator();
		while (i.hasNext()) {
			if (bugCode.equals(i.next()))
				return true;
		}
		return false;
	}
}

// vim:ts=4
