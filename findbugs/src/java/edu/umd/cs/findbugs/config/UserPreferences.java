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

package edu.umd.cs.findbugs.config;

import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * User Preferences outside of any one Project.
 * This consists of a class to manage the findbugs.prop file found in the user.dir.
 *
 * @author Dave Brosius
 */
public class UserPreferences {
	private static final int MAX_RECENT_FILES = 9;
	private static final String DETECTOR_THRESHOLD_KEY = "detector_threshold";
	private static final String FILTER_SETTINGS_KEY = "filter_settings";
	private LinkedList<String> recentProjectsList = new LinkedList<String>();
	private HashMap<String, Boolean> detectorStateList = new HashMap<String, Boolean>();
	private ProjectFilterSettings filterSettings;
	private static UserPreferences preferencesSingleton = new UserPreferences();

	private UserPreferences() {
		this.filterSettings = ProjectFilterSettings.createDefault();
	}

	public static UserPreferences getUserPreferences() {
		return preferencesSingleton;
	}

	public void read() {
		File prefFile = new File(System.getProperty("user.home"), "Findbugs.prefs");
		if (!prefFile.exists() || !prefFile.isFile())
			return;
		BufferedInputStream prefStream = null;
		Properties props = new Properties();
		try {
			prefStream = new BufferedInputStream(new FileInputStream(prefFile));
			props.load(prefStream);
		} catch (Exception e) {
			//Report? - probably not
		} finally {
			try {
				if (prefStream != null)
					prefStream.close();
			} catch (IOException ioe) {
			}
		}

		if (props.size() == 0)
			return;
		for (int i = 0; i < MAX_RECENT_FILES; i++) {
			String key = "recent" + i;
			String projectName = (String) props.get(key);
			if (projectName != null)
				recentProjectsList.add(projectName);
		}

		int i = 0;
		while (true) {
			String key = "detector" + i;
			String detectorState = (String) props.get(key);
			if (detectorState == null)
				break;
			int pipePos = detectorState.indexOf("|");
			if (pipePos >= 0) {
				String name = detectorState.substring(0, pipePos);
				String enabled = detectorState.substring(pipePos + 1);
				detectorStateList.put(name, Boolean.valueOf(enabled));
			}
			i++;
		}

		if (props.get(FILTER_SETTINGS_KEY) != null) {
			// Properties contain encoded project filter settings.
			filterSettings = ProjectFilterSettings.fromEncodedString(props.getProperty(FILTER_SETTINGS_KEY));
		} else {
			// Properties contain only minimum warning priority threshold (probably).
			// We will honor this threshold, and enable all bug categories.
			String threshold = (String) props.get(DETECTOR_THRESHOLD_KEY);
			if (threshold != null) {
				try {
					int detectorThreshold = Integer.parseInt(threshold);
					setUserDetectorThreshold(detectorThreshold);
				} catch (NumberFormatException nfe) {
					//Ok to ignore
				}
			}
		}

	}

	public void write() {
		Properties props = new Properties();
		for (int i = 0; i < recentProjectsList.size(); i++) {
			String projectName = recentProjectsList.get(i);
			String key = "recent" + i;
			props.put(key, projectName);
		}

		Iterator it = detectorStateList.entrySet().iterator();
		int i = 0;
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			props.put("detector" + i, entry.getKey() + "|" + String.valueOf(((Boolean) entry.getValue()).booleanValue()));
			i++;
		}

		// Save ProjectFilterSettings
		props.put(FILTER_SETTINGS_KEY, filterSettings.toEncodedString());
		
		// Backwards-compatibility: save minimum warning priority as integer.
		// This will allow the properties file to work with older versions
		// of FindBugs.
		props.put(DETECTOR_THRESHOLD_KEY, String.valueOf(filterSettings.getMinPriorityAsInt()));

		File prefFile = new File(System.getProperty("user.home"), "Findbugs.prefs");
		BufferedOutputStream prefStream = null;
		try {
			prefStream = new BufferedOutputStream(new FileOutputStream(prefFile));
			props.store(prefStream, "FindBugs User Preferences");
			prefStream.flush();
		} catch (IOException e) {
			//Report? -- probably not
		} finally {
			try {
				if (prefStream != null)
					prefStream.close();
			} catch (IOException ioe) {
			}
		}
	}

	public List<String> getRecentProjects() {
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

	public void loadUserDetectorPreferences() {
		Iterator<DetectorFactory> i = DetectorFactoryCollection.instance().factoryIterator();
		while (i.hasNext()) {
			DetectorFactory factory = i.next();
			Boolean enabled = detectorStateList.get(factory.getShortName());
			if (enabled != null)
				factory.setEnabled(enabled.booleanValue());
		}
	}

	public void storeUserDetectorPreferences() {
		detectorStateList.clear();
		Iterator<DetectorFactory> i = DetectorFactoryCollection.instance().factoryIterator();
		while (i.hasNext()) {
			DetectorFactory factory = i.next();
			detectorStateList.put(factory.getShortName(), Boolean.valueOf(factory.isEnabled()));
		}
	}
	
	public ProjectFilterSettings getFilterSettings() {
		return this.filterSettings;
	}

	public int getUserDetectorThreshold() {
		return filterSettings.getMinPriorityAsInt();
	}

	public void setUserDetectorThreshold(int threshold) {
		String minPriority = ProjectFilterSettings.getIntPriorityAsString(threshold);
		filterSettings.setMinPriority(minPriority);
	}
}

// vim:ts=4
