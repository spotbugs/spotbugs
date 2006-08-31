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

/*
 * BugInstanceGroup.java
 *
 * Created on April 6, 2003, 11:31 AM
 */

package edu.umd.cs.findbugs.gui;

/**
 * A BugInstanceGroup represents a node in the bug tree which groups
 * related bug instances.  For example, it might group all of the bug instances
 * for the same class.
 *
 * @author David Hovemeyer
 */
public class BugInstanceGroup {

	private String groupType;
	private String groupName;
	private int memberCount;

	/**
	 * Creates a new instance of BugInstanceGroup.
	 *
	 * @param groupType string indicating why the bug instances in the group
	 *                  are related
	 * @param groupName name of the group (e.g., the class name if the group
	 *                  is all bug instances in the same class)
	 */
	public BugInstanceGroup(String groupType, String groupName) {
		this.groupType = groupType;
		this.groupName = groupName;
		this.memberCount = 0;
	}

	/**
	 * Get the group type.
	 */
	public String getGroupType() {
		return groupType;
	}

	/**
	 * Increment the member count (number of bug instances in this group).
	 */
	public void incrementMemberCount() {
		++memberCount;
	}

	/**
	 * Get the member count (number of bug instances in this group).
	 */
	public int getMemberCount() {
		return memberCount;
	}

	/**
	 * Convert to string.
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(groupName);
		buf.append(" (");
		buf.append(memberCount);
		buf.append(")");
		return buf.toString();
	}

}
