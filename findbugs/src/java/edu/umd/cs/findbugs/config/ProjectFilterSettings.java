/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.I18N;

/**
 * Settings for user filtering of warnings for a project.
 * This includes selecting particular bug categories
 * to view, as well as a minimum warning priority.
 * Includes support for encoding these settings as a String,
 * which can easily be stored as a persistent project property
 * in Eclipse.
 * 
 * @see BugInstance
 * @author David Hovemeyer
 */
public class ProjectFilterSettings implements Cloneable {
	/** Text string for high priority. */
	public static final String HIGH_PRIORITY = "High";

	/** Text string for medium priority. */
	public static final String MEDIUM_PRIORITY = "Medium";

	/** Text string for low priority. */
	public static final String LOW_PRIORITY = "Low";
	
	/** Text string for experimental priority. */
	public static final String EXPERIMENTAL_PRIORITY = "Experimental";

	/** Default warning threshold priority. */
	public static final String DEFAULT_PRIORITY = MEDIUM_PRIORITY;

	/** Map of priority level names to their numeric values.  */
	private static Map<String, Integer> priorityNameToValueMap = new HashMap<String, Integer>();
	static {
		priorityNameToValueMap.put(HIGH_PRIORITY, new Integer(Detector.HIGH_PRIORITY));
		priorityNameToValueMap.put(MEDIUM_PRIORITY, new Integer(Detector.NORMAL_PRIORITY));
		priorityNameToValueMap.put(LOW_PRIORITY, new Integer(Detector.LOW_PRIORITY));
		priorityNameToValueMap.put(EXPERIMENTAL_PRIORITY, new Integer(Detector.EXP_PRIORITY));
	}
	
	/**
	 * The character used for delimiting whole fields in filter settings encoded as strings
	 */
	private static String FIELD_DELIMITER="|";
	/**
	 * The character used for delimiting list items in filter settings encoded as strings
	 */
	private static String LISTITEM_DELIMITER=",";

	// Fields
	private Set<String> activeBugCategorySet;
	private List<DetectorFactory> detectorFactories;
	private String minPriority;
	private int minPriorityAsInt;
	
	/**
	 * Constructor.
	 * This is not meant to be called directly; use one of the factory methods instead.
	 */
	private ProjectFilterSettings() {
		this.activeBugCategorySet = new HashSet<String>();
		this.detectorFactories = new ArrayList<DetectorFactory>();
		setMinPriority(DEFAULT_PRIORITY);
	}
	
	/**
	 * Factory method to create a default ProjectFilterSettings object.
	 * Uses the default warning priority threshold, and enables
	 * all bug categories.
	 * 
	 * @return a default ProjectFilterSettings object
	 */
	public static ProjectFilterSettings createDefault() {
		ProjectFilterSettings result = new ProjectFilterSettings();
		
		// Add all bugs categories
		for (Iterator<String> i = I18N.instance().getBugCategories().iterator(); i.hasNext(); ) {
			result.addCategory(i.next());
		}
		
		// Add enabled detector factories
		Iterator iterator =
			DetectorFactoryCollection.instance().factoryIterator();
		while (iterator.hasNext()) {
			DetectorFactory factory = (DetectorFactory) iterator.next();
			if (factory.isEnabled())
				result.getDetectorFactories().add(factory);
		}
		
		// Set default priority threshold
		result.setMinPriority(DEFAULT_PRIORITY);
		
		return result;
	}

	/**
	 * Create ProjectFilterSettings from an encoded string.
	 * 
	 * @param s the encoded string
	 * @return the ProjectFilterSettings
	 */
	public static ProjectFilterSettings fromEncodedString(String s) {
		ProjectFilterSettings result = new ProjectFilterSettings();
		
		int bar = s.indexOf(FIELD_DELIMITER);
		if (bar >= 0) {
			String minPriority = s.substring(0, bar);
			if (priorityNameToValueMap.get(minPriority) == null)
				minPriority = DEFAULT_PRIORITY;
			
			result.setMinPriority(minPriority);
			
			s = s.substring(bar + 1);
		}
		
		// Parse bug categories
		bar = s.indexOf(FIELD_DELIMITER);		
		String categories = (bar >=0) ? s.substring(0,bar) : s;
		StringTokenizer t = new StringTokenizer(categories, LISTITEM_DELIMITER);
		while (t.hasMoreTokens()) {
			String category = t.nextToken();
			result.addCategory(category);
		}
		
		// Parse detector factories
		if (bar>=0){
			String factories = s.substring(bar+1,s.length());
			t = new StringTokenizer(factories, LISTITEM_DELIMITER);
			while (t.hasMoreTokens()) {				
					DetectorFactory factory =
						DetectorFactoryCollection.instance().getFactory(t.nextToken());
					if (factory != null) {
						result.getDetectorFactories().add(factory);
					}				
			}
			
		}
		
		return result;
			
	}
	

	/**
	 * Return whether or not a warning should be displayed,
	 * according to the project filter settings.
	 * 
	 * @param bugInstance the warning
	 * @return true if the warning should be displayed, false if not
	 */
	public boolean displayWarning(BugInstance bugInstance) {
		return bugInstance.getPriority() <= getMinPriorityAsInt()
			&& containsCategory(bugInstance.getBugPattern().getCategory());
	}
	
