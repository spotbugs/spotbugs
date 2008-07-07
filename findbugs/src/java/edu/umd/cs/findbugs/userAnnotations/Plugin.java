/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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
public interface Plugin {

	/**
	 * Get Set of property names defining properties required
	 * for the plugin to persist the user annotations.
	 */
	Set<String> getPropertyNames();
	
	/**
	 * Set the key/value pairs defining properties required
	 * for the plugin to persist the user annotations.
	 * 
	 * @param properties set of key/value pairs defining properties required by the plugin
	 * @return FIXME: what does this mean?
	 */
	boolean setProperties(Map<String,String> properties);

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

}
