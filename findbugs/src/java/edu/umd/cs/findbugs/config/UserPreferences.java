/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 Dave Brosius <dbrosius@qis.net>
 * Copyright (C) 2004,2005 University of Maryland
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;

/**
 * User Preferences outside of any one Project.
 * This consists of a class to manage the findbugs.prop file found in the user.home.
 *
 * @author Dave Brosius
 */
public class UserPreferences implements Cloneable {
	private static final String PREF_FILE_NAME = ".Findbugs_prefs";
	private static final int MAX_RECENT_FILES = 9;
	private static final String DETECTOR_THRESHOLD_KEY = "detector_threshold";
	private static final String FILTER_SETTINGS_KEY = "filter_settings";
	private static final String DEFAULT_DIRECTORY = "default_directory";
	private LinkedList<String> recentProjectsList = new LinkedList<String>();
	private Map<String, Boolean> detectorEnablementMap = new HashMap<String, Boolean>();
	private ProjectFilterSettings filterSettings;
	private static UserPreferences preferencesSingleton = new UserPreferences();

	private UserPreferences() {
		this.filterSettings = ProjectFilterSettings.createDefault();
	}
	
	/**
	 * Create default UserPreferences.
	 * 
	 * @return default UserPreferences
	 */
	public static UserPreferences createDefaultUserPreferences() {
		return new UserPreferences();
	}

	/**
	 * Get UserPreferences singleton.
	 * This should only be used if there is a single set of user
	 * preferences to be used for all projects.
	 * 
	 * @return the UserPreferences
	 */
	public static UserPreferences getUserPreferences() {
		return preferencesSingleton;
	}

	/**
	 * Read persistent global UserPreferences from file in 
	 * the user's home directory.
	 */
	public void read() {
		File prefFile = new File(System.getProperty("user.home"), PREF_FILE_NAME);
		if (!prefFile.exists() || !prefFile.isFile())
			return;
		try {
			read(new FileInputStream(prefFile));
		} catch (IOException e) {
			// Ignore - just use default preferences
		}
	}
	
