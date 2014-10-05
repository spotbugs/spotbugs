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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.WillClose;

import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.IFindBugsEngine;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.util.Util;

/**
 * User Preferences outside of any one Project. This consists of a class to
 * manage the findbugs.prop file found in the user.home.
 *
 * @author Dave Brosius
 */
public class UserPreferences implements Cloneable {

    /**
     * Separator string for values composed from a string and boolean
     */
    private static final char BOOL_SEPARATOR = '|';

    public static final String EFFORT_MIN = "min";

    public static final String EFFORT_DEFAULT = "default";

    public static final String EFFORT_MAX = "max";

    /**
     * Key prefix for custom plugins, full key consists of a prefix + plugin index starting with 0
     */
    public static final String KEY_PLUGIN = "plugin";

    // Private constants

    private static final String PREF_FILE_NAME = ".Findbugs_prefs";

    private static final int MAX_RECENT_FILES = 9;

    private static final String CLOUD_ID_KEY = "cloud_id";

    private static final String DETECTOR_THRESHOLD_KEY = "detector_threshold";

    private static final String FILTER_SETTINGS_KEY = "filter_settings";

    private static final String FILTER_SETTINGS2_KEY = "filter_settings_neg";

    private static final String RUN_AT_FULL_BUILD = "run_at_full_build";

    private static final String EFFORT_KEY = "effort";

    /**
     * Key prefix for custom filters, full key consists of a prefix + filter index starting with 0
     */
    public static final String KEY_INCLUDE_FILTER = "includefilter";

    /**
     * Key prefix for custom filters, full key consists of a prefix + filter index starting with 0
     */
    public static final String KEY_EXCLUDE_FILTER = "excludefilter";

    /**
     * Key prefix for custom filters, full key consists of a prefix + filter index starting with 0
     */
    public static final String KEY_EXCLUDE_BUGS = "excludebugs";

    // Fields

    private LinkedList<String> recentProjectsList;

    private Map<String, Boolean> detectorEnablementMap;

    private ProjectFilterSettings filterSettings;

    private boolean runAtFullBuild;

    private String effort;

    private String cloudId;

    private Map<String, Boolean> includeFilterFiles;

    private Map<String, Boolean> excludeFilterFiles;

    private Map<String, Boolean> excludeBugsFiles;

    private Map<String, Boolean> customPlugins;

