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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;

import edu.umd.cs.findbugs.xml.XMLOutput;

public abstract class CompoundMatcher implements Matcher {
	protected LinkedHashSet<Matcher> children = new LinkedHashSet<Matcher>();

	@Override
	public int hashCode() {
		int result = this.getClass().hashCode();
		for(Matcher m : children) 
			result += m.hashCode();
		return result;
	}
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (o.getClass() != this.getClass()) return false;
		CompoundMatcher m = (CompoundMatcher) o;
		return children.equals(m.children);
	}
	protected int numberChildren() {
		return children.size();
	}
	public void addChild(Matcher child) {
		children.add(child);
	}
	protected void removeChild(Matcher child) {
		children.remove(child);
	}
	protected void clear() {
		children.clear();
	}

	public Collection<Matcher> getChildren() {
		return Collections.unmodifiableCollection(children);
	}
	public Iterator<Matcher> childIterator() {
		return children.iterator();
	}
	public void writeChildrenXML(XMLOutput xmlOutput)  throws IOException {
		for(Matcher m : children) 
			m.writeXML(xmlOutput, false);
	}
	
	@Override
	public String toString() {
		if (children.isEmpty()) return "";
		StringBuffer buf = new StringBuffer();
		for(Matcher m : children) 
			buf.append(m).append(" ");
		buf.setLength(buf.length()-1);
		return buf.toString();
	}
}

// vim:ts=4
