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
import java.util.ArrayList;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * A list of all bugs to filter out, only we call it suppressing them
 */
@Deprecated
public class SuppressionMatcher extends ArrayList<BugInstance> implements Matcher
{
	private static final long serialVersionUID = -689204051024507484L;

	public boolean match(BugInstance bugInstance)
	{
		return (!contains(bugInstance));
	}

	@Override
	public boolean add(BugInstance bugInstance)
	{
		if (contains(bugInstance))
			return false;
		return super.add(bugInstance);
	}
	 public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
		    throw new UnsupportedOperationException(); 
	    }
}
