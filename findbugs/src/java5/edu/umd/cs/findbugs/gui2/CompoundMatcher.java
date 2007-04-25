/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import java.io.IOException;
import java.util.HashSet;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.xml.XMLOutput;


/**
 * Holds a bunch of matchers, and only matches a bug if all
 * the submatchers match it.  Matchetymatch.
 */
public class CompoundMatcher extends HashSet<Matcher> implements Matcher
{
	private static final long serialVersionUID = -6167545252176658833L;

	public boolean match(BugInstance bugInstance)
	{
		for (Matcher i : this)
			if (!i.match(bugInstance))
				return false;
		return true;
	}

    public void writeXML(XMLOutput xmlOutput) throws IOException {
	    throw new UnsupportedOperationException(); 
    }
}
