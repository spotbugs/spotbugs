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

import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;


/**
 * @author pwilliam
 */
 class BugCollectionStorageCloud extends AbstractCloud {

	BugCollectionStorageCloud(BugCollection bc) {
			super(bc);
	}
	
	public Mode getMode() {
	    return Mode.COMMUNAL;
    }

	public String getUser() {
	    // TODO Auto-generated method stub
	    return null;
    }

	public UserDesignation getUserDesignation(BugInstance b) {
	    BugDesignation bd = b.getUserDesignation();
	    if (bd == null) return UserDesignation.UNCLASSIFIED;
	    return UserDesignation.valueOf(bd.getDesignationKey());
    }

	public String getUserEvaluation(BugInstance b) {
    	BugDesignation bd = b.getUserDesignation();
  	    if (bd == null) return "";
  	    return bd.getAnnotationText();
    }

	public long getUserTimestamp(BugInstance b) {
    	BugDesignation bd = b.getUserDesignation();
  	    if (bd == null) return Long.MAX_VALUE;
  	    return bd.getTimestamp();
    }

	public void setMode(Mode m) {
	    // TODO Auto-generated method stub
    }

	public void setUserDesignation(BugInstance b, UserDesignation u, long timestamp) {
    	BugDesignation bd = b.getNonnullUserDesignation();
    	bd.setDesignationKey(u.name());
    	bd.setTimestamp(timestamp);
    }


	 public void setUserEvaluation(BugInstance b, String e, long timestamp) {
       	BugDesignation bd = b.getNonnullUserDesignation();
    	bd.setAnnotationText(e);
    	bd.setTimestamp(timestamp);
    	bd.setUser(getUser());
    }

	public void setUserTimestamp(BugInstance b, long timestamp) {
     	BugDesignation bd = b.getNonnullUserDesignation();
    	bd.setTimestamp(timestamp);
    	bd.setUser(getUser()); 
    }

	public long getFirstSeen(BugInstance b) {
	    long firstVersion = b.getFirstVersion();
	    AppVersion v = bugCollection.getAppVersionFromSequenceNumber(firstVersion);
	    if (v == null)
	    	return bugCollection.getTimestamp();
	    return v.getTimestamp();
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#bugsPopulated()
     */
    public void bugsPopulated() {
	    assert true;
	    
    }

	public boolean availableForInitialization() {
		return true;
	}
    public boolean initialize() {
	    return true;
	    
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#storeUserAnnotation(edu.umd.cs.findbugs.BugInstance)
     */
    public void storeUserAnnotation(BugInstance bugInstance) {
	    // TODO Auto-generated method stub
	    
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#enableBugLink(java.lang.String)
     */
    public boolean bugLinkEnabled(String label) {
	    throw new UnsupportedOperationException();
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#bugFiled(edu.umd.cs.findbugs.BugInstance, java.lang.Object)
     */
    public void bugFiled(BugInstance b, Object bugLink) {
    	 throw new UnsupportedOperationException();
	    
    }



	

}
