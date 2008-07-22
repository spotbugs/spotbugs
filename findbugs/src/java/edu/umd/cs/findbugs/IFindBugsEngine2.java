/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2008, University of Maryland
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

import java.io.IOException;
import java.util.Map;

/**
 * Additional capabilities which can be supported by FindBugs engine classes.
 * This is probably not a stable API.
 * 
 * @author David Hovemeyer
 */
public interface IFindBugsEngine2 extends IFindBugsEngine {
	/**
	 * Load given user annotation plugin.
	 * 
	 * @param userAnnotationPluginClassName name of user annotation plugin class
	 * @param configurationProperties       user annotation plugin configuration properties
	 * @throws java.io.IOException
	 */
	public void loadUserAnnotationPlugin(String userAnnotationPluginClassName, Map<String,String> configurationProperties)
		throws IOException;
	
	/**
	 * Set whether or not the user annotation plugin should be used to
	 * load user annotations to be applied to generated analysis results.
	 * 
	 * @param userAnnotationSync true if user annotations should be sync'ed with generated analysis results,
	 *                           false if not
	 */
	public void setUserAnnotationSync(boolean userAnnotationSync);
}
