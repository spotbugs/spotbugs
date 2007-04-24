/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
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

package edu.umd.cs.findbugs.filter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class CompoundMatcher implements Matcher {
	private List<Matcher> childList = new LinkedList<Matcher>();

	@Override
	public int hashCode() {
		int result = this.getClass().hashCode();
		for(Matcher m : childList) 
			result += m.hashCode();
		return result;
	}
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (o.getClass() != this.getClass()) return false;
		CompoundMatcher m = (CompoundMatcher) o;
		return childList.equals(m.childList);
	}
	public int numberChildren() {
		return childList.size();
	}
	public void addChild(Matcher child) {
		childList.add(child);
	}

	public Iterator<Matcher> childIterator() {
		return childList.iterator();
	}
}

// vim:ts=4
