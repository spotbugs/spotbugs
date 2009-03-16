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

package edu.umd.cs.findbugs.userAnnotations;

import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;

/**
 * Interface to allow persistence of bug user annotations using arbitrary
 * mechanisms.
 */
public interface UserAnnotationPlugin {
	interface Listener {
		void issueUpdate(BugInstance bug);
		void statusUpdated();
	}

	/**
	 * For the given BugCollection, load the user annotations
	 * for each BugInstance in the collection.
	 * 
	 * @param bugs a BugCollection
	 */
	void loadUserAnnotations(BugCollection bugs);
	
	/**
	 * Store the user annotation for the given BugInstance.
	 * 
	 * @param bug a BugInstance
	 */
	void storeUserAnnotation(BugInstance bug);
	
	/**
	 * Store the user annotation for all BugInstances in the
	 * given BugCollection.
	 * 
	 * @param bugs a BugCollection
	 */
	void storeUserAnnotations(BugCollection bugs);

	/**
     * @return
     */
    String getStatusMsg();
    
    public void addListener(Listener listener);
    public void removeListener(Listener listener);
    
    public void shutdown();

}
