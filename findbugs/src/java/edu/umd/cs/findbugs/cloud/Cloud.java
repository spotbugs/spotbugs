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

import java.io.PrintWriter;
import java.net.URL;
import java.util.Date;
import java.util.SortedSet;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;

/**
 * An interface for describing how a bug collection
 * interacts with the FindBugs Cloud.
 * 
 * Each Cloud instance is associated with a BugCollection.
 */
public interface Cloud {
	
	enum Mode { COMMUNAL {
	    @Override
	    BugDesignation getPrimaryDesignation(String user, SortedSet<BugDesignation> designations) {
		    if (designations.isEmpty()) 
		    	return null;
		    return designations.first();
	    }
    }, VOTING {
	    @Override
	    BugDesignation getPrimaryDesignation(String user, SortedSet<BugDesignation> designations) {
		   for(BugDesignation bd : designations) {
			   if (user.equals(bd.getUser())) {
				   // has voted
				   return designations.first();
			   }
		   }
		   return null;
	    }
    }, SECRET {
	    @Override
	    BugDesignation getPrimaryDesignation(String user, SortedSet<BugDesignation> designations) {
	    	 for(BugDesignation bd : designations) {
	    		 if (user.equals(bd.getUser())) {
					  return bd;
				   }
			   }
			   return null;
	    }
    };
	abstract @CheckForNull BugDesignation getPrimaryDesignation(String user, SortedSet<BugDesignation> designations);
	};
	
    public static enum BugFilingStatus {
		FILE_BUG("File bug") {
			@Override
			public boolean bugIsFiled() {
				return false;
			}
		}, FILE_AGAIN("File again"), 
		BUG_PENDING("Bug pending") {
			@Override
			public boolean linkEnabled() {
				return false;
			}
		},
		VIEW_BUG("View bug"), NA("") {
			@Override
			public boolean linkEnabled() {
				return false;
			}
			@Override
			public boolean bugIsFiled() {
				return false;
			}
		};

		final String displayName;

		public boolean bugIsFiled() {
			return true;
		}

		public boolean linkEnabled() {
			return true;
		}
		BugFilingStatus(String name) {
			this.displayName = name;
		}
		
		@Override
        public String toString() {
			return displayName;
		}

	}

	BugCollection getBugCollection();
    String getStatusMsg();
    
    public void addListener(CloudListener listener);
    public void removeListener(CloudListener listener);
    
    public boolean initialize();
    
    public void bugsPopulated();
    public void shutdown();
    
	Mode getMode();
	void setMode(Mode m);
	
	boolean getIWillFix(BugInstance b);
	
	boolean supportsSourceLinks();
	
	String getUser();
	
	String getSourceLinkToolTip(@CheckForNull BugInstance b);
	URL getSourceLink(BugInstance b);
	
	boolean supportsBugLinks();
	BugFilingStatus getBugLinkStatus(BugInstance b);
	URL getBugLink(BugInstance b);
	void bugFiled(BugInstance b, @CheckForNull Object bugLink);
	
	boolean supportsCloudReports();
	String getCloudReport(BugInstance b);
	
	boolean supportsClaims();
	String claimedBy(BugInstance b);	
	boolean claim(BugInstance b);
	
	long getUserTimestamp(BugInstance b);
	void setUserTimestamp(BugInstance b, long timestamp);
	Date getUserDate(BugInstance b);
	
	UserDesignation getUserDesignation(BugInstance b);
	void setUserDesignation(BugInstance b, UserDesignation u, long timestamp);
	
	String getUserEvaluation(BugInstance b);
	void setUserEvaluation(BugInstance b, String e, long timestamp);
	
	long getFirstSeen(BugInstance b);
	boolean overallClassificationIsNotAProblem(BugInstance b);
	/**
     * @param bugInstance
     */
    void storeUserAnnotation(BugInstance bugInstance);	
    
    public void printCloudReport(Iterable<BugInstance> bugs, PrintWriter w);
}
