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
    
    /**
     * Creates a new instance of BugInstanceGroup.
     * @param groupType string indicating why the bug instances in the group
     *   are related
     * @param groupName name of the group (e.g., the class name if the group
     *   is all bug instances in the same class)
     */
    public BugInstanceGroup(String groupType, String groupName) {
	this.groupType = groupType;
	this.groupName = groupName;
    }
    
    /**
     * Get the group type.
     */
    public String getGroupType() {
	return groupType;
    }
    
    /**
     * Get the group name.
     */
    public String toString() {
	return groupName;
    }
    
}
