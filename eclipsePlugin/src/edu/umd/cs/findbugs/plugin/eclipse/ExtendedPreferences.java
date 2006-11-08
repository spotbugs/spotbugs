/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2006 University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */
package edu.umd.cs.findbugs.plugin.eclipse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.config.AnalysisFeatureSetting;

/**
 * Extended preferences on top of the default FindBugs UserPreferences to hold
 * some additional values not present in the default implementation. This class
 * may not be needed in the future if the UserPreferences class is extended
 * instead.
 * 
 * @see edu.umd.cs.findbugs.config.UserPreferences
 * 
 * @author Peter Hendriks
 */
public class ExtendedPreferences implements Cloneable {

	public static final String EFFORT_MIN = "min";

	public static final String EFFORT_DEFAULT = "default";

	public static final String EFFORT_MAX = "max";

	private static final String EFFORT_KEY = "effort";

	private static final String INCLUDE_FILTER_KEY = "includefilter";

	private static final String EXCLUDE_FILTER_KEY = "excludefilter";

	private String effort = EFFORT_DEFAULT;

	private String[] includeFilterFiles = new String[0];

	private String[] excludeFilterFiles = new String[0];

	public String getEffort() {
		return effort;
	}

	public void setEffort(String effort) {
		if (!EFFORT_MIN.equals(effort) && !EFFORT_DEFAULT.equals(effort)
				&& !EFFORT_MAX.equals(effort)) {
			throw new IllegalArgumentException("Effort \"" + effort
					+ "\" is not a valid effort value.");
		}
		this.effort = effort;

	}

	public String[] getIncludeFilterFiles() {
		return includeFilterFiles;
	}

	public void setIncludeFilterFiles(String[] includeFilterFiles) {
		if (includeFilterFiles == null) {
			throw new IllegalArgumentException(
					"includeFilterFiles may not be null.");
		}
		this.includeFilterFiles = includeFilterFiles;
	}

	public void setExcludeFilterFiles(String[] excludeFilterFiles) {
		if (excludeFilterFiles == null) {
			throw new IllegalArgumentException(
					"excludeFilterFiles may not be null.");
		}
		this.excludeFilterFiles = excludeFilterFiles;
	}

	public String[] getExcludeFilterFiles() {
		return excludeFilterFiles;
	}

	/**
	 * Reads the extended preferences from a FindBugs UserPreferences file.
	 * 
	 * @param file
	 *            The file to read. This file must exist.
	 * 
	 * @throws IOException
	 *             If a read/write error occurs.
	 */
	public void read(File file) throws IOException {
		if (file == null) {
			throw new IllegalArgumentException("File may not be null");
		}
		if (!file.exists() || !file.isFile()) {
			throw new IllegalArgumentException("File \""
					+ file.getAbsolutePath()
					+ "\" should exist and must be a file.");
		}
		Properties props = new Properties();
		FileInputStream inputStream = new FileInputStream(file);
		try {
			props.load(inputStream);
		} finally {
			inputStream.close();
		}
		effort = props.getProperty(EFFORT_KEY, EFFORT_DEFAULT);
		includeFilterFiles = readFilters(props, INCLUDE_FILTER_KEY);
		excludeFilterFiles = readFilters(props, EXCLUDE_FILTER_KEY);
	}

	/**
	 * Helper method to read array of strings out of the properties file, using
	 * a Findbugs style format.
	 * 
	 * @param props
	 *            The properties file to read the array from.
	 * @param keyPrefix
	 *            The key prefix of the array.
	 * @return The array of Strings, or an empty array if no values exist.
	 */
	private String[] readFilters(Properties props, String keyPrefix) {
		List filters = new ArrayList();
		int counter = 0;
		boolean keyFound = true;
		while (keyFound) {
			String property = props.getProperty(keyPrefix + counter);
			if (property != null) {
				filters.add(property);
				counter++;
			} else {
				keyFound = false;
			}
		}

		return (String[]) filters.toArray(new String[filters.size()]);
	}

	/**
	 * Helper method to write array of strings out of the properties file, using
	 * a Findbugs style format.
	 * 
	 * @param props
	 *            The properties file to write the array to.
	 * @param keyPrefix
	 *            The key prefix of the array.
	 * @param filters
	 *            The filters array to write to the properties.
	 */
	private void writeFilters(Properties props, String keyPrefix,
			String[] filters) {
		int counter = 0;
		for (; counter < filters.length; counter++) {
			props.setProperty(keyPrefix + counter, filters[counter]);
		}
		// remove obsolete keys from the properties file
		boolean keyFound = true;
		while (keyFound) {
			String key = keyPrefix + counter;
			String property = props.getProperty(key);
			if (property == null) {
				keyFound = false;
			} else {
				props.remove(key);
			}
		}
	}

	/**
	 * Write the extended preferences to an existing FindBugs preferences file.
	 * 
	 * @param file
	 *            The FindBugs Preferences file. Should already exist.
	 * 
	 * @throws IOException
	 *             If a read/write error occurs.
	 */
	public void write(File file) throws IOException {
		if (file == null) {
			throw new IllegalArgumentException("File may not be null");
		}
		if (!file.exists() || !file.isFile()) {
			throw new IllegalArgumentException("File \""
					+ file.getAbsolutePath()
					+ "\" should exist and must be a file.");
		}
		Properties props = new Properties();
		FileInputStream inputStream = new FileInputStream(file);
		try {
			props.load(inputStream);
			props.setProperty(EFFORT_KEY, effort);
			writeFilters(props, INCLUDE_FILTER_KEY, includeFilterFiles);
			writeFilters(props, EXCLUDE_FILTER_KEY, excludeFilterFiles);
			FileOutputStream outputStream = new FileOutputStream(file);
			try {
				props.store(outputStream, "FindBugs User Preferences");
			} finally {
				outputStream.close();
			}
		} finally {
			inputStream.close();
		}
		effort = props.getProperty(EFFORT_KEY, EFFORT_DEFAULT);
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof ExtendedPreferences) {
			ExtendedPreferences other = (ExtendedPreferences) obj;
			return effort.equals(other.effort)
					&& Arrays.equals(includeFilterFiles,
							other.includeFilterFiles)
					&& Arrays.equals(excludeFilterFiles,
							other.excludeFilterFiles);
		}
		return false;
	}

	/**
	 * Returns the effort level as an array of feature settings as expected by
	 * FindBugs.
	 * 
	 * @return The array of feature settings corresponding to the current effort
	 *         setting.
	 */
	public AnalysisFeatureSetting[] getAnalysisFeatureSettings() {
		if (effort.equals(EFFORT_DEFAULT)) {
			return FindBugs.DEFAULT_EFFORT;
		} else if (effort.equals(EFFORT_MIN)) {
			return FindBugs.MIN_EFFORT;
		}
		return FindBugs.MAX_EFFORT;
	}
}