	/**
	 * Set minimum warning priority threshold.
	 * 
	 * @param value the priority: one of "High", "Medium", or "Low"
	 */
	public void setMinPriority(String minPriority) {
		this.minPriority = minPriority;
		
		Integer value = priorityNameToValueMap.get(minPriority);
		if (value == null) {
			value = priorityNameToValueMap.get(DEFAULT_PRIORITY);
			if (value == null)
				throw new IllegalStateException();
		}
		
		this.minPriorityAsInt = value.intValue();

	}
	
	/**
	 * Get the minimum warning priority threshold.
	 * 
	 * @return minimum warning priority threshold: one of "High", "Medium", or "Low"
	 */
	public String getMinPriority() {
		return this.minPriority;
	}
	
	/**
	 * Return the minimum warning priority threshold as an integer.
	 * 
	 * @return the minimum warning priority threshold as an integer
	 */
	public int getMinPriorityAsInt() {
		return minPriorityAsInt;
	}

	/**
	 * Add a bug category to the set of categories to be displayed.
	 * 
	 * @param category the bug category: e.g., "CORRECTNESS"
	 */
	public void addCategory(String category) {
		this.activeBugCategorySet.add(category);
	}

	/**
	 * Remove a bug category from the set of categories to be displayed.
	 *
	 * @param category the bug category: e.g., "CORRECTNESS"
	 */
	public void removeCategory(String category) {
		this.activeBugCategorySet.remove(category);
	}

	/**
	 * Clear all bug categories.
	 */
	public void clearAllCategories() {
		this.activeBugCategorySet.clear();
	}
	
	/**
	 * Return whether or not the given category is enabled
	 * in the project filter settings.
	 * 
	 * @param category the category
	 * @return true if the category is enabled, false if not
	 */
	public boolean containsCategory(String category) {
		return activeBugCategorySet.contains(category);
	}
	
	/**
	 * Return set of active (enabled) bug categories.
	 * 
	 * @return the set of active categories
	 */
	public Set<String> getActiveCategorySet() {
		Set<String> result = new HashSet<String>();
		result.addAll(this.activeBugCategorySet);
		return result;
	}

	/**
	 * Create a string containing the encoded form of the ProjectFilterSettings.
	 * 
	 * @return an encoded string
	 */
	public String toEncodedString() {
		StringBuffer buf = new StringBuffer();
		buf.append(getMinPriority());
		
		// Encode bug categories
		buf.append(FIELD_DELIMITER);
		for (Iterator<String> i = activeBugCategorySet.iterator(); i.hasNext(); ) {
			buf.append(i.next());
			if (i.hasNext())
				buf.append(LISTITEM_DELIMITER);			
			
		}
		
		// Encode active bug factories
		buf.append(FIELD_DELIMITER);
		for (Iterator<DetectorFactory> i = this.detectorFactories.iterator(); i.hasNext(); ) {
			buf.append(i.next().getShortName());
			if (i.hasNext())
				buf.append(LISTITEM_DELIMITER);
		}		
		
		return buf.toString();
	}

	public String toString() {
		return toEncodedString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass())
			return false;
		ProjectFilterSettings other = (ProjectFilterSettings) obj;
		
		if (!this.getMinPriority().equals(other.getMinPriority()))
			return false;
		
		Set<String> mine = this.activeBugCategorySet;
		Set<String> yours = other.activeBugCategorySet;
		if (!mine.containsAll(yours) || !yours.containsAll(mine))
			return false;
		
		List<DetectorFactory> myFactories = this.getDetectorFactories();
		List<DetectorFactory> theirFactories = other.getDetectorFactories();
		if (!myFactories.containsAll(theirFactories) || !theirFactories.containsAll(myFactories))
			return false;
		
		return true;
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone()  {
		try {
			// Create shallow copy
			ProjectFilterSettings clone = (ProjectFilterSettings) super.clone();
			
			// Copy field contents
			clone.activeBugCategorySet = new HashSet<String>();
			clone.activeBugCategorySet.addAll(this.activeBugCategorySet);
			clone.detectorFactories = new ArrayList<DetectorFactory>();
			clone.detectorFactories.addAll(this.detectorFactories);
			clone.setMinPriority(this.getMinPriority());
			
			return clone;
		} catch (CloneNotSupportedException e) {
			// Should not happen!
			throw new IllegalStateException();
		}
		
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return minPriority.hashCode() + 1009 * activeBugCategorySet.hashCode();
	}
	
	/**
	 * Convert an integer warning priority threshold value to
	 * a String.
	 */
	public static String getIntPriorityAsString(int prio) {
		String minPriority;
		switch (prio) {
			case Detector.EXP_PRIORITY:
				minPriority = ProjectFilterSettings.EXPERIMENTAL_PRIORITY;
				break;
			case Detector.LOW_PRIORITY:
				minPriority = ProjectFilterSettings.LOW_PRIORITY;
				break;
			case Detector.NORMAL_PRIORITY:
				minPriority = ProjectFilterSettings.MEDIUM_PRIORITY;
				break;
			case Detector.HIGH_PRIORITY:
				minPriority = ProjectFilterSettings.HIGH_PRIORITY;
				break;
			default:
				minPriority = ProjectFilterSettings.DEFAULT_PRIORITY;
				break;
		}
		return minPriority;
	}
	/**
	 * @return Returns the enabled detector factories.
	 */
	public List<DetectorFactory> getDetectorFactories() {
		return detectorFactories;
	}
	/**
	 * @param detectorFactories The list of detector factories to set.
	 */
	public void setDetectorFactories(List<DetectorFactory> detectorFactories) {
		this.detectorFactories = detectorFactories;
	}
}

// vim:ts=4
