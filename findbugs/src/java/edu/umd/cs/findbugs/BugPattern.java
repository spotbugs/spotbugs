/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

public class BugPattern implements Comparable {
	private String type;
	private String abbrev;
	private String category;
	private String shortDescription;
	private String longDescription;
	private String detailText;

	public BugPattern(String type, String abbrev, String category, String shortDescription,
		String longDescription, String detailText) {
		this.type = type;
		this.abbrev = abbrev;
		this.category = category;
		this.shortDescription = shortDescription;
		this.longDescription = longDescription;
		this.detailText = detailText;
	}

	public String getType() { return type; }
	public String getAbbrev() { return abbrev; }
	public String getCategory() { return category; }
	public String getShortDescription() { return shortDescription; }
	public String getLongDescription() { return longDescription; }
	public String getDetailText() { return detailText; }

	public int compareTo(Object o) {
		if (!(o instanceof BugPattern))
			return -1;
		BugPattern other = (BugPattern) o;
		return type.compareTo(other.type);
	}

}

// vim:ts=4