    private UserPreferences() {
        filterSettings = ProjectFilterSettings.createDefault();
        recentProjectsList = new LinkedList<String>();
        detectorEnablementMap = new HashMap<String, Boolean>();
        runAtFullBuild = true;
        effort = EFFORT_DEFAULT;
        includeFilterFiles = new TreeMap<String, Boolean>();
        excludeFilterFiles = new TreeMap<String, Boolean>();
        excludeBugsFiles = new TreeMap<String, Boolean>();
        customPlugins = new TreeMap<String, Boolean>();
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
     * Read persistent global UserPreferences from file in the user's home
     * directory.
     */
    public void read() {
        File prefFile = new File(SystemProperties.getProperty("user.home"), PREF_FILE_NAME);
        if (!prefFile.exists() || !prefFile.isFile()) {
            return;
        }
        try {
            read(new FileInputStream(prefFile));
        } catch (IOException e) {
            // Ignore - just use default preferences
        }
    }

    /**
     * Read user preferences from given input stream. The InputStream is
     * guaranteed to be closed by this method.
     *
     * @param in
     *            the InputStream
     * @throws IOException
     */
    public void read(@WillClose InputStream in) throws IOException {
        BufferedInputStream prefStream = null;
        Properties props = new Properties();
        try {
            prefStream = new BufferedInputStream(in);
            props.load(prefStream);
        } finally {
            try {
                if (prefStream != null) {
                    prefStream.close();
                }
            } catch (IOException ioe) {
                // Ignore
            }
        }

        if (props.size() == 0) {
            return;
        }
        for (int i = 0; i < MAX_RECENT_FILES; i++) {
            String key = "recent" + i;
            String projectName = (String) props.get(key);
            if (projectName != null) {
                recentProjectsList.add(projectName);
            }
        }

        for (Map.Entry<?, ?> e : props.entrySet()) {

            String key = (String) e.getKey();
            if (!key.startsWith("detector") || key.startsWith("detector_")) {
                // it is not a detector enablement property
                continue;
            }
            String detectorState = (String) e.getValue();
            int pipePos = detectorState.indexOf(BOOL_SEPARATOR);
            if (pipePos >= 0) {
                String name = detectorState.substring(0, pipePos);
                String enabled = detectorState.substring(pipePos + 1);
                detectorEnablementMap.put(name, Boolean.valueOf(enabled));
            }
        }

        if (props.get(FILTER_SETTINGS_KEY) != null) {
            // Properties contain encoded project filter settings.
            filterSettings = ProjectFilterSettings.fromEncodedString(props.getProperty(FILTER_SETTINGS_KEY));
        } else {
            // Properties contain only minimum warning priority threshold
            // (probably).
            // We will honor this threshold, and enable all bug categories.
            String threshold = (String) props.get(DETECTOR_THRESHOLD_KEY);
            if (threshold != null) {
                try {
                    int detectorThreshold = Integer.parseInt(threshold);
                    setUserDetectorThreshold(detectorThreshold);
                } catch (NumberFormatException nfe) {
                    // Ok to ignore
                }
            }
        }
        if (props.get(FILTER_SETTINGS2_KEY) != null) {
            // populate the hidden bug categories in the project filter settings
            ProjectFilterSettings.hiddenFromEncodedString(filterSettings, props.getProperty(FILTER_SETTINGS2_KEY));
        }
        if (props.get(RUN_AT_FULL_BUILD) != null) {
            runAtFullBuild = Boolean.parseBoolean(props.getProperty(RUN_AT_FULL_BUILD));
        }
        effort = props.getProperty(EFFORT_KEY, EFFORT_DEFAULT);
        cloudId = props.getProperty(CLOUD_ID_KEY);
        includeFilterFiles = readProperties(props, KEY_INCLUDE_FILTER);
        excludeFilterFiles = readProperties(props, KEY_EXCLUDE_FILTER);
        excludeBugsFiles = readProperties(props, KEY_EXCLUDE_BUGS);
        customPlugins = readProperties(props, KEY_PLUGIN);
    }

    /**
     * Write persistent global UserPreferences to file in user's home directory.
     */
    public void write() {
        try {
            File prefFile = new File(SystemProperties.getProperty("user.home"), PREF_FILE_NAME);
            write(new FileOutputStream(prefFile));
        } catch (IOException e) {
            if (FindBugs.DEBUG) {
                e.printStackTrace(); // Ignore
            }
        }
    }

    /**
     * Write UserPreferences to given OutputStream. The OutputStream is
     * guaranteed to be closed by this method.
     *
     * @param out
     *            the OutputStream
     * @throws IOException
     */
    public void write(@WillClose OutputStream out) throws IOException {

        Properties props = new SortedProperties();

        for (int i = 0; i < recentProjectsList.size(); i++) {
            String projectName = recentProjectsList.get(i);
            String key = "recent" + i;
            props.put(key, projectName);
        }

        Iterator<Entry<String, Boolean>> it = detectorEnablementMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Boolean> entry = it.next();
            props.put("detector" + entry.getKey(), entry.getKey() + BOOL_SEPARATOR + String.valueOf(entry.getValue().booleanValue()));
        }

        // Save ProjectFilterSettings
        props.put(FILTER_SETTINGS_KEY, filterSettings.toEncodedString());
        props.put(FILTER_SETTINGS2_KEY, filterSettings.hiddenToEncodedString());

        // Backwards-compatibility: save minimum warning priority as integer.
        // This will allow the properties file to work with older versions
        // of FindBugs.
        props.put(DETECTOR_THRESHOLD_KEY, String.valueOf(filterSettings.getMinPriorityAsInt()));
        props.put(RUN_AT_FULL_BUILD, String.valueOf(runAtFullBuild));
        props.setProperty(EFFORT_KEY, effort);
        if (cloudId != null) {
            props.setProperty(CLOUD_ID_KEY, cloudId);
        }
        writeProperties(props, KEY_INCLUDE_FILTER, includeFilterFiles);
        writeProperties(props, KEY_EXCLUDE_FILTER, excludeFilterFiles);
        writeProperties(props, KEY_EXCLUDE_BUGS, excludeBugsFiles);
        writeProperties(props, KEY_PLUGIN, customPlugins);

        OutputStream prefStream = null;
        try {
            prefStream = new BufferedOutputStream(out);
            props.store(prefStream, "FindBugs User Preferences");
            prefStream.flush();
        } finally {
            try {
                if (prefStream != null) {
                    prefStream.close();
                }
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
     * Add given project filename to the front of the recently-used project
     * list.
     *
     * @param projectName
     *            project filename
     */
    public void useProject(String projectName) {
        removeProject(projectName);
        recentProjectsList.addFirst(projectName);
        while (recentProjectsList.size() > MAX_RECENT_FILES) {
            recentProjectsList.removeLast();
        }
    }

    /**
     * Remove project filename from the recently-used project list.
     *
     * @param projectName
     *            project filename
     */
    public void removeProject(String projectName) {
        // It should only be in list once (usually in slot 0) but check entire
        // list...
        Iterator<String> it = recentProjectsList.iterator();
        while (it.hasNext()) {
            // LinkedList, so remove() via iterator is faster than
            // remove(index).
            if (projectName.equals(it.next())) {
                it.remove();
            }
        }
    }

    /**
     * Set the enabled/disabled status of given Detector.
     *
     * @param factory
     *            the DetectorFactory for the Detector to be enabled/disabled
     * @param enable
     *            true if the Detector should be enabled, false if it should be
     *            Disabled
     */
    public void enableDetector(DetectorFactory factory, boolean enable) {
        detectorEnablementMap.put(factory.getShortName(), enable);
    }

    /**
     * Get the enabled/disabled status of given Detector.
     *
     * @param factory
     *            the DetectorFactory of the Detector
     * @return true if the Detector is enabled, false if not
     */
    public boolean isDetectorEnabled(DetectorFactory factory) {
        String detectorName = factory.getShortName();
        Boolean enabled = detectorEnablementMap.get(detectorName);
        if (enabled == null) {
            // No explicit preference has been specified for this detector,
            // so use the default enablement specified by the
            // DetectorFactory.
            enabled = factory.isDefaultEnabled();
            detectorEnablementMap.put(detectorName, enabled);
        }
        return enabled;
    }

    /**
     * Enable or disable all known Detectors.
     *
     * @param enable
     *            true if all detectors should be enabled, false if they should
     *            all be disabled
     */
    public void enableAllDetectors(boolean enable) {
        detectorEnablementMap.clear();

        Collection<Plugin> allPlugins = Plugin.getAllPlugins();
        for (Plugin plugin : allPlugins) {
            for (DetectorFactory factory : plugin.getDetectorFactories()) {
                detectorEnablementMap.put(factory.getShortName(), enable);
            }
        }
    }

    /**
     * Set the ProjectFilterSettings.
     *
     * @param filterSettings
     *            the ProjectFilterSettings
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
     * Set the detector threshold (min severity to report a warning).
     *
     * @param threshold
     *            the detector threshold
     */
    public void setUserDetectorThreshold(int threshold) {
        String minPriority = ProjectFilterSettings.getIntPriorityAsString(threshold);
        filterSettings.setMinPriority(minPriority);
    }

    /**
     * Set the enabled/disabled status of running findbugs automatically for
     * full builds.
     *
     * @param enable
     *            true if running FindBugs at full builds should be enabled,
     *            false if it should be Disabled
     */
    public void setRunAtFullBuild(boolean enable) {
        this.runAtFullBuild = enable;
    }

    /**
     * Get the enabled/disabled status of runAtFullBuild
     *
     * @return true if the running for full builds is enabled, false if not
     */
    public boolean isRunAtFullBuild() {
        return runAtFullBuild;
    }

    /**
     * Set the detector threshold (min severity to report a warning).
     *
     * @param threshold
     *            the detector threshold
     */
    public void setUserDetectorThreshold(String threshold) {
        filterSettings.setMinPriority(threshold);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        UserPreferences other = (UserPreferences) obj;

        return runAtFullBuild == other.runAtFullBuild && recentProjectsList.equals(other.recentProjectsList)
                && detectorEnablementMap.equals(other.detectorEnablementMap) && filterSettings.equals(other.filterSettings)
                && effort.equals(other.effort) && includeFilterFiles.equals(other.includeFilterFiles)
                && excludeFilterFiles.equals(other.excludeFilterFiles) && excludeBugsFiles.equals(other.excludeBugsFiles)
                && customPlugins.equals(other.customPlugins)
                && Util.nullSafeEquals(cloudId, other.cloudId);
    }

    @Override
    public int hashCode() {
        return recentProjectsList.hashCode() + detectorEnablementMap.hashCode() + filterSettings.hashCode() + effort.hashCode()
                + includeFilterFiles.hashCode() + excludeFilterFiles.hashCode() + (runAtFullBuild ? 1 : 0)
                + Util.nullSafeHashcode(cloudId);
    }

    @Override
    public UserPreferences clone() {
        try {
            UserPreferences dup = (UserPreferences) super.clone();
            // Deep copy
            dup.recentProjectsList = new LinkedList<String>(recentProjectsList);
            dup.detectorEnablementMap = new HashMap<String, Boolean>(detectorEnablementMap);
            dup.filterSettings = (ProjectFilterSettings) this.filterSettings.clone();
            dup.includeFilterFiles = new TreeMap<String, Boolean>(includeFilterFiles);
            dup.excludeFilterFiles = new TreeMap<String, Boolean>(excludeFilterFiles);
            dup.excludeBugsFiles = new TreeMap<String, Boolean>(excludeBugsFiles);
            dup.customPlugins = new TreeMap<String, Boolean>(customPlugins);
            return dup;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public String getEffort() {
        return effort;
    }

    public void setEffort(String effort) {
        if (!EFFORT_MIN.equals(effort) && !EFFORT_DEFAULT.equals(effort) && !EFFORT_MAX.equals(effort)) {
            throw new IllegalArgumentException("Effort \"" + effort + "\" is not a valid effort value.");
        }
        this.effort = effort;

    }

    /**
     * @return Returns the cloudId.
     */
    public String getCloudId() {
        return cloudId;
    }

    /**
     * @param cloudId The cloudId to set.
     */
    public void setCloudId(String cloudId) {
        this.cloudId = cloudId;
    }

    public Map<String, Boolean> getIncludeFilterFiles() {
        return includeFilterFiles;
    }

    public void setIncludeFilterFiles(Map<String, Boolean> includeFilterFiles) {
        if (includeFilterFiles == null) {
            throw new IllegalArgumentException("includeFilterFiles may not be null.");
        }
        this.includeFilterFiles = includeFilterFiles;
    }

    public Map<String, Boolean> getExcludeBugsFiles() {
        return excludeBugsFiles;
    }

    public void setExcludeBugsFiles(Map<String, Boolean> excludeBugsFiles) {
        if (excludeBugsFiles == null) {
            throw new IllegalArgumentException("excludeBugsFiles may not be null.");
        }
        this.excludeBugsFiles = excludeBugsFiles;
    }

    public void setExcludeFilterFiles(Map<String, Boolean> excludeFilterFiles) {
        if (excludeFilterFiles == null) {
            throw new IllegalArgumentException("excludeFilterFiles may not be null.");
        }
        this.excludeFilterFiles = excludeFilterFiles;
    }

    public Map<String, Boolean> getExcludeFilterFiles() {
        return excludeFilterFiles;
    }

    /**
     * Additional plugins which could be used by {@link IFindBugsEngine} (if
     * enabled), or which shouldn't be used (if disabled). If a plugin is not
     * included in the set, it's enablement depends on it's default settings.
     *
     * @param customPlugins
     *            map with additional third party plugin locations (as absolute
     *            paths), never null, but might be empty
     * @see Plugin#isCorePlugin()
     * @see Plugin#isGloballyEnabled()
     */
    public void setCustomPlugins(Map<String, Boolean> customPlugins) {
        if (customPlugins == null) {
            throw new IllegalArgumentException("customPlugins may not be null.");
        }
        this.customPlugins = customPlugins;
    }

    /**
     * Additional plugins which could be used by {@link IFindBugsEngine} (if
     * enabled), or which shouldn't be used (if disabled). If a plugin is not
     * included in the set, it's enablement depends on it's default settings.
     *
     * @return map with additional third party plugins, might be empty, never
     *         null. The keys are either absolute plugin paths or plugin id's.
     *         <b>Special case</b>: if the path consists of one path segment
     *         then it represents the plugin id for a plugin to be
     *         <b>disabled</b>. A value of a particular key can be null (same as
     *         disabled)
     * @see Plugin#isCorePlugin()
     * @see Plugin#isGloballyEnabled()
     */
    public Map<String, Boolean> getCustomPlugins() {
        return customPlugins;
    }

    /**
     * Additional plugins which could be used or shouldn't be used (depending on
     * given argument) by {@link IFindBugsEngine}. If a plugin is not included
     * in the set, it's enablement depends on it's default settings.
     *
     * @return set with additional third party plugins, might be empty, never
     *         null. The elements are either absolute plugin paths or plugin id's.
     *         <b>Special case</b>: if the path consists of one path segment
     *         then it represents the plugin id for a plugin to be
     *         <b>disabled</b>.
     * @see Plugin#isCorePlugin()
     * @see Plugin#isGloballyEnabled()
     */
    public Set<String> getCustomPlugins(boolean enabled){
        Set<Entry<String, Boolean>> entrySet = customPlugins.entrySet();
        Set<String> result = new TreeSet<String>();
        for (Entry<String, Boolean> entry : entrySet) {
            if(enabled) {
                if(entry.getValue() != null && entry.getValue().booleanValue()) {
                    result.add(entry.getKey());
                }
            } else {
                if(entry.getValue() == null || !entry.getValue().booleanValue()) {
                    result.add(entry.getKey());
                }
            }
        }
        return result;
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
    private static Map<String, Boolean> readProperties(Properties props, String keyPrefix) {
        Map<String, Boolean> filters = new TreeMap<String, Boolean>();
        int counter = 0;
        boolean keyFound = true;
        while (keyFound) {
            String property = props.getProperty(keyPrefix + counter);
            if (property != null) {
                int pipePos = property.indexOf(BOOL_SEPARATOR);
                if (pipePos >= 0) {
                    String name = property.substring(0, pipePos);
                    String enabled = property.substring(pipePos + 1);
                    filters.put(name, Boolean.valueOf(enabled));
                } else {
                    filters.put(property, Boolean.TRUE);
                }
                counter++;
            } else {
                keyFound = false;
            }
        }

        return filters;
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
    private static void writeProperties(Properties props, String keyPrefix, Map<String, Boolean> filters) {
        int counter = 0;
        Set<Entry<String, Boolean>> entrySet = filters.entrySet();
        for (Entry<String, Boolean> entry : entrySet) {
            props.setProperty(keyPrefix + counter, entry.getKey() + BOOL_SEPARATOR + entry.getValue());
            counter++;
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
     * Returns the effort level as an array of feature settings as expected by
     * FindBugs.
     *
     * @return The array of feature settings corresponding to the current effort
     *         setting.
     */
    public AnalysisFeatureSetting[] getAnalysisFeatureSettings() {
        if (EFFORT_DEFAULT.equals(effort)) {
            return FindBugs.DEFAULT_EFFORT;
        } else if (EFFORT_MIN.equals(effort)) {
            return FindBugs.MIN_EFFORT;
        }
        return FindBugs.MAX_EFFORT;
    }
}
