/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 Dave Brosius <dbrosius@qis.net>
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
 * UserPreferences.java
 *
 * Created on May 26, 2004, 11:55 PM
 */

package edu.umd.cs.findbugs;

import java.io.*;
import java.util.*;

/**
 * User Preferences outside of any one Project
 * This consists of a class to manage the findbugs.prop file found in the user.dir
 *   -- initially this file only contains the recent project list
 *   -- perhaps in the future will contain enabled/disabled detectors
 */
 
public class UserPreferences {
	private static final int MAX_RECENT_FILES = 9;
	private LinkedList<String> recentProjectsList = new LinkedList<String>();
	private static UserPreferences preferencesSingleton = new UserPreferences();
	
	private UserPreferences() {
	}
	
	public static UserPreferences getUserPreferences() {
		return preferencesSingleton;
	}
	
	public void read() {
		File prefFile = new File( System.getProperty( "user.home" ), "Findbugs.prefs" );
		if (!prefFile.exists() || !prefFile.isFile())
			return;
		BufferedInputStream prefStream = null;
		Properties props = new Properties();
		try {
			prefStream = new BufferedInputStream(new FileInputStream(prefFile));
			props = new Properties();
			props.load(prefStream);
		} catch (Exception e) {
			//Report? - probably not
		}
		finally {
			try {
				if (prefStream != null)
					prefStream.close();
			}
			catch (IOException ioe) {}
		}
		
		if (props.size() == 0)
			return;
		for (int i = 0; i < MAX_RECENT_FILES; i++) {
			String key = "recent" + i;
			String projectName = (String)props.get(key);
			if (projectName != null)
				recentProjectsList.add(projectName);
		}
	}
	
	public void write() {
		Properties props = new Properties();
		for (int i = 0; i < recentProjectsList.size(); i++) {
			String projectName = recentProjectsList.get(i);
			String key = "recent" + i;
			props.put(key,projectName);
		}
		File prefFile = new File( System.getProperty( "user.home" ), "Findbugs.prefs" );
		BufferedOutputStream prefStream = null;
		try {
			prefStream = new BufferedOutputStream(new FileOutputStream(prefFile));
			props.store(prefStream, "FindBugs User Preferences");
			prefStream.flush();
		} catch (Exception e) {
			//Report? -- probably not
		} finally {
			try {
				if (prefStream != null)
					prefStream.close();
			}
			catch (IOException ioe) {}
		}
	}
	
	public List<String> getRecentProjects(){
		return recentProjectsList;
	}
	
	public void useProject(String projectName) {
		for (int i = 0; i < recentProjectsList.size(); i++) {
			if (projectName.equals(recentProjectsList.get(i))) {
				recentProjectsList.remove(i);
				recentProjectsList.addFirst(projectName);
				return;
			}
		}
		recentProjectsList.addFirst(projectName);
		if (recentProjectsList.size() > MAX_RECENT_FILES)
			recentProjectsList.removeLast();
	}
	
	public void removeProject(String projectName) {
		//It really should always be in slot 0, but...
		for (int i = 0; i < recentProjectsList.size(); i++) {
			if (projectName.equals(recentProjectsList.get(i))) {
				recentProjectsList.remove(i);
				break;
			}
		}	
	}
}

// vim:ts=4
