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
import java.util.concurrent.CopyOnWriteArraySet;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;

/**
 * @author pwilliam
 */
public abstract class AbstractCloud implements Cloud {

	final BugCollection bugCollection;
	
	AbstractCloud(BugCollection bugs) {
		this.bugCollection = bugs;
	}
	
	public BugCollection getBugCollection() {
		return bugCollection;
	}
	
	public boolean supportsBugLinks() {
		return false;
	}

	public boolean supportsClaims() {
		return false;
	}

	public boolean supportsCloudReports() {
		return false;
	}

	public boolean supportsSourceLinks() {
		return false;
	}

	public String claimedBy(BugInstance b) {
		throw new UnsupportedOperationException();
	}
	
	public boolean claim(BugInstance b) {
		throw new UnsupportedOperationException();
	}

	public URL getBugLink(BugInstance b) {
		throw new UnsupportedOperationException();
	}
	public BugFilingStatus getBugLinkStatus(BugInstance b) {
		throw new UnsupportedOperationException();
	}
	public String getCloudReport(BugInstance b) {
		throw new UnsupportedOperationException();
	}

	public URL getSourceLink(BugInstance b) {
		throw new UnsupportedOperationException();
	}

    public String getSourceLinkToolTip(BugInstance b) {
    	throw new UnsupportedOperationException();
    }
	public Date getUserDate(BugInstance b) {
		return new Date(getUserTimestamp(b));
	}

	public boolean hasExistingBugLink(BugInstance b) {
		throw new UnsupportedOperationException();
	}

	CopyOnWriteArraySet<CloudListener> listeners = new CopyOnWriteArraySet<CloudListener>();

	public void addListener(CloudListener listener) {
		listeners.add(listener);
	}

	public void removeListener(CloudListener listener) {
		listeners.remove(listener);
	}

	protected void updatedStatus() {
		for (CloudListener listener : listeners)
			listener.statusUpdated();
	}

	protected void updatedIssue(BugInstance bug) {
		for (CloudListener listener : listeners)
			listener.issueUpdate(bug);
	}
	
    public String getStatusMsg() {
	   return "";
    }

    public void shutdown() {
	    
    }
    public void printCloudReport(Iterable<BugInstance> bugs, PrintWriter w) {
    	w.println("No cloud report available");
    	return;
    }

    public boolean getIWillFix(BugInstance b) {
    	return getUserDesignation(b) == UserDesignation.I_WILL_FIX;
    }
}