	/**
	 * Read user preferences from given input stream.
	 * The InputStream is guaranteed to be closed by this method.
	 * 
	 * @param in the InputStream
	 * @throws IOException
	 */
	public void read(InputStream in) throws IOException {
		BufferedInputStream prefStream = null;
		Properties props = new Properties();
		try {
			prefStream = new BufferedInputStream(in);
			props.load(prefStream);
		} finally {
			try {
				if (prefStream != null)
					prefStream.close();
			} catch (IOException ioe) {
				// Ignore
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
				detectorEnablementMap.put(name, Boolean.valueOf(enabled));
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
		
		String dd = (String)props.get(DEFAULT_DIRECTORY);
		if (dd != null)
			System.setProperty("user.dir", dd);

	}

	/**
	 * Write persistent global UserPreferences to file 
	 * in user's home directory.
	 */
	public void write() {
		try {
			File prefFile = new File(System.getProperty("user.home"), PREF_FILE_NAME); 
			write(new FileOutputStream(prefFile));
		} catch (IOException e) {
			// Ignore
		}
	}

	/**
	 * Write UserPreferences to given OutputStream.
	 * The OutputStream is guaranteed to be closed by this method.
	 * 
	 * @param out the OutputStream
	 * @throws IOException
	 */
	public void write(OutputStream out) throws IOException {
		Properties props = new Properties();
		for (int i = 0; i < recentProjectsList.size(); i++) {
			String projectName = recentProjectsList.get(i);
			String key = "recent" + i;
			props.put(key, projectName);
		}

		Iterator<Map.Entry<String, Boolean>> it = detectorEnablementMap.entrySet().iterator();
		int i = 0;
		while (it.hasNext()) {
			Map.Entry<String, Boolean> entry = it.next();
			props.put("detector" + i, entry.getKey() + "|" + String.valueOf(entry.getValue().booleanValue()));
			i++;
		}

		// Save ProjectFilterSettings
		props.put(FILTER_SETTINGS_KEY, filterSettings.toEncodedString());
		
		// Backwards-compatibility: save minimum warning priority as integer.
		// This will allow the properties file to work with older versions
		// of FindBugs.
		props.put(DETECTOR_THRESHOLD_KEY, String.valueOf(filterSettings.getMinPriorityAsInt()));
		
		props.put(DEFAULT_DIRECTORY, System.getProperty("user.dir"));

		OutputStream prefStream = null;
		try {
			prefStream = new BufferedOutputStream(out);
			props.store(prefStream, "FindBugs User Preferences");
			prefStream.flush();
		} finally {
			try {
				if (prefStream != null)
					prefStream.close();
			} catch (IOException ioe) {
			}
		}
	}

	/**
	 * Get List of recent project filenames.
	 * 
	 * @return List of recent project filenames
	 */
	public List<String> getRecentProjects() {
		return recentProjectsList;
	}

	/**
	 * Add given project filename to the front of the recently-used
	 * project list.
	 * 
	 * @param projectName project filename
	 */
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

	/**
	 * Remove project filename from the recently-used project list.
	 * 
	 * @param projectName project filename
	 */
	public void removeProject(String projectName) {
		//It really should always be in slot 0, but...
		for (int i = 0; i < recentProjectsList.size(); i++) {
			if (projectName.equals(recentProjectsList.get(i))) {
				recentProjectsList.remove(i);
				break;
			}
		}
	}
	
	/**
	 * Set the enabled/disabled status of given Detector.
	 * 
	 * @param factory the DetectorFactory for the Detector to be enabled/disabled
	 * @param enable  true if the Detector should be enabled,
	 *                false if it should be Disabled
	 */
	public void enableDetector(DetectorFactory factory, boolean enable) {
		detectorEnablementMap.put(factory.getShortName(), enable ? Boolean.TRUE : Boolean.FALSE);
	}
	
	/**
	 * Get the enabled/disabled status of given Detector.
	 * 
	 * @param factory the DetectorFactory of the Detector
	 * @return true if the Detector is enabled, false if not
	 */
	public boolean isDetectorEnabled(DetectorFactory factory) {
		String detectorName = factory.getShortName();
		Boolean enabled = detectorEnablementMap.get(detectorName);
		if (enabled == null) {
			// No explicit preference has been specified for this detector,
			// so use the default enablement specified by the
			// DetectorFactory.
			enabled = factory.isDefaultEnabled() ? Boolean.TRUE : Boolean.FALSE;
			detectorEnablementMap.put(detectorName, enabled);
		}
		return enabled.booleanValue();
	}

	/**
	 * Enable or disable all known Detectors.
	 * 
	 * @param enable true if all detectors should be enabled,
	 *               false if they should all be disabled
	 */
	public void enableAllDetectors(boolean enable) {
		detectorEnablementMap.clear();
		
		DetectorFactoryCollection factoryCollection = DetectorFactoryCollection.instance();
		for (Iterator<DetectorFactory> i = factoryCollection.factoryIterator(); i.hasNext();) {
			DetectorFactory factory = i.next();
			detectorEnablementMap.put(
					factory.getShortName(), enable ? Boolean.TRUE : Boolean.FALSE);
		}
	}
	
	/**
	 * Set the ProjectFilterSettings.
	 * 
	 * @param filterSettings the ProjectFilterSettings
	 */
	public void setProjectFilterSettings(ProjectFilterSettings filterSettings) {
		this.filterSettings = filterSettings;
	}
	
	/**
	 * Get ProjectFilterSettings.
	 * 
	 * @return the ProjectFilterSettings
	 */
	public ProjectFilterSettings getFilterSettings() {
		return this.filterSettings;
	}

	/**
	 * Get the detector threshold (min severity to report a warning).
	 * 
	 * @return the detector threshold
	 */
	public int getUserDetectorThreshold() {
		return filterSettings.getMinPriorityAsInt();
	}

	/**
	 * Set the detector threshold  (min severity to report a warning). 
	 * 
	 * @param threshold the detector threshold
	 */
	public void setUserDetectorThreshold(int threshold) {
		String minPriority = ProjectFilterSettings.getIntPriorityAsString(threshold);
		filterSettings.setMinPriority(minPriority);
	}
	
	/**
	 * Set the detector threshold  (min severity to report a warning). 
	 * 
	 * @param threshold the detector threshold
	 */
	public void setUserDetectorThreshold(String threshold) {
		filterSettings.setMinPriority(threshold);
	}
	//@Override
	@Override
         public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass())
			return false;
		
		UserPreferences other = (UserPreferences) obj;
		
		return recentProjectsList.equals(other.recentProjectsList)
			&& detectorEnablementMap.equals(other.detectorEnablementMap)
			&& filterSettings.equals(other.filterSettings);
	}
	
	//@Override
	@Override
         public int hashCode() {
		return recentProjectsList.hashCode()
			+ detectorEnablementMap.hashCode()
			+ filterSettings.hashCode();
	}
	
	//@Override
	@Override
         public Object clone() {
		try {
			UserPreferences dup = (UserPreferences) super.clone();
			
			dup.recentProjectsList = new LinkedList<String>();
			dup.recentProjectsList.addAll(this.recentProjectsList);
			
			dup.detectorEnablementMap = new HashMap<String, Boolean>();
			dup.detectorEnablementMap.putAll(this.detectorEnablementMap);
			
			dup.filterSettings = (ProjectFilterSettings) this.filterSettings.clone();
			
			return dup;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}

// vim:ts=4
